package com.tooe.core.db.graph;

import java.util.concurrent.Callable;

import com.tooe.core.application.Actors$;
import com.tooe.core.db.graph.msg.GraphPutUserGroups;
import scala.Symbol;
import scala.concurrent.Future;

import static akka.dispatch.Futures.future;
import static akka.pattern.Patterns.pipe;

import com.tooe.core.db.graph.domain.Friendship;
import com.tooe.core.db.graph.msg.GraphDeleteFriends;
import com.tooe.core.db.graph.msg.GraphFriendship;
import com.tooe.core.db.graph.msg.GraphPutFriends;

public class GraphPutFriendsActor extends GraphActor {

    public static final Symbol Id = Actors$.MODULE$.PutFriendsGraph();

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof GraphPutFriends) {
			final GraphPutFriends msg = (GraphPutFriends) message;
			Future<GraphFriendship> putFriendshipFuture = future(new Callable<GraphFriendship>() {
				public GraphFriendship call() throws GraphException {
						Friendship friendship = getFriendshipService().putFriendship(msg.getUserIdA(), msg.getUserIdB());
						return new GraphFriendship(friendship.getFriendsIds(), friendship.getUsergroups());
				}
			}, getDispatcher());
			pipe(putFriendshipFuture, getDispatcher()).to(getSender());		
			
		} else {
			if (message instanceof GraphDeleteFriends) {
				final GraphDeleteFriends msg = (GraphDeleteFriends) message;
				Future<Boolean> deleteFriendshipFuture = future(new Callable<Boolean>() {
					public Boolean call() throws GraphException {
							getFriendshipService().deleteFriendship(msg.getUserIdA(), msg.getUserIdB());
							return true;
					}
				}, getDispatcher());
				pipe(deleteFriendshipFuture, getDispatcher()).to(getSender());
				
			} else {
				if (message instanceof GraphPutUserGroups) {
					final GraphPutUserGroups msg = (GraphPutUserGroups) message;

					Future<GraphFriendship> putUsergroupsFuture = future(new Callable<GraphFriendship>() {
						public GraphFriendship call() throws GraphException {
								Friendship friendship = getFriendshipService().setUsergroups(msg.getUserIdA(), msg.getUserIdB(), msg.getUsergroups());
								return new GraphFriendship(friendship.getFriendsIds(), friendship.getUsergroups());
						}
					}, getDispatcher());
					pipe(putUsergroupsFuture, getDispatcher()).to(getSender());

				}
			}
		}
	}

	// private List<String> getFavoriteLocations(String userId) {
	// List<String> locationIds = new ArrayList<String>();
	// Vertex user = getUser(userId);
	// if (user != null) {
	// //Query query =
	// user.query().labels(GraphConfig.IN_EDGE_LABEL).has(GraphConfig.IS_FAVORITE_LOCATION_EDGE_PROPERTY,
	// true);
	// Query query = user.query().labels(GraphConfig.IN_EDGE_LABEL);
	// for (Vertex location : query.vertices()) {
	// locationIds.add(location.getProperty(GraphConfig.ID_VERTEX_PROPERTY).toString());
	// }
	// }
	// return locationIds;
	// }

}
