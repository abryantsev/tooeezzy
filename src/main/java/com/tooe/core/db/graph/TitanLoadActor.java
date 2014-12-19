package com.tooe.core.db.graph;

import java.util.ArrayList;

import org.bson.types.ObjectId;

import com.thinkaurelius.titan.core.TitanGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tooe.core.db.graph.data.GraphTestData;
import com.tooe.core.db.graph.load.GraphLoader;
import com.tooe.core.db.graph.msg.TitanLoad;
import com.tooe.core.application.Actors$;
import scala.Symbol;

public class TitanLoadActor extends GraphActor {

  final static public Symbol Id = Actors$.MODULE$.TitanLoadGraph();
	
  private TitanGraph graph;
	GraphLoader loader;

//	@Override
//	public void preStart() {
//	}

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof TitanLoad) {
			load();
		}
		getSender().tell("ok", getSelf());
	}
	
	private void load() {
// TODO move to preStart or someware else to initialize loader	  
    loader = new GraphLoader(grepo.getGraph());
   
		loader.showFeatures();
		
		loader.init();
		
		loader.showIndicies();

		loader.printVertsCount();

    // Bulk Load
    //BulkDeleteNameProp();
    //BulkDelete();

    //PrintVertsCount();
    //describeVerts("user", null);
    //describeVerts("location", null);
        
    // Bulk Load
    //BulkLoad(1000);
    
    //PrintVertsCount();
    
    System.out.println("\nTitan Load ...");
    loader.describeVerts(null, GraphTestData.vertsUserIds);
    loader.describeVerts(null, GraphTestData.vertsLocIds);
	}
	
  private void BulkLoad(long cou) {
    System.out.println("\t\tFilling bulk verticies...");
    
    Vertex iam = graph.getVertices("name", "iam").iterator().next();
    
    for ( int i = 0; i < cou; i++ ) {
      Vertex vertUser = graph.addVertex(null);
      vertUser.setProperty("type", "user");
      vertUser.setProperty("name", ObjectId.get().toString());
      
      graph.addEdge(null, vertUser, iam, "friend");
      
      Vertex vertLoc = graph.addVertex(null);
      vertLoc.setProperty("type", "location");
      vertLoc.setProperty("lat", 39.739037);
      vertLoc.setProperty("name", ObjectId.get().toString());
      
      graph.addEdge(null, vertUser, vertLoc, "in");
    }
    
    graph.commit();
  }

  private void BulkDelete() {
    System.out.println("\t\tDeleting user verticies w/o name property...");

    ArrayList<Object> vertErr = new ArrayList<>(100);
    
    for (Vertex vert: graph.getVertices("type", "user")) {
      if (vert.getProperty("name") == null) {
        try {
          graph.removeVertex(vert);
        } catch (Exception e) {
          System.out.println("\t\t\terror removing vertex ID - "+vert.getId().toString());
          vertErr.add(vert.getId());
        }
      }
    }
    System.out.println("\t\tCommiting user verticies delete operation...");
    graph.commit();
    
    System.out.println("\t\tRemoving not deleted user vertecies...");
    for (Object vert : vertErr) {
      graph.removeVertex(graph.getVertex(vert));
    }
    graph.commit();

    vertErr = new ArrayList<>(100);
    
    for (Vertex vert: graph.getVertices("type", "location")) {
      if (vert.getProperty("name") == null) {
        try {
          graph.removeVertex(vert);
        } catch (Exception e) {
          System.out.println("\t\t\terror removing vertex ID - "+vert.getId().toString());
          vertErr.add(vert.getId());
        }
      }
    }
    System.out.println("\t\tCommiting location verticies delete operation...");
    graph.commit();

    System.out.println("\t\tRemoving not deleted location vertecies...");
    for (Object vert : vertErr) {
      graph.removeVertex(graph.getVertex(vert));
    }
    graph.commit();
  }

  private void BulkDeleteNameProp() {
    System.out.println("\t\tDeleting name property from user verticies...");
    for (Vertex vert: graph.getVertices("type", "user") ) {
      if (vert.getProperty("name") != null) {
        vert.removeProperty("name");
      }
    }
    System.out.println("\t\tCommiting name property of user verticies delete operation...");
    graph.commit();
    
    System.out.println("\t\tDeleting name property from location verticies...");
    for (Vertex vert: graph.getVertices("type", "location") ) {
      if (vert.getProperty("name") != null) {
        vert.removeProperty("name");
      }
    }
    System.out.println("\t\tCommiting name property of location verticies delete operation...");
    graph.commit();
  }


    
}
