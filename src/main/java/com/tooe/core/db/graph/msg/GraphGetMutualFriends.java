package com.tooe.core.db.graph.msg;

import com.tooe.core.domain.UserId;

public class GraphGetMutualFriends extends GraphUsersTupel {

	private static final long serialVersionUID = 3278449893683208210L;

	public GraphGetMutualFriends(UserId userIdA, UserId userIdB) {
		super(userIdA, userIdB);
	}

	@Override
	public String toString() {
		return "GraphGetMutualFriends{userIdA=" + userIdA + ", userIdB="
				+ userIdB + "}";
	}
}
