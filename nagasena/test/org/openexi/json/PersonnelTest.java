package org.openexi.json;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.openexi.proc.EXIDecoder;
import org.openexi.proc.common.EventDescription;
import org.openexi.proc.common.EventType;
import org.openexi.proc.io.Scanner;

import junit.framework.Assert;
import junit.framework.TestCase;

public class PersonnelTest extends TestCase {

  public PersonnelTest(String name) {
    super(name);
  }

  ///////////////////////////////////////////////////////////////////////////
  // Test cases
  ///////////////////////////////////////////////////////////////////////////

  /**
   */
  public void testPersonnelExample_01() throws Exception {
  
    InputStream inputJsonStream = getClass().getResource("/json/personnel.json").openStream();

    Transmogrifier encoder = new Transmogrifier();

    ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
    encoder.setOutputStream(baos);
    encoder.encode(inputJsonStream);
    inputJsonStream.close();

    final byte[] exi4json = baos.toByteArray();
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(JsonSchema.getGrammarCache());
    
    System.out.println(exi4json.length + " bytes");
    
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
    Assert.assertEquals("array", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("array", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("key", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("personnel", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
    Assert.assertEquals("key", eventType.name);
    Assert.assertEquals("", eventType.uri);

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
    Assert.assertEquals("map", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("map", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("key", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("person", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
    Assert.assertEquals("key", eventType.name);
    Assert.assertEquals("", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("string", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("string", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("key", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("id", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
    Assert.assertEquals("key", eventType.name);
    Assert.assertEquals("", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("Boss", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("map", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("map", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("key", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("name", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
    Assert.assertEquals("key", eventType.name);
    Assert.assertEquals("", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("string", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("string", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("key", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("family", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
    Assert.assertEquals("key", eventType.name);
    Assert.assertEquals("", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("Smith", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("string", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("string", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("key", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("given", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
    Assert.assertEquals("key", eventType.name);
    Assert.assertEquals("", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("Bill", exiEvent.getCharacters().makeString());
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
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("string", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("string", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("key", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("email", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
    Assert.assertEquals("key", eventType.name);
    Assert.assertEquals("", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("smith@foo.com", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());
    
    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("other", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("other", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("key", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("YearsOfService", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
    Assert.assertEquals("key", eventType.name);
    Assert.assertEquals("", eventType.uri);

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
    Assert.assertEquals("number", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("number", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("key", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("weight", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
    Assert.assertEquals("key", eventType.name);
    Assert.assertEquals("", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("1754E-1", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("string", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("string", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("key", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("birthday", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
    Assert.assertEquals("key", eventType.name);
    Assert.assertEquals("", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("1955-03-24", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("map", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("map", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("key", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("link", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
    Assert.assertEquals("key", eventType.name);
    Assert.assertEquals("", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("string", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("string", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("key", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("subordinates", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
    Assert.assertEquals("key", eventType.name);
    Assert.assertEquals("", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("worker", exiEvent.getCharacters().makeString());
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
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("map", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("map", eventType.name);
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
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("key", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("person", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
    Assert.assertEquals("key", eventType.name);
    Assert.assertEquals("", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("string", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("string", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("key", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("id", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
    Assert.assertEquals("key", eventType.name);
    Assert.assertEquals("", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("worker", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("map", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("map", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("key", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("name", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
    Assert.assertEquals("key", eventType.name);
    Assert.assertEquals("", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("string", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("string", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("key", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("family", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
    Assert.assertEquals("key", eventType.name);
    Assert.assertEquals("", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("Jones", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("string", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("string", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("key", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("given", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
    Assert.assertEquals("key", eventType.name);
    Assert.assertEquals("", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("Bill", exiEvent.getCharacters().makeString());
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
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("string", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("string", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("key", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("email", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
    Assert.assertEquals("key", eventType.name);
    Assert.assertEquals("", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("jones@foo.com", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());
    
    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("other", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("other", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("key", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("YearsOfService", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
    Assert.assertEquals("key", eventType.name);
    Assert.assertEquals("", eventType.uri);

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
    Assert.assertEquals("15", exiEvent.getCharacters().makeString());
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
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("number", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("number", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("key", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("weight", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
    Assert.assertEquals("key", eventType.name);
    Assert.assertEquals("", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("1754E-1", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("string", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("string", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("key", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("birthday", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
    Assert.assertEquals("key", eventType.name);
    Assert.assertEquals("", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("1968-07-16", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("map", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("map", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("key", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("link", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
    Assert.assertEquals("key", eventType.name);
    Assert.assertEquals("", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("string", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("string", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("key", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("manager", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
    Assert.assertEquals("key", eventType.name);
    Assert.assertEquals("", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("Boss", exiEvent.getCharacters().makeString());
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
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("map", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("map", eventType.name);
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
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("key", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("person", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
    Assert.assertEquals("key", eventType.name);
    Assert.assertEquals("", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("string", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("string", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("key", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("id", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
    Assert.assertEquals("key", eventType.name);
    Assert.assertEquals("", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("worker", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("map", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("map", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("key", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("name", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
    Assert.assertEquals("key", eventType.name);
    Assert.assertEquals("", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("string", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("string", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("key", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("family", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
    Assert.assertEquals("key", eventType.name);
    Assert.assertEquals("", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("Jones", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("string", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("string", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("key", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("given", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
    Assert.assertEquals("key", eventType.name);
    Assert.assertEquals("", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("Sam", exiEvent.getCharacters().makeString());
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
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("string", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("string", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("key", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("email", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
    Assert.assertEquals("key", eventType.name);
    Assert.assertEquals("", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("sjones@foo.com", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());
    
    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("other", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("other", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("key", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("YearsOfService", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
    Assert.assertEquals("key", eventType.name);
    Assert.assertEquals("", eventType.uri);

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
    Assert.assertEquals("number", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("number", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("key", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("weight", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
    Assert.assertEquals("key", eventType.name);
    Assert.assertEquals("", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("1892E-1", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("string", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("string", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("key", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("birthday", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
    Assert.assertEquals("key", eventType.name);
    Assert.assertEquals("", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("1959-01-26", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("map", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("map", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("key", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("link", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
    Assert.assertEquals("key", eventType.name);
    Assert.assertEquals("", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("string", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("string", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("key", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("manager", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
    Assert.assertEquals("key", eventType.name);
    Assert.assertEquals("", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("Boss", exiEvent.getCharacters().makeString());
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

