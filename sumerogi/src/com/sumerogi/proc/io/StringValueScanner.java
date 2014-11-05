package com.sumerogi.proc.io;

import java.io.IOException;

import com.sumerogi.proc.common.StringTable;
import com.sumerogi.schema.Characters;

final class StringValueScanner extends ValueScannerBase {
  
  // Owner scanner
  private final Scanner m_scanner;
  private StringTable.GlobalValuePartition m_globalValuePartition;
  
  protected StringValueScanner(Scanner scanner) {
    m_scanner = scanner;
  }

  void setStringTable(StringTable stringTable) {
    m_globalValuePartition = stringTable.globalValuePartition;
  }
  
  @Override
  public Characters scan(int localNameId) throws IOException {
    int ucsCount = m_scanner.readUnsignedInteger(m_istream);
    if ((ucsCount & 0xFFFFFFFE) != 0) { // i.e. length > 1 
      if ((ucsCount -= 2) != 0) {
        final Characters value = m_scanner.readLiteralString(ucsCount, m_istream);
        m_globalValuePartition.addValue(value, localNameId);
        return value;
      }
      else
        return Characters.CHARACTERS_EMPTY;
    }
    else {
      final int id;
      if (ucsCount == 0) {
        final StringTable.LocalValuePartition localPartition;
        localPartition = m_globalValuePartition.getLocalPartition(localNameId);
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
