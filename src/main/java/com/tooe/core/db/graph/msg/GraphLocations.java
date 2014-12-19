package com.tooe.core.db.graph.msg;

import java.util.Collection;

import com.tooe.core.domain.LocationId;
import org.apache.commons.collections.CollectionUtils;

public class GraphLocations extends GraphIds<LocationId> {

	private static final long serialVersionUID = -3904268614418539095L;


	public GraphLocations(Collection<LocationId> locationIds) {
		super(locationIds);
	}

	public Collection<LocationId> getLocations() {
		return this.ids;
	}
	
	@Override
	public String toString() {
		return "GraphLocations{locationIds=" + ids.toString() + "}";
	}


	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof GraphLocations)) {
			return false;
		}
		GraphLocations that = (GraphLocations) obj;
		return CollectionUtils.isEqualCollection(this.getLocations(), that.getLocations());		
	}
	
}
