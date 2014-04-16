package org.openexi.proc.io;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;

import org.openexi.proc.common.QName;
import org.openexi.schema.XSDateTime;

abstract class DateTimeValueScriberBase extends ValueScriberBase {
  
  protected int nextPosition;
  
  protected DateTimeValueScriberBase(QName name) {
    super(name);
  }

  @Override
  public final Object toValue(String value, Scribble scribble, Scriber scriber) {
    return scribble.dateTime;
  }

  @Override
  public final void doScribe(Object value, int localName, int uri, int tp, OutputStream channelStream, Scriber scriber) throws IOException  {
    scribeDateTimeValue((XSDateTime)value, channelStream, scriber);
  }
  
  @Override
  public final void scribe(String value, Scribble scribble, int localName, int uri, int tp, OutputStream channelStream, Scriber scriber) throws IOException {
    scribeDateTimeValue(scribble.dateTime, channelStream, scriber);
  }

  abstract void scribeDateTimeValue(XSDateTime dateTime, OutputStream channelStream, Scriber scriber) throws IOException;

  ////////////////////////////////////////////////////////////

  public final static void canonicalizeValue(Scribble scribble) {
    scribble.dateTime.normalize();
  }

  ////////////////////////////////////////////////////////////

  protected final void writeYear(int year, OutputStream ostream, Scriber scriber) throws IOException {
    if ((year -= 2000) < 0) {
      scriber.writeBoolean(true, ostream);
      year = -1 - year;
    }
    else {
      scriber.writeBoolean(false, ostream);
    }
    scriber.writeUnsignedInteger32(year, ostream);
  }

  protected final void writeMonthDay(int month, int day, OutputStream ostream, Scriber scriber) throws IOException {
    final int monthDay = month * 32 + day;
    scriber.writeNBitUnsigned(monthDay, 9, ostream);
  }

  protected final void writeTime(int hour, int minute, int second, BigInteger reverseFractionalSecond, OutputStream ostream, Scriber scriber) throws IOException {
    int time = (hour * 64 + minute) * 64 + second;
    scriber.writeNBitUnsigned(time, 17, ostream);
    if (reverseFractionalSecond != null) {
      scriber.writeBoolean(true, ostream);
      assert reverseFractionalSecond.signum() == 1;
      scriber.writeUnsignedInteger(reverseFractionalSecond, ostream);
    }
    else {
      scriber.writeBoolean(false, ostream);
    }
  }

  protected final void writeTimeZone(int timeZone, OutputStream ostream, Scriber scriber) throws IOException {
    if (timeZone != XSDateTime.FIELD_UNDEFINED) {
      scriber.writeBoolean(true, ostream);
      final int hours = timeZone / 60;
      final int minutes = timeZone % 60;
      scriber.writeNBitUnsigned((hours + 14) * 64 + minutes, 11, ostream);
    }
    else {
      scriber.writeBoolean(false, ostream);
    }
  }

  ////////////////////////////////////////////////////////////

  protected final boolean parseYearField(String value, Scribble scribble) {
    final boolean isNegative; 
    if (isNegative = value.charAt(nextPosition) == '-')
      ++nextPosition;
    int year, n_digits;
    for (year = n_digits = 0; nextPosition < limitPosition; n_digits++, nextPosition++) {
      final char c = value.charAt(nextPosition);
      if (c < '0' || '9' < c)
        break;
      year = 10 * year + (c - '0');
    }
    if (n_digits < 4 || year == 0)
      return false;
    scribble.intValue1 = isNegative ? 0 - year : year; 
    return true; 
  }
  
  protected final boolean parseMonthField(String value, Scribble scribble) {
    int month, n_digits;
    for (month = n_digits = 0; nextPosition < limitPosition; n_digits++, nextPosition++) {
      final char c = value.charAt(nextPosition);
      if (c < '0' || '9' < c)
        break;
      month = 10 * month + (c - '0');
    }
    if (n_digits != 2 || month == 0 || 12 < month)
      return false;
    scribble.intValue1 = month;
    return true;
  }

  protected final boolean parseDayField(String value, int year, int month, Scribble scribble) {
    int day, n_digits;
    for (day = n_digits = 0; nextPosition < limitPosition; n_digits++, nextPosition++) {
      final char c = value.charAt(nextPosition);
      if (c < '0' || '9' < c)
        break;
      day = 10 * day + (c - '0');
    }
    if (n_digits != 2 || day == 0 || XSDateTime.getLastDayOfMonth(year, month) < day)
      return false;
    scribble.intValue1 = day;
    return true;
  }

  protected final boolean parseHourField(String value, Scribble scribble) {
    int hour, n_digits;
    for (hour = n_digits = 0; nextPosition < limitPosition; n_digits++, nextPosition++) {
      final char c = value.charAt(nextPosition);
      if (c < '0' || '9' < c)
        break;
      hour = 10 * hour + (c - '0');
    }
    if (n_digits != 2 || 24 < hour)
      return false;
    scribble.intValue1 = hour;
    return true;
  }

  protected final boolean parseMinuteField(String value, Scribble scribble) {
    int minute, n_digits;
    for (minute = n_digits = 0; nextPosition < limitPosition; n_digits++, nextPosition++) {
      final char c = value.charAt(nextPosition);
      if (c < '0' || '9' < c)
        break;
      minute = 10 * minute + (c - '0');
    }
    if (n_digits != 2 || 59 < minute)
      return false;
    scribble.intValue1 = minute;
    return true;
  }

  protected final boolean parseSecondField(String value, Scribble scribble) {
    int second, n_digits;
    for (second = n_digits = 0; nextPosition < limitPosition; n_digits++, nextPosition++) {
      final char c = value.charAt(nextPosition);
      if (c < '0' || '9' < c)
        break;
      second = 10 * second + (c - '0');
    }
    if (n_digits != 2)
      return false;
    scribble.intValue1 = second;
    return true;
  }

  protected final BigInteger parseFractionalSecondField(String value) {
    BigInteger magnitude = BigInteger.ONE;
    BigInteger reverseFractionalSecond = BigInteger.ZERO;
    int n_digits;
    for (n_digits = 0; nextPosition < limitPosition; n_digits++, nextPosition++, magnitude = BigInteger.TEN.multiply(magnitude)) {
      final char c = value.charAt(nextPosition);
      if (c < '0' || '9' < c)
        break;
      reverseFractionalSecond = reverseFractionalSecond.add(magnitude.multiply(BigInteger.valueOf(c - '0')));
    }
    if (n_digits == 0)
      return null;
    return reverseFractionalSecond;
  }
  
  protected final boolean parseTimezoneField(String value, Scribble scribble) {
    int tz = 0;
    boolean isNegative = false; 
    char c = value.charAt(nextPosition++);
    if (c == 'Z') {
      tz = 0;
    }
    else {
      if (c == '-')
        isNegative = true;
      else if (c != '+')
        return false;
      if (nextPosition == limitPosition)
        return false;
      if (!parseHourField(value, scribble) || nextPosition == limitPosition)
        return false;
      int hour = scribble.intValue1;
      if (value.charAt(nextPosition++) != ':' || nextPosition == limitPosition)
        return false;
      if (!parseMinuteField(value, scribble))
        return false;
      int minute = scribble.intValue1;
      tz = 60 * hour + minute;
    }
    if (isNegative)
      tz = 0 - tz;
    scribble.intValue1 = tz;
    return true;
  }
  
}
