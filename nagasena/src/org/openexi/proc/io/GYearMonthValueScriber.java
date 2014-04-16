package org.openexi.proc.io;

import java.io.IOException;
import java.io.OutputStream;

import org.openexi.proc.common.QName;
import org.openexi.proc.util.ExiUriConst;
import org.openexi.schema.EXISchema;
import org.openexi.schema.EXISchemaConst;
import org.openexi.schema.XSDateTime;

public final class GYearMonthValueScriber extends DateTimeValueScriberBase {

  public static final GYearMonthValueScriber instance;
  static {
    instance = new GYearMonthValueScriber();
  }

  private GYearMonthValueScriber() {
    super(new QName("exi:gYearMonth", ExiUriConst.W3C_2009_EXI_URI));
  }

  @Override
  public short getCodecID() {
    return Scriber.CODEC_GYEARMONTH;
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
    final int year, month;
    int tz = XSDateTime.FIELD_UNDEFINED;
    nextPosition = startPosition; // OK, Let's start parsing
    if (!parseYearField(value, scribble) || nextPosition == limitPosition)
      return false;
    year = scribble.intValue1;
    if (value.charAt(nextPosition++) != '-' || nextPosition == limitPosition)
      return false;
    if (!parseMonthField(value, scribble))
      return false;
    month = scribble.intValue1;
    if (nextPosition != limitPosition) {
      if (!parseTimezoneField(value, scribble) || nextPosition != limitPosition)
        return false;
      tz = scribble.intValue1;
    }
    scribble.dateTime = new XSDateTime(year, month, XSDateTime.FIELD_UNDEFINED, 
        XSDateTime.FIELD_UNDEFINED, XSDateTime.FIELD_UNDEFINED, 
        XSDateTime.FIELD_UNDEFINED, null, 
        tz, EXISchemaConst.G_YEARMONTH_TYPE);
    return true;
  }

  ////////////////////////////////////////////////////////////
  
  @Override
  void scribeDateTimeValue(XSDateTime dateTime, OutputStream channelStream, Scriber scriber) throws IOException {
    writeYear(dateTime.year, channelStream, scriber);
    writeMonthDay(dateTime.month, 0, channelStream, scriber);
    writeTimeZone(dateTime.timeZone, channelStream, scriber);
  }
  
}
