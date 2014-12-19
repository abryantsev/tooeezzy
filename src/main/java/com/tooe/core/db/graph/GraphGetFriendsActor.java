package com.tooe.core.db.graph;

import java.util.Collection;
import java.util.List;

import com.tooe.core.application.Actors$;
import com.tooe.core.db.graph.msg.GraphFriends;
import com.tooe.core.db.graph.msg.GraphGetFriends;
import com.tooe.core.db.graph.msg.GraphCheckFriends;
import com.tooe.core.db.graph.msg.GraphGetFriendship;
import com.tooe.core.db.graph.msg.GraphFriendship;
import com.tooe.core.db.graph.msg.GraphGetMutualFriends;

import com.tooe.core.domain.UserId;
import com.tooe.core.db.graph.domain.Friendship;
import scala.Symbol;
import scala.concurrent.Future;
import static akka.dispatch.Futures.future;
import static akka.pattern.Patterns.pipe;
import java.util.concurrent.Callable;

public class GraphGetFriendsActor extends GraphActor {

    public static final Symbol Id = Actors$.MODULE$.GetFriendsGraph();

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof GraphGetFriends) {
			final GraphGetFriends getFriendsMsg = (GraphGetFriends) message;		
			
			Future<GraphFriends> getFriendsFuture = future(new Callable<GraphFriends>() {
				public GraphFriends call() throws GraphException {
						List<UserId> friendsIds = getFriendshipService().getFriends(getFriendsMsg.getUserId(),
							getFriendsMsg.getGroup());
						return new GraphFriends(friendsIds);
				}
			}, getDispatcher());
			pipe(getFriendsFuture, getDispatcher()).to(getSender());
		} else {
			if (message instanceof GraphCheckFriends) {
				final GraphCheckFriends checkFriendsMsg = (GraphCheckFriends) message;		
				
				Future<Boolean> checkFriendsFuture = future(new Callable<Boolean>() {
					public Boolean call() throws GraphException {
						Boolean haveFriendship = getFriendshipService().haveFriendship(checkFriendsMsg.getUserIdA(), checkFriendsMsg.getUserIdB());
						return haveFriendship;
					}
				}, getDispatcher());
				pipe(checkFriendsFuture, getDispatcher()).to(getSender());
			} else {
				if (message instanceof GraphGetFriendship) {
					final GraphGetFriendship getFriendshipMsg = (GraphGetFriendship) message;		
	
					Future<GraphFriendship> getFriendshipFuture = future(new Callable<GraphFriendship>() {
						public GraphFriendship call() throws GraphException {
							Friendship friendship = getFriendshipService().getFriendship(getFriendshipMsg.getUserIdA(), getFriendshipMsg.getUserIdB());
							return new GraphFriendship(friendship.getFriendsIds(), friendship.getUsergroups());
						}
					}, getDispatcher());
					pipe(getFriendshipFuture, getDispatcher()).to(getSender());
				} else {
					if (message instanceof GraphGetMutualFriends) {
						final GraphGetMutualFriends getMutualFriendsMsg = (GraphGetMutualFriends) message;		
		
						Future<GraphFriends> getMutualFriendsMsgFuture = future(new Callable<GraphFriends>() {
							public GraphFriends call() throws GraphException {
								Collection<UserId> friends = getFriendshipService().getMutualFriends(getMutualFriendsMsg.getUserIdA(), getMutualFriendsMsg.getUserIdB());
								return new GraphFriends(friends);
							}
						}, getDispatcher());
						pipe(getMutualFriendsMsgFuture, getDispatcher()).to(getSender());					
					}
				}
			}
		}
	}

}
