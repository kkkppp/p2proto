package org.p2proto.domain;

import org.springframework.context.MessageSource;

import java.util.Locale;

public sealed interface DomainType permits BaseDomain  {
	int code();
	String getInternalName();
	String resourceId();
	String getLiquibaseType();
	boolean isAutoIncrement();
	boolean isVirtual();

	default String getLocalizedLabel(MessageSource messageSource, Locale locale) {
		return messageSource.getMessage(resourceId(), null, locale);
	}

	default String wherePredicate(String columnName) {
		return columnName + " = ?";
	}

	default Object convertValue(Object value) {
		return value;
	}
}



