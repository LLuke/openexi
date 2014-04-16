package org.openexi.sax;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.ArrayList;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.LocatorImpl;

import javax.xml.parsers.SAXParserFactory;

import junit.framework.Assert;

import org.openexi.proc.EXIDecoder;
import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.BinaryDataSource;
import org.openexi.proc.common.BinaryDataUtil;
import org.openexi.proc.common.EventDescription;
import org.openexi.proc.common.EventCode;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventTypeList;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.events.EXIEventNS;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.proc.io.BinaryDataSink;
import org.openexi.proc.io.Scanner;
import org.openexi.proc.io.ScriberRuntimeException;
import org.openexi.schema.Base64;
import org.openexi.schema.EXISchema;
import org.openexi.schema.TestBase;
import org.openexi.scomp.EXISchemaFactoryErrorMonitor;
import org.openexi.scomp.EXISchemaFactoryTestUtil;
import org.openexi.scomp.EXISchemaFactoryTestUtilContext;

public class SAXTransmogrifierTest extends TestBase {
  
  public SAXTransmogrifierTest(String name) {
    super(name);
    m_saxParserFactory = SAXParserFactory.newInstance();
    m_saxParserFactory.setNamespaceAware(true);
    m_stringBuilder = new StringBuilder();
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
  private SAXParserFactory m_saxParserFactory;
  
  private StringBuilder m_stringBuilder;
  
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
   * Schema:
   * <xsd:element name="A">
   *   <xsd:complexType>
   *     <xsd:sequence>
   *       <xsd:sequence>
   *         <xsd:element ref="foo:AB"/>
   *         <xsd:element ref="foo:AC" minOccurs="0" maxOccurs="2"/>
   *       </xsd:sequence>
   *       <xsd:element ref="foo:AD"/>
   *       <xsd:element ref="foo:AE" minOccurs="0"/>
   *     </xsd:sequence>
   *   </xsd:complexType>
   * </xsd:element>
   * 
   * Instance:
   * <A>
   *   <AB/><AC/><AC/><AD/><AE/>
   * </A>
   */
  public void testStrict_01() throws Exception {
    EXISchema corpus;
    try {
      corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), 
          new EXISchemaFactoryTestUtilContext(m_compilerErrors, m_stringBuilder));
    }
    finally {
      //System.out.println(m_stringBuilder.toString());
    }
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    String xmlString;
    byte[] bts;
    int n_events;
    
    xmlString = 
      "<A xmlns='urn:foo'>\n" +
      "  <AB/><AC/><AC/><AD/><AE/>\n" +
      "</A>\n";

    Transmogrifier encoder = new Transmogrifier();
    encoder.setGrammarCache(grammarCache);

    for (AlignmentType alignment : Alignments) {
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      final SAXTransmogrifier saxTransmogrifier;

      XMLReader xmlReader = m_saxParserFactory.newSAXParser().getXMLReader();
      saxTransmogrifier = encoder.getSAXTransmogrifier();
      xmlReader.setContentHandler(saxTransmogrifier);
      
      Assert.assertSame(grammarCache, saxTransmogrifier.getGrammarCache());
      
      xmlReader.parse(new InputSource(new StringReader(xmlString)));

      bts = baos.toByteArray();
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EventDescription exiEvent;
      n_events = 0;
      
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
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("AB", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("AB", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex()); // because of xsi:type
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("AC", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("AC", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("AC", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("AC", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("AD", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("AD", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("AE", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("AE", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getEE().getIndex());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      ++n_events;

      Assert.assertEquals(19, n_events);
    }

    xmlString = 
      "<A xmlns='urn:foo'>\n" +
      "  <AB/><AC/><AC/><AC/>\n" + // The 3rd <AC/> is not expected.
      "</A>\n";

    for (AlignmentType alignment : Alignments) {
      
      encoder.setAlignmentType(alignment);

      boolean caught;
  
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      XMLReader xmlReader = m_saxParserFactory.newSAXParser().getXMLReader();
      xmlReader.setContentHandler(encoder.getSAXTransmogrifier());
      
      caught = false;
      try {
        xmlReader.parse(new InputSource(new StringReader(xmlString)));
      }
      catch (SAXException se) {
        caught = true;
        TransmogrifierException te = (TransmogrifierException)se.getException();
        Assert.assertEquals(TransmogrifierException.UNEXPECTED_ELEM, te.getCode());
      }
      Assert.assertTrue(caught);
    }
  }

  /**
   * Exercise CM and PI in "all" group.
   * 
   * Schema:
   * <xsd:element name="C">
   *   <xsd:complexType>
   *     <xsd:all>
   *       <xsd:element ref="foo:AB" minOccurs="0" />
   *       <xsd:element ref="foo:AC" minOccurs="0" />
   *     </xsd:all>
   *   </xsd:complexType>
   * </xsd:element>
   *
   * Instance:
   * <C><AC/><!-- Good? --><?eg Good! ?></C><?eg Good? ?><!-- Good! -->
   */
  public void testLexicalHandler_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    short options = GrammarOptions.DEFAULT_OPTIONS; 
    options = GrammarOptions.addCM(options);
    options = GrammarOptions.addPI(options);
    
    GrammarCache grammarCache = new GrammarCache(corpus, options);
    
    final String xmlString = 
      "<C xmlns='urn:foo'><AC/><!-- Good? --><?eg Good! ?></C><?eg Good? ?><!-- Good! -->";

    Transmogrifier encoder = new Transmogrifier();
    encoder.setGrammarCache(grammarCache);

    for (AlignmentType alignment : Alignments) {
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
  
      XMLReader xmlReader = m_saxParserFactory.newSAXParser().getXMLReader();
      xmlReader.setContentHandler(encoder.getSAXTransmogrifier());
      xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", encoder.getSAXTransmogrifier());
      
      xmlReader.parse(new InputSource(new StringReader(xmlString)));

      byte[] bts = baos.toByteArray();
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      ArrayList<EventDescription> exiEventList = new ArrayList<EventDescription>();
      
      EventDescription exiEvent;
      int n_events = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        exiEventList.add(exiEvent);
      }
      
      Assert.assertEquals(10, n_events);
  
      EventType eventType;
      EventTypeList eventTypeList;
  
      exiEvent = exiEventList.get(0);
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
  
      exiEvent = exiEventList.get(1);
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("C", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
  
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("AC", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("AC", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(11, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("AB", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventType.ITEM_CM, eventType.itemType);
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventType.ITEM_PI, eventType.itemType);
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventType.ITEM_CM, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventType.ITEM_PI, eventType.itemType);
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EventDescription.EVENT_CM, exiEvent.getEventKind());
      Assert.assertEquals(" Good? ", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_CM, eventType.itemType);
      Assert.assertEquals(5, eventType.getIndex());
      // make sure this ITEM_CM belongs to AllGroupGrammar
      // This assertion is not relevant in Nagasena
      //Assert.assertEquals(EXISchema.GROUP_ALL, ((GroupGrammar)EventTypeAccessor.getGrammar(eventType)).getCompositor()); 
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(7, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("AB", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("AC", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_PI, eventType.itemType);
  
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EventDescription.EVENT_PI, exiEvent.getEventKind());
      Assert.assertEquals("eg", exiEvent.getName());
      Assert.assertEquals("Good! ", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_PI, eventType.itemType);
      Assert.assertEquals(6, eventType.getIndex());
      // make sure this ITEM_CM belongs to AllGroupGrammar
      // This assertion is not relevant in Nagasena
      //Assert.assertEquals(EXISchema.GROUP_ALL, ((GroupGrammar)EventTypeAccessor.getGrammar(eventType)).getCompositor()); 
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(7, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("AB", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("AC", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_CM, eventType.itemType);
  
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(7, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("AB", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("AC", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_CM, eventType.itemType);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_PI, eventType.itemType);
  
      exiEvent = exiEventList.get(7);
      Assert.assertEquals(EventDescription.EVENT_PI, exiEvent.getEventKind());
      Assert.assertEquals("eg", exiEvent.getName());
      Assert.assertEquals("Good? ", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_PI, eventType.itemType);
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_CM, eventType.itemType);
  
      exiEvent = exiEventList.get(8);
      Assert.assertEquals(EventDescription.EVENT_CM, exiEvent.getEventKind());
      Assert.assertEquals(" Good! ", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_CM, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_PI, eventType.itemType);
  
      exiEvent = exiEventList.get(9);
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_CM, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_PI, eventType.itemType);
    }
  }
  
  /**
   * Tests BinaryDataHandler methods.
   */
  public void testBinaryData_01a() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/base64Binary.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    final String textValue = "QUJDREVGR0hJSg==";
    
    final byte[] octets = new byte[64];
    final int n_octets = Base64.decode(textValue, octets);
    
    Transmogrifier encoder = new Transmogrifier();
    encoder.setGrammarCache(grammarCache);

    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);

    for (AlignmentType alignment : Alignments) {
      for (boolean preserveLexicalValues : new boolean[] { true, false }) {
        for (boolean binaryDataEnabled : new boolean[] { true, false }) {
          decoder.setEnableBinaryData(binaryDataEnabled);
          Scanner scanner;
          
          encoder.setAlignmentType(alignment);
          decoder.setAlignmentType(alignment);
    
          encoder.setPreserveLexicalValues(preserveLexicalValues);
          decoder.setPreserveLexicalValues(preserveLexicalValues);
    
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          encoder.setOutputStream(baos);
    
          SAXTransmogrifier saxTransmogrifier = encoder.getSAXTransmogrifier();
          BinaryDataSink binaryDataSink;

          saxTransmogrifier.setDocumentLocator(new LocatorImpl());
          saxTransmogrifier.startDocument();
          saxTransmogrifier.startPrefixMapping("foo", "urn:foo");
          saxTransmogrifier.startElement("urn:foo", "Base64Binary", "foo:Base64Binary", new AttributesImpl());
          try {
            binaryDataSink = saxTransmogrifier.startBinaryData(n_octets);
          }
          catch (SAXException se) {
            TransmogrifierException te = (TransmogrifierException)se.getException();
            Assert.assertEquals(TransmogrifierException.UNEXPECTED_BINARY_VALUE, te.getCode());
            Assert.assertTrue(preserveLexicalValues);
            continue;
          }
          saxTransmogrifier.binaryData(octets, 0, n_octets, binaryDataSink);
          saxTransmogrifier.endBinaryData(binaryDataSink);
          Assert.assertFalse(preserveLexicalValues);
          saxTransmogrifier.endElement("urn:foo", "Base64Binary", "foo:Base64Binary");
          saxTransmogrifier.endPrefixMapping("foo");
          saxTransmogrifier.endDocument();
          
          final byte[] bts = baos.toByteArray();
          decoder.setInputStream(new ByteArrayInputStream(bts));
    
          scanner = decoder.processHeader();
          EventDescription event;
          int n_events = 0;
          byte[] decodedBytes = null;
          String decodedText = null;
          while ((event = scanner.nextEvent()) != null) {
            final byte eventKind = event.getEventKind();
            if (EventDescription.EVENT_BLOB == eventKind) {
              Assert.assertTrue(binaryDataEnabled);
              decodedBytes = BinaryDataUtil.makeBytes(event.getBinaryDataSource());
            }
            else if (EventDescription.EVENT_CH == eventKind) {
              Assert.assertTrue(!binaryDataEnabled || preserveLexicalValues);
              decodedText = event.getCharacters().makeString();
            }
            ++n_events;
          }
          Assert.assertEquals(5, n_events);
          if (binaryDataEnabled && !preserveLexicalValues) {
            Assert.assertEquals(n_octets, decodedBytes.length);
            for (int i = 0; i < n_octets; i++) {
              Assert.assertEquals(octets[i], decodedBytes[i]);
            }
          }
          else {
            Assert.assertEquals(textValue, decodedText);
          }
        }
      }
    }
  }

  /**
   * Call BinaryDataHandler's API sequence startBinaryData, binaryData and endBinaryData *twice*. 
   */
  public void testBinaryData_01b() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/base64Binary.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    final byte[] octets1, octets2;
    
    final String textValue1 = "QUJDREVGR0hJSg==";
    final int n_octets1 = Base64.decode(textValue1, octets1 = new byte[64]);

    final String textValue2 = "aG9nZXBpeW9mb29iYXI=";
    final int n_octets2 = Base64.decode(textValue2, octets2 = new byte[64]);
    
    Transmogrifier encoder = new Transmogrifier();
    encoder.setGrammarCache(grammarCache);

    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);

    for (AlignmentType alignment : Alignments) {
      for (boolean preserveLexicalValues : new boolean[] { true, false }) {
        for (boolean binaryDataEnabled : new boolean[] { true, false }) {
          decoder.setEnableBinaryData(binaryDataEnabled);
          Scanner scanner;
          
          encoder.setAlignmentType(alignment);
          decoder.setAlignmentType(alignment);
    
          encoder.setPreserveLexicalValues(preserveLexicalValues);
          decoder.setPreserveLexicalValues(preserveLexicalValues);
    
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          encoder.setOutputStream(baos);
    
          SAXTransmogrifier saxTransmogrifier = encoder.getSAXTransmogrifier();
          BinaryDataSink binaryDataSink;

          saxTransmogrifier.setDocumentLocator(new LocatorImpl());
          saxTransmogrifier.startDocument();
          saxTransmogrifier.startPrefixMapping("foo", "urn:foo");
          saxTransmogrifier.startElement("urn:foo", "A", "foo:A", new AttributesImpl());
          saxTransmogrifier.startElement("urn:foo", "Base64Binary", "foo:Base64Binary", new AttributesImpl());
          try {
            binaryDataSink = saxTransmogrifier.startBinaryData(n_octets1);
          }
          catch (SAXException se) {
            TransmogrifierException te = (TransmogrifierException)se.getException();
            Assert.assertEquals(TransmogrifierException.UNEXPECTED_BINARY_VALUE, te.getCode());
            Assert.assertTrue(preserveLexicalValues);
            continue;
          }
          saxTransmogrifier.binaryData(octets1, 0, n_octets1, binaryDataSink);
          saxTransmogrifier.endBinaryData(binaryDataSink);
          Assert.assertFalse(preserveLexicalValues);
          saxTransmogrifier.endElement("urn:foo", "Base64Binary", "foo:Base64Binary");
          saxTransmogrifier.startElement("urn:foo", "Base64Binary", "foo:Base64Binary", new AttributesImpl());
          binaryDataSink = saxTransmogrifier.startBinaryData(n_octets2);
          saxTransmogrifier.binaryData(octets2, 0, n_octets2, binaryDataSink);
          saxTransmogrifier.endBinaryData(binaryDataSink);
          saxTransmogrifier.endElement("urn:foo", "Base64Binary", "foo:Base64Binary");
          saxTransmogrifier.endElement("urn:foo", "A", "foo:A");
          saxTransmogrifier.endPrefixMapping("foo");
          saxTransmogrifier.endDocument();
          
          final byte[] bts = baos.toByteArray();
          decoder.setInputStream(new ByteArrayInputStream(bts));
    
          scanner = decoder.processHeader();
          EventDescription event;
          int n_events = 0;
          byte[] decodedBytes1, decodedBytes2;
          decodedBytes1 = decodedBytes2 = null;
          String decodedText1, decodedText2;
          decodedText1 = decodedText2 = null;
          while ((event = scanner.nextEvent()) != null) {
            final byte eventKind = event.getEventKind();
            if (EventDescription.EVENT_BLOB == eventKind) {
              Assert.assertTrue(binaryDataEnabled);
              if (decodedBytes1 == null)
                decodedBytes1 = BinaryDataUtil.makeBytes(event.getBinaryDataSource());
              else
                decodedBytes2 = BinaryDataUtil.makeBytes(event.getBinaryDataSource());
            }
            else if (EventDescription.EVENT_CH == eventKind) {
              Assert.assertTrue(!binaryDataEnabled || preserveLexicalValues);
              if (decodedText1 == null)
                decodedText1 = event.getCharacters().makeString();
              else
                decodedText2 = event.getCharacters().makeString();
            }
            ++n_events;
          }
          Assert.assertEquals(10, n_events);
          if (binaryDataEnabled && !preserveLexicalValues) {
            Assert.assertEquals(n_octets1, decodedBytes1.length);
            for (int i = 0; i < n_octets1; i++) {
              Assert.assertEquals(octets1[i], decodedBytes1[i]);
            }
            Assert.assertEquals(n_octets2, decodedBytes2.length);
            for (int i = 0; i < n_octets2; i++) {
              Assert.assertEquals(octets2[i], decodedBytes2[i]);
            }
          }
          else {
            Assert.assertEquals(textValue1, decodedText1);
            Assert.assertEquals(textValue2, decodedText2);
          }
        }
      }
    }
  }
  
  /**
   * Decode binary values in chunks. 
   */
  public void testBinaryData_01c() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/base64Binary.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    final String textValue =
        "RHIuIFN1ZSBDbGFyayBpcyBjdXJyZW50bHkgYSBSZWdlbnRzIFByb2Zlc3NvciBvZiBDaGVtaXN0\n" +
        "cnkgYXQgV2FzaGluZ3RvbiBTdGF0ZSBVbml2ZXJzaXR5IGluIFB1bGxtYW4sIFdBLCB3aGVyZSBz\n" +
        "aGUgaGFzIHRhdWdodCBhbmQgY29uZHVjdGVkIHJlc2VhcmNoIGluIGFjdGluaWRlIGVudmlyb25t\n" +
        "ZW50YWwgY2hlbWlzdHJ5IGFuZCByYWRpb2FuYWx5dGljYWwgY2hlbWlzdHJ5IHNpbmNlIDE5OTYu\n" +
        "ICBGcm9tIDE5OTIgdG8gMTk5NiwgRHIuIENsYXJrIHdhcyBhIFJlc2VhcmNoIEVjb2xvZ2lzdCBh\n" +
        "dCB0aGUgVW5pdmVyc2l0eSBvZiBHZW9yZ2lh4oCZcyBTYXZhbm5haCBSaXZlciBFY29sb2d5IExh\n" +
        "Ym9yYXRvcnkuICBQcmlvciB0byBoZXIgcG9zaXRpb24gYXQgdGhlIFVuaXZlcnNpdHkgb2YgR2Vv\n" +
        "cmdpYSwgc2hlIHdhcyBhIFNlbmlvciBTY2llbnRpc3QgYXQgdGhlIFdlc3Rpbmdob3VzZSBTYXZh\n" +
        "bm5haCBSaXZlciBDb21wYW554oCZcyBTYXZhbm5haCBSaXZlciBUZWNobm9sb2d5IENlbnRlci4g\n" +
        "IERyLiBDbGFyayBoYXMgc2VydmVkIG9uIHZhcmlvdXMgYm9hcmRzIGFuZCBhZHZpc29yeSBjb21t\n" +
        "aXR0ZWVzLCBpbmNsdWRpbmcgdGhlIE5hdGlvbmFsIEFjYWRlbWllcyBOdWNsZWFyIGFuZCBSYWRp\n" +
        "YXRpb24gU3R1ZGllcyBCb2FyZCBhbmQgdGhlIERlcGFydG1lbnQgb2YgRW5lcmd54oCZcyBCYXNp\n" +
        "YyBFbmVyZ3kgU2NpZW5jZXMgQWR2aXNvcnkgQ29tbWl0dGVlLiAgRHIuIENsYXJrIGhvbGRzIGEg\n" +
        "UGguRC4gYW5kIE0uUy4gaW4gSW5vcmdhbmljL1JhZGlvY2hlbWlzdHJ5IGZyb20gRmxvcmlkYSBT\n" +
        "dGF0ZSBVbml2ZXJzaXR5IGFuZCBhIEIuUy4gaW4gQ2hlbWlzdHJ5IGZyb20gTGFuZGVyIENvbGxl\n" +
        "Z2UuDQo=";
    
    final byte[] octets = new byte[1024];
    final int n_octets = Base64.decode(textValue, octets);
    
    Transmogrifier encoder = new Transmogrifier();
    encoder.setGrammarCache(grammarCache);

    for (AlignmentType alignment : Alignments) {
      EXIDecoder decoder = new EXIDecoder();
      decoder.setGrammarCache(grammarCache);

      decoder.setEnableBinaryData(true);
      decoder.setInitialBinaryDataBufferSize(64);
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      SAXTransmogrifier saxTransmogrifier = encoder.getSAXTransmogrifier();
      BinaryDataSink binaryDataSink;

      saxTransmogrifier.setDocumentLocator(new LocatorImpl());
      saxTransmogrifier.startDocument();
      saxTransmogrifier.startPrefixMapping("foo", "urn:foo");
      saxTransmogrifier.startElement("urn:foo", "Base64Binary", "foo:Base64Binary", new AttributesImpl());
      binaryDataSink = saxTransmogrifier.startBinaryData(n_octets);
      saxTransmogrifier.binaryData(octets, 0, n_octets, binaryDataSink);
      saxTransmogrifier.endBinaryData(binaryDataSink);
      saxTransmogrifier.endElement("urn:foo", "Base64Binary", "foo:Base64Binary");
      saxTransmogrifier.endPrefixMapping("foo");
      saxTransmogrifier.endDocument();
      
      final byte[] bts = baos.toByteArray();
      decoder.setInputStream(new ByteArrayInputStream(bts));

      ByteArrayOutputStream octetStream = new ByteArrayOutputStream();
      
      scanner = decoder.processHeader();
      try {
        scanner.setBinaryChunkSize(100);
      }
      catch (UnsupportedOperationException uoe) {
        Assert.assertTrue(alignment == AlignmentType.compress || alignment == AlignmentType.preCompress);
      }
      EventDescription event;
      int n_events = 0;
      byte[] decodedBytes = null;
      while ((event = scanner.nextEvent()) != null) {
        final byte eventKind = event.getEventKind();
        if (EventDescription.EVENT_BLOB == eventKind) {
          int n_chunks = 0;
          BinaryDataSource binaryData = event.getBinaryDataSource();
          octetStream.write(binaryData.getByteArray(), binaryData.getStartIndex(), binaryData.getLength());
          ++n_chunks;
          do {
            if (!binaryData.hasNext())
              break;
            binaryData.next();
            octetStream.write(binaryData.getByteArray(), binaryData.getStartIndex(), binaryData.getLength());
            ++n_chunks;
          } 
          while (true);
          if (alignment == AlignmentType.compress || alignment == AlignmentType.preCompress) {
            Assert.assertEquals(1, n_chunks);
            Assert.assertTrue(n_octets < binaryData.getByteArray().length && 2* n_octets > binaryData.getByteArray().length);
          }
          else {
            Assert.assertEquals(9, n_chunks);
            Assert.assertEquals(128, binaryData.getByteArray().length);
          }
          decodedBytes = octetStream.toByteArray();
        }
        else if (EventDescription.EVENT_CH == eventKind) {
          Assert.fail();
        }
        ++n_events;
      }
      Assert.assertEquals(5, n_events);
      Assert.assertEquals(n_octets, decodedBytes.length);
      for (int i = 0; i < n_octets; i++) {
        Assert.assertEquals(octets[i], decodedBytes[i]);
      }
    }
  }

  /**
   * Call binaryData method successively in a row.
   */
  public void testBinaryData_01d() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/base64Binary.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    final String textValue =
        "RHIuIFN1ZSBDbGFyayBpcyBjdXJyZW50bHkgYSBSZWdlbnRzIFByb2Zlc3NvciBvZiBDaGVtaXN0\n" +
        "cnkgYXQgV2FzaGluZ3RvbiBTdGF0ZSBVbml2ZXJzaXR5IGluIFB1bGxtYW4sIFdBLCB3aGVyZSBz\n" +
        "aGUgaGFzIHRhdWdodCBhbmQgY29uZHVjdGVkIHJlc2VhcmNoIGluIGFjdGluaWRlIGVudmlyb25t\n" +
        "ZW50YWwgY2hlbWlzdHJ5IGFuZCByYWRpb2FuYWx5dGljYWwgY2hlbWlzdHJ5IHNpbmNlIDE5OTYu\n" +
        "ICBGcm9tIDE5OTIgdG8gMTk5NiwgRHIuIENsYXJrIHdhcyBhIFJlc2VhcmNoIEVjb2xvZ2lzdCBh\n" +
        "dCB0aGUgVW5pdmVyc2l0eSBvZiBHZW9yZ2lh4oCZcyBTYXZhbm5haCBSaXZlciBFY29sb2d5IExh\n" +
        "Ym9yYXRvcnkuICBQcmlvciB0byBoZXIgcG9zaXRpb24gYXQgdGhlIFVuaXZlcnNpdHkgb2YgR2Vv\n" +
        "cmdpYSwgc2hlIHdhcyBhIFNlbmlvciBTY2llbnRpc3QgYXQgdGhlIFdlc3Rpbmdob3VzZSBTYXZh\n" +
        "bm5haCBSaXZlciBDb21wYW554oCZcyBTYXZhbm5haCBSaXZlciBUZWNobm9sb2d5IENlbnRlci4g\n" +
        "IERyLiBDbGFyayBoYXMgc2VydmVkIG9uIHZhcmlvdXMgYm9hcmRzIGFuZCBhZHZpc29yeSBjb21t\n" +
        "aXR0ZWVzLCBpbmNsdWRpbmcgdGhlIE5hdGlvbmFsIEFjYWRlbWllcyBOdWNsZWFyIGFuZCBSYWRp\n" +
        "YXRpb24gU3R1ZGllcyBCb2FyZCBhbmQgdGhlIERlcGFydG1lbnQgb2YgRW5lcmd54oCZcyBCYXNp\n" +
        "YyBFbmVyZ3kgU2NpZW5jZXMgQWR2aXNvcnkgQ29tbWl0dGVlLiAgRHIuIENsYXJrIGhvbGRzIGEg\n" +
        "UGguRC4gYW5kIE0uUy4gaW4gSW5vcmdhbmljL1JhZGlvY2hlbWlzdHJ5IGZyb20gRmxvcmlkYSBT\n" +
        "dGF0ZSBVbml2ZXJzaXR5IGFuZCBhIEIuUy4gaW4gQ2hlbWlzdHJ5IGZyb20gTGFuZGVyIENvbGxl\n" +
        "Z2UuDQo=";
    
    final byte[] octets = new byte[1024];
    final int n_octets = Base64.decode(textValue, octets);

    Transmogrifier encoder = new Transmogrifier();
    encoder.setGrammarCache(grammarCache);

    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    
    for (AlignmentType alignment : Alignments) {
      for (boolean preserveLexicalValues : new boolean[] { true, false }) {
        for (boolean binaryDataEnabled : new boolean[] { true, false }) {
          decoder.setEnableBinaryData(binaryDataEnabled);
          Scanner scanner;
          
          encoder.setAlignmentType(alignment);
          decoder.setAlignmentType(alignment);
    
          encoder.setPreserveLexicalValues(preserveLexicalValues);
          decoder.setPreserveLexicalValues(preserveLexicalValues);
    
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          encoder.setOutputStream(baos);
    
          SAXTransmogrifier saxTransmogrifier = encoder.getSAXTransmogrifier();
          BinaryDataSink binaryDataSink;
          
          saxTransmogrifier.setDocumentLocator(new LocatorImpl());
          saxTransmogrifier.startDocument();
          saxTransmogrifier.startPrefixMapping("foo", "urn:foo");
          saxTransmogrifier.startElement("urn:foo", "Base64Binary", "foo:Base64Binary", new AttributesImpl());
          try {
            binaryDataSink = saxTransmogrifier.startBinaryData(n_octets);
            saxTransmogrifier.binaryData(octets, 0, n_octets - 400, binaryDataSink);
            saxTransmogrifier.binaryData(octets, n_octets - 400, 400, binaryDataSink);
            saxTransmogrifier.endBinaryData(binaryDataSink);
          }
          catch (SAXException se) {
            TransmogrifierException te = (TransmogrifierException)se.getException();
            Assert.assertEquals(TransmogrifierException.UNEXPECTED_BINARY_VALUE, te.getCode());
            Assert.assertTrue(preserveLexicalValues);
            continue;
          }
          Assert.assertFalse(preserveLexicalValues);
          saxTransmogrifier.endElement("urn:foo", "Base64Binary", "foo:Base64Binary");
          saxTransmogrifier.endPrefixMapping("foo");
          saxTransmogrifier.endDocument();
          
          final byte[] bts = baos.toByteArray();
          decoder.setInputStream(new ByteArrayInputStream(bts));
    
          scanner = decoder.processHeader();
          EventDescription event;
          int n_events = 0;
          byte[] decodedBytes = null;
          String decodedText = null;
          while ((event = scanner.nextEvent()) != null) {
            final byte eventKind = event.getEventKind();
            if (EventDescription.EVENT_BLOB == eventKind) {
              Assert.assertTrue(binaryDataEnabled);
              decodedBytes = BinaryDataUtil.makeBytes(event.getBinaryDataSource());
            }
            else if (EventDescription.EVENT_CH == eventKind) {
              Assert.assertTrue(!binaryDataEnabled || preserveLexicalValues);
              decodedText = event.getCharacters().makeString();
            }
            ++n_events;
          }
          Assert.assertEquals(5, n_events);
          if (binaryDataEnabled && !preserveLexicalValues) {
            Assert.assertEquals(n_octets, decodedBytes.length);
            for (int i = 0; i < n_octets; i++) {
              Assert.assertEquals(octets[i], decodedBytes[i]);
            }
          }
          else {
            Assert.assertEquals(textValue, decodedText);
          }
        }
      }
    }
  }
  
  /**
   * Test SAXTransmogrifier's characters(byte[] binaryValue, int offset, int length) method
   * where the method is invoked in a wrong context.
   */
  public void testBinaryData_02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/integer.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    final String textValue = "QUJDREVGR0hJSg==";
    
    final byte[] octets = new byte[64];
    final int n_octets = Base64.decode(textValue, octets);
    
    Transmogrifier encoder = new Transmogrifier();
    encoder.setGrammarCache(grammarCache);

    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    
    for (AlignmentType alignment : Alignments) {
      for (boolean preserveLexicalValues : new boolean[] { true, false }) {
        encoder.setAlignmentType(alignment);
        decoder.setAlignmentType(alignment);
  
        encoder.setPreserveLexicalValues(preserveLexicalValues);
        decoder.setPreserveLexicalValues(preserveLexicalValues);
  
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        encoder.setOutputStream(baos);
  
        SAXTransmogrifier saxTransmogrifier = encoder.getSAXTransmogrifier();

        saxTransmogrifier.setDocumentLocator(new LocatorImpl());
        saxTransmogrifier.startDocument();
        saxTransmogrifier.startPrefixMapping("foo", "urn:foo");
        saxTransmogrifier.startElement("urn:foo", "Integer", "foo:Integer", new AttributesImpl());
        try {
          saxTransmogrifier.startBinaryData(n_octets);
        }
        catch (SAXException se) {
          TransmogrifierException te = (TransmogrifierException)se.getException();
          Assert.assertEquals(TransmogrifierException.UNEXPECTED_BINARY_VALUE, te.getCode());
          continue;
        }
        Assert.fail();
      }
    }
  }
  
  /**
   * Duplicate NS event xmlns:foo="urn:foo" in the same element.
   */
  public void testDuplicateNS_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/verySimpleDefault.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));
    
    byte[] bts;
    int n_events;
    
    Transmogrifier encoder = new Transmogrifier();
    encoder.setGrammarCache(grammarCache);

    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);

    for (AlignmentType alignment : Alignments) {
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      SAXTransmogrifier saxTransmogrifier = encoder.getSAXTransmogrifier();
      
      saxTransmogrifier.setDocumentLocator(new LocatorImpl());
      saxTransmogrifier.startDocument();
      saxTransmogrifier.startPrefixMapping("foo", "urn:foo");
      saxTransmogrifier.startPrefixMapping("foo", "urn:foo");
      saxTransmogrifier.startElement("", "B", "B", new AttributesImpl());
      saxTransmogrifier.characters("xyz".toCharArray(), 0, 3);
      saxTransmogrifier.endElement("", "B", "B");
      saxTransmogrifier.endPrefixMapping("foo");
      saxTransmogrifier.endDocument();
      
      bts = baos.toByteArray();
      
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EventDescription exiEvent;
      n_events = 0;
      
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
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("", eventType.uri);
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
      Assert.assertEquals("foo", ((EXIEventNS)exiEvent).getPrefix());
      Assert.assertEquals("urn:foo", ((EXIEventNS)exiEvent).getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("xyz", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNotNull(eventTypeList.getEE());
      Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
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
   * Duplicate NS event xmlns:foo="" in the same element.
   */
  public void testDuplicateNS_02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/verySimpleDefault.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));
    
    byte[] bts;
    int n_events;
    
    Transmogrifier encoder = new Transmogrifier();
    encoder.setGrammarCache(grammarCache);

    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);

    for (AlignmentType alignment : Alignments) {
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      SAXTransmogrifier saxTransmogrifier = encoder.getSAXTransmogrifier();
      
      saxTransmogrifier.setDocumentLocator(new LocatorImpl());
      saxTransmogrifier.startDocument();
      saxTransmogrifier.startPrefixMapping("foo", "");
      saxTransmogrifier.startPrefixMapping("foo", "");
      saxTransmogrifier.startElement("", "B", "B", new AttributesImpl());
      saxTransmogrifier.characters("xyz".toCharArray(), 0, 3);
      saxTransmogrifier.endElement("", "B", "B");
      saxTransmogrifier.endPrefixMapping("foo");
      saxTransmogrifier.endDocument();
      
      bts = baos.toByteArray();
      
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EventDescription exiEvent;
      n_events = 0;
      
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
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("", eventType.uri);
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
      Assert.assertEquals("foo", ((EXIEventNS)exiEvent).getPrefix());
      Assert.assertEquals("", ((EXIEventNS)exiEvent).getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("xyz", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNotNull(eventTypeList.getEE());
      Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
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
   * Duplicate NS event xmlns="urn:foo" in the same element.
   */
  public void testDuplicateNS_03() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/verySimpleDefault.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));
    
    byte[] bts;
    int n_events;
    
    Transmogrifier encoder = new Transmogrifier();
    encoder.setGrammarCache(grammarCache);

    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);

    for (AlignmentType alignment : Alignments) {
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      SAXTransmogrifier saxTransmogrifier = encoder.getSAXTransmogrifier();
      
      saxTransmogrifier.setDocumentLocator(new LocatorImpl());
      saxTransmogrifier.startDocument();
      saxTransmogrifier.startPrefixMapping("", "urn:foo");
      saxTransmogrifier.startPrefixMapping("", "urn:foo");
      saxTransmogrifier.startElement("", "B", "B", new AttributesImpl());
      saxTransmogrifier.characters("xyz".toCharArray(), 0, 3);
      saxTransmogrifier.endElement("", "B", "B");
      saxTransmogrifier.endPrefixMapping("foo");
      saxTransmogrifier.endDocument();
      
      bts = baos.toByteArray();
      
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EventDescription exiEvent;
      n_events = 0;
      
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
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("", eventType.uri);
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
      Assert.assertEquals("", ((EXIEventNS)exiEvent).getPrefix());
      Assert.assertEquals("urn:foo", ((EXIEventNS)exiEvent).getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("xyz", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNotNull(eventTypeList.getEE());
      Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
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
   * Duplicate NS event xmlns="" in the same element.
   */
  public void testDuplicateNS_04() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/verySimpleDefault.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));
    
    byte[] bts;
    int n_events;
    
    Transmogrifier encoder = new Transmogrifier();
    encoder.setGrammarCache(grammarCache);

    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);

    for (AlignmentType alignment : Alignments) {
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      SAXTransmogrifier saxTransmogrifier = encoder.getSAXTransmogrifier();
      
      saxTransmogrifier.setDocumentLocator(new LocatorImpl());
      saxTransmogrifier.startDocument();
      saxTransmogrifier.startPrefixMapping("", "");
      saxTransmogrifier.startPrefixMapping("", "");
      saxTransmogrifier.startElement("", "B", "B", new AttributesImpl());
      saxTransmogrifier.characters("xyz".toCharArray(), 0, 3);
      saxTransmogrifier.endElement("", "B", "B");
      saxTransmogrifier.endPrefixMapping("foo");
      saxTransmogrifier.endDocument();
      
      bts = baos.toByteArray();
      
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EventDescription exiEvent;
      n_events = 0;
      
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
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("", eventType.uri);
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
      Assert.assertEquals("", ((EXIEventNS)exiEvent).getPrefix());
      Assert.assertEquals("", ((EXIEventNS)exiEvent).getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("xyz", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNotNull(eventTypeList.getEE());
      Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
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
   * Tests BinaryDataHandler's startBinaryData method with manifested total size 
   * greater than Integer.MAX_VALUE.
   */
  public void testBinaryDataLong_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/base64Binary.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    Transmogrifier encoder = new Transmogrifier();
    encoder.setGrammarCache(grammarCache);

    for (AlignmentType alignment : Alignments) {
      encoder.setAlignmentType(alignment);

      encoder.setOutputStream(new ByteArrayOutputStream());

      SAXTransmogrifier saxTransmogrifier = encoder.getSAXTransmogrifier();

      saxTransmogrifier.setDocumentLocator(new LocatorImpl());
      saxTransmogrifier.startDocument();
      saxTransmogrifier.startPrefixMapping("foo", "urn:foo");
      saxTransmogrifier.startElement("urn:foo", "Base64Binary", "foo:Base64Binary", new AttributesImpl());
      try {
        saxTransmogrifier.startBinaryData(((long)Integer.MAX_VALUE) * 10); // big data
      }
      catch (SAXException se) {
        Assert.assertTrue(alignment == AlignmentType.preCompress || alignment == AlignmentType.compress);
        TransmogrifierException te = (TransmogrifierException)se.getException();
        Assert.assertEquals(TransmogrifierException.SCRIBER_ERROR, te.getCode());
        ScriberRuntimeException se2 = (ScriberRuntimeException)te.getException();
        Assert.assertEquals(ScriberRuntimeException.BINARY_DATA_SIZE_TOO_LARGE, se2.getCode());
        continue;
      }
      Assert.assertTrue(alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned);
    }
  }

  /**
   * Test decoding big binary size (> Integer.MAX_VALUE). (Only the size part) 
   */
  public void testBinaryDataLong_02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/base64Binary.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    final String textValue =
        "RHIuIFN1ZSBDbGFyayBpcyBjdXJyZW50bHkgYSBSZWdlbnRzIFByb2Zlc3NvciBvZiBDaGVtaXN0\n" +
        "cnkgYXQgV2FzaGluZ3RvbiBTdGF0ZSBVbml2ZXJzaXR5IGluIFB1bGxtYW4sIFdBLCB3aGVyZSBz\n" +
        "aGUgaGFzIHRhdWdodCBhbmQgY29uZHVjdGVkIHJlc2VhcmNoIGluIGFjdGluaWRlIGVudmlyb25t\n" +
        "ZW50YWwgY2hlbWlzdHJ5IGFuZCByYWRpb2FuYWx5dGljYWwgY2hlbWlzdHJ5IHNpbmNlIDE5OTYu\n" +
        "ICBGcm9tIDE5OTIgdG8gMTk5NiwgRHIuIENsYXJrIHdhcyBhIFJlc2VhcmNoIEVjb2xvZ2lzdCBh\n" +
        "dCB0aGUgVW5pdmVyc2l0eSBvZiBHZW9yZ2lh4oCZcyBTYXZhbm5haCBSaXZlciBFY29sb2d5IExh\n" +
        "Ym9yYXRvcnkuICBQcmlvciB0byBoZXIgcG9zaXRpb24gYXQgdGhlIFVuaXZlcnNpdHkgb2YgR2Vv\n" +
        "cmdpYSwgc2hlIHdhcyBhIFNlbmlvciBTY2llbnRpc3QgYXQgdGhlIFdlc3Rpbmdob3VzZSBTYXZh\n" +
        "bm5haCBSaXZlciBDb21wYW554oCZcyBTYXZhbm5haCBSaXZlciBUZWNobm9sb2d5IENlbnRlci4g\n" +
        "IERyLiBDbGFyayBoYXMgc2VydmVkIG9uIHZhcmlvdXMgYm9hcmRzIGFuZCBhZHZpc29yeSBjb21t\n" +
        "aXR0ZWVzLCBpbmNsdWRpbmcgdGhlIE5hdGlvbmFsIEFjYWRlbWllcyBOdWNsZWFyIGFuZCBSYWRp\n" +
        "YXRpb24gU3R1ZGllcyBCb2FyZCBhbmQgdGhlIERlcGFydG1lbnQgb2YgRW5lcmd54oCZcyBCYXNp\n" +
        "YyBFbmVyZ3kgU2NpZW5jZXMgQWR2aXNvcnkgQ29tbWl0dGVlLiAgRHIuIENsYXJrIGhvbGRzIGEg\n" +
        "UGguRC4gYW5kIE0uUy4gaW4gSW5vcmdhbmljL1JhZGlvY2hlbWlzdHJ5IGZyb20gRmxvcmlkYSBT\n" +
        "dGF0ZSBVbml2ZXJzaXR5IGFuZCBhIEIuUy4gaW4gQ2hlbWlzdHJ5IGZyb20gTGFuZGVyIENvbGxl\n" +
        "Z2UuDQo=";
    
    final byte[] octets = new byte[1024];
    final int n_octets = Base64.decode(textValue, octets);
    
    Transmogrifier encoder = new Transmogrifier();
    encoder.setGrammarCache(grammarCache);

    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);

    decoder.setEnableBinaryData(true);
    decoder.setInitialBinaryDataBufferSize(64);
    Scanner scanner;
    
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    encoder.setOutputStream(baos);

    SAXTransmogrifier saxTransmogrifier = encoder.getSAXTransmogrifier();
    BinaryDataSink binaryDataSink;

    saxTransmogrifier.setDocumentLocator(new LocatorImpl());
    saxTransmogrifier.startDocument();
    saxTransmogrifier.startPrefixMapping("foo", "urn:foo");
    saxTransmogrifier.startElement("urn:foo", "Base64Binary", "foo:Base64Binary", new AttributesImpl());
    binaryDataSink = saxTransmogrifier.startBinaryData(((long)Integer.MAX_VALUE) * 10); // big data
    saxTransmogrifier.binaryData(octets, 0, n_octets, binaryDataSink);
    // Stop encoding here prematurely. We can't output gigantic data.
    
    final byte[] bts = baos.toByteArray();
    decoder.setInputStream(new ByteArrayInputStream(bts));

    ByteArrayOutputStream octetStream = new ByteArrayOutputStream();

    final int chunkSize = 100;
    scanner = decoder.processHeader();
    scanner.setBinaryChunkSize(chunkSize);
    
    EventDescription event;
    while ((event = scanner.nextEvent()) != null) {
      final byte eventKind = event.getEventKind();
      if (EventDescription.EVENT_BLOB == eventKind) {
        BinaryDataSource binaryData = event.getBinaryDataSource();
        Assert.assertEquals(((long)Integer.MAX_VALUE) * 10 - chunkSize, binaryData.getRemainingBytesCount());
        octetStream.write(binaryData.getByteArray(), binaryData.getStartIndex(), binaryData.getLength());
        byte[] decodedBytes = octetStream.toByteArray();
        for (int i = 0; i < chunkSize; i++) {
          Assert.assertEquals(octets[i], decodedBytes[i]);
        }
        return;
      }
      else if (EventDescription.EVENT_CH == eventKind) {
        Assert.fail();
      }
    }
    Assert.fail();
  }

  /**
   * Tests BinaryDataHandler's startBinaryData method with manifested total size 
   * greater than Integer.MAX_VALUE.
   */
  public void testBinaryDataSizeMismatch_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/base64Binary.xsd", getClass(), new EXISchemaFactoryTestUtilContext(m_compilerErrors));
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    Transmogrifier encoder = new Transmogrifier();
    encoder.setGrammarCache(grammarCache);
    
    byte[] bts = new byte[100];

    for (AlignmentType alignment : Alignments) {
      encoder.setAlignmentType(alignment);

      encoder.setOutputStream(new ByteArrayOutputStream());

      SAXTransmogrifier saxTransmogrifier = encoder.getSAXTransmogrifier();

      BinaryDataSink binaryDataSink;

      saxTransmogrifier.setDocumentLocator(new LocatorImpl());
      saxTransmogrifier.startDocument();
      saxTransmogrifier.startPrefixMapping("foo", "urn:foo");
      saxTransmogrifier.startElement("urn:foo", "Base64Binary", "foo:Base64Binary", new AttributesImpl());

      binaryDataSink = saxTransmogrifier.startBinaryData(100);
      saxTransmogrifier.binaryData(bts, 0, 30, binaryDataSink);
      saxTransmogrifier.binaryData(bts, 0, 30, binaryDataSink);
      try {
        saxTransmogrifier.endBinaryData(binaryDataSink);
      }
      catch (SAXException se) {
        TransmogrifierException te = (TransmogrifierException)se.getException();
        Assert.assertEquals(TransmogrifierException.SCRIBER_ERROR, te.getCode());
        ScriberRuntimeException se2 = (ScriberRuntimeException)te.getException();
        Assert.assertEquals(ScriberRuntimeException.BINARY_DATA_SIZE_MISMATCH, se2.getCode());
        continue;
      }
      Assert.fail();
    }
  }
  
}
