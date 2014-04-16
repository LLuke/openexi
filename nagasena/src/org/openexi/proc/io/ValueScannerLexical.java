package org.openexi.proc.io;

import java.io.IOException;
import java.io.InputStream;

import org.openexi.proc.common.QName;
import org.openexi.proc.grammars.Apparatus;
import org.openexi.schema.Characters;

final class ValueScannerLexical extends ValueScanner {

  private final ValueScanner m_baseValueScanner;
  private final StringValueScanner m_stringValueScanner;
  
  public ValueScannerLexical(ValueScanner baseValueScanner, StringValueScanner stringValueScanner) {
    m_baseValueScanner = baseValueScanner;
    m_stringValueScanner = stringValueScanner;
  }
  
  @Override
  public final QName getName() {
    return m_baseValueScanner.getName();
  }
  
  @Override
  public final short getCodecID() {
    return Apparatus.CODEC_LEXICAL;
  }

  @Override
  public int getBuiltinRCS(int simpleType) {
    return m_baseValueScanner.getBuiltinRCS(simpleType);
  }

  @Override
  public final void setInputStream(InputStream istream) {
    m_baseValueScanner.setInputStream(istream);
  }

  @Override
  public Characters scan(int name, int uri, int tp) throws IOException {
    return m_stringValueScanner.scan(name, uri, m_baseValueScanner.getBuiltinRCS(tp));
  }

}
