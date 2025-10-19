package org.p2proto.domain;

public final class AutoIncrementDomain extends BaseDomain {
	public static final AutoIncrementDomain INSTANCE = new AutoIncrementDomain();

	public AutoIncrementDomain() {
		super(8, "AUTOINCREMENT", "label.domain.autoincrement", "INT", true, false);
	}
}



