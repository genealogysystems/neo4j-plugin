package com.findarecord.neo4j.plugin;


import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.util.GeometricShapeFactory;
import org.geotools.geojson.geom.GeometryJSON;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.impl.traversal.TraversalDescriptionImpl;

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
    ArrayList<String> collectionIDs = new ArrayList<>();

    //create bounding envelope
    Envelope envelope = geometry.getEnvelopeInternal();

    //get min and max latitude
    double minLat = envelope.getMinY();
    double maxLat = envelope.getMaxY();
    double minLon = envelope.getMinX();
    double maxLon = envelope.getMaxX();

    //perform query
    try(Transaction tx = graphDb.beginTx()) {

      Node start;
      start = graphDb.getNodeById(0);
      TraversalDescription traversal = new TraversalDescriptionImpl()
      .breadthFirst()
      //only traverse paths in our bounding box
      //.evaluator(getBoxEvaluator(minLat,maxLat,minLon,maxLon));
      //only return collections
      .evaluator( Evaluators.includeWhereLastRelationshipTypeIs(DynamicRelationshipType.withName("colRelIdxContain")));

      for(Path path : traversal.traverse(start)) {
        collectionIDs.add(path.endNode().getId()+"");
      }
      tx.success();
    }

    return collectionIDs;
  }

  private Evaluator getBoxEvaluator(double minLat, double maxLat, double minLon, double maxLon) {
    return new Evaluator() {
      @Override
      public Evaluation evaluate( final Path path )
      {
        if ( path.length() == 0 )
        {
          return Evaluation.EXCLUDE_AND_CONTINUE;
        }

        boolean isExpectedType = path.lastRelationship().isType(DynamicRelationshipType.withName("idxContain"));

        return Evaluation.ofIncludes(isExpectedType);
      }
    };
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
