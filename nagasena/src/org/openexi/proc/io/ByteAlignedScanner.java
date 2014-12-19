package org.openexi.proc.io;

import java.io.IOException;
import java.io.InputStream;

import org.openexi.proc.common.AlignmentType;

final class ByteAlignedScanner extends SimpleScanner {

  /*
   * Constructor is intentionally non-public.
   * Use ScannerFactory to instantiate a ByteAlignedScanner.
   */
  ByteAlignedScanner() {
    super(false);
  }
  
  @Override
  public final AlignmentType getAlignmentType() {
    return AlignmentType.byteAligned;
  }

  @Override
  protected boolean readBoolean(InputStream istream) throws IOException {
    // use m_inputStream irrespective of istream
    return ByteAlignedCommons.readBoolean(m_inputStream);
  }

  @Override
  protected int readNBitUnsigned(int width, InputStream istream) throws IOException {
    // use m_inputStream irrespective of istream
    if (width != 0) {
      return ByteAlignedCommons.readNBitUnsigned(width, m_inputStream);
    }
    return 0;
  }
  
  @Override
  protected int readEightBitsUnsigned(InputStream istream) throws IOException {
    return ByteAlignedCommons.readEightBitsUnsigned(m_inputStream);
  }
}
