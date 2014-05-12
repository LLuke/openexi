package org.openexi.sax;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;

import junit.framework.Assert;

import org.xml.sax.InputSource;

import org.openexi.proc.EXIDecoder;
import org.openexi.proc.EXISchemaResolver;
import org.openexi.proc.HeaderOptionsOutputType;
import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EXIOptions;
import org.openexi.proc.common.EventCode;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventTypeList;
import org.openexi.proc.common.EventDescription;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.common.SchemaId;
import org.openexi.proc.common.XmlUriConst;
import org.openexi.proc.events.EXIEventNS;
import org.openexi.proc.events.EXIEventSchemaNil;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.proc.grammars.OptionsGrammarCache;
import org.openexi.proc.io.Scanner;
import org.openexi.proc.util.ExiUriConst;
import org.openexi.schema.EXISchema;
import org.openexi.schema.EmptySchema;
import org.openexi.schema.TestBase;
import org.openexi.scomp.EXISchemaFactoryErrorMonitor;
import org.openexi.scomp.EXISchemaFactoryTestUtil;

public class OptionsDocumentTest extends TestBase {

  public OptionsDocumentTest(String name) {
    super(name);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    m_compilerErrors = new EXISchemaFactoryErrorMonitor();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    m_compilerErrors.clear();
  }

  private EXISchemaFactoryErrorMonitor m_compilerErrors;

  final AlignmentType[] Alignments = new AlignmentType[] { 
      AlignmentType.bitPacked, 
      AlignmentType.byteAligned, 
      AlignmentType.preCompress, 
      AlignmentType.compress 
  };

  ///////////////////////////////////////////////////////////////////////////
  // Test cases
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Schema:
   * 
   * Instance:
   */
  public void testSchemaId_01() throws Exception {
    GrammarCache grammarCache = OptionsGrammarCache.getGrammarCache(); 
    
    Transmogrifier encoder = new Transmogrifier();
    encoder.setGrammarCache(grammarCache);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    encoder.setOutputStream(baos);
    
    String xmlString;
    byte[] bts;
    EXIDecoder decoder;
    Scanner scanner;
    int n_events;
    
    xmlString = 
      "<header xmlns='http://www.w3.org/2009/exi' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>\n" +
      "  <common>\n" +
      "    <schemaId xsi:nil='true'/>\n" +
      "  </common>\n" +
      "</header>\n";
    
    encoder.encode(new InputSource(new StringReader(xmlString)));
    
    bts = baos.toByteArray();
    
    decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    decoder.setInputStream(new ByteArrayInputStream(bts));
    scanner = decoder.processHeader();
    Assert.assertNull(scanner.getHeaderOptions());
    
    EventDescription exiEvent;
    EventType eventType;
    EventTypeList eventTypeList;

    n_events = 0;

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
    Assert.assertEquals("header", exiEvent.getName());
    Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("header", eventType.name);
    Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, eventType.uri);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertEquals(2, eventTypeList.getLength());
    eventType = eventTypeList.item(1);
    Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
    ++n_events;
    
    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("common", exiEvent.getName());
    Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("common", eventType.name);
    Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, eventType.uri);
    Assert.assertEquals(1, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertEquals(4, eventTypeList.getLength());
    eventType = eventTypeList.item(0);
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("lesscommon", eventType.name);
    Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, eventType.uri);
    eventType = eventTypeList.item(2);
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("strict", eventType.name);
    Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, eventType.uri);
    eventType = eventTypeList.item(3);
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    ++n_events;
    
    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("schemaId", exiEvent.getName());
    Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("schemaId", eventType.name);
    Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, eventType.uri);
    Assert.assertEquals(2, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertEquals(4, eventTypeList.getLength());
    eventType = eventTypeList.item(0);
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("compression", eventType.name);
    Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, eventType.uri);
    eventType = eventTypeList.item(1);
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("fragment", eventType.name);
    Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, eventType.uri);
    eventType = eventTypeList.item(3);
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_NL, exiEvent.getEventKind());
    Assert.assertEquals("nil", exiEvent.getName());
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
    Assert.assertTrue(((EXIEventSchemaNil)exiEvent).isNilled());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
    Assert.assertEquals("nil", eventType.name);
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertEquals(2, eventTypeList.getLength());
    eventType = eventTypeList.item(1);
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    eventTypeList = eventType.getEventTypeList();
    Assert.assertEquals(1, eventTypeList.getLength());
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertEquals(1, eventTypeList.getLength());
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertEquals(2, eventTypeList.getLength());
    eventType = eventTypeList.item(0);
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("strict", eventType.name);
    Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, eventType.uri);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    ++n_events;

    Assert.assertEquals(9, n_events);
  }

  /**
   */
  public void testAlignmentOption_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/optionsSchema.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    
    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      encoder.setGrammarCache(grammarCache);
      encoder.setAlignmentType(alignment);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      encoder.setOutputOptions(HeaderOptionsOutputType.lessSchemaId);
      
      String xmlString;
      byte[] bts;
      EXIDecoder decoder;
      Scanner scanner;
      int n_events;
      
      xmlString = "<header xmlns='http://www.w3.org/2009/exi'><strict/></header>\n";
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder = new EXIDecoder();
      decoder.setGrammarCache(grammarCache);
      // DO NOT SET AlignmentType for decoder.
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EventDescription exiEvent;
      EventType eventType;
      n_events = 0;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("header", exiEvent.getName());
      Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("strict", exiEvent.getName());
      Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      ++n_events;
      
      Assert.assertEquals(6, n_events);
    }
  }

  /**
   */
  public void testStrictOption_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/optionsSchema.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    
    GrammarCache encodeGrammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    GrammarCache decodeGrammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      encoder.setGrammarCache(encodeGrammarCache);
      encoder.setAlignmentType(alignment);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      encoder.setOutputOptions(HeaderOptionsOutputType.lessSchemaId);
      
      String xmlString;
      byte[] bts;
      EXIDecoder decoder;
      Scanner scanner;
      int n_events;
      
      xmlString = "<header xmlns='http://www.w3.org/2009/exi'><strict/></header>\n";
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder = new EXIDecoder();
      decoder.setGrammarCache(decodeGrammarCache);
      // DO NOT SET AlignmentType for decoder.
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EventDescription exiEvent;
      EventType eventType;
      n_events = 0;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("header", exiEvent.getName());
      Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("strict", exiEvent.getName());
      Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      ++n_events;
      
      Assert.assertEquals(6, n_events);
    }
  }
  
  /**
   */
  public void testPreserveCommentsOption_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/optionsSchema.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    
    GrammarCache encodeGrammarCache = new GrammarCache(corpus, GrammarOptions.addCM(GrammarOptions.DEFAULT_OPTIONS));
    GrammarCache decodeGrammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      encoder.setGrammarCache(encodeGrammarCache);
      encoder.setAlignmentType(alignment);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      encoder.setOutputOptions(HeaderOptionsOutputType.lessSchemaId);
      
      String xmlString;
      byte[] bts;
      EXIDecoder decoder;
      Scanner scanner;
      int n_events;
      
      xmlString = "<header xmlns='http://www.w3.org/2009/exi'><!-- A --><strict/><!-- B --></header>\n";
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder = new EXIDecoder();
      decoder.setGrammarCache(decodeGrammarCache);
      // DO NOT SET AlignmentType for decoder.
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EventDescription exiEvent;
      EventType eventType;
      n_events = 0;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("header", exiEvent.getName());
      Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CM, exiEvent.getEventKind());
      Assert.assertEquals(" A ", exiEvent.getCharacters().makeString());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("strict", exiEvent.getName());
      Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CM, exiEvent.getEventKind());
      Assert.assertEquals(" B ", exiEvent.getCharacters().makeString());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      ++n_events;
      
      Assert.assertEquals(8, n_events);
    }
  }
  
  /**
   */
  public void testPreserveProcessingInstructionOption_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/optionsSchema.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    
    GrammarCache encodeGrammarCache = new GrammarCache(corpus, GrammarOptions.addPI(GrammarOptions.DEFAULT_OPTIONS));
    GrammarCache decodeGrammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      encoder.setGrammarCache(encodeGrammarCache);
      encoder.setAlignmentType(alignment);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      encoder.setOutputOptions(HeaderOptionsOutputType.lessSchemaId);
      
      String xmlString;
      byte[] bts;
      EXIDecoder decoder;
      Scanner scanner;
      int n_events;
      
      xmlString = "<header xmlns='http://www.w3.org/2009/exi'><?eg Good! ?><strict/><?eg Good? ?></header>\n";
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder = new EXIDecoder();
      decoder.setGrammarCache(decodeGrammarCache);
      // DO NOT SET AlignmentType for decoder.
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EventDescription exiEvent;
      EventType eventType;
      
      n_events = 0;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("header", exiEvent.getName());
      Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_PI, exiEvent.getEventKind());
      Assert.assertEquals("Good! ", exiEvent.getCharacters().makeString());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("strict", exiEvent.getName());
      Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_PI, exiEvent.getEventKind());
      Assert.assertEquals("Good? ", exiEvent.getCharacters().makeString());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      ++n_events;
      
      Assert.assertEquals(8, n_events);
    }
  }

  /**
   */
  public void testPreserveDtdOption_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/optionsSchema.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    
    GrammarCache encodeGrammarCache = new GrammarCache(corpus, GrammarOptions.addDTD(GrammarOptions.DEFAULT_OPTIONS));
    GrammarCache decodeGrammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      encoder.setResolveExternalGeneralEntities(false);
      encoder.setGrammarCache(encodeGrammarCache);
      encoder.setAlignmentType(alignment);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      encoder.setOutputOptions(HeaderOptionsOutputType.lessSchemaId);
      
      String xmlString;
      byte[] bts;
      EXIDecoder decoder;
      Scanner scanner;
      int n_events;
      
      xmlString = "<!DOCTYPE header [ <!ENTITY ent SYSTEM 'er-entity.xml'> ]>" + 
        "<header xmlns='http://www.w3.org/2009/exi'>&ent;<strict/></header>\n";
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder = new EXIDecoder();
      decoder.setGrammarCache(decodeGrammarCache);
      // DO NOT SET AlignmentType for decoder.
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EventDescription exiEvent;
      EventType eventType;
      n_events = 0;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_DTD, exiEvent.getEventKind());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("header", exiEvent.getName());
      Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ER, exiEvent.getEventKind());
      Assert.assertEquals("ent", exiEvent.getName());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("strict", exiEvent.getName());
      Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      ++n_events;
      
      Assert.assertEquals(8, n_events);
    }
  }

  /**
   */
  public void testPreservePrefixesOption_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/optionsSchema.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    
    GrammarCache encodeGrammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));
    GrammarCache decodeGrammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      encoder.setGrammarCache(encodeGrammarCache);
      encoder.setAlignmentType(alignment);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      encoder.setOutputOptions(HeaderOptionsOutputType.lessSchemaId);
      
      String xmlString;
      byte[] bts;
      EXIDecoder decoder;
      Scanner scanner;
      int n_events;
      
      xmlString = "<exi:header xmlns:exi='http://www.w3.org/2009/exi'><exi:strict/></exi:header>\n";
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder = new EXIDecoder();
      decoder.setGrammarCache(decodeGrammarCache);
      // DO NOT SET AlignmentType for decoder.
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EventDescription exiEvent;
      EventType eventType;
      n_events = 0;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("header", exiEvent.getName());
      Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
      Assert.assertNull(exiEvent.getPrefix());
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
      Assert.assertEquals("exi", exiEvent.getPrefix());
      Assert.assertEquals("http://www.w3.org/2009/exi", exiEvent.getURI());
      Assert.assertTrue(((EXIEventNS)exiEvent).getLocalElementNs());
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("strict", exiEvent.getName());
      Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
      Assert.assertEquals("exi", exiEvent.getPrefix());
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      ++n_events;
      
      Assert.assertEquals(7, n_events);
    }
  }

  /**
   */
  public void testSchemaIdOption_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/optionsSchema.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    
    GrammarCache encodeGrammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    GrammarCache decodeGrammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      encoder.setGrammarCache(encodeGrammarCache, new SchemaId("aiueo"));
      encoder.setAlignmentType(alignment);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      encoder.setOutputOptions(HeaderOptionsOutputType.all);
      
      String xmlString;
      byte[] bts;
      EXIDecoder decoder;
      Scanner scanner;
      int n_events;
      EXIOptions headerOptions;
      
      xmlString = "<header xmlns='http://www.w3.org/2009/exi'><strict/></header>\n";
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder = new EXIDecoder();
      decoder.setGrammarCache(decodeGrammarCache);
      // DO NOT SET AlignmentType for decoder.
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      headerOptions = scanner.getHeaderOptions();
      Assert.assertEquals("aiueo", headerOptions.getSchemaId().getValue());
      
      EventDescription exiEvent;
      EventType eventType;
      n_events = 0;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("header", exiEvent.getName());
      Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("strict", exiEvent.getName());
      Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      ++n_events;
      
      Assert.assertEquals(6, n_events);
    }
  }

  /**
   * Use HeaderOptionsOutputType of value lessSchemaId to suppress 
   * schemaId in header options.
   */
  public void testSchemaIdOption_02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/optionsSchema.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    
    GrammarCache encodeGrammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    GrammarCache decodeGrammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      encoder.setGrammarCache(encodeGrammarCache, new SchemaId("aiueo"));
      encoder.setAlignmentType(alignment);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      encoder.setOutputOptions(HeaderOptionsOutputType.lessSchemaId);
      
      String xmlString;
      byte[] bts;
      EXIDecoder decoder;
      Scanner scanner;
      int n_events;
      EXIOptions headerOptions;
      
      xmlString = "<header xmlns='http://www.w3.org/2009/exi'><strict/></header>\n";
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder = new EXIDecoder();
      decoder.setGrammarCache(decodeGrammarCache);
      // DO NOT SET AlignmentType for decoder.
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      headerOptions = scanner.getHeaderOptions();
      Assert.assertNull(headerOptions.getSchemaId());
      
      EventDescription exiEvent;
      EventType eventType;
      n_events = 0;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("header", exiEvent.getName());
      Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("strict", exiEvent.getName());
      Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      ++n_events;
      
      Assert.assertEquals(6, n_events);
    }
  }

  /**
   * Let the decoder fetch a grammar cache via EXISchemaResolver.
   */
  public void testSchemaIdOption_03() throws Exception {
    final EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/optionsSchema.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    
    GrammarCache encodeGrammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      encoder.setGrammarCache(encodeGrammarCache, new SchemaId("aiueo"));
      encoder.setAlignmentType(alignment);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      encoder.setOutputOptions(HeaderOptionsOutputType.all);
      
      String xmlString;
      byte[] bts;
      EXIDecoder decoder;
      Scanner scanner;
      int n_events;
      EXIOptions headerOptions;
      
      xmlString = "<header xmlns='http://www.w3.org/2009/exi'><strict/></header>\n";
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder = new EXIDecoder();
      decoder.setEXISchemaResolver(new EXISchemaResolver() {
          public GrammarCache resolveSchema(String schemaId, short grammarOptions) {
            if ("aiueo".equals(schemaId)) {
              return new GrammarCache(corpus, grammarOptions);
            }
            Assert.fail();
            return null;
          }
        }
      );
      // DO NOT SET AlignmentType for decoder.
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      headerOptions = scanner.getHeaderOptions();
      Assert.assertEquals("aiueo", headerOptions.getSchemaId().getValue());
      
      EventDescription exiEvent;
      EventType eventType;
      n_events = 0;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("header", exiEvent.getName());
      Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("strict", exiEvent.getName());
      Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      ++n_events;
      
      Assert.assertEquals(6, n_events);
    }
  }
  
  /**
   */
  public void testSchemaIdOptionNil_01() throws Exception {
    
    GrammarCache grammarCache = new GrammarCache((EXISchema)null, 
        GrammarOptions.addCM(GrammarOptions.DEFAULT_OPTIONS));
    
    final String xmlString;
    byte[] bts;
    int n_events;
    
    xmlString = "<None xmlns='urn:foo'><!-- abc --><!-- def --></None>\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      
      encoder.setOutputOptions(HeaderOptionsOutputType.all);
      encoder.setAlignmentType(alignment);

      encoder.setGrammarCache(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      EXIDecoder decoder = new EXIDecoder();
      decoder.setInputStream(new ByteArrayInputStream(bts));
      Scanner scanner = decoder.processHeader();
      
      EventDescription exiEvent;
      EventType eventType;
      EventTypeList eventTypeList;
      n_events = 0;
  
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
      Assert.assertEquals("None", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CM, exiEvent.getEventKind());
      Assert.assertEquals(" abc ", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_CM, eventType.itemType);
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CM, exiEvent.getEventKind());
      Assert.assertEquals(" def ", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_CM, eventType.itemType);
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      ++n_events;
      
      Assert.assertEquals(6, n_events);
    }
  }

  /**
   */
  public void testSelfContainedOption_01() throws Exception {
    Transmogrifier encoder = new Transmogrifier();
    encoder.setGrammarCache(OptionsGrammarCache.getGrammarCache());
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    encoder.setOutputStream(baos);
    encoder.setOutputOptions(HeaderOptionsOutputType.none);
    
    String xmlString;
    EXIDecoder decoder;
    Scanner scanner;
    
    xmlString = "<header xmlns='http://www.w3.org/2009/exi'><lesscommon><uncommon><selfContained/></uncommon></lesscommon></header>\n";
    
    encoder.encode(new InputSource(new StringReader(xmlString)));
    
    byte[] bts = baos.toByteArray();
    /**
     * 0011 0000 where the third bit (Presence Bit) is on.
     * This effectively makes the encoded body appear as an header options. 
     */
    bts[0] = (byte)0xB0;  
    
    decoder = new EXIDecoder();
    decoder.setGrammarCache(new GrammarCache(EmptySchema.getEXISchema(), GrammarOptions.STRICT_OPTIONS));
    decoder.setInputStream(new ByteArrayInputStream(bts));
    scanner = decoder.processHeader();
    
    final EXIOptions options = scanner.getHeaderOptions();
    Assert.assertTrue(options.getInfuseSC());
  }

}
