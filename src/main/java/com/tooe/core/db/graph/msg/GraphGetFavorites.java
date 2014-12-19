package com.tooe.core.db.graph.msg;

import com.tooe.core.domain.LocationId;
import com.tooe.core.domain.UserId;

public abstract class GraphGetFavorites extends GraphGetForUser {

	private static final long serialVersionUID = 8964768943902460788L;

	protected final LocationId locationId;

	protected GraphGetFavorites(UserId userId,LocationId id) {
		super(userId);
		this.locationId = id;
	}

    public LocationId getLocationId() {
        return locationId;
    }

	@Override
	public abstract String toString();
}
