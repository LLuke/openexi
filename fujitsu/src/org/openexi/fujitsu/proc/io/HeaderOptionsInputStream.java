package org.openexi.fujitsu.proc.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * An implementation of BitInputStream representing the header options
 * document in an EXI stream. 
 */
public final class HeaderOptionsInputStream extends BitInputStream {

  HeaderOptionsInputStream(InputStream in) {
    super.in = in;
  }

  public int read() throws IOException {
    return in.read();
  }

  /**
   * Retrieves a single bit.
   */
  public boolean getBit() throws IOException {
    if (--getCount >= 0)
      return ((bitBuf >>> getCount) & 1) == 1;
    getCount = 7;
    bitBuf = in.read();
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
      if ((bitBuf = in.read()) == -1) {
        getCount = 0;
        return x;
      }
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
