package org.openexi.proc.io;

import java.io.IOException;
import java.io.OutputStream;

import org.openexi.proc.common.QName;
import org.openexi.schema.EXISchema;
import org.openexi.schema.EXISchemaConst;
import org.openexi.schema.XSDateTime;

public final class GMonthValueScriber extends DateTimeValueScriberBase {

  public static final GMonthValueScriber instance;
  static {
    instance = new GMonthValueScriber();
  }

  private GMonthValueScriber() {
    super(new QName("exi:gMonth", "http://www.w3.org/2009/exi"));
  }

  @Override
  public short getCodecID() {
    return Scriber.CODEC_GMONTH;
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
    final int month;
    int tz = XSDateTime.FIELD_UNDEFINED;
    nextPosition = startPosition; // OK, Let's start parsing
    if (value.charAt(nextPosition++) != '-' || nextPosition == limitPosition || 
        value.charAt(nextPosition++) != '-'  || nextPosition == limitPosition)
      return false;
    if (!parseMonthField(value, scribble))
      return false;
    month = scribble.intValue1;
    if (nextPosition != limitPosition) {
      if (!parseTimezoneField(value, scribble) || nextPosition != limitPosition)
        return false;
      tz = scribble.intValue1;
    }
    scribble.dateTime = new XSDateTime(XSDateTime.FIELD_UNDEFINED, 
        month, XSDateTime.FIELD_UNDEFINED, XSDateTime.FIELD_UNDEFINED, 
        XSDateTime.FIELD_UNDEFINED, XSDateTime.FIELD_UNDEFINED, 
        null, tz, EXISchemaConst.G_MONTH_TYPE);
    return true;
  }

  ////////////////////////////////////////////////////////////
  
  @Override
  void scribeDateTimeValue(XSDateTime dateTime, OutputStream channelStream, Scriber scriber) throws IOException {
    writeMonthDay(dateTime.month, 0, channelStream, scriber);
    writeTimeZone(dateTime.timeZone, channelStream, scriber);
  }

}
