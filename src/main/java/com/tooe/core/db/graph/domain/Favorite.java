package com.tooe.core.db.graph.domain;

import com.tinkerpop.blueprints.Vertex;
import com.tooe.core.domain.LocationId;
import com.tooe.core.domain.UserId;
import org.bson.types.ObjectId;

public class Favorite {

	private Vertex user;

	private Vertex location;
	
	public Favorite(Vertex user, Vertex location) {
		this.user = user;
		this.location = location;
	}
	
	public Vertex getUser() {
		return user;
	}

	public Vertex getLocation() {
		return location;
	}

	public UserId getUserId() {
		return new UserId(new ObjectId(user.getProperty(UserProps.OBJECTID.name()).toString()));
	}	

	public LocationId getLocationId() {
		return new LocationId(new ObjectId(location.getProperty(LocationProps.OBJECTID.name()).toString()));
	}	
}
