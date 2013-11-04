package com.findarecord.neo4j.plugin;

import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geojson.geom.GeometryJSON;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.UniqueFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Map;


public class CollectionIndex {

  /*
   * The amount of precision after the decimal point
   * in both latitude and longitude
   */
  private int precision = 2;


  private GraphDatabaseService graphDb;

  private UniqueFactory<Node> nodeFactory;

  private UniqueFactory<Relationship> relationshipFactory;

  public CollectionIndex(GraphDatabaseService graphDb) {
    this.graphDb = graphDb;
    try ( Transaction tx = graphDb.beginTx() )
    {
      nodeFactory = new UniqueFactory.UniqueNodeFactory( graphDb, "nodeIdx" )
      {
        @Override
        protected void initialize( Node created, Map<String, Object> properties )
        {
          created.setProperty( "id", properties.get( "id" ) );
        }
      };

      tx.success();
    }
  }

  public Iterable<String> queryDistance() {

    return null;
  }

  public Iterable<String> queryPolygon() {

    return null;
  }

  public String indexPolygon(String geojson) {

    boolean noErrors = true;
    //validate/initialize polygon
    int decimals = 15;
    GeometryJSON gjson = new GeometryJSON(decimals);
    Reader reader = new StringReader(geojson);
    try {
      gjson.readGeometryCollection(reader);
    } catch (IOException e) {
      return e.toString();
    }


    //turn polygon into boxes
    ArrayList<Box> boxes = convertPolygonToBoxes();

    //insert boxes into graph
    try(Transaction tx = graphDb.beginTx()) {
      for(Box box : boxes) {
        if(!insertBox(box)) {
          noErrors = false;
        }
      }
      tx.success();
    }

    return "";
  }

  /**
   * Returns true if box was successfully inserted
   */
  private boolean insertBox(Box box) {

    //get root node
    Node from = graphDb.getNodeById(0);

    //loop through segments and insert each segment
    for(Segment seg : box.getSegments()) {
      from = insertSegment(seg, from);
    }

    return true;
  }

  private Node insertSegment(Segment seg, Node fromNode) {
    //TODO consider moving transaction to this level to allow for more granular concurrency

    //insert/create next segment
    Node toNode = nodeFactory.getOrCreate( "id", seg.getId() );
    UniqueFactory.UniqueEntity<Relationship> rel = createRelationship(fromNode, toNode, "id", seg.getId());
    if(rel.wasCreated()) {
      rel.entity().setProperty("x", seg.getXInt());
      rel.entity().setProperty("y", seg.getYInt());
    }

    return toNode;  //To change body of created methods use File | Settings | File Templates.
  }

  private UniqueFactory.UniqueEntity<Relationship> createRelationship(final Node start, final Node end, String indexableKey, final String indexableValue) {

    UniqueFactory<Relationship> factory = new UniqueFactory.UniqueRelationshipFactory(graphDb, "relIdx") {
      @Override
      protected Relationship create(Map<String, Object> properties) {
        Relationship r =  start.createRelationshipTo(end, DynamicRelationshipType.withName("idxContain"));
        return r;
      }
    };

    return factory.getOrCreateWithOutcome(indexableKey, indexableValue);
  }

  private Iterable<Box> convertDistanceToBoxes() {

    return null;
  }

  private ArrayList<Box> convertPolygonToBoxes() {
    ArrayList<Box> boxes = new ArrayList<Box>();

    boxes.add(new Box("123.45","90.123", precision));

    return boxes;
  }


  private void tmp() {

    /*
    //get index nodefactory
    UniqueFactory<Node> factory =  getUniqueFactory(graphDb);

    //test node creation
    try ( Transaction tx = graphDb.beginTx() ) {
      Node node = factory.getOrCreate( "name", "testing123" );
      result.add(node.toString());
      tx.success();
    }
    */
  }

}
