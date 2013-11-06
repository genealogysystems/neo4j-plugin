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
import java.util.Set;

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
    double minLon = envelope.getMinX();
    double maxLon = envelope.getMaxX();
    double minLat = envelope.getMinY();
    double maxLat = envelope.getMaxY();

    //perform query
    try(Transaction tx = graphDb.beginTx()) {

      Node start;
      start = graphDb.getNodeById(0);
      TraversalDescription traversal = new TraversalDescriptionImpl()
      .breadthFirst()
      //only traverse paths in our bounding box
      .evaluator(getBoxEvaluator(minLon, maxLon, minLat, maxLat))
      //only return collections
      .evaluator(Evaluators.includeWhereLastRelationshipTypeIs(DynamicRelationshipType.withName(Settings.NEO_BOX_INTERSECT)));

      for(Path path : traversal.traverse(start)) {
        collectionIDs.add(path.endNode().getId()+"");
      }
      tx.success();
    }

    return collectionIDs;
  }

  private Evaluator getBoxEvaluator(final double minLon, final double maxLon, final double minLat, final double maxLat) {
    return new Evaluator() {
      @Override
      public Evaluation evaluate( final Path path )
      {
        if ( path.length() == 0 )
        {
          return Evaluation.EXCLUDE_AND_CONTINUE;
        }

        //if outside our boundary, exclude and prune, else include and continue
        Relationship rel = path.lastRelationship();
        if(rel.isType(DynamicRelationshipType.withName(Settings.NEO_BOX_LINK))
           && (maxLon < (double)rel.getProperty("minLon")
              || maxLat < (double)rel.getProperty("minLat")
              || minLon > (double)rel.getProperty("maxLon")
              || minLat > (double)rel.getProperty("maxLat")
              )
          ) {
           return Evaluation.EXCLUDE_AND_PRUNE;
        } else {
           return Evaluation.INCLUDE_AND_CONTINUE;
        }
      }
    };
  }

  private Evaluator getCoverageEvaluator(final double from, final double to) {
    return new Evaluator() {
      @Override
      public Evaluation evaluate( final Path path )
      {
        if ( path.length() == 0 )
        {
          return Evaluation.EXCLUDE_AND_CONTINUE;
        }

        //if outside our boundary, exclude and prune, else include and continue
        Relationship rel = path.lastRelationship();
        if(rel.isType(DynamicRelationshipType.withName(Settings.NEO_BOX_INTERSECT))
            && (from > (double)rel.getProperty("to")
               || to < (double)rel.getProperty("from")
               )
            ) {
          return Evaluation.EXCLUDE_AND_PRUNE;
        } else {
          return Evaluation.INCLUDE_AND_CONTINUE;
        }
      }
    };
  }

  private Evaluator getCollectionEvaluator(final double from, final double to, final Set<String> tags) {
    return new Evaluator() {
      @Override
      public Evaluation evaluate( final Path path )
      {
        if ( path.length() == 0 )
        {
          return Evaluation.EXCLUDE_AND_CONTINUE;
        }

        //if outside our boundary, exclude and prune, else include and continue
        Relationship rel = path.lastRelationship();
        if(rel.isType(DynamicRelationshipType.withName(Settings.NEO_COVERAGE_CONTAINS))
            && (from > (double)rel.getProperty("to")
               || to < (double)rel.getProperty("from")
               || !tags.contains((String)rel.getProperty("tag"))
               )
            ) {
          return Evaluation.EXCLUDE_AND_PRUNE;
        } else {
          return Evaluation.INCLUDE_AND_CONTINUE;
        }
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
