package org.p2proto.formula;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.p2proto.dto.TableMetadata;
import org.p2proto.dto.ColumnMetaData;
import org.p2proto.formula.parser.FormulaLexer;
import org.p2proto.formula.parser.FormulaParser;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility methods for validating column formulas.
 */
public final class FormulaUtils {

    private FormulaUtils() {
        // utility class
    }

    /**
     * Validate a single formula string against the given table metadata.
     * <ul>
     *   <li>All referenced variables must match existing column names.</li>
     *   <li>Function names/arity must match what FormulaSqlVisitor enforces (concat/upper/lower/substring/...)</li>
     *   <li>Syntax must be valid according to Formula.g4.</li>
     * </ul>
     *
     * @param formula       formula text, e.g. "concat($first_name, ' ', upper($last_name))"
     * @param tableMetadata table metadata containing allowed columns
     * @throws ValidationException if syntax, variable names, or function usage is invalid
     */
    public static void validateFormula(String formula,
                                       TableMetadata tableMetadata) throws ValidationException {

        // Treat null/blank as "no formula" – change if you want to forbid empty
        if (formula == null || formula.isBlank()) {
            return;
        }

        // 1) Collect allowed variable names from table metadata (column names)
        Set<String> allowedVariables = tableMetadata.getColumns().stream()
                .map(ColumnMetaData::getName)
                .collect(Collectors.toUnmodifiableSet());

        try {
            // 2) Setup ANTLR lexer/parser
            CharStream input = CharStreams.fromString(formula);
            FormulaLexer lexer = new FormulaLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            FormulaParser parser = new FormulaParser(tokens);

            // 2a) Convert parse errors into FormulaValidationException instead of printing to stderr
            parser.removeErrorListeners();
            parser.addErrorListener(new BaseErrorListener() {
                @Override
                public void syntaxError(Recognizer<?, ?> recognizer,
                                        Object offendingSymbol,
                                        int line,
                                        int charPositionInLine,
                                        String msg,
                                        RecognitionException e) {
                    throw new RuntimeException("Syntax error at " + line + ":" + charPositionInLine + " - " + msg, e);
                }
            });

            // 3) Parse using the root rule from Formula.g4 (parse : expr EOF;)
            ParseTree tree = parser.parse();

            // 4) Walk with your visitor to enforce:
            //    - allowed variable names
            //    - allowed functions and arity
            // concatAsFunction = true here because we only care about validation
            FormulaSqlVisitor visitor = new FormulaSqlVisitor(allowedVariables, true);
            visitor.visit(tree);

        } catch (RuntimeException ex) {
            // If syntax error or any other RuntimeException:
            String msg = "Invalid formula '" + formula + "': " + ex.getMessage();
            throw new ValidationException(ex.getMessage());
        }
    }
}
