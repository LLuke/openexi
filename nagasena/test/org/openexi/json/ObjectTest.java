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

public class ObjectTest extends TestCase {

  public ObjectTest(String name) {
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
   * {
   *   "a": "wenhua",
   *   "b": "pianyi",
   *   "c": "xyz"
   * }
   * 
   * <map xmlns="http://www.w3.org/2015/EXI/json">
   *   <a><string>wenhua</string></a>
   *   <b><string>pianyi</string></b>
   *   <c><string>xyz</string></c>
   * </map>
   */
  public void testString_01() throws Exception {
  
    InputStream inputJsonStream = getClass().getResource("/json/object_stringValue_01.json").openStream();
    String[] keyValues = { "a", "b", "c" };
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
    Assert.assertEquals("map", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("map", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

    for (int i = 0; i < 3; i++) {
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals(keyValues[i], exiEvent.getName());
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
      Assert.assertEquals(stringValues[i], exiEvent.getCharacters().makeString());
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
   * {
   *   "a": [ "xyz" ]
   * }
   * 
   * <map xmlns="http://www.w3.org/2015/EXI/json">
   *   <a>
   *     <array>
   *       <string>xyz</string>
   *     </array>
   *   </a>
   * </map>
   */
  public void testArray_01() throws Exception {
  
    InputStream inputJsonStream = getClass().getResource("/json/object_arrayValue_01.json").openStream();

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
    Assert.assertEquals("array", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);

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
   * {
   *   "a": {
   *     "b" : "xyz"
   *   }
   * }
   * 
   * <map xmlns="http://www.w3.org/2015/EXI/json">
   *   <a>
   *     <map>
   *       <b>
   *         <string>xyz</string>
   *       </b>
   *     </map>
   *   </a>
   * </map>
   */
  public void testMap_01() throws Exception {
  
    InputStream inputJsonStream = getClass().getResource("/json/object_mapValue_01.json").openStream();

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
    Assert.assertEquals("map", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("b", exiEvent.getName());
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
   * {
   *   "a": 123,
   * }
   * 
   * <map xmlns="http://www.w3.org/2015/EXI/json">
   *   <a>
   *     <other>
   *       <integer>123</integer>
   *     </other>
   *   </a>
   * </map>
   */
  public void testInteger_01() throws Exception {
  
    InputStream inputJsonStream = getClass().getResource("/json/object_integerValue_01.json").openStream();

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
    Assert.assertEquals("other", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);

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
   * {
   *   "a": 123.45,
   * }
   * 
   * <map xmlns="http://www.w3.org/2015/EXI/json">
   *   <a>
   *     <number>12345E-2</number>
   *   </a>
   * </map>
   */
  public void testNumber_01() throws Exception {
  
    InputStream inputJsonStream = getClass().getResource("/json/object_numberValue_01.json").openStream();

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
    Assert.assertEquals("number", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);

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
   * {
   *   "a": true,
   *   "b": false"
   * }
   * 
   * <map xmlns="http://www.w3.org/2015/EXI/json">
   *   <a>
   *     <boolean>true</boolean>
   *   </a>
   *   <b>
   *     <boolean>false</boolean>
   *   </b>
   * </map>
   */
  public void testBoolean_01() throws Exception {
  
    InputStream inputJsonStream = getClass().getResource("/json/object_booleanValue_01.json").openStream();

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
    Assert.assertEquals("boolean", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);

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
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("b", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("boolean", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);

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
   * {
   *   "a": null
   * }
   * 
   * <map xmlns="http://www.w3.org/2015/EXI/json">
   *   <a>
   *     <null/>
   *   </a>
   * </map>
   */
  public void testNull_01() throws Exception {
  
    InputStream inputJsonStream = getClass().getResource("/json/object_nullValue_01.json").openStream();

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
    Assert.assertEquals("null", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);

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
  
}

