package com.tooe.core.db.graph.msg;

import com.tooe.core.domain.UserId;

public class GraphPutFriends extends GraphUsersTupel {

	private static final long serialVersionUID = -786754626889501111L;

	public GraphPutFriends(UserId userIdA, UserId userIdB) {
		super(userIdA, userIdB);
	}	
}
