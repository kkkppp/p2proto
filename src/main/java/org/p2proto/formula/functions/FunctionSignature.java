package org.p2proto.formula.functions;


import org.p2proto.formula.types.Type;

import java.util.List;
import java.util.Optional;

public class FunctionSignature {
    public final String name;
    public final int minArity;
    public final Optional<Integer> maxArity; // empty for unlimited (vararg)
    public final List<ParamKind> params;     // fixed params in order; final item can be VARARG

    public final Type returns;

    public FunctionSignature(String name, int minArity, Optional<Integer> maxArity, List<ParamKind> params, Type returns) {
        this.name = name;
        this.minArity = minArity;
        this.maxArity = maxArity;
        this.params = params;
        this.returns = returns;
    }
}
