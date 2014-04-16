package org.openexi.fujitsu.proc.io;

import java.io.IOException;
import java.io.OutputStream;

import org.openexi.fujitsu.proc.common.QName;
import org.openexi.fujitsu.proc.util.URIConst;

abstract class BinaryValueScriber extends ValueScriberBase {

  BinaryValueScriber(Scriber scriber, String codecName) {
    super(scriber, new QName(codecName, URIConst.W3C_2009_EXI_URI));
  }
  
  @Override
  public final void scribe(String value, Scribble scribble, String localName, String uri, int tp, OutputStream channelStream) throws IOException {
    scribeBinaryValue(scribble.binaryValue, scribble.intValue1, channelStream);
  }
  
  @Override
  public final Object toValue(String value, Scribble scribble) {
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
  public final void doScribe(Object value, String localName, String uri, int tp, OutputStream channelStream) throws IOException  {
    final byte[] bts = (byte[])value;
    scribeBinaryValue(bts, bts != null ? bts.length : 0, channelStream);
  }

  ////////////////////////////////////////////////////////////

  private void scribeBinaryValue(byte[] bts, int n_bytes, OutputStream ostream) throws IOException {
    m_scriber.writeUnsignedInteger32(n_bytes, ostream);
    for (int i = 0; i < n_bytes; i++) {
      m_scriber.writeNBitUnsigned(bts[i], 8, ostream);
    }
  }

  /**
   * Expand the octet array if necessary.
   */
  final byte[] expandOctetArray(int n_maxBytes, byte[] octets) {
    int length;
    if (octets != null && (length = octets.length) != 0) {
      if (length < n_maxBytes) {
        do {
          length <<= 1;  
        }
        while (length < n_maxBytes);
      }
      else
        return octets;
    }
    else if (n_maxBytes != 0) {
      length = n_maxBytes;
    }
    else {
      return octets;
    }
    return new byte[length];
  }
  
}
