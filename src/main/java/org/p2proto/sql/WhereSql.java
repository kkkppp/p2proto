package org.p2proto.sql;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

public record WhereSql(String sql, MapSqlParameterSource params) {}
