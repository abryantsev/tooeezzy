package com.tooe.core.db.graph.msg;

import com.tooe.core.domain.UserId;

public class GraphGetFriendship extends GraphUsersTupel {

	private static final long serialVersionUID = 4583907929688292134L;

	public GraphGetFriendship(UserId userIdA, UserId userIdB) {
		super(userIdA, userIdB);
	}

}
