package org.openexi.proc.io;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;

import org.openexi.proc.common.AlignmentType;

public final class ByteAlignedScriber extends SimpleScriber {

  public ByteAlignedScriber()  {
    super(false);
  }
  
  @Override
  public AlignmentType getAlignmentType() {
    return AlignmentType.byteAligned;
  }

  /**
   * Set an output stream to which encoded streams are written out.
   * @param dataStream output stream
   */
  @Override
  public void setOutputStream(OutputStream dataStream) {
    m_outputStream = dataStream; 
  }

  @Override
  protected void writeUnsignedInteger32(int ulong, OutputStream ostream) throws IOException {
    ByteAlignedCommons.writeUnsignedInteger32(ulong, m_outputStream);
  }
  
  @Override
  protected void writeUnsignedInteger64(long uint, OutputStream ostream) throws IOException {
    ByteAlignedCommons.writeUnsignedInteger64(uint, m_outputStream);
  }
  
  @Override
  protected void writeUnsignedInteger(BigInteger uint, OutputStream ostream) throws IOException {
    ByteAlignedCommons.writeUnsignedInteger(uint, m_outputStream);
  }

  @Override
  protected void writeBoolean(boolean val, OutputStream ostream) throws IOException {
    m_outputStream.write(val ? 1 : 0);
  }
  
  @Override
  protected void writeNBitUnsigned(int val, int width, OutputStream ostream) throws IOException {
    ByteAlignedCommons.writeNBitUnsigned(val, width, m_outputStream);
  }
  
  @Override
  public void finish() throws IOException {
    m_outputStream.flush();    
  }

}
