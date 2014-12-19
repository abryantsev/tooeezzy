package com.tooe.core.db.graph;

public class GraphGetLocationsActor extends GraphActor {

	@Override
	public void onReceive(Object message) throws Exception {
//		if (message instanceof GraphGetFavoriteLocations) {
//			GraphGetFavoriteLocations getLocationsMsg = (GraphGetFavoriteLocations) message;
//			List<String> locationIds = getFavoriteLocations(getLocationsMsg.getUserId());
//			getSender().tell(new GraphLocations(locationIds), getSelf());
//		}
////		if (message instanceof GraphSearchLocationsCity) {
////			GraphSearchLocationsCity searchLocationsMsg = (GraphSearchLocationsCity) message;
////			List<String> locationIds = searchLocationsWithCity(searchLocationsMsg.getUserId(), searchLocationsMsg);
////			getSender().tell(new GraphLocations(locationIds), getSelf());
////		}
////		if (message instanceof GraphSearchLocationsCoords) {
////			GraphSearchLocationsCoords searchLocationsMsg = (GraphSearchLocationsCoords) message;
////			List<String> locationIds = searchLocationsWithCoords(searchLocationsMsg.getUserId(), searchLocationsMsg);
////			getSender().tell(new GraphLocations(locationIds), getSelf());
////		}
	}

//	private List<String> getFavoriteLocations(String userId) {
//		List<String> locationIds = new ArrayList<String>();
//		Vertex user = grepo.getUser(userId);
//		if (user != null) {
//			//Query query = user.query().labels(GraphConfig.IN_EDGE_LABEL).has(GraphConfig.IS_FAVORITE_LOCATION_EDGE_PROPERTY, true);
//			Query query = user.query().labels(LocationRelationType.FAVORITE.name());
//			for (Vertex location : query.vertices()) {
//				locationIds.add(location.getProperty(LocationProps.OBJECTID.name()).toString());
//			}
//		}
//		return locationIds;
//	}

//	private List<String> searchLocationsWithCity(String userId, GraphSearchLocationsCity searchAttrs) {
//		List<String> locationIds = new ArrayList<String>();
//		Vertex user = getUser(userId);
//		if (user != null) {
//			Query query = getCoreQuery(searchAttrs, user);
//			query.has(GraphConfig.CITY_LOCATION_EDGE_PROPERTY, searchAttrs.getCityId());
//			for (Vertex location : query.vertices()) {
//				locationIds.add(location.getProperty(GraphConfig.ID_VERTEX_PROPERTY).toString());
//			}
//		}
//		return locationIds;
//	}
//
//	private List<String> searchLocationsWithCoords(String userId, GraphSearchLocationsCoords searchAttrs) {
//		List<String> locationIds = new ArrayList<String>();
//		Vertex user = getUser(userId);
//		if (user != null) {
//			Query query = getCoreQuery(searchAttrs, user);
//
//			//TODO ............................................. coords compare with bounding box
//			
//			for (Vertex location : query.vertices()) {
//				locationIds.add(location.getProperty(GraphConfig.ID_VERTEX_PROPERTY).toString());
//			}
//		}
//		return locationIds;
//	}

//	private Query getCoreQuery(GraphSearchLocations searchAttrs,
//			Vertex user) {
//		Query query = user.query().labels(GraphConfig.IN_EDGE_LABEL);
//		if(searchAttrs.inCheckinLocations()){
//			query.has(GraphConfig.IS_FAVORITE_LOCATION_EDGE_PROPERTY, true);
//		}
//		if(searchAttrs.byFriends()){
//			// TODO .............................................
//		}
//		if(searchAttrs.getCategory() != null){
//			query.has(searchAttrs.getCategory(), true);
//		}
//		if(searchAttrs.getName() != null){
//			query.has(GraphConfig.NAME_LOCATION_EDGE_PROPERTY, searchAttrs.getName());
//		}
//		if(searchAttrs.hasPromotions()){
//			query.has(GraphConfig.HAS_PROMOTIONS_LOCATION_EDGE_PROPERTY, true);
//		}
//		return query;
//	}
}
