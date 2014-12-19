package com.tooe.core.db.graph.msg;

import com.tooe.core.domain.UserId;

public class GraphSearchLocationsCoords extends GraphSearchLocations {

	private static final long serialVersionUID = 208915844880082059L;

	protected final Float lon;
	protected final Float lat;
	
	public GraphSearchLocationsCoords(UserId userId, boolean inCheckinLocations,
			boolean byFriends, String category, String name, boolean hasPromotions, Float lon, Float lat) {
		super(userId, inCheckinLocations, byFriends, category, name, hasPromotions);
		this.lon = lon;
		this.lat = lat;
	}

	public Float getLon() {
		return lon;
	}

	public Float getLat() {
		return lat;
	}

	@Override
	public String toString() {
		return "GraphSearchLocationsWithCoords{userId=" + userId + "}";
	}
}
