package org.openexi.sax;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.ArrayList;

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
import org.openexi.proc.common.EXIEvent;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.common.SchemaId;
import org.openexi.proc.common.XmlUriConst;
import org.openexi.proc.events.EXIEventNS;
import org.openexi.proc.events.EXIEventSchemaNil;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.proc.grammars.OptionsGrammarCache;
import org.openexi.proc.io.Scanner;
import org.openexi.proc.util.ExiUriConst;
import org.openexi.sax.Transmogrifier;
import org.openexi.schema.EXISchema;
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
    encoder.setEXISchema(grammarCache);
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
    decoder.setEXISchema(grammarCache);
    decoder.setInputStream(new ByteArrayInputStream(bts));
    scanner = decoder.processHeader();
    Assert.assertNull(scanner.getHeaderOptions());
    
    ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();
    
    EXIEvent exiEvent;
    n_events = 0;
    while ((exiEvent = scanner.nextEvent()) != null) {
      ++n_events;
      exiEventList.add(exiEvent);
    }
    
    Assert.assertEquals(9, n_events);

    EventType eventType;
    EventTypeList eventTypeList;

    exiEvent = exiEventList.get(0);
    Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertNull(eventTypeList.getEE());
    
    exiEvent = exiEventList.get(1);
    Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
    Assert.assertEquals("header", exiEvent.getName());
    Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
    Assert.assertEquals("header", eventType.getName());
    Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, eventType.getURI());
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertEquals(2, eventTypeList.getLength());
    eventType = eventTypeList.item(1);
    Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
    
    exiEvent = exiEventList.get(2);
    Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
    Assert.assertEquals("common", exiEvent.getName());
    Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
    Assert.assertEquals("common", eventType.getName());
    Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, eventType.getURI());
    Assert.assertEquals(1, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertEquals(4, eventTypeList.getLength());
    eventType = eventTypeList.item(0);
    Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
    Assert.assertEquals("lesscommon", eventType.getName());
    Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, eventType.getURI());
    eventType = eventTypeList.item(2);
    Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
    Assert.assertEquals("strict", eventType.getName());
    Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, eventType.getURI());
    eventType = eventTypeList.item(3);
    Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
    
    exiEvent = exiEventList.get(3);
    Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
    Assert.assertEquals("schemaId", exiEvent.getName());
    Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
    Assert.assertEquals("schemaId", eventType.getName());
    Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, eventType.getURI());
    Assert.assertEquals(2, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertEquals(4, eventTypeList.getLength());
    eventType = eventTypeList.item(0);
    Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
    Assert.assertEquals("compression", eventType.getName());
    Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, eventType.getURI());
    eventType = eventTypeList.item(1);
    Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
    Assert.assertEquals("fragment", eventType.getName());
    Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, eventType.getURI());
    eventType = eventTypeList.item(3);
    Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);

    exiEvent = exiEventList.get(4);
    Assert.assertEquals(EXIEvent.EVENT_NL, exiEvent.getEventVariety());
    Assert.assertEquals("nil", exiEvent.getName());
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
    Assert.assertTrue(((EXIEventSchemaNil)exiEvent).isNilled());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
    Assert.assertEquals("nil", eventType.getName());
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.getURI());
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertEquals(2, eventTypeList.getLength());
    eventType = eventTypeList.item(1);
    Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);

    exiEvent = exiEventList.get(5);
    Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
    eventType = exiEvent.getEventType();
    eventTypeList = eventType.getEventTypeList();
    Assert.assertEquals(1, eventTypeList.getLength());

    exiEvent = exiEventList.get(6);
    Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertEquals(1, eventTypeList.getLength());

    exiEvent = exiEventList.get(7);
    Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
    Assert.assertEquals(1, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertEquals(2, eventTypeList.getLength());
    eventType = eventTypeList.item(0);
    Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
    Assert.assertEquals("strict", eventType.getName());
    Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, eventType.getURI());

    exiEvent = exiEventList.get(8);
    Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
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
      encoder.setEXISchema(grammarCache);
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
      decoder.setEXISchema(grammarCache);
      // DO NOT SET AlignmentType for decoder.
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();
      
      EXIEvent exiEvent;
      n_events = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        exiEventList.add(exiEvent);
      }
      
      Assert.assertEquals(6, n_events);
  
      EventType eventType;
  
      exiEvent = exiEventList.get(0);
      Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
      
      exiEvent = exiEventList.get(1);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("header", exiEvent.getName());
      Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
      
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("strict", exiEvent.getName());
      Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
      
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
  
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
    }
  }

  /**
   */
  public void testCompressionOption_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/optionsSchema.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    
    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier encoder = new Transmogrifier();
    encoder.setEXISchema(grammarCache);
    encoder.setAlignmentType(AlignmentType.compress);
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
    decoder.setEXISchema(grammarCache);
    decoder.setAlignmentType(AlignmentType.bitPacked); // try to confuse decoder.
    decoder.setInputStream(new ByteArrayInputStream(bts));
    scanner = decoder.processHeader();
    
    ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();
    
    EXIEvent exiEvent;
    n_events = 0;
    while ((exiEvent = scanner.nextEvent()) != null) {
      ++n_events;
      exiEventList.add(exiEvent);
    }
    
    Assert.assertEquals(6, n_events);

    EventType eventType;

    exiEvent = exiEventList.get(0);
    Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
    
    exiEvent = exiEventList.get(1);
    Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
    Assert.assertEquals("header", exiEvent.getName());
    Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
    
    exiEvent = exiEventList.get(2);
    Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
    Assert.assertEquals("strict", exiEvent.getName());
    Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
    
    exiEvent = exiEventList.get(3);
    Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());

    exiEvent = exiEventList.get(4);
    Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());

    exiEvent = exiEventList.get(5);
    Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
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
      encoder.setEXISchema(encodeGrammarCache);
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
      decoder.setEXISchema(decodeGrammarCache);
      // DO NOT SET AlignmentType for decoder.
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();
      
      EXIEvent exiEvent;
      n_events = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        exiEventList.add(exiEvent);
      }
      
      Assert.assertEquals(6, n_events);
  
      EventType eventType;
  
      exiEvent = exiEventList.get(0);
      Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
      
      exiEvent = exiEventList.get(1);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("header", exiEvent.getName());
      Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
      
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("strict", exiEvent.getName());
      Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
      
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
  
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
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
      encoder.setEXISchema(encodeGrammarCache);
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
      decoder.setEXISchema(decodeGrammarCache);
      // DO NOT SET AlignmentType for decoder.
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();
      
      EXIEvent exiEvent;
      n_events = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        exiEventList.add(exiEvent);
      }
      
      Assert.assertEquals(8, n_events);
  
      EventType eventType;
  
      exiEvent = exiEventList.get(0);
      Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
      
      exiEvent = exiEventList.get(1);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("header", exiEvent.getName());
      Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
      
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_CM, exiEvent.getEventVariety());
      Assert.assertEquals(" A ", exiEvent.getCharacters().makeString());

      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("strict", exiEvent.getName());
      Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
      
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());

      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_CM, exiEvent.getEventVariety());
      Assert.assertEquals(" B ", exiEvent.getCharacters().makeString());

      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
  
      exiEvent = exiEventList.get(7);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
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
      encoder.setEXISchema(encodeGrammarCache);
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
      decoder.setEXISchema(decodeGrammarCache);
      // DO NOT SET AlignmentType for decoder.
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();
      
      EXIEvent exiEvent;
      n_events = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        exiEventList.add(exiEvent);
      }
      
      Assert.assertEquals(8, n_events);
  
      EventType eventType;
  
      exiEvent = exiEventList.get(0);
      Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
      
      exiEvent = exiEventList.get(1);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("header", exiEvent.getName());
      Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
      
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_PI, exiEvent.getEventVariety());
      Assert.assertEquals("Good! ", exiEvent.getCharacters().makeString());

      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("strict", exiEvent.getName());
      Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
      
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());

      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_PI, exiEvent.getEventVariety());
      Assert.assertEquals("Good? ", exiEvent.getCharacters().makeString());

      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
  
      exiEvent = exiEventList.get(7);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
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
      encoder.setEXISchema(encodeGrammarCache);
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
      decoder.setEXISchema(decodeGrammarCache);
      // DO NOT SET AlignmentType for decoder.
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();
      
      EXIEvent exiEvent;
      n_events = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        exiEventList.add(exiEvent);
      }
      
      Assert.assertEquals(8, n_events);
  
      EventType eventType;
  
      exiEvent = exiEventList.get(0);
      Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
      
      exiEvent = exiEventList.get(1);
      Assert.assertEquals(EXIEvent.EVENT_DTD, exiEvent.getEventVariety());

      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("header", exiEvent.getName());
      Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
      
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_ER, exiEvent.getEventVariety());
      Assert.assertEquals("ent", exiEvent.getName());

      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("strict", exiEvent.getName());
      Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
      
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());

      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
  
      exiEvent = exiEventList.get(7);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
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
      encoder.setEXISchema(encodeGrammarCache);
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
      decoder.setEXISchema(decodeGrammarCache);
      // DO NOT SET AlignmentType for decoder.
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();
      
      EXIEvent exiEvent;
      n_events = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        exiEventList.add(exiEvent);
      }
      
      Assert.assertEquals(7, n_events);
  
      EventType eventType;
  
      exiEvent = exiEventList.get(0);
      Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
      
      exiEvent = exiEventList.get(1);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("header", exiEvent.getName());
      Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
      Assert.assertNull(exiEvent.getPrefix());
      
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_NS, exiEvent.getEventVariety());
      Assert.assertEquals("exi", exiEvent.getPrefix());
      Assert.assertEquals("http://www.w3.org/2009/exi", exiEvent.getURI());
      Assert.assertTrue(((EXIEventNS)exiEvent).getLocalElementNs());
      
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("strict", exiEvent.getName());
      Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
      Assert.assertEquals("exi", exiEvent.getPrefix());
      
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());

      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
  
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
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
      encoder.setEXISchema(encodeGrammarCache, new SchemaId("aiueo"));
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
      decoder.setEXISchema(decodeGrammarCache);
      // DO NOT SET AlignmentType for decoder.
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      headerOptions = scanner.getHeaderOptions();
      Assert.assertEquals("aiueo", headerOptions.getSchemaId().getValue());
      
      ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();
      
      EXIEvent exiEvent;
      n_events = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        exiEventList.add(exiEvent);
      }
      
      Assert.assertEquals(6, n_events);
  
      EventType eventType;
  
      exiEvent = exiEventList.get(0);
      Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
      
      exiEvent = exiEventList.get(1);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("header", exiEvent.getName());
      Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
      
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("strict", exiEvent.getName());
      Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
      
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
  
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
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
      encoder.setEXISchema(encodeGrammarCache, new SchemaId("aiueo"));
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
      decoder.setEXISchema(decodeGrammarCache);
      // DO NOT SET AlignmentType for decoder.
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      headerOptions = scanner.getHeaderOptions();
      Assert.assertNull(headerOptions.getSchemaId());
      
      ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();
      
      EXIEvent exiEvent;
      n_events = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        exiEventList.add(exiEvent);
      }
      
      Assert.assertEquals(6, n_events);
  
      EventType eventType;
  
      exiEvent = exiEventList.get(0);
      Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
      
      exiEvent = exiEventList.get(1);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("header", exiEvent.getName());
      Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
      
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("strict", exiEvent.getName());
      Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
      
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
  
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
    }
  }

  /**
   * Let the decoder fetch a schema via EXISchemaResolver.
   */
  public void testSchemaIdOption_03() throws Exception {
    final EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/optionsSchema.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    
    GrammarCache encodeGrammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      encoder.setEXISchema(encodeGrammarCache, new SchemaId("aiueo"));
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
          public EXISchema resolveSchema(String schemaId) {
            if ("aiueo".equals(schemaId)) {
              return corpus;
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
      
      ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();
      
      EXIEvent exiEvent;
      n_events = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        exiEventList.add(exiEvent);
      }
      
      Assert.assertEquals(6, n_events);
  
      EventType eventType;
  
      exiEvent = exiEventList.get(0);
      Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
      
      exiEvent = exiEventList.get(1);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("header", exiEvent.getName());
      Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
      
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("strict", exiEvent.getName());
      Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
      
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
  
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
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

      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      EXIDecoder decoder = new EXIDecoder();
      decoder.setInputStream(new ByteArrayInputStream(bts));
      Scanner scanner = decoder.processHeader();
      
      ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();
      
      EXIEvent exiEvent;
      n_events = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        exiEventList.add(exiEvent);
      }
      
      Assert.assertEquals(6, n_events);
  
      EventType eventType;
      EventTypeList eventTypeList;
  
      exiEvent = exiEventList.get(0);
      Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      
      exiEvent = exiEventList.get(1);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("None", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_CM, exiEvent.getEventVariety());
      Assert.assertEquals(" abc ", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_CM, eventType.itemType);
      
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_CM, exiEvent.getEventVariety());
      Assert.assertEquals(" def ", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_CM, eventType.itemType);
      
      exiEvent = exiEventList.get(4);
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
    }
  }

}
