// File: src/main/java/org/p2proto/repository/TableMetadataCrudRepository.java
package org.p2proto.repository;

import lombok.extern.slf4j.Slf4j;
import org.p2proto.ddl.Domain;
import org.p2proto.dto.ColumnDefaultHolder;
import org.p2proto.dto.ColumnMetaData;
import org.p2proto.dto.TableMetadata;
import org.p2proto.sql.*;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

import static org.p2proto.service.TableService.CURRENT_TIMESTAMP;

@Slf4j
public class TableMetadataCrudRepository {

    public static final String PASSWORD_MASK = "********";

    private final NamedParameterJdbcTemplate namedJdbc;
    private final TableMetadata meta;
    private final PasswordEncoder passwordEncoder;

    public TableMetadataCrudRepository(JdbcTemplate jdbcTemplate,
                                       TableMetadata tableMetadata,
                                       PasswordEncoder passwordEncoder) {
        this.namedJdbc = new NamedParameterJdbcTemplate(jdbcTemplate);
        this.meta = Objects.requireNonNull(tableMetadata, "tableMetadata");
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder");
    }

    // ---------- READ ----------

    public List<Map<String, Object>> findAll() {
        String sql = meta.generateSelectStatement();
        return namedJdbc.getJdbcTemplate().queryForList(sql);
    }

    public Optional<Map<String, Object>> findById(Object pkValue) {
        Criterion c = Condition.eq(meta.getPrimaryKeyMeta().getName(), pkValue);
        WhereSql ws = new WhereRenderer(meta).render(c);
        String sql = meta.generateSelectStatement() + " " + ws.sql();
        try {
            Map<String, Object> row = namedJdbc.queryForMap(sql, ws.params());
            return Optional.of(row);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<Map<String, Object>> findBy(Criterion criterion) {
        WhereSql ws = new WhereRenderer(meta).render(criterion);
        String sql = meta.generateSelectStatement() + " " + ws.sql();
        return namedJdbc.queryForList(sql, ws.params());
    }

    // ---------- INSERT ----------

    public int insert(Map<String, Object> rowData) {
        Map<String, Object> filtered = prepareAndFilterRowData(rowData, true);
        if (filtered.isEmpty()) throw new IllegalArgumentException("No valid columns to insert.");

        String cols = String.join(", ", filtered.keySet());
        String params = filtered.keySet().stream().map(k -> ":" + k).collect(Collectors.joining(", "));
        String sql = "INSERT INTO " + meta.getTableName() + " (" + cols + ") VALUES (" + params + ")";
        return namedJdbc.update(sql, new MapSqlParameterSource(filtered));
    }

    // ---------- UPDATE ----------

    public int update(Object pkValue, Map<String, Object> rowData) {
        // delegate to updateBy on PK criterion
        return updateBy(Condition.eq(meta.getPrimaryKeyMeta().getName(), pkValue), rowData);
    }

    /** Bulk/conditional UPDATE using criteria. Returns affected row count. */
    public int updateBy(Criterion criterion, Map<String, Object> rowData) {
        Map<String, Object> filtered = prepareAndFilterRowData(rowData, false);
        filtered.remove(meta.getPrimaryKeyMeta().getName()); // never update PK

        if (filtered.isEmpty()) return 0;

        String set = filtered.keySet().stream()
                .map(c -> c + " = :" + c)
                .collect(Collectors.joining(", "));

        WhereSql ws = new WhereRenderer(meta).render(criterion);
        String sql = "UPDATE " + meta.getTableName() + " SET " + set + " " + ws.sql();

        MapSqlParameterSource ps = new MapSqlParameterSource(filtered);
        // merge WHERE params last (no name conflicts because SET keys are column names, WHERE are p1,p2,...)
        ws.params().getValues().forEach(ps::addValue);

        return namedJdbc.update(sql, ps);
    }

    // ---------- DELETE ----------

    public int delete(Object pkValue) {
        return deleteBy(Condition.eq(meta.getPrimaryKeyMeta().getName(), pkValue));
    }

    /** Bulk/conditional DELETE using criteria. */
    public int deleteBy(Criterion criterion) {
        WhereSql ws = new WhereRenderer(meta).render(criterion);
        String sql = "DELETE FROM " + meta.getTableName() + " " + ws.sql();
        return namedJdbc.update(sql, ws.params());
    }

    // ---------- UPSERT-ish convenience ----------

    public int save(Object pkValue, Map<String, Object> recordData) {
        if (pkValue == null || pkValue.toString().isBlank()) return insert(recordData);
        return update(pkValue, recordData);
    }

    // ---------- Internals ----------

    private Map<String, Object> prepareAndFilterRowData(Map<String, Object> rowData, boolean insert) {
        Map<String, Object> src = (rowData == null) ? new HashMap<>() : new HashMap<>(rowData);

        // defaults
        for (ColumnMetaData c : meta.getColumns()) {
            if (c.getDomain() != null && c.getDomain().isAutoIncrement()) continue;
            if (isBlank(src.get(c.getName())) && c.getDefaultValue() != null) {
                Object def = getDefaultValue(c, insert);
                if (def != null) src.put(c.getName(), def);
            }
        }

        Map<String, Object> out = new LinkedHashMap<>();
        for (ColumnMetaData c : meta.getColumns()) {
            if (c.getDomain() != null && c.getDomain().isAutoIncrement()) continue;
            String name = c.getName();
            if (!src.containsKey(name)) continue;

            Object val = src.get(name);

            if (c.getDomain() == Domain.PASSWORD && PASSWORD_MASK.equals(val)) {
                continue; // ignore masked password (no change)
            }

            Object dbv = getDBValue(c.getDomain(), val);
            // keep explicit nulls to allow clearing fields
            out.put(name, dbv);
        }
        return out;
    }

    private static boolean isBlank(Object v) {
        return v == null || (v instanceof String s && s.isBlank());
    }

    private Object getDefaultValue(ColumnMetaData meta, boolean insert) {
        ColumnDefaultHolder def = meta.getDefaultValue();
        if (def == null) return null;

        boolean fire = (insert && def.getTriggerEvent() == ColumnDefaultHolder.TriggerEvent.ON_CREATE)
                || (!insert && def.getTriggerEvent() == ColumnDefaultHolder.TriggerEvent.ON_UPDATE);
        if (!fire) return null;

        Object raw = switch (def.getValueType()) {
            case CONSTANT   -> def.getValue();
            case FORMULA -> resolveExpression(def.getValue());
        };
        return getDBValue(meta.getDomain(), raw);
    }

    private Object resolveExpression(String expr) {
        if (CURRENT_TIMESTAMP.equals(expr)) return Timestamp.from(Instant.now());
        throw new IllegalArgumentException("Unknown expression: " + expr);
    }

    /** Uniform conversion for inserts/updates. */
    private Object getDBValue(Domain domain, Object value) {
        if (value == null) return null;
        if (value instanceof String s && s.isBlank()) return (domain == Domain.TEXT) ? "" : null;

        if (domain == null) return value;

        switch (domain) {
            case BOOLEAN -> {
                if (value instanceof Boolean b) return b;
                return Boolean.valueOf(value.toString().trim());
            }
            case PASSWORD -> {
                return passwordEncoder.encode(value.toString());
            }
            case DATE -> {
                if (value instanceof java.time.LocalDate d) return d;
                if (value instanceof java.sql.Date d) return d.toLocalDate();
                if (value instanceof String s) return java.time.LocalDate.parse(s);
                return value;
            }
            case DATETIME -> {
                if (value instanceof java.time.OffsetDateTime odt) return odt;
                if (value instanceof java.time.LocalDateTime ldt) return ldt;
                if (value instanceof java.time.Instant i) return java.time.OffsetDateTime.ofInstant(i, ZoneOffset.UTC);
                if (value instanceof java.util.Date d) return java.time.OffsetDateTime.ofInstant(d.toInstant(), ZoneOffset.UTC);
                if (value instanceof String s) {
                    try { return java.time.OffsetDateTime.parse(s); }
                    catch (Exception ignored) { return java.time.LocalDateTime.parse(s); }
                }
                return value;
            }
            case UUID -> {
                if (value instanceof java.util.UUID u) return u;
                return java.util.UUID.fromString(value.toString());
            }
            default -> {
                return value; // INTEGER, FLOAT, TEXT, etc.
            }
        }
    }
}
