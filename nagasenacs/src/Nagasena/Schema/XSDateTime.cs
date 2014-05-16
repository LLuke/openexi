using System;
using System.Diagnostics;
using System.Globalization;
using System.IO;
using System.Numerics;
using System.Text;

namespace Nagasena.Schema {

  public sealed class XSDateTime {

    public static readonly int FIELD_UNDEFINED = int.MinValue;

    /// <summary>
    /// One of the eight calendar-related datatype identifers defined 
    /// in EXISchemaConst.  
    /// </summary>
    private sbyte primTypeId;

    public int year, month, day, hour, minute, second, timeZone;
    public BigInteger? reverseFractionalSecond;

    private static readonly int[] LAST_DAY_OF_MONTH;
    static XSDateTime() {
      LAST_DAY_OF_MONTH = new int[] { FIELD_UNDEFINED, 31, -1, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };
    }
    private const int DEFAULT_YEAR = 4;

    public static int getLastDayOfMonth(int year, int month) {
      if (month == FIELD_UNDEFINED) {
        Debug.Assert(year == FIELD_UNDEFINED);
        month = 1;
      }
      if (year == FIELD_UNDEFINED) {
        year = DEFAULT_YEAR;
      }
      /// M := modulo(monthValue, 1, 13)
      /// Y := yearValue + fQuotient(monthValue, 1, 13)
      long quotientModulo = calculateQuotientModulo(month, 1, 13);
      month = getModulo(quotientModulo);
      year = year + getQuotient(quotientModulo);

      int lastDay;
      if ((lastDay = LAST_DAY_OF_MONTH[month]) == -1) {
        Debug.Assert(month == 2);
        if (year == FIELD_UNDEFINED || year % 400 == 0 || year % 100 != 0 && year % 4 == 0) {
          lastDay = 29;
        }
        else {
          lastDay = 28;
        }
      }
      return lastDay;
    }

    internal XSDateTime(XSDateTime dateTime) {
      primTypeId = dateTime.primTypeId;
      year = dateTime.year;
      month = dateTime.month;
      day = dateTime.day;
      hour = dateTime.hour;
      minute = dateTime.minute;
      second = dateTime.second;
      reverseFractionalSecond = dateTime.reverseFractionalSecond;
      timeZone = dateTime.timeZone;
    }

    public XSDateTime() {
    }

    public XSDateTime(int year, int month, int day, int hour, int minute, int second, 
      BigInteger? reverseFractionalSecond, int timeZone, sbyte primTypeId) {
      this.primTypeId = primTypeId;
      this.year = year;
      this.month = month;
      this.day = day;
      this.hour = hour;
      this.minute = minute;
      this.second = second;
      this.reverseFractionalSecond = reverseFractionalSecond;
      this.timeZone = timeZone;
    }

    public override bool Equals(object obj) {
      if (obj is XSDateTime) {
        XSDateTime dateTime = (XSDateTime)obj;
        int timeZone1, timeZone2;
        timeZone1 = timeZone;
        timeZone2 = dateTime.timeZone;
        // Both are normalized values.
        Debug.Assert((timeZone1 == 0 || timeZone1 == FIELD_UNDEFINED) && (timeZone2 == 0 || timeZone2 == FIELD_UNDEFINED));
        if (timeZone != dateTime.timeZone) {
          return false;
        }
        else {
          if (primTypeId != dateTime.primTypeId || year != dateTime.year || month != dateTime.month || day != dateTime.day || 
              hour != dateTime.hour || minute != dateTime.minute || second != dateTime.second) {
            return false;
          }
          if (!reverseFractionalSecond.HasValue) {
            return !dateTime.reverseFractionalSecond.HasValue || dateTime.reverseFractionalSecond.Value.Sign == 0;
          }
          else if (!dateTime.reverseFractionalSecond.HasValue) {
            return reverseFractionalSecond.Value.Sign == 0;
          }
          else {
            return reverseFractionalSecond == dateTime.reverseFractionalSecond;
          }
        }
      }
      return false;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Normalization (See Appendix E in XML Schema 1.0 datatype spec)
    ///////////////////////////////////////////////////////////////////////////

    public void normalize() {
      switch (primTypeId) {
        case EXISchemaConst.TIME_TYPE:
          year = DEFAULT_YEAR;
          month = 1;
          day = 1;
          break;
        case EXISchemaConst.DATE_TYPE:
          hour = 0;
          minute = 0;
          second = 0;
          reverseFractionalSecond = null;
          break;
        case EXISchemaConst.G_YEARMONTH_TYPE:
          day = 1;
          hour = 0;
          minute = 0;
          second = 0;
          reverseFractionalSecond = null;
          break;
        case EXISchemaConst.G_YEAR_TYPE:
          month = 1;
          day = 1;
          hour = 0;
          minute = 0;
          second = 0;
          reverseFractionalSecond = null;
          break;
        case EXISchemaConst.G_MONTHDAY_TYPE:
          year = DEFAULT_YEAR;
          hour = 0;
          minute = 0;
          second = 0;
          reverseFractionalSecond = null;
          break;
        case EXISchemaConst.G_DAY_TYPE:
          year = DEFAULT_YEAR;
          month = 1;
          hour = 0;
          minute = 0;
          second = 0;
          reverseFractionalSecond = null;
          break;
        case EXISchemaConst.G_MONTH_TYPE:
          year = DEFAULT_YEAR;
          day = 1;
          hour = 0;
          minute = 0;
          second = 0;
          reverseFractionalSecond = null;
          break;
        case EXISchemaConst.DATETIME_TYPE:
          break;
        default:
          Debug.Assert(false);
          return;
      }

      int durationMinute = timeZone != FIELD_UNDEFINED ? 0 - timeZone : 0;
      Debug.Assert(-840 <= durationMinute && durationMinute <= 840); // 14 * 60 = 840

      int dMinute = durationMinute % 60;
      int dHour = durationMinute / 60;

      int carry, temp;
      long quotientModulo;
      /// Seconds (Note D[second] is zero in this context)
      /// temp := S[second] + D[second]
      /// E[second] := modulo(temp, 60)
      /// carry := fQuotient(temp, 60)
      temp = second;
      second = temp % 60;
      carry = temp / 60;
      /// Minutes
      /// temp := S[minute] + D[minute] + carry
      /// E[minute] := modulo(temp, 60)
      /// carry := fQuotient(temp, 60)
      quotientModulo = calculateQuotientModulo(minute + dMinute + carry, 60);
      minute = getModulo(quotientModulo);
      carry = getQuotient(quotientModulo);
      /// Hours
      /// temp := S[hour] + D[hour] + carry
      /// E[hour] := modulo(temp, 24)
      /// carry := fQuotient(temp, 24)
      quotientModulo = calculateQuotientModulo(hour + dHour + carry, 24);
      hour = getModulo(quotientModulo);
      carry = getQuotient(quotientModulo);

      /// Days
      /// if S[day] > maximumDayInMonthFor(E[year], E[month])
      ///   tempDays := maximumDayInMonthFor(E[year], E[month])
      /// else if S[day] < 1
      ///   tempDays := 1
      /// else
      ///   tempDays := S[day]
      /// E[day] := tempDays + D[day] + carry
      /// START LOOP
      ///   IF E[day] < 1
      ///     E[day] := E[day] + maximumDayInMonthFor(E[year], E[month] - 1)
      ///     carry := -1
      ///   ELSE IF E[day] > maximumDayInMonthFor(E[year], E[month]) 
      ///     E[day] := E[day] - maximumDayInMonthFor(E[year], E[month])
      ///     carry := 1
      ///   ELSE EXIT LOOP
      ///   temp := E[month] + carry
      ///   E[month] := modulo(temp, 1, 13)
      ///   E[year] := E[year] + fQuotient(temp, 1, 13)
      ///   GOTO START LOOP
      int tempDays, lastDay;
      if (day > (lastDay = getLastDayOfMonth(year, month))) {
        tempDays = lastDay;
      }
      else if (day < 1) {
        tempDays = 1;
      }
      else {
        tempDays = day;
      }
      day = tempDays + carry;
      while (true) {
        if (day < 1) {
          day = day + getLastDayOfMonth(year, month - 1);
          carry = -1;
        }
        else if (day > (lastDay = getLastDayOfMonth(year, month))) {
          day = day - lastDay;
          carry = 1;
        }
        else {
          break;
        }
        temp = month + carry;
        quotientModulo = calculateQuotientModulo(temp, 1, 13);
        month = getModulo(quotientModulo);
        year = year + getQuotient(quotientModulo);
      }

      timeZone = timeZone != FIELD_UNDEFINED ? 0 : FIELD_UNDEFINED;
    }

    private static long calculateQuotientModulo(int value, int low, int high) {
      long quotientModulo = calculateQuotientModulo(value - low, high - low);
      int quotient = getQuotient(quotientModulo);
      int modulo = getModulo(quotientModulo) + low;
      return toCompositeLong(quotient, modulo);
    }

    private static long calculateQuotientModulo(int value, int factor) {
      Debug.Assert(factor > 0);
      int quotient, modulo;
      quotient = value / factor;
      if ((modulo = value % factor) < 0) {
        quotient -= 1;
        modulo += factor;
      }
      return toCompositeLong(quotient, modulo);
    }

    private static long toCompositeLong(int quotient, int modulo) {
      return (long)(((ulong)quotient) << 32 | (uint)modulo);
    }

    private static int getQuotient(long quotientModulo) {
      return (int)((long)((ulong)quotientModulo >> 32));
    }

    private static int getModulo(long quotientModulo) {
      return unchecked((int)(quotientModulo & 0xFFFFFFFFL));
    }

    ///////////////////////////////////////////////////////////////////////////
    // Serialization/Deserialization
    ///////////////////////////////////////////////////////////////////////////

    public static XSDateTime readIn(Stream @in) {
      XSDateTime dateTime = new XSDateTime();
      dateTime.primTypeId = (sbyte)@in.ReadByte();

      dateTime.year = FIELD_UNDEFINED;
      dateTime.month = FIELD_UNDEFINED;
      dateTime.day = FIELD_UNDEFINED;
      dateTime.hour = FIELD_UNDEFINED;
      dateTime.minute = FIELD_UNDEFINED;
      dateTime.second = FIELD_UNDEFINED;
      dateTime.reverseFractionalSecond = null;
      dateTime.timeZone = FIELD_UNDEFINED;

      switch (dateTime.primTypeId) {
        case EXISchemaConst.TIME_TYPE:
          dateTime.hour = EXISchema.ReadInt(@in);
            //ReadInt();
          dateTime.minute = EXISchema.ReadInt(@in);
          dateTime.second = EXISchema.ReadInt(@in);
          dateTime.reverseFractionalSecond = readPredicatedInteger(@in);
          break;
        case EXISchemaConst.DATE_TYPE:
          dateTime.year = EXISchema.ReadInt(@in);
          dateTime.month = EXISchema.ReadInt(@in);
          dateTime.day = EXISchema.ReadInt(@in);
          break;
        case EXISchemaConst.G_YEARMONTH_TYPE:
          dateTime.year = EXISchema.ReadInt(@in);
          dateTime.month = EXISchema.ReadInt(@in);
          break;
        case EXISchemaConst.G_YEAR_TYPE:
          dateTime.year = EXISchema.ReadInt(@in);
          break;
        case EXISchemaConst.G_MONTHDAY_TYPE:
          dateTime.month = EXISchema.ReadInt(@in);
          dateTime.day = EXISchema.ReadInt(@in);
          break;
        case EXISchemaConst.G_DAY_TYPE:
          dateTime.day = EXISchema.ReadInt(@in);
          break;
        case EXISchemaConst.G_MONTH_TYPE:
          dateTime.month = EXISchema.ReadInt(@in);
          break;
        case EXISchemaConst.DATETIME_TYPE:
          dateTime.year = EXISchema.ReadInt(@in);
          dateTime.month = EXISchema.ReadInt(@in);
          dateTime.day = EXISchema.ReadInt(@in);
          dateTime.hour = EXISchema.ReadInt(@in);
          dateTime.minute = EXISchema.ReadInt(@in);
          dateTime.second = EXISchema.ReadInt(@in);
          dateTime.reverseFractionalSecond = readPredicatedInteger(@in);
          break;
        default:
          Debug.Assert(false);
          break;
      }
      dateTime.timeZone = readPredicatedInt(@in);
      return dateTime;
    }

    public void writeOut(Stream @out) {
      @out.WriteByte((byte)primTypeId);
      switch (primTypeId) {
        case EXISchemaConst.TIME_TYPE:
          EXISchema.WriteInt(hour, @out);
          EXISchema.WriteInt(minute, @out);
          EXISchema.WriteInt(second, @out);
          writePredicatedInteger(reverseFractionalSecond, @out);
          break;
        case EXISchemaConst.DATE_TYPE:
          EXISchema.WriteInt(year, @out);
          EXISchema.WriteInt(month, @out);
          EXISchema.WriteInt(day, @out);
          break;
        case EXISchemaConst.G_YEARMONTH_TYPE:
          EXISchema.WriteInt(year, @out);
          EXISchema.WriteInt(month, @out);
          break;
        case EXISchemaConst.G_YEAR_TYPE:
          EXISchema.WriteInt(year, @out);
          break;
        case EXISchemaConst.G_MONTHDAY_TYPE:
          EXISchema.WriteInt(month, @out);
          EXISchema.WriteInt(day, @out);
          break;
        case EXISchemaConst.G_DAY_TYPE:
          EXISchema.WriteInt(day, @out);
          break;
        case EXISchemaConst.G_MONTH_TYPE:
          EXISchema.WriteInt(month, @out);
          break;
        case EXISchemaConst.DATETIME_TYPE:
          EXISchema.WriteInt(year, @out);
          EXISchema.WriteInt(month, @out);
          EXISchema.WriteInt(day, @out);
          EXISchema.WriteInt(hour, @out);
          EXISchema.WriteInt(minute, @out);
          EXISchema.WriteInt(second, @out);
          writePredicatedInteger(reverseFractionalSecond, @out);
          break;
        default:
          Debug.Assert(false);
          break;
      }
      writePredicatedInt(timeZone, @out);
    }

    private static void writePredicatedInt(int val, Stream @out) {
      if (val != FIELD_UNDEFINED) {
        @out.WriteByte(1);
        EXISchema.WriteInt(val, @out);
      }
      else {
        @out.WriteByte(0);
      }
    }

    private static int readPredicatedInt(Stream @in) {
      int predicate;
      if ((predicate = @in.ReadByte()) == 1) {
        return EXISchema.ReadInt(@in);
      }
      else {
        Debug.Assert(predicate == 0);
        return FIELD_UNDEFINED;
      }
    }

    private static void writePredicatedInteger(BigInteger? val, Stream @out) {
      if (val.HasValue) {
        @out.WriteByte(1);
        EXISchema.writeString(val.Value.ToString(NumberFormatInfo.InvariantInfo), @out);
      }
      else {
        @out.WriteByte(0);
      }
    }

    private static BigInteger? readPredicatedInteger(Stream @in) {
      int predicate;
      if ((predicate = @in.ReadByte()) == 1) {
        return Convert.ToInt64(EXISchema.readString(@in));
      }
      else {
        Debug.Assert(predicate == 0);
        return null;
      }
    }

    ///////////////////////////////////////////////////////////////////////////
    // toString() implementation
    ///////////////////////////////////////////////////////////////////////////

    public override string ToString() {
      StringBuilder buffer = new StringBuilder();
      switch (primTypeId) {
        case EXISchemaConst.TIME_TYPE:
          printTwoDigits(hour, buffer);
          printTwoDigits(minute, buffer.Append(':'));
          printTwoDigits(second, buffer.Append(':'));
          if (reverseFractionalSecond.HasValue) {
            printFractionalSecond(reverseFractionalSecond.Value, buffer);
          }
          break;
        case EXISchemaConst.DATE_TYPE:
          printYear(year, buffer);
          printTwoDigits(month, buffer.Append('-'));
          printTwoDigits(day, buffer.Append('-'));
          break;
        case EXISchemaConst.G_YEARMONTH_TYPE:
          printYear(year, buffer);
          printTwoDigits(month, buffer.Append('-'));
          break;
        case EXISchemaConst.G_YEAR_TYPE:
          printYear(year, buffer);
          break;
        case EXISchemaConst.G_MONTHDAY_TYPE:
          printTwoDigits(month, buffer.Append("--"));
          printTwoDigits(day, buffer.Append('-'));
          break;
        case EXISchemaConst.G_DAY_TYPE:
          printTwoDigits(day, buffer.Append("---"));
          break;
        case EXISchemaConst.G_MONTH_TYPE:
          printTwoDigits(month, buffer.Append("--"));
          break;
        case EXISchemaConst.DATETIME_TYPE:
          printYear(year, buffer);
          printTwoDigits(month, buffer.Append('-'));
          printTwoDigits(day, buffer.Append('-'));
          buffer.Append('T');
          printTwoDigits(hour, buffer);
          printTwoDigits(minute, buffer.Append(':'));
          printTwoDigits(second, buffer.Append(':'));
          if (reverseFractionalSecond.HasValue) {
            printFractionalSecond(reverseFractionalSecond.Value, buffer);
          }
          break;
        default:
          Debug.Assert(false);
          return null;
      }
      if (timeZone != FIELD_UNDEFINED) {
        printTimezone(timeZone, buffer);
      }
      return buffer.ToString(/**/);
    }

    private static void printTimezone(int timeZone, StringBuilder buffer) {
      Debug.Assert(timeZone != FIELD_UNDEFINED);
      if (timeZone == 0) {
        buffer.Append('Z');
      }
      else {
        if (timeZone > 0) {
          buffer.Append('+');
        }
        else {
          buffer.Append('-');
          timeZone = 0 - timeZone;
        }
        int h = timeZone / 60;
        printTwoDigits(h, buffer);
        buffer.Append(':');
        int m = timeZone % 60;
        printTwoDigits(m, buffer);
      }
    }

    private static void printTwoDigits(int val, StringBuilder buffer) {
      Debug.Assert(val != FIELD_UNDEFINED && val >= 0 && val < 100);
      if (val < 10) {
        buffer.Append("0");
        buffer.Append((char)('0' + val));
      }
      else {
        buffer.Append((char)('0' + val / 10));
        buffer.Append((char)('0' + val % 10));
      }
    }

    private static void printYear(int year, StringBuilder buffer) {
      Debug.Assert(year != FIELD_UNDEFINED && year != 0);
      if (year < 0) {
        buffer.Append('-');
        year = 0 - year;
      }
      if (year < 10) {
        buffer.Append("000");
        buffer.Append((char)('0' + year));
      }
      else if (year < 100) {
        buffer.Append("00");
        buffer.Append((char)('0' + year / 10));
        buffer.Append((char)('0' + year % 10));
      }
      else if (year < 1000) {
        buffer.Append("0");
        buffer.Append((char)('0' + year / 100));
        buffer.Append((char)('0' + (year / 10) % 10));
        buffer.Append((char)('0' + year % 10));
      }
      else {
        buffer.Append(year);
      }
    }

    private static void printFractionalSecond(BigInteger reverseFractionalSecond, StringBuilder buffer) {
      string reverseDigits = reverseFractionalSecond.ToString(NumberFormatInfo.InvariantInfo);
      int len = reverseDigits.Length;
      for (int i = len - 1; i > -1; i--) {
        buffer.Append('.').Append(reverseDigits[i]);
      }
    }

  }

}