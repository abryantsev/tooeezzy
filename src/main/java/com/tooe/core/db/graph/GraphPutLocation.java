package com.tooe.core.db.graph;

import com.tooe.core.db.graph.msg.GraphLocation;
import com.tooe.core.domain.LocationId;

public class GraphPutLocation extends GraphLocation implements
		LocationGraphActor.Message {

	private static final long serialVersionUID = 5508532273377234585L;

	public GraphPutLocation(LocationId locationId) {
		super(locationId);
	}
}
