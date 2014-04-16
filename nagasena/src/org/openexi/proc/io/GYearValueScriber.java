package org.openexi.proc.io;

import java.io.IOException;
import java.io.OutputStream;

import org.openexi.proc.common.QName;
import org.openexi.proc.util.ExiUriConst;
import org.openexi.schema.EXISchema;
import org.openexi.schema.EXISchemaConst;
import org.openexi.schema.XSDateTime;

public final class GYearValueScriber extends DateTimeValueScriberBase {

  public static final GYearValueScriber instance;
  static {
    instance = new GYearValueScriber();
  }
  
  private GYearValueScriber() {
    super(new QName("exi:gYear", ExiUriConst.W3C_2009_EXI_URI));
  }

  @Override
  public short getCodecID() {
    return Scriber.CODEC_GYEAR;
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
    final int year;
    int tz = XSDateTime.FIELD_UNDEFINED;
    nextPosition = startPosition; // OK, Let's start parsing
    if (!parseYearField(value, scribble))
      return false;
    year = scribble.intValue1;
    if (nextPosition != limitPosition) {
      if (!parseTimezoneField(value, scribble) || nextPosition != limitPosition)
        return false;
      tz = scribble.intValue1;
    }
    scribble.dateTime = new XSDateTime(year, XSDateTime.FIELD_UNDEFINED, 
        XSDateTime.FIELD_UNDEFINED, XSDateTime.FIELD_UNDEFINED,
        XSDateTime.FIELD_UNDEFINED, XSDateTime.FIELD_UNDEFINED, 
        null, tz, EXISchemaConst.G_YEAR_TYPE);
    return true;
  }

  ////////////////////////////////////////////////////////////
  
  @Override
  void scribeDateTimeValue(XSDateTime dateTime, OutputStream channelStream, Scriber scriber) throws IOException {
    writeYear(dateTime.year, channelStream, scriber);
    writeTimeZone(dateTime.timeZone, channelStream, scriber);
  }
  
}
