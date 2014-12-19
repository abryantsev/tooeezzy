package com.tooe.core.db.graph.msg;

import com.tooe.core.domain.UserId;

public class GraphDeleteFriends extends GraphUsersTupel {

	private static final long serialVersionUID = -2701800313436579068L;

	public GraphDeleteFriends(UserId userIdA, UserId userIdB) {
		super(userIdA, userIdB);
	}	
}
