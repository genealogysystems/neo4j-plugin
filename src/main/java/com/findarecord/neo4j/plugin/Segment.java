package com.findarecord.neo4j.plugin;

/**
 * Represents a segment node in the graph.
 */
public class Segment {
  private char x;
  private char y;
  private String id;

  public Segment(char x, char y, String prefix) {
    this.x = x;
    this.y = y;
    this.id = prefix+x+","+y+":";
  }

  public char getX() {
    return x;
  }

  public int getXInt() {
    return Character.digit(x,10);
  }

  public char getY() {
    return y;
  }

  public int getYInt() {
    return Character.digit(y,10);
  }

  @Override
  public String toString() {
      return x+","+y;
  }

  public String getId() {
    return id;
  }
}
