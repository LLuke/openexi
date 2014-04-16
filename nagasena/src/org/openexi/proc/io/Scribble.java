package org.openexi.proc.io;

import org.openexi.schema.XSDateTime;

public final class Scribble {
  
  public int intValue1; 
  public int intValue2;

  public long longValue;

  public boolean booleanValue1;
  public boolean booleanValue2;
  
  public String stringValue1;
  public String stringValue2;
  
  public Scribble[] listOfScribbles;
  public ValueScriber valueScriber;
  
  public XSDateTime dateTime;
  public byte[] binaryValue;
  
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
    this.dateTime = scribble.dateTime;
    final byte[] srcBinaryValue;
    if ((srcBinaryValue = scribble.binaryValue) != null) {
      final int len = srcBinaryValue.length;
      final byte[] binaryValue = new byte[len];
      System.arraycopy(srcBinaryValue, 0, binaryValue, 0, len);
      this.binaryValue = binaryValue;
    }
    else
      this.binaryValue = null;
    // for now, there is no need for copying listOfScribbles and valueScriber fields.
  }
  
  public void clear() {
    intValue1 = intValue2 = 0;
    longValue = 0;
    booleanValue1 = booleanValue2 = false;
    stringValue1 = stringValue2 = null;
    listOfScribbles = null;
    valueScriber = null;
    dateTime = null;
    binaryValue = null;
  }
  
  /**
   * Expand the byte array if necessary.
   */
  public final byte[] expandOctetArray(int n_maxBytes) {
    int length;
    if (binaryValue != null && (length = binaryValue.length) != 0) {
      if (length < n_maxBytes) {
        do {
          length <<= 1;  
        }
        while (length < n_maxBytes);
      }
      else
        return binaryValue;
    }
    else if (n_maxBytes != 0) {
      length = n_maxBytes;
    }
    else {
      return binaryValue;
    }
    return new byte[length];
  }
  
}
