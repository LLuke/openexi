package org.openexi.fujitsu.proc.io;

import java.io.IOException;
import java.io.OutputStream;

import org.openexi.fujitsu.schema.EXISchema;

public abstract class ValueScriber extends ValueApparatus {

  protected final Scriber m_scriber;
  
  protected ValueScriber(Scriber scriber) {
    m_scriber = scriber;
  }

  public abstract int getBuiltinRCS(int simpleType);
  
  ////////////////////////////////////////////////////////////
  
  public abstract boolean process(String value, int tp, EXISchema schema, Scribble scribble);
  
  public abstract void scribe(String value, Scribble scribble, String localName, String uri, int tp) throws IOException;
  
  public abstract void scribe(String value, Scribble scribble, String localName, String uri, int tp, OutputStream channelStream) throws IOException;
  
  ////////////////////////////////////////////////////////////
  
  public abstract Object toValue(String value, Scribble scribble);

  /**
   * ScriberValueHolder calls this method to write out an Object to a channelStream.
   */
  public abstract void doScribe(Object value, String localName, String uri, int tp, OutputStream channelStream) throws IOException;

  ////////////////////////////////////////////////////////////
  
  public void setEXISchema(EXISchema schema) {
  }
  
  public void setStringTable(StringTable stringTable) {
  }

  public void setValueMaxLength(int valueMaxLength) {
  }

}
