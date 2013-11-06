package com.findarecord.neo4j.plugin;


import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.util.GeometricShapeFactory;
import org.geotools.geojson.geom.GeometryJSON;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;

public class CollectionQuery {

  private GraphDatabaseService graphDb;

  public CollectionQuery(GraphDatabaseService graphDb) {
     this.graphDb = graphDb;
  }

  public ArrayList<String> queryPolygon(String geoString) {
    ArrayList<String> collectionIDs = new ArrayList<>();

    //get geometry
    Geometry geometry = geoJSONtoGeometry(geoString);

    //if we have a valid geometry, query it
    if(geometry != null) {
       collectionIDs = queryGeometry(geometry);
    }

    return collectionIDs;
  }

  public ArrayList<String> queryDistance(double lat, double lon, double radius) {
    ArrayList<String> collectionIDs;

    //TODO convert radius properly

    //create a circle
    GeometricShapeFactory shapeFactory = new GeometricShapeFactory();
    shapeFactory.setNumPoints(32);
    shapeFactory.setCentre(new Coordinate(lon, lat));
    shapeFactory.setSize(radius * 2);
    Geometry circle = shapeFactory.createCircle();

    //perform query
    collectionIDs = queryGeometry(circle);

    return collectionIDs;
  }

  private ArrayList<String> queryGeometry(Geometry geometry) {
    ArrayList<String> collectionIDs;

    //create bounding envelope
    Envelope envelope = geometry.getEnvelopeInternal();

    //get min and max latitude
    double minLat = envelope.getMinY();
    double maxLat = envelope.getMaxY();
    double minLon = envelope.getMinX();
    double maxLon = envelope.getMaxX();

    //perform query
    collectionIDs = doNeo4jTraversal();

    return collectionIDs;
  }

  private ArrayList<String> doNeo4jTraversal() {
    ArrayList<String> collectionIDs = new ArrayList<>();

    collectionIDs.add("test");

    return collectionIDs;
  }

  private Geometry geoJSONtoGeometry(String geoString) {
    Geometry geometry;
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
