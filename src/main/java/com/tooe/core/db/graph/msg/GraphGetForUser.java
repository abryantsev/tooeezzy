package com.tooe.core.db.graph.msg;

import java.io.Serializable;

import com.tooe.core.domain.UserId;

public abstract class GraphGetForUser implements Serializable {

	private static final long serialVersionUID = 4913798143808684890L;
	
	protected final UserId userId;

	protected GraphGetForUser(UserId userId) {
		this.userId = userId;
	}

	public UserId getUserId() {
		return userId;
	}
	
	public abstract String toString();
	
}
