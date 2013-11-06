package com.findarecord.neo4j.plugin;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.server.plugins.*;

import java.util.ArrayList;

@Description( "Find-A-Record Collections Index" )
public class CollectionIndexPlugin extends ServerPlugin {

  @Name( "query_distance" )
  @Description( "Perform a Collection query" )
  @PluginTarget( GraphDatabaseService.class )
  public Iterable<String> queryDistance( @Source GraphDatabaseService graphDb,
                                         @Description( "The latitude" )
                                         @Parameter( name = "lat" ) Double lat,
                                         @Description( "The longitude" )
                                         @Parameter( name = "lon" ) Double lon,
                                         @Description( "The radius" )
                                         @Parameter( name = "radius" ) Double radius) {
    ArrayList<String> result = new ArrayList<>();

    //instantiate collections index
    CollectionQuery idx = new CollectionQuery(graphDb);

    return idx.queryDistance(lat,lon,radius);
  }

  @Name( "index" )
  @Description( "Index a Collection" )
  @PluginTarget( GraphDatabaseService.class )
  public Iterable<String> index( @Source GraphDatabaseService graphDb,
                                 @Description( "The collection id" )
                                 @Parameter( name = "collection_id" ) String collectionId,
                                 @Description( "The geojson string" )
                                 @Parameter( name = "geojson" ) String geojson) {
    //create result string
    ArrayList<String> result = new ArrayList<>();

    //instantiate collections index
    CollectionIndex idx = new CollectionIndex(graphDb);

    //index collection
    result.add(idx.indexCollection(collectionId));

    //index the passed in geojson
    String res = idx.indexGeoJSON(geojson);

    //spit out result
    result.add(res+"");

    //return
    return result;
  }

}

