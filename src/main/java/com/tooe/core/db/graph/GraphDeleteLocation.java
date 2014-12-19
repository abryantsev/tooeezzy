package com.tooe.core.db.graph;

import com.tooe.core.db.graph.msg.GraphLocation;
import com.tooe.core.domain.LocationId;

public class GraphDeleteLocation extends GraphLocation implements
		LocationGraphActor.Message {

	private static final long serialVersionUID = -3136669306392294289L;

	public GraphDeleteLocation(LocationId locationId) {
		super(locationId);
	}
}
