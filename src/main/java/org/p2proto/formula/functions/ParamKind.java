package org.p2proto.formula.functions;

import org.p2proto.formula.types.Type;

public final class ParamKind {
    public enum Kind { FIXED, VARARG }
    public final Kind kind;
    public final Type type;

    private ParamKind(Kind kind, Type type) {
        this.kind = kind; this.type = type;
    }

    public static ParamKind fixed(Type t) { return new ParamKind(Kind.FIXED, t); }
    public static ParamKind vararg(Type t) { return new ParamKind(Kind.VARARG, t); }
}
