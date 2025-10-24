package org.p2proto.domain;

public final class UuidDomain extends BaseDomain {
	public static final UuidDomain INSTANCE = new UuidDomain();

	private UuidDomain() {
		super(1, "UUID", "label.domain.uuid", "UUID", true, false);
	}

	@Override
	public String wherePredicate(String columnName) {
		return columnName + " = ?::uuid";
	}

	@Override
	public Object convertValue(Object value) {
		if (value == null) return null;
		if (value instanceof java.util.UUID u) return u;
		return java.util.UUID.fromString(value.toString());
	}
}



