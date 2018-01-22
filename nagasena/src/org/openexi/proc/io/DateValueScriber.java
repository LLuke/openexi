package org.openexi.proc.io;

import java.io.IOException;
import java.io.OutputStream;

import org.openexi.proc.common.QName;
import org.openexi.schema.EXISchema;
import org.openexi.schema.EXISchemaConst;
import org.openexi.schema.XSDateTime;

public final class DateValueScriber extends DateTimeValueScriberBase {

  public static final DateValueScriber instance;
  static {
    instance = new DateValueScriber();
  }

  private DateValueScriber() {
    super(new QName("exi:date", "http://www.w3.org/2009/exi"));
  }

  @Override
  public short getCodecID() {
    return Scriber.CODEC_DATE;
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
    final int year, month, day;
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
    if (!parseDayField(value, year, month, scribble))
      return false;
    day = scribble.intValue1;
    if (nextPosition != limitPosition) {
      if (!parseTimezoneField(value, scribble) || nextPosition != limitPosition)
        return false;
      tz = scribble.intValue1;
    }
    scribble.dateTime = new XSDateTime(year, month, day, 
        XSDateTime.FIELD_UNDEFINED, XSDateTime.FIELD_UNDEFINED, 
        XSDateTime.FIELD_UNDEFINED, null, 
        tz, EXISchemaConst.DATE_TYPE);
    scribble.dateTime.normalize(useUTCTime);
    return true;
  }

  ////////////////////////////////////////////////////////////
  
  @Override
  void scribeDateTimeValue(XSDateTime dateTime, OutputStream channelStream, Scriber scriber) throws IOException {
    writeYear(dateTime.year, channelStream, scriber);
    writeMonthDay(dateTime.month, dateTime.day, channelStream, scriber);
    writeTimeZone(dateTime.timeZone, channelStream, scriber);
  }

}
