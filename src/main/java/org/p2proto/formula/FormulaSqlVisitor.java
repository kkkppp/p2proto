package org.p2proto.formula;

import org.p2proto.formula.functions.FunctionRegistry;
import org.p2proto.formula.functions.FunctionSignature;
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

    // track all referenced variables in order of first appearance
    private final Set<String> referencedVariables = new LinkedHashSet<>();

    public FormulaSqlVisitor(Set<String> allowedVariables, boolean useConcatFunction) {
        this.allowedVariables = Objects.requireNonNull(allowedVariables);
        this.useConcatFunction = useConcatFunction;
    }

    /** Expose referenced variable names (without the '$' prefix). */
    public Set<String> getReferencedVariables() {
        return Collections.unmodifiableSet(referencedVariables);
    }

    @Override
    public String visitParse(FormulaParser.ParseContext ctx) {
        return visit(ctx.expr());
    }

    @Override
    public String visitFunctionCall(FormulaParser.FunctionCallContext ctx) {
        String fnRaw = ctx.IDENT().getText();
        String fnLower = fnRaw.toLowerCase(Locale.ROOT);

        // 1) Lookup function in registry
        FunctionSignature signature = FunctionRegistry.get(fnLower)
                .orElseThrow(() -> new ValidationException("Unsupported function: " + fnRaw));

        // 2) Count arguments
        List<FormulaParser.ExprContext> argExprs =
                (ctx.argList() != null) ? ctx.argList().expr() : List.of();

        int argCount = argExprs.size();
        int minArgs = signature.minArity;           // <-- use field
        Optional<Integer> maxArgsOpt = signature.maxArity; // <-- use field

        // 3) Validate arity
        if (argCount < minArgs) {
            throw new ValidationException(
                    "Function " + fnRaw + " expects at least " + minArgs +
                            " argument(s), got " + argCount
            );
        }
        if (maxArgsOpt.isPresent() && argCount > maxArgsOpt.get()) {
            throw new ValidationException(
                    "Function " + fnRaw + " expects at most " + maxArgsOpt.get() +
                            " argument(s), got " + argCount
            );
        }

        // 4) Recursively visit arguments (validates variables, nested functions, etc.)
        List<String> args = argExprs.stream()
                .map(this::visit)
                .collect(Collectors.toList());

        if (args.isEmpty()) {
            // Should be impossible if minArgs >= 1, but keep defensive
            throw new ValidationException("Function " + fnRaw + " requires at least 1 argument.");
        }

        // 5) Emit SQL depending on function
        switch (fnLower) {
            case "concat":
                return buildConcatSql(args);
            case "substring":
                return buildSubstringSql(args);
            case "upper":
            case "lower":
                return fnLower + "(" + args.get(0) + ")";
            default:
                throw new ValidationException("No SQL generator implemented for function: " + fnRaw);
        }
    }

    @Override
    public String visitAtom(FormulaParser.AtomContext ctx) {
        if (ctx.VARIABLE() != null) {
            String raw = ctx.VARIABLE().getText(); // like "$first_name"
            String name = raw.substring(1);        // strip leading $

            validateVariableName(name);
            referencedVariables.add(name);

            return quoteIdentifierIfNeeded(name);
        } else if (ctx.STRING() != null) {
            return normalizeStringLiteral(ctx.STRING().getText());
        }
        throw new ValidationException("Unsupported atom: " + ctx.getText());
    }

    // ---------- helpers ----------

    private String buildConcatSql(List<String> args) {
        if (useConcatFunction) {
            return "concat(" + String.join(", ", args) + ")";
        } else {
            return args.stream()
                    .map(this::wrapForNullSafeConcat)
                    .collect(Collectors.joining(" || "));
        }
    }

    private String buildSubstringSql(List<String> args) {
        // Registry already enforced min/max args: substring(text, from[, len])
        return "substring(" + String.join(", ", args) + ")";
    }

    private void validateVariableName(String name) {
        if (!allowedVariables.contains(name)) {
            throw new ValidationException("Unknown variable: $" + name);
        }
    }

    private String quoteIdentifierIfNeeded(String ident) {
        if (ident.matches("[a-z_][a-z0-9_]*")) {
            return ident;
        }
        String escaped = ident.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private String normalizeStringLiteral(String tokenText) {
        String inner = tokenText.substring(1, tokenText.length() - 1)
                .replace("\\'", "'");     // lexer allows \' — convert to plain '
        inner = inner.replace("'", "''"); // SQL escape
        return "'" + inner + "'";
    }

    private String wrapForNullSafeConcat(String expr) {
        if (expr.startsWith("'") && expr.endsWith("'")) {
            return expr;
        }
        return "coalesce(" + expr + ", '')";
    }
}
