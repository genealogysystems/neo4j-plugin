package com.findarecord.neo4j.plugin;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: john
 * Date: 11/1/13
 * Time: 2:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class Box {
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
    y = prepString(x);

    //loop through and create segments
    for (int i = 0; i < x.length(); i++){
      newSegments.add(new Segment(x.charAt(i),y.charAt(i)));
    }

    return newSegments;  //To change body of created methods use File | Settings | File Templates.
  }

  private String prepString(String str) {
    String part1;
    String part2 = "";

    String[] parts = str.split("\\.");

    //if no period, pad to 3 places with 0 and add up to significant chars
    part1 = String.format("%03s",parts[0]);
    if(parts.length == 2) {
      part2 = String.format("-%05s",parts[1]);
    }

    return part1+part2;
  }


}
