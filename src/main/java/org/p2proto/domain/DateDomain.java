package org.p2proto.domain;

public final class DateDomain extends BaseDomain {
	public static final DateDomain INSTANCE = new DateDomain();

	public DateDomain() {
		super(4, "DATE", "label.domain.date", "DATE", false, false);
	}
}


