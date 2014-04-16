package org.openexi.fujitsu.proc.io;

import org.openexi.fujitsu.proc.util.Base64;
import org.openexi.fujitsu.schema.EXISchema;

final class Base64BinaryValueScriber extends BinaryValueScriber {
  
  public Base64BinaryValueScriber(Scriber scriber) {
    super(scriber, "exi:base64Binary");
  }

  @Override
  public short getCodecID() {
    return Scriber.CODEC_BASE64BINARY;
  }
  
  @Override
  public int getBuiltinRCS(int simpleType) {
    return BuiltinRCS.RCS_ID_BASE64BINARY;
  }
  
  ////////////////////////////////////////////////////////////

  @Override
  public boolean process(String value, int tp, EXISchema schema, Scribble scribble) {
    final int len = value.length();
    int n_maxBytes = (len >>> 2) * 3;
    if ((len & 0x03) != 0)
      n_maxBytes += 3;
    final byte[] binaryValue = expandOctetArray(n_maxBytes, scribble.binaryValue);
    final int n_bytes;
    if ((n_bytes = Base64.decode(value, binaryValue)) == -1)
      return false;
    scribble.intValue1 = n_bytes;
    scribble.binaryValue = binaryValue;
    return true;
  }

}
