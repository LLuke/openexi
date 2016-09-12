package org.openexi.json;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.openexi.proc.EXIDecoder;
import org.openexi.proc.common.EventDescription;
import org.openexi.proc.common.EventType;
import org.openexi.proc.io.Scanner;
import org.openexi.schema.EXISchema;
import org.openexi.schema.EXISchemaConst;

import junit.framework.Assert;
import junit.framework.TestCase;

public class SingleValueTest extends TestCase {

  public SingleValueTest(String name) {
    super(name);
  }

  private static byte getAncestryId(int contentDatatype) {
    EXISchema schema = EXI4JsonSchema.getEXISchema();
    return schema.ancestryIds[schema.getSerialOfType(contentDatatype)];
  }
  
  ///////////////////////////////////////////////////////////////////////////
  // Test cases
  ///////////////////////////////////////////////////////////////////////////

  /**
   * "xyz"
   *  
   * <string xmlns="http://www.w3.org/2015/EXI/json">xyz</string>
   */
  public void testString_01() throws Exception {
  
    Transmogrifier encoder = new Transmogrifier();

    ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
    encoder.setOutputStream(baos);
    encoder.encode("\"xyz\"\n");

    final byte[] exi4json = baos.toByteArray();
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(EXI4JsonSchema.getGrammarCache());
    
    decoder.setInputStream(new ByteArrayInputStream(exi4json));
    Scanner scanner = decoder.processHeader();
    
    EventDescription exiEvent;
    EventType eventType;
    
    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SD, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("string", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("string", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("xyz", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, getAncestryId(scanner.getGrammarState().contentDatatype)); 

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());
  }

  /**
   * 20
   *  
   * <other xmlns="http://www.w3.org/2015/EXI/json">
   *   <integer>20</integer>
   * </other>
   */
  public void testInteger_01() throws Exception {
  
    Transmogrifier encoder = new Transmogrifier();

    ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
    encoder.setOutputStream(baos);
    encoder.encode("20\n");

    final byte[] exi4json = baos.toByteArray();
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(EXI4JsonSchema.getGrammarCache());
    
    decoder.setInputStream(new ByteArrayInputStream(exi4json));
    Scanner scanner = decoder.processHeader();
    
    EventDescription exiEvent;
    EventType eventType;
    
    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SD, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("other", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("other", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("integer", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("integer", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("20", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    Assert.assertEquals(EXISchemaConst.INTEGER_TYPE, getAncestryId(scanner.getGrammarState().contentDatatype)); 
  
    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());
  }

  /**
   * 175.4
   *  
   * <number>20</number>
   */
  public void testFloat_01() throws Exception {
  
    Transmogrifier encoder = new Transmogrifier();

    ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
    encoder.setOutputStream(baos);
    encoder.encode("175.4\n");

    final byte[] exi4json = baos.toByteArray();
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(EXI4JsonSchema.getGrammarCache());
    
    decoder.setInputStream(new ByteArrayInputStream(exi4json));
    Scanner scanner = decoder.processHeader();
    
    EventDescription exiEvent;
    EventType eventType;
    
    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SD, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("number", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("number", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("1754E-1", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    Assert.assertEquals(EXISchemaConst.FLOAT_TYPE, getAncestryId(scanner.getGrammarState().contentDatatype)); 
  
    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());
  }

  /**
   * true
   *  
   * <boolean>true</boolean>
   */
  public void testTrue_01() throws Exception {
  
    Transmogrifier encoder = new Transmogrifier();

    ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
    encoder.setOutputStream(baos);
    encoder.encode("true\n");

    final byte[] exi4json = baos.toByteArray();
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(EXI4JsonSchema.getGrammarCache());
    
    decoder.setInputStream(new ByteArrayInputStream(exi4json));
    Scanner scanner = decoder.processHeader();
    
    EventDescription exiEvent;
    EventType eventType;
    
    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SD, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("boolean", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("boolean", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("true", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    Assert.assertEquals(EXISchemaConst.BOOLEAN_TYPE, getAncestryId(scanner.getGrammarState().contentDatatype)); 
  
    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());
  }

  /**
   * false
   *  
   * <boolean>false</boolean>
   */
  public void testFalse_01() throws Exception {
  
    Transmogrifier encoder = new Transmogrifier();

    ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
    encoder.setOutputStream(baos);
    encoder.encode("false\n");

    final byte[] exi4json = baos.toByteArray();
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(EXI4JsonSchema.getGrammarCache());
    
    decoder.setInputStream(new ByteArrayInputStream(exi4json));
    Scanner scanner = decoder.processHeader();
    
    EventDescription exiEvent;
    EventType eventType;
    
    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SD, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("boolean", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("boolean", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("false", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    Assert.assertEquals(EXISchemaConst.BOOLEAN_TYPE, getAncestryId(scanner.getGrammarState().contentDatatype)); 
  
    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());
  }
  
  /**
   * null
   *  
   * <null/>
   */
  public void testNull_01() throws Exception {
  
    Transmogrifier encoder = new Transmogrifier();

    ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
    encoder.setOutputStream(baos);
    encoder.encode("null\n");

    final byte[] exi4json = baos.toByteArray();
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(EXI4JsonSchema.getGrammarCache());
    
    decoder.setInputStream(new ByteArrayInputStream(exi4json));
    Scanner scanner = decoder.processHeader();
    
    EventDescription exiEvent;
    EventType eventType;
    
    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SD, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("null", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("null", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());
  }
  
}

