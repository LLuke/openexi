package org.openexi.fujitsu.proc.io;

import java.io.IOException;
import java.io.InputStream;

import org.openexi.fujitsu.proc.common.AlignmentType;
import org.openexi.fujitsu.proc.grammars.DocumentGrammarState;

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
  
  void init(DocumentGrammarState documentGrammarState) {
    m_documentGrammarState = documentGrammarState;
  }

  @Override
  public void setInputStream(InputStream istream) {
    m_bodyBitInputStream.setInputStream(istream);
    m_bitInputStream = m_bodyBitInputStream;
    super.setInputStream(m_bitInputStream);
  }

  public void setEXIOptionsInputStream(InputStream istream) {
    m_bitInputStream = new HeaderOptionsInputStream(istream);
    super.setInputStream(m_bitInputStream);
  }

  public void takeover(HeaderOptionsInputStream istream) {
    m_bodyBitInputStream.inheritResidue(istream);
    m_bitInputStream = m_bodyBitInputStream;
    super.setInputStream(m_bitInputStream);
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
  public int readNBitUnsigned(int width, InputStream istream) throws IOException {
    // use m_dataStream irrespective of istream
    return m_bitInputStream.getBits(width);
  }
  
}
