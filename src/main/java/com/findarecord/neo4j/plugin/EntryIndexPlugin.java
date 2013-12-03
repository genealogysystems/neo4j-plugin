package com.findarecord.neo4j.plugin;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.server.plugins.*;

import java.util.ArrayList;

@Description( "Find-A-Record Collections Index" )
public class EntryIndexPlugin extends ServerPlugin {

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
    EntryQuery idx = new EntryQuery(graphDb);

    return idx.queryDistance(lon, lat,radius, from, to, tags, count, offset);
  }

  @Name( "index" )
  @Description( "Index an Entry" )
  @PluginTarget( GraphDatabaseService.class )
  public Iterable<String> index( @Source GraphDatabaseService graphDb,
                                 @Description( "id" )
                                 @Parameter( name = "id" ) String entryId,
                                 @Description( "repo_id" )
                                 @Parameter( name = "repo_id" ) String repoId,
                                 @Description( "collection_id" )
                                 @Parameter( name = "collection_id" ) String collectionId,
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

    //instantiate entry index
    EntryIndex idx = new EntryIndex(graphDb);

    //index entry
    result.add(idx.indexEntry(entryId, collectionId, from, to, tags, geojson));

    //return
    return result;
  }

  @Name( "delete" )
  @Description( "Delete an Entry" )
  @PluginTarget( GraphDatabaseService.class )
  public Iterable<String> delete( @Source GraphDatabaseService graphDb,
                                 @Description( "id" )
                                 @Parameter( name = "id" ) String entryId) {
    //create result string
    ArrayList<String> result = new ArrayList<>();

    //instantiate entry index
    EntryIndex idx = new EntryIndex(graphDb);

    //delete entry
    idx.deleteEntry(entryId);

    //return
    return result;
  }

}
