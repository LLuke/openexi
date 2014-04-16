using System;
using System.IO;

using QName = Nagasena.Proc.Common.QName;
using ExiUriConst = Nagasena.Proc.Util.ExiUriConst;

namespace Nagasena.Proc.IO {

  internal abstract class BinaryValueScriber : ValueScriberBase, BinaryDataSink {

    internal BinaryValueScriber(string codecName) : base(new QName(codecName, ExiUriConst.W3C_2009_EXI_URI)) {
    }

    public override sealed void scribe(string value, Scribble scribble, int localName, int uri, int tp, Stream channelStream, Scriber scriber) {
      scribeBinaryValue(scribble.binaryValue, scribble.intValue1, channelStream, scriber);
    }

    public override sealed object toValue(string value, Scribble scribble, Scriber scriber) {
      int n_bytes;
      if ((n_bytes = scribble.intValue1) != 0) {
        byte[] bts = new byte[n_bytes];
        Array.Copy(scribble.binaryValue, 0, bts, 0, n_bytes);
        return bts;
      }
      else {
        return null;
      }
    }

    public override sealed void doScribe(object value, int localName, int uri, int tp, Stream channelStream, Scriber scriber) {
      byte[] bts = (byte[])value;
      scribeBinaryValue(bts, bts != null ? bts.Length : 0, channelStream, scriber);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Methods used for directly writing binary values.
    ///////////////////////////////////////////////////////////////////////////

    public void startBinaryData(long totalSize, Scribble scribble, Scriber scriber) {
      scribble.longValue = totalSize;
      if ((totalSize & (long.MinValue >> 31)) != 0) {
        scriber.writeUnsignedInteger64(totalSize, null);
      }
      else {
        scriber.writeUnsignedInteger32((int)totalSize, null);
      }
    }

    public void binaryData(byte[] byteArray, int offset, int length, Scribble scribble, Scriber scriber) {
      for (int i = 0; i < length; i++) {
        scriber.writeNBitUnsigned(byteArray[offset + i], 8, null);
      }
      scribble.longValue -= length;
    }

    public void endBinaryData(Scribble scribble, int localName, int uri, Scriber scriber) {
      if (scribble.longValue != 0) {
        throw new ScriberRuntimeException(ScriberRuntimeException.BINARY_DATA_SIZE_MISMATCH);
      }
    }

    ////////////////////////////////////////////////////////////

    private void scribeBinaryValue(byte[] bts, int n_bytes, Stream ostream, Scriber scriber) {
      scriber.writeUnsignedInteger32(n_bytes, ostream);
      for (int i = 0; i < n_bytes; i++) {
        scriber.writeNBitUnsigned(bts[i], 8, ostream);
      }
    }

  }

}