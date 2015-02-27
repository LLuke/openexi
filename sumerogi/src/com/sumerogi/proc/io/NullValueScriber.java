package com.sumerogi.proc.io;

import java.io.IOException;
import java.io.OutputStream;

public final class NullValueScriber extends ValueScriberBase {
  
  public static final NullValueScriber instance;
  static {
    instance = new NullValueScriber();
  }

  @Override
  public boolean process(char[] value, int offset, int length, Scribble scribble, Scriber scriber) {
    assert false;
    return false;
  }

  @Override
  public void scribe(String value, Scribble scribble, int localName, OutputStream channelStream, Scriber scriber) 
    throws IOException {
  }

  @Override
  public Object toValue(String value, Scribble scribble, Scriber scriber) {
    return null;
  }

  @Override
  public void doScribe(Object value, int localName, OutputStream channelStream, Scriber scriber) 
    throws IOException {
  }

}
