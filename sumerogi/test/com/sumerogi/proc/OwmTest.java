package com.sumerogi.proc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import com.sumerogi.proc.common.AlignmentType;
import com.sumerogi.proc.common.EventDescription;
import com.sumerogi.proc.common.EventType;
import com.sumerogi.proc.io.Scanner;

import junit.framework.Assert;
import junit.framework.TestCase;

public class OwmTest extends TestCase {
  
  public OwmTest(String name) {
    super(name);
  }

  private static final AlignmentType[] Alignments = new AlignmentType[] {
    AlignmentType.bitPacked, 
    AlignmentType.byteAligned, 
    AlignmentType.preCompress,
    AlignmentType.compress
  };

  ///////////////////////////////////////////////////////////////////////////
  // Test cases
  ///////////////////////////////////////////////////////////////////////////

  /**
   */
  public void testData_01() throws Exception {
  
    Transmogrifier encoder = new Transmogrifier();
    ESONDecoder decoder = new ESONDecoder();
    
    for (AlignmentType alignment : Alignments) {
      InputStream inputStream = getClass().getResource("/owm/owm-1-1cities.json").openStream();
      
      ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
      encoder.setOutputStream(baos);
      
      encoder.setAlignmentType(alignment);
      encoder.encode(inputStream);
      inputStream.close();
      
      decoder.setInputStream(new ByteArrayInputStream(baos.toByteArray()));
      
      Scanner scanner = decoder.processHeader();
      
      EventDescription event;
      EventType eventType;
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_DOCUMENT, eventType.itemType);
      Assert.assertEquals(EventDescription.EVENT_START_DOCUMENT, event.getEventKind());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_ANONYMOUS, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertNull(event.getName());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertEquals("group", event.getName());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertEquals("current", event.getName());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertEquals("city", event.getName());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("id", event.getName());
      Assert.assertEquals("688532", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("name", event.getName());
      Assert.assertEquals("Yalta", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertEquals("coord", event.getName());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("lon", event.getName());
      Assert.assertEquals("37.27", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("lat", event.getName());
      Assert.assertEquals("46.96", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("country", event.getName());
      Assert.assertEquals("UA", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertEquals("sun", event.getName());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("rise", event.getName());
      Assert.assertEquals("2014-09-12T03:05:08", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("set", event.getName());
      Assert.assertEquals("2014-09-12T15:49:00", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertEquals("temperature", event.getName());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("value", event.getName());
      Assert.assertEquals("292.04", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("min", event.getName());
      Assert.assertEquals("292.04", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("max", event.getName());
      Assert.assertEquals("292.04", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("unit", event.getName());
      Assert.assertEquals("kelvin", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertEquals("humidity", event.getName());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("value", event.getName());
      Assert.assertEquals("78", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("unit", event.getName());
      Assert.assertEquals("%", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertEquals("pressure", event.getName());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("value", event.getName());
      Assert.assertEquals("1014", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("unit", event.getName());
      Assert.assertEquals("hPa", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertEquals("wind", event.getName());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertEquals("speed", event.getName());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("value", event.getName());
      Assert.assertEquals("1.54", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NULL_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NULL, event.getEventKind());
      Assert.assertEquals("name", event.getName());
      Assert.assertEquals("null", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertEquals("direction", event.getName());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("value", event.getName());
      Assert.assertEquals("306", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("code", event.getName());
      Assert.assertEquals("NW", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("name", event.getName());
      Assert.assertEquals("Northwest", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertEquals("clouds", event.getName());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("value", event.getName());
      Assert.assertEquals("0", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("name", event.getName());
      Assert.assertEquals("clear sky", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NULL_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NULL, event.getEventKind());
      Assert.assertEquals("visibility", event.getName());
      Assert.assertEquals("null", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertEquals("precipitation", event.getName());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("mode", event.getName());
      Assert.assertEquals("no", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertEquals("weather", event.getName());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("number", event.getName());
      Assert.assertEquals("800", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("value", event.getName());
      Assert.assertEquals("Sky is Clear", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("icon", event.getName());
      Assert.assertEquals("01n", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertEquals("lastupdate", event.getName());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("value", event.getName());
      Assert.assertEquals("2014-09-12T21:54:18", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_DOCUMENT, eventType.itemType);
    }
  }

  /**
   */
  public void testData_02() throws Exception {
  
    Transmogrifier encoder = new Transmogrifier();
    ESONDecoder decoder = new ESONDecoder();
    
    for (AlignmentType alignment : Alignments) {
      InputStream inputStream = getClass().getResource("/owm/owm-1-2cities.json").openStream();
      
      ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
      encoder.setOutputStream(baos);
      
      encoder.setAlignmentType(alignment);
      encoder.encode(inputStream);
      inputStream.close();
      
      decoder.setInputStream(new ByteArrayInputStream(baos.toByteArray()));
      
      Scanner scanner = decoder.processHeader();
      
      EventDescription event;
      EventType eventType;
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_DOCUMENT, eventType.itemType);
      Assert.assertEquals(EventDescription.EVENT_START_DOCUMENT, event.getEventKind());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_ANONYMOUS, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertNull(event.getName());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertEquals("group", event.getName());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_ARRAY_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_ARRAY, event.getEventKind());
      Assert.assertEquals("current", event.getName());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_ANONYMOUS, eventType.itemType);
      Assert.assertEquals(2, eventType.depth);
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertNull(event.getName());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertEquals("city", event.getName());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("id", event.getName());
      Assert.assertEquals("688532", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("name", event.getName());
      Assert.assertEquals("Yalta", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertEquals("coord", event.getName());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("lon", event.getName());
      Assert.assertEquals("37.27", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("lat", event.getName());
      Assert.assertEquals("46.96", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("country", event.getName());
      Assert.assertEquals("UA", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertEquals("sun", event.getName());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("rise", event.getName());
      Assert.assertEquals("2014-09-12T03:05:08", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("set", event.getName());
      Assert.assertEquals("2014-09-12T15:49:00", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertEquals("temperature", event.getName());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("value", event.getName());
      Assert.assertEquals("292.04", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("min", event.getName());
      Assert.assertEquals("292.04", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("max", event.getName());
      Assert.assertEquals("292.04", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("unit", event.getName());
      Assert.assertEquals("kelvin", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertEquals("humidity", event.getName());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("value", event.getName());
      Assert.assertEquals("78", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("unit", event.getName());
      Assert.assertEquals("%", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertEquals("pressure", event.getName());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("value", event.getName());
      Assert.assertEquals("1014", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("unit", event.getName());
      Assert.assertEquals("hPa", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertEquals("wind", event.getName());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertEquals("speed", event.getName());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("value", event.getName());
      Assert.assertEquals("1.54", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NULL_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NULL, event.getEventKind());
      Assert.assertEquals("name", event.getName());
      Assert.assertEquals("null", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertEquals("direction", event.getName());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("value", event.getName());
      Assert.assertEquals("306", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("code", event.getName());
      Assert.assertEquals("NW", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("name", event.getName());
      Assert.assertEquals("Northwest", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertEquals("clouds", event.getName());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("value", event.getName());
      Assert.assertEquals("0", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("name", event.getName());
      Assert.assertEquals("clear sky", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NULL_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NULL, event.getEventKind());
      Assert.assertEquals("visibility", event.getName());
      Assert.assertEquals("null", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertEquals("precipitation", event.getName());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("mode", event.getName());
      Assert.assertEquals("no", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertEquals("weather", event.getName());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("number", event.getName());
      Assert.assertEquals("800", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("value", event.getName());
      Assert.assertEquals("Sky is Clear", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("icon", event.getName());
      Assert.assertEquals("01n", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertEquals("lastupdate", event.getName());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("value", event.getName());
      Assert.assertEquals("2014-09-12T21:54:18", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_ANONYMOUS, eventType.itemType);
      Assert.assertEquals(1, eventType.depth);
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertNull(event.getName());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_NAMED, eventType.itemType);
      Assert.assertEquals("city", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertEquals("city", event.getName());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_NAMED, eventType.itemType);
      Assert.assertEquals("id", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("id", event.getName());
      Assert.assertEquals("2517733", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_NAMED, eventType.itemType);
      Assert.assertEquals("name", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("name", event.getName());
      Assert.assertEquals("Ferreries", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_NAMED, eventType.itemType);
      Assert.assertEquals("coord", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertEquals("coord", event.getName());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_NAMED, eventType.itemType);
      Assert.assertEquals("lon", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("lon", event.getName());
      Assert.assertEquals("4.01", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_NAMED, eventType.itemType);
      Assert.assertEquals("lat", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("lat", event.getName());
      Assert.assertEquals("39.98", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_NAMED, eventType.itemType);
      Assert.assertEquals("country", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("country", event.getName());
      Assert.assertEquals("ES", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_NAMED, eventType.itemType);
      Assert.assertEquals("sun", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertEquals("sun", event.getName());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_NAMED, eventType.itemType);
      Assert.assertEquals("rise", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("rise", event.getName());
      Assert.assertEquals("2014-09-12T05:22:29", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_NAMED, eventType.itemType);
      Assert.assertEquals("set", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("set", event.getName());
      Assert.assertEquals("2014-09-12T17:57:39", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_NAMED, eventType.itemType);
      Assert.assertEquals("temperature", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertEquals("temperature", event.getName());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_NAMED, eventType.itemType);
      Assert.assertEquals("value", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("value", event.getName());
      Assert.assertEquals("299.075", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_NAMED, eventType.itemType);
      Assert.assertEquals("min", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("min", event.getName());
      Assert.assertEquals("299.075", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_NAMED, eventType.itemType);
      Assert.assertEquals("max", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("max", event.getName());
      Assert.assertEquals("299.075", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_NAMED, eventType.itemType);
      Assert.assertEquals("unit", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("unit", event.getName());
      Assert.assertEquals("kelvin", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_NAMED, eventType.itemType);
      Assert.assertEquals("humidity", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertEquals("humidity", event.getName());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_NAMED, eventType.itemType);
      Assert.assertEquals("value", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("value", event.getName());
      Assert.assertEquals("100", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_NAMED, eventType.itemType);
      Assert.assertEquals("unit", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("unit", event.getName());
      Assert.assertEquals("%", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_NAMED, eventType.itemType);
      Assert.assertEquals("pressure", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertEquals("pressure", event.getName());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_NAMED, eventType.itemType);
      Assert.assertEquals("value", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("value", event.getName());
      Assert.assertEquals("1028.89", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_NAMED, eventType.itemType);
      Assert.assertEquals("unit", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("unit", event.getName());
      Assert.assertEquals("hPa", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_NAMED, eventType.itemType);
      Assert.assertEquals("wind", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertEquals("wind", event.getName());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_NAMED, eventType.itemType);
      Assert.assertEquals("speed", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertEquals("speed", event.getName());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_NAMED, eventType.itemType);
      Assert.assertEquals("value", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("value", event.getName());
      Assert.assertEquals("4.52", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("name", event.getName());
      Assert.assertEquals("Gentle Breeze", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_NAMED, eventType.itemType);
      Assert.assertEquals("direction", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertEquals("direction", event.getName());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_NAMED, eventType.itemType);
      Assert.assertEquals("value", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("value", event.getName());
      Assert.assertEquals("85.5001", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_NAMED, eventType.itemType);
      Assert.assertEquals("code", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("code", event.getName());
      Assert.assertEquals("E", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_NAMED, eventType.itemType);
      Assert.assertEquals("name", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("name", event.getName());
      Assert.assertEquals("East", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_NAMED, eventType.itemType);
      Assert.assertEquals("clouds", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertEquals("clouds", event.getName());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_NAMED, eventType.itemType);
      Assert.assertEquals("value", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("value", event.getName());
      Assert.assertEquals("0", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_NAMED, eventType.itemType);
      Assert.assertEquals("name", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("name", event.getName());
      Assert.assertEquals("clear sky", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NULL_NAMED, eventType.itemType);
      Assert.assertEquals("visibility", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NULL, event.getEventKind());
      Assert.assertEquals("visibility", event.getName());
      Assert.assertEquals("null", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_NAMED, eventType.itemType);
      Assert.assertEquals("precipitation", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertEquals("precipitation", event.getName());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_NAMED, eventType.itemType);
      Assert.assertEquals("mode", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("mode", event.getName());
      Assert.assertEquals("no", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_NAMED, eventType.itemType);
      Assert.assertEquals("weather", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertEquals("weather", event.getName());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_NAMED, eventType.itemType);
      Assert.assertEquals("number", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("number", event.getName());
      Assert.assertEquals("800", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_NAMED, eventType.itemType);
      Assert.assertEquals("value", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("value", event.getName());
      Assert.assertEquals("Sky is Clear", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_NAMED, eventType.itemType);
      Assert.assertEquals("icon", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("icon", event.getName());
      Assert.assertEquals("01n", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_NAMED, eventType.itemType);
      Assert.assertEquals("lastupdate", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertEquals("lastupdate", event.getName());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_NAMED, eventType.itemType);
      Assert.assertEquals("value", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("value", event.getName());
      Assert.assertEquals("2014-09-12T21:57:17", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_ARRAY, eventType.itemType);
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_DOCUMENT, eventType.itemType);
    }
  }
  
}