package org.openexi.proc.io;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;

import org.openexi.proc.common.AlignmentType;

public final class BitPackedScriber extends SimpleScriber {

  private BitOutputStream m_dataStream;

  BitPackedScriber(boolean isForEXIOptions) {
    super(isForEXIOptions);
    m_dataStream = null;
  }
  
  @Override
  public AlignmentType getAlignmentType() {
    return AlignmentType.bitPacked;
  }

  /**
   * Set an output stream to which encoded streams are written out.
   * @param dataStream output stream
   */
  @Override
  public void setOutputStream(OutputStream dataStream) {
    m_dataStream = new BitOutputStream(dataStream); 
  }

  public void setBitOutputStream(BitOutputStream dataStream) {
    m_dataStream = dataStream; 
  }

  public BitOutputStream getBitOutputStream() {
    return m_dataStream;
  }

  public void writeBoolean(boolean val) throws IOException {
    m_dataStream.putBit(val);
  }

  @Override
  protected void writeUnsignedInteger32(int uint, OutputStream ostream) throws IOException {
    boolean continued = true;
    do {
      int nextByte = uint & 0x007F; 
      if ((uint >>>= 7) != 0)
        nextByte |= 0x0080; // set continuation flag on
      else
        continued = false;
      writeNBitUnsigned(nextByte, 8, (OutputStream)null);
    }
    while (continued);
  }

  @Override
  protected void writeUnsignedInteger64(long ulong, OutputStream ostream) throws IOException {
    boolean continued = true;
    do {
      int nextByte = (int)ulong & 0x007F; 
      if ((ulong >>>= 7) != 0)
        nextByte |= 0x0080; // set continuation flag on
      else
        continued = false;
      writeNBitUnsigned(nextByte, 8, (OutputStream)null);
    }
    while (continued);
  }

  @Override
  protected void writeUnsignedInteger(BigInteger uint, OutputStream ostream) throws IOException {
    boolean continued = true;
    do {
      int nextByte = uint.and(BIGINTEGER_0x007F).intValue();
      uint = uint.shiftRight(7);
      if (!uint.equals(BigInteger.ZERO))
        nextByte |= 0x0080; // set continuation flag on
      else
        continued = false;
      writeNBitUnsigned(nextByte, 8, (OutputStream)null);
    }
    while (continued);
  }
  
  @Override
  protected void writeBoolean(boolean val, OutputStream ostream) throws IOException {
    m_dataStream.putBit(val);
  }
  
  @Override
  protected void writeNBitUnsigned(int val, int width, OutputStream ostream) throws IOException {
    m_dataStream.putBits(width, val);
  }
  
  @Override
  public void finish() throws IOException {
    m_dataStream.flush();    
  }

}
