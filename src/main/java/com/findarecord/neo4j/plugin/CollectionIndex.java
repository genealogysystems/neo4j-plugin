package com.findarecord.neo4j.plugin;

import com.vividsolutions.jts.geom.*;
import org.geotools.geojson.geom.GeometryJSON;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.UniqueFactory;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Map;


public class CollectionIndex {

  private double depth = 0.001;
  private int numDecimals = 3;
  private int width = 10;
  private GraphDatabaseService graphDb;
  private UniqueFactory<Node> nodeFactory;

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

  public String indexGeoJSON(String geoString) {

    Geometry geometry = geoJSONtoGeometry(geoString);

    String ret = indexGeometry(geometry);

    return ret;
  }

  private String indexGeometry(Geometry geometry) {
    String ret = "";
    //start transaction
    //TODO

    //index first level
    ret = indexFirstLevel(geometry);

    return ret;
  }

  private String indexFirstLevel(Geometry geometryToIndex) {

    String ret = "";

    //get boxes for this level
    ArrayList<Box> boxes = getFirstLevelBoxes();

    //loop through boxes
    for(Box box : boxes) {
      //if geometryToIndex contains this boxes, insert and continue
      if(geometryToIndex.contains(box.getPolygon())) {
        ret += insertBox(box);
        continue;
      }

      //if geometryToIndex intersects this boxes, recurse
      if(geometryToIndex.intersects(box.getPolygon())) {
        //ret += "intersect found:"+box.toString()+" || ";
        ret += indexNLevel(geometryToIndex, box.getLat(), box.getLon(), 1);
      }
    }
    return ret;
  }

  private String indexNLevel(Geometry geometryToIndex, double lat, double lon, double precision) {
    String ret = "";

    //get boxes for this level
    ArrayList<Box> boxes = getNLevelBoxes(lat, lon, precision);

    //ret += "level "+lat+" "+lon+" "+precision+" - boxes="+boxes.size()+" || ";

    //loop through boxes
    for(Box box : boxes) {
      //if geometryToIndex contains this boxes, insert and continue
      if(geometryToIndex.contains(box.getPolygon())) {
        ret += insertBox(box);
        continue;
      }

      //if geometryToIndex intersects this boxes, recurse or stop
      if(geometryToIndex.intersects(box.getPolygon())) {
        //ret += "intersect found at depth "+precision+": "+box.toString()+" || ";
        //if we are at our max depth, insert rather than recurse
        if(precision == depth) {
          ret += insertBox(box);
        } else {
          ret += indexNLevel(geometryToIndex, box.getLat(), box.getLon(), precision/width);
        }
      }
    }
    return ret;
  }

  private ArrayList<Box> getFirstLevelBoxes() {

    ArrayList<Box> boxes = new ArrayList<>();
    int precision = 10;
    int fromLat = 90;
    int toLat = -90;
    int fromLon = -180;
    int toLon = 180;

    for(int lon=fromLon;lon<toLon; lon = lon+precision) {
      for(int lat=fromLat;lat>toLat; lat = lat-precision) {
        boxes.add(new Box(lat,lon, precision));
      }
    }

    return boxes;
  }

  private ArrayList<Box> getNLevelBoxes(double fromLat, double fromLon, double precision) {
    ArrayList<Box> boxes = new ArrayList<>();
    double toLat = fromLat-(precision*width);
    double toLon = fromLon+(precision*width);

    for(double lon=fromLon;lon<toLon; lon = lon+precision) {
      for(double lat=fromLat;lat>toLat; lat = lat-precision) {
        boxes.add(new Box(lat,lon, precision));
      }
    }

    return boxes;
  }

  private String insertBox(Box box) {
    String ret;

    try ( Transaction tx = graphDb.beginTx() ) {
      //get root node
      Node fromNode = graphDb.getNodeById(0);

      ArrayList<String> ids = box.getIds(numDecimals);

      for(String id: ids) {
        //create new node
        Node toNode = nodeFactory.getOrCreate( "id", id );

        //create relationship
        UniqueFactory.UniqueEntity<Relationship> rel = createRelationship(fromNode, toNode, "id", id);
        if(rel.wasCreated()) {
          rel.entity().setProperty("minLat", box.getLat());
          rel.entity().setProperty("maxLat", box.getLat()+box.getPrecision());

          rel.entity().setProperty("minLon", box.getLon()-box.getPrecision());
          rel.entity().setProperty("maxLon", box.getLon());
        }

        //point fromNode to toNode
        fromNode = toNode;
      }
      tx.success();
      return ids.toString();
    }


    //return "";
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

}
