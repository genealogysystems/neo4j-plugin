package com.findarecord.neo4j.plugin;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.UniqueFactory;
import org.neo4j.server.plugins.*;
import org.neo4j.tooling.GlobalGraphOperations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Description( "Find-A-Record Collections Index" )
public class CollectionIndex extends ServerPlugin {

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

    //get index nodefactory
    UniqueFactory<Node> factory =  getUniqueFactory(graphDb);

    //test node creation
    try ( Transaction tx = graphDb.beginTx() ) {
      Node node = factory.getOrCreate( "name", "testing123" );
      result.add(node.toString());
      tx.success();
    }
    return result;
  }


  private   UniqueFactory getUniqueFactory(GraphDatabaseService graphDb) {
    try ( Transaction tx = graphDb.beginTx() )
    {
      UniqueFactory<Node> factory = new UniqueFactory.UniqueNodeFactory( graphDb, "users" )
      {
        @Override
        protected void initialize( Node created, Map<String, Object> properties )
        {
          created.setProperty( "name", properties.get( "name" ) );
        }
      };
      return factory;
    }
  }
}

