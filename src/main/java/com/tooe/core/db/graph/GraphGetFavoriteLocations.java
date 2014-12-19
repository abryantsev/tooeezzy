package com.tooe.core.db.graph;

import com.tooe.core.db.graph.msg.GraphGetForUser;
import com.tooe.core.domain.UserId;

public class GraphGetFavoriteLocations extends GraphGetForUser {

	private static final long serialVersionUID = -1394266878323306763L;

	public GraphGetFavoriteLocations(UserId userId) {
		super(userId);
	}

	@Override
	public String toString() {
		return "GraphGetFavoriteLocations{userId=" + userId + "}";
	}
}
