package org.p2proto.domain;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class DomainFactory {
	private static final Map<Integer, DomainType> BY_CODE = Map.of(
		1, UuidDomain.INSTANCE,
		2, TextDomain.INSTANCE,
		3, BooleanDomain.INSTANCE,
		4, DateDomain.INSTANCE,
		5, DateTimeDomain.INSTANCE,
		6, IntegerDomain.INSTANCE,
		7, FloatDomain.INSTANCE,
		8, AutoIncrementDomain.INSTANCE,
		9, PasswordDomain.INSTANCE,
		10, FormulaDomain.INSTANCE
	);

	private static final Map<String, DomainType> BY_NAME = BY_CODE.values().stream()
		.collect(Collectors.toUnmodifiableMap(d -> d.getInternalName().toUpperCase(), d -> d));

	private DomainFactory() {}

	public static DomainType fromCode(int code) {
		DomainType type = BY_CODE.get(code);
		if (type == null) throw new IllegalArgumentException("Unknown Domain code: " + code);
		return type;
	}

	public static DomainType fromInternalName(String internalName) {
		DomainType type = BY_NAME.get(internalName.toUpperCase());
		if (type == null) throw new IllegalArgumentException("Unknown Domain internalName: " + internalName);
		return type;
	}

	public static List<DomainType> all() {
		return List.copyOf(BY_CODE.values());
	}
}



