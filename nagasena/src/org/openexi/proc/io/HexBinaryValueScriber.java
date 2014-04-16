package org.openexi.proc.io;

import org.openexi.proc.grammars.Apparatus;
import org.openexi.schema.EXISchema;
import org.openexi.schema.HexBin;

public final class HexBinaryValueScriber extends BinaryValueScriber {
  
  public static final HexBinaryValueScriber instance;
  static {
    instance = new HexBinaryValueScriber();
  }

  private HexBinaryValueScriber() {
    super("exi:hexBinary");
  }

  @Override
  public short getCodecID() {
    return Apparatus.CODEC_HEXBINARY;
  }
  
  @Override
  public int getBuiltinRCS(int simpleType, Scriber scriber) {
    return BuiltinRCS.RCS_ID_HEXBINARY;
  }
  
  ////////////////////////////////////////////////////////////
  
  @Override
  public boolean process(String value, int tp, EXISchema schema, Scribble scribble, Scriber scriber) {
    final int len = value.length();
    int n_maxBytes = len >>> 1;
    final byte[] binaryValue = scribble.expandOctetArray(n_maxBytes);
    final int n_bytes;
    if ((n_bytes = HexBin.decode(value, binaryValue)) == -1)
      return false;
    scribble.intValue1 = n_bytes;
    scribble.binaryValue = binaryValue;
    return true;
  }

}
