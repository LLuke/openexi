package org.openexi.fujitsu.proc.io;

import java.io.IOException;
import java.io.OutputStream;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.openexi.fujitsu.proc.common.QName;
import org.openexi.fujitsu.schema.EXISchema;

final class DateValueScriber extends DateTimeValueScriberBase {
  
  public DateValueScriber(Scriber scriber, DatatypeFactory datatypeFactory) {
    super(scriber, new QName("exi:date", "http://www.w3.org/2009/exi"), datatypeFactory);
  }

  @Override
  public short getCodecID() {
    return Scriber.CODEC_DATE;
  }
  
  @Override
  public int getBuiltinRCS(int simpleType) {
    return BuiltinRCS.RCS_ID_DATETIME;
  }

  ////////////////////////////////////////////////////////////

  @Override
  public boolean process(String value, int tp, EXISchema schema, Scribble scribble) {
    if (!trimWhitespaces(value))
      return false;
    
    value = value.substring(startPosition, limitPosition);
    
    XMLGregorianCalendar dateTime = null;
    javax.xml.namespace.QName qname = null;
    try {
      dateTime = m_datatypeFactory.newXMLGregorianCalendar(value);
      qname = dateTime.getXMLSchemaType();
    }
    catch (RuntimeException re) {
    }
    if (dateTime == null || qname == null)
      return false;
    if (DatatypeConstants.DATE != qname)
      return false;
    scribble.calendar = dateTime;
    return true;
  }

  ////////////////////////////////////////////////////////////
  
  @Override
  void scribeDateTimeValue(XMLGregorianCalendar dateTime, OutputStream channelStream) throws IOException {
    writeYear(dateTime.getYear(), channelStream);
    writeMonthDay(dateTime.getMonth(), dateTime.getDay(), channelStream);
    writeTimeZone(dateTime.getTimezone(), channelStream);
  }

}
