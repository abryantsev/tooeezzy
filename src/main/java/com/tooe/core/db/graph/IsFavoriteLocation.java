package com.tooe.core.db.graph;

import com.tooe.core.db.graph.msg.GraphGetFavorites;
import com.tooe.core.domain.LocationId;
import com.tooe.core.domain.UserId;

public class IsFavoriteLocation extends GraphGetFavorites {

	private static final long serialVersionUID = 2345142577111903372L;

	public IsFavoriteLocation(UserId userId, LocationId locationId) {
		super(userId, locationId);
	}

	@Override
	public String toString() {
		return "IsFavoriteLocation{userId=" + userId + ", locationId"
				+ locationId.id() + "}";
	}
}
