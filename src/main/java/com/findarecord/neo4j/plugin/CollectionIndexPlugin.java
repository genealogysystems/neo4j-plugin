package com.findarecord.neo4j.plugin;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.server.plugins.*;

import java.util.ArrayList;

@Description( "Find-A-Record Collections Index" )
public class CollectionIndexPlugin extends ServerPlugin {

  @Name( "query" )
  @Description( "Perform a Collection query" )
  @PluginTarget( GraphDatabaseService.class )
  public Iterable<String> query( @Source GraphDatabaseService graphDb ) {
    ArrayList<String> result = new ArrayList<String>();

    try(Transaction tx = graphDb.beginTx()) {
      result.add(graphDb.getNodeById(0).getId() + "");
      tx.success();
    }
    return result;
  }

  @Name( "index" )
  @Description( "Index a Collection" )
  @PluginTarget( GraphDatabaseService.class )
  public Iterable<String> index( @Source GraphDatabaseService graphDb ) {
    ArrayList<String> result = new ArrayList<String>();

    CollectionIndex idx = new CollectionIndex(graphDb);

    String res = idx.indexPolygon();

    result.add(res);

    return result;
  }

}

