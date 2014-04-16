package org.openexi.fujitsu.proc.io;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.openexi.fujitsu.proc.common.QName;
import org.openexi.fujitsu.schema.EXISchema;

abstract class DateTimeValueScriberBase extends ValueScriberBase {
  
  protected final DatatypeFactory m_datatypeFactory;
  protected final Scribble m_scribble;

  protected DateTimeValueScriberBase(Scriber scriber, QName name, DatatypeFactory datatypeFactory) {
    super(scriber, name);
    m_datatypeFactory = datatypeFactory;
    m_scribble  = new Scribble();
  }

  @Override
  public final Object toValue(String value, Scribble scribble) {
    return scribble.calendar;
  }

  @Override
  public final void doScribe(Object value, String localName, String uri, int tp, OutputStream channelStream) throws IOException  {
    scribeDateTimeValue((XMLGregorianCalendar)value, channelStream);
  }
  
  @Override
  public final void scribe(String value, Scribble scribble, String localName, String uri, int tp, OutputStream channelStream) throws IOException {
    scribeDateTimeValue(scribble.calendar, channelStream);
  }

  abstract void scribeDateTimeValue(XMLGregorianCalendar dateTime, OutputStream channelStream) throws IOException;

  protected final void writeYear(int year, OutputStream ostream) throws IOException {
    if ((year -= 2000) < 0) {
      m_scriber.writeBoolean(true, ostream);
      year = -1 - year;
    }
    else {
      m_scriber.writeBoolean(false, ostream);
    }
    m_scriber.writeUnsignedInteger32(year, ostream);
  }

  protected final void writeMonthDay(int month, int day, OutputStream ostream) throws IOException {
    final int monthDay = month * 32 + day;
    m_scriber.writeNBitUnsigned(monthDay, 9, ostream);
  }

  protected final void writeTime(int hour, int minute, int second, BigDecimal fractionalSecond, OutputStream ostream) throws IOException {
    int time = (hour * 64 + minute) * 64 + second;
    m_scriber.writeNBitUnsigned(time, 17, ostream);
    if (fractionalSecond != null) {
      m_scriber.writeBoolean(true, ostream);
      m_scriber.m_decimalValueScriberInherent.process(fractionalSecond.toPlainString(), EXISchema.NIL_NODE, (EXISchema)null, m_scribble);
      final int intValue = Integer.parseInt(m_scribble.stringValue2);
      m_scriber.writeUnsignedInteger32(intValue, ostream);
    }
    else {
      m_scriber.writeBoolean(false, ostream);
    }
  }

  protected final void writeTimeZone(int timeZone, OutputStream ostream) throws IOException {
    if (timeZone != DatatypeConstants.FIELD_UNDEFINED) {
      m_scriber.writeBoolean(true, ostream);
      final int hours = timeZone / 60;
      final int minutes = timeZone % 60;
      m_scriber.writeNBitUnsigned((hours + 14) * 64 + minutes, 11, ostream);
    }
    else {
      m_scriber.writeBoolean(false, ostream);
    }
  }

}
