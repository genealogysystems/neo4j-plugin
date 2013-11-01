package com.findarecord.neo4j.plugin;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.server.plugins.*;
import org.neo4j.tooling.GlobalGraphOperations;

import java.util.List;

@Description( "Find-A-Record Collections Index" )
public class CollectionIndex extends ServerPlugin
{
  @Name( "get_all_nodes" )
  @Description( "Get all nodes from the Neo4j graph database" )
  @PluginTarget( GraphDatabaseService.class )
  public Iterable<Node> getAllNodes( @Source GraphDatabaseService graphDb )
  {
    Iterable<Node> result;

    try(Transaction tx = graphDb.beginTx()) {
      result = GlobalGraphOperations.at(graphDb).getAllNodes();
      tx.success();
    }
    return result;
  }
}
