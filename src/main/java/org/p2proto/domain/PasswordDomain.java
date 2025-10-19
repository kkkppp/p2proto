package org.p2proto.domain;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public final class PasswordDomain extends BaseDomain {
	public static final PasswordDomain INSTANCE = new PasswordDomain();
	private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	public PasswordDomain() {
		super(9, "PASSWORD", "label.domain.password", "VARCHAR(255)", false, false);
	}

	@Override
	public Object convertValue(Object value) {
		if (value == null) return null;
		return passwordEncoder.encode(value.toString());
	}
}



