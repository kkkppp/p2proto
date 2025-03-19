package org.p2proto.ddl;

import org.springframework.context.MessageSource;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public enum Domain {

    UUID(1, "UUID", "label.domain.uuid", "UUID"),
    TEXT(2, "TEXT", "label.domain.text", "VARCHAR(255)"),
    BOOLEAN(3, "BOOLEAN", "label.domain.boolean", "BOOLEAN"),
    DATE(4, "DATE", "label.domain.date", "DATE"),
    DATETIME(5, "DATETIME", "label.domain.datetime", "TIMESTAMP"),
    INTEGER(6, "INTEGER", "label.domain.integer", "INTEGER"),
    FLOAT(7, "FLOAT", "label.domain.float", "FLOAT"),
    AUTOINCREMENT(8, "AUTOINCREMENT", "label.domain.autoincrement", "SERIAL"),
    PASSWORD(9, "PASSWORD", "label.domain.password", "VARCHAR(255)");

    private final int code;
    private final String internalName;
    private final String resourceId;       // For Spring NLS
    private final String liquibaseType;    // For Liquibase migrations

    Domain(int code, String internalName, String resourceId, String liquibaseType) {
        this.code = code;
        this.internalName = internalName;
        this.resourceId = resourceId;
        this.liquibaseType = liquibaseType;
    }

    public int getCode() {
        return code;
    }

    public String getInternalName() {
        return internalName;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getLiquibaseType() {
        return liquibaseType;
    }

    /**
     * Returns the localized label for this enum constant, using the provided MessageSource and Locale.
     */
    public String getLocalizedLabel(MessageSource messageSource, Locale locale) {
        return messageSource.getMessage(resourceId, null, locale);
    }

    /**
     * Lookup the enum based on its numeric code.
     */
    public static Domain fromCode(int code) {
        return Arrays.stream(values())
                .filter(domain -> domain.getCode() == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown Domain code: " + code));
    }

    /**
     * Lookup the enum based on its internal name.
     */
    public static Domain fromInternalName(String internalName) {
        return Arrays.stream(values())
                .filter(domain -> domain.getInternalName().equalsIgnoreCase(internalName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown Domain internalName: " + internalName));
    }

    /**
     * Retrieves a sorted list of strings in the format:
     * "internalName - localizedLabel"
     * sorted alphabetically by the localized label in the given Locale.
     */
    public static List<String> getSortedList(MessageSource messageSource, Locale locale) {
        return Arrays.stream(Domain.values())
                .sorted(Comparator.comparing(e -> e.getLocalizedLabel(messageSource, locale)))
                .map(e -> e.getInternalName() + " - " + e.getLocalizedLabel(messageSource, locale))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a sorted list of strings in the format:
     * "internalName - localizedLabel"
     * sorted alphabetically by the internalName.
     */
    public static List<String> getSortedListByInternalName(MessageSource messageSource, Locale locale) {
        return Arrays.stream(Domain.values())
                .sorted(Comparator.comparing(Domain::getInternalName))
                .map(e -> e.getInternalName() + " - " + e.getLocalizedLabel(messageSource, locale))
                .collect(Collectors.toList());
    }
}
