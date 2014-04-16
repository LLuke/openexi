package org.openexi.scomp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;

import org.openexi.proc.EXIDecoder;
import org.openexi.proc.common.EventDescription;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventTypeList;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.proc.io.Scanner;
import org.openexi.sax.Transmogrifier;
import org.openexi.sax.TransmogrifierException;
import org.openexi.schema.EXISchema;
import org.openexi.schema.EXISchemaConst;
import org.xml.sax.InputSource;

import junit.framework.Assert;
import junit.framework.TestCase;

public class EXISchemaReaderTest extends TestCase {

  public EXISchemaReaderTest(String name) {
    super(name);
    m_stringBuilder = new StringBuilder();
    m_schemaReader = new EXISchemaReader();
    m_compilerErrors = new EXISchemaFactoryErrorMonitor();
  }
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }
  
  private EXISchemaFactoryErrorMonitor m_compilerErrors;

  private final StringBuilder m_stringBuilder;
  private final EXISchemaReader m_schemaReader;
  
  ///////////////////////////////////////////////////////////////////////////
  // Test cases
  ///////////////////////////////////////////////////////////////////////////

  /**
   * String datatype with whiteSpace being "preserve".
   */
  public void testStringElement_01() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/stringElement_01.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    String originalValue = "<foo:A xmlns:foo='urn:foo'>" + "abc" + "</foo:A>\n";
    
    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    transmogrifier.setOutputStream(outputStream);
    transmogrifier.encode(new InputSource(new StringReader(originalValue)));
    byte[] bts = outputStream.toByteArray();
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    decoder.setInputStream(new ByteArrayInputStream(bts));
    Scanner scanner = decoder.processHeader();

    EventDescription exiEvent;
    int n_events = 0;

    EventType eventType;
    EventTypeList eventTypeList;
    int contentDatatype;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertNull(eventTypeList.getEE());
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("abc", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    contentDatatype = scanner.currentState.contentDatatype;
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
    Assert.assertEquals(EXISchema.WHITESPACE_PRESERVE, schema.getWhitespaceFacetValueOfStringSimpleType(contentDatatype));
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertEquals(1, eventTypeList.getLength());
    ++n_events;
    
    Assert.assertEquals(5, n_events);
  }

  /**
   * String datatype with whiteSpace being "replace".
   */
  public void testStringElement_02() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/stringElement_02.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    String originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  abc  " + "</foo:A>\n";
    
    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    transmogrifier.setOutputStream(outputStream);
    transmogrifier.encode(new InputSource(new StringReader(originalValue)));
    byte[] bts = outputStream.toByteArray();
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    decoder.setInputStream(new ByteArrayInputStream(bts));
    Scanner scanner = decoder.processHeader();

    EventDescription exiEvent;
    int n_events = 0;

    EventType eventType;
    EventTypeList eventTypeList;
    int contentDatatype;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertNull(eventTypeList.getEE());
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("  abc  ", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    contentDatatype = scanner.currentState.contentDatatype;
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
    Assert.assertEquals(EXISchema.WHITESPACE_REPLACE, schema.getWhitespaceFacetValueOfStringSimpleType(contentDatatype));
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertEquals(1, eventTypeList.getLength());
    ++n_events;
    
    Assert.assertEquals(5, n_events);
  }

  /**
   * String datatype with whiteSpace being "collapse".
   */
  public void testStringElement_03() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/stringElement_03.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    String originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  abc  " + "</foo:A>\n";
    
    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    transmogrifier.setOutputStream(outputStream);
    transmogrifier.encode(new InputSource(new StringReader(originalValue)));
    byte[] bts = outputStream.toByteArray();
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    decoder.setInputStream(new ByteArrayInputStream(bts));
    Scanner scanner = decoder.processHeader();

    EventDescription exiEvent;
    int n_events = 0;

    EventType eventType;
    EventTypeList eventTypeList;
    int contentDatatype;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertNull(eventTypeList.getEE());
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("  abc  ", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    contentDatatype = scanner.currentState.contentDatatype;
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
    Assert.assertEquals(EXISchema.WHITESPACE_COLLAPSE, schema.getWhitespaceFacetValueOfStringSimpleType(contentDatatype));
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertEquals(1, eventTypeList.getLength());
    ++n_events;
    
    Assert.assertEquals(5, n_events);
  }
  
  /**
   * String datatype with enumerated values.
   */
  public void testEnumeratedStringElement_01() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/enumeratedStringElement_01.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    String originalValue = "<foo:A xmlns:foo='urn:foo'>" + "Nagoya" + "</foo:A>\n";
    
    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    transmogrifier.setOutputStream(outputStream);
    transmogrifier.encode(new InputSource(new StringReader(originalValue)));
    byte[] bts = outputStream.toByteArray();
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    decoder.setInputStream(new ByteArrayInputStream(bts));
    Scanner scanner = decoder.processHeader();

    EventDescription exiEvent;
    int n_events = 0;

    EventType eventType;
    EventTypeList eventTypeList;
    int contentDatatype;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertNull(eventTypeList.getEE());
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("Nagoya", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    contentDatatype = scanner.currentState.contentDatatype;
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
    Assert.assertEquals(EXISchema.WHITESPACE_PRESERVE, schema.getWhitespaceFacetValueOfStringSimpleType(contentDatatype));
    Assert.assertEquals(3, schema.getEnumerationFacetCountOfAtomicSimpleType(contentDatatype));
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertEquals(1, eventTypeList.getLength());
    ++n_events;
    
    Assert.assertEquals(5, n_events);
  }

  /**
   * String datatype with RCS.
   */
  public void testPatternedStringElement_01() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/patternedStringElement_01.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    String originalValue = "<foo:A xmlns:foo='urn:foo'>" + "Nagoya" + "</foo:A>\n";
    
    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    transmogrifier.setOutputStream(outputStream);
    transmogrifier.encode(new InputSource(new StringReader(originalValue)));
    byte[] bts = outputStream.toByteArray();
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    decoder.setInputStream(new ByteArrayInputStream(bts));
    Scanner scanner = decoder.processHeader();

    EventDescription exiEvent;
    int n_events = 0;

    EventType eventType;
    EventTypeList eventTypeList;
    int contentDatatype;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertNull(eventTypeList.getEE());
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("Nagoya", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    contentDatatype = scanner.currentState.contentDatatype;
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
    Assert.assertEquals(EXISchema.WHITESPACE_PRESERVE, schema.getWhitespaceFacetValueOfStringSimpleType(contentDatatype));
    Assert.assertEquals(53, schema.getRestrictedCharacterCountOfStringSimpleType(contentDatatype));
    int rcs = schema.getRestrictedCharacterOfSimpleType(contentDatatype);
    int[] types = schema.getTypes();
    Assert.assertEquals('A', types[rcs + 0]);
    Assert.assertEquals('M', types[rcs + 12]);
    Assert.assertEquals('N', types[rcs + 13]);
    Assert.assertEquals('Z', types[rcs + 25]);
    Assert.assertEquals('_', types[rcs + 26]);
    Assert.assertEquals('a', types[rcs + 27]);
    Assert.assertEquals('m', types[rcs + 39]);
    Assert.assertEquals('n', types[rcs + 40]);
    Assert.assertEquals('z', types[rcs + 52]);
    
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertEquals(1, eventTypeList.getLength());
    ++n_events;
    
    Assert.assertEquals(5, n_events);
  }

  /**
   * String datatype with both enumerated values and RCS.
   */
  public void testPatternedStringElement_02() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/patternedStringElement_02.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    String originalValue = "<foo:A xmlns:foo='urn:foo'>" + "Assange" + "</foo:A>\n";
    
    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    transmogrifier.setOutputStream(outputStream);
    transmogrifier.encode(new InputSource(new StringReader(originalValue)));
    byte[] bts = outputStream.toByteArray();
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    decoder.setInputStream(new ByteArrayInputStream(bts));
    Scanner scanner = decoder.processHeader();

    EventDescription exiEvent;
    int n_events = 0;

    EventType eventType;
    EventTypeList eventTypeList;
    int contentDatatype;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertNull(eventTypeList.getEE());
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("Assange", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    contentDatatype = scanner.currentState.contentDatatype;
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(schema.getBaseTypeOfSimpleType(contentDatatype))));
    Assert.assertEquals(EXISchema.WHITESPACE_PRESERVE, schema.getWhitespaceFacetValueOfStringSimpleType(contentDatatype));
    Assert.assertEquals(3, schema.getEnumerationFacetCountOfAtomicSimpleType(contentDatatype));
    Assert.assertEquals(52, schema.getRestrictedCharacterCountOfStringSimpleType(contentDatatype));
    int rcs = schema.getRestrictedCharacterOfSimpleType(contentDatatype);
    int[] types = schema.getTypes();
    Assert.assertEquals('A', types[rcs + 0]);
    Assert.assertEquals('M', types[rcs + 12]);
    Assert.assertEquals('N', types[rcs + 13]);
    Assert.assertEquals('Z', types[rcs + 25]);
    Assert.assertEquals('a', types[rcs + 26]);
    Assert.assertEquals('m', types[rcs + 38]);
    Assert.assertEquals('n', types[rcs + 39]);
    Assert.assertEquals('z', types[rcs + 51]);
    
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertEquals(1, eventTypeList.getLength());
    ++n_events;
    
    Assert.assertEquals(5, n_events);
    
    originalValue = "<foo:A xmlns:foo='urn:foo'>" + "Tokyo" + "</foo:A>\n";

    // Let's verify that enumeration is used, instead of RCS
    transmogrifier.setOutputStream(new ByteArrayOutputStream());
    try {
      transmogrifier.encode(new InputSource(new StringReader(originalValue)));
    }
    catch (TransmogrifierException te) {
      Assert.assertEquals(TransmogrifierException.UNEXPECTED_CHARS, te.getCode());
      Assert.assertTrue(te.getMessage().contains("Tokyo"));
     return; 
    }
    Assert.fail();
  }
  
  /**
   * Boolean datatype without <Patterned/>
   */
  public void testBooleanElement_01() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/booleanElement_01.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    final String[] xmlStrings;
    final String[] originalValues = {
      "  true  ", 
      "  false  ",
    };
    final String[] resultValues = {
      "true", 
      "false",
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:A>\n";
    };

    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    for (i = 0; i < xmlStrings.length; i++) {
      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.setGrammarCache(grammarCache);
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      transmogrifier.setOutputStream(outputStream);
      transmogrifier.encode(new InputSource(new StringReader(xmlStrings[i])));
      byte[] bts = outputStream.toByteArray();
      
      EXIDecoder decoder = new EXIDecoder();
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      Scanner scanner = decoder.processHeader();
  
      EventDescription exiEvent;
      int n_events = 0;
  
      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals(resultValues[i], exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.assertEquals(EXISchemaConst.BOOLEAN_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      Assert.assertFalse(schema.isPatternedBooleanSimpleType(contentDatatype));
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      ++n_events;
      
      Assert.assertEquals(5, n_events);
    }
  }

  /**
   * Boolean datatype without <Patterned/>
   */
  public void testBooleanElement_02() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/booleanElement_02.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    final String[] xmlStrings;
    final String[] originalValues = {
      "  true  ", 
      "  false  ",
    };
    final String[] resultValues = {
      "true", 
      "false",
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:A>\n";
    };

    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    for (i = 0; i < xmlStrings.length; i++) {
      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.setGrammarCache(grammarCache);
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      transmogrifier.setOutputStream(outputStream);
      transmogrifier.encode(new InputSource(new StringReader(xmlStrings[i])));
      byte[] bts = outputStream.toByteArray();
      
      EXIDecoder decoder = new EXIDecoder();
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      Scanner scanner = decoder.processHeader();
  
      EventDescription exiEvent;
      int n_events = 0;
  
      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals(resultValues[i], exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.assertEquals(EXISchemaConst.BOOLEAN_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      Assert.assertTrue(schema.isPatternedBooleanSimpleType(contentDatatype));
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      ++n_events;
      
      Assert.assertEquals(5, n_events);
    }
  }
  
  /**
   * Decimal datatype
   */
  public void testDecimalElement_01() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/decimalElement_01.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    String originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  12345.67890  " + "</foo:A>\n";
    
    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    transmogrifier.setOutputStream(outputStream);
    transmogrifier.encode(new InputSource(new StringReader(originalValue)));
    byte[] bts = outputStream.toByteArray();
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    decoder.setInputStream(new ByteArrayInputStream(bts));
    Scanner scanner = decoder.processHeader();

    EventDescription exiEvent;
    int n_events = 0;

    EventType eventType;
    EventTypeList eventTypeList;
    int contentDatatype;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertNull(eventTypeList.getEE());
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("12345.6789", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    contentDatatype = scanner.currentState.contentDatatype;
    Assert.assertEquals(EXISchemaConst.DECIMAL_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertEquals(1, eventTypeList.getLength());
    ++n_events;
    
    Assert.assertEquals(5, n_events);
  }
  
  /**
   * Decimal datatype with enumerated values.
   */
  public void testEnumeratedDecimalElement_01() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/enumeratedDecimalElement_01.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    final String[] xmlStrings;
    final String[] values = {
        " \t\r 100.1234567\n",
        "101.2345678",
        "102.3456789"
    };
    final String[] resultValues = {
        "100.1234567",
        "101.2345678",
        "102.3456789"
    };

    xmlStrings = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + values[i] + "</foo:A>\n";
    };

    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    
    for (int i = 0; i < xmlStrings.length; i++) {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      transmogrifier.setOutputStream(outputStream);
      transmogrifier.encode(new InputSource(new StringReader(xmlStrings[i])));
      byte[] bts = outputStream.toByteArray();
      
      decoder.setInputStream(new ByteArrayInputStream(bts));
      Scanner scanner = decoder.processHeader();
  
      EventDescription exiEvent;
      int n_events = 0;
  
      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals(resultValues[i], exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.assertEquals(EXISchemaConst.DECIMAL_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      Assert.assertEquals(3, schema.getEnumerationFacetCountOfAtomicSimpleType(contentDatatype));
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      ++n_events;
      
      Assert.assertEquals(5, n_events);
    }

    String xmlString = "<foo:A xmlns:foo='urn:foo'>" + "123.456789" + "</foo:A>\n";

    // Make sure a value that does not match the enumeration fail to encode.
    transmogrifier.setOutputStream(new ByteArrayOutputStream());
    try {
      transmogrifier.encode(new InputSource(new StringReader(xmlString)));
    }
    catch (TransmogrifierException te) {
      Assert.assertEquals(TransmogrifierException.UNEXPECTED_CHARS, te.getCode());
      Assert.assertTrue(te.getMessage().contains("123.456789"));
      return; 
    }
    Assert.fail();
  }
  
  /**
   * Float datatype
   */
  public void testFloatElement_01() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/floatElement_01.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    String originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  12.78e-2  " + "</foo:A>\n";
    
    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    transmogrifier.setOutputStream(outputStream);
    transmogrifier.encode(new InputSource(new StringReader(originalValue)));
    byte[] bts = outputStream.toByteArray();
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    decoder.setInputStream(new ByteArrayInputStream(bts));
    Scanner scanner = decoder.processHeader();

    EventDescription exiEvent;
    int n_events = 0;

    EventType eventType;
    EventTypeList eventTypeList;
    int contentDatatype;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertNull(eventTypeList.getEE());
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("1278E-4", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    contentDatatype = scanner.currentState.contentDatatype;
    Assert.assertEquals(EXISchemaConst.FLOAT_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertEquals(1, eventTypeList.getLength());
    ++n_events;
    
    Assert.assertEquals(5, n_events);
  }
  
  /**
   * Double datatype
   */
  public void testFloatElement_02() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/floatElement_02.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    String originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  1267.43233e12  " + "</foo:A>\n";
    
    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    transmogrifier.setOutputStream(outputStream);
    transmogrifier.encode(new InputSource(new StringReader(originalValue)));
    byte[] bts = outputStream.toByteArray();
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    decoder.setInputStream(new ByteArrayInputStream(bts));
    Scanner scanner = decoder.processHeader();

    EventDescription exiEvent;
    int n_events = 0;

    EventType eventType;
    EventTypeList eventTypeList;
    int contentDatatype;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertNull(eventTypeList.getEE());
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("126743233E7", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    contentDatatype = scanner.currentState.contentDatatype;
    Assert.assertEquals(EXISchemaConst.DOUBLE_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertEquals(1, eventTypeList.getLength());
    ++n_events;
    
    Assert.assertEquals(5, n_events);
  }
  
  /**
   * Float datatype with enumerated values.
   */
  public void testEnumeratedFloatElement_01() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/enumeratedFloatElement_01.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    final String[] xmlStrings;
    final String[] values = {
        " \t\r 103.01\n",
        "105.01",
        "107.01"
    };
    final String[] resultValues = {
        "10301E-2",
        "10501E-2",
        "10701E-2"
    };

    xmlStrings = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + values[i] + "</foo:A>\n";
    };

    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    
    for (int i = 0; i < xmlStrings.length; i++) {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      transmogrifier.setOutputStream(outputStream);
      transmogrifier.encode(new InputSource(new StringReader(xmlStrings[i])));
      byte[] bts = outputStream.toByteArray();
      
      decoder.setInputStream(new ByteArrayInputStream(bts));
      Scanner scanner = decoder.processHeader();
  
      EventDescription exiEvent;
      int n_events = 0;
  
      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals(resultValues[i], exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.assertEquals(EXISchemaConst.FLOAT_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      Assert.assertEquals(3, schema.getEnumerationFacetCountOfAtomicSimpleType(contentDatatype));
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      ++n_events;
      
      Assert.assertEquals(5, n_events);
    }

    String xmlString = "<foo:A xmlns:foo='urn:foo'>" + "123.456789" + "</foo:A>\n";

    // Make sure a value that does not match the enumeration fail to encode.
    transmogrifier.setOutputStream(new ByteArrayOutputStream());
    try {
      transmogrifier.encode(new InputSource(new StringReader(xmlString)));
    }
    catch (TransmogrifierException te) {
      Assert.assertEquals(TransmogrifierException.UNEXPECTED_CHARS, te.getCode());
      Assert.assertTrue(te.getMessage().contains("123.456789"));
      return; 
    }
    Assert.fail();
  }
  
  /**
   * Double datatype with enumerated values.
   */
  public void testEnumeratedFloatElement_02() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/enumeratedFloatElement_02.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    final String[] xmlStrings;
    final String[] values = {
        " \t\r -1E4\n",
        "1267.43233E12",
        "12.78e-2",
        "12",
        "1200",
        "0",
        "-0",
        "INF",
        "-INF",
        "NaN",
        "0E3",
    };
    final String[] resultValues = {
        "-1E4",
        "126743233E7",
        "1278E-4",
        "12",
        "12E2",
        "0",
        "0",
        "INF",
        "-INF",
        "NaN",
        "0",
    };
    
    xmlStrings = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + values[i] + "</foo:A>\n";
    };

    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    
    for (int i = 0; i < xmlStrings.length; i++) {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      transmogrifier.setOutputStream(outputStream);
      transmogrifier.encode(new InputSource(new StringReader(xmlStrings[i])));
      byte[] bts = outputStream.toByteArray();
      
      decoder.setInputStream(new ByteArrayInputStream(bts));
      Scanner scanner = decoder.processHeader();
  
      EventDescription exiEvent;
      int n_events = 0;
  
      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals(resultValues[i], exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.assertEquals(EXISchemaConst.DOUBLE_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      Assert.assertEquals(11, schema.getEnumerationFacetCountOfAtomicSimpleType(contentDatatype));
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      ++n_events;
      
      Assert.assertEquals(5, n_events);
    }

    String xmlString = "<foo:A xmlns:foo='urn:foo'>" + "123.456789" + "</foo:A>\n";

    // Make sure a value that does not match the enumeration fail to encode.
    transmogrifier.setOutputStream(new ByteArrayOutputStream());
    try {
      transmogrifier.encode(new InputSource(new StringReader(xmlString)));
    }
    catch (TransmogrifierException te) {
      Assert.assertEquals(TransmogrifierException.UNEXPECTED_CHARS, te.getCode());
      Assert.assertTrue(te.getMessage().contains("123.456789"));
      return; 
    }
    Assert.fail();
  }
  
  /**
   * Integer datatype that uses signed integer representation. 
   */
  public void testIntegerElement_01() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/integerElement_01.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    String originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  1234567890  " + "</foo:A>\n";
    
    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    transmogrifier.setOutputStream(outputStream);
    transmogrifier.encode(new InputSource(new StringReader(originalValue)));
    byte[] bts = outputStream.toByteArray();
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    decoder.setInputStream(new ByteArrayInputStream(bts));
    Scanner scanner = decoder.processHeader();

    EventDescription exiEvent;
    int n_events = 0;

    EventType eventType;
    EventTypeList eventTypeList;
    int contentDatatype;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertNull(eventTypeList.getEE());
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("1234567890", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    contentDatatype = scanner.currentState.contentDatatype;
    Assert.assertEquals(EXISchemaConst.INTEGER_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
    Assert.assertEquals(EXISchema.INTEGER_CODEC_DEFAULT, schema.getWidthOfIntegralSimpleType(contentDatatype));
    Assert.assertEquals(EXISchema.NIL_VALUE, schema.getMinInclusiveFacetOfIntegerSimpleType(contentDatatype)); 
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertEquals(1, eventTypeList.getLength());
    ++n_events;
    
    Assert.assertEquals(5, n_events);
  }
  
  /**
   * Integer datatype that uses unsigned integer representation. 
   */
  public void testIntegerElement_02a() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/integerElement_02a.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    String originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  1234567890  " + "</foo:A>\n";
    
    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    transmogrifier.setOutputStream(outputStream);
    transmogrifier.encode(new InputSource(new StringReader(originalValue)));
    byte[] bts = outputStream.toByteArray();
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    decoder.setInputStream(new ByteArrayInputStream(bts));
    Scanner scanner = decoder.processHeader();

    EventDescription exiEvent;
    int n_events = 0;

    EventType eventType;
    EventTypeList eventTypeList;
    int contentDatatype;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertNull(eventTypeList.getEE());
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("1234567890", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    contentDatatype = scanner.currentState.contentDatatype;
    Assert.assertEquals(EXISchemaConst.POSITIVE_INTEGER_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
    Assert.assertEquals(EXISchema.INTEGER_CODEC_NONNEGATIVE, schema.getWidthOfIntegralSimpleType(contentDatatype));
    Assert.assertEquals(EXISchema.NIL_VALUE, schema.getMinInclusiveFacetOfIntegerSimpleType(contentDatatype)); 
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertEquals(1, eventTypeList.getLength());
    ++n_events;
    
    Assert.assertEquals(5, n_events);
  }
  
  /**
   * Integer datatype that uses unsigned integer representation. 
   */
  public void testIntegerElement_02b() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/integerElement_02b.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    String originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  1234567890  " + "</foo:A>\n";
    
    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    transmogrifier.setOutputStream(outputStream);
    transmogrifier.encode(new InputSource(new StringReader(originalValue)));
    byte[] bts = outputStream.toByteArray();
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    decoder.setInputStream(new ByteArrayInputStream(bts));
    Scanner scanner = decoder.processHeader();

    EventDescription exiEvent;
    int n_events = 0;

    EventType eventType;
    EventTypeList eventTypeList;
    int contentDatatype;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertNull(eventTypeList.getEE());
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("1234567890", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    contentDatatype = scanner.currentState.contentDatatype;
    Assert.assertEquals(EXISchemaConst.INT_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
    Assert.assertEquals(EXISchema.INTEGER_CODEC_NONNEGATIVE, schema.getWidthOfIntegralSimpleType(contentDatatype));
    Assert.assertEquals(EXISchema.NIL_VALUE, schema.getMinInclusiveFacetOfIntegerSimpleType(contentDatatype)); 
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertEquals(1, eventTypeList.getLength());
    ++n_events;
    
    Assert.assertEquals(5, n_events);
  }
  
  /**
   * Integer datatype that uses 6-bits representation with an int minInclusive value. 
   */
  public void testIntegerElement_03() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/integerElement_03.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    String originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  78  " + "</foo:A>\n";
    
    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    transmogrifier.setOutputStream(outputStream);
    transmogrifier.encode(new InputSource(new StringReader(originalValue)));
    byte[] bts = outputStream.toByteArray();
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    decoder.setInputStream(new ByteArrayInputStream(bts));
    Scanner scanner = decoder.processHeader();

    EventDescription exiEvent;
    int n_events = 0;

    EventType eventType;
    EventTypeList eventTypeList;
    int contentDatatype;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertNull(eventTypeList.getEE());
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("78", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    contentDatatype = scanner.currentState.contentDatatype;
    Assert.assertEquals(EXISchemaConst.INTEGER_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
    Assert.assertEquals(6, schema.getWidthOfIntegralSimpleType(contentDatatype));
    int variant = schema.getMinInclusiveFacetOfIntegerSimpleType(contentDatatype);
    Assert.assertEquals(15, schema.getIntValueOfVariant(variant));
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertEquals(1, eventTypeList.getLength());
    ++n_events;
    
    Assert.assertEquals(5, n_events);
  }
  
  /**
   * Integer datatype that uses 6-bits representation with a long minInclusive value. 
   */
  public void testIntegerElement_04() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/integerElement_04.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    String originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  12678967543296  " + "</foo:A>\n";
    
    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    transmogrifier.setOutputStream(outputStream);
    transmogrifier.encode(new InputSource(new StringReader(originalValue)));
    byte[] bts = outputStream.toByteArray();
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    decoder.setInputStream(new ByteArrayInputStream(bts));
    Scanner scanner = decoder.processHeader();

    EventDescription exiEvent;
    int n_events = 0;

    EventType eventType;
    EventTypeList eventTypeList;
    int contentDatatype;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertNull(eventTypeList.getEE());
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("12678967543296", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    contentDatatype = scanner.currentState.contentDatatype;
    Assert.assertEquals(EXISchemaConst.INTEGER_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
    Assert.assertEquals(6, schema.getWidthOfIntegralSimpleType(contentDatatype));
    int variant = schema.getMinInclusiveFacetOfIntegerSimpleType(contentDatatype);
    Assert.assertEquals(12678967543233L, schema.getLongValueOfVariant(variant));
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertEquals(1, eventTypeList.getLength());
    ++n_events;
    
    Assert.assertEquals(5, n_events);
  }
  
  /**
   * Integer datatype with enumerated values.
   */
  public void testEnumeratedIntegerElement_01() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/enumeratedIntegerElement_01.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    final String[] xmlStrings;
    final String[] values = {
        " \t\r 115\n",
        "9223372036854775807",
        "-9223372036854775808",
        "98765432109876543210",
        "987654321098765432",
        "-987654321098765432"
    };
    final String[] resultValues = {
        "115", 
        "9223372036854775807",
        "-9223372036854775808",
        "98765432109876543210",
        "987654321098765432",
        "-987654321098765432"
    };

    xmlStrings = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + values[i] + "</foo:A>\n";
    };

    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    
    for (int i = 0; i < xmlStrings.length; i++) {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      transmogrifier.setOutputStream(outputStream);
      transmogrifier.encode(new InputSource(new StringReader(xmlStrings[i])));
      byte[] bts = outputStream.toByteArray();
      
      decoder.setInputStream(new ByteArrayInputStream(bts));
      Scanner scanner = decoder.processHeader();
  
      EventDescription exiEvent;
      int n_events = 0;
  
      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals(resultValues[i], exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.assertEquals(EXISchemaConst.INTEGER_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      Assert.assertEquals(6, schema.getEnumerationFacetCountOfAtomicSimpleType(contentDatatype));
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      ++n_events;
      
      Assert.assertEquals(5, n_events);
    }

    String xmlString = "<foo:A xmlns:foo='urn:foo'>" + "123" + "</foo:A>\n";

    // Make sure a value that does not match the enumeration fail to encode.
    transmogrifier.setOutputStream(new ByteArrayOutputStream());
    try {
      transmogrifier.encode(new InputSource(new StringReader(xmlString)));
    }
    catch (TransmogrifierException te) {
      Assert.assertEquals(TransmogrifierException.UNEXPECTED_CHARS, te.getCode());
      Assert.assertTrue(te.getMessage().contains("123"));
      return; 
    }
    Assert.fail();
  }

  /**
   * Duration datatype
   */
  public void testDurationElement_01() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/durationElement_01.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    String originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  P1Y2M3DT10H30M  " + "</foo:A>\n";
    
    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    transmogrifier.setOutputStream(outputStream);
    transmogrifier.encode(new InputSource(new StringReader(originalValue)));
    byte[] bts = outputStream.toByteArray();
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    decoder.setInputStream(new ByteArrayInputStream(bts));
    Scanner scanner = decoder.processHeader();

    EventDescription exiEvent;
    int n_events = 0;

    EventType eventType;
    EventTypeList eventTypeList;
    int contentDatatype;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertNull(eventTypeList.getEE());
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("  P1Y2M3DT10H30M  ", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    contentDatatype = scanner.currentState.contentDatatype;
    Assert.assertEquals(EXISchemaConst.DURATION_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertEquals(1, eventTypeList.getLength());
    ++n_events;
    
    Assert.assertEquals(5, n_events);
  }
  
  /**
   * Duration datatype with enumerated values.
   */
  public void testEnumeratedDurationElement_01() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/enumeratedDurationElement_01.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    final String[] xmlStrings;
    final String[] values = {
        " \t\r P1Y2M3DT10H30M\n",
        "P1Y2M4DT10H30M",
        "P1Y2M5DT10H30M"
    };
    final String[] resultValues = {
        "P1Y2M3DT10H30M",
        "P1Y2M4DT10H30M",
        "P1Y2M5DT10H30M"
    };
    
    xmlStrings = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + values[i] + "</foo:A>\n";
    };

    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    
    for (int i = 0; i < xmlStrings.length; i++) {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      transmogrifier.setOutputStream(outputStream);
      transmogrifier.encode(new InputSource(new StringReader(xmlStrings[i])));
      byte[] bts = outputStream.toByteArray();
      
      decoder.setInputStream(new ByteArrayInputStream(bts));
      Scanner scanner = decoder.processHeader();
  
      EventDescription exiEvent;
      int n_events = 0;
  
      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals(resultValues[i], exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.assertEquals(EXISchemaConst.DURATION_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      Assert.assertEquals(3, schema.getEnumerationFacetCountOfAtomicSimpleType(contentDatatype));
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      ++n_events;
      
      Assert.assertEquals(5, n_events);
    }

    String xmlString = "<foo:A xmlns:foo='urn:foo'>" + "P1Y2M6DT10H30M" + "</foo:A>\n";

    // Make sure a value that does not match the enumeration fail to encode.
    transmogrifier.setOutputStream(new ByteArrayOutputStream());
    try {
      transmogrifier.encode(new InputSource(new StringReader(xmlString)));
    }
    catch (TransmogrifierException te) {
      Assert.assertEquals(TransmogrifierException.UNEXPECTED_CHARS, te.getCode());
      Assert.assertTrue(te.getMessage().contains("P1Y2M6DT10H30M"));
      return; 
    }
    Assert.fail();
  }

  /**
   * DateTime datatype
   */
  public void testDateTimeElement_01() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/dateTimeElement_01.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    String originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  2003-03-19T13:20:00-05:00  " + "</foo:A>\n";
    
    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    transmogrifier.setOutputStream(outputStream);
    transmogrifier.encode(new InputSource(new StringReader(originalValue)));
    byte[] bts = outputStream.toByteArray();
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    decoder.setInputStream(new ByteArrayInputStream(bts));
    Scanner scanner = decoder.processHeader();

    EventDescription exiEvent;
    int n_events = 0;

    EventType eventType;
    EventTypeList eventTypeList;
    int contentDatatype;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertNull(eventTypeList.getEE());
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("2003-03-19T13:20:00-05:00", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    contentDatatype = scanner.currentState.contentDatatype;
    Assert.assertEquals(EXISchemaConst.DATETIME_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertEquals(1, eventTypeList.getLength());
    ++n_events;
    
    Assert.assertEquals(5, n_events);
  }
  
  /**
   * DateTime datatype with enumerated values.
   */
  public void testEnumeratedDateTimeElement_01() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/enumeratedDateTimeElement_01.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    final String[] xmlStrings;
    final String[] values = {
        " \t\r 2003-03-19T13:20:00-05:00\n",
        "2003-03-20T13:20:00-05:00",
        "2003-03-21T13:20:00-05:00",
        "2013-06-03T24:00:00-05:00",
        "2013-06-04T06:00:00Z",
        "2012-07-01T00:00:00Z",
    };
    final String[] resultValues = {
        "2003-03-19T13:20:00-05:00",
        "2003-03-20T13:20:00-05:00",
        "2003-03-21T13:20:00-05:00",
        "2013-06-03T24:00:00-05:00",
        "2013-06-04T06:00:00Z",
        "2012-07-01T00:00:00Z",
    };
    
    xmlStrings = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + values[i] + "</foo:A>\n";
    };

    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    
    for (int i = 0; i < xmlStrings.length; i++) {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      transmogrifier.setOutputStream(outputStream);
      transmogrifier.encode(new InputSource(new StringReader(xmlStrings[i])));
      byte[] bts = outputStream.toByteArray();
      
      decoder.setInputStream(new ByteArrayInputStream(bts));
      Scanner scanner = decoder.processHeader();
  
      EventDescription exiEvent;
      int n_events = 0;
  
      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals(resultValues[i], exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.assertEquals(EXISchemaConst.DATETIME_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      Assert.assertEquals(6, schema.getEnumerationFacetCountOfAtomicSimpleType(contentDatatype));
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      ++n_events;
      
      Assert.assertEquals(5, n_events);
    }

    String xmlString = "<foo:A xmlns:foo='urn:foo'>" + "2011-03-11T14:46:18+09:00" + "</foo:A>\n";

    // Make sure a value that does not match the enumeration fail to encode.
    transmogrifier.setOutputStream(new ByteArrayOutputStream());
    try {
      transmogrifier.encode(new InputSource(new StringReader(xmlString)));
    }
    catch (TransmogrifierException te) {
      Assert.assertEquals(TransmogrifierException.UNEXPECTED_CHARS, te.getCode());
      Assert.assertTrue(te.getMessage().contains("2011-03-11T14:46:18+09:00"));
      return; 
    }
    Assert.fail();
  }

  /**
   * Time datatype
   */
  public void testTimeElement_01() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/timeElement_01.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    String originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  13:20:00-05:00  " + "</foo:A>\n";
    
    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    transmogrifier.setOutputStream(outputStream);
    transmogrifier.encode(new InputSource(new StringReader(originalValue)));
    byte[] bts = outputStream.toByteArray();
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    decoder.setInputStream(new ByteArrayInputStream(bts));
    Scanner scanner = decoder.processHeader();

    EventDescription exiEvent;
    int n_events = 0;

    EventType eventType;
    EventTypeList eventTypeList;
    int contentDatatype;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertNull(eventTypeList.getEE());
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("13:20:00-05:00", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    contentDatatype = scanner.currentState.contentDatatype;
    Assert.assertEquals(EXISchemaConst.TIME_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertEquals(1, eventTypeList.getLength());
    ++n_events;
    
    Assert.assertEquals(5, n_events);
  }

  /**
   * Time datatype with enumerated values.
   */
  public void testEnumeratedTimeElement_01() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/enumeratedTimeElement_01.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    final String[] xmlStrings;
    final String[] values = {
        " \t\r 13:20:00-05:00\n",
        "13:22:00-05:00",
        "13:24:00-05:00",
    };
    final String[] resultValues = {
        "13:20:00-05:00",
        "13:22:00-05:00",
        "13:24:00-05:00",
    };
    
    xmlStrings = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + values[i] + "</foo:A>\n";
    };

    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    
    for (int i = 0; i < xmlStrings.length; i++) {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      transmogrifier.setOutputStream(outputStream);
      transmogrifier.encode(new InputSource(new StringReader(xmlStrings[i])));
      byte[] bts = outputStream.toByteArray();
      
      decoder.setInputStream(new ByteArrayInputStream(bts));
      Scanner scanner = decoder.processHeader();
  
      EventDescription exiEvent;
      int n_events = 0;
  
      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals(resultValues[i], exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.assertEquals(EXISchemaConst.TIME_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      Assert.assertEquals(3, schema.getEnumerationFacetCountOfAtomicSimpleType(contentDatatype));
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      ++n_events;
      
      Assert.assertEquals(5, n_events);
    }

    String xmlString = "<foo:A xmlns:foo='urn:foo'>" + "13:26:00-05:00" + "</foo:A>\n";

    // Make sure a value that does not match the enumeration fail to encode.
    transmogrifier.setOutputStream(new ByteArrayOutputStream());
    try {
      transmogrifier.encode(new InputSource(new StringReader(xmlString)));
    }
    catch (TransmogrifierException te) {
      Assert.assertEquals(TransmogrifierException.UNEXPECTED_CHARS, te.getCode());
      Assert.assertTrue(te.getMessage().contains("13:26:00-05:00"));
      return; 
    }
    Assert.fail();
  }
  
  /**
   * Date datatype
   */
  public void testDateElement_01() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/dateElement_01.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    String originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  2003-03-19-05:00  " + "</foo:A>\n";
    
    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    transmogrifier.setOutputStream(outputStream);
    transmogrifier.encode(new InputSource(new StringReader(originalValue)));
    byte[] bts = outputStream.toByteArray();
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    decoder.setInputStream(new ByteArrayInputStream(bts));
    Scanner scanner = decoder.processHeader();

    EventDescription exiEvent;
    int n_events = 0;

    EventType eventType;
    EventTypeList eventTypeList;
    int contentDatatype;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertNull(eventTypeList.getEE());
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("2003-03-19-05:00", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    contentDatatype = scanner.currentState.contentDatatype;
    Assert.assertEquals(EXISchemaConst.DATE_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertEquals(1, eventTypeList.getLength());
    ++n_events;
    
    Assert.assertEquals(5, n_events);
  }

  /**
   * Date datatype with enumerated values.
   */
  public void testEnumeratedDateElement_01() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/enumeratedDateElement_01.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    final String[] xmlStrings;
    final String[] values = {
        " \t\r 2003-03-19-05:00\n",
        "2003-03-21-05:00",
        "2003-03-23-05:00",
    };
    final String[] resultValues = {
        "2003-03-19-05:00",
        "2003-03-21-05:00",
        "2003-03-23-05:00",
    };
    
    xmlStrings = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + values[i] + "</foo:A>\n";
    };

    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    
    for (int i = 0; i < xmlStrings.length; i++) {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      transmogrifier.setOutputStream(outputStream);
      transmogrifier.encode(new InputSource(new StringReader(xmlStrings[i])));
      byte[] bts = outputStream.toByteArray();
      
      decoder.setInputStream(new ByteArrayInputStream(bts));
      Scanner scanner = decoder.processHeader();
  
      EventDescription exiEvent;
      int n_events = 0;
  
      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals(resultValues[i], exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.assertEquals(EXISchemaConst.DATE_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      Assert.assertEquals(3, schema.getEnumerationFacetCountOfAtomicSimpleType(contentDatatype));
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      ++n_events;
      
      Assert.assertEquals(5, n_events);
    }

    String xmlString = "<foo:A xmlns:foo='urn:foo'>" + "2003-03-25-05:00" + "</foo:A>\n";

    // Make sure a value that does not match the enumeration fail to encode.
    transmogrifier.setOutputStream(new ByteArrayOutputStream());
    try {
      transmogrifier.encode(new InputSource(new StringReader(xmlString)));
    }
    catch (TransmogrifierException te) {
      Assert.assertEquals(TransmogrifierException.UNEXPECTED_CHARS, te.getCode());
      Assert.assertTrue(te.getMessage().contains("2003-03-25-05:00"));
      return; 
    }
    Assert.fail();
  }
  
  /**
   * GYearMonth datatype
   */
  public void testGYearMonthElement_01() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/gYearMonthElement_01.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    String originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  2003-04-05:00  " + "</foo:A>\n";
    
    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    transmogrifier.setOutputStream(outputStream);
    transmogrifier.encode(new InputSource(new StringReader(originalValue)));
    byte[] bts = outputStream.toByteArray();
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    decoder.setInputStream(new ByteArrayInputStream(bts));
    Scanner scanner = decoder.processHeader();

    EventDescription exiEvent;
    int n_events = 0;

    EventType eventType;
    EventTypeList eventTypeList;
    int contentDatatype;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertNull(eventTypeList.getEE());
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("2003-04-05:00", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    contentDatatype = scanner.currentState.contentDatatype;
    Assert.assertEquals(EXISchemaConst.G_YEARMONTH_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertEquals(1, eventTypeList.getLength());
    ++n_events;
    
    Assert.assertEquals(5, n_events);
  }

  /**
   * GYearMonth datatype with enumerated values.
   */
  public void testEnumeratedGYearMonthElement_01() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/enumeratedGYearMonthElement_01.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    final String[] xmlStrings;
    final String[] values = {
        " \t\r 2003-04-05:00\n",
        "2003-06-05:00",
        "2003-08-05:00",
    };
    final String[] resultValues = {
        "2003-04-05:00",
        "2003-06-05:00",
        "2003-08-05:00",
    };
    
    xmlStrings = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + values[i] + "</foo:A>\n";
    };

    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    
    for (int i = 0; i < xmlStrings.length; i++) {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      transmogrifier.setOutputStream(outputStream);
      transmogrifier.encode(new InputSource(new StringReader(xmlStrings[i])));
      byte[] bts = outputStream.toByteArray();
      
      decoder.setInputStream(new ByteArrayInputStream(bts));
      Scanner scanner = decoder.processHeader();
  
      EventDescription exiEvent;
      int n_events = 0;
  
      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals(resultValues[i], exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.assertEquals(EXISchemaConst.G_YEARMONTH_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      Assert.assertEquals(3, schema.getEnumerationFacetCountOfAtomicSimpleType(contentDatatype));
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      ++n_events;
      
      Assert.assertEquals(5, n_events);
    }

    String xmlString = "<foo:A xmlns:foo='urn:foo'>" + "2003-10-05:00" + "</foo:A>\n";

    // Make sure a value that does not match the enumeration fail to encode.
    transmogrifier.setOutputStream(new ByteArrayOutputStream());
    try {
      transmogrifier.encode(new InputSource(new StringReader(xmlString)));
    }
    catch (TransmogrifierException te) {
      Assert.assertEquals(TransmogrifierException.UNEXPECTED_CHARS, te.getCode());
      Assert.assertTrue(te.getMessage().contains("2003-10-05:00"));
      return; 
    }
    Assert.fail();
  }

  /**
   * GYear datatype
   */
  public void testGYearElement_01() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/gYearElement_01.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    String originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  1969+09:00  " + "</foo:A>\n";
    
    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    transmogrifier.setOutputStream(outputStream);
    transmogrifier.encode(new InputSource(new StringReader(originalValue)));
    byte[] bts = outputStream.toByteArray();
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    decoder.setInputStream(new ByteArrayInputStream(bts));
    Scanner scanner = decoder.processHeader();

    EventDescription exiEvent;
    int n_events = 0;

    EventType eventType;
    EventTypeList eventTypeList;
    int contentDatatype;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertNull(eventTypeList.getEE());
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("1969+09:00", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    contentDatatype = scanner.currentState.contentDatatype;
    Assert.assertEquals(EXISchemaConst.G_YEAR_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertEquals(1, eventTypeList.getLength());
    ++n_events;
    
    Assert.assertEquals(5, n_events);
  }

  /**
   * GYear datatype with enumerated values.
   */
  public void testEnumeratedGYearElement_01() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/enumeratedGYearElement_01.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    final String[] xmlStrings;
    final String[] values = {
        " \t\r 1969+09:00\n",
        "1971+09:00",
        "1973+09:00",
        "0001",
        "0012",
        "0123",
        "12345",
    };
    final String[] resultValues = {
        "1969+09:00",
        "1971+09:00",
        "1973+09:00",
        "0001",
        "0012",
        "0123",
        "12345",
    };

    xmlStrings = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + values[i] + "</foo:A>\n";
    };

    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    
    for (int i = 0; i < xmlStrings.length; i++) {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      transmogrifier.setOutputStream(outputStream);
      transmogrifier.encode(new InputSource(new StringReader(xmlStrings[i])));
      byte[] bts = outputStream.toByteArray();
      
      decoder.setInputStream(new ByteArrayInputStream(bts));
      Scanner scanner = decoder.processHeader();
  
      EventDescription exiEvent;
      int n_events = 0;
  
      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals(resultValues[i], exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.assertEquals(EXISchemaConst.G_YEAR_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      Assert.assertEquals(7, schema.getEnumerationFacetCountOfAtomicSimpleType(contentDatatype));
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      ++n_events;
      
      Assert.assertEquals(5, n_events);
    }

    String xmlString = "<foo:A xmlns:foo='urn:foo'>" + "1975+09:00" + "</foo:A>\n";

    // Make sure a value that does not match the enumeration fail to encode.
    transmogrifier.setOutputStream(new ByteArrayOutputStream());
    try {
      transmogrifier.encode(new InputSource(new StringReader(xmlString)));
    }
    catch (TransmogrifierException te) {
      Assert.assertEquals(TransmogrifierException.UNEXPECTED_CHARS, te.getCode());
      Assert.assertTrue(te.getMessage().contains("1975+09:00"));
      return; 
    }
    Assert.fail();
  }

  /**
   * GMonthDay datatype
   */
  public void testGMonthDayElement_01() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/gMonthDayElement_01.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    String originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  --09-16+09:00  " + "</foo:A>\n";
    
    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    transmogrifier.setOutputStream(outputStream);
    transmogrifier.encode(new InputSource(new StringReader(originalValue)));
    byte[] bts = outputStream.toByteArray();
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    decoder.setInputStream(new ByteArrayInputStream(bts));
    Scanner scanner = decoder.processHeader();

    EventDescription exiEvent;
    int n_events = 0;

    EventType eventType;
    EventTypeList eventTypeList;
    int contentDatatype;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertNull(eventTypeList.getEE());
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("--09-16+09:00", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    contentDatatype = scanner.currentState.contentDatatype;
    Assert.assertEquals(EXISchemaConst.G_MONTHDAY_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertEquals(1, eventTypeList.getLength());
    ++n_events;
    
    Assert.assertEquals(5, n_events);
  }

  /**
   * GMonthDay datatype with enumerated values.
   */
  public void testEnumeratedGMonthDayElement_01() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/enumeratedGMonthDayElement_01.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    final String[] xmlStrings;
    final String[] values = {
        " \t\r --09-16+09:00\n",
        "--09-18+09:00",
        "--09-20+09:00",
        "--02-28-10:00",
        "--03-31-10:00",
        "--02-29-10:00",
    };
    final String[] resultValues = {
        "--09-16+09:00",
        "--09-18+09:00",
        "--09-20+09:00",
        "--02-28-10:00",
        "--03-31-10:00",
        "--02-29-10:00",
    };

    xmlStrings = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + values[i] + "</foo:A>\n";
    };

    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    
    for (int i = 0; i < xmlStrings.length; i++) {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      transmogrifier.setOutputStream(outputStream);
      transmogrifier.encode(new InputSource(new StringReader(xmlStrings[i])));
      byte[] bts = outputStream.toByteArray();
      
      decoder.setInputStream(new ByteArrayInputStream(bts));
      Scanner scanner = decoder.processHeader();
  
      EventDescription exiEvent;
      int n_events = 0;
  
      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals(resultValues[i], exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.assertEquals(EXISchemaConst.G_MONTHDAY_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      Assert.assertEquals(6, schema.getEnumerationFacetCountOfAtomicSimpleType(contentDatatype));
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      ++n_events;
      
      Assert.assertEquals(5, n_events);
    }

    String xmlString = "<foo:A xmlns:foo='urn:foo'>" + "--09-22+09:00" + "</foo:A>\n";

    // Make sure a value that does not match the enumeration fail to encode.
    transmogrifier.setOutputStream(new ByteArrayOutputStream());
    try {
      transmogrifier.encode(new InputSource(new StringReader(xmlString)));
    }
    catch (TransmogrifierException te) {
      Assert.assertEquals(TransmogrifierException.UNEXPECTED_CHARS, te.getCode());
      Assert.assertTrue(te.getMessage().contains("--09-22+09:00"));
      return; 
    }
    Assert.fail();
  }

  /**
   * GDay datatype
   */
  public void testGDayElement_01() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/gDayElement_01.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    String originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  ---16+09:00  " + "</foo:A>\n";
    
    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    transmogrifier.setOutputStream(outputStream);
    transmogrifier.encode(new InputSource(new StringReader(originalValue)));
    byte[] bts = outputStream.toByteArray();
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    decoder.setInputStream(new ByteArrayInputStream(bts));
    Scanner scanner = decoder.processHeader();

    EventDescription exiEvent;
    int n_events = 0;

    EventType eventType;
    EventTypeList eventTypeList;
    int contentDatatype;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertNull(eventTypeList.getEE());
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("---16+09:00", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    contentDatatype = scanner.currentState.contentDatatype;
    Assert.assertEquals(EXISchemaConst.G_DAY_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertEquals(1, eventTypeList.getLength());
    ++n_events;
    
    Assert.assertEquals(5, n_events);
  }

  /**
   * GDay datatype with enumerated values.
   */
  public void testEnumeratedGDayElement_01() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/enumeratedGDayElement_01.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    final String[] xmlStrings;
    final String[] values = {
        " \t\r ---16+09:00\n",
        "---18+09:00",
        "---20+09:00",
    };
    final String[] resultValues = {
        "---16+09:00",
        "---18+09:00",
        "---20+09:00",
    };

    xmlStrings = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + values[i] + "</foo:A>\n";
    };

    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    
    for (int i = 0; i < xmlStrings.length; i++) {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      transmogrifier.setOutputStream(outputStream);
      transmogrifier.encode(new InputSource(new StringReader(xmlStrings[i])));
      byte[] bts = outputStream.toByteArray();
      
      decoder.setInputStream(new ByteArrayInputStream(bts));
      Scanner scanner = decoder.processHeader();
  
      EventDescription exiEvent;
      int n_events = 0;
  
      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals(resultValues[i], exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.assertEquals(EXISchemaConst.G_DAY_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      Assert.assertEquals(3, schema.getEnumerationFacetCountOfAtomicSimpleType(contentDatatype));
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      ++n_events;
      
      Assert.assertEquals(5, n_events);
    }

    String xmlString = "<foo:A xmlns:foo='urn:foo'>" + "---22+09:00" + "</foo:A>\n";

    // Make sure a value that does not match the enumeration fail to encode.
    transmogrifier.setOutputStream(new ByteArrayOutputStream());
    try {
      transmogrifier.encode(new InputSource(new StringReader(xmlString)));
    }
    catch (TransmogrifierException te) {
      Assert.assertEquals(TransmogrifierException.UNEXPECTED_CHARS, te.getCode());
      Assert.assertTrue(te.getMessage().contains("---22+09:00"));
      return; 
    }
    Assert.fail();
  }

  /**
   * GMonth datatype
   */
  public void testGMonthElement_01() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/gMonthElement_01.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    String originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  --07+09:00  " + "</foo:A>\n";
    
    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    transmogrifier.setOutputStream(outputStream);
    transmogrifier.encode(new InputSource(new StringReader(originalValue)));
    byte[] bts = outputStream.toByteArray();
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    decoder.setInputStream(new ByteArrayInputStream(bts));
    Scanner scanner = decoder.processHeader();

    EventDescription exiEvent;
    int n_events = 0;

    EventType eventType;
    EventTypeList eventTypeList;
    int contentDatatype;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertNull(eventTypeList.getEE());
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("--07+09:00", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    contentDatatype = scanner.currentState.contentDatatype;
    Assert.assertEquals(EXISchemaConst.G_MONTH_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertEquals(1, eventTypeList.getLength());
    ++n_events;
    
    Assert.assertEquals(5, n_events);
  }

  /**
   * GMonth datatype with enumerated values.
   */
  public void testEnumeratedGMonthElement_01() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/enumeratedGMonthElement_01.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    final String[] xmlStrings;
    final String[] values = {
        " \t\r --07+09:00\n",
        "--09+09:00",
        "--11+09:00",
    };
    final String[] resultValues = {
        "--07+09:00",
        "--09+09:00",
        "--11+09:00",
    };
    
    xmlStrings = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + values[i] + "</foo:A>\n";
    };

    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    
    for (int i = 0; i < xmlStrings.length; i++) {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      transmogrifier.setOutputStream(outputStream);
      transmogrifier.encode(new InputSource(new StringReader(xmlStrings[i])));
      byte[] bts = outputStream.toByteArray();
      
      decoder.setInputStream(new ByteArrayInputStream(bts));
      Scanner scanner = decoder.processHeader();
  
      EventDescription exiEvent;
      int n_events = 0;
  
      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals(resultValues[i], exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.assertEquals(EXISchemaConst.G_MONTH_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      Assert.assertEquals(3, schema.getEnumerationFacetCountOfAtomicSimpleType(contentDatatype));
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      ++n_events;
      
      Assert.assertEquals(5, n_events);
    }

    String xmlString = "<foo:A xmlns:foo='urn:foo'>" + "--05+09:00" + "</foo:A>\n";

    // Make sure a value that does not match the enumeration fail to encode.
    transmogrifier.setOutputStream(new ByteArrayOutputStream());
    try {
      transmogrifier.encode(new InputSource(new StringReader(xmlString)));
    }
    catch (TransmogrifierException te) {
      Assert.assertEquals(TransmogrifierException.UNEXPECTED_CHARS, te.getCode());
      Assert.assertTrue(te.getMessage().contains("--05+09:00"));
      return; 
    }
    Assert.fail();
  }

  /**
   * GMonth datatype with obsolete enumerated values.
   */
  public void testEnumeratedGMonthElement_02() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/enumeratedGMonthElement_02.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    final String[] xmlStrings;
    final String[] values = {
        " \t\r --07+09:00\n",
        "--09+09:00",
        "--11+09:00",
    };
    final String[] resultValues = {
        "--07+09:00",
        "--09+09:00",
        "--11+09:00",
    };
    
    xmlStrings = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + values[i] + "</foo:A>\n";
    };

    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    
    for (int i = 0; i < xmlStrings.length; i++) {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      transmogrifier.setOutputStream(outputStream);
      transmogrifier.encode(new InputSource(new StringReader(xmlStrings[i])));
      byte[] bts = outputStream.toByteArray();
      
      decoder.setInputStream(new ByteArrayInputStream(bts));
      Scanner scanner = decoder.processHeader();
  
      EventDescription exiEvent;
      int n_events = 0;
  
      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals(resultValues[i], exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.assertEquals(EXISchemaConst.G_MONTH_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      Assert.assertEquals(0, schema.getEnumerationFacetCountOfAtomicSimpleType(contentDatatype));
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      ++n_events;
      
      Assert.assertEquals(5, n_events);
    }

    String xmlString = "<foo:A xmlns:foo='urn:foo'>" + "--05+09:00" + "</foo:A>\n";

    // Make sure a value that does not match the enumeration fail to encode.
    transmogrifier.setOutputStream(new ByteArrayOutputStream());
    transmogrifier.encode(new InputSource(new StringReader(xmlString)));
  }
  
  /**
   * HexBinary datatype
   */
  public void testHexBinaryElement_01() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/hexBinaryElement_01.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    String originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  6161616161  " + "</foo:A>\n";
    
    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    transmogrifier.setOutputStream(outputStream);
    transmogrifier.encode(new InputSource(new StringReader(originalValue)));
    byte[] bts = outputStream.toByteArray();
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    decoder.setInputStream(new ByteArrayInputStream(bts));
    Scanner scanner = decoder.processHeader();

    EventDescription exiEvent;
    int n_events = 0;

    EventType eventType;
    EventTypeList eventTypeList;
    int contentDatatype;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertNull(eventTypeList.getEE());
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("6161616161", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    contentDatatype = scanner.currentState.contentDatatype;
    Assert.assertEquals(EXISchemaConst.HEXBINARY_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertEquals(1, eventTypeList.getLength());
    ++n_events;
    
    Assert.assertEquals(5, n_events);
  }

  /**
   * HexBinary datatype with enumerated values.
   */
  public void testEnumeratedHexBinaryElement_01() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/enumeratedHexBinaryElement_01.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    final String[] xmlStrings;
    final String[] values = {
        " \t\r 6161616161\n",
        "6363636363",
        "6565656565",
    };
    final String[] resultValues = {
        "6161616161",
        "6363636363",
        "6565656565",
    };
    
    xmlStrings = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + values[i] + "</foo:A>\n";
    };

    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    
    for (int i = 0; i < xmlStrings.length; i++) {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      transmogrifier.setOutputStream(outputStream);
      transmogrifier.encode(new InputSource(new StringReader(xmlStrings[i])));
      byte[] bts = outputStream.toByteArray();
      
      decoder.setInputStream(new ByteArrayInputStream(bts));
      Scanner scanner = decoder.processHeader();
  
      EventDescription exiEvent;
      int n_events = 0;
  
      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals(resultValues[i], exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.assertEquals(EXISchemaConst.HEXBINARY_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      Assert.assertEquals(3, schema.getEnumerationFacetCountOfAtomicSimpleType(contentDatatype));
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      ++n_events;
      
      Assert.assertEquals(5, n_events);
    }

    String xmlString = "<foo:A xmlns:foo='urn:foo'>" + "6262626262" + "</foo:A>\n";

    // Make sure a value that does not match the enumeration fail to encode.
    transmogrifier.setOutputStream(new ByteArrayOutputStream());
    try {
      transmogrifier.encode(new InputSource(new StringReader(xmlString)));
    }
    catch (TransmogrifierException te) {
      Assert.assertEquals(TransmogrifierException.UNEXPECTED_CHARS, te.getCode());
      Assert.assertTrue(te.getMessage().contains("6262626262"));
      return; 
    }
    Assert.fail();
  }

  /**
   * Base64Binary datatype
   */
  public void testBase64BinaryElement_01() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/base64BinaryElement_01.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    String originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  YWFhYWE=  " + "</foo:A>\n";
    
    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    transmogrifier.setOutputStream(outputStream);
    transmogrifier.encode(new InputSource(new StringReader(originalValue)));
    byte[] bts = outputStream.toByteArray();
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    decoder.setInputStream(new ByteArrayInputStream(bts));
    Scanner scanner = decoder.processHeader();

    EventDescription exiEvent;
    int n_events = 0;

    EventType eventType;
    EventTypeList eventTypeList;
    int contentDatatype;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertNull(eventTypeList.getEE());
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("YWFhYWE=", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    contentDatatype = scanner.currentState.contentDatatype;
    Assert.assertEquals(EXISchemaConst.BASE64BINARY_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertEquals(1, eventTypeList.getLength());
    ++n_events;
    
    Assert.assertEquals(5, n_events);
  }

  /**
   * Base64Binary datatype with enumerated values.
   */
  public void testEnumeratedBase64BinaryElement_01() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/enumeratedBase64BinaryElement_01.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    final String[] xmlStrings;
    final String[] values = {
        " \t\r YWFhYWE=\n",
        "Y2NjY2M=",
        "ZWVlZWU=",
    };
    final String[] resultValues = {
        "YWFhYWE=",
        "Y2NjY2M=",
        "ZWVlZWU=",
    };
    
    xmlStrings = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + values[i] + "</foo:A>\n";
    };

    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    
    for (int i = 0; i < xmlStrings.length; i++) {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      transmogrifier.setOutputStream(outputStream);
      transmogrifier.encode(new InputSource(new StringReader(xmlStrings[i])));
      byte[] bts = outputStream.toByteArray();
      
      decoder.setInputStream(new ByteArrayInputStream(bts));
      Scanner scanner = decoder.processHeader();
  
      EventDescription exiEvent;
      int n_events = 0;
  
      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals(resultValues[i], exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.assertEquals(EXISchemaConst.BASE64BINARY_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      Assert.assertEquals(3, schema.getEnumerationFacetCountOfAtomicSimpleType(contentDatatype));
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      ++n_events;
      
      Assert.assertEquals(5, n_events);
    }

    String xmlString = "<foo:A xmlns:foo='urn:foo'>" + "YmJiYmI=" + "</foo:A>\n";

    // Make sure a value that does not match the enumeration fail to encode.
    transmogrifier.setOutputStream(new ByteArrayOutputStream());
    try {
      transmogrifier.encode(new InputSource(new StringReader(xmlString)));
    }
    catch (TransmogrifierException te) {
      Assert.assertEquals(TransmogrifierException.UNEXPECTED_CHARS, te.getCode());
      Assert.assertTrue(te.getMessage().contains("YmJiYmI="));
      return; 
    }
    Assert.fail();
  }

  /**
   * AnyURI datatype
   */
  public void testAnyURIElement_01() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/anyURIElement_01.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    String originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  urn:foo  " + "</foo:A>\n";
    
    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    transmogrifier.setOutputStream(outputStream);
    transmogrifier.encode(new InputSource(new StringReader(originalValue)));
    byte[] bts = outputStream.toByteArray();
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    decoder.setInputStream(new ByteArrayInputStream(bts));
    Scanner scanner = decoder.processHeader();

    EventDescription exiEvent;
    int n_events = 0;

    EventType eventType;
    EventTypeList eventTypeList;
    int contentDatatype;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertNull(eventTypeList.getEE());
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("  urn:foo  ", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    contentDatatype = scanner.currentState.contentDatatype;
    Assert.assertEquals(EXISchemaConst.ANYURI_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertEquals(1, eventTypeList.getLength());
    ++n_events;
    
    Assert.assertEquals(5, n_events);
  }

  /**
   * AnyURI datatype with enumerated values.
   */
  public void testEnumeratedAnyURIElement_01() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/enumeratedAnyURIElement_01.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    final String[] xmlStrings;
    final String[] values = {
        " \t\r urn:foo\n",
        "urn:goo",
        "urn:hoo",
    };
    final String[] resultValues = {
        "urn:foo",
        "urn:goo",
        "urn:hoo",
    };
    
    xmlStrings = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + values[i] + "</foo:A>\n";
    };

    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    
    for (int i = 0; i < xmlStrings.length; i++) {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      transmogrifier.setOutputStream(outputStream);
      transmogrifier.encode(new InputSource(new StringReader(xmlStrings[i])));
      byte[] bts = outputStream.toByteArray();
      
      decoder.setInputStream(new ByteArrayInputStream(bts));
      Scanner scanner = decoder.processHeader();
  
      EventDescription exiEvent;
      int n_events = 0;
  
      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals(resultValues[i], exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.assertEquals(EXISchemaConst.ANYURI_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      Assert.assertEquals(3, schema.getEnumerationFacetCountOfAtomicSimpleType(contentDatatype));
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      ++n_events;
      
      Assert.assertEquals(5, n_events);
    }

    String xmlString = "<foo:A xmlns:foo='urn:foo'>" + "urn:ioo" + "</foo:A>\n";

    // Make sure a value that does not match the enumeration fail to encode.
    transmogrifier.setOutputStream(new ByteArrayOutputStream());
    try {
      transmogrifier.encode(new InputSource(new StringReader(xmlString)));
    }
    catch (TransmogrifierException te) {
      Assert.assertEquals(TransmogrifierException.UNEXPECTED_CHARS, te.getCode());
      Assert.assertTrue(te.getMessage().contains("urn:ioo"));
      return; 
    }
    Assert.fail();
  }

  /**
   * QName datatype
   */
  public void testQNameElement_01() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/qNameElement_01.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    String originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  foo:A  " + "</foo:A>\n";
    
    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    transmogrifier.setOutputStream(outputStream);
    transmogrifier.encode(new InputSource(new StringReader(originalValue)));
    byte[] bts = outputStream.toByteArray();
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    decoder.setInputStream(new ByteArrayInputStream(bts));
    Scanner scanner = decoder.processHeader();

    EventDescription exiEvent;
    int n_events = 0;

    EventType eventType;
    EventTypeList eventTypeList;
    int contentDatatype;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertNull(eventTypeList.getEE());
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("  foo:A  ", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    contentDatatype = scanner.currentState.contentDatatype;
    Assert.assertEquals(EXISchemaConst.QNAME_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertEquals(1, eventTypeList.getLength());
    ++n_events;
    
    Assert.assertEquals(5, n_events);
  }

  /**
   * QName datatype with enumerated values.
   */
  public void testEnumeratedQNameElement_01() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/enumeratedQNameElement_01.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    final String[] xmlStrings;
    final String[] values = {
        " \t\r foo:A\n",
        "goo:A",
        "hoo:A" // undefined values are accepted 
    };
    final String[] resultValues = {
        " \t\n foo:A\n",
        "goo:A",
        "hoo:A",
    };
    
    xmlStrings = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + values[i] + "</foo:A>\n";
    };

    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    
    for (int i = 0; i < xmlStrings.length; i++) {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      transmogrifier.setOutputStream(outputStream);
      transmogrifier.encode(new InputSource(new StringReader(xmlStrings[i])));
      byte[] bts = outputStream.toByteArray();
      
      decoder.setInputStream(new ByteArrayInputStream(bts));
      Scanner scanner = decoder.processHeader();
  
      EventDescription exiEvent;
      int n_events = 0;
  
      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals(resultValues[i], exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.assertEquals(EXISchemaConst.QNAME_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      Assert.assertEquals(0, schema.getEnumerationFacetCountOfAtomicSimpleType(contentDatatype));
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      ++n_events;
      
      Assert.assertEquals(5, n_events);
    }
  }

  /**
   * NOTATION datatype with enumerated values.
   */
  public void testEnumeratedNotationElement_01() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/enumeratedNotationElement_01.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    final String[] xmlStrings;
    final String[] values = {
        " \t\r foo:cat\n",
        "foo:dog",
        "foo:pig",
        "foo:monkey" // undefined values are accepted 
    };
    final String[] resultValues = {
        " \t\n foo:cat\n",
        "foo:dog",
        "foo:pig",
        "foo:monkey" 
    };
    
    xmlStrings = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + values[i] + "</foo:A>\n";
    };

    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    
    for (int i = 0; i < xmlStrings.length; i++) {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      transmogrifier.setOutputStream(outputStream);
      transmogrifier.encode(new InputSource(new StringReader(xmlStrings[i])));
      byte[] bts = outputStream.toByteArray();
      
      decoder.setInputStream(new ByteArrayInputStream(bts));
      Scanner scanner = decoder.processHeader();
  
      EventDescription exiEvent;
      int n_events = 0;
  
      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals(resultValues[i], exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.assertEquals(EXISchemaConst.NOTATION_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      Assert.assertEquals(0, schema.getEnumerationFacetCountOfAtomicSimpleType(contentDatatype));
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      ++n_events;
      
      Assert.assertEquals(5, n_events);
    }
  }

  /**
   * Local attribute.
   */
  public void testAttributeLocal_01() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/attributeLocal_01.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    final String[] xmlStrings;
    final String[] originalValues = {
      "  true  ", 
      "  false  ",
    };
    final String[] resultValues = {
      "true", 
      "false",
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' a='" + originalValues[i] + "'>" +  "</foo:A>\n";
    };

    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    for (i = 0; i < xmlStrings.length; i++) {
      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.setGrammarCache(grammarCache);
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      transmogrifier.setOutputStream(outputStream);
      transmogrifier.encode(new InputSource(new StringReader(xmlStrings[i])));
      byte[] bts = outputStream.toByteArray();
      
      EXIDecoder decoder = new EXIDecoder();
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      Scanner scanner = decoder.processHeader();
  
      EventDescription exiEvent;
      int n_events = 0;
  
      EventType eventType;
      EventTypeList eventTypeList;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
      Assert.assertEquals(resultValues[i], exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      ++n_events;
      
      Assert.assertEquals(5, n_events);
    }
  }

  /**
   * Use of Global attribute through attribute uses.
   */
  public void testAttributeGlobal_01() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/attributeGlobal_01.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    final String[] xmlStrings;
    final String[] originalValues = {
      "  true  ", 
      "  false  ",
    };
    final String[] resultValues = {
      "true", 
      "false",
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' foo:a='" + originalValues[i] + "'>" +  "</foo:A>\n";
    };

    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    for (i = 0; i < xmlStrings.length; i++) {
      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.setGrammarCache(grammarCache);
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      transmogrifier.setOutputStream(outputStream);
      transmogrifier.encode(new InputSource(new StringReader(xmlStrings[i])));
      byte[] bts = outputStream.toByteArray();
      
      EXIDecoder decoder = new EXIDecoder();
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      Scanner scanner = decoder.processHeader();
  
      EventDescription exiEvent;
      int n_events = 0;
  
      EventType eventType;
      EventTypeList eventTypeList;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
      Assert.assertEquals(resultValues[i], exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      ++n_events;
      
      Assert.assertEquals(5, n_events);
    }
  }

  /**
   * Use of Global attribute through attribute wildcards.
   */
  public void testAttributeGlobal_02() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/attributeGlobal_02.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    final String[] xmlStrings;
    final String[] originalValues = {
      "  true  ", 
      "  false  ",
    };
    final String[] resultValues = {
      "true", 
      "false",
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' foo:a='" + originalValues[i] + "'>" +  "</foo:A>\n";
    };

    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    for (i = 0; i < xmlStrings.length; i++) {
      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.setGrammarCache(grammarCache);
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      transmogrifier.setOutputStream(outputStream);
      transmogrifier.encode(new InputSource(new StringReader(xmlStrings[i])));
      byte[] bts = outputStream.toByteArray();
      
      EXIDecoder decoder = new EXIDecoder();
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      Scanner scanner = decoder.processHeader();
  
      EventDescription exiEvent;
      int n_events = 0;
  
      EventType eventType;
      EventTypeList eventTypeList;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
      Assert.assertEquals(resultValues[i], exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:foo", eventType.uri);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      ++n_events;
      
      Assert.assertEquals(5, n_events);
    }
  }

  /**
   * Content datatype of complex types 
   */
  public void testContentDatatype_01() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/contentDatatype_01.xsd", getClass(), 
        new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder, m_schemaReader));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    Assert.assertNotNull(schema);
    
    String originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  2003-03-19-05:00  " + "</foo:A>\n";
    
    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(grammarCache);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    transmogrifier.setOutputStream(outputStream);
    transmogrifier.encode(new InputSource(new StringReader(originalValue)));
    byte[] bts = outputStream.toByteArray();
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    decoder.setInputStream(new ByteArrayInputStream(bts));
    Scanner scanner = decoder.processHeader();

    EventDescription exiEvent;
    int n_events = 0;

    EventType eventType;
    EventTypeList eventTypeList;
    int contentDatatype;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertNull(eventTypeList.getEE());
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("2003-03-19-05:00", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    contentDatatype = scanner.currentState.contentDatatype;
    Assert.assertEquals(EXISchemaConst.DATE_TYPE, schema.getSerialOfType(contentDatatype));
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertEquals(1, eventTypeList.getLength());
    ++n_events;
    
    Assert.assertEquals(5, n_events);
  }

}
