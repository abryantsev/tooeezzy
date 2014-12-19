package com.tooe.core.db.graph.msg;

import java.io.Serializable;

import com.tooe.core.domain.UserId;

public class GraphUsersTupel implements Serializable {

	private static final long serialVersionUID = 7214849839507089020L;
	
	protected final UserId userIdA;
	protected final UserId userIdB;

	public GraphUsersTupel(UserId userIdA, UserId userIdB) {
		this.userIdA = userIdA;
		this.userIdB = userIdB;
	}

	public UserId getUserIdA() {
		return userIdA;
	}
	
	public UserId getUserIdB() {
		return userIdB;
	}
	
}
