package com.findarecord.neo4j.plugin;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;


public class Box {

  private int precision = 3;

  private String id;
  private ArrayList<Segment> segments;
  private String x;
  private String y;

  public Box(String x, String y) {
    this.x = x;
    this.y = y;

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

  public String getId() {
    return id;
  }

  private ArrayList<Segment> createSegments(String x, String y) {
    ArrayList<Segment> newSegments = new ArrayList<Segment>();

    //pad x
    x = prepString(x);

    //pad y
    y = prepString(y);

    //loop through and create segments
    for (int i = 0; i < x.length(); i++){
      newSegments.add(new Segment(x.charAt(i),y.charAt(i)));
    }

    return newSegments;
  }

  private String prepString(String str) {
    String part1;
    String part2 = "";

    String[] parts = str.split("\\.");

    //if no period, pad to 3 places with 0 and add up to significant chars
    part1 = StringUtils.leftPad(parts[0], 3, '0');
    if(parts.length == 2) {
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
