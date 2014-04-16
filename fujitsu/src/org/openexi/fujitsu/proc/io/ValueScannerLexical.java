package org.openexi.fujitsu.proc.io;

import java.io.IOException;
import java.io.InputStream;

import org.openexi.fujitsu.proc.common.CharacterSequence;
import org.openexi.fujitsu.proc.common.QName;

final class ValueScannerLexical extends ValueScanner {

  private final ValueScanner m_baseValueScanner;
  private final StringValueScanner m_stringValueScanner;
  
  public ValueScannerLexical(ValueScanner baseValueScanner, StringValueScanner stringValueScanner) {
    m_baseValueScanner = baseValueScanner;
    m_stringValueScanner = stringValueScanner;
  }
  
  public final QName getName() {
    return m_baseValueScanner.getName();
  }
  
  public final short getCodecID() {
    return m_baseValueScanner.getCodecID();
  }

  @Override
  public int getBuiltinRCS(int simpleType) {
    return m_baseValueScanner.getBuiltinRCS(simpleType);
  }

  @Override
  public CharacterSequence scan(String localName, String uri, int tp, InputStream istream) throws IOException {
    return m_stringValueScanner.scan(localName, uri, m_baseValueScanner.getBuiltinRCS(tp), istream);
  }

}
