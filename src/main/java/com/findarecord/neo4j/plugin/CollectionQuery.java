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
import java.util.*;

public class CollectionQuery {

  private GraphDatabaseService graphDb;

  public CollectionQuery(GraphDatabaseService graphDb) {
     this.graphDb = graphDb;
  }

  public ArrayList<String> queryPolygon(String geoString, Integer from, Integer to, String[] tags, Integer count, Integer offset) {
    ArrayList<String> collectionIDs = new ArrayList<>();

    //get geometry
    Geometry geometry = geoJSONtoGeometry(geoString);

    //if we have a valid geometry, query it
    if(geometry != null) {
       collectionIDs = queryGeometry(geometry, from, to, tags, count, offset);
    }

    return collectionIDs;
  }

  public ArrayList<String> queryDistance(double lat, double lon, double radius, Integer from, Integer to, String[] tags, Integer count, Integer offset) {
    ArrayList<String> collectionIDs;

    //TODO convert radius properly

    //create a circle
    GeometricShapeFactory shapeFactory = new GeometricShapeFactory();
    shapeFactory.setNumPoints(32);
    shapeFactory.setCentre(new Coordinate(lon, lat));
    shapeFactory.setSize(radius * 2);
    Geometry circle = shapeFactory.createCircle();

    //perform query
    collectionIDs = queryGeometry(circle, from, to, tags, count, offset);

    return collectionIDs;
  }

  private ArrayList<String> queryGeometry(Geometry geometry, Integer from, Integer to, String[] tags, Integer count, Integer offset) {
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
      .evaluator(getEvaluator(minLon, maxLon, minLat, maxLat, from, to, new HashSet<String>(Arrays.asList(tags))))
      //only return collections
      .evaluator(Evaluators.includeWhereLastRelationshipTypeIs(DynamicRelationshipType.withName(Settings.NEO_BOX_INTERSECT)));

      List<Node> hits = new ArrayList<Node>();

      for(Path path : traversal.traverse(start)) {
        hits.add(path.endNode());
      }

      Collections.sort(hits, new Comparator<Node>() {
        @Override
        public int compare(Node node, Node node2) {
          if(node.getId()>node2.getId()) {
            return -1;
          } else {
            return 1;
          }
        }
      });

      int i = 0;
      int end = offset+count;
      for(Node col: hits) {
        if(i >= offset && i < end) {
          collectionIDs.add((String)col.getProperty("id"));
        }
        i++;
      }
      tx.success();
    }

    return collectionIDs;
  }

  private Evaluator getEvaluator(final double minLon, final double maxLon, final double minLat, final double maxLat,final int from, final int to, final Set<String> tags) {
    return new Evaluator() {
      @Override
      public Evaluation evaluate( final Path path )
      {
        if ( path.length() == 0 )
        {
          return Evaluation.EXCLUDE_AND_CONTINUE;
        }

        boolean includeAndContinue = true;

        //if outside our boundary, exclude and prune, else include and continue
        Relationship rel = path.lastRelationship();
        Node node = path.endNode();
        if(rel.isType(DynamicRelationshipType.withName(Settings.NEO_BOX_LINK))
           && (maxLon < (double)rel.getProperty("minLon")
              || maxLat < (double)rel.getProperty("minLat")
              || minLon > (double)rel.getProperty("maxLon")
              || minLat > (double)rel.getProperty("maxLat")
              )
          ) {
          includeAndContinue = false;
        }
        if(rel.isType(DynamicRelationshipType.withName(Settings.NEO_BOX_INTERSECT))) {
          boolean hasTags = false;

          //if we were passed in no tags, don't check them
          if(tags.size() == 0) {
            hasTags = true;
            //else loop through the node's tags and make sure they match
          } else {
            String[] nodeTags = (String[])node.getProperty("tags");
            for(String nodeTag:nodeTags) {
              if(tags.contains(nodeTag)) {
                hasTags = true;
              }
            }
          }

          if(from > (int)node.getProperty("to")
              || to < (int)node.getProperty("from")
              || !hasTags) {
            includeAndContinue = false;
          }
        }


        if(includeAndContinue) {
          return Evaluation.INCLUDE_AND_CONTINUE;
        } else {
          return Evaluation.EXCLUDE_AND_PRUNE;
        }

      }
    };
  }

  private Evaluator getCollectionEvaluator(final int from, final int to, final Set<String> tags) {
    return new Evaluator() {
      @Override
      public Evaluation evaluate( final Path path )
      {
        if ( path.length() == 0 )
        {
          return Evaluation.EXCLUDE_AND_CONTINUE;
        }

        //if outside our boundary, exclude and prune, else include and continue
        Node node = path.endNode();
        Relationship rel = path.lastRelationship();
        if(rel.isType(DynamicRelationshipType.withName(Settings.NEO_COVERAGE_CONTAINS))) {
          return Evaluation.EXCLUDE_AND_PRUNE;
          /*
          boolean hasTags = false;
          String[] nodeTags = (String[])node.getProperty("tags");
          for(String nodeTag:nodeTags) {
            if(tags.contains(nodeTag)) {
              hasTags = true;
            }
          }

          hasTags = false;

          if(from > (int)node.getProperty("to")
                 || to < (int)node.getProperty("from")
                 || !hasTags) {
            return Evaluation.EXCLUDE_AND_PRUNE;
          } else {
            return Evaluation.EXCLUDE_AND_PRUNE;
          }
          */
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
