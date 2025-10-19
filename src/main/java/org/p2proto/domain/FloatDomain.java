package org.p2proto.domain;

public final class FloatDomain extends BaseDomain {
	public static final FloatDomain INSTANCE = new FloatDomain();

	public FloatDomain() {
		super(7, "FLOAT", "label.domain.float", "FLOAT", false, false);
	}
}



