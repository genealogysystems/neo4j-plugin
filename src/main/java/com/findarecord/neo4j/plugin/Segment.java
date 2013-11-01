package com.findarecord.neo4j.plugin;

/**
 * Represents a segment node in the graph.
 */
public class Segment {
  private char x;
  private char y;
  private String id;

  public Segment(char x, char y) {
    this.x = x;
    this.y = y;
    this.id = "";
  }

  public char getX() {
    return x;
  }

  public void setX(char x) {
    this.x = x;
  }

  public char getY() {
    return y;
  }

  public void setY(char y) {
    this.y = y;
  }

  @Override
  public String toString() {
      return x+","+y;
  }

  public String getId() {
    return id;
  }
}
