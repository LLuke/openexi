package org.openexi.proc.io;

import java.io.IOException;
import java.io.OutputStream;

import org.openexi.proc.common.QName;
import org.openexi.proc.util.ExiUriConst;
import org.openexi.schema.EXISchema;

final class BooleanValueScriber extends ValueScriberBase {
  
  public static final BooleanValueScriber instance;
  static {
    instance = new BooleanValueScriber();
  }
  
  private BooleanValueScriber() {
    super(new QName("exi:boolean", ExiUriConst.W3C_2009_EXI_URI));
  }
  
  @Override
  public short getCodecID() {
    return Scriber.CODEC_BOOLEAN;
  }
  
  @Override
  public int getBuiltinRCS(int simpleType, Scriber scriber) {
    return BuiltinRCS.RCS_ID_BOOLEAN;
  }

  ////////////////////////////////////////////////////////////
  
  @Override
  public boolean process(String value, int tp, EXISchema schema, Scribble scribble, Scriber scriber) {
    if (!trimWhitespaces(value))
      return false;
    
    final boolean distinguishLexicalValues = scribble.booleanValue2 = schema.isPatternedBooleanSimpleType(tp);
    
    final int len = limitPosition - startPosition;
    if (len == 4 && value.charAt(startPosition) == 't' && value.charAt(startPosition + 1) == 'r' &&
        value.charAt(startPosition + 2) == 'u' && value.charAt(startPosition + 3) == 'e') {
      if (distinguishLexicalValues)
        scribble.intValue1 = 2;
      else
        scribble.booleanValue1 = true;
    }
    else if (len == 5 && value.charAt(startPosition) == 'f' && value.charAt(startPosition + 1) == 'a' &&
        value.charAt(startPosition + 2) == 'l' && value.charAt(startPosition + 3) == 's' && 
        value.charAt(startPosition + 4) == 'e') {
      if (distinguishLexicalValues)
        scribble.intValue1 = 0;
      else
        scribble.booleanValue1 = false;
    }
    else if (len == 1) {
      char c;
      if ((c = value.charAt(startPosition)) == '1') {
        if (distinguishLexicalValues)
          scribble.intValue1 = 3;
        else
          scribble.booleanValue1 = true;
      }
      else if (c == '0') {
        if (distinguishLexicalValues)
          scribble.intValue1 = 1;
        else
          scribble.booleanValue1 = false;
      }
      else
        return false;
    }
    else
      return false;
    
    return true;
  }

  @Override
  public void scribe(String value, Scribble scribble, int localName, int uri, int tp, OutputStream channelStream, Scriber scriber) throws IOException {
    if (scribble.booleanValue2)
      scriber.writeNBitUnsigned(scribble.intValue1, 2, channelStream);
    else 
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
    if (scribble.booleanValue2)
      return new BooleanValue(true, true, (byte)scribble.intValue1); 
    else
      return new BooleanValue(false, scribble.booleanValue1, (byte)-1);
  }

  @Override
  public void doScribe(Object value, int localName, int uri, int tp, OutputStream channelStream, Scriber scriber) throws IOException {
    final BooleanValue booleanValue = (BooleanValue)value;
    if (booleanValue.distinguishLexicalValues)
      scriber.writeNBitUnsigned(booleanValue.lexicalValueId, 2, channelStream);
    else
      scriber.writeBoolean(booleanValue.booleanValue, channelStream);
  }

}
