package org.p2proto.formula;

import org.p2proto.formula.parser.FormulaBaseVisitor;
import org.p2proto.formula.parser.FormulaParser;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Visits the parse tree, validates variables and functions, and emits Postgres SQL.
 */
public class FormulaSqlVisitor extends FormulaBaseVisitor<String> {

    private final Set<String> allowedVariables;
    private final boolean useConcatFunction; // true -> concat(a,b,c); false -> a || b || c

    public FormulaSqlVisitor(Set<String> allowedVariables, boolean useConcatFunction) {
        this.allowedVariables = Objects.requireNonNull(allowedVariables);
        this.useConcatFunction = useConcatFunction;
    }

    @Override
    public String visitParse(FormulaParser.ParseContext ctx) {
        return visit(ctx.expr());
    }

    @Override
    public String visitFunctionCall(FormulaParser.FunctionCallContext ctx) throws ValidationException {
        String fn = ctx.IDENT().getText();
        if (!"concat".equalsIgnoreCase(fn)) {
            throw new ValidationException("Unsupported function: " + fn + ". Only concat(...) is allowed.");
        }

        List<String> args = new ArrayList<>();
        if (ctx.argList() != null) {
            for (FormulaParser.ExprContext e : ctx.argList().expr()) {
                args.add(visit(e)); // recursively process (validates nested concat/variables)
            }
        }

        if (args.isEmpty()) {
            throw new ValidationException("concat(...) requires at least 1 argument.");
        }

        // Emit Postgres-friendly SQL
        if (useConcatFunction) {
            // CONCAT(varargs)
            return "concat(" + String.join(", ", args) + ")";
        } else {
            // a || b || c (needs care for NULLs — wrap with coalesce if you want non-null outputs)
            // Example: coalesce(a,'') || coalesce(b,'') || ' ' || coalesce(c,'')
            return args.stream()
                    .map(this::wrapForNullSafeConcat)
                    .collect(Collectors.joining(" || "));
        }
    }

    @Override
    public String visitAtom(FormulaParser.AtomContext ctx) {
        if (ctx.VARIABLE() != null) {
            String raw = ctx.VARIABLE().getText(); // like "$first_name"
            String name = raw.substring(1);        // strip leading $
            validateVariableName(name);
            return quoteIdentifierIfNeeded(name);
        } else if (ctx.STRING() != null) {
            return normalizeStringLiteral(ctx.STRING().getText());
        }
        throw new ValidationException("Unsupported atom: " + ctx.getText());
    }

    // ---------- helpers ----------

    private void validateVariableName(String name) {
        if (!allowedVariables.contains(name)) {
            throw new ValidationException("Unknown variable: $" + name);
        }
    }

    /**
     * Return a SQL identifier. Simple approach: if it’s not strictly [a-z_][a-z0-9_]*, then double-quote it.
     */
    private String quoteIdentifierIfNeeded(String ident) {
        if (ident.matches("[a-z_][a-z0-9_]*")) {
            return ident;
        }
        // Escape embedded double quotes by doubling them
        String escaped = ident.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    /**
     * Normalize single-quoted string. We accept the token as-is (already quoted).
     * Optionally, you can unescape/re-escape. Here we ensure it's valid SQL literal.
     */
    private String normalizeStringLiteral(String tokenText) {
        // tokenText includes the surrounding quotes.
        // Make sure embedded single quotes are doubled for Postgres.
        String inner = tokenText.substring(1, tokenText.length() - 1)
                .replace("\\'", "'");         // lexer allows \' — convert to plain '
        inner = inner.replace("'", "''");      // SQL escape
        return "'" + inner + "'";
    }

    /**
     * For "||" mode: wrap expressions with COALESCE(expr,'') to avoid NULL propagation.
     * If you don't want that behavior, just return expr.
     */
    private String wrapForNullSafeConcat(String expr) {
        // Treat literals differently: string literal is already safe
        if (expr.startsWith("'") && expr.endsWith("'")) {
            return expr;
        }
        return "coalesce(" + expr + ", '')";
    }
}
