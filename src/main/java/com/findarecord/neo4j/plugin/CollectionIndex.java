package com.findarecord.neo4j.plugin;

import com.vividsolutions.jts.geom.*;
import org.geotools.geojson.geom.GeometryJSON;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.UniqueFactory;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Map;


public class CollectionIndex {


  private GraphDatabaseService graphDb;


  public CollectionIndex(GraphDatabaseService graphDb) {
    this.graphDb = graphDb;
  }

  public String indexGeoJSON(String geoString) {

    Geometry geometry = geoJSONtoGeometry(geoString);

    String ret = indexGeometry(geometry);

    return "";
  }

  private String indexGeometry(Geometry geometry) {


    return null;
  }

  private Geometry geoJSONtoGeometry(String geoString) {
    Geometry geometry = null;
    GeometryJSON gJSON = new GeometryJSON(15); //15 precision
    Reader reader = new StringReader(geoString);
    try {
      geometry = gJSON.read(reader);

    } catch (IOException e) {
      return null;
    }

    return geometry;
  }

}
