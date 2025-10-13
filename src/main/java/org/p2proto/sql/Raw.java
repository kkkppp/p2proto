package org.p2proto.sql;

import java.util.*;

public final class Raw implements Criterion {
    public final String sql;
    public final Map<String, Object> args;
    public Raw(String sql, Map<String, Object> args) {
        this.sql  = Objects.requireNonNull(sql);
        this.args = Map.copyOf(Objects.requireNonNull(args));
    }
}
