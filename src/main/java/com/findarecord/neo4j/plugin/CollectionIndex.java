package com.findarecord.neo4j.plugin;

import com.vividsolutions.jts.geom.*;
import org.geotools.geojson.geom.GeometryJSON;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.UniqueFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Map;


public class CollectionIndex {

  private GraphDatabaseService graphDb;

  private Node collectionNode;

  public CollectionIndex(GraphDatabaseService graphDb) {
    this.graphDb = graphDb;
  }

  public String indexCollection(String collectionId) {
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
      tx.success();
    }


    return "";
  }

  public String indexGeoJSON(String geoString) {

    Geometry geometry = geoJSONtoGeometry(geoString);

    return indexGeometry(geometry);
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
        ret += "intersect found:"+box.toString()+" || ";
        ret += indexNLevel(geometryToIndex, box.getLon(), box.getLat(), 1);
      }
    }
    return ret;
  }

  private String indexNLevel(Geometry geometryToIndex, double lon, double lat, double precision) {
    String ret = "";

    //get boxes for this level
    ArrayList<Box> boxes = getNLevelBoxes(lon, lat, precision);

    ret += "level "+lon+" "+lat+" "+precision+" - boxes="+boxes.size()+" || ";

    //loop through boxes
    for(Box box : boxes) {
      //if geometryToIndex contains this boxes, insert and continue
      if(geometryToIndex.contains(box.getPolygon())) {
        ret += insertBox(box);
        continue;
      }

      ret += "box: "+box.toString()+" || ";

      //if geometryToIndex intersects this boxes, recurse or stop
      if(geometryToIndex.intersects(box.getPolygon())) {
        ret += "intersect found at depth "+precision+": "+box.toString()+" || ";
        //if we are at our max depth, insert rather than recurse
        if(precision == Settings.DEPTH) {
          ret += insertBox(box);
        } else {
          ret += indexNLevel(geometryToIndex, box.getLon(), box.getLat(), precision/Settings.WIDTH);
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
        boxes.add(new Box(lon,lat, precision));
      }
    }

    return boxes;
  }

  private ArrayList<Box> getNLevelBoxes(double fromLon, double fromLat, double precision) {
    ArrayList<Box> boxes = new ArrayList<>();
    double toLat = fromLat+(precision*Settings.WIDTH);
    double toLon = fromLon+(precision*Settings.WIDTH);

    for(double lon=fromLon;lon<toLon; lon = lon+precision) {
      for(double lat=fromLat;lat<toLat; lat = lat+precision) {
        boxes.add(new Box(lon,lat, precision));
      }
    }

    return boxes;
  }

  private String insertBox(Box box) {

    try ( Transaction tx = graphDb.beginTx() ) {
      //get root node
      Node fromNode = graphDb.getNodeById(0);
      String lastId = null;
      ArrayList<String> ids = box.getIds(Settings.DECIMALS);

      UniqueFactory<Node> nodeFactory = new UniqueFactory.UniqueNodeFactory( graphDb, Settings.NEO_BOX )
      {
        @Override
        protected void initialize( Node created, Map<String, Object> properties )
        {
          created.setProperty( "id", properties.get( "id" ) );
        }
      };

      for(String id: ids) {

        lastId = id;

        //create new node
        Node toNode = nodeFactory.getOrCreate( "id", id );

        //create relationship
        UniqueFactory.UniqueEntity<Relationship> rel = createRelationship(fromNode, toNode, "id", id, Settings.NEO_BOX_LINK_INDEX, Settings.NEO_BOX_LINK);
        if(rel.wasCreated()) {
          rel.entity().setProperty("minLat", box.getLat());
          rel.entity().setProperty("maxLat", box.getLat()+box.getPrecision());
          rel.entity().setProperty("minLon", box.getLon());
          rel.entity().setProperty("maxLon", box.getLon()+box.getPrecision());
        }

        //point fromNode to toNode
        fromNode = toNode;
      }

      //create relationship to collection Node
      UniqueFactory.UniqueEntity<Relationship> colRel = createRelationship(fromNode, collectionNode, "id", lastId, Settings.NEO_BOX_INTERSECT_INDEX, Settings.NEO_BOX_INTERSECT);
      if(colRel.wasCreated()) {
        colRel.entity().setProperty("from", 0);
        colRel.entity().setProperty("to", 9999);
      }

      tx.success();
      return ids.toString();
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
        return start.createRelationshipTo(end, DynamicRelationshipType.withName(relName));
      }
    };

    return factory.getOrCreateWithOutcome(indexableKey, indexableValue);
  }

}
