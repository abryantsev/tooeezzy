package com.tooe.core.db.graph.msg;

import java.io.Serializable;

import com.tooe.core.domain.LocationId;
import com.tooe.core.domain.UserId;

public class GraphDeleteFavorite implements Serializable {

	private static final long serialVersionUID = 7254452542809365565L;

	protected final UserId userId;
	protected final LocationId locationId;

	public GraphDeleteFavorite(UserId userId, LocationId locationId) {
		this.userId = userId;
		this.locationId = locationId;
	}

	public UserId getUserId() {
		return userId;
	}
	
	public LocationId getLocationId() {
		return locationId;
	}
	
}
