package org.openexi.proc.io;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;

import org.openexi.proc.common.QName;
import org.openexi.proc.util.ExiUriConst;
import org.openexi.schema.EXISchema;
import org.openexi.schema.EXISchemaConst;
import org.openexi.schema.XSDateTime;

public final class DateTimeValueScriber extends DateTimeValueScriberBase {

  public static final DateTimeValueScriber instance;
  static {
    instance = new DateTimeValueScriber();
  }

  private DateTimeValueScriber() {
    super(new QName("exi:dateTime", ExiUriConst.W3C_2009_EXI_URI));
  }

  @Override
  public short getCodecID() {
    return Scriber.CODEC_DATETIME;
  }

  @Override
  public int getBuiltinRCS(int simpleType, Scriber scriber) {
    return BuiltinRCS.RCS_ID_DATETIME;
  }

  ////////////////////////////////////////////////////////////

  @Override
  public boolean process(String value, int tp, EXISchema schema, Scribble scribble, Scriber scriber) {
    if (!trimWhitespaces(value))
      return false;
    final boolean useUTCTime = scribble.booleanValue1;
    final int year, month, day, hour, minute, second;
    BigInteger reverseFractionalSecond = null;
    int tz = XSDateTime.FIELD_UNDEFINED;
    nextPosition = startPosition; // OK, Let's start parsing
    if (!parseYearField(value, scribble) || nextPosition == limitPosition)
      return false;
    year = scribble.intValue1;
    if (value.charAt(nextPosition++) != '-' || nextPosition == limitPosition)
      return false;
    if (!parseMonthField(value, scribble) || nextPosition == limitPosition)
      return false;
    month = scribble.intValue1;
    if (value.charAt(nextPosition++) != '-' || nextPosition == limitPosition)
      return false;
    if (!parseDayField(value, year, month, scribble) || nextPosition == limitPosition)
      return false;
    day = scribble.intValue1;
    if (value.charAt(nextPosition++) != 'T' || nextPosition == limitPosition)
      return false;
    if (!parseHourField(value, scribble) || nextPosition == limitPosition)
      return false;
    hour = scribble.intValue1;
    if (value.charAt(nextPosition++) != ':' || nextPosition == limitPosition)
      return false;
    if (!parseMinuteField(value, scribble) || nextPosition == limitPosition)
      return false;
    minute = scribble.intValue1;
    if (hour == 24 && minute != 0)
      return false;
    if (value.charAt(nextPosition++) != ':' || nextPosition == limitPosition)
      return false;
    if (!parseSecondField(value, scribble))
      return false;
    second = scribble.intValue1;
    if (hour == 24 && second != 0)
      return false;
    if (nextPosition != limitPosition) {
      if (value.charAt(nextPosition) == '.') {
        if (++nextPosition == limitPosition)
          return false;
        else if ((reverseFractionalSecond = parseFractionalSecondField(value)) == null)
          return false;
        else if (reverseFractionalSecond.signum() == 0)
          reverseFractionalSecond = null;
      }
      if (nextPosition != limitPosition) {
        if (!parseTimezoneField(value, scribble) || nextPosition != limitPosition)
          return false;
        tz = scribble.intValue1;
      }
    }
    scribble.dateTime = new XSDateTime(year, month, day, hour, minute, second, reverseFractionalSecond, tz, EXISchemaConst.DATETIME_TYPE);
    if (useUTCTime)
      scribble.dateTime.normalize();
    return true;
  }

  @Override
  void scribeDateTimeValue(XSDateTime dateTime, OutputStream channelStream, Scriber scriber) throws IOException {
    writeYear(dateTime.year, channelStream, scriber);
    writeMonthDay(dateTime.month, dateTime.day, channelStream, scriber);
    writeTime(dateTime.hour, dateTime.minute, dateTime.second,
        dateTime.reverseFractionalSecond, channelStream, scriber);
    writeTimeZone(dateTime.timeZone, channelStream, scriber);
  }
  
}
