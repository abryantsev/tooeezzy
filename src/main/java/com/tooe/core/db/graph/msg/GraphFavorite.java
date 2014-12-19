package com.tooe.core.db.graph.msg;

import com.tooe.core.domain.LocationId;
import com.tooe.core.domain.UserId;

public class GraphFavorite extends GraphLocation {

	private static final long serialVersionUID = 7422409648079214020L;

	protected final UserId userId;
	
	public GraphFavorite(UserId userId, LocationId locationId) {
		super(locationId);
		this.userId = userId;
	}

	public UserId getUserId() {
		return this.userId;
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName()+" {userId=" + getUserId() + ", locationId=" + getLocationId() + "}";
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof GraphFavorite)) {
			return false;
		}
		GraphFavorite that = (GraphFavorite) obj;
		return this.getUserId().equals(that.getUserId()) && this.getLocationId().equals(that.getLocationId());		
	}

}
