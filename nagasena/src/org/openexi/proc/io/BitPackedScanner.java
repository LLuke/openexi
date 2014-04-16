package org.openexi.proc.io;

import java.io.IOException;
import java.io.InputStream;

import org.openexi.proc.common.AlignmentType;

public final class BitPackedScanner extends SimpleScanner {

  /**
   * Either an instance of EXIOptionsInputStream or BufferedBitInputStream.
   */
  private BitInputStream m_bitInputStream;
  private final BodyBitInputStream m_bodyBitInputStream;

  /*
   * Constructor is intentionally non-public.
   * Use ScannerFactory to instantiate a BitPackedScanner.
   */
  BitPackedScanner(boolean isForEXIOptions) {
    super(isForEXIOptions);
    m_bodyBitInputStream = isForEXIOptions ? null : new BodyBitInputStream();
  }

  @Override
  public void setInputStream(InputStream istream) {
    m_bodyBitInputStream.setInputStream(istream);
    m_bitInputStream = m_bodyBitInputStream;
    super.setInputStream((InputStream)null);
  }

  public void setEXIOptionsInputStream(InputStream istream) {
    m_bitInputStream = new HeaderOptionsInputStream(istream);
    super.setInputStream((InputStream)null);
  }

  public void takeover(HeaderOptionsInputStream istream) {
    m_bodyBitInputStream.inheritResidue(istream);
    m_bitInputStream = m_bodyBitInputStream;
    super.setInputStream((InputStream)null);
  }

  public BitInputStream getBitInputStream() {
    return m_bitInputStream;
  }

  @Override
  public AlignmentType getAlignmentType() {
    return AlignmentType.bitPacked;
  }

  @Override
  protected boolean readBoolean(InputStream istream) throws IOException {
    // use m_dataStream irrespective of istream
    return m_bitInputStream.getBit(); 
  }

  @Override
  protected int readNBitUnsigned(int width, InputStream istream) throws IOException {
    // use m_dataStream irrespective of istream
  	switch (width) {
  	    case 0:
  	      return 0;
  		case 1:
  		  return m_bitInputStream.getOneBit();
  		case 2:
  		  return m_bitInputStream.getTwoBits();
  		case 3:
  		  return m_bitInputStream.getThreeBits();
  		case 4:
  		  return m_bitInputStream.getFourBits();
  		case 5:
  		  return m_bitInputStream.getFiveBits();
	    default:
	      return m_bitInputStream.getBits(width);
  	}
  }
  
  @Override
  protected int readEightBitsUnsigned(InputStream istream) throws IOException {
    return m_bitInputStream.getEightBits();
  }

}
