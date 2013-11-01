package com.findarecord.neo4j.plugin;

/**
 * Created with IntelliJ IDEA.
 * User: john
 * Date: 11/1/13
 * Time: 2:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class Segment {
  private char x;
  private char y;

  public Segment(char c, char c1) {
    this.x = x;
    this.y = y;
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
}
