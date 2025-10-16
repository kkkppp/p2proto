package org.p2proto.domain;

public final class UuidDomain extends BaseDomain {
	public static final UuidDomain INSTANCE = new UuidDomain();

	private UuidDomain() {
		super(1, "UUID", "label.domain.uuid", "UUID", false, false);
	}

	@Override
	public String wherePredicate(String columnName) {
		return columnName + " = ?::uuid";
	}
}


