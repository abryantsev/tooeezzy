package com.tooe.core.db.graph.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tinkerpop.blueprints.Query;
import com.tinkerpop.blueprints.Vertex;
import com.tooe.core.db.graph.GraphException;
import com.tooe.core.db.graph.domain.Friendship;
import com.tooe.core.db.graph.domain.FriendshipType;
import com.tooe.core.db.graph.domain.UserProps;
import com.tooe.core.domain.UserId;

@Service
public class FriendshipService {

  Logger logger = LoggerFactory.getLogger(FriendshipService.class);
  
	@Autowired
	private GraphRepository grepo;

	public GraphRepository getGraphRepository() {
		return grepo;
	}

	// -- put friendship --
//	public Friendship putFriendship(Vertex userA, Vertex userB)
//			throws GraphException {
//		return grepo.putFriendship(userA, userB, FriendshipType.FRIEND);
//	}

//	public Friendship putFriendship(UserId userIdA, UserId userIdB, String... friendshipTypes) throws GraphException {
//		FriendshipType[] ftypes = new FriendshipType[friendshipTypes.length];
//		for(int i=0; i<friendshipTypes.length; i++){
//			ftypes[i] = FriendshipType.valueOf(friendshipTypes[i]);
//		}
//		return putFriendship(userIdA, userIdB, ftypes);
//	}

	public Friendship putFriendship(UserId userIdA, UserId userIdB)
			throws GraphException {
    
	  grepo.clean();    

    Vertex userA = grepo.getUser(userIdA);
		Vertex userB = grepo.getUser(userIdB);
		if (userA == null || userB == null) {
			throw new GraphException("User not found.");
		}
		return grepo.putFriendship(userA, userB);
	}

	public Friendship setUsergroups(UserId userIdA, UserId userIdB, FriendshipType... usergroups) throws GraphException {
	  
	  grepo.clean();
	  
    Vertex userA = grepo.getUser(userIdA);
    Vertex userB = grepo.getUser(userIdB);
    if(userA == null || userB == null) { 
      throw new GraphException("User not found.");
    }
    Set<FriendshipType> oldUsergroups = grepo.getUsergroups(userA, userB);
    
//    Friendship friendship = getFriendship(userIdA, userIdB);
//		if(friendship == null) {
//		  logger.error("Frendship relation not found. Eventual consistency issue - see CAP theorem");
//		}
		
//		List<FriendshipType> newUsergroups;
//		if(usergroups == null || usergroups.length == 0) {
//			newUsergroups = Arrays.asList(FriendshipType.FRIEND);
//		} else {
//			newUsergroups = Arrays.asList(usergroups);
//		}
//		
		return grepo.setUsergroups(userA, userB, oldUsergroups, usergroups);
	}
	
	// -- delete friendship --
	public void deleteFriendship(UserId userIdA, UserId userIdB)
			throws GraphException {
    
	  grepo.clean();    

    Vertex userA = grepo.getUser(userIdA);
		Vertex userB = grepo.getUser(userIdB);
		if (userA == null || userB == null) {
			throw new GraphException("User not found.");
		}
		grepo.deleteFriendship(userA, userB);
	}

//	private void deleteFriendship(Vertex userA, Vertex userB, String friendshipType) throws GraphException {
//		grepo.deleteFriendship(userA, userB, FriendshipType.valueOf(friendshipType));
//	}
	
	// -- get friendship --
	public boolean haveFriendship(UserId userIdA, UserId userIdB) throws GraphException {

	  grepo.clean();    
		
	  Vertex userA = grepo.getUser(userIdA);
		Vertex userB = grepo.getUser(userIdB);
		if (userA == null || userB == null) {
			throw new GraphException("User not found.");
		}
		return grepo.haveValidFriendship(userA, userB);
	}
	
	//TODO: refactor!
	public Friendship getFriendship(UserId userIdA, UserId userIdB) throws GraphException {
	  
	  grepo.clean();	  
	  
		Vertex userA = grepo.getUser(userIdA);
		Vertex userB = grepo.getUser(userIdB);
		if (userA == null || userB == null) {
			throw new GraphException("User not found.");
		}
		return grepo.getFriendship(userA, userB);
	}
	
	// -- search friends --
	public List<UserId> getFriends(UserId userId, FriendshipType ftype) throws GraphException {
	  
    grepo.clean();    
	  
		List<UserId> friendsIds = new ArrayList<UserId>();
		Vertex user = grepo.getUser(userId);
		if (user != null) {
			Query query = user.query().labels(ftype.name());
			for (Vertex friend : query.vertices()) {
				friendsIds.add(new UserId( new ObjectId(friend.getProperty(UserProps.OBJECTID.name()).toString())));
			}
		}
		return friendsIds;
	}	
	
	public Collection<UserId> getMutualFriends(UserId userIdA, UserId userIdB) throws GraphException {

	  grepo.clean();    
		
	  Vertex userA = grepo.getUser(userIdA);
		Vertex userB = grepo.getUser(userIdB);
		if (userA == null || userB == null) {
			throw new GraphException("User not found.");
		}
		List<UserId> friendsIds = new ArrayList<UserId>();
		for (Vertex friend : grepo.getMutualFriends(userA, userB)) {
			friendsIds.add(new UserId( new ObjectId(friend.getProperty(UserProps.OBJECTID.name()).toString())));
		}
		return friendsIds;
	}
	
}
