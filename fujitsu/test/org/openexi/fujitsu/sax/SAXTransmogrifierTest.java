package org.openexi.fujitsu.sax;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.ArrayList;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.SAXParserFactory;

import junit.framework.Assert;

import org.openexi.fujitsu.proc.EXIDecoder;
import org.openexi.fujitsu.proc.common.AlignmentType;
import org.openexi.fujitsu.proc.common.EXIEvent;
import org.openexi.fujitsu.proc.common.EventCode;
import org.openexi.fujitsu.proc.common.EventType;
import org.openexi.fujitsu.proc.common.EventTypeList;
import org.openexi.fujitsu.proc.common.GrammarOptions;
import org.openexi.fujitsu.proc.grammars.EventTypeAccessor;
import org.openexi.fujitsu.proc.grammars.GrammarCache;
import org.openexi.fujitsu.proc.grammars.GroupGrammar;
import org.openexi.fujitsu.proc.io.Scanner;
import org.openexi.fujitsu.schema.EXISchema;
import org.openexi.fujitsu.schema.TestBase;
import org.openexi.fujitsu.scomp.EXISchemaFactoryErrorMonitor;
import org.openexi.fujitsu.scomp.EXISchemaFactoryTestUtil;

public class SAXTransmogrifierTest extends TestBase {
  
  public SAXTransmogrifierTest(String name) {
    super(name);
    m_saxParserFactory = SAXParserFactory.newInstance();
    m_saxParserFactory.setNamespaceAware(true);
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
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
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
    encoder.setEXISchema(grammarCache);

    for (AlignmentType alignment : Alignments) {
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      XMLReader xmlReader = m_saxParserFactory.newSAXParser().getXMLReader();
      xmlReader.setContentHandler(encoder.getSAXTransmogrifier());
      
      xmlReader.parse(new InputSource(new StringReader(xmlString)));

      bts = baos.toByteArray();
      
      decoder.setEXISchema(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();
      
      EXIEvent exiEvent;
      n_events = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        exiEventList.add(exiEvent);
      }
      
      Assert.assertEquals(19, n_events);
  
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
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AB", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex()); // because of xsi:type
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);
      
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
  
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AC", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AC", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
  
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);
      
      exiEvent = exiEventList.get(7);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
  
      exiEvent = exiEventList.get(8);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AC", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AC", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
  
      exiEvent = exiEventList.get(9);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);
      
      exiEvent = exiEventList.get(10);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
  
      exiEvent = exiEventList.get(11);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AD", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AD", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
  
      exiEvent = exiEventList.get(12);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);
      
      exiEvent = exiEventList.get(13);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
  
      exiEvent = exiEventList.get(14);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AE", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AE", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getEE().getIndex());
  
      exiEvent = exiEventList.get(15);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);
      
      exiEvent = exiEventList.get(16);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
  
      exiEvent = exiEventList.get(17);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
  
      exiEvent = exiEventList.get(18);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
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
    encoder.setEXISchema(grammarCache);

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
      
      decoder.setEXISchema(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();
      
      EXIEvent exiEvent;
      int n_events = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        exiEventList.add(exiEvent);
      }
      
      Assert.assertEquals(10, n_events);
  
      EventType eventType;
      EventTypeList eventTypeList;
  
      exiEvent = exiEventList.get(0);
      Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
  
      exiEvent = exiEventList.get(1);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("C", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
  
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AC", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AC", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(11, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_CM, eventType.itemType);
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventCode.ITEM_PI, eventType.itemType);
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_CM, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_PI, eventType.itemType);
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_CM, exiEvent.getEventVariety());
      Assert.assertEquals(" Good? ", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_CM, eventType.itemType);
      Assert.assertEquals(5, eventType.getIndex());
      // make sure this ITEM_CM belongs to AllGroupGrammar
      Assert.assertEquals(EXISchema.GROUP_ALL, ((GroupGrammar)EventTypeAccessor.getGrammar(eventType)).getCompositor()); 
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(7, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AC", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_PI, eventType.itemType);
  
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_PI, exiEvent.getEventVariety());
      Assert.assertEquals("eg", exiEvent.getName());
      Assert.assertEquals("Good! ", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_PI, eventType.itemType);
      Assert.assertEquals(6, eventType.getIndex());
      // make sure this ITEM_CM belongs to AllGroupGrammar
      Assert.assertEquals(EXISchema.GROUP_ALL, ((GroupGrammar)EventTypeAccessor.getGrammar(eventType)).getCompositor()); 
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(7, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AC", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_CM, eventType.itemType);
  
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(7, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AC", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_CM, eventType.itemType);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_PI, eventType.itemType);
  
      exiEvent = exiEventList.get(7);
      Assert.assertEquals(EXIEvent.EVENT_PI, exiEvent.getEventVariety());
      Assert.assertEquals("eg", exiEvent.getName());
      Assert.assertEquals("Good? ", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_PI, eventType.itemType);
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_ED, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_CM, eventType.itemType);
  
      exiEvent = exiEventList.get(8);
      Assert.assertEquals(EXIEvent.EVENT_CM, exiEvent.getEventVariety());
      Assert.assertEquals(" Good! ", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_CM, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_ED, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_PI, eventType.itemType);
  
      exiEvent = exiEventList.get(9);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_CM, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_PI, eventType.itemType);
    }
  }
  
}
