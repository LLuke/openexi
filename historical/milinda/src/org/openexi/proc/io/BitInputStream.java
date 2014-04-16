package org.openexi.proc.io;

import java.io.IOException;
import java.io.InputStream;

public abstract class BitInputStream extends InputStream {

  InputStream in;
  int getCount;
  int bitBuf;
  
  BitInputStream() {
    getCount = 0;
    bitBuf = 0;
  }
  
  public abstract boolean getBit() throws IOException;

  public abstract int getBits(int n) throws IOException;

}
