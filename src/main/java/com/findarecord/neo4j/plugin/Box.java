package com.findarecord.neo4j.plugin;

import com.vividsolutions.jts.geom.*;


/**
 * Created with IntelliJ IDEA.
 * User: john
 * Date: 11/5/13
 * Time: 10:49 AM
 * To change this template use File | Settings | File Templates.
 */
public class Box {

  //this is the width and height of the box;
  private double precision;

  //this is the longitude
  private double lon;

  //this is the latitude
  private double lat;

  private Polygon polygon;

  public Box(double lat, double lon, double precision) {
    this.lat = lat;
    this.lon = lon;
    this.precision = precision;
    Coordinate[] coords = new Coordinate[5];
    coords[0] = new Coordinate(lon,lat);
    coords[1] = new Coordinate(lon,lat+precision);
    coords[2] = new Coordinate(lon-precision,lat+precision);
    coords[3] = new Coordinate(lon-precision,lat);
    coords[4] = new Coordinate(lon,lat);
    LinearRing ring = new GeometryFactory().createLinearRing(coords);
    polygon = new GeometryFactory().createPolygon(ring,null);
  }

  public String toString() {
    return lat+":"+lon+" - "+precision;
  }

  public Geometry getPolygon() {
    return polygon;
  }

  public double getLon() {
    return lon;
  }

  public double getLat() {
    return lat;
  }
}
