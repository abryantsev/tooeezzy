package com.tooe.core.db.graph.msg;

import java.io.Serializable;

import com.tooe.core.domain.UserId;

public class GraphDeleteUser implements Serializable {

	private static final long serialVersionUID = -5474371361355606645L;

	protected final UserId userId;
	protected final boolean acknowledge;

	public GraphDeleteUser(UserId userId) {
		this.userId = userId;
		this.acknowledge = false;
	}

	public GraphDeleteUser(UserId userId, boolean acknowledge) {
		this.userId = userId;
		this.acknowledge = acknowledge;
	}
	
	public UserId getUserId() {
		return userId;
	}

	public boolean getAcknowledge() {
		return acknowledge;
	}
	
}
