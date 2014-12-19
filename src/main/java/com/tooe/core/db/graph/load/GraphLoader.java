package com.tooe.core.db.graph.load;

import java.util.HashMap;
import java.util.Set;

import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.TitanKey;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tooe.core.db.graph.domain.FavoriteProps;
import com.tooe.core.db.graph.domain.FriendshipProps;
import com.tooe.core.db.graph.domain.FriendshipType;
import com.tooe.core.db.graph.domain.LocationRelationType;
import com.tooe.core.db.graph.domain.UserProps;

public class GraphLoader {

	public TitanGraph graph;

	public GraphLoader(TitanGraph graph) {
		this.graph = graph;
	}

	public TitanGraph getGraph() {
		return graph;
	}

	public void setGraph(TitanGraph graph) {
		this.graph = graph;
	}

	public void init() {
		createIndiciesAndTypes();
	}

	// !!! test data are NOT consistent with NEW graph types !!!

//	public void initWithData(GraphData graphData) {
//		createIndiciesAndTypes();
//		
//		putGraphData(graphData);
//	}

//	public void putGraphData(GraphData graphData) {	
//		createVertsEdges(GraphTestData.vertsUserIds, GraphTestData.vertsUserKeys,
//				GraphTestData.edgesUserIds, GraphTestData.edgesUserKeys,
//				GraphTestData.vertsUserData, GraphTestData.edgesUserData, "friend");
//
//		createVertsEdges(GraphTestData.vertsLocIds, GraphTestData.vertsLocKeys,
//				GraphTestData.edgesLocIds, GraphTestData.edgesLocKeys,
//				GraphTestData.vertsLocData, GraphTestData.edgesLocData, "in");
//	}
	
	private void createIndiciesAndTypes() {
		// Vertex properties
		if (graph.getType(UserProps.TYPE.name()) == null) {
			graph.makeKey(UserProps.TYPE.name()).dataType(String.class).single().indexed(Vertex.class).make();
		}
		if (graph.getType(UserProps.OBJECTID.name()) == null) {
			graph.makeKey(UserProps.OBJECTID.name()).dataType(String.class).unique().indexed(Vertex.class).make();
		}
		
		String INDEX_NAME = "search";
		
		TitanKey friendId = null;
		TitanKey locationId = null;
		// Edge properties
		if (graph.getType(FriendshipProps.FRIENDID.name()) == null) { //friendid property on friendship edge
//			graph.makeType().name("friendid").dataType(String.class).unique(Direction.OUT)
//					.indexed(Edge.class).makePropertyKey();
			friendId = graph.makeKey(FriendshipProps.FRIENDID.name()).dataType(String.class).single().make();
		}
		if (graph.getType(FavoriteProps.LOCATIONID.name()) == null) { //locationid property on favorite place edge
			locationId = graph.makeKey(FavoriteProps.LOCATIONID.name()).dataType(String.class).single().make();
		}

		//Edge types
		if(graph.getType(LocationRelationType.FAVORITE.name()) == null) { //favorite place relation
			graph.makeLabel(LocationRelationType.FAVORITE.name()).manyToMany().make();
		}
		if(graph.getType(FriendshipType.FRIEND.name()) == null) { //general friend relation
			graph.makeLabel(FriendshipType.FRIEND.name()).manyToMany().unidirected().make();
		}
		if(graph.getType(FriendshipType.MYFRIEND.name()) == null) { //friends group relation
			graph.makeLabel(FriendshipType.MYFRIEND.name()).manyToMany().unidirected().make();
		}
		if(graph.getType(FriendshipType.BESTFRIEND.name()) == null) { //bestfriends group relation
			graph.makeLabel(FriendshipType.BESTFRIEND.name()).manyToMany().unidirected().make();
		}
		if(graph.getType(FriendshipType.FAMILY.name()) == null) { //family group relation
			graph.makeLabel(FriendshipType.FAMILY.name()).manyToMany().unidirected().make();
		}
		if(graph.getType(FriendshipType.COLLEAGUE.name()) == null) { //colleagues group relation
			graph.makeLabel(FriendshipType.COLLEAGUE.name()).manyToMany().unidirected().make();
		}

		
		
//		if (graph.getType("lon") == null) {
//			graph.makeType().name("lon").dataType(Double.class).unique(Direction.OUT).indexed(Vertex.class)
//					.makePropertyKey();
//		}
//		if (graph.getType("lat") == null) {
//			graph.makeType().name("lat").dataType(Double.class).unique(Direction.OUT).indexed(Vertex.class)
//					.makePropertyKey();
//		}
//		if (graph.getType("promotions") == null) {
//			graph.makeType().name("promotions").dataType(Object.class).unique(Direction.OUT)
//					.indexed(Vertex.class).makePropertyKey();
//		}
//		if (graph.getType("city") == null) {
//			graph.makeType().name("city").dataType(String.class).unique(Direction.OUT).indexed(Vertex.class)
//					.makePropertyKey();
//		}
//		if (graph.getType("cafe") == null) {
//			graph.makeType().name("cafe").dataType(Object.class).unique(Direction.OUT).indexed(Vertex.class)
//					.makePropertyKey();
//		}
//		if (graph.getType("rest") == null) {
//			graph.makeType().name("rest").dataType(Object.class).unique(Direction.OUT).indexed(Vertex.class)
//					.makePropertyKey();
//		}
//		if (graph.getType("beauty") == null) {
//			graph.makeType().name("beauty").dataType(Object.class).unique(Direction.OUT)
//					.indexed(Vertex.class).makePropertyKey();
//		}
//		if (graph.getType("children") == null) {
//			graph.makeType().name("children").dataType(Object.class).unique(Direction.OUT)
//					.indexed(Vertex.class).makePropertyKey();
//		}
//		if (graph.getType("courses") == null) {
//			graph.makeType().name("courses").dataType(Object.class).unique(Direction.OUT)
//					.indexed(Vertex.class).makePropertyKey();
//		}
//		if (graph.getType("shops") == null) {
//			graph.makeType().name("shops").dataType(Object.class).unique(Direction.OUT).indexed(Vertex.class)
//					.makePropertyKey();
//		}
//		if (graph.getType("flowers") == null) {
//			graph.makeType().name("flowers").dataType(Object.class).unique(Direction.OUT)
//					.indexed(Vertex.class).makePropertyKey();
//		}
//		if (graph.getType("service") == null) {
//			graph.makeType().name("service").dataType(Object.class).unique(Direction.OUT)
//					.indexed(Vertex.class).makePropertyKey();
//		}
//		if (graph.getType("name") == null) {
//			graph.makeType().name("name").dataType(String.class).unique(Direction.OUT).indexed(Vertex.class)
//					.makePropertyKey();
//		}
//		if (graph.getType("favorite") == null) {
//			graph.makeType().name("favorite").dataType(Object.class).unique(Direction.OUT)
//					.indexed(Vertex.class).makePropertyKey();
//		}
//		if (graph.getType("checkin") == null) {
//			graph.makeType().name("checkin").dataType(Object.class).unique(Direction.OUT)
//					.indexed(Vertex.class).makePropertyKey();
//		}

		// Edge types (OLD)
//		if (graph.getType("boolattr") == null) {
//			TitanKey boolattr = graph.makeType().name("boolattr").dataType(Boolean.class)
//					.unique(Direction.OUT).makePropertyKey();
//			graph.makeType().name("friend").unidirected().signature(boolattr)
//					.makeEdgeLabel();
//			graph.makeType().name("in").directed().signature(boolattr).makeEdgeLabel();
//		}
		
		graph.commit();
	}
	
	
	// === test data are NOT consistent with NEW Types ===

	private void createVertsEdges(String[] vertsIds, String[] vertsKeys,
			String[] edgesIds, String[] edgesKeys, Object[][] vertsData,
			Object[][] edgesData, String edgeLabel) {
		Boolean createEdges = false;

		for (int i = 0; i < vertsData.length; i++) {
			if (!graph.getVertices("name", vertsIds[i]).iterator().hasNext()) {
				Vertex vert = graph.addVertex(null);

				for (int j = 0; j < vertsData[i].length; j++) {
					if (vertsData[i][j] != null) {
						vert.setProperty(vertsKeys[j], vertsData[i][j]);
					}
				}
				vert.setProperty(vertsKeys[vertsData[i].length], vertsIds[i]);

				createEdges = true;
			}
		}

		graph.commit();

		HashMap<String, Vertex> verts = readVerticies(vertsIds);

		if (createEdges) {
			for (int i = 0; i < edgesData.length; i++) {
				String vertOut = edgesIds[i].split("-")[0];
				String vertIn = edgesIds[i].split("-")[1];

				Edge edge = graph.addEdge(null, verts.get(vertOut), verts.get(vertIn),
						edgeLabel);
				for (int j = 0; j < edgesData[i].length; j++) {
					Object value = edgesData[i][j];
					if (value != null) {
						edge.setProperty(edgesKeys[j], 
								value );
								//(value instanceof Boolean) ? ((Boolean)value).booleanValue() : value );
								//(value instanceof Boolean) ? ((Boolean)value).booleanValue() : value c
					}
				}
			}
		}

		graph.commit();
	}

	private HashMap<String, Vertex> readVerticies(String[] vertsIds) {
		HashMap<String, Vertex> verts = new HashMap<String, Vertex>(3);

		for (Vertex vert : graph.getVertices("type", "user")) {
			verts.put(vert.getProperty("name").toString(), vert);
		}
		for (Vertex vert : graph.getVertices("type", "location")) {
			verts.put(vert.getProperty("name").toString(), vert);
		}

		return verts;
	}

	public void showFeatures() {
		System.out.println("\nTitan Load ...");
		System.out.println("\tFeatures:");
		System.out.println("\t\t" + graph.getFeatures().toString());
	}

	public void showIndicies() {
		Set<String> idxNames = graph.getIndexedKeys(Vertex.class);

		System.out.println("\nTitan Load ...");
		for (String idxName : idxNames) {
			System.out.println("\tVertex index -> " + idxName);
		}
	}

	public void printVertsCount() {
		Long i = 0L;
		for (Vertex vertex : graph.getVertices("type", "user")) {
			i++;
		}
		for (Vertex vertex : graph.getVertices("type", "location")) {
			i++;
		}

		System.out.println("\nTitan Load ...");
		System.out.println("\tverticies сount -> " + i.toString());
	}

	public void describeVerts(String vertType, String[] names) {
		if (vertType == null) {
			for (String name : names) {
				printVertsProps(graph.getVertices("name", name).iterator().next());
			}
		} else {
			for (Vertex vertex : graph.getVertices("type", vertType)) {
				printVertsProps(vertex);
			}
		}
	}

	private void printVertsProps(Vertex vertex) {
		System.out.println("\tvertex \"id\" -> " + vertex.getId().toString());

		for (String vertexProp : vertex.getPropertyKeys()) {
			System.out.println("\t\tproperty \"" + vertexProp + "\" -> "
					+ vertex.getProperty(vertexProp).toString());

			System.out.println("\t\t\ttype name -> "
					+ graph.getType(vertexProp).getName());
			System.out.println("\t\t\ttype -> "
					+ ((TitanKey) graph.getType(vertexProp)).getDataType().getName());
		}
	}

}
