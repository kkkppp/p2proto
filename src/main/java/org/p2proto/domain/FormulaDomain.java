package org.p2proto.domain;

public final class FormulaDomain extends BaseDomain {
	public static final FormulaDomain INSTANCE = new FormulaDomain();

	public FormulaDomain() {
		super(10, "FORMULA", "label.domain.formula", "TEXT", false, true);
	}
}


