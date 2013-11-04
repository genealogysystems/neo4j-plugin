package com.findarecord.neo4j.plugin;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;


public class Box {

  private int precision;

  private ArrayList<Segment> segments;

  private String x;

  private String y;

  public Box(String x, String y, int precision) {
    this.x = x;
    this.y = y;
    this.precision = precision;
    this.segments = createSegments(x, y);
  }

  public String getX() {
    return x;
  }

  public String getY() {
    return y;
  }

  public ArrayList<Segment> getSegments() {
    return segments;
  }

  private ArrayList<Segment> createSegments(String x, String y) {
    ArrayList<Segment> newSegments = new ArrayList<Segment>();

    //pad x
    x = prepString(x);

    //pad y
    y = prepString(y);

    //loop through and create segments
    String prefix = "";
    Segment segment;
    for (int i = 0; i < x.length(); i++){
      segment = new Segment(x.charAt(i),y.charAt(i),prefix);
      prefix = segment.getId();
      newSegments.add(segment);
    }

    return newSegments;
  }

  private String prepString(String str) {
    String part1;
    String part2;

    String[] parts = str.split("\\.");

    //pad to 3 places with 0 for everything left of the decimal
    part1 = StringUtils.leftPad(parts[0], 3, '0');

    //if there was a decimal, make sure it is the right length
    //otherwise padd to precision
    if(parts.length == 2) {
      //cut off any extra precision before padding out to "precision" places
      part2 = StringUtils.substring(parts[1], 0, precision);
      part2 = StringUtils.rightPad(part2, precision, '0');
    } else {
      part2 = StringUtils.rightPad("", precision, '0');
    }

    return part1+part2;
  }

  @Override
  public String toString() {
    String ret = "";

    for(Segment s : this.segments) {
        ret += s.toString()+"   ";
    }

    return ret;
  }

}
