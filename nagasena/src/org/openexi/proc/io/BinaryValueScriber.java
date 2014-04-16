package org.openexi.proc.io;

import java.io.IOException;
import java.io.OutputStream;

import org.openexi.proc.common.QName;
import org.openexi.proc.util.ExiUriConst;

public abstract class BinaryValueScriber extends ValueScriberBase implements BinaryDataSink {

  BinaryValueScriber(String codecName) {
    super(new QName(codecName, ExiUriConst.W3C_2009_EXI_URI));
  }
  
  @Override
  public final void scribe(String value, Scribble scribble, int localName, int uri, int tp, OutputStream channelStream, Scriber scriber) throws IOException {
    scribeBinaryValue(scribble.binaryValue, scribble.intValue1, channelStream, scriber);
  }
  
  @Override
  public final Object toValue(String value, Scribble scribble, Scriber scriber) {
    final int n_bytes;
    if ((n_bytes = scribble.intValue1) != 0) {
      final byte[] bts = new byte[n_bytes];
      System.arraycopy(scribble.binaryValue, 0, bts, 0, n_bytes);
      return bts;
    }
    else
      return null;
  }

  @Override
  public final void doScribe(Object value, int localName, int uri, int tp, OutputStream channelStream, Scriber scriber) throws IOException  {
    final byte[] bts = (byte[])value;
    scribeBinaryValue(bts, bts != null ? bts.length : 0, channelStream, scriber);
  }

  ///////////////////////////////////////////////////////////////////////////
  // Methods used for directly writing binary values.
  ///////////////////////////////////////////////////////////////////////////
  
  public final void startBinaryData(long totalSize, Scribble scribble, Scriber scriber) throws IOException {
    scribble.longValue = totalSize;
    if ((totalSize & (Long.MIN_VALUE >> 31)) != 0)
      scriber.writeUnsignedInteger64(totalSize, null);
    else
      scriber.writeUnsignedInteger32((int)totalSize, null);
  }

  public final void binaryData(byte[] byteArray, int offset, int length, Scribble scribble, Scriber scriber) throws IOException {
    for (int i = 0; i < length; i++) {
      scriber.writeNBitUnsigned(byteArray[offset + i], 8, null);
    }
    scribble.longValue -= length;
  }

  public final void endBinaryData(Scribble scribble, int localName, int uri, Scriber scriber) throws ScriberRuntimeException {
    if (scribble.longValue != 0) {
      throw new ScriberRuntimeException(ScriberRuntimeException.BINARY_DATA_SIZE_MISMATCH);
    }
  }

  ////////////////////////////////////////////////////////////

  private void scribeBinaryValue(byte[] bts, int n_bytes, OutputStream ostream, Scriber scriber) throws IOException {
    scriber.writeUnsignedInteger32(n_bytes, ostream);
    for (int i = 0; i < n_bytes; i++) {
      scriber.writeNBitUnsigned(bts[i], 8, ostream);
    }
  }

}
