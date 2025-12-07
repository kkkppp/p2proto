package org.p2proto.formula;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.p2proto.dto.TableMetadata;
import org.p2proto.dto.ColumnMetaData;
import org.p2proto.formula.parser.FormulaLexer;
import org.p2proto.formula.parser.FormulaParser;

import java.util.*;
import java.util.function.Function;
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
     * Same semantics as before, but delegates to
     * {@link #validateFormulaAndExtractColumns(String, TableMetadata)} and ignores the result.
     *
     * @param formula       formula text, e.g. "concat($first_name, ' ', upper($last_name))"
     * @param tableMetadata table metadata containing allowed columns
     * @throws ValidationException if syntax, variable names, or function usage is invalid
     */
    public static void validateFormula(String formula,
                                       TableMetadata tableMetadata) throws ValidationException {
        validateFormulaAndExtractColumns(formula, tableMetadata);
    }

    /**
     * Validate a single formula string against the given table metadata and
     * return the set of referenced columns.
     *
     * <ul>
     *   <li>All referenced variables must match existing column names.</li>
     *   <li>Function names/arity must match what {@link FormulaSqlVisitor} enforces.</li>
     *   <li>Syntax must be valid according to Formula.g4.</li>
     * </ul>
     *
     * @param formula       formula text, e.g. "concat($first_name, ' ', upper($last_name))"
     * @param tableMetadata table metadata containing allowed columns
     * @return ordered set of referenced ColumnMetaData (order of first appearance in the formula)
     * @throws ValidationException if syntax, variable names, or function usage is invalid
     */
    public static Set<ColumnMetaData> validateFormulaAndExtractColumns(String formula,
                                                                       TableMetadata tableMetadata)
            throws ValidationException {

        // Treat null/blank as "no formula" – change if you want to forbid empty
        if (formula == null || formula.isBlank()) {
            return Collections.emptySet();
        }

        // 1) Collect allowed variable names from table metadata (column names)
        List<ColumnMetaData> columns = tableMetadata.getColumns();
        Set<String> allowedVariables = columns.stream()
                .map(ColumnMetaData::getName)
                .collect(Collectors.toUnmodifiableSet());

        // Also build a lookup map name -> ColumnMetaData so we can map back later
        Map<String, ColumnMetaData> columnsByName = columns.stream()
                .collect(Collectors.toMap(
                        ColumnMetaData::getName,
                        Function.identity(),
                        (a, b) -> a,
                        LinkedHashMap::new // preserve insertion order
                ));

        try {
            // 2) Setup ANTLR lexer/parser
            CharStream input = CharStreams.fromString(formula);
            FormulaLexer lexer = new FormulaLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            FormulaParser parser = new FormulaParser(tokens);

            // 2a) Convert parse errors into ValidationException instead of printing to stderr
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

            // 5) Extract referenced variable names from the visitor and map to ColumnMetaData
            Set<String> referencedNames = visitor.getReferencedVariables();

            // Preserve formula-order of referenced variables (visitor should use LinkedHashSet)
            Set<ColumnMetaData> referencedColumns = new LinkedHashSet<>();
            for (String name : referencedNames) {
                ColumnMetaData col = columnsByName.get(name);
                if (col == null) {
                    // This should not happen if visitor validated variables correctly,
                    // but guard anyway.
                    throw new ValidationException("Referenced variable $" + name +
                            " not found in table metadata columns");
                }
                referencedColumns.add(col);
            }

            return Collections.unmodifiableSet(referencedColumns);

        } catch (RuntimeException ex) {
            // If syntax error or any other RuntimeException:
            String msg = "Invalid formula '" + formula + "': " + ex.getMessage();
            throw new ValidationException(msg, ex);
        }
    }
}
