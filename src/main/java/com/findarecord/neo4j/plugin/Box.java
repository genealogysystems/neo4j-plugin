package com.findarecord.neo4j.plugin;

import com.vividsolutions.jts.geom.*;
import org.apache.commons.lang.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;


public class Box {

  //this is the width and height of the box;
  private double precision;

  //this is the longitude
  private double lon;

  //this is the latitude
  private double lat;

  private Polygon polygon;

  public Box(double lon, double lat, double precision) {
    this.lat = lat;
    this.lon = lon;
    this.precision = precision;
    Coordinate[] coords = new Coordinate[5];
    coords[0] = new Coordinate(lon,lat);
    coords[1] = new Coordinate(lon+precision,lat);
    coords[2] = new Coordinate(lon+precision,lat+precision);
    coords[3] = new Coordinate(lon,lat+precision);
    coords[4] = new Coordinate(lon,lat);
    LinearRing ring = new GeometryFactory().createLinearRing(coords);
    polygon = new GeometryFactory().createPolygon(ring,null);
  }

  public String toString() {
    return lat+","+lon+":"+(lat+precision)+","+(lon+precision);
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

  public double getPrecision() {
    return precision;
  }

  public ArrayList<String> getIds(int numDecimals) {
    ArrayList<String> ids = new ArrayList<>();

    String latString = format(lat,numDecimals);
    String lonString = format(lon,numDecimals);

    String id = latString.substring(0,3)+","+lonString.substring(0,3);

    //add first id
    ids.add(id);

    for (int i = 3; i < latString.length(); i++){
      id += ":"+latString.charAt(i)+","+lonString.charAt(i);
      ids.add(id);
    }


    return ids;
  }

  private String format(double num, int numDecimals) {
    String part1;
    String part2;

    //get string
    BigDecimal bdNum = new BigDecimal(Math.abs(num)).setScale(numDecimals, RoundingMode.FLOOR);
    String numString = bdNum.toString();

    //split on decimal
    String[] parts = numString.split("\\.");

    //pad to 3 places with 0 for everything left of the decimal
    part1 = StringUtils.leftPad(parts[0], 3, '0');

    //add plus or minus
    if(num < 0) {
      part1 = "-"+part1;
    } else {
      part1 = "+"+part1;
    }

    //if there was a decimal, make sure it is the right length
    //otherwise pad to precision
    if(parts.length == 2) {
      //cut off any extra precision before padding out to "precision" places
      part2 = StringUtils.substring(parts[1], 0, numDecimals);
      part2 = StringUtils.rightPad(part2, numDecimals, '0');
    } else {
      part2 = StringUtils.rightPad("", numDecimals, '0');
    }

    return part1+part2;
  }

}
