package org.p2proto.domain;

import org.p2proto.dto.ColumnMetaData;

public final class FormulaDomain extends BaseDomain {
	public static final FormulaDomain INSTANCE = new FormulaDomain();
	public static final String FORMULA_KEY = "formuladomain.formulakey";

	public FormulaDomain() {
		super(10, "FORMULA", "label.domain.formula", "TEXT", false, true);
	}

	@Override
	public String selectPredicate(ColumnMetaData meta) {
		return meta.getAdditionalProperties().get(FORMULA_KEY) + " as "+meta.getName();
	}
}



