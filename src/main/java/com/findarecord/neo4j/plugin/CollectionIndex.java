package com.findarecord.neo4j.plugin;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.server.plugins.*;
import org.neo4j.tooling.GlobalGraphOperations;

@Description( "Find-A-Record Collections Index" )
public class CollectionIndex extends ServerPlugin
{
  @Name( "get_all_nodes" )
  @Description( "Get all nodes from the Neo4j graph database" )
  @PluginTarget( GraphDatabaseService.class )
  public Iterable<Node> getAllNodes( @Source GraphDatabaseService graphDb )
  {
    return GlobalGraphOperations.at(graphDb).getAllNodes();
  }
}
