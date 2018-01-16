package org.openexi.schema;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;

public final class XSDateTime {
  
  public static final int FIELD_UNDEFINED = Integer.MIN_VALUE;
  
  /**
   * One of the eight calendar-related datatype identifers defined 
   * in EXISchemaConst.  
   */
  private byte primTypeId; 
  
  public int year, month, day, hour, minute, second, timeZone;
  public BigInteger reverseFractionalSecond;
  
  private final static int[] LAST_DAY_OF_MONTH; 
  static {
    LAST_DAY_OF_MONTH = new int[] { FIELD_UNDEFINED, 31, -1, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };
  }
  private final static int DEFAULT_YEAR  = 4;
  
  public static int getLastDayOfMonth(int year, int month) {
    if (month == FIELD_UNDEFINED) {
      assert year == FIELD_UNDEFINED;
      month = 1;
    }
    if (year == FIELD_UNDEFINED)
      year = DEFAULT_YEAR;
    /**
     * M := modulo(monthValue, 1, 13)
     * Y := yearValue + fQuotient(monthValue, 1, 13)
     */
    final long quotientModulo = calculateQuotientModulo(month, 1, 13);
    month = getModulo(quotientModulo);
    year = year + getQuotient(quotientModulo);
    
    int lastDay;
    if ((lastDay = LAST_DAY_OF_MONTH[month]) == -1) {
      assert month == 2;
      if (year == FIELD_UNDEFINED || 
          year % 400 == 0 || year % 100 != 0 && year % 4 == 0)
        lastDay = 29;
      else
        lastDay = 28;
    }
    return lastDay;
  }

  XSDateTime(XSDateTime dateTime) {
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
  
  public XSDateTime(int year, int month, int day, int hour, int minute, 
      int second, BigInteger reverseFractionalSecond, int timeZone, byte primTypeId) {
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
  
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof XSDateTime) {
      final XSDateTime dateTime = (XSDateTime)obj;
      final int timeZone1, timeZone2;
      timeZone1 = timeZone;
      timeZone2 = dateTime.timeZone;
      // Both are normalized values.
      assert (timeZone1 == 0 || timeZone1 == FIELD_UNDEFINED) &&
          (timeZone2 == 0 || timeZone2 == FIELD_UNDEFINED);
      if (timeZone != dateTime.timeZone)
        return false;
      else {
        if (primTypeId != dateTime.primTypeId ||
            year != dateTime.year || month != dateTime.month ||
            day != dateTime.day || hour != dateTime.hour ||
            minute != dateTime.minute || second != dateTime.second)
          return false;
        if (reverseFractionalSecond == null)
          return dateTime.reverseFractionalSecond == null || dateTime.reverseFractionalSecond.signum() == 0;
        else if (dateTime.reverseFractionalSecond == null)
          return reverseFractionalSecond.signum() == 0;
        else
          return reverseFractionalSecond.equals(dateTime.reverseFractionalSecond);
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
        assert false;
        return;
    }
    
    final int durationMinute = timeZone != FIELD_UNDEFINED ? 0 - timeZone : 0; 
    assert -840 <= durationMinute && durationMinute <= 840; // 14 * 60 = 840
    
    final int dMinute = durationMinute % 60;
    final int dHour = durationMinute / 60;
    
    int carry, temp;
    long quotientModulo;
    /**
     * EXI does not modify seconds part.
     */
    carry = 0;
    /**
     * Minutes
     * temp := S[minute] + D[minute] + carry
     * E[minute] := modulo(temp, 60)
     * carry := fQuotient(temp, 60)
     */
    quotientModulo = calculateQuotientModulo(minute + dMinute + carry, 60);
    minute = getModulo(quotientModulo); 
    carry = getQuotient(quotientModulo);
    /**
     * Hours
     * temp := S[hour] + D[hour] + carry
     * E[hour] := modulo(temp, 24)
     * carry := fQuotient(temp, 24)
     */
    quotientModulo = calculateQuotientModulo(hour + dHour + carry, 24);
    hour = getModulo(quotientModulo);
    carry = getQuotient(quotientModulo);
    
    /**
     * Days
     * if S[day] > maximumDayInMonthFor(E[year], E[month])
     *   tempDays := maximumDayInMonthFor(E[year], E[month])
     * else if S[day] < 1
     *   tempDays := 1
     * else
     *   tempDays := S[day]
     * E[day] := tempDays + D[day] + carry
     * START LOOP
     *   IF E[day] < 1
     *     E[day] := E[day] + maximumDayInMonthFor(E[year], E[month] - 1)
     *     carry := -1
     *   ELSE IF E[day] > maximumDayInMonthFor(E[year], E[month]) 
     *     E[day] := E[day] - maximumDayInMonthFor(E[year], E[month])
     *     carry := 1
     *   ELSE EXIT LOOP
     *   temp := E[month] + carry
     *   E[month] := modulo(temp, 1, 13)
     *   E[year] := E[year] + fQuotient(temp, 1, 13)
     *   GOTO START LOOP
     */
    int tempDays, lastDay;
    if (day > (lastDay = getLastDayOfMonth(year, month)))
      tempDays = lastDay;
    else if (day < 1)
      tempDays = 1;
    else
      tempDays = day;
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
      else
        break;
      temp = month + carry;
      quotientModulo = calculateQuotientModulo(temp, 1, 13);
      month = getModulo(quotientModulo);
      year = year + getQuotient(quotientModulo);
    }
    
    timeZone = timeZone != FIELD_UNDEFINED ? 0 : FIELD_UNDEFINED;
  }

  private static long calculateQuotientModulo(int value, int low, int high) {
    final long quotientModulo = calculateQuotientModulo(value - low, high - low);
    int quotient = getQuotient(quotientModulo);
    int modulo = getModulo(quotientModulo) + low;
    return toCompositeLong(quotient, modulo);
  }
  
  private static long calculateQuotientModulo(int value, int factor) {
    assert factor > 0;
    int quotient, modulo;
    quotient = value / factor;
    if ((modulo = value % factor) < 0) {
      quotient -= 1;
      modulo += factor;
    }
    return toCompositeLong(quotient, modulo);
  }
  
  private static long toCompositeLong(int quotient, int modulo) {
    return ((long)quotient) << 32 | modulo;
  }
  
  private static int getQuotient(long quotientModulo) {
    return (int)(quotientModulo >>> 32);
  }

  private static int getModulo(long quotientModulo) {
    return (int)(quotientModulo & 0xFFFFFFFFL);
  }
  
  ///////////////////////////////////////////////////////////////////////////
  // Serialization/Deserialization
  ///////////////////////////////////////////////////////////////////////////

  public static XSDateTime readIn(DataInputStream in) throws IOException, ClassNotFoundException {
    final XSDateTime dateTime = new XSDateTime();
    dateTime.primTypeId = (byte)in.read(); 

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
        dateTime.hour = in.readInt();
        dateTime.minute = in.readInt();
        dateTime.second = in.readInt();
        dateTime.reverseFractionalSecond = readPredicatedInteger(in);
        break;
      case EXISchemaConst.DATE_TYPE:
        dateTime.year = in.readInt(); 
        dateTime.month = in.readInt();
        dateTime.day = in.readInt();
        break;
      case EXISchemaConst.G_YEARMONTH_TYPE:
        dateTime.year = in.readInt(); 
        dateTime.month = in.readInt();
        break;
      case EXISchemaConst.G_YEAR_TYPE:
        dateTime.year = in.readInt(); 
        break;
      case EXISchemaConst.G_MONTHDAY_TYPE:
        dateTime.month = in.readInt();
        dateTime.day = in.readInt();
        break;
      case EXISchemaConst.G_DAY_TYPE:
        dateTime.day = in.readInt();
        break;
      case EXISchemaConst.G_MONTH_TYPE:
        dateTime.month = in.readInt();
        break;
      case EXISchemaConst.DATETIME_TYPE:
        dateTime.year = in.readInt(); 
        dateTime.month = in.readInt();
        dateTime.day = in.readInt();
        dateTime.hour = in.readInt();
        dateTime.minute = in.readInt();
        dateTime.second = in.readInt();
        dateTime.reverseFractionalSecond = readPredicatedInteger(in);
        break;
      default:
        assert false;
        break;
    }
    dateTime.timeZone = readPredicatedInt(in);
    return dateTime;
  }
  
  public void writeOut(DataOutputStream out) throws IOException {
    out.write(primTypeId);
    switch (primTypeId) {
      case EXISchemaConst.TIME_TYPE:
        out.writeInt(hour);
        out.writeInt(minute);
        out.writeInt(second);
        writePredicatedInteger(reverseFractionalSecond, out);
        break;
      case EXISchemaConst.DATE_TYPE:
        out.writeInt(year);
        out.writeInt(month);
        out.writeInt(day);
        break;
      case EXISchemaConst.G_YEARMONTH_TYPE:
        out.writeInt(year);
        out.writeInt(month);
        break;
      case EXISchemaConst.G_YEAR_TYPE:
        out.writeInt(year);
        break;
      case EXISchemaConst.G_MONTHDAY_TYPE:
        out.writeInt(month);
        out.writeInt(day);
        break;
      case EXISchemaConst.G_DAY_TYPE:
        out.writeInt(day);
        break;
      case EXISchemaConst.G_MONTH_TYPE:
        out.writeInt(month);
        break;
      case EXISchemaConst.DATETIME_TYPE:
        out.writeInt(year);
        out.writeInt(month);
        out.writeInt(day);
        out.writeInt(hour);
        out.writeInt(minute);
        out.writeInt(second);
        writePredicatedInteger(reverseFractionalSecond, out);
        break;
      default:
        assert false;
        break;
    }
    writePredicatedInt(timeZone, out);
  }
  
  private static void writePredicatedInt(int val, DataOutputStream out) throws IOException {
    if (val != FIELD_UNDEFINED) {
      out.write(1);
      out.writeInt(val);
    }
    else
      out.write(0);
  }
  
  private static int readPredicatedInt(DataInputStream in) throws IOException {
    final int predicate;
    if ((predicate = in.read()) == 1)
      return in.readInt();
    else {
      assert predicate == 0;
      return FIELD_UNDEFINED;
    }
  }

  private static void writePredicatedInteger(BigInteger val, DataOutputStream out) throws IOException {
    if (val != null) {
      out.write(1);
      EXISchema.writeString(val.toString(), out);
    }
    else
      out.write(0);
  }
  
  private static BigInteger readPredicatedInteger(DataInputStream in) throws IOException {
    final int predicate;
    if ((predicate = in.read()) == 1)
      return new BigInteger(EXISchema.readString(in));
    else {
      assert predicate == 0;
      return null;
    }
  }
  
  ///////////////////////////////////////////////////////////////////////////
  // toString() implementation
  ///////////////////////////////////////////////////////////////////////////

  @Override
  public String toString() {
    final StringBuilder buffer = new StringBuilder();
    switch (primTypeId) {
      case EXISchemaConst.TIME_TYPE:
        printTwoDigits(hour, buffer);
        printTwoDigits(minute, buffer.append(':'));
        printTwoDigits(second, buffer.append(':'));
        if (reverseFractionalSecond != null)
          printFractionalSecond(reverseFractionalSecond, buffer);
        break;
      case EXISchemaConst.DATE_TYPE:
        printYear(year, buffer);
        printTwoDigits(month, buffer.append('-'));
        printTwoDigits(day, buffer.append('-'));
        break;
      case EXISchemaConst.G_YEARMONTH_TYPE:
        printYear(year, buffer);
        printTwoDigits(month, buffer.append('-'));
        break;
      case EXISchemaConst.G_YEAR_TYPE:
        printYear(year, buffer);
        break;
      case EXISchemaConst.G_MONTHDAY_TYPE:
        printTwoDigits(month, buffer.append("--"));
        printTwoDigits(day, buffer.append('-'));
        break;
      case EXISchemaConst.G_DAY_TYPE:
        printTwoDigits(day, buffer.append("---"));
        break;
      case EXISchemaConst.G_MONTH_TYPE:
        printTwoDigits(month, buffer.append("--"));
        break;
      case EXISchemaConst.DATETIME_TYPE:
        printYear(year, buffer);
        printTwoDigits(month, buffer.append('-'));
        printTwoDigits(day, buffer.append('-'));
        buffer.append('T');
        printTwoDigits(hour, buffer);
        printTwoDigits(minute, buffer.append(':'));
        printTwoDigits(second, buffer.append(':'));
        if (reverseFractionalSecond != null)
          printFractionalSecond(reverseFractionalSecond, buffer);
        break;
      default:
        assert false;
        return null;
    }
    if (timeZone != FIELD_UNDEFINED)
      printTimezone(timeZone, buffer);
    return buffer.toString();
  }
  
  private static void printTimezone(int timeZone, StringBuilder buffer) {
    assert timeZone != FIELD_UNDEFINED;
    if (timeZone == 0)
      buffer.append('Z');
    else {
      if (timeZone > 0)
        buffer.append('+');
      else {
        buffer.append('-');
        timeZone = 0 - timeZone;
      }
      final int h = timeZone / 60;
      printTwoDigits(h, buffer);
      buffer.append(':');
      final int m = timeZone % 60;
      printTwoDigits(m, buffer);
    }
  }
  
  private static void printTwoDigits(int val, StringBuilder buffer) {
    assert val != FIELD_UNDEFINED && val >= 0 && val < 100;
    if (val < 10) {
      buffer.append("0");
      buffer.append((char)('0' + val));
    }
    else {
      buffer.append((char)('0' + val / 10));
      buffer.append((char)('0' + val % 10));
    }
  }

  private static void printYear(int year, StringBuilder buffer) {
    assert year != FIELD_UNDEFINED && year != 0;
    if (year < 0) {
      buffer.append('-');
      year = 0 - year;
    }
    if (year < 10) {
      buffer.append("000");
      buffer.append((char)('0' + year));
    }
    else if (year < 100) {
      buffer.append("00");
      buffer.append((char)('0' + year / 10));
      buffer.append((char)('0' + year % 10));
    }
    else if (year < 1000) { 
      buffer.append("0");
      buffer.append((char)('0' + year / 100));
      buffer.append((char)('0' + (year / 10) % 10));
      buffer.append((char)('0' + year % 10));
    }
    else
      buffer.append(year);
  }

  private static void printFractionalSecond(BigInteger reverseFractionalSecond, StringBuilder buffer) {
    final String reverseDigits = reverseFractionalSecond.toString();
    final int len = reverseDigits.length();
    for (int i = len - 1; i > -1; i--)
      buffer.append('.').append(reverseDigits.charAt(i));
  }
  
}
