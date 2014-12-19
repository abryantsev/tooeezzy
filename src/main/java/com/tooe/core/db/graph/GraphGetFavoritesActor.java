package com.tooe.core.db.graph;

import java.util.List;
import java.util.concurrent.Callable;

import com.tooe.core.application.Actors$;
import com.tooe.core.domain.LocationId;
import scala.Symbol;
import scala.concurrent.Future;

import static akka.dispatch.Futures.future;
import static akka.pattern.Patterns.pipe;
import com.tooe.core.db.graph.msg.GraphLocations;

public class GraphGetFavoritesActor extends GraphActor {

    final static public Symbol Id = Actors$.MODULE$.GetFavoriteLocationsGraph();

    @Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof GraphGetFavoriteLocations) {
			final GraphGetFavoriteLocations msg = (GraphGetFavoriteLocations) message;
			Future<GraphLocations> getFavoritesFuture = future(new Callable<GraphLocations>() {
				public GraphLocations call() throws GraphException {
						List<LocationId> favorites = getFavoritesService().getFavorites(msg.getUserId());
						return new GraphLocations(favorites);
				}
			}, getDispatcher());
			pipe(getFavoritesFuture, getDispatcher()).to(getSender());		
			
		} else if(message instanceof IsFavoriteLocation) {
            final IsFavoriteLocation msg = (IsFavoriteLocation) message;
            Future<Boolean> isFavoriteFuture = future(new Callable<Boolean>() {
                public Boolean call() throws GraphException {
                    return getFavoritesService().isFavorite(msg.getUserId(), msg.getLocationId());
                }
            }, getDispatcher());
            pipe(isFavoriteFuture, getDispatcher()).to(getSender());
    } else if (message instanceof GraphGetFriendsFavoriteLocations) {
			final GraphGetFriendsFavoriteLocations msg = (GraphGetFriendsFavoriteLocations) message;
			Future<GraphLocations> getFavoritesFuture = future(new Callable<GraphLocations>() {
				public GraphLocations call() throws GraphException {
						List<LocationId> favorites = getFavoritesService().getFriendsFavorites(msg.getUserId());
						return new GraphLocations(favorites);
				}
			}, getDispatcher());
			pipe(getFavoritesFuture, getDispatcher()).to(getSender());		
			
		}
	}

}
