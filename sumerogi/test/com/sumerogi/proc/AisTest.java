package com.sumerogi.proc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import com.sumerogi.proc.common.AlignmentType;
import com.sumerogi.proc.common.EventDescription;
import com.sumerogi.proc.common.EventType;
import com.sumerogi.proc.grammars.GrammarCache;
import com.sumerogi.proc.io.Scanner;

import junit.framework.Assert;
import junit.framework.TestCase;

public class AisTest extends TestCase {
  
  public AisTest(String name) {
    super(name);
  }

  private static final AlignmentType[] Alignments = new AlignmentType[] { 
    AlignmentType.bitPacked, 
    AlignmentType.byteAligned 
  };

  ///////////////////////////////////////////////////////////////////////////
  // Test cases
  ///////////////////////////////////////////////////////////////////////////

  /**
   */
  public void testData_01() throws Exception {
  
    EJSONEncoder encoder = new EJSONEncoder();
    EJSONDecoder decoder = new EJSONDecoder();
    
    for (AlignmentType alignment : Alignments) {
      InputStream inputStream = getClass().getResource("/ais/ais-1-0001posreps.json").openStream();
      
      ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
      encoder.setOutputStream(baos);
      
      encoder.setAlignmentType(alignment);
      encoder.encode(inputStream);
      inputStream.close();
      
      byte[] bts = baos.toByteArray();
      
      System.out.println(bts.length);
      
      ByteArrayInputStream bais = new ByteArrayInputStream(bts);
      
      GrammarCache grammarCache = new GrammarCache();
  
      decoder.setGrammarCache(grammarCache);
      
      decoder.setInputStream(bais);
      
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
      Assert.assertEquals("positionReports", event.getName());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertEquals("positionReport", event.getName());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("messageId", event.getName());
      Assert.assertEquals("3", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("repeatIndicator", event.getName());
      Assert.assertEquals("0", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("userId", event.getName());
      Assert.assertEquals("323151387", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("navStatus", event.getName());
      Assert.assertEquals("5", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("rateOfTurn", event.getName());
      Assert.assertEquals("0", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("speedOverGround", event.getName());
      Assert.assertEquals("0", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_BOOLEAN_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_BOOLEAN_VALUE, event.getEventKind());
      Assert.assertEquals("positionAccuracy", event.getName());
      Assert.assertEquals("false", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("longitude", event.getName());
      Assert.assertEquals("4.0327399999999995", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("latitude", event.getName());
      Assert.assertEquals("51.93891833333333", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("courseOverGround", event.getName());
      Assert.assertEquals("894", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("trueHeading", event.getName());
      Assert.assertEquals("93", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("timeStamp", event.getName());
      Assert.assertEquals("49", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("specialManoeuvre", event.getName());
      Assert.assertEquals("0", event.getCharacters().makeString());
      
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
  
    EJSONEncoder encoder = new EJSONEncoder();
    EJSONDecoder decoder = new EJSONDecoder();
    
    for (AlignmentType alignment : Alignments) {
      InputStream inputStream = getClass().getResource("/ais/ais-1-0002posreps.json").openStream();
      
      ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
      encoder.setOutputStream(baos);
      
      encoder.setAlignmentType(alignment);
      encoder.encode(inputStream);
      inputStream.close();
      
      byte[] bts = baos.toByteArray();
      
      System.out.println(bts.length);
      
      ByteArrayInputStream bais = new ByteArrayInputStream(bts);
      
      GrammarCache grammarCache = new GrammarCache();
  
      decoder.setGrammarCache(grammarCache);
      
      decoder.setInputStream(bais);
      
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
      Assert.assertEquals("positionReports", event.getName());
  
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_ARRAY_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_ARRAY, event.getEventKind());
      Assert.assertEquals("positionReport", event.getName());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_ANONYMOUS, eventType.itemType);
      Assert.assertEquals(2, eventType.depth);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertNull(event.getName());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("messageId", event.getName());
      Assert.assertEquals("3", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("repeatIndicator", event.getName());
      Assert.assertEquals("0", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("userId", event.getName());
      Assert.assertEquals("323151387", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("navStatus", event.getName());
      Assert.assertEquals("1", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("rateOfTurn", event.getName());
      Assert.assertEquals("0", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("speedOverGround", event.getName());
      Assert.assertEquals("0", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_BOOLEAN_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_BOOLEAN_VALUE, event.getEventKind());
      Assert.assertEquals("positionAccuracy", event.getName());
      Assert.assertEquals("false", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("longitude", event.getName());
      Assert.assertEquals("117.36497666666666", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("latitude", event.getName());
      Assert.assertEquals("-20.534186666666667", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("courseOverGround", event.getName());
      Assert.assertEquals("3100", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("trueHeading", event.getName());
      Assert.assertEquals("310", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("timeStamp", event.getName());
      Assert.assertEquals("53", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("specialManoeuvre", event.getName());
      Assert.assertEquals("0", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_ANONYMOUS, eventType.itemType);
      Assert.assertEquals(1, eventType.depth);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertNull(event.getName());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_NAMED, eventType.itemType);
      Assert.assertEquals("messageId", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("messageId", event.getName());
      Assert.assertEquals("3", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_NAMED, eventType.itemType);
      Assert.assertEquals("repeatIndicator", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("repeatIndicator", event.getName());
      Assert.assertEquals("0", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_NAMED, eventType.itemType);
      Assert.assertEquals("userId", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("userId", event.getName());
      Assert.assertEquals("839323491", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_NAMED, eventType.itemType);
      Assert.assertEquals("navStatus", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("navStatus", event.getName());
      Assert.assertEquals("0", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_NAMED, eventType.itemType);
      Assert.assertEquals("rateOfTurn", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("rateOfTurn", event.getName());
      Assert.assertEquals("-127", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_NAMED, eventType.itemType);
      Assert.assertEquals("speedOverGround", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("speedOverGround", event.getName());
      Assert.assertEquals("48", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_BOOLEAN_VALUE_NAMED, eventType.itemType);
      Assert.assertEquals("positionAccuracy", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_BOOLEAN_VALUE, event.getEventKind());
      Assert.assertEquals("positionAccuracy", event.getName());
      Assert.assertEquals("true", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_NAMED, eventType.itemType);
      Assert.assertEquals("longitude", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("longitude", event.getName());
      Assert.assertEquals("-128.46390333333332", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_NAMED, eventType.itemType);
      Assert.assertEquals("latitude", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("latitude", event.getName());
      Assert.assertEquals("52.435941666666665", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_NAMED, eventType.itemType);
      Assert.assertEquals("courseOverGround", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("courseOverGround", event.getName());
      Assert.assertEquals("1976", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_NAMED, eventType.itemType);
      Assert.assertEquals("trueHeading", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("trueHeading", event.getName());
      Assert.assertEquals("200", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_NAMED, eventType.itemType);
      Assert.assertEquals("timeStamp", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("timeStamp", event.getName());
      Assert.assertEquals("52", event.getCharacters().makeString());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_NAMED, eventType.itemType);
      Assert.assertEquals("specialManoeuvre", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertEquals("specialManoeuvre", event.getName());
      Assert.assertEquals("0", event.getCharacters().makeString());
      
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