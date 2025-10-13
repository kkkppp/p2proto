package org.p2proto.sql;

import java.util.*;

public final class Condition implements Criterion {
    public final String column;
    public final Op op;
    public final Object value;

    private Condition(String column, Op op, Object value) {
        this.column = Objects.requireNonNull(column, "column");
        this.op = Objects.requireNonNull(op, "op");
        this.value = value;
    }
    public static Condition of(String column, Op op, Object value){ return new Condition(column, op, value); }

    public static Condition eq(String c, Object v)    { return of(c, Op.EQ, v); }
    public static Condition ne(String c, Object v)    { return of(c, Op.NE, v); }
    public static Condition gt(String c, Object v)    { return of(c, Op.GT, v); }
    public static Condition ge(String c, Object v)    { return of(c, Op.GE, v); }
    public static Condition lt(String c, Object v)    { return of(c, Op.LT, v); }
    public static Condition le(String c, Object v)    { return of(c, Op.LE, v); }
    public static Condition like(String c, Object v)  { return of(c, Op.LIKE, v); }
    public static Condition ilike(String c, Object v) { return of(c, Op.ILIKE, v); }
    public static Condition in(String c, java.util.Collection<?> v)    { return of(c, Op.IN, List.copyOf(v)); }
    public static Condition notIn(String c, java.util.Collection<?> v) { return of(c, Op.NOT_IN, List.copyOf(v)); }
    public static Condition between(String c, Object a, Object b)      { return of(c, Op.BETWEEN, List.of(a,b)); }
    public static Condition isNull(String c)      { return of(c, Op.IS_NULL, null); }
    public static Condition isNotNull(String c)   { return of(c, Op.IS_NOT_NULL, null); }
}
