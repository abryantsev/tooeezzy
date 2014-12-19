package com.tooe.core.db.graph.msg;

import java.io.Serializable;

import com.tooe.core.domain.UserId;

public class GraphPutUser implements Serializable {

	private static final long serialVersionUID = 8920781666606733275L;

	protected final UserId userId;

	public GraphPutUser(UserId userId) {
		this.userId = userId;
	}

	public UserId getUserId() {
		return userId;
	}

}
