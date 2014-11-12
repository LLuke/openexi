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

public class DocumentGrammarTest extends TestCase {
  
  public DocumentGrammarTest(String name) {
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
  public void testString_01() throws Exception {
  
    EJSONEncoder encoder = new EJSONEncoder();
    EJSONDecoder decoder = new EJSONDecoder();
    
    for (AlignmentType alignment : Alignments) {
      InputStream inputStream = getClass().getResource("/string_01.json").openStream();
      
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
      Assert.assertEquals(EventType.ITEM_STRING_VALUE_ANONYMOUS, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_STRING_VALUE, event.getEventKind());
      Assert.assertNull(event.getName());
      Assert.assertEquals("wenhua", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_DOCUMENT, eventType.itemType);
    }
  }
  
  /**
   */
  public void testInteger_01() throws Exception {
  
    EJSONEncoder encoder = new EJSONEncoder();
    EJSONDecoder decoder = new EJSONDecoder();
    
    for (AlignmentType alignment : Alignments) {
      InputStream inputStream = getClass().getResource("/integer_01.json").openStream();

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
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_ANONYMOUS, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertNull(event.getName());
      Assert.assertEquals("12345", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_DOCUMENT, eventType.itemType);
    }
  }

  /**
   */
  public void testDecimal_01() throws Exception {
  
    EJSONEncoder encoder = new EJSONEncoder();
    EJSONDecoder decoder = new EJSONDecoder();
    
    for (AlignmentType alignment : Alignments) {
      InputStream inputStream = getClass().getResource("/decimal_01.json").openStream();
      
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
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_ANONYMOUS, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertNull(event.getName());
      Assert.assertEquals("12345.678", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_DOCUMENT, eventType.itemType);
    }
  }

  /**
   */
  public void testFloat_01() throws Exception {
  
    EJSONEncoder encoder = new EJSONEncoder();
    EJSONDecoder decoder = new EJSONDecoder();
    
    for (AlignmentType alignment : Alignments) {
      InputStream inputStream = getClass().getResource("/float_01.json").openStream();

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
      Assert.assertEquals(EventType.ITEM_NUMBER_VALUE_ANONYMOUS, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NUMBER_VALUE, event.getEventKind());
      Assert.assertNull(event.getName());
      Assert.assertEquals("-1E4", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_DOCUMENT, eventType.itemType);
    }
  }

  /**
   */
  public void testNull_01() throws Exception {
  
    EJSONEncoder encoder = new EJSONEncoder();
    EJSONDecoder decoder = new EJSONDecoder();
    
    for (AlignmentType alignment : Alignments) {
      InputStream inputStream = getClass().getResource("/null_01.json").openStream();

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
      Assert.assertEquals(EventType.ITEM_NULL_ANONYMOUS, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_NULL, event.getEventKind());
      Assert.assertNull(event.getName());
      Assert.assertEquals("null", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_DOCUMENT, eventType.itemType);
    }
  }

  /**
   */
  public void testTrue_01() throws Exception {
  
    EJSONEncoder encoder = new EJSONEncoder();
    EJSONDecoder decoder = new EJSONDecoder();
    
    for (AlignmentType alignment : Alignments) {
      InputStream inputStream = getClass().getResource("/true_01.json").openStream();

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
      Assert.assertEquals(EventType.ITEM_BOOLEAN_VALUE_ANONYMOUS, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_BOOLEAN_VALUE, event.getEventKind());
      Assert.assertNull(event.getName());
      Assert.assertEquals("true", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_DOCUMENT, eventType.itemType);
    }
  }
  
  /**
   */
  public void testFalse_01() throws Exception {
  
    EJSONEncoder encoder = new EJSONEncoder();
    EJSONDecoder decoder = new EJSONDecoder();
    
    for (AlignmentType alignment : Alignments) {
      InputStream inputStream = getClass().getResource("/false_01.json").openStream();

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
      Assert.assertEquals(EventType.ITEM_BOOLEAN_VALUE_ANONYMOUS, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(EventDescription.EVENT_BOOLEAN_VALUE, event.getEventKind());
      Assert.assertNull(event.getName());
      Assert.assertEquals("false", event.getCharacters().makeString());
  
      event = scanner.nextEvent();
      eventType = event.getEventType();
      Assert.assertEquals(EventType.ITEM_END_DOCUMENT, eventType.itemType);
    }
  }
  
}