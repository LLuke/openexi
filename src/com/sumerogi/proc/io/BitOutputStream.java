package com.sumerogi.proc.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 */
public class BitOutputStream  {
  
  private int putCount = 8;
  private int bitBuf = 0;
  
  private final OutputStream m_out;

  BitOutputStream(OutputStream out) {
    m_out = out;
  }

  /**
   * Returns the right-most n-bits of x
   */
  private static int rightBits(int n, int x) {
    return x & ((1 << n) - 1);
  }

  /**
   * Put a single bit.
   */
  public void putBit(boolean bit) throws IOException {
    putCount--;
    if (bit)
      bitBuf |= (1 << putCount);
    if (putCount == 0) {
      m_out.write(bitBuf);
      bitBuf = 0;
      putCount = 8;
    }
  }

  /**
   * Put the right-most n-bits of x.
   */
  public void putBits(int n, int x) throws IOException {
    while (n >= putCount) {
      n -= putCount;
      bitBuf |= rightBits(putCount, x >>> n);
      m_out.write(bitBuf);
      bitBuf = 0;
      putCount = 8;
    }
    putCount -= n;
    bitBuf |= rightBits(n, x) << putCount;
  }

//  /**
//   */
//  public void close() throws IOException {
//    if (putCount != 8) {
//      flush();
//    }
//    super.close();
//  }
  
  public void flush() throws IOException {
    if (putCount != 8) {
      putBits(putCount, 0);
    }
    m_out.flush();
  }
  
}
