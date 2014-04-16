package org.openexi.proc.io;

import java.io.IOException;
import java.io.InputStream;

public abstract class BitInputStream {

  InputStream in;
  int nBits;
  int bitBuf;
  
  BitInputStream() {
    nBits = 0;
    bitBuf = 0;
  }
  
  public abstract boolean getBit() throws IOException;

  public abstract int getBits(int n) throws IOException;

  public abstract int getOneBit() throws IOException;

  public abstract int getTwoBits() throws IOException;

  public abstract int getThreeBits() throws IOException;

  public abstract int getFourBits() throws IOException;

  public abstract int getFiveBits() throws IOException;

  public abstract int getEightBits() throws IOException;

}
