package org.p2proto.domain;

abstract sealed class BaseDomain implements DomainType permits AutoIncrementDomain, BooleanDomain, DateDomain, DateTimeDomain, FloatDomain, FormulaDomain, IntegerDomain, PasswordDomain, TextDomain, UuidDomain {
	private final int code;
	private final String internalName;
	private final String resourceId;
	private final String liquibaseType;
	private final boolean autoIncrement;
	private final boolean virtual;

	protected BaseDomain(int code, String internalName, String resourceId, String liquibaseType, boolean autoIncrement, boolean virtual) {
		this.code = code;
		this.internalName = internalName;
		this.resourceId = resourceId;
		this.liquibaseType = liquibaseType;
		this.autoIncrement = autoIncrement;
		this.virtual = virtual;
	}

	@Override public int code() { return code; }
	@Override public String getInternalName() { return internalName; }
	@Override public String resourceId() { return resourceId; }
	@Override public String getLiquibaseType() { return liquibaseType; }
	@Override public boolean isAutoIncrement() { return autoIncrement; }
	@Override public boolean isVirtual() { return virtual; }
}



