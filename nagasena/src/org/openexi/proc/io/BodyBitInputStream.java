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
    nBits = 0;
    bitBuf = 0;
    m_curPos = m_bufLen = 0;
  }

  void inheritResidue(HeaderOptionsInputStream inputStreamDirect) {
    in = inputStreamDirect.in;
    nBits = inputStreamDirect.nBits;
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

  /**
   * Retrieves a single bit.
   */
  @Override
  public boolean getBit() throws IOException {
    if (--nBits != -1)
      return ((bitBuf >>> nBits) & 1) == 1;
    if (m_curPos == m_bufLen && fill() == -1)
      throw new EOFException();
    nBits = 7;
    bitBuf = m_bytes[m_curPos++] & 0xFF;
    return ((bitBuf >>> 7) & 1) == 1;
  }

  /**
   * Retrieves n-bits where 0 < n <= 32. 
   */
  @Override
  public int getBits(int n) throws IOException {
    int x = 0;
    while (n > nBits) {
      /**
       * NOTE: used to be the following next two lines.
       * n -= getCount;
       * x |= rightBits(getCount, bitBuf) << n;
       */
      x |= (bitBuf & ((1 << nBits) - 1)) << (n -= nBits);
      if (m_curPos == m_bufLen && fill() == -1) {
        throw new EOFException();
      }      
      bitBuf = m_bytes[m_curPos++] & 0xFF;
      nBits = 8;
    }
    /**
     * NOTE: used to be the following next two lines.
     * getCount -= n;
     * return x | rightBits(n, bitBuf >>> getCount);  
     */
    return x | (bitBuf >>> (nBits -= n)) & ((1 << n) - 1);
  }

  @Override
  public int getOneBit() throws IOException {
    switch (nBits) {
	    case 0:
        if (m_curPos == m_bufLen && fill() == -1)
          throw new EOFException();
        bitBuf = m_bytes[m_curPos++];
	      nBits = 7;
	      return (bitBuf >>> 7) & 0x01;
    	case 1:
	      nBits = 0;
	      return bitBuf & 0x01;
	    case 2:
	    	nBits = 1;
	    	return (bitBuf >>> 1) & 0x01;
	    case 3:
	    	nBits = 2;
	    	return (bitBuf >>> 2) & 0x01;
	    case 4:
	    	nBits = 3;
	    	return (bitBuf >>> 3) & 0x01;
	    case 5:
	    	nBits = 4;
	    	return (bitBuf >>> 4) & 0x01;
	    case 6:
	    	nBits = 5;
	    	return (bitBuf >>> 5) & 0x01;
	    case 7:
	    	nBits = 6;
	    	return (bitBuf >>> 6) & 0x01;
	    case 8:
	    	nBits = 7;
	    	return (bitBuf >>> 7) & 0x01;
	    default:
	      assert false;
	      return -1;
	  }
  }

  @Override
  public int getTwoBits() throws IOException {
    switch (nBits) {
	    case 0:
        if (m_curPos == m_bufLen && fill() == -1)
          throw new EOFException();
        bitBuf = m_bytes[m_curPos++];
	      nBits = 6;
	      return (bitBuf >>> 6) & 0x03;
    	case 1:
        final int x = (bitBuf & 0x01) << 1;
        if (m_curPos == m_bufLen && fill() == -1)
          throw new EOFException();
        bitBuf = m_bytes[m_curPos++];
	      nBits = 7;
	      return x | ((bitBuf >>> 7) & 0x01);
	    case 2:
	    	nBits = 0;
	    	return bitBuf & 0x03;
	    case 3:
	    	nBits = 1;
	    	return (bitBuf >>> 1) & 0x03;
	    case 4:
	    	nBits = 2;
	    	return (bitBuf >>> 2) & 0x03;
	    case 5:
	    	nBits = 3;
	    	return (bitBuf >>> 3) & 0x03;
	    case 6:
	    	nBits = 4;
	    	return (bitBuf >>> 4) & 0x03;
	    case 7:
	    	nBits = 5;
	    	return (bitBuf >>> 5) & 0x03;
	    case 8:
	    	nBits = 6;
	    	return (bitBuf >>> 6) & 0x03;
	    default:
	      assert false;
	      return -1;
	  }
  }

  @Override
  public int getThreeBits() throws IOException {
  	final int x;
    switch (nBits) {
	    case 0:
        if (m_curPos == m_bufLen && fill() == -1)
          throw new EOFException();
        bitBuf = m_bytes[m_curPos++];
	      nBits = 5;
	      return (bitBuf >>> 5) & 0x07;
    	case 1:
        x = (bitBuf & 0x01) << 2;
        if (m_curPos == m_bufLen && fill() == -1)
          throw new EOFException();
        bitBuf = m_bytes[m_curPos++];
	      nBits = 6;
	      return x | ((bitBuf >>> 6) & 0x03);
	    case 2:
        x = (bitBuf & 0x03) << 1;
        if (m_curPos == m_bufLen && fill() == -1)
          throw new EOFException();
        bitBuf = m_bytes[m_curPos++];
	      nBits = 7;
	      return x | ((bitBuf >>> 7) & 0x01);
	    case 3:
	    	nBits = 0;
	    	return bitBuf & 0x07;
	    case 4:
	    	nBits = 1;
	    	return (bitBuf >>> 1) & 0x07;
	    case 5:
	    	nBits = 2;
	    	return (bitBuf >>> 2) & 0x07;
	    case 6:
	    	nBits = 3;
	    	return (bitBuf >>> 3) & 0x07;
	    case 7:
	    	nBits = 4;
	    	return (bitBuf >>> 4) & 0x07;
	    case 8:
	    	nBits = 5;
	    	return (bitBuf >>> 5) & 0x07;
	    default:
	      assert false;
	      return -1;
	  }
  }

  @Override
  public int getFourBits() throws IOException {
  	final int x;
    switch (nBits) {
	    case 0:
        if (m_curPos == m_bufLen && fill() == -1)
          throw new EOFException();
        bitBuf = m_bytes[m_curPos++];
	      nBits = 4;
	      return (bitBuf >>> 4) & 0x0F;
    	case 1:
        x = (bitBuf & 0x01) << 3;
        if (m_curPos == m_bufLen && fill() == -1)
          throw new EOFException();
        bitBuf = m_bytes[m_curPos++];
	      nBits = 5;
	      return x | ((bitBuf >>> 5) & 0x07);
	    case 2:
        x = (bitBuf & 0x03) << 2;
        if (m_curPos == m_bufLen && fill() == -1)
          throw new EOFException();
        bitBuf = m_bytes[m_curPos++];
	      nBits = 6;
	      return x | ((bitBuf >>> 6) & 0x03);
	    case 3:
        x = (bitBuf & 0x07) << 1;
        if (m_curPos == m_bufLen && fill() == -1)
          throw new EOFException();
        bitBuf = m_bytes[m_curPos++];
	      nBits = 7;
	      return x | ((bitBuf >>> 7) & 0x01);
	    case 4:
	    	nBits = 0;
	    	return bitBuf & 0x0F;
	    case 5:
	    	nBits = 1;
	    	return (bitBuf >>> 1) & 0x0F;
	    case 6:
	    	nBits = 2;
	    	return (bitBuf >>> 2) & 0x0F;
	    case 7:
	    	nBits = 3;
	    	return (bitBuf >>> 3) & 0x0F;
	    case 8:
	    	nBits = 4;
	    	return (bitBuf >>> 4) & 0x0F;
	    default:
	      assert false;
	      return -1;
	  }
  }

  @Override
  public int getFiveBits() throws IOException {
  	final int x;
    switch (nBits) {
	    case 0:
        if (m_curPos == m_bufLen && fill() == -1)
          throw new EOFException();
        bitBuf = m_bytes[m_curPos++];
	      nBits = 3;
	      return (bitBuf >>> 3) & 0x1F;
    	case 1:
        x = (bitBuf & 0x01) << 4;
        if (m_curPos == m_bufLen && fill() == -1)
          throw new EOFException();
        bitBuf = m_bytes[m_curPos++];
	      nBits = 4;
	      return x | ((bitBuf >>> 4) & 0x0F);
	    case 2:
        x = (bitBuf & 0x03) << 3;
        if (m_curPos == m_bufLen && fill() == -1)
          throw new EOFException();
        bitBuf = m_bytes[m_curPos++];
	      nBits = 5;
	      return x | ((bitBuf >>> 5) & 0x07);
	    case 3:
        x = (bitBuf & 0x07) << 2;
        if (m_curPos == m_bufLen && fill() == -1)
          throw new EOFException();
        bitBuf = m_bytes[m_curPos++];
	      nBits = 6;
	      return x | ((bitBuf >>> 6) & 0x03);
	    case 4:
        x = (bitBuf & 0x0F) << 1;
        if (m_curPos == m_bufLen && fill() == -1)
          throw new EOFException();
        bitBuf = m_bytes[m_curPos++];
	      nBits = 7;
	      return x | ((bitBuf >>> 7) & 0x01);
	    case 5:
	    	nBits = 0;
	    	return bitBuf & 0x1F;
	    case 6:
	    	nBits = 1;
	    	return (bitBuf >>> 1) & 0x1F;
	    case 7:
	    	nBits = 2;
	    	return (bitBuf >>> 2) & 0x1F;
	    case 8:
	    	nBits = 3;
	    	return (bitBuf >>> 3) & 0x1F;
	    default:
	      assert false;
	      return -1;
	  }
  }

  @Override
  public int getEightBits() throws IOException {
    final int x;
    switch (nBits) {
      case 0:
        if (m_curPos == m_bufLen && fill() == -1)
          throw new EOFException();
        return m_bytes[m_curPos++] & 0xFF;
      case 8:
        nBits = 0;
        return bitBuf;
      case 1:
        x = (bitBuf & 1) << 7;
        if (m_curPos == m_bufLen && fill() == -1) {
          throw new EOFException();
        }      
        bitBuf = m_bytes[m_curPos++] & 0xFF;
        return x | (bitBuf >>> 1);
      case 2:
        x = (bitBuf & 3) << 6;
        if (m_curPos == m_bufLen && fill() == -1) {
          throw new EOFException();
        }      
        bitBuf = m_bytes[m_curPos++] & 0xFF;
        return x | (bitBuf >>> 2);
      case 3:
        x = (bitBuf & 7) << 5;
        if (m_curPos == m_bufLen && fill() == -1) {
          throw new EOFException();
        }      
        bitBuf = m_bytes[m_curPos++] & 0xFF;
        return x | (bitBuf >>> 3);
      case 4:
        x = (bitBuf & 15) << 4;
        if (m_curPos == m_bufLen && fill() == -1) {
          throw new EOFException();
        }      
        bitBuf = m_bytes[m_curPos++] & 0xFF;
        return x | (bitBuf >>> 4);
      case 5:
        x = (bitBuf & 31) << 3;
        if (m_curPos == m_bufLen && fill() == -1) {
          throw new EOFException();
        }      
        bitBuf = m_bytes[m_curPos++] & 0xFF;
        return x | (bitBuf >>> 5);
      case 6:
        x = (bitBuf & 63) << 2;
        if (m_curPos == m_bufLen && fill() == -1) {
          throw new EOFException();
        }      
        bitBuf = m_bytes[m_curPos++] & 0xFF;
        return x | (bitBuf >>> 6);
      case 7:
        x = (bitBuf & 127) << 1;
        if (m_curPos == m_bufLen && fill() == -1) {
          throw new EOFException();
        }      
        bitBuf = m_bytes[m_curPos++] & 0xFF;
        return x | (bitBuf >>> 7);
      default:
        assert false;
        return -1;
    }
  }
  
  /**
   * DON't remove this comment!
   * Returns the right-most n-bits of x
   * private static int rightBits(int n, int x) {
   *   return x & ((1 << n) - 1);
   * }
   */
  
}
