package org.p2proto.formula;

import org.junit.jupiter.api.Test;
import org.p2proto.dto.ColumnMetaData;
import org.p2proto.dto.TableMetadata;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FormulaUtilsTest {

    /**
     * Helper to build a simple TableMetadata with columns:
     *  - id (PK)
     *  - first_name
     *  - last_name
     */
    private TableMetadata buildSimpleTableMetadata() {
        ColumnMetaData idCol = new ColumnMetaData(
                UUID.randomUUID(),           // id
                "id",                        // name
                "ID",                        // label
                null,                        // domain
                true,                        // primaryKey
                false,                       // removable
                null,                        // defaultValue
                Collections.emptyMap()       // additionalProperties
        );

        ColumnMetaData firstName = new ColumnMetaData(
                UUID.randomUUID(),           // id
                "first_name",                // name
                "First Name",                // label
                null,                        // domain
                false,                       // primaryKey
                true,                        // removable
                null,                        // defaultValue
                Collections.emptyMap()
        );

        ColumnMetaData lastName = new ColumnMetaData(
                UUID.randomUUID(),
                "last_name",
                "Last Name",
                null,
                false,
                true,
                null,
                Collections.emptyMap()
        );

        return TableMetadata.builder()
                .tableName("test_table")
                // use Lombok @Singular("column") – adds all three columns
                .column(idCol)
                .column(firstName)
                .column(lastName)
                // primaryKeyMeta must be non-null and present in columnsByName
                .primaryKeyMeta(idCol)
                .build();
    }

    @Test
    void validateFormula_nullFormula_noException() {
        TableMetadata metadata = buildSimpleTableMetadata();
        assertDoesNotThrow(() -> FormulaUtils.validateFormula(null, metadata));
    }

    @Test
    void validateFormula_blankFormula_noException() {
        TableMetadata metadata = buildSimpleTableMetadata();
        assertDoesNotThrow(() -> FormulaUtils.validateFormula("   ", metadata));
    }

    @Test
    void validateFormula_validConcat_ok() {
        TableMetadata metadata = buildSimpleTableMetadata();
        String formula = "concat($first_name, ' ', $last_name)";
        assertDoesNotThrow(() -> FormulaUtils.validateFormula(formula, metadata));
    }

    @Test
    void validateFormula_unknownVariable_throwsValidationException() {
        TableMetadata metadata = buildSimpleTableMetadata();
        String formula = "concat($first_name, ' ', $middle_name)";

        assertThrows(ValidationException.class,
                () -> FormulaUtils.validateFormula(formula, metadata),
                "Expected ValidationException due to unknown variable $middle_name");
    }

    @Test
    void validateFormula_unsupportedFunction_throwsValidationException() {
        TableMetadata metadata = buildSimpleTableMetadata();
        String formula = "foo($first_name, $last_name)";

        assertThrows(ValidationException.class,
                () -> FormulaUtils.validateFormula(formula, metadata),
                "Expected ValidationException due to unsupported function foo");
    }

    @Test
    void validateFormula_syntaxError_throwsValidationException() {
        TableMetadata metadata = buildSimpleTableMetadata();
        // Missing closing parenthesis -> syntax error
        String formula = "concat($first_name, ' ', $last_name";

        assertThrows(ValidationException.class,
                () -> FormulaUtils.validateFormula(formula, metadata),
                "Expected ValidationException due to syntax error");
    }
}
