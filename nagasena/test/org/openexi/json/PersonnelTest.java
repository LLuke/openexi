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

public class PersonnelTest extends TestCase {

  public PersonnelTest(String name) {
    super(name);
  }

  private byte getAncestryId(EXISchema schema, int contentDatatype) {
    return schema.ancestryIds[schema.getSerialOfType(contentDatatype)];
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
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, getAncestryId(JsonSchema.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

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
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, getAncestryId(JsonSchema.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

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
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, getAncestryId(JsonSchema.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

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
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, getAncestryId(JsonSchema.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

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
    Assert.assertEquals(EXISchemaConst.INTEGER_TYPE, getAncestryId(JsonSchema.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

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
    Assert.assertEquals(EXISchemaConst.FLOAT_TYPE, getAncestryId(JsonSchema.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

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
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, getAncestryId(JsonSchema.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

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
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, getAncestryId(JsonSchema.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

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
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, getAncestryId(JsonSchema.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

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
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, getAncestryId(JsonSchema.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

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
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, getAncestryId(JsonSchema.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

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
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, getAncestryId(JsonSchema.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

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
    Assert.assertEquals(EXISchemaConst.INTEGER_TYPE, getAncestryId(JsonSchema.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

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
    Assert.assertEquals(EXISchemaConst.FLOAT_TYPE, getAncestryId(JsonSchema.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

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
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, getAncestryId(JsonSchema.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

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
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, getAncestryId(JsonSchema.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

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
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, getAncestryId(JsonSchema.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

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
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, getAncestryId(JsonSchema.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

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
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, getAncestryId(JsonSchema.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

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
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, getAncestryId(JsonSchema.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

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
    Assert.assertEquals(EXISchemaConst.INTEGER_TYPE, getAncestryId(JsonSchema.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

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
    Assert.assertEquals(EXISchemaConst.FLOAT_TYPE, getAncestryId(JsonSchema.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

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
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, getAncestryId(JsonSchema.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

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
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, getAncestryId(JsonSchema.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

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

  /**
   */
  public void testPersonnelExample_02() throws Exception {
  
    InputStream inputJsonStream = getClass().getResource("/json/personnel.json").openStream();

    Transmogrifier2 encoder = new Transmogrifier2();

    ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
    encoder.setOutputStream(baos);
    encoder.encode(inputJsonStream);
    inputJsonStream.close();

    final byte[] exi4json = baos.toByteArray();
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(JsonSchema2.getGrammarCache());
    
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
    Assert.assertEquals("personnel", exiEvent.getName());
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
    Assert.assertEquals("map", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("map", eventType.name);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("person", exiEvent.getName());
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
    Assert.assertEquals("id", exiEvent.getName());
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
    Assert.assertEquals("Boss", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, getAncestryId(JsonSchema2.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

    exiEvent = scanner.nextEvent(); // </string>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent(); // </id>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("name", exiEvent.getName());
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
    Assert.assertEquals("family", exiEvent.getName());
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
    Assert.assertEquals("Smith", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, getAncestryId(JsonSchema2.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

    exiEvent = scanner.nextEvent(); // </string>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent(); // </family>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("given", exiEvent.getName());
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
    Assert.assertEquals("Bill", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, getAncestryId(JsonSchema2.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

    exiEvent = scanner.nextEvent(); // </string>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());
    
    exiEvent = scanner.nextEvent(); // </given>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());
    
    exiEvent = scanner.nextEvent(); // </map>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent(); // </name>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("email", exiEvent.getName());
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
    Assert.assertEquals("smith@foo.com", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, getAncestryId(JsonSchema2.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

    exiEvent = scanner.nextEvent(); // </string>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());
    
    exiEvent = scanner.nextEvent(); // </email>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());
    
    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("YearsOfService", exiEvent.getName());
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
    Assert.assertEquals("20", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    Assert.assertEquals(EXISchemaConst.INTEGER_TYPE, getAncestryId(JsonSchema2.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

    exiEvent = scanner.nextEvent(); // </integer>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent(); // </other>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent(); // </YearsOfService>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("weight", exiEvent.getName());
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
    Assert.assertEquals("1754E-1", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    Assert.assertEquals(EXISchemaConst.FLOAT_TYPE, getAncestryId(JsonSchema2.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

    exiEvent = scanner.nextEvent(); // </number>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent(); // </weight>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("birthday", exiEvent.getName());
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
    Assert.assertEquals("1955-03-24", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, getAncestryId(JsonSchema2.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

    exiEvent = scanner.nextEvent(); // </string>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent(); // </birthday>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("link", exiEvent.getName());
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
    Assert.assertEquals("subordinates", exiEvent.getName());
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
    Assert.assertEquals("worker", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, getAncestryId(JsonSchema2.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

    exiEvent = scanner.nextEvent(); // </string>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());
    
    exiEvent = scanner.nextEvent(); // </subordinates>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent(); // </map>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent(); // </link>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent(); // </map>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent(); // </person>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent(); // </map>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());
    
    ///////////////////////////////////////////////////////////////////////////////

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
    Assert.assertEquals("person", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
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
    Assert.assertEquals("id", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);
    
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
    Assert.assertEquals("worker", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, getAncestryId(JsonSchema2.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

    exiEvent = scanner.nextEvent(); // </string>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent(); // </id>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("name", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
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
    Assert.assertEquals("family", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);
    
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
    Assert.assertEquals("Jones", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, getAncestryId(JsonSchema2.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

    exiEvent = scanner.nextEvent(); // </string>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent(); // </family>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("given", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);
    
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
    Assert.assertEquals("Bill", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, getAncestryId(JsonSchema2.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

    exiEvent = scanner.nextEvent(); // </string>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());
    
    exiEvent = scanner.nextEvent(); // </given>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());
    
    exiEvent = scanner.nextEvent(); // </map>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent(); // </name>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("email", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);
    
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
    Assert.assertEquals("jones@foo.com", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, getAncestryId(JsonSchema2.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

    exiEvent = scanner.nextEvent(); // </string>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());
    
    exiEvent = scanner.nextEvent(); // </email>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());
    
    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("YearsOfService", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
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
    Assert.assertEquals("15", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    Assert.assertEquals(EXISchemaConst.INTEGER_TYPE, getAncestryId(JsonSchema2.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

    exiEvent = scanner.nextEvent(); // </integer>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent(); // </other>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent(); // </YearsOfService>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("weight", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
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
    Assert.assertEquals("1754E-1", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    Assert.assertEquals(EXISchemaConst.FLOAT_TYPE, getAncestryId(JsonSchema2.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

    exiEvent = scanner.nextEvent(); // </number>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent(); // </weight>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("birthday", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);
    
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
    Assert.assertEquals("1968-07-16", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, getAncestryId(JsonSchema2.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

    exiEvent = scanner.nextEvent(); // </string>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent(); // </birthday>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("link", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
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
    Assert.assertEquals("manager", exiEvent.getName());
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
    Assert.assertEquals("Boss", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, getAncestryId(JsonSchema2.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

    exiEvent = scanner.nextEvent(); // </string>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());
    
    exiEvent = scanner.nextEvent(); // </subordinates>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent(); // </map>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent(); // </link>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent(); // </map>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent(); // </person>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent(); // </map>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());
    
    ///////////////////////////////////////////////////////////////////////////////
    
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
    Assert.assertEquals("person", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
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
    Assert.assertEquals("id", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);
    
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
    Assert.assertEquals("worker", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, getAncestryId(JsonSchema2.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

    exiEvent = scanner.nextEvent(); // </string>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent(); // </id>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("name", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
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
    Assert.assertEquals("family", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);
    
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
    Assert.assertEquals("Jones", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, getAncestryId(JsonSchema2.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

    exiEvent = scanner.nextEvent(); // </string>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent(); // </family>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("given", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);
    
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
    Assert.assertEquals("Sam", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, getAncestryId(JsonSchema2.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

    exiEvent = scanner.nextEvent(); // </string>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());
    
    exiEvent = scanner.nextEvent(); // </given>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());
    
    exiEvent = scanner.nextEvent(); // </map>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent(); // </name>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("email", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);
    
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
    Assert.assertEquals("sjones@foo.com", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, getAncestryId(JsonSchema2.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

    exiEvent = scanner.nextEvent(); // </string>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());
    
    exiEvent = scanner.nextEvent(); // </email>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());
    
    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("YearsOfService", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
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
    Assert.assertEquals("20", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    Assert.assertEquals(EXISchemaConst.INTEGER_TYPE, getAncestryId(JsonSchema2.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

    exiEvent = scanner.nextEvent(); // </integer>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent(); // </other>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent(); // </YearsOfService>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("weight", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
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
    Assert.assertEquals("1892E-1", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    Assert.assertEquals(EXISchemaConst.FLOAT_TYPE, getAncestryId(JsonSchema2.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

    exiEvent = scanner.nextEvent(); // </number>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent(); // </weight>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("birthday", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);
    
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
    Assert.assertEquals("1959-01-26", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, getAncestryId(JsonSchema2.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

    exiEvent = scanner.nextEvent(); // </string>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent(); // </birthday>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("link", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
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
    Assert.assertEquals("manager", exiEvent.getName());
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
    Assert.assertEquals("http://www.w3.org/2015/EXI/json", eventType.uri);
    
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
    Assert.assertEquals("Boss", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, getAncestryId(JsonSchema2.getEXISchema(), scanner.getGrammarState().contentDatatype)); 

    exiEvent = scanner.nextEvent(); // </string>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());
    
    exiEvent = scanner.nextEvent(); // </subordinates>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent(); // </map>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent(); // </link>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent(); // </map>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent(); // </person>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    exiEvent = scanner.nextEvent(); // </map>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());

    ///////////////////////////////////////////////////////////////////////////////

    exiEvent = scanner.nextEvent(); // </array>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());
    
    exiEvent = scanner.nextEvent(); // </personnel>
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getDepth());
    
    exiEvent = scanner.nextEvent(); // </map>
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

