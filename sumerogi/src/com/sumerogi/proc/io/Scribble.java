package com.sumerogi.proc.io;

public final class Scribble {
  
  public int intValue1; 
  public int intValue2;

  public long longValue;

  public boolean booleanValue1;
  public boolean booleanValue2;
  
  public String stringValue1;
  public String stringValue2;
  
  public Scribble() {
  }
  
  public Scribble(Scribble scribble) {
    this.intValue1 = scribble.intValue1;
    this.intValue2 = scribble.intValue2;
    this.longValue = scribble.longValue;
    this.booleanValue1 = scribble.booleanValue1;
    this.booleanValue2 = scribble.booleanValue2;
    this.stringValue1 = scribble.stringValue1;
    this.stringValue2 = scribble.stringValue2;
  }
  
  public void clear() {
    intValue1 = intValue2 = 0;
    longValue = 0;
    booleanValue1 = booleanValue2 = false;
    stringValue1 = stringValue2 = null;
  }
  
}
