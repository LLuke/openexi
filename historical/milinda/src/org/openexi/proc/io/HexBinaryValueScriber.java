package org.openexi.proc.io;

import org.openexi.proc.util.HexBin;
import org.openexi.schema.EXISchema;

final class HexBinaryValueScriber extends BinaryValueScriber {

  public HexBinaryValueScriber(Scriber scriber) {
    super(scriber, "exi:hexBinary");
  }

  @Override
  public short getCodecID() {
    return Scriber.CODEC_HEXBINARY;
  }
  
  @Override
  public int getBuiltinRCS(int simpleType) {
    return BuiltinRCS.RCS_ID_HEXBINARY;
  }
  
  ////////////////////////////////////////////////////////////
  
  @Override
  public boolean process(String value, int tp, EXISchema schema, Scribble scribble) {
    final int len = value.length();
    int n_maxBytes = len >>> 1;
    final byte[] binaryValue = expandOctetArray(n_maxBytes, scribble.binaryValue);
    final int n_bytes;
    if ((n_bytes = HexBin.decode(value, binaryValue)) == -1)
      return false;
    scribble.intValue1 = n_bytes;
    scribble.binaryValue = binaryValue;
    return true;
  }

}
