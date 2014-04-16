package org.openexi.proc.io;

import java.io.InputStream;
import java.io.EOFException;
import java.io.IOException;

/**
 * An implementation of BitInputStream representing the EXI body of
 * a bit-packed stream.
 */
public final class BodyBitInputStream extends BitInputStream {

  private static final int BYTEBUFFER_LENGTH = 8192;
  private final byte[] m_bytes;
  private int m_bufLen;
  private int m_curPos;

  BodyBitInputStream() {
    m_bytes = new byte[BYTEBUFFER_LENGTH];
    m_curPos = m_bufLen = 0;
  }
  
  void setInputStream(InputStream in) {
    this.in = in;
    getCount = 0;
    bitBuf = 0;
    m_curPos = m_bufLen = 0;
  }

  void inheritResidue(HeaderOptionsInputStream inputStreamDirect) {
    in = inputStreamDirect.in;
    getCount = inputStreamDirect.getCount;
    bitBuf = inputStreamDirect.bitBuf;
    m_curPos = m_bufLen = 0;
  }
  
  private int fill() throws IOException {
    assert m_curPos == m_bufLen;
    m_curPos = 0;
    if ((m_bufLen = in.read(m_bytes, 0, BYTEBUFFER_LENGTH)) == -1) {
      m_bufLen = 0;
      return -1; /** EOF */
    }
    return m_bufLen;
  }

  @Override
  public int read() throws IOException {
    if (m_curPos == m_bufLen && fill() == -1) {
      return -1;
    }
    return m_bytes[m_curPos++];
  }

  /**
   * Retrieves a single bit.
   */
  public boolean getBit() throws IOException {
    if (--getCount >= 0)
      return ((bitBuf >>> getCount) & 1) == 1;
    getCount = 7;
    if (m_curPos == m_bufLen && fill() == -1) {
      throw new EOFException();
    }      
    bitBuf = m_bytes[m_curPos++];
    return ((bitBuf >>> 7) & 1) == 1;
  }

  /**
   * Retrieves n-bits where 0 < n <= 32. 
   */
  public int getBits(int n) throws IOException {
    int x = 0;
    while (n > getCount) {
      /**
       * NOTE: used to be the following next two lines.
       * n -= getCount;
       * x |= rightBits(getCount, bitBuf) << n;
       */
      x |= (bitBuf & ((1 << getCount) - 1)) << (n -= getCount);
      if (m_curPos == m_bufLen && fill() == -1) {
        throw new EOFException();
      }      
      bitBuf = m_bytes[m_curPos++];
      getCount = 8;
    }
    /**
     * NOTE: used to be the following next two lines.
     * getCount -= n;
     * return x | rightBits(n, bitBuf >>> getCount);  
     */
    return x | (bitBuf >>> (getCount -= n)) & ((1 << n) - 1);
  }
  
  /**
   * DON't remove this comment!
   * Returns the right-most n-bits of x
   * private static int rightBits(int n, int x) {
   *   return x & ((1 << n) - 1);
   * }
   */
  
}
