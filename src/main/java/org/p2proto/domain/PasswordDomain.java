package org.p2proto.domain;

public final class PasswordDomain extends BaseDomain {
	public static final PasswordDomain INSTANCE = new PasswordDomain();

	public PasswordDomain() {
		super(9, "PASSWORD", "label.domain.password", "VARCHAR(255)", false, false);
	}
}


