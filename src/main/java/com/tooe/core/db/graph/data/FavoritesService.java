package com.tooe.core.db.graph.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tinkerpop.blueprints.Query;
import com.tinkerpop.blueprints.Vertex;
import com.tooe.core.db.graph.GraphException;
import com.tooe.core.db.graph.domain.Favorite;
import com.tooe.core.db.graph.domain.FriendshipType;
import com.tooe.core.db.graph.domain.LocationProps;
import com.tooe.core.db.graph.domain.LocationRelationType;
import com.tooe.core.domain.LocationId;
import com.tooe.core.domain.UserId;

@Service
public class FavoritesService {

	@Autowired
	private GraphRepository grepo;

	// -- put favorite --
	public Favorite putFavorite(UserId userId, LocationId locationId) throws GraphException {
    
	  grepo.clean();    

    Vertex user = grepo.getUser(userId);
		Vertex location = grepo.getLocation(locationId);
		if (user == null) {
			throw new GraphException("User not found.");
		}
		if (location == null) {
			throw new GraphException("Location not found.");
		}
		return putFavorite(user, location);
	}

	private Favorite putFavorite(Vertex user, Vertex location) throws GraphException {
		Favorite favorite = new Favorite(user,location);
		if(!grepo.isFavorite(user, location)){
			grepo.performPutFavorite(user, location);
		} else {
			throw new GraphException("is already a favorite location.");
		}
		return favorite;
	}

	// -- delete favorite --
	public void deleteFavorite(UserId userId, LocationId locationId)
			throws GraphException {

	  grepo.clean();    
		
	  Vertex user = grepo.getUser(userId);
		Vertex location = grepo.getLocation(locationId);
		if (user == null) {
			throw new GraphException("User not found.");
		}
		if (location == null) {
			throw new GraphException("Location not found.");
		}
		grepo.performDeleteFavorite(user, location);
	}

	// -- search favorites --
	public List<LocationId> getFavorites(UserId userId) throws GraphException {

	  grepo.clean();    
		
	  List<LocationId> locationsIds = new ArrayList<LocationId>();
		Vertex user = grepo.getUser(userId);
		if (user != null) {
			Query query = user.query().labels(LocationRelationType.FAVORITE.name());
			for (Vertex location : query.vertices()) {
				locationsIds.add(new LocationId(new ObjectId(location.getProperty(LocationProps.OBJECTID.name()).toString())));
			}
		}
		return locationsIds;
	}

	public boolean isFavorite(UserId userId, LocationId locationId) throws GraphException {

	  grepo.clean();    
		
	  Vertex user = grepo.getUser(userId);
		Vertex location = grepo.getLocation(locationId);
		if (user == null) {
			throw new GraphException("User not found.");
		}
		if (location == null) {
			throw new GraphException("Location not found.");
		}
		return grepo.isFavorite(user, location);
	}

	//TODO: change to graph traversing
	public List<LocationId> getFriendsFavorites(UserId userId) throws GraphException {

	  grepo.clean();    
		
	  Set<LocationId> locationsIds = new HashSet<LocationId>();
		Vertex user = grepo.getUser(userId);
		if (user != null) {
			Query query = user.query().labels(FriendshipType.FRIEND.name());
			for (Vertex users : query.vertices()) {
				List<LocationId> frieandLocations = getFavorites(new UserId(new ObjectId(users.getProperty(LocationProps.OBJECTID.name()).toString())));
				locationsIds.addAll(frieandLocations);
			}
		}
		return new ArrayList<LocationId>(locationsIds);
	}	
		
}
