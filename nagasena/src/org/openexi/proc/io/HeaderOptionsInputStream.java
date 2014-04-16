package org.openexi.proc.io;

import java.io.EOFException;
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
  @Override
  public boolean getBit() throws IOException {
    if (--nBits >= 0)
      return ((bitBuf >>> nBits) & 1) == 1;
    nBits = 7;
    bitBuf = in.read();
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
      if ((bitBuf = in.read()) == -1) {
        nBits = 0;
        return x;
      }
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
	      if ((bitBuf = in.read()) == -1)
	        throw new EOFException();
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
	      if ((bitBuf = in.read()) == -1)
	        throw new EOFException();
	      nBits = 6;
	      return (bitBuf >>> 6) & 0x03;
    	case 1:
        final int x = (bitBuf & 0x01) << 1;
	      if ((bitBuf = in.read()) == -1)
	        throw new EOFException();
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
	      if ((bitBuf = in.read()) == -1)
	        throw new EOFException();
	      nBits = 5;
	      return (bitBuf >>> 5) & 0x07;
    	case 1:
        x = (bitBuf & 0x01) << 2;
	      if ((bitBuf = in.read()) == -1)
	        throw new EOFException();
	      nBits = 6;
	      return x | ((bitBuf >>> 6) & 0x03);
	    case 2:
        x = (bitBuf & 0x03) << 1;
	      if ((bitBuf = in.read()) == -1)
	        throw new EOFException();
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
	      if ((bitBuf = in.read()) == -1)
	        throw new EOFException();
	      nBits = 4;
	      return (bitBuf >>> 4) & 0x0F;
    	case 1:
        x = (bitBuf & 0x01) << 3;
	      if ((bitBuf = in.read()) == -1)
	        throw new EOFException();
	      nBits = 5;
	      return x | ((bitBuf >>> 5) & 0x07);
	    case 2:
        x = (bitBuf & 0x03) << 2;
	      if ((bitBuf = in.read()) == -1)
	        throw new EOFException();
	      nBits = 6;
	      return x | ((bitBuf >>> 6) & 0x03);
	    case 3:
        x = (bitBuf & 0x07) << 1;
	      if ((bitBuf = in.read()) == -1)
	        throw new EOFException();
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
	      if ((bitBuf = in.read()) == -1)
	        throw new EOFException();
	      nBits = 3;
	      return (bitBuf >>> 3) & 0x1F;
    	case 1:
        x = (bitBuf & 0x01) << 4;
	      if ((bitBuf = in.read()) == -1)
	        throw new EOFException();
	      nBits = 4;
	      return x | ((bitBuf >>> 4) & 0x0F);
	    case 2:
        x = (bitBuf & 0x03) << 3;
	      if ((bitBuf = in.read()) == -1)
	        throw new EOFException();
	      nBits = 5;
	      return x | ((bitBuf >>> 5) & 0x07);
	    case 3:
        x = (bitBuf & 0x07) << 2;
	      if ((bitBuf = in.read()) == -1)
	        throw new EOFException();
	      nBits = 6;
	      return x | ((bitBuf >>> 6) & 0x03);
	    case 4:
        x = (bitBuf & 0x0F) << 1;
	      if ((bitBuf = in.read()) == -1)
	        throw new EOFException();
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
    switch (nBits) {
      case 0:
        if ((bitBuf = in.read()) == -1)
          throw new EOFException();
        return bitBuf;
      case 8:
        nBits = 0;
        return bitBuf;
      case 1:
      case 2:
      case 3:
      case 4:
      case 5:
      case 6:
      case 7:
        final int x;
        x = (bitBuf & ((1 << nBits) - 1)) << (8 - nBits);
        if ((bitBuf = in.read()) == -1) {
          throw new EOFException();
        }      
        return x | (bitBuf >>> nBits);
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
