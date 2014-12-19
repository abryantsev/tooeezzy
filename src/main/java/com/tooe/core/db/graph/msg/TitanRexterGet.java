package com.tooe.core.db.graph.msg;

import java.io.Serializable;

public class TitanRexterGet implements Serializable {

	private static final long serialVersionUID = -4533663653884666466L;

	private final String content;

	public TitanRexterGet(String content) {
		this.content = content;
	}

	public String getContent() {
		return content;
	}

	@Override
	public String toString() {
		return "TitanRexterGet{content=" + content + "}";
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof TitanRexterGet
				&& ((TitanRexterGet) obj).getContent().equals(content);
	}
}
