package org.p2proto.domain;

public final class DateTimeDomain extends BaseDomain {
	public static final DateTimeDomain INSTANCE = new DateTimeDomain();

	public DateTimeDomain() {
		super(5, "DATETIME", "label.domain.datetime", "TIMESTAMP", false, false);
	}
}


