package org.p2proto.domain;

public final class BooleanDomain extends BaseDomain {
	public static final BooleanDomain INSTANCE = new BooleanDomain();

	public BooleanDomain() {
		super(3, "BOOLEAN", "label.domain.boolean", "BOOLEAN", false, false);
	}
}


