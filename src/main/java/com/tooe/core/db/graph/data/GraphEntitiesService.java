package com.tooe.core.db.graph.data;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tinkerpop.blueprints.Vertex;
import com.tooe.core.db.graph.GraphException;
import com.tooe.core.db.graph.domain.LocationProps;
import com.tooe.core.db.graph.domain.UserProps;
import com.tooe.core.domain.LocationId;
import com.tooe.core.domain.UserId;

@Service
public class GraphEntitiesService {

	@Autowired
	private GraphRepository grepo;

	public LocationId putLocation(LocationId locationId) throws GraphException {

	  grepo.clean();    
		
		Vertex location = grepo.getLocation(locationId);
		if (location != null) {
			throw new GraphException("location " + locationId.id()
					+ " already exists");
		}
		location = grepo.putLocation(locationId);
		if (location == null) {
			throw new GraphException("location " + locationId.id()
					+ " could not be created");
		}
		return new LocationId(new ObjectId(location.getProperty(LocationProps.OBJECTID.name()).toString()));
	}

	public void deleteLocation(LocationId locationId) throws GraphException {

	  grepo.clean();    
		
		grepo.deleteLocation(locationId);
	}	
	
	public UserId putUser(UserId userId) throws GraphException {

	  grepo.clean();    
		
		Vertex user = grepo.getUser(userId);
		if(user != null){
			throw new GraphException("user "+userId+" already exists");
		}
		user = grepo.putUser(userId);
		if(user == null){
			throw new GraphException("user "+userId+" could not be created");
		}
		return new UserId(new ObjectId(user.getProperty(UserProps.OBJECTID.name()).toString()));
	}

	public void deleteUser(UserId userId) throws GraphException {

	  grepo.clean();    
		
	  grepo.deleteUser(userId);
	}

}
