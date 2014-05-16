using System;
using System.Diagnostics;
using System.Globalization;
using System.IO;
using System.Text;

using QName = Nagasena.Proc.Common.QName;

namespace Nagasena.Proc.IO {

  internal abstract class DateTimeValueScannerBase : ValueScannerBase {

    protected internal readonly char[] m_dateTimeCharacters;
    protected internal int m_n_dateTimeCharacters;
    private readonly char[] m_transientCharacters;

    protected internal readonly Scanner m_scanner;

    protected internal DateTimeValueScannerBase(QName name, Scanner scanner) : base(name) {
      m_scanner = scanner;
      m_dateTimeCharacters = new char[256];
      m_n_dateTimeCharacters = 0;
      m_transientCharacters = new char[64];
    }

    /// <summary>
    /// @y.exclude </summary>
    public override sealed int getBuiltinRCS(int simpleType) {
      return BuiltinRCS.RCS_ID_DATETIME;
    }

    protected internal void readYear(Stream istream) {
      bool isNegative = m_scanner.readBoolean(istream);
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
        char digitChar = (char)(48 + year % 10);
        m_transientCharacters[n_transientCharacters++] = digitChar;
      }
      --n_transientCharacters;
      for (; n_transientCharacters != -1; n_transientCharacters--) {
        m_dateTimeCharacters[m_n_dateTimeCharacters++] = m_transientCharacters[n_transientCharacters];
      }
    }

    protected internal void readGDay(Stream istream) {
      int day = m_scanner.readNBitUnsigned(9, istream);
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

    protected internal void readGMonth(Stream istream) {
      int intValue = m_scanner.readNBitUnsigned(9, istream);
      int month = (int)((uint)intValue >> 5);
      Debug.Assert(intValue % 32 == 0);
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

    protected internal void readGMonthDay(Stream istream) {
      int intValue = m_scanner.readNBitUnsigned(9, istream);
      int month = (int)((uint)intValue >> 5);
      int day = intValue & 0x001F;
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

    protected internal void readGYearMonth(Stream istream) {
      readYear(istream);
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = '-';
      int intValue = m_scanner.readNBitUnsigned(9, istream);
      int month = (int)((uint)intValue >> 5);
      Debug.Assert(intValue % 32 == 0);
      if (month < 10) {
        m_dateTimeCharacters[m_n_dateTimeCharacters++] = '0';
        m_dateTimeCharacters[m_n_dateTimeCharacters++] = (char)(48 + month);
      }
      else {
        m_dateTimeCharacters[m_n_dateTimeCharacters++] = (char)(48 + month / 10);
        m_dateTimeCharacters[m_n_dateTimeCharacters++] = (char)(48 + month % 10);
      }
    }

    protected internal void readMonthDay(Stream istream) {
      int intValue = m_scanner.readNBitUnsigned(9, istream);
      int month = (int)((uint)intValue >> 5);
      int day = intValue & 0x001F;
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

    protected internal void readTime(Stream istream) {
      int intValue = m_scanner.readNBitUnsigned(17, istream);
      int hours = intValue >> 12; // intValue / 4096
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
        char[] charArray = Convert.ToString(intValue, NumberFormatInfo.InvariantInfo).ToCharArray();
        Array.Reverse(charArray);
        for (int i = 0; i < charArray.Length; i++) {
          m_dateTimeCharacters[m_n_dateTimeCharacters++] = charArray[i];
        }
      }
    }

    protected internal void readTimeZone(Stream istream) {
      if (m_scanner.readBoolean(istream)) {
        int intValue = m_scanner.readNBitUnsigned(11, istream);
        if ((intValue -= 64 * 14) != 0) {
          if (intValue < 0) {
            m_dateTimeCharacters[m_n_dateTimeCharacters++] = '-';
            intValue = 0 - intValue;
          }
          else {
            m_dateTimeCharacters[m_n_dateTimeCharacters++] = '+';
          }
          int hours = intValue / 64;
          int minutes = intValue % 64;
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
        else {
          m_dateTimeCharacters[m_n_dateTimeCharacters++] = 'Z';
        }
      }
    }


  }

}