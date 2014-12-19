package com.tooe.core.db.graph.msg;

import com.tooe.core.domain.UserId;

public class GraphSearchLocationsCity extends GraphSearchLocations {

	private static final long serialVersionUID = -1328509412924143968L;

	protected final String cityId;
	
	public GraphSearchLocationsCity(UserId userId, boolean inCheckinLocations,
			boolean byFriends, String category, String name, boolean hasPromotions, String cityId) {
		super(userId, inCheckinLocations, byFriends, category, name, hasPromotions);
		this.cityId = cityId;
	}

	public String getCityId() {
		return cityId;
	}

	@Override
	public String toString() {
		return "GraphSearchLocationsWithCity{userId=" + userId + "}";
	}
}
