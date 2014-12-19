package com.tooe.core.db.graph;

import com.tooe.core.db.graph.msg.GraphGetForUser;
import com.tooe.core.domain.UserId;

public class GraphGetFriendsFavoriteLocations extends GraphGetForUser {

	private static final long serialVersionUID = 1399506639266231964L;

	public GraphGetFriendsFavoriteLocations(UserId userId) {
		super(userId);
	}

	@Override
	public String toString() {
		return "GraphGetFriendsFavoriteLocations{userId=" + userId + "}";
	}
}
