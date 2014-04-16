package org.openexi.proc.io;

import java.io.IOException;
import java.io.OutputStream;

import org.openexi.proc.grammars.ValueApparatus;
import org.openexi.schema.EXISchema;

public abstract class ValueScriber extends ValueApparatus {

  public abstract int getBuiltinRCS(int simpleType, Scriber scriber);
  
  ////////////////////////////////////////////////////////////
  
  public abstract boolean process(String value, int tp, EXISchema schema, Scribble scribble, Scriber scriber);
  
  public abstract void scribe(String value, Scribble scribble, int localName, int uri, int tp, Scriber scriber) throws IOException;
  
  /**
   * ScriberValueHolder calls this method to write out an Object to a channelStream.
   */
  public abstract void scribe(String value, Scribble scribble, int localName, int uri, int tp, OutputStream channelStream, Scriber scriber) throws IOException;
  
  ////////////////////////////////////////////////////////////
  
  public abstract Object toValue(String value, Scribble scribble, Scriber scriber);

  public abstract void doScribe(Object value, int localName, int uri, int tp, OutputStream channelStream, Scriber scriber) throws IOException;

}
