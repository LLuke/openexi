package org.openexi.fujitsu.schema;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

public final class XSDateTime implements Externalizable {
  
  private XMLGregorianCalendar m_calendar;
  
  private int m_typeSerial; 
  
  private static final DatatypeFactory m_datatypeFactory;
  static {
    DatatypeFactory datatypeFactory = null;
    try {
      datatypeFactory = DatatypeFactory.newInstance();
    }
    catch(DatatypeConfigurationException dce) {
      throw new RuntimeException(dce);
    }
    finally {
      m_datatypeFactory = datatypeFactory;
    }
  }
  
  public XSDateTime() {
  }
  
  public XSDateTime(int year, int month, int day, int hour, int minute, 
      int second, int milliSecond, int timeZone, int typeSerial) {
    init(year, month, day, hour, minute, second, milliSecond, timeZone, typeSerial);
  }
  
  private void init(int year, int month, int day, int hour, int minute,
      int second, int milliSecond, int timeZone, int typeSerial) {
    this.m_typeSerial = typeSerial;
    m_calendar = m_datatypeFactory.newXMLGregorianCalendar(
        year, month, day, hour, minute, second, milliSecond, timeZone);
  }
  
  public XMLGregorianCalendar getXMLGregorianCalendar() {
    return m_calendar;
  }
  
  public int getTypeSerial() {
    return m_typeSerial;
  }
  
  public boolean equals(Object obj) {
    if (obj instanceof XSDateTime) {
      final XSDateTime dateTime = (XSDateTime)obj;
      return m_typeSerial == dateTime.m_typeSerial && m_calendar.equals(dateTime.getXMLGregorianCalendar());
    }
    return false;
  }

  ///////////////////////////////////////////////////////////////////////////
  // Serialization/Deserialization
  ///////////////////////////////////////////////////////////////////////////

  public void readExternal(ObjectInput in)
      throws IOException, ClassNotFoundException {

    m_typeSerial = in.readInt();

    final int year = in.readInt();
    final int month = in.readInt();
    final int day = in.readInt();
    final int hour = in.readInt();
    final int minute = in.readInt();
    final int second = in.readInt();
    final int milliSecond = in.readInt();
    final int timeZone = in.readInt();

    init(year, month, day, hour, minute, second, milliSecond, timeZone, m_typeSerial);
  }
  
  public void writeExternal(ObjectOutput out) throws IOException {
    final int year = m_calendar.getYear();
    final int month = m_calendar.getMonth();
    final int day = m_calendar.getDay();
    final int hour = m_calendar.getHour();
    final int minute = m_calendar.getMinute();
    final int second = m_calendar.getSecond();
    final int milliSecond = m_calendar.getMillisecond();
    final int timeZone = m_calendar.getTimezone();
    
    out.writeInt(m_typeSerial);
    out.writeInt(year);
    out.writeInt(month);
    out.writeInt(day);
    out.writeInt(hour);
    out.writeInt(minute);
    out.writeInt(second);
    out.writeInt(milliSecond);
    out.writeInt(timeZone);
  }

}
