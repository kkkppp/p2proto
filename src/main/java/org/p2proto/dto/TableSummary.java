package org.p2proto.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Base class containing basic table information without columns.
 * Used for table listings where column details are not needed.
 */
@Getter
@Builder(builderMethodName = "summaryBuilder")
public class TableSummary {
    private final UUID id;
    private final String tableName;            // Physical name
    private final String tableLabel;           // Singular label
    private final String tablePluralLabel;     // Plural label
    private final TableTypeEnum tableType;

    public enum TableTypeEnum {
        STANDARD, USERS, ACCESS
    }
}
