package com.findarecord.neo4j.plugin;

import com.vividsolutions.jts.geom.*;
import org.apache.commons.lang.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;


public class Box {

  //this is the width and height of the box;
  private BigDecimal precision;

  //this is the longitude
  private BigDecimal lon;

  //this is the latitude
  private BigDecimal lat;

  private Polygon polygon;

  public Box(BigDecimal lon, BigDecimal lat, BigDecimal precision) {
    this.lon = lon;
    this.lat = lat;
    this.precision = precision;
    Coordinate[] coords = new Coordinate[5];
    coords[0] = new Coordinate(lon.doubleValue(),lat.doubleValue());
    coords[1] = new Coordinate(lon.add(precision).doubleValue(),lat.doubleValue());
    coords[2] = new Coordinate(lon.add(precision).doubleValue(),lat.add(precision).doubleValue());
    coords[3] = new Coordinate(lon.doubleValue(),lat.add(precision).doubleValue());
    coords[4] = new Coordinate(lon.doubleValue(),lat.doubleValue());
    LinearRing ring = new GeometryFactory().createLinearRing(coords);
    polygon = new GeometryFactory().createPolygon(ring,null);
  }

  public String toString() {
    return lon+","+lat+":"+lon.add(precision)+","+lat.add(precision);
  }

  public Geometry getPolygon() {
    return polygon;
  }

  public BigDecimal getLon() {
    return lon;
  }

  public BigDecimal getLat() {
    return lat;
  }

  public BigDecimal getPrecision() {
    return precision;
  }

  public ArrayList<String> getIds(int numDecimals) {
    ArrayList<String> ids = new ArrayList<>();

    String latString = format(lat,numDecimals);
    String lonString = format(lon,numDecimals);

    //+123.45678 > +12 - +123 --> +123.4 --> +123.45 --> ...

    //add first 2 until decimal
    ids.add(lonString.substring(0,3)+","+latString.substring(0,3));
    ids.add(lonString.substring(0,4)+","+latString.substring(0,4));

    for (int i = 6; i <= latString.length(); i++){
      ids.add(lonString.substring(0,i)+","+latString.substring(0,i));
    }

    return ids;
  }

  private String format(BigDecimal num, int numDecimals) {
    String part1;
    String part2;

    //get string
    //BigDecimal bdNum = new BigDecimal(Math.abs(num.doubleValue())).setScale(numDecimals, RoundingMode.FLOOR);
    BigDecimal bdNum = num.abs();
    String numString = bdNum.toString();

    //split on decimal
    String[] parts = numString.split("\\.");

    //pad to 3 places with 0 for everything left of the decimal
    part1 = StringUtils.leftPad(parts[0], 3, '0');

    //add plus or minus
    if(num.doubleValue() < 0) {
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

    return part1+"."+part2;
  }

}
