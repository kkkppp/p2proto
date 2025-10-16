package org.p2proto.sql;

import org.p2proto.domain.DomainType;
import org.p2proto.dto.ColumnMetaData;
import org.p2proto.dto.TableMetadata;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public final class WhereRenderer {

    private final TableMetadata meta;
    private final AtomicInteger counter = new AtomicInteger(0);

    public WhereRenderer(TableMetadata meta) { this.meta = Objects.requireNonNull(meta); }

    public WhereSql render(Criterion c) {
        if (c == null) return new WhereSql("", new MapSqlParameterSource());
        MapSqlParameterSource ps = new MapSqlParameterSource();
        String body = renderCriterion(c, ps);
        if (body.isBlank()) return new WhereSql("", ps);
        return new WhereSql("WHERE " + body, ps);
    }

    private String renderCriterion(Criterion c, MapSqlParameterSource ps) {
        if (c instanceof Condition cond) return renderCondition(cond, ps);
        if (c instanceof Group g)        return renderGroup(g, ps);
        if (c instanceof Raw raw) {
            raw.args.forEach(ps::addValue);
            return "(" + raw.sql + ")";
        }
        throw new IllegalArgumentException("Unknown criterion: " + c);
    }

    private String renderGroup(Group g, MapSqlParameterSource ps) {
        String glue = (g.logic == Logic.AND) ? " AND " : " OR ";
        List<String> parts = new ArrayList<>();
        for (Criterion sub : g.items) {
            String s = renderCriterion(sub, ps);
            if (!s.isBlank()) parts.add(s);
        }
        if (parts.isEmpty()) return "";
        return "(" + String.join(glue, parts) + ")";
    }

    private String renderCondition(Condition c, MapSqlParameterSource ps) {
        ColumnMetaData col = requireColumn(c.column);
        DomainType d = col.getDomain();
        String name = col.getName(); // add quoting here if needed

        if ((c.op == Op.EQ || c.op == Op.NE) && c.value == null) {
            return name + ((c.op == Op.EQ) ? " IS NULL" : " IS NOT NULL");
        }

        switch (c.op) {
            case IS_NULL:     return name + " IS NULL";
            case IS_NOT_NULL: return name + " IS NOT NULL";

            case LIKE:
            case ILIKE: {
                String p = nextParam();
                ps.addValue(p, c.value);
                String op = (c.op == Op.LIKE) ? "LIKE" : "ILIKE";
                return name + " " + op + " :" + p;
            }

            case IN:
            case NOT_IN: {
                List<?> vals = asList(c.value, "IN/NOT_IN");
                if (vals.isEmpty()) return (c.op == Op.IN) ? "1=0" : "1=1";
                String plist = vals.stream().map(v -> {
                    String p = nextParam();
                ps.addValue(p, convert(d, v));
                    return ":" + p;
                }).collect(Collectors.joining(", "));
                return name + ((c.op == Op.IN) ? " IN (" : " NOT IN (") + plist + ")";
            }

            case BETWEEN: {
                List<?> pair = asList(c.value, "BETWEEN");
                if (pair.size() != 2) throw new IllegalArgumentException("BETWEEN needs 2 values");
                String p1 = nextParam(), p2 = nextParam();
                ps.addValue(p1, convert(d, pair.get(0)));
                ps.addValue(p2, convert(d, pair.get(1)));
                return name + " BETWEEN :" + p1 + " AND :" + p2;
            }

            case EQ, NE, GT, GE, LT, LE: {
                String p = nextParam();
                ps.addValue(p, convert(d, c.value));
                String op = switch (c.op) {
                    case EQ -> "="; case NE -> "<>"; case GT -> ">"; case GE -> ">="; case LT -> "<"; case LE -> "<=";
                    default -> throw new IllegalStateException();
                };
                if ("UUID".equalsIgnoreCase(d.getInternalName())) return name + " " + op + " :" + p + "::uuid";
                return name + " " + op + " :" + p;
            }
        }
        throw new UnsupportedOperationException("Unsupported operator: " + c.op);
    }

    private ColumnMetaData requireColumn(String name) {
        ColumnMetaData c = meta.getColumnsByName().get(name);
        if (c == null) throw new IllegalArgumentException("Unknown column: " + name);
        return c;
    }

    private String nextParam() { return "p" + counter.incrementAndGet(); }

    private static List<?> asList(Object v, String ctx) {
        if (v == null) return List.of();
        if (v instanceof List<?> l) return l;
        if (v instanceof Collection<?> c) return new ArrayList<>(c);
        throw new IllegalArgumentException(ctx + " expects a collection");
    }

    private static Object convert(DomainType d, Object v) {
        if (v == null) return null;
        if ("UUID".equalsIgnoreCase(d.getInternalName()) && !(v instanceof java.util.UUID)) {
            return java.util.UUID.fromString(v.toString());
        }
        return v;
    }
}
