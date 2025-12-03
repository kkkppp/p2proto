package org.p2proto.formula.functions;


import org.p2proto.formula.types.Type;

import java.util.*;

import static org.p2proto.formula.functions.ParamKind.fixed;
import static org.p2proto.formula.functions.ParamKind.vararg;

public final class FunctionRegistry {
    private static final Map<String, FunctionSignature> REGISTRY = new HashMap<>();

    static {
        // concat(text, text, ...) -> TEXT
        REGISTRY.put("concat", new FunctionSignature(
                "concat",
                1, Optional.empty(),
                List.of(vararg(Type.TEXT)),
                Type.TEXT
        ));

        // substring(text, from[, len]) -> TEXT
        REGISTRY.put("substring", new FunctionSignature(
                "substring",
                2, Optional.of(3),
                List.of(fixed(Type.TEXT), fixed(Type.INT), vararg(Type.INT)), // 2nd is INT; optional 3rd INT
                Type.TEXT
        ));

        // upper(text) -> TEXT
        REGISTRY.put("upper", new FunctionSignature(
                "upper",
                1, Optional.of(1),
                List.of(fixed(Type.TEXT)),
                Type.TEXT
        ));

        // lower(text) -> TEXT
        REGISTRY.put("lower", new FunctionSignature(
                "lower",
                1, Optional.of(1),
                List.of(fixed(Type.TEXT)),
                Type.TEXT
        ));
    }

    private FunctionRegistry() {}

    public static Optional<FunctionSignature> get(String name) {
        return Optional.ofNullable(REGISTRY.get(name.toLowerCase(Locale.ROOT)));
    }

    public static Set<String> names() {
        return Collections.unmodifiableSet(REGISTRY.keySet());
    }
}
