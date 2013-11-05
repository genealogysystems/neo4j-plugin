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
    ArrayList<String> result = new ArrayList<>();

    return result;
  }

  @Name( "index" )
  @Description( "Index a Collection" )
  @PluginTarget( GraphDatabaseService.class )
  public Iterable<String> index( @Source GraphDatabaseService graphDb,
                                 @Description( "The geojson string" )
                                 @Parameter( name = "geojson" ) String geojson) {
    //create result string
    ArrayList<String> result = new ArrayList<String>();

    //instantiate collections index
    CollectionIndex idx = new CollectionIndex(graphDb);

    //index the passed in geojson
    String res = idx.indexGeoJSON(geojson);

    //spit out result
    result.add(res+"");

    //return
    return result;
  }

}

