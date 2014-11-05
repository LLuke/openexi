package com.sumerogi.proc.io;

import java.io.IOException;
import java.io.OutputStream;

final class BooleanValueScriber extends ValueScriberBase {
  
  public static final BooleanValueScriber instance;
  static {
    instance = new BooleanValueScriber();
  }
  
  private BooleanValueScriber() {
  }
  
  ////////////////////////////////////////////////////////////

  @Override
  public boolean process(char[] value, int offset, int length, Scribble scribble, Scriber scriber) {
    if (!trimWhitespaces(value, offset, length))
      return false;
    
    final int len = limitPosition - startPosition;
    if (len == 4 && value[startPosition] == 't' && value[startPosition + 1] == 'r' &&
        value[startPosition + 2] == 'u' && value[startPosition + 3] == 'e') {
      scribble.booleanValue1 = true;
    }
    else if (len == 5 && value[startPosition] == 'f' && value[startPosition + 1] == 'a' &&
        value[startPosition + 2] == 'l' && value[startPosition + 3] == 's' && 
        value[startPosition + 4] == 'e') {
      scribble.booleanValue1 = false;
    }
    else
      return false;
    
    return true;
  }
  
  @Override
  public void scribe(String value, Scribble scribble, int localName, OutputStream channelStream, Scriber scriber) throws IOException {
//    if (scribble.booleanValue2)
//      scriber.writeNBitUnsigned(scribble.intValue1, 2, channelStream);
//    else 
    scriber.writeBoolean(scribble.booleanValue1, channelStream);
  }

  private static class BooleanValue {
    boolean distinguishLexicalValues;
    boolean booleanValue;
    byte lexicalValueId;
    BooleanValue(boolean preserveLexicalValue, boolean booleanValue, byte lexicalValueId) {
      this.distinguishLexicalValues = preserveLexicalValue;
      this.booleanValue = booleanValue;
      this.lexicalValueId = lexicalValueId;
    }
  }

  @Override
  public Object toValue(String value, Scribble scribble, Scriber scriber) {
//    if (scribble.booleanValue2)
//      return new BooleanValue(true, true, (byte)scribble.intValue1); 
//    else
    return new BooleanValue(false, scribble.booleanValue1, (byte)-1);
  }

  @Override
  public void doScribe(Object value, int localName, OutputStream channelStream, Scriber scriber) throws IOException {
    final BooleanValue booleanValue = (BooleanValue)value;
    if (booleanValue.distinguishLexicalValues)
      scriber.writeNBitUnsigned(booleanValue.lexicalValueId, 2, channelStream);
    else
      scriber.writeBoolean(booleanValue.booleanValue, channelStream);
  }

}
