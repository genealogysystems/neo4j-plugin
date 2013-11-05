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
    Geometry geometry;
    GeometryJSON gJSON = new GeometryJSON(15); //15 precision
    Reader reader = new StringReader(geojson);
    try {
      geometry = gJSON.read(reader);

    } catch (IOException e) {
      return e.toString();
    }


    //turn polygon into boxes
    String result = insertPolygon(geometry);


    return result;
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

  private String insertPolygon(Geometry geometry) {
    String ret = "";

    //get first level of contains and find out intersects and covers
    //todo move to insertBox
    try(Transaction tx = graphDb.beginTx()) {

      ArrayList<Geometry> topLevel = getFirstLevelGeometries();

      ret += "number of topLevel: "+topLevel.size()+"  \n";

      for(Geometry currentGeometry : topLevel) {

        //ret += "comapring "+currentGeometry.toString()+"\n\n";
        //if envelope is entirely inside geometry, add to contains
        if(geometry.contains(currentGeometry)) {
          //convert polygon to box and insert, then continue
          continue;
        }

        //if envelope intersects geometry, recurse
        if(geometry.intersects(currentGeometry)) {
          //recurse on this geometry
          ret += "found geom "+currentGeometry.toString();
        }
      }
      tx.success();
    }

    return ret;
  }

  private ArrayList<Geometry> getFirstLevelGeometries() {
    ArrayList<Geometry> geometries = new ArrayList<>();
    int fromX = -18;
    int toX = 18;
    int fromY = 9;
    int toY = -9;

    for(int x=fromX;x<toX; x = x+1) {
      for(int y=fromY;y>toY; y = y-1) {
        Coordinate[] coords = new Coordinate[5];
        coords[0] = new CustomCoordinate(x,y,10);
        coords[1] = new CustomCoordinate(x+1,y,10);
        coords[2] = new CustomCoordinate(x+1,y+1,10);
        coords[3] = new CustomCoordinate(x,y+1,10);
        coords[4] = new CustomCoordinate(x,y,10);
        LinearRing ring = new GeometryFactory().createLinearRing(coords);
        Geometry geom = new GeometryFactory().createPolygon(ring,null);
        geometries.add(geom);
      }
    }

    return geometries;
  }


  private String insertPolygonRecursive(Geometry geometry, int x, int y, int precision) {
    //x and y should already be multiplied by 10, and precision divided by 10

    String ret = "";

    //base case compare precision and return early

    //generate geometries
    ArrayList<Geometry> topLevel = getNLevelGeometries(x,y,precision);

    for(Geometry currentGeometry : topLevel) {

      if(geometry.contains(currentGeometry)) {
        //convert polygon to box and insert, then continue
        CustomCoordinate coord = (CustomCoordinate)currentGeometry.getCoordinate();
        insertBox(new Box(coord.boxX,coord.boxX, precision));
        continue;
      }

      //if envelope intersects geometry, recurse
      if(geometry.intersects(currentGeometry)) {
        //recurse on this geometry
        ret += "found geom "+currentGeometry.toString();
      }
    }

    //check contains

    //check intersects



    return ret;
  }

  private ArrayList<Geometry> getNLevelGeometries(int topX, int topY, int precision) {
    //x and y should already be multiplied by 10, and precision divided by 10

    ArrayList<Geometry> geometries = new ArrayList<>();
    int fromX = topX;
    int toX = topX+10;
    int fromY = topY;
    int toY = topY+10;

    for(int x=fromX;x<toX; x = x+1) {
      for(int y=fromY;y>toY; y = y-1) {
        Coordinate[] coords = new Coordinate[5];
        coords[0] = new CustomCoordinate(x,y,precision);
        coords[1] = new CustomCoordinate(x+1,y,precision);
        coords[2] = new CustomCoordinate(x+1,y+1,precision);
        coords[3] = new CustomCoordinate(x,y+1,precision);
        coords[4] = new CustomCoordinate(x,y,precision);
        LinearRing ring = new GeometryFactory().createLinearRing(coords);
        Geometry geom = new GeometryFactory().createPolygon(ring,null);
        geometries.add(geom);
      }
    }

    return geometries;
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

}
