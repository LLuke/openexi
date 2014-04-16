package org.openexi.proc.io;

import java.io.IOException;
import java.io.InputStream;

import org.openexi.proc.common.QName;

abstract class DateTimeValueScannerBase extends ValueScannerBase {
  
  protected final char[] m_dateTimeCharacters;
  protected int m_n_dateTimeCharacters;
  private final char[] m_transientCharacters;
  // REVISIT: abandon m_stringBuffer2
  private final StringBuilder m_stringBuffer2;

  protected final Scanner m_scanner; 

  protected DateTimeValueScannerBase(QName name, Scanner scanner) {
    super(name);
    m_scanner = scanner;
    m_dateTimeCharacters = new char[256];
    m_n_dateTimeCharacters = 0;
    m_transientCharacters = new char[64];
    m_stringBuffer2 = new StringBuilder();
  }
  
  /** @y.exclude */
  @Override
  public final int getBuiltinRCS(int simpleType) {
    return BuiltinRCS.RCS_ID_DATETIME;
  }

  protected final void readYear(InputStream istream) throws IOException {
    final boolean isNegative = m_scanner.readBoolean(istream);
    int year = m_scanner.readUnsignedInteger(istream);
    year = isNegative ? 1999 - year : year + 2000; 
    if (year < 0) {
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = '-';
      year = 0 - year;
    }
    if (year < 10) {
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = '0';
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = '0';
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = '0';
    }
    else if (year < 100) {
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = '0';
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = '0';
    }
    else if (year < 1000) {
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = '0';
    }
    int n_transientCharacters = 0;
    for (; year != 0; year /= 10) {
      final char digitChar = (char)(48 + year % 10);
      m_transientCharacters[n_transientCharacters++] = digitChar;
    }
    --n_transientCharacters;
    for (; n_transientCharacters != -1; n_transientCharacters--)
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = m_transientCharacters[n_transientCharacters];
  }
  
  protected final void readGDay(InputStream istream) throws IOException {
    final int day = m_scanner.readNBitUnsigned(9, istream);
    m_dateTimeCharacters[m_n_dateTimeCharacters++] = '-';
    m_dateTimeCharacters[m_n_dateTimeCharacters++] = '-';
    m_dateTimeCharacters[m_n_dateTimeCharacters++] = '-';
    if (day < 10) {
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = '0';
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = (char)(48 + day);
    }
    else {
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = (char)(48 + day / 10);
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = (char)(48 + day % 10);
    }
  }
  
  protected final void readGMonth(InputStream istream) throws IOException {
    final int intValue = m_scanner.readNBitUnsigned(9, istream);
    int month = intValue >>> 5;
    assert intValue % 32 == 0;
    m_dateTimeCharacters[m_n_dateTimeCharacters++] = '-';
    m_dateTimeCharacters[m_n_dateTimeCharacters++] = '-';
    if (month < 10) {
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = '0';
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = (char)(48 + month);
    }
    else {
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = (char)(48 + month / 10);
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = (char)(48 + month % 10);
    }
  }
  
  protected final void readGMonthDay(InputStream istream) throws IOException {
    final int intValue = m_scanner.readNBitUnsigned(9, istream);
    final int month = intValue >>> 5;
    final int day = intValue & 0x001F; 
    m_dateTimeCharacters[m_n_dateTimeCharacters++] = '-';
    m_dateTimeCharacters[m_n_dateTimeCharacters++] = '-';
    if (month < 10) {
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = '0';
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = (char)(48 + month);
    }
    else {
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = (char)(48 + month / 10);
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = (char)(48 + month % 10);
    }
    m_dateTimeCharacters[m_n_dateTimeCharacters++] = '-';
    if (day < 10) {
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = '0';
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = (char)(48 + day);
    }
    else {
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = (char)(48 + day / 10);
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = (char)(48 + day % 10);
    }
  }
  
  protected final void readGYearMonth(InputStream istream) throws IOException {
    readYear(istream);
    m_dateTimeCharacters[m_n_dateTimeCharacters++] = '-';
    final int intValue = m_scanner.readNBitUnsigned(9, istream);
    final int month = intValue >>> 5;
    assert intValue % 32 == 0;
    if (month < 10) {
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = '0';
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = (char)(48 + month);
    }
    else {
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = (char)(48 + month / 10);
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = (char)(48 + month % 10);
    }
  }
  
  protected final void readMonthDay(InputStream istream) throws IOException {
    final int intValue = m_scanner.readNBitUnsigned(9, istream);
    final int month = intValue >>> 5;
    final int day = intValue & 0x001F; 
    m_dateTimeCharacters[m_n_dateTimeCharacters++] = '-';
    if (month < 10) {
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = '0';
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = (char)(48 + month);
    }
    else {
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = (char)(48 + month / 10);
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = (char)(48 + month % 10);
    }
    m_dateTimeCharacters[m_n_dateTimeCharacters++] = '-';
    if (day < 10) {
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = '0';
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = (char)(48 + day);
    }
    else {
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = (char)(48 + day / 10);
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = (char)(48 + day % 10);
    }
  }
  
  protected final void readTime(InputStream istream) throws IOException {
    int intValue = m_scanner.readNBitUnsigned(17, istream);
    final int hours = intValue >> 12; // intValue / 4096
    intValue &= 0x0FFF; // intValue %= 4096
    int minutes = intValue / 64;
    int seconds = intValue % 64;
    if (hours < 10) {
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = '0';
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = (char)(48 + hours);
    }
    else {
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = (char)(48 + hours / 10);
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = (char)(48 + hours % 10);
    }
    if (minutes < 10) {
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = ':';
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = '0';
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = (char)(48 + minutes);
    }
    else {
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = ':';
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = (char)(48 + minutes / 10);
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = (char)(48 + minutes % 10);
    }
    if (seconds < 10) {
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = ':';
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = '0';
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = (char)(48 + seconds);
    }
    else {
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = ':';
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = (char)(48 + seconds / 10);
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = (char)(48 + seconds % 10);
    }
    if (m_scanner.readBoolean(istream)) {
      intValue = m_scanner.readUnsignedInteger(istream);
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = '.';
      // REVISIT: abandon m_stringBuffer2
      m_stringBuffer2.setLength(0);
      m_stringBuffer2.append(Integer.toString(intValue)).reverse();
      for (int i = 0; i < m_stringBuffer2.length(); i++)
        m_dateTimeCharacters[m_n_dateTimeCharacters++] = m_stringBuffer2.charAt(i);
    }
  }
  
  protected final void readTimeZone(InputStream istream) throws IOException {
    if (m_scanner.readBoolean(istream)) {
      int intValue = m_scanner.readNBitUnsigned(11, istream);
      if ((intValue -= 64 * 14) != 0) {
        if (intValue < 0) {
          m_dateTimeCharacters[m_n_dateTimeCharacters++] = '-';
          intValue = 0 - intValue;
        }
        else
          m_dateTimeCharacters[m_n_dateTimeCharacters++] = '+';
        final int hours = intValue / 64;
        final int minutes = intValue % 64;
        if (hours < 10) {
          m_dateTimeCharacters[m_n_dateTimeCharacters++] = '0';
          m_dateTimeCharacters[m_n_dateTimeCharacters++] = (char)(48 + hours);
        }
        else {
          m_dateTimeCharacters[m_n_dateTimeCharacters++] = (char)(48 + hours / 10);
          m_dateTimeCharacters[m_n_dateTimeCharacters++] = (char)(48 + hours % 10);
        }
        if (minutes < 10) {
          m_dateTimeCharacters[m_n_dateTimeCharacters++] = ':';
          m_dateTimeCharacters[m_n_dateTimeCharacters++] = '0';
          m_dateTimeCharacters[m_n_dateTimeCharacters++] = (char)(48 + minutes);
        }
        else {
          m_dateTimeCharacters[m_n_dateTimeCharacters++] = ':';
          m_dateTimeCharacters[m_n_dateTimeCharacters++] = (char)(48 + minutes / 10);
          m_dateTimeCharacters[m_n_dateTimeCharacters++] = (char)(48 + minutes % 10);
        }
      }
      else
        m_dateTimeCharacters[m_n_dateTimeCharacters++] = 'Z';
    }
  }
  

}
