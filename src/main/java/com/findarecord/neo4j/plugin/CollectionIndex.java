package com.findarecord.neo4j.plugin;

import com.vividsolutions.jts.geom.*;
import org.geotools.geojson.geom.GeometryJSON;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.UniqueFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;


public class CollectionIndex {

  private GraphDatabaseService graphDb;

  private Node collectionNode;

  public CollectionIndex(GraphDatabaseService graphDb) {
    this.graphDb = graphDb;
  }

  public String indexCollection(String collectionId, Integer from, Integer to, String[] tags) {
    try ( Transaction tx = graphDb.beginTx() ) {
      UniqueFactory<Node> factory = new UniqueFactory.UniqueNodeFactory( graphDb, Settings.NEO_COLLECTION )
      {
        @Override
        protected void initialize( Node created, Map<String, Object> properties )
        {
          created.setProperty( "id", properties.get( "id" ) );
        }
      };
      collectionNode = factory.getOrCreate("id", collectionId);
      collectionNode.setProperty("from",from);
      collectionNode.setProperty("to",to);
      collectionNode.setProperty("tags",tags);
      //collectionNode.addLabel(DynamicLabel.label( "Collection" ));

      //remove all old relationships
      for(Relationship rel: collectionNode.getRelationships()) {
        rel.delete();
      }

      tx.success();
    }


    return "";
  }

  public String indexGeoJSON(String geoString) {
    String ret = "";
    Geometry geometry = geoJSONtoGeometry(geoString);

    int toIndex = geometry.getNumGeometries();

    for(int i=0; i < toIndex; i++) {
      ret += indexGeometry(geometry.getGeometryN(i));
    }

    return ret;
  }

  private String indexGeometry(Geometry geometry) {

    //index first level
    return indexFirstLevel(geometry);
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
        //ret += "   "+box.getNodeId();
        ret += indexNLevel(geometryToIndex, box.getLon(), box.getLat(), new BigDecimal(1), box.getIds());
      }
    }
    return ret;
  }

  private String indexNLevel(Geometry geometryToIndex, BigDecimal lon, BigDecimal lat, BigDecimal precision, ArrayList<String> ids) {
    String ret = "";

    //get boxes for this level
    ArrayList<Box> boxes = getNLevelBoxes(lon, lat, precision, ids);

    //ret += "level "+lon+" "+lat+" "+precision+" - boxes="+boxes.size()+" || ";

    //loop through boxes
    for(Box box : boxes) {
      //if geometryToIndex contains this boxes, insert and continue
      if(geometryToIndex.contains(box.getPolygon())) {
        ret += insertBox(box);
        continue;
      }

      //ret += "box: "+box.toString()+" || ";

      //if geometryToIndex intersects this boxes, recurse or stop
      if(geometryToIndex.intersects(box.getPolygon())) {
        //ret += "intersect found at depth "+precision+": "+box.toString()+" || ";
        //if we are at our max depth, insert rather than recurse
        if(precision.compareTo(Settings.DEPTH) <= 0) {
          ret += insertBox(box);
        } else {
          //ret += "recursing: new precision is "+precision.divide(Settings.WIDTH)+" || ";
          ret += indexNLevel(geometryToIndex, box.getLon(), box.getLat(), precision.divide(Settings.WIDTH), box.getIds());
        }
      }
    }
    return ret;
  }

  private ArrayList<Box> getFirstLevelBoxes() {

    ArrayList<Box> boxes = new ArrayList<>();
    int precision = 10;
    int fromLon = -180;
    int toLon = 180;
    int fromLat = -90;
    int toLat = 90;

    for(int lon=fromLon;lon<toLon; lon = lon+precision) {
      for(int lat=fromLat;lat<toLat; lat = lat+precision) {
        boxes.add(new Box(new BigDecimal(lon),new BigDecimal(lat), new BigDecimal(precision), new ArrayList<String>()));
      }
    }

    return boxes;
  }

  private ArrayList<Box> getNLevelBoxes(BigDecimal fromLon, BigDecimal fromLat, BigDecimal precision,  ArrayList<String> ids) {
    ArrayList<Box> boxes = new ArrayList<>();
    BigDecimal toLat = fromLat.add(precision.multiply(Settings.WIDTH));
    BigDecimal toLon = fromLon.add(precision.multiply(Settings.WIDTH));

    for(BigDecimal lon=fromLon;lon.compareTo(toLon) < 0; lon = lon.add(precision)) {
      for(BigDecimal lat=fromLat;lat.compareTo(toLat) < 0; lat = lat.add(precision)) {
        boxes.add(new Box(lon,lat, precision, ids));
      }
    }

    return boxes;
  }

  private String insertBox(Box box) {

    try ( Transaction tx = graphDb.beginTx() ) {
      //get root node
      Node fromNode = graphDb.getNodeById(0);
      String lastId = null;
      ArrayList<String> ids = box.getIds();

      UniqueFactory<Node> nodeFactory = new UniqueFactory.UniqueNodeFactory( graphDb, Settings.NEO_BOX )
      {
        @Override
        protected void initialize( Node created, Map<String, Object> properties )
        {
          created.setProperty( "id", properties.get( "id" ) );
        }
      };

      BigDecimal currentPrecision = new BigDecimal(10);

      String idString = "";

      for(String id: ids) {

        idString += ":"+id;

        lastId = id;

        //create new node
        Node toNode = nodeFactory.getOrCreate( "id", idString );

        //create relationship
        UniqueFactory.UniqueEntity<Relationship> rel = createRelationship(fromNode, toNode, "id", id, Settings.NEO_BOX_LINK_INDEX, Settings.NEO_BOX_LINK);
        if(rel.wasCreated()) {

          String[] idArr = id.split(",");

          BigDecimal lon = new BigDecimal(idArr[0]);
          BigDecimal lat = new BigDecimal(idArr[1]);

          rel.entity().setProperty("minLon", lon.doubleValue());
          rel.entity().setProperty("maxLon", lon.add(currentPrecision).doubleValue());
          rel.entity().setProperty("minLat", lat.doubleValue());
          rel.entity().setProperty("maxLat", lat.add(currentPrecision).doubleValue());
        }

        //point fromNode to toNode
        fromNode = toNode;
        currentPrecision = currentPrecision.divide(Settings.WIDTH);
      }

      //create relationship to collection Node
      fromNode.createRelationshipTo(collectionNode, DynamicRelationshipType.withName(Settings.NEO_BOX_INTERSECT));
      //UniqueFactory.UniqueEntity<Relationship> colRel = createRelationship(fromNode, collectionNode, "id", lastId, Settings.NEO_BOX_INTERSECT_INDEX, Settings.NEO_BOX_INTERSECT);

      tx.success();
      //return ids.toString();
      return "";
    }

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

  private UniqueFactory.UniqueEntity<Relationship> createRelationship(final Node start, final Node end, String indexableKey, final String indexableValue, String indexName, final String relName) {

    UniqueFactory<Relationship> factory = new UniqueFactory.UniqueRelationshipFactory(graphDb, indexName) {
      @Override
      protected Relationship create(Map<String, Object> properties) {
        return end.createRelationshipTo(start, DynamicRelationshipType.withName(relName));
      }
    };

    return factory.getOrCreateWithOutcome(indexableKey, indexableValue);
  }

}
