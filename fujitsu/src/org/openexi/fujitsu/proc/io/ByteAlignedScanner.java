package org.openexi.fujitsu.proc.io;

import java.io.IOException;
import java.io.InputStream;

import org.openexi.fujitsu.proc.common.AlignmentType;

public final class ByteAlignedScanner extends SimpleScanner {

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
  public int readNBitUnsigned(int width, InputStream istream) throws IOException {
    // use m_inputStream irrespective of istream
    return ByteAlignedCommons.readNBitUnsigned(width, m_inputStream);
  }
  
}
