package com.tooe.core.db.graph.msg;

import com.tooe.core.db.graph.domain.FriendshipType;
import com.tooe.core.domain.UserId;

public class GraphGetFriends extends GraphGetForUser {

	private static final long serialVersionUID = 7997436405594996653L;
	
	private final FriendshipType group;

	public GraphGetFriends(UserId userId, FriendshipType group) {
		super(userId);
		this.group = group;
	}

    public GraphGetFriends(UserId userId) {
		super(userId);
		this.group = FriendshipType.FRIEND;
	}

	public FriendshipType getGroup() {
		return group;
	}

	@Override
	public String toString() {
		return "GraphGetFriends{userId=" + userId + ", group="
				+ group + "}";
	}
}
