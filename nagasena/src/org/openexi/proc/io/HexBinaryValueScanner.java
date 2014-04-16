package org.openexi.proc.io;

import java.io.IOException;

import org.openexi.proc.common.QName;
import org.openexi.proc.grammars.Apparatus;
import org.openexi.proc.util.ExiUriConst;
import org.openexi.schema.Characters;
import org.openexi.schema.HexBin;

final class HexBinaryValueScanner extends BinaryValueScanner {

  private final StringBuffer m_stringBuffer;
  
  HexBinaryValueScanner(Scanner scanner) {
    super(new QName("exi:hexBinary", ExiUriConst.W3C_2009_EXI_URI), scanner);
    m_stringBuffer = new StringBuffer();
  }

  @Override
  public short getCodecID() {
    return Apparatus.CODEC_HEXBINARY;
  }

  @Override
  public int getBuiltinRCS(int simpleType) {
    return BuiltinRCS.RCS_ID_HEXBINARY;
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
    m_stringBuffer.setLength(0);
    HexBin.encode(m_octets, len, m_stringBuffer);
    final String stringValue = m_stringBuffer.toString();
    final int length = stringValue.length();
    m_scanner.m_characterBuffer.ensureCharacters(length);
    return m_scanner.m_characterBuffer.addString(stringValue, length);
  }

}
