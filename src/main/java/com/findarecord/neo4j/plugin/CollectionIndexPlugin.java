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
                                         @Parameter( name = "radius" ) Double radius,
                                         @Description( "from" )
                                         @Parameter( name = "from" ) Integer from,
                                         @Description( "to" )
                                         @Parameter( name = "to" ) Integer to,
                                         @Description( "tags" )
                                         @Parameter( name = "tags" ) String[] tags,
                                         @Description( "count" )
                                         @Parameter( name = "count" ) Integer count,
                                         @Description( "offset" )
                                         @Parameter( name = "offset" ) Integer offset) {
    ArrayList<String> result = new ArrayList<>();

    //instantiate collections index
    CollectionQuery idx = new CollectionQuery(graphDb);

    return idx.queryDistance(lat,lon,radius, from, to, tags, count, offset);
  }

  @Name( "index" )
  @Description( "Index a Collection" )
  @PluginTarget( GraphDatabaseService.class )
  public Iterable<String> index( @Source GraphDatabaseService graphDb,
                                 @Description( "id" )
                                 @Parameter( name = "id" ) String entryId,
                                 @Description( "repo_id" )
                                 @Parameter( name = "repo_id" ) String repoId,
                                 @Description( "from" )
                                 @Parameter( name = "from" ) Integer from,
                                 @Description( "to" )
                                 @Parameter( name = "to" ) Integer to,
                                 @Description( "tags" )
                                 @Parameter( name = "tags" ) String[] tags,
                                 @Description( "geojson" )
                                 @Parameter( name = "geojson" ) String geojson) {
    //create result string
    ArrayList<String> result = new ArrayList<>();

    //instantiate collections index
    CollectionIndex idx = new CollectionIndex(graphDb);

    //index collection
    result.add(idx.indexCollection(entryId, from, to, tags));

    //index the passed in geojson
    String res = idx.indexGeoJSON(geojson);

    //spit out result
    result.add(res+"");

    //return
    return result;
  }

}

