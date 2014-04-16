package org.openexi.proc.io;

import java.io.IOException;

import org.openexi.proc.common.QName;
import org.openexi.proc.grammars.Apparatus;
import org.openexi.proc.util.ExiUriConst;
import org.openexi.schema.Base64;
import org.openexi.schema.Characters;

final class Base64BinaryValueScanner extends BinaryValueScanner {
  
  Base64BinaryValueScanner(Scanner scanner) {
    super(new QName("exi:base64Binary", ExiUriConst.W3C_2009_EXI_URI), scanner);
  }
  
  @Override
  public short getCodecID() {
    return Apparatus.CODEC_BASE64BINARY;
  }
  
  @Override
  public int getBuiltinRCS(int simpleType) {
    return BuiltinRCS.RCS_ID_BASE64BINARY;
  }
  
  @Override
  public Characters scan(int localNameId, int uriId, int tp) throws IOException {
    final int len = m_scanner.readUnsignedInteger(m_istream);
    if (m_octets.length < len) {
      expandBuffer(len);
    }
    for (int i = 0; i < len; i++) {
      m_octets[i] = (byte)m_scanner.readEightBitsUnsigned(m_istream);
    }
    final int maxChars = Base64.calculateTextMaxLength(len);
    m_scanner.m_characterBuffer.ensureCharacters(maxChars);
    final char[] characters = m_scanner.m_characterBuffer.characters;
    final int startIndex = m_scanner.m_characterBuffer.allocCharacters(maxChars);
    int n_chars = Base64.encode(m_octets, 0, len, characters, startIndex);
    return new Characters(characters, startIndex, n_chars, m_scanner.m_characterBuffer.isVolatile);
  }

}
