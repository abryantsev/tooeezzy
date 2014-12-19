package com.tooe.core.db.graph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import scala.concurrent.ExecutionContext;
import akka.pattern.Patterns;
import akka.util.Timeout;

import com.tooe.core.db.graph.data.FavoritesService;
import com.tooe.core.db.graph.data.FriendshipService;
import com.tooe.core.db.graph.data.GraphEntitiesService;
import com.tooe.core.db.graph.data.GraphRepository;
import com.tooe.core.infrastructure.LookupBean;
import com.tooe.extensions.scala.Settings;
import com.tooe.extensions.scala.SettingsImpl;

public abstract class GraphActor extends UntypedActor {

	protected final static Logger logger = LoggerFactory.getLogger(GraphActor.class);
	
	//	protected TitanGraph g;
	protected GraphRepository grepo;
	protected FavoritesService favoritesService;
	protected FriendshipService friendshipService;
	protected GraphEntitiesService entitiesService;

	protected GraphRepository getGraphRepository() {
		return grepo;
	}
	
	protected FavoritesService getFavoritesService() {
		return favoritesService;
	}

	protected FriendshipService getFriendshipService() {
		return friendshipService;
	}

	protected GraphEntitiesService getGraphEntitiesService() {
		return entitiesService;
	}

	@Override
	public void preStart() {
		 SettingsImpl settings = (SettingsImpl) Settings.apply(this.getContext().system());
//		SettingsImpl settings = Settings.get(getContext().system());
		String profile = settings.tooeProfile();
		Boolean initGraphService = settings.initGraphService();
		logger.info("starting ..."+this.getClass().getName() + " for profile: " + profile);
		if(initGraphService != null && initGraphService) {
			logger.info("initGraphService ...");
			ActorRef beanLookup = getContext().actorFor("/user/spring/beanLookup");
			// ActorRef beanLookup = getContext().actorOf(new
			// Props(BeanLookupActor.class));
			Timeout timeout = new Timeout(Duration.create(10, "seconds"));
			Future<Object> future = Patterns.ask(beanLookup, new LookupBean(
					GraphRepository.class), timeout);
			Future<Object> future2 = Patterns.ask(beanLookup, new LookupBean(
					FavoritesService.class), timeout);
			Future<Object> future3 = Patterns.ask(beanLookup, new LookupBean(
					FriendshipService.class), timeout);
			Future<Object> future4 = Patterns.ask(beanLookup, new LookupBean(
					GraphEntitiesService.class), timeout);
			try {
				this.grepo = (GraphRepository) Await.result(future,
						timeout.duration());
				this.favoritesService = (FavoritesService) Await.result(future2,
						timeout.duration());
				this.friendshipService = (FriendshipService) Await.result(future3,
						timeout.duration());
				this.entitiesService = (GraphEntitiesService) Await.result(future4,
						timeout.duration());
	//			this.g = grepo.getGraph();
			} catch (Exception e) {
				logger.info("eroro starting ..."+this.getClass().getName()+" ... "+e.getMessage());
				e.printStackTrace();
			}
		}
	}

	@Override
	public void postStop() {
//		g.shutdown();
		grepo.shutdownGraph();
	}

	protected ExecutionContext getDispatcher() {
		return this.getContext().system().dispatcher();
	}

	
//	protected Vertex getUser(String userId) {
//		Iterator<Vertex> userVerticesIterator = g.getVertices(
//				GraphConfig.ID_VERTEX_PROPERTY, userId).iterator();
//		return userVerticesIterator.hasNext() ? userVerticesIterator.next() : null;
//	}

}
