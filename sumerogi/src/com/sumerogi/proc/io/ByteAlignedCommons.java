package com.sumerogi.proc.io;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;

import com.sumerogi.proc.io.Scriber;

public final class ByteAlignedCommons {

  private ByteAlignedCommons() {
  }

  public static final boolean readBoolean(InputStream istream) throws IOException {
    int val;
    switch (val = istream.read()) {
      case 1:
        return true;
      case 0:
        return false;
      default:
        assert val == -1;
        throw new EOFException();
    }
  }

  public static int readNBitUnsigned(final int width, final InputStream istream) throws IOException {
    assert width != 0;    
    final int n_bits = (width & 0x0007) != 0 ? (width & ~0x0007) + 8 : width & ~0x0007;
    int val = 0;
    int shift = 0;
    do {
      final int nextByte;
      if ((nextByte = istream.read()) == -1)
        throw new EOFException();
      val |= nextByte << shift;
      shift += 8;
    }
    while (shift != n_bits);
    return val;
  }
  
  public static int readEightBitsUnsigned(final InputStream istream) throws IOException {
    final int nextByte;
    if ((nextByte = istream.read()) == -1)
      throw new EOFException();
    return nextByte;
  }
  
  public static void writeNBitUnsigned(int val, int width, OutputStream ostream) throws IOException {
    final int quotient = width >>> 3; // i.e. width / 8
    final int n_bytes = (width & 0x0007) != 0 ? quotient + 1 : quotient; 
    for (int i = 0; i < n_bytes; i++) {
      ostream.write((val >>> (i << 3)) & 0xFF);
    }
  }
  
  public static void writeUnsignedInteger32(int uinit, OutputStream ostream) throws IOException {
    boolean continued = true;
    do {
      int nextByte = uinit & 0x007F; 
      if ((uinit >>>= 7) != 0)
        nextByte |= 0x0080; // set continuation flag on
      else
        continued = false;
      ostream.write(nextByte);
    }
    while (continued);
  }

  public static void writeUnsignedInteger64(long uinit, OutputStream ostream) throws IOException {
    boolean continued = true;
    do {
      int nextByte = (int)uinit & 0x007F; 
      if ((uinit >>>= 7) != 0)
        nextByte |= 0x0080; // set continuation flag on
      else
        continued = false;
      ostream.write(nextByte);
    }
    while (continued);
  }

  public static void writeUnsignedInteger(BigInteger uinit, OutputStream ostream) throws IOException {
    boolean continued = true;
    do {
      int nextByte = uinit.and(Scriber.BIGINTEGER_0x007F).intValue(); 
      uinit = uinit.shiftRight(7);
      if (!uinit.equals(BigInteger.ZERO))
        nextByte |= 0x0080; // set continuation flag on
      else
        continued = false;
      ostream.write(nextByte);
    }
    while (continued);
  }
  
}
