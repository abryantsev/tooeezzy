package com.tooe.core.db.graph;

import com.tooe.core.application.Actors$;
import com.tooe.core.db.graph.msg.GraphDeleteUser;
import com.tooe.core.db.graph.msg.GraphPutUser;
import com.tooe.core.db.graph.msg.GraphPutUserAcknowledgement;
import com.tooe.core.domain.UserId;
import scala.Symbol;
import scala.concurrent.Future;

import java.util.concurrent.Callable;

import static akka.dispatch.Futures.future;
import static akka.pattern.Patterns.pipe;

public class UserGraphActor extends GraphActor {

  public static final Symbol Id = Actors$.MODULE$.UserGraph();

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof GraphPutUser) {
			final GraphPutUser msg = (GraphPutUser) message;

			Future<GraphPutUserAcknowledgement> putUserFuture = future(new Callable<GraphPutUserAcknowledgement>() {
				public GraphPutUserAcknowledgement call() throws GraphException {
						UserId userId = msg.getUserId();
						return new GraphPutUserAcknowledgement(getGraphEntitiesService().putUser(userId));
				}
			}, getDispatcher());
			pipe(putUserFuture, getDispatcher()).to(getSender());
			
		} else {
			if (message instanceof GraphDeleteUser) {
					final GraphDeleteUser msg = (GraphDeleteUser) message;
					Future<Boolean> deleteUserFuture = future(new Callable<Boolean>() {
						public Boolean call() throws GraphException {
								getGraphEntitiesService().deleteUser(msg.getUserId());
								return true;
						}
					}, getDispatcher());
					pipe(deleteUserFuture, getDispatcher()).to(getSender());				
			}
		}
	}
}
