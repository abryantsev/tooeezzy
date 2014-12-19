package com.tooe.core.db.graph.msg;

import java.io.Serializable;

import com.tooe.core.domain.LocationId;

public class GraphLocation implements Serializable {

	private static final long serialVersionUID = -684858508914790258L;

	protected final LocationId locationId;

	public GraphLocation(LocationId locationId) {
		this.locationId = locationId;
	}

	public LocationId getLocationId() {
		return locationId;
	}

}
