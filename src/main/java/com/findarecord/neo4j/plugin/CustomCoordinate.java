package com.findarecord.neo4j.plugin;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Created with IntelliJ IDEA.
 * User: john
 * Date: 11/5/13
 * Time: 9:17 AM
 * To change this template use File | Settings | File Templates.
 */
public class CustomCoordinate extends Coordinate {

  public String boxX;
  public String boxY;
  public int precision;

  public CustomCoordinate(double x, double y, int precision) {
    super(x*precision,y*precision);

    //TODO fix this
    this.boxX = x+"";
    this.boxY = y+"";
    this.precision = precision;
  }

}
