package org.openexi.proc.common;

public class BinaryDataUtil {
  
  public static byte[] makeBytes(BinaryDataSource binaryData) {
    final byte[] bts = new byte[binaryData.getLength()];
    System.arraycopy(binaryData.getByteArray(), binaryData.getStartIndex(), bts, 0, binaryData.getLength());
    return bts;
  }

}
