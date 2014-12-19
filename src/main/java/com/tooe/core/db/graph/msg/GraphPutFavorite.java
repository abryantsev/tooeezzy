package com.tooe.core.db.graph.msg;

import com.tooe.core.domain.LocationId;
import com.tooe.core.domain.UserId;

public class GraphPutFavorite extends GraphLocation {

	private static final long serialVersionUID = 107750330936034826L;

	protected final UserId userId;
	
	public GraphPutFavorite(UserId userId, LocationId locationId) {
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
		if (!(obj instanceof GraphPutFavorite)) {
			return false;
		}
		GraphPutFavorite that = (GraphPutFavorite) obj;
		return this.getUserId().equals(that.getUserId()) && this.getLocationId().equals(that.getLocationId());		
	}

}
