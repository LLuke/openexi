package org.openexi.proc.io;

import java.io.IOException;

import org.openexi.proc.common.QName;
import org.openexi.proc.common.StringTable;
import org.openexi.proc.grammars.Apparatus;
import org.openexi.proc.util.ExiUriConst;
import org.openexi.schema.Characters;

final class StringValueScanner extends ValueScannerBase {
  
  // Owner scanner
  private final Scanner m_scanner;
  private StringTable.GlobalValuePartition m_globalValuePartition;
  
  private int m_valueMaxExclusiveLength;

  protected StringValueScanner(Scanner scanner) {
    super(new QName("exi:string", ExiUriConst.W3C_2009_EXI_URI));
    m_scanner = scanner;
    m_valueMaxExclusiveLength = Integer.MAX_VALUE;
  }

  @Override
  public short getCodecID() {
    return Apparatus.CODEC_STRING;
  }    
  
  void setStringTable(StringTable stringTable) {
    m_globalValuePartition = stringTable.globalValuePartition;
  }
  
  public void setValueMaxLength(int valueMaxLength) {
    assert valueMaxLength >= 0;
    m_valueMaxExclusiveLength = valueMaxLength != Integer.MAX_VALUE ? valueMaxLength + 1 : Integer.MAX_VALUE;
  }
  
  @Override
  public Characters scan(int localNameId, int uriId, int tp) throws IOException {
    int ucsCount = m_scanner.readUnsignedInteger(m_istream);
    if ((ucsCount & 0xFFFFFFFE) != 0) { // i.e. length > 1 
      if ((ucsCount -= 2) != 0) {
        final Characters value = m_scanner.readLiteralString(ucsCount, tp, m_istream);
        if (ucsCount < m_valueMaxExclusiveLength) {
          m_globalValuePartition.addValue(value, localNameId, uriId);
        }
        return value;
      }
      else
        return Characters.CHARACTERS_EMPTY;
    }
    else {
      final int id;
      if (ucsCount == 0) {
        final StringTable.LocalValuePartition localPartition;
        localPartition = m_globalValuePartition.getLocalPartition(localNameId, uriId);
        id = m_scanner.readNBitUnsigned(localPartition.width, m_istream);
        return localPartition.valueEntries[id].value;
      }
      else { // length == 1
        id = m_scanner.readNBitUnsigned(m_globalValuePartition.width, m_istream);
        return m_globalValuePartition.valueEntries[id].value;
      }
    }
  }

}
