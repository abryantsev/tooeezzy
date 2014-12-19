package com.tooe.core.db.graph.msg;

import com.tooe.core.domain.UserId;

public class GraphCheckFriends extends GraphUsersTupel {

	private static final long serialVersionUID = -1808221761045196622L;

	public GraphCheckFriends(UserId userIdA, UserId userIdB) {
		super(userIdA, userIdB);
	}

}
