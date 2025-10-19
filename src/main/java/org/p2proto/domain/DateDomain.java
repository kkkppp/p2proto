package org.p2proto.domain;

public final class DateDomain extends BaseDomain {
	public static final DateDomain INSTANCE = new DateDomain();

	public DateDomain() {
		super(4, "DATE", "label.domain.date", "DATE", false, false);
	}

	@Override
	public Object convertValue(Object value) {
		if (value == null) return null;
		if (value instanceof java.time.LocalDate d) return d;
		if (value instanceof java.sql.Date d) return d.toLocalDate();
		if (value instanceof String s) return java.time.LocalDate.parse(s);
		return value;
	}
}



