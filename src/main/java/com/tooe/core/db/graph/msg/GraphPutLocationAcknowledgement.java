package com.tooe.core.db.graph.msg;

import com.tooe.core.domain.LocationId;

public class GraphPutLocationAcknowledgement extends GraphLocation {

	private static final long serialVersionUID = 8832158740616257732L;

	public GraphPutLocationAcknowledgement(LocationId locationId) {
		super(locationId);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof GraphPutLocationAcknowledgement)) {
			return false;
		}
		GraphPutLocationAcknowledgement that = (GraphPutLocationAcknowledgement) obj;
		return this.getLocationId().equals(that.getLocationId());
	}
}
