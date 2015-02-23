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

public class DocumentGrammarTest extends TestCase {
  
  public DocumentGrammarTest(String name) {
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
  public void testString_01() throws Exception {
  
    Transmogrifier encoder = new Transmogrifier();
    ESONDecoder decoder = new ESONDecoder();
    
    for (AlignmentType alignment : Alignments) {
      String json = "  \"wenhua\"";
      
      ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
      encoder.setOutputStream(baos);

      encoder.setAlignmentType(alignment);
      encoder.encode(json);
      
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
  
  public void testStringJSONify_01() throws Exception {
    String json = "  \"wenhua\"";

    Transmogrifier encoder = new Transmogrifier();
    JSONifier decoder = new JSONifier();
    
    for (AlignmentType alignment : Alignments) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
      encoder.setOutputStream(baos);
      
      encoder.setAlignmentType(alignment);
      encoder.encode(json);
      
      byte[] eson = baos.toByteArray();
      
      baos = new ByteArrayOutputStream(); 
      
      decoder.decode(new ByteArrayInputStream(eson), baos);
      
      String decodedJSON = new String(baos.toByteArray(), "UTF-8");
      
      Assert.assertEquals("\"wenhua\"", decodedJSON);
    }
  }
  
  /**
   */
  public void testInteger_01() throws Exception {
  
    Transmogrifier encoder = new Transmogrifier();
    ESONDecoder decoder = new ESONDecoder();
    
    for (AlignmentType alignment : Alignments) {
      InputStream inputStream = getClass().getResource("/integer_01.json").openStream();

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
  
    Transmogrifier encoder = new Transmogrifier();
    ESONDecoder decoder = new ESONDecoder();
    
    for (AlignmentType alignment : Alignments) {
      InputStream inputStream = getClass().getResource("/decimal_01.json").openStream();
      
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
  
    Transmogrifier encoder = new Transmogrifier();
    ESONDecoder decoder = new ESONDecoder();
    
    for (AlignmentType alignment : Alignments) {
      InputStream inputStream = getClass().getResource("/float_01.json").openStream();

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
  
    Transmogrifier encoder = new Transmogrifier();
    ESONDecoder decoder = new ESONDecoder();
    
    for (AlignmentType alignment : Alignments) {
      InputStream inputStream = getClass().getResource("/null_01.json").openStream();

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

  public void testNullJSONify_01() throws Exception {
    String json = " null";

    Transmogrifier encoder = new Transmogrifier();
    JSONifier decoder = new JSONifier();
    
    for (AlignmentType alignment : Alignments) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
      encoder.setOutputStream(baos);
      
      encoder.setAlignmentType(alignment);
      encoder.encode(json);
      
      byte[] eson = baos.toByteArray();
      
      baos = new ByteArrayOutputStream(); 
      
      decoder.decode(new ByteArrayInputStream(eson), baos);
      
      String decodedJSON = new String(baos.toByteArray(), "UTF-8");
      
      Assert.assertEquals("null", decodedJSON);
    }
  }
  
  /**
   */
  public void testTrue_01() throws Exception {
  
    Transmogrifier encoder = new Transmogrifier();
    ESONDecoder decoder = new ESONDecoder();
    
    for (AlignmentType alignment : Alignments) {
      InputStream inputStream = getClass().getResource("/true_01.json").openStream();

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
  
    Transmogrifier encoder = new Transmogrifier();
    ESONDecoder decoder = new ESONDecoder();
    
    for (AlignmentType alignment : Alignments) {
      InputStream inputStream = getClass().getResource("/false_01.json").openStream();

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