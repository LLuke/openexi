package com.sumerogi.proc.io;

import java.io.IOException;
import java.io.OutputStream;

import com.sumerogi.proc.grammars.ValueApparatus;

public abstract class ValueScriber extends ValueApparatus {

  ////////////////////////////////////////////////////////////
  
  public abstract boolean process(char[] value, int offset, int length, Scribble scribble, Scriber scriber);
  
  public abstract void scribe(String value, Scribble scribble, int localName, Scriber scriber) throws IOException;
  
  /**
   * ScriberValueHolder calls this method to write out an Object to a channelStream.
   */
  public abstract void scribe(String value, Scribble scribble, int localName, OutputStream channelStream, Scriber scriber) throws IOException;
  
  ////////////////////////////////////////////////////////////
  
  public abstract Object toValue(String value, Scribble scribble, Scriber scriber);

  public abstract void doScribe(Object value, int localName, OutputStream channelStream, Scriber scriber) throws IOException;

}
