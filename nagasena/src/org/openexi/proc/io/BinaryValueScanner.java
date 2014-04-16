package org.openexi.proc.io;

import java.io.IOException;

import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.BinaryDataSource;
import org.openexi.proc.common.IBinaryValueScanner;
import org.openexi.proc.common.QName;

public abstract class BinaryValueScanner extends ValueScannerBase implements IBinaryValueScanner {

  protected final Scanner m_scanner;

  // REVISIT: Can we abandon m_octets?
  protected byte[] m_octets;
  private final OctetBuffer m_octetBuffer;
  private final boolean m_resetBeforeScan;
  
  public BinaryValueScanner(QName qname, Scanner scanner) {
    super(qname);
    m_scanner = scanner;
    m_octets = new byte[8192];
    m_octetBuffer = m_scanner.octetBuffer;
    final AlignmentType alignmentType = scanner.getAlignmentType();
    m_resetBeforeScan = alignmentType == AlignmentType.bitPacked || alignmentType == AlignmentType.byteAligned; 
  }

  protected final void expandBuffer(int len) {
    int _length;
    for (_length = m_octets.length << 1; _length < len; _length <<= 1);
    m_octets = new byte[_length];
  }

  ///////////////////////////////////////////////////////////////////////////
  /// IBinaryValueScanner methods
  ///////////////////////////////////////////////////////////////////////////

  public final BinaryDataSource scan(long n_remainingBytes, BinaryDataSource binaryDataSource) throws IOException {
    if (n_remainingBytes == -1) { // i.e. head chunk
      n_remainingBytes = m_scanner.readUnsignedIntegerAsLong(m_istream);
    }
    int n_nextBytes = m_scanner.m_binaryChunkSize;
    if (n_nextBytes == -1) // i.e. unspecified n_nextBytes 
      n_nextBytes = n_remainingBytes > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)n_remainingBytes;  
    else if (n_nextBytes > n_remainingBytes)
      n_nextBytes = (int)n_remainingBytes;
    if (m_resetBeforeScan)
      m_octetBuffer.nextIndex = 0;
    m_octetBuffer.ensureOctets(n_nextBytes);
    final int offset = m_octetBuffer.allocOctets(n_nextBytes);
    final byte[] octets = m_octetBuffer.octets; 
    for (int i = 0; i < n_nextBytes; i++) {
      octets[offset + i] = (byte)m_scanner.readEightBitsUnsigned(m_istream);
    }
    if (binaryDataSource == null)
      binaryDataSource = new BinaryDataSource();
    binaryDataSource.setValues(octets, offset, n_nextBytes, this, n_remainingBytes - n_nextBytes);
    return binaryDataSource;
  }
  
}
