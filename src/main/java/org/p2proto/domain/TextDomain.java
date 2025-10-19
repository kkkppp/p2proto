package org.p2proto.domain;

public final class TextDomain extends BaseDomain {
	public static final TextDomain INSTANCE = new TextDomain();

	private TextDomain() {
		super(2, "TEXT", "label.domain.text", "VARCHAR", false, false);
	}
}



