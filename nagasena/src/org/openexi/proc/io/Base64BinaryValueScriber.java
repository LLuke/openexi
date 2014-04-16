package org.openexi.proc.io;

import org.openexi.proc.grammars.Apparatus;
import org.openexi.schema.Base64;
import org.openexi.schema.EXISchema;

public final class Base64BinaryValueScriber extends BinaryValueScriber {
  
  public static final Base64BinaryValueScriber instance;
  static {
    instance = new Base64BinaryValueScriber();
  }
  
  private Base64BinaryValueScriber() {
    super("exi:base64Binary");
  }

  @Override
  public short getCodecID() {
    return Apparatus.CODEC_BASE64BINARY;
  }
  
  @Override
  public int getBuiltinRCS(int simpleType, Scriber scriber) {
    return BuiltinRCS.RCS_ID_BASE64BINARY;
  }
  
  ////////////////////////////////////////////////////////////

  @Override
  public boolean process(String value, int tp, EXISchema schema, Scribble scribble, Scriber scriber) {
    final int len = value.length();
    int n_maxBytes = (len >>> 2) * 3;
    if ((len & 0x03) != 0)
      n_maxBytes += 3;
    final byte[] binaryValue = scribble.expandOctetArray(n_maxBytes);
    final int n_bytes;
    if ((n_bytes = Base64.decode(value, binaryValue)) == -1)
      return false;
    scribble.intValue1 = n_bytes;
    scribble.binaryValue = binaryValue;
    return true;
  }

}
