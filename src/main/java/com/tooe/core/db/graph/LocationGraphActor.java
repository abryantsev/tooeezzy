package com.tooe.core.db.graph;

import static akka.dispatch.Futures.future;
import static akka.pattern.Patterns.pipe;

import java.util.concurrent.Callable;

import com.tooe.core.application.Actors$;
import com.tooe.core.domain.LocationId;
import scala.Symbol;
import scala.concurrent.Future;

import com.tooe.core.db.graph.msg.GraphPutLocationAcknowledgement;

public class LocationGraphActor extends GraphActor {

    final static public Symbol Id = Actors$.MODULE$.LocationGraph();

    public interface Message{}

    @Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof GraphPutLocation) {
			final GraphPutLocation msg = (GraphPutLocation) message;

			Future<GraphPutLocationAcknowledgement> putlocationIdFuture = future(
					new Callable<GraphPutLocationAcknowledgement>() {
						public GraphPutLocationAcknowledgement call() throws GraphException {
							LocationId locationId = msg.getLocationId();
							return new GraphPutLocationAcknowledgement(getGraphEntitiesService().putLocation(locationId));
						}
					}, getDispatcher());
			pipe(putlocationIdFuture, getDispatcher()).to(getSender());

		} else {
			if (message instanceof GraphDeleteLocation) {
				final GraphDeleteLocation msg = (GraphDeleteLocation) message;
				Future<Boolean> deleteFavoriteFuture = future(new Callable<Boolean>() {
					public Boolean call() throws GraphException {
						getGraphEntitiesService().deleteLocation(msg.getLocationId());
						return true;
					}
				}, getDispatcher());
				pipe(deleteFavoriteFuture, getDispatcher()).to(getSender());
			}
		}
	}
}
