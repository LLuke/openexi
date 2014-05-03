using System;

namespace Nagasena.Proc.Common {

  public class BinaryDataUtil {

    public static byte[] makeBytes(BinaryDataSource binaryData) {
      byte[] bts = new byte[binaryData.Length];
      Array.Copy(binaryData.ByteArray, binaryData.StartIndex, bts, 0, binaryData.Length);
      return bts;
    }

  }

}