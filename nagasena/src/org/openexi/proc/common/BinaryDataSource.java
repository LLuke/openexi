package org.openexi.proc.common;

import java.io.IOException;

public final class BinaryDataSource {

  private byte[] m_byteArray;
  private int m_startIndex;
  private int m_length;
  
  private long m_n_remainingBytes;
  
  private IBinaryValueScanner m_scanner;

  public final byte[] getByteArray() {
    return m_byteArray;
  }
  
  public final int getStartIndex() {
    return m_startIndex;
  }

  public final int getLength() {
    return m_length;
  }
  
  public final long getRemainingBytesCount() {
    return m_n_remainingBytes;
  }

  /**
   * @y.exclude
   */
  public void setValues(byte[] byteArray, int startIndex, int length, IBinaryValueScanner scanner, long n_remainingBytes) {
    m_byteArray = byteArray;
    m_startIndex = startIndex;
    m_length = length;
    m_n_remainingBytes = n_remainingBytes;
    m_scanner = scanner;
  }
  
  public boolean hasNext() {
    return m_n_remainingBytes > 0;
  }
  
  public int next() throws IOException {
    if (m_n_remainingBytes > 0) {
      m_scanner.scan(m_n_remainingBytes, this);
      return m_length;
    }
    return -1;
  }
  
}
