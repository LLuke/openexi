package org.openexi.proc.io;

import java.io.IOException;
import java.io.OutputStream;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.openexi.proc.common.QName;
import org.openexi.schema.EXISchema;

class TimeValueScriber extends DateTimeValueScriberBase {
  
  public TimeValueScriber(Scriber scriber, DatatypeFactory datatypeFactory) {
    super(scriber, new QName("exi:time", "http://www.w3.org/2009/exi"), datatypeFactory);
  }

  @Override
  public short getCodecID() {
    return Scriber.CODEC_TIME;
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
    if (DatatypeConstants.TIME != qname)
      return false;
    scribble.calendar = dateTime;
    return true;
  }

  ////////////////////////////////////////////////////////////
  
  @Override
  void scribeDateTimeValue(XMLGregorianCalendar dateTime, OutputStream channelStream) throws IOException {
    writeTime(dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond(), dateTime.getFractionalSecond(), channelStream);
    writeTimeZone(dateTime.getTimezone(), channelStream);
  }
  
}
