package com.tooe.core.db.graph.msg;

import java.io.Serializable;

import com.tooe.core.db.graph.domain.FriendshipType;
import com.tooe.core.domain.UserId;

public class GraphPutUserGroups implements Serializable {

	private static final long serialVersionUID = -6252682020656515301L;

	protected final UserId userIdA;
	protected final UserId userIdB;
	protected final FriendshipType[] usergroups;


	public GraphPutUserGroups(UserId userIdA, UserId userIdB, FriendshipType[] usergroups) {
		this.userIdA = userIdA;
		this.userIdB = userIdB;
		this.usergroups = usergroups;
	}

	public UserId getUserIdA() {
		return userIdA;
	}
	
	public UserId getUserIdB() {
		return userIdB;
	}

	public FriendshipType[] getUsergroups() {
		return usergroups;
	}
	
}
