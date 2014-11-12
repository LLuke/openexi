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

public class BuiltinArrayGrammarTest extends TestCase {
  
  public BuiltinArrayGrammarTest(String name) {
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
  public void testRootArrayGrammar_01() throws Exception {
  
    EJSONEncoder encoder = new EJSONEncoder();
    EJSONDecoder decoder = new EJSONDecoder();
    
    for (AlignmentType alignment : Alignments) {
      InputStream inputStream = getClass().getResource("/rootArrayGrammar_01.json").openStream();

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
      Assert.assertEquals(EventType.ITEM_START_ARRAY_ANONYMOUS, eventType.itemType);
      Assert.assertEquals(EventDescription.EVENT_START_ARRAY, event.getEventKind());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_ANONYMOUS, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(3, eventType.getDepth());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertNull(event.getName());
      Assert.assertEquals("wenhua", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_ANONYMOUS, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(1, eventType.getDepth());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertNull(event.getName());
      Assert.assertEquals("pianyi", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_ANONYMOUS, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(1, eventType.getDepth());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertNull(event.getName());
      Assert.assertEquals("wenhua", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_ANONYMOUS, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(1, eventType.getDepth());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertNull(event.getName());
      Assert.assertEquals("xyz", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_ANONYMOUS, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(1, eventType.getDepth());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertNull(event.getName());
      Assert.assertEquals("yihun", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_ARRAY, eventType.itemType);
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_DOCUMENT, eventType.itemType);
    }
  }

  /**
   */
  public void testRootArrayGrammar_02() throws Exception {
  
    EJSONEncoder encoder = new EJSONEncoder();
    EJSONDecoder decoder = new EJSONDecoder();
    
    for (AlignmentType alignment : Alignments) {
      InputStream inputStream = getClass().getResource("/rootArrayGrammar_02.json").openStream();
      
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
      Assert.assertEquals(EventType.ITEM_START_ARRAY_ANONYMOUS, eventType.itemType);
      Assert.assertEquals(EventDescription.EVENT_START_ARRAY, event.getEventKind());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_ARRAY_ANONYMOUS, eventType.itemType);
      Assert.assertEquals(EventDescription.EVENT_START_ARRAY, event.getEventKind());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_ANONYMOUS, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertNull(event.getName());
      Assert.assertEquals("wenhua", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_ANONYMOUS, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertNull(event.getName());
      Assert.assertEquals("pianyi", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_ARRAY, eventType.itemType);
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_ARRAY_ANONYMOUS, eventType.itemType);
      Assert.assertEquals(EventDescription.EVENT_START_ARRAY, event.getEventKind());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_ANONYMOUS, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertNull(event.getName());
      Assert.assertEquals("wenhua", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_ANONYMOUS, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertNull(event.getName());
      Assert.assertEquals("xyz", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_ARRAY, eventType.itemType);
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_ARRAY_ANONYMOUS, eventType.itemType);
      Assert.assertEquals(EventDescription.EVENT_START_ARRAY, event.getEventKind());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_ANONYMOUS, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertNull(event.getName());
      Assert.assertEquals("yihun", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_ARRAY, eventType.itemType);
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_ARRAY, eventType.itemType);
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_DOCUMENT, eventType.itemType);
    }
  }

  /**
   */
  public void testRootArrayGrammar_03() throws Exception {
  
    EJSONEncoder encoder = new EJSONEncoder();
    EJSONDecoder decoder = new EJSONDecoder();
    
    for (AlignmentType alignment : Alignments) {
      InputStream inputStream = getClass().getResource("/rootArrayGrammar_03.json").openStream();
      
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
      Assert.assertEquals(EventType.ITEM_START_ARRAY_ANONYMOUS, eventType.itemType);
      Assert.assertEquals(2, eventType.getDepth());
      Assert.assertEquals(EventDescription.EVENT_START_ARRAY, event.getEventKind());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_ANONYMOUS, eventType.itemType);
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertNull(event.getName());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("a", event.getName());
      Assert.assertEquals("xyz", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("d", event.getName());
      Assert.assertEquals("wenhua", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("b", event.getName());
      Assert.assertEquals("pianyi", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_ANONYMOUS, eventType.itemType);
      Assert.assertEquals(1, eventType.getDepth());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertNull(event.getName());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_NAMED, eventType.itemType);
      Assert.assertEquals("d", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("d", event.getName());
      Assert.assertEquals("pianyi", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_WILDCARD, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("e", event.getName());
      Assert.assertEquals("yihun", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_NAMED, eventType.itemType);
      Assert.assertEquals("b", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("b", event.getName());
      Assert.assertEquals("xyz", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_START_OBJECT_ANONYMOUS, eventType.itemType);
      Assert.assertEquals(1, eventType.getDepth());
      Assert.assertEquals(EventDescription.EVENT_START_OBJECT, event.getEventKind());
      Assert.assertNull(event.getName());
      
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_NAMED, eventType.itemType);
      Assert.assertEquals("a", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("a", event.getName());
      Assert.assertEquals("wenhua", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_NAMED, eventType.itemType);
      Assert.assertEquals("b", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("b", event.getName());
      Assert.assertEquals("pianyi", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_NAMED, eventType.itemType);
      Assert.assertEquals("a", eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertEquals("a", event.getName());
      Assert.assertEquals("xyz", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_OBJECT, eventType.itemType);
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_ARRAY, eventType.itemType);
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_DOCUMENT, eventType.itemType);
    }
  }
  
}