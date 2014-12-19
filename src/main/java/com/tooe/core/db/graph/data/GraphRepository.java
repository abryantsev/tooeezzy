package com.tooe.core.db.graph.data;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.TitanTransaction;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.VertexQuery;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;
import com.tooe.core.db.graph.GraphException;
import com.tooe.core.db.graph.domain.Favorite;
import com.tooe.core.db.graph.domain.FavoriteProps;
import com.tooe.core.db.graph.domain.Friendship;
import com.tooe.core.db.graph.domain.FriendshipProps;
import com.tooe.core.db.graph.domain.FriendshipType;
import com.tooe.core.db.graph.domain.LocationProps;
import com.tooe.core.db.graph.domain.LocationRelationType;
import com.tooe.core.db.graph.domain.UserProps;
import com.tooe.core.db.graph.domain.VertexType;
import com.tooe.core.domain.LocationId;
import com.tooe.core.domain.UserId;

@Service
public class GraphRepository {

  final static Logger logger = LoggerFactory.getLogger(GraphRepository.class);
  
	@Autowired
	private TitanGraph graph;

	public TitanGraph getGraph() {
		return graph;
	}

	public void shutdownGraph() {
		graph.shutdown();
	}

	public TitanTransaction getTitanTransaction() {
		return graph.newTransaction();
	}
	
	// -- location --
	public Vertex putLocation(LocationId locationId) {
		Vertex location = graph.addVertex(null);
		location.setProperty(LocationProps.TYPE.name(), VertexType.LOCATION.name());
		location.setProperty(LocationProps.OBJECTID.name(), locationId.id().toString());

		graph.commit();
		return location;
	}

	public Vertex getLocation(LocationId locationId) {
		Iterator<Vertex> locationVerticesIterator = graph.getVertices(
				LocationProps.OBJECTID.name(), locationId.id().toString()).iterator();
		return locationVerticesIterator.hasNext() ? locationVerticesIterator.next() : null;
	}

	public void deleteLocation(LocationId locationId) throws GraphException {
		Vertex location = getLocation(locationId);
		if (location == null) {
			throw new GraphException("Location not found.");
		}
		Iterable<Edge> relations = location.query().edges();
		for(Edge e : relations) {
			e.remove();
		}
		location.remove();
		
		graph.commit();	
 }	
	

	// -- user --
	public Vertex putUser(UserId userId) {
		Vertex user = graph.addVertex(null);
		user.setProperty(UserProps.TYPE.name(), VertexType.USER.name());
		user.setProperty(UserProps.OBJECTID.name(), userId.id().toString());

		graph.commit();
		return user;
	}

	public Vertex getUser(UserId userId) {
		Iterator<Vertex> userVerticesIterator = graph.getVertices(
				UserProps.OBJECTID.name(), userId.id().toString()).iterator();
		return userVerticesIterator.hasNext() ? userVerticesIterator.next() : null;
	}

	public void deleteUser(UserId userId) throws GraphException {
		Vertex user = getUser(userId);
		if (user == null) {
			throw new GraphException("User not found.");
		}
		Iterable<Edge> relations = user.query().edges();
		for(Edge e : relations) {
			e.remove();
		}
		user.remove();
		
		graph.commit();
	}
	
	// -- put friendship --
	public Friendship putFriendship(Vertex userA, Vertex userB) throws GraphException {
		createFriendship(userA, userB);
		return new Friendship(userA, userB);
//		return getFriendship(userA, userB);
	}

	private void createFriendship(Vertex userA, Vertex userB) {
//		Edge edgeAB = graph.addEdge(null, userA, userB,	FriendshipType.FRIEND.name());
//		edgeAB.setProperty(FriendshipProps.FRIENDID.name(),
//				userB.getProperty(UserProps.OBJECTID.name()));
		
		userA.addEdge(FriendshipType.FRIEND.name(), userB).setProperty(FriendshipProps.FRIENDID.name(), userB.getProperty(UserProps.OBJECTID.name()));
		
//		Edge edgeBA = graph.addEdge(null, userB, userA,	FriendshipType.FRIEND.name());
//		edgeBA.setProperty(FriendshipProps.FRIENDID.name(),
//				userA.getProperty(UserProps.OBJECTID.name()));

		userB.addEdge(FriendshipType.FRIEND.name(), userA).setProperty(FriendshipProps.FRIENDID.name(), userA.getProperty(UserProps.OBJECTID.name()));

		
		graph.commit();

    logger.debug("Friendship added between users: userA - "+userA.toString()+", userB - "+userB.toString());

	}
					
//	public Friendship putFriendship(Vertex userA, Vertex userB, FriendshipType... ftypes) throws GraphException {
//		Friendship friendship = new Friendship(userA, userB);
//		for(FriendshipType ftype : ftypes){
//			if(!haveValidFriendship(userA, userB, ftype)){
//				performPutFriendship(userA, userB, ftype);
//			} else {
//				throw new GraphException("friendship already exists.");
//			}
//			if(!ftype.equals(FriendshipType.FRIEND)) {
//				friendship.addUsergroup(ftype);
//			}
//		}
//		
//		List<FriendshipType> uValues = ListUtils.removeAll(Arrays.asList(ftypes), Arrays.asList(FriendshipType.FRIEND));
//		if(friendship.getUsergroups().size() != uValues.size()) {
//			throw new GraphException("Friendship is not complete.");
//		}
//		
//		graph.commit();
//		
//		return friendship;
//	}

//	private void removeUsergroup(Vertex userA, Vertex userB, FriendshipType ftype) throws GraphException {
//		Edge singleEdgeAB = getSingleEdge(userA, ftype.name(),
//				Direction.OUT, FriendshipProps.FRIENDID.name(),
//				userB.getProperty(UserProps.OBJECTID.name()).toString());
////		Edge singleEdgeBA = getSingleEdge(userB, ftype.name(),
////				Direction.OUT, FriendshipProps.FRIENDID.name(),
////				userA.getProperty(UserProps.OBJECTID.name()).toString());
//		
////		if (singleEdgeAB == null || singleEdgeBA == null) {
//		if (singleEdgeAB == null) {
//			throw new GraphException("friendship with usergroup "+ftype+" not found.");
//		}
//		
//		graph.removeEdge(singleEdgeAB);
////		graph.removeEdge(singleEdgeBA);
//		graph.commit();
//	}

//	private void setUsergroup(Vertex userA, Vertex userB, FriendshipType ftype) {
//		Edge edgeAB = graph.addEdge(null, userA, userB,	ftype.name());
//		edgeAB.setProperty(FriendshipProps.FRIENDID.name(),
//				userB.getProperty(UserProps.OBJECTID.name()));
//		
////		Edge edgeBA = graph.addEdge(null, userB, userA,	ftype.name());
////		edgeBA.setProperty(FriendshipProps.FRIENDID.name(),
////				userA.getProperty(UserProps.OBJECTID.name()));
//		graph.commit();
//	}

	private void mergeUsergroups(Vertex userA, Vertex userB, 
			Collection<FriendshipType> ftypesToAdd, Collection<FriendshipType> ftypesToRemove) throws GraphException {
//		System.out.println("start mergeUsergroups... ");
		
//		getUsergroups(userA, userB);
		
//		System.out.println("start remove... ");
		userA = graph.getVertex(userA);
		userB = graph.getVertex(userB);
		
		for(FriendshipType ftype : ftypesToRemove){
			deleteRelation(userA, userB, ftype);
//			Edge singleEdgeAB = getSingleEdge(userA, ftype.name(),
//					Direction.OUT, FriendshipProps.FRIENDID.name(),
//					userB.getProperty(UserProps.OBJECTID.name()).toString());
//			if (singleEdgeAB == null) {
//				throw new GraphException("friendship with usergroup "+ftype+" not found.");
//			}
//			System.out.println("remove.edgeAB: "+singleEdgeAB.getId());
//			graph.removeEdge(singleEdgeAB);
		}

//		System.out.println("start add... ");

		for(FriendshipType ftype : ftypesToAdd){
			Edge edgeAB = graph.addEdge(null, userA, userB,	ftype.name());
			edgeAB.setProperty(FriendshipProps.FRIENDID.name(),
					userB.getProperty(UserProps.OBJECTID.name()));
//			System.out.println("add.edgeAB: "+edgeAB.getId());
		}
		
		graph.commit();

//		System.out.println("start commit... ");
	}
			
	public Friendship setUsergroups(Vertex userA, Vertex userB, Collection<FriendshipType> oldUsergroups, FriendshipType... usergroups) throws GraphException {
//		Vertex userA = friendship.getUserA();
//		Vertex userB = friendship.getUserB();
		
//		System.out.println("userA: "+userA);
//		System.out.println("userB: "+userB);
//		System.out.println("has: "+Arrays.toString(friendship.getUsergroups().toArray()));
//		logFriendship("befor setUsergroups: ", userA, userB);
//		System.out.println("set: "+Arrays.toString(usergroups));
		Collection<FriendshipType> relationstoRemove = CollectionUtils.subtract(oldUsergroups, Arrays.asList(usergroups));
//		relationstoRemove.remove(FriendshipType.FRIEND);
		Collection<FriendshipType> relationsToAdd = CollectionUtils.subtract(Arrays.asList(usergroups), oldUsergroups);
//		relationsToAdd.remove(FriendshipType.FRIEND);
//		System.out.println("remove: "+Arrays.toString(relationstoRemove.toArray()));
//		System.out.println("add: "+Arrays.toString(relationsToAdd.toArray()));
		
		mergeUsergroups(userA, userB, relationsToAdd, relationstoRemove);

//		logFriendship("after setUsergroups: ", userA, userB);
//		return getFriendship(userA, userB);
    Set<FriendshipType> ftypes = new HashSet<FriendshipType> ();
    ftypes.addAll(Arrays.asList(usergroups));
    return  new Friendship(userA, userB, ftypes);
	}

	private void logFriendship(String msg, Vertex userA, Vertex userB)
			throws GraphException {
		Friendship friendship = getFriendship(userA, userB);
		if(friendship == null){
			System.out.println(msg+" --ERROR -- NO Friendship!!!");			
		} else {
			System.out.println(msg+" --usergroups: "+Arrays.toString(friendship.getUsergroups().toArray())+" --users: "+Arrays.toString(friendship.getFriendsIds().toArray()));
		}
	}

	// -- delete friendship --
	//without commit!!
	private void deleteRelation(Vertex userA, Vertex userB, FriendshipType ftype) {
		Iterable<Edge> relationsAB = getAllRelations(userA, userB, ftype);
		for(Edge edge : relationsAB) {
			graph.removeEdge(edge);
		}
	}

	public void deleteFriendship(Vertex userA, Vertex userB)
			throws GraphException {
		
		Iterable<Edge> relationsAB = getAllRelations(userA, userB, FriendshipType.values());
		Iterable<Edge> relationsBA = getAllRelations(userB, userA, FriendshipType.values());
		for(Edge edge : relationsAB) {
		  logger.error("remove edge "+edge);
			graph.removeEdge(edge);
		}
		for(Edge edge : relationsBA) {
		  logger.error("remove edge "+edge);
			graph.removeEdge(edge);
		}
		
//		logFriendship("deleteFriendship.1: ", userA, userB);
//		for(FriendshipType ftype : FriendshipType.values()){
//			Edge singleEdgeAB = getSingleEdge(userA, ftype.name(),
//					Direction.OUT, FriendshipProps.FRIENDID.name(),
//					userB.getProperty(UserProps.OBJECTID.name()).toString());
//			Edge singleEdgeBA = getSingleEdge(userB, ftype.name(),
//					Direction.OUT, FriendshipProps.FRIENDID.name(),
//					userA.getProperty(UserProps.OBJECTID.name()).toString());
//			if(singleEdgeAB != null){
//				graph.removeEdge(singleEdgeAB);
//			}
//			if(singleEdgeBA != null){
//				graph.removeEdge(singleEdgeBA);
//			}
//		}
//		logFriendship("deleteFriendship.2: ", userA, userB);
		graph.commit();
//		logFriendship("deleteFriendship.3: ", userA, userB);
		
//		long date1 = new Date().getTime();
//		for(int i=0; i<100; i++){
//			long date2 = new Date().getTime();
//			if(date2 - date1 > 1000) {
//				logFriendship("deleteFriendship.i: "+i+" -- ", userA, userB);
//				date1 = date2;
//			}
//		}
	}

//	public void deleteFriendship(Vertex userA, Vertex userB, FriendshipType ftype)
//			throws GraphException {
//		performDeleteFriendship(userA, userB, ftype);
//		graph.commit();
//	}

	// -- private --
//	private Edge removeRelation(Vertex vertex, String edgeLabel, Direction direction,
//			String edgeFilterName, String edgeFilterValue) throws GraphException {
//		VertexQuery query = vertex.query().labels(edgeLabel).direction(direction).has(edgeFilterName, edgeFilterValue);
//		long edgescount = query.count();
//		if (edgescount > 1) {
//			System.out.println("****************************BUM! "+vertex.getId() + "...edgeLabel: " + edgeLabel + "... edgeFilter:" + edgeFilterName + "=" + edgeFilterValue);
//			throw new GraphException("Multiple edges found for type "+edgeLabel);
//		}
//		for(Edge e : query.edges()){
//			e.remove();
//		}
//		graph.commit();
//		return edgescount == 0? null : query.edges().iterator().next();
//	}

	private Edge getSingleEdge(Vertex vertex, String edgeLabel, Direction direction,
			String edgeFilterName, String edgeFilterValue) throws GraphException {
		vertex = graph.getVertex(vertex);
		VertexQuery query = vertex.query().labels(edgeLabel).direction(direction).has(edgeFilterName, edgeFilterValue);
		long edgescount = query.count();
		if (edgescount > 1) {
//			System.out.println("****************************BUM! "+vertex.getId() + "...edgeLabel: " + edgeLabel + "... edgeFilter:" + edgeFilterName + "=" + edgeFilterValue);
//			throw new GraphException("Multiple edges found for type "+edgeLabel);
		  logger.error("Multiple edges found for type "+edgeLabel);
		}
		return edgescount == 0? null : query.edges().iterator().next();
	}

	private Iterable<Edge> getAllRelations(Vertex vertexA, Vertex vertexB, FriendshipType... ftypes) {
		VertexQuery queryAB = vertexA.query().labels(FriendshipType.getUsergroupNames(ftypes)).direction(Direction.OUT).
				has(FriendshipProps.FRIENDID.name(), vertexB.getProperty(UserProps.OBJECTID.name()).toString());
	  logger.debug("getAllRelations("+Arrays.asList(ftypes).toString()+"): "+ queryAB.count());
		return queryAB.edges();
	}	
		
	public boolean haveValidFriendship(Vertex vertexA, Vertex vertexB) throws GraphException {
		VertexQuery queryAB = vertexA.query().labels(FriendshipType.FRIEND.name()).direction(Direction.OUT).
				has(FriendshipProps.FRIENDID.name(), vertexB.getProperty(UserProps.OBJECTID.name()).toString());
		VertexQuery queryBA = vertexB.query().labels(FriendshipType.FRIEND.name()).direction(Direction.OUT).
				has(FriendshipProps.FRIENDID.name(), vertexA.getProperty(UserProps.OBJECTID.name()).toString());
		long edgesABcount = queryAB.count();
		long edgesBAcount = queryBA.count();
		if(edgesABcount != edgesBAcount || edgesABcount > 1 || edgesBAcount > 1) {
		  logger.error("Invalid friendship relation: vertexA: "+vertexA.toString()+" - vertexB: "+vertexB.toString());
			throw new GraphException("Invalid friendship relation.");
		}
		return edgesABcount == 1;
	}	

//	private boolean haveUsergroup(Vertex vertexA, Vertex vertexB, FriendshipType ftype) throws GraphException {
//		VertexQuery queryAB = vertexA.query().labels(ftype.name()).direction(Direction.OUT).
//				has(FriendshipProps.FRIENDID.name(), vertexB.getProperty(UserProps.OBJECTID.name()).toString());
//		long edgesABcount = queryAB.count();
////		System.out.println("ftype: "+ftype+" - count: "+edgesABcount);
//		if(edgesABcount > 1) {
//			throw new GraphException("Invalid friendship relation for tiype "+ftype);
//		}
//		return edgesABcount == 1;
//	}	

	public Set<FriendshipType> getUsergroups(Vertex vertexA, Vertex vertexB) throws GraphException {
		Set<FriendshipType> ftypes = new HashSet<FriendshipType> ();
		Iterable<Edge> edges = getAllRelations(vertexA, vertexB, FriendshipType.usergroupValues());
		for(Edge e: edges){
			ftypes.add(FriendshipType.getFriendshipTypes(e.getLabel())[0]);
//			System.out.println("getUsergroups: "+ e.getId() + " -- " + e.toString());
		}
		return ftypes;
	}

	public Friendship getFriendship(Vertex userA, Vertex userB)	throws GraphException {
		if(!haveValidFriendship(userA, userB)){
			return null;
		}
//		Set<FriendshipType> ftypes = new HashSet<FriendshipType>();
//		for(FriendshipType ftype : FriendshipType.usergroupValues()){
//			if(haveUsergroup(userA, userB, ftype)) {
//				ftypes.add(ftype);
//			}
//		}
		return new Friendship(userA, userB, getUsergroups(userA, userB));
	}	

//	private List<FriendshipType> getFriendshipTypes(String... usergroups)
//			throws GraphException {
//		FriendshipType[] friendshipTypes;
//		try {
//			friendshipTypes = FriendshipType.getFriendshipTypes(usergroups);
//			if(friendshipTypes == null || friendshipTypes.length != usergroups.length){
//				throw new GraphException("invalid usergroups.");
//			}
//		} catch (IllegalArgumentException e) {
//			throw new GraphException("invalid usergroups.");
//		}
//		return Arrays.asList(friendshipTypes);
//	}

	public Collection<Vertex> getMutualFriends(Vertex userA, Vertex userB) {
		final String userBId = userB.getProperty(UserProps.OBJECTID.name()).toString();
		
		GremlinPipeline<Vertex, Vertex> pipe = new GremlinPipeline<Vertex, Vertex>(userA)
				.out(FriendshipType.FRIEND.name()).dedup().filter(new PipeFunction<Vertex, Boolean>() {
                    public Boolean compute(Vertex friend) {
                        return new GremlinPipeline<Vertex, Boolean>(friend).outE(FriendshipType.FRIEND.name())
                                .has(FriendshipProps.FRIENDID.name(), userBId).count() > 0;
                    }
                });
        return pipe.toList();
	}
	
	public void performPutFavorite(Vertex user, Vertex location) {
		Edge edgeFavorite = graph.addEdge(null, user, location,	LocationRelationType.FAVORITE.name());
		edgeFavorite.setProperty(FavoriteProps.LOCATIONID.name(),
				location.getProperty(LocationProps.OBJECTID.name()));		
		graph.commit();
	}

	public void performDeleteFavorite(Vertex user, Vertex location) throws GraphException {
		Edge singleEdgeFavorite = getSingleEdge(user, LocationRelationType.FAVORITE.name(),
				Direction.OUT, FavoriteProps.LOCATIONID.name(),
				location.getProperty(LocationProps.OBJECTID.name()).toString());
		
		if (singleEdgeFavorite == null) {
			throw new GraphException("favorite location not found.");
		}
		
		graph.removeEdge(singleEdgeFavorite);
		graph.commit();
	}
	
	public Favorite getFavorite(Vertex user, Vertex location)	throws GraphException {
		if(!isFavorite(user, location)){
			return null;
		}
		return new Favorite(user, location);
	}	
	
	public boolean isFavorite(Vertex user, Vertex location) throws GraphException {
		LocationId locationID = new LocationId(new ObjectId(location.getProperty(LocationProps.OBJECTID.name()).toString()));
		VertexQuery queryFavorite = user.query().labels(LocationRelationType.FAVORITE.name()).direction(Direction.OUT).
				has(FavoriteProps.LOCATIONID.name(), locationID.id().toString());
		long edgesFavoritecount = queryFavorite.count();
		if(edgesFavoritecount > 1) {
			throw new GraphException("Invalid favorite relation.");
		}
		return edgesFavoritecount == 1;
	}	


	public void clean() {
	  graph.rollback();
	}
	
	
}
