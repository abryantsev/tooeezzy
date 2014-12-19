package com.tooe.core.db.graph.msg;

import java.io.Serializable;

public class TitanGet implements Serializable {

	private static final long serialVersionUID = 3235925886238078L;

	private final String content;

	public TitanGet(String content) {
		this.content = content;
	}

	public String getContent() {
		return content;
	}

	@Override
	public String toString() {
		return "TitanGet{content=" + content + "}";
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof TitanGet
				&& ((TitanGet) obj).getContent().equals(content);
	}
}
