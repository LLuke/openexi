package org.openexi.proc.io;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;

import org.openexi.proc.common.AlignmentType;

public final class ByteAlignedScriber extends SimpleScriber {

  private OutputStream m_dataStream;

  public ByteAlignedScriber()  {
    super(false);
    m_dataStream = null;
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
    m_dataStream = dataStream; 
  }

  @Override
  protected OutputStream getOutputStream() {
    return m_dataStream;
  }

  @Override
  protected void writeUnsignedInteger32(int ulong, OutputStream ostream) throws IOException {
    ByteAlignedCommons.writeUnsignedInteger32(ulong, m_dataStream);
  }
  
  @Override
  protected void writeUnsignedInteger64(long uint, OutputStream ostream) throws IOException {
    ByteAlignedCommons.writeUnsignedInteger64(uint, m_dataStream);
  }
  
  @Override
  protected void writeUnsignedInteger(BigInteger uint, OutputStream ostream) throws IOException {
    ByteAlignedCommons.writeUnsignedInteger(uint, m_dataStream);
  }

  @Override
  protected void writeBoolean(boolean val, OutputStream ostream) throws IOException {
    m_dataStream.write(val ? 1 : 0);
  }
  
  @Override
  protected void writeNBitUnsigned(int val, int width, OutputStream ostream) throws IOException {
    ByteAlignedCommons.writeNBitUnsigned(val, width, m_dataStream);
  }
  
  @Override
  public void finish() throws IOException {
    m_dataStream.flush();    
  }

}
