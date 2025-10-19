package org.p2proto.domain;

import java.time.Instant;
import java.time.ZoneOffset;

public final class DateTimeDomain extends BaseDomain {
	public static final DateTimeDomain INSTANCE = new DateTimeDomain();

	public DateTimeDomain() {
		super(5, "DATETIME", "label.domain.datetime", "TIMESTAMP", false, false);
	}

	@Override
	public Object convertValue(Object value) {
		if (value == null) return null;
		if (value instanceof java.time.OffsetDateTime odt) return odt;
		if (value instanceof java.time.LocalDateTime ldt) return ldt;
		if (value instanceof java.time.Instant i) return java.time.OffsetDateTime.ofInstant(i, ZoneOffset.UTC);
		if (value instanceof java.util.Date d) return java.time.OffsetDateTime.ofInstant(d.toInstant(), ZoneOffset.UTC);
		if (value instanceof String s) {
			try { return java.time.OffsetDateTime.parse(s); }
			catch (Exception ignored) { return java.time.LocalDateTime.parse(s); }
		}
		return value;
	}
}



