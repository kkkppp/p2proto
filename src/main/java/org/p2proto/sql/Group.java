package org.p2proto.sql;

import java.util.*;

public final class Group implements Criterion {
    public final Logic logic;
    public final List<Criterion> items;

    private Group(Logic logic, List<Criterion> items) {
        this.logic = Objects.requireNonNull(logic);
        this.items = List.copyOf(items);
    }
    public static Group and(Criterion... cs) { return new Group(Logic.AND, Arrays.asList(cs)); }
    public static Group or (Criterion... cs) { return new Group(Logic.OR , Arrays.asList(cs)); }
}
