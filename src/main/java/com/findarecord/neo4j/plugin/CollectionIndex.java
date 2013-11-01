package com.findarecord.neo4j.plugin;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.UniqueFactory;

import java.util.ArrayList;
import java.util.Map;


public class CollectionIndex {

  /*
   * The amount of precision after the decimal point
   * in both latitude and longitude
   */
  private int precision = 3;


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
      /*
      relationshipFactory = new UniqueFactory.UniqueRelationshipFactory( graphDb, "relIdx" )
      {
        @Override
        protected Relationship create(Map<String, Object> _) {

          created.
          //final Relationship relationship = startNode.createRelationshipTo(endNode, DynamicRelationshipType.withName(type));
          //return setProperties(relationship, properties);
        }
      };
      */
      tx.success();
    }
  }

  public Iterable<String> queryDistance() {

    return null;
  }

  public Iterable<String> queryPolygon() {

    return null;
  }

  public String indexPolygon() {

    //validate/initialize polygon

    //turn polygon into boxes
    ArrayList<Box> boxes = convertPolygonToBoxes();

    try(Transaction tx = graphDb.beginTx()) {
      //insert boxes into graph
      for(Box box : boxes) {
        insertBox(box);
      }
      tx.success();
    }

    return boxes.get(0).toString();
  }

  private void insertBox(Box box) {

    //get root node
    Node from = graphDb.getNodeById(0);

    //loop through segments and insert each segment
    for(Segment seg : box.getSegments()) {
      from = insertSegment(seg, from);
    }
  }

  private Node insertSegment(Segment seg, Node from) {
    //TODO consider moving transaction to this level to allow for more granular concurrency


    //insert/create next segment
    //Node node = nodeFactory.getOrCreate( "id", seg.getId() );

    return null;  //To change body of created methods use File | Settings | File Templates.
  }


  private Iterable<Box> convertDistanceToBoxes() {

    return null;
  }

  private ArrayList<Box> convertPolygonToBoxes() {
    ArrayList<Box> boxes = new ArrayList<Box>();

    boxes.add(new Box("123.45","90.123"));

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
