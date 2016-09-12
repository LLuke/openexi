package org.openexi.json;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.openexi.proc.EXIDecoder;
import org.openexi.proc.common.EventDescription;
import org.openexi.proc.common.EventType;
import org.openexi.proc.io.Scanner;
import org.openexi.schema.EXISchema;
import org.openexi.schema.EXISchemaConst;

import junit.framework.Assert;
import junit.framework.TestCase;

public class ArrayTest extends TestCase {

  public ArrayTest(String name) {
    super(name);
  }

  private byte getAncestryId(int contentDatatype) {
    EXISchema schema = EXI4JsonSchema.getEXISchema();
    return schema.ancestryIds[schema.getSerialOfType(contentDatatype)];
  }
  
  ///////////////////////////////////////////////////////////////////////////
  // Test cases
  ///////////////////////////////////////////////////////////////////////////

  /**
   * [
   *   "wenhua",
   *   "pianyi",
   *   "xyz"
   * ]
   * 
   * <array xmlns="http://www.w3.org/2015/EXI/json">
   *   <string>wenhua</string>
   *   <string>pianyi</string>
   *   <string>xyz</string>
   * </array>
   */
  public void testString_01() throws Exception {
  
    InputStream inputJsonStream = getClass().getResource("/json/array_stringValue_01.json").openStream();
    String[] stringValues = { "wenhua", "pianyi", "xyz" };

    Transmogrifier encoder = new Transmogrifier();

    ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
    encoder.setOutputStream(baos);
    encoder.encode(inputJsonStream);
    inputJsonStream.close();

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
    Assert.assertEquals("array", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("array", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

    for (int i = 0; i < 3; i++) {
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
      Assert.assertEquals(stringValues[i], exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      Assert.assertEquals(EXISchemaConst.STRING_TYPE, getAncestryId(scanner.getGrammarState().contentDatatype)); 

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(1, eventType.getDepth());
    }

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
   * [
   *   {
   *     "a": "xyz"
   *   }
   * ]
   * 
   * <array xmlns="http://www.w3.org/2015/EXI/json">
   *   <map>
   *     <a>
   *       <string>xyz</string>
   *     </a>
   *   </map>
   * </array>
   */
  public void testMap_01() throws Exception {
  
    InputStream inputJsonStream = getClass().getResource("/json/array_mapValue_01.json").openStream();

    Transmogrifier encoder = new Transmogrifier();

    ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
    encoder.setOutputStream(baos);
    encoder.encode(inputJsonStream);
    inputJsonStream.close();

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
    Assert.assertEquals("array", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("array", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("map", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("map", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("a", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("string", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);

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
   * [
   *   123
   * ]
   * 
   * <array xmlns="http://www.w3.org/2015/EXI/json">
   *   <other>
   *     <integer>123</integer>
   *   </other>
   * </array>
   */
  public void testInteger_01() throws Exception {
  
    InputStream inputJsonStream = getClass().getResource("/json/array_integerValue_01.json").openStream();

    Transmogrifier encoder = new Transmogrifier();

    ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
    encoder.setOutputStream(baos);
    encoder.encode(inputJsonStream);
    inputJsonStream.close();

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
    Assert.assertEquals("array", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("array", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

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
    Assert.assertEquals("123", exiEvent.getCharacters().makeString());
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
   * [
   *   123.45
   * ]
   * 
   * <array xmlns="http://www.w3.org/2015/EXI/json">
   *   <number>123.45</number>
   * </array>
   */
  public void testNumber_01() throws Exception {
  
    InputStream inputJsonStream = getClass().getResource("/json/array_numberValue_01.json").openStream();

    Transmogrifier encoder = new Transmogrifier();

    ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
    encoder.setOutputStream(baos);
    encoder.encode(inputJsonStream);
    inputJsonStream.close();

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
    Assert.assertEquals("array", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("array", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

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
    Assert.assertEquals("12345E-2", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    Assert.assertEquals(EXISchemaConst.FLOAT_TYPE, getAncestryId(scanner.getGrammarState().contentDatatype)); 

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
   * [
   *   true,
   *   false
   * ]
   * 
   * <array xmlns="http://www.w3.org/2015/EXI/json">
   *   <boolean>true</boolean>
   *   <boolean>false</boolean>
   * </array>
   */
  public void testBoolean_01() throws Exception {
  
    InputStream inputJsonStream = getClass().getResource("/json/array_booleanValue_01.json").openStream();

    Transmogrifier encoder = new Transmogrifier();

    ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
    encoder.setOutputStream(baos);
    encoder.encode(inputJsonStream);
    inputJsonStream.close();

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
    Assert.assertEquals("array", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("array", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

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
   * [
   *   null
   * ]
   * 
   * <array xmlns="http://www.w3.org/2015/EXI/json">
   *   <null/>
   * </array>
   */
  public void testNull_01() throws Exception {
  
    InputStream inputJsonStream = getClass().getResource("/json/array_nullValue_01.json").openStream();

    Transmogrifier encoder = new Transmogrifier();

    ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
    encoder.setOutputStream(baos);
    encoder.encode(inputJsonStream);
    inputJsonStream.close();

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
    Assert.assertEquals("array", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("array", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

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

