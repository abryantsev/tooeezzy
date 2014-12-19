package com.tooe.core.db.graph;

import java.util.concurrent.Callable;

import com.tooe.core.application.Actors$;
import scala.Symbol;
import scala.concurrent.Future;
import akka.dispatch.Futures.*;
import static akka.dispatch.Futures.future;
import static akka.pattern.Patterns.pipe;

import com.tooe.core.db.graph.domain.Favorite;
import com.tooe.core.db.graph.msg.GraphDeleteFavorite;
import com.tooe.core.db.graph.msg.GraphFavorite;
import com.tooe.core.db.graph.msg.GraphPutFavorite;

public class GraphPutFavoritesActor extends GraphActor {

    final static public Symbol Id = Actors$.MODULE$.PutLocationToFavoriteGraph();

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof GraphPutFavorite) {
			final GraphPutFavorite msg = (GraphPutFavorite) message;
			Future<GraphFavorite> putFavoriteFuture = future(new Callable<GraphFavorite>() {
				public GraphFavorite call() throws GraphException {
						Favorite favorite = getFavoritesService().putFavorite(msg.getUserId(), msg.getLocationId());
						return new GraphFavorite(favorite.getUserId(), favorite.getLocationId());
				}
			}, getDispatcher());
			pipe(putFavoriteFuture, getDispatcher()).to(getSender());		
			
		} else {
			if (message instanceof GraphDeleteFavorite) {
				final GraphDeleteFavorite msg = (GraphDeleteFavorite) message;
				Future<Boolean> deleteFavoriteFuture = future(new Callable<Boolean>() {
					public Boolean call() throws GraphException {
							getFavoritesService().deleteFavorite(msg.getUserId(), msg.getLocationId());
							return true;
					}
				}, getDispatcher());
				pipe(deleteFavoriteFuture, getDispatcher()).to(getSender());
				
			}			
		}
	}

}
