package org.p2proto.domain;

public final class IntegerDomain extends BaseDomain {
	public static final IntegerDomain INSTANCE = new IntegerDomain();

	public IntegerDomain() {
		super(6, "INTEGER", "label.domain.integer", "INT", false, false);
	}
}



