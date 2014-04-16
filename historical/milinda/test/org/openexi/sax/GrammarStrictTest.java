package org.openexi.sax;

import junit.framework.Assert;

import java.io.StringReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import java.util.ArrayList;

import java.net.URL;

import org.xml.sax.InputSource;

import org.openexi.proc.EXIDecoder;
import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EventCode;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventTypeList;
import org.openexi.proc.common.EXIEvent;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.common.XmlUriConst;
import org.openexi.proc.events.EXIEventSchemaNil;
import org.openexi.proc.io.Scanner;
import org.openexi.proc.grammars.EventTypeSchema;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.sax.Transmogrifier;
import org.openexi.sax.TransmogrifierException;
import org.openexi.schema.EXISchema;
import org.openexi.schema.TestBase;

import org.openexi.scomp.Docbook43Schema;
import org.openexi.scomp.EXISchemaFactoryErrorMonitor;
import org.openexi.scomp.EXISchemaFactoryTestUtil;

public class GrammarStrictTest extends TestBase {

  public GrammarStrictTest(String name) {
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
  public void testAcceptanceForA_01() throws Exception {
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
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      encoder.encode(new InputSource(new StringReader(xmlString)));
      
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
      Transmogrifier encoder = new Transmogrifier();
      
      encoder.setAlignmentType(alignment);

      boolean caught;
  
      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      caught = false;
      try {
        encoder.encode(new InputSource(new StringReader(xmlString)));
      }
      catch (TransmogrifierException eee) {
        caught = true;
        Assert.assertEquals(TransmogrifierException.UNEXPECTED_ELEM, eee.getCode());
      }
      Assert.assertTrue(caught);
    }
  }

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
   *   <AB/><AD/>
   * </A>
   */
  public void testAcceptanceForA_02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    String xmlString;
    byte[] bts;
    int n_events;
    
    xmlString = 
      "<A xmlns='urn:foo'>\n" +
      "  <AB/><AD/>\n" +
      "</A>\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      encoder.encode(new InputSource(new StringReader(xmlString)));
      
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
      
      Assert.assertEquals(10, n_events);
  
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
      Assert.assertEquals(1, eventType.getIndex());
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
      Assert.assertEquals("AD", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AD", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
  
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);
      
      exiEvent = exiEventList.get(7);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
  
      exiEvent = exiEventList.get(8);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex());
  
      exiEvent = exiEventList.get(9);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
    }
    
    xmlString = 
      "<A xmlns='urn:foo'>\n" +
      "  <AB/><AC/><AC/><AE/>\n" + // <AD/> is required.
      "</A>\n";

    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      
      encoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      boolean caught;
  
      caught = false;
      try {
        encoder.encode(new InputSource(new StringReader(xmlString)));
      }
      catch (TransmogrifierException eee) {
        caught = true;
        Assert.assertEquals(TransmogrifierException.UNEXPECTED_ELEM, eee.getCode());
      }
      Assert.assertTrue(caught);
    }    
  }

  /**
   * Schema: 
   * <xsd:element name="B">
   *   <xsd:complexType>
   *     <xsd:sequence>
   *       <xsd:element ref="foo:AB"/>
   *       <xsd:element ref="foo:AC" minOccurs="0" maxOccurs="2"/>
   *       <xsd:element ref="foo:AD" minOccurs="0"/>
   *     </xsd:sequence>
   *   </xsd:complexType>
   * </xsd:element>
   *
   * Instance:
   * <B>
   *   <AB/><AC/><AC/><AD/>
   * </B>
   */
  public void testAcceptanceForB() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    final String xmlString = 
      "<B xmlns='urn:foo'>\n" +
      "  <AB/><AC/><AC/><AD/>\n" +
      "</B>\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      encoder.encode(new InputSource(new StringReader(xmlString)));
  
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
      
      Assert.assertEquals(16, n_events);
      
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
      Assert.assertEquals("B", eventType.getName());
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
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
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
      Assert.assertEquals(3, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals("AD", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertNotNull(eventTypeList.getEE());
  
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
      Assert.assertEquals(3, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals("AD", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertNotNull(eventTypeList.getEE());
  
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
      Assert.assertEquals(2, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
  
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
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      
      exiEvent = exiEventList.get(15);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
    }
  }
  
  /**
   * Test getNextSubstanceParticles() method with nested sequences. 
   * 
   * <xsd:element name="D">
   *   <xsd:complexType>
   *     <xsd:sequence>
   *       <xsd:sequence>
   *         <xsd:element name="A" minOccurs="0" maxOccurs="2"/>
   *         <xsd:sequence maxOccurs="2">
   *           <xsd:element name="B" />
   *           <xsd:element name="C" minOccurs="0" />
   *           <xsd:element name="D" minOccurs="0" />
   *         </xsd:sequence>
   *       </xsd:sequence>
   *       <xsd:element name="E" minOccurs="0"/>
   *       <xsd:element name="F" minOccurs="0"/>
   *     </xsd:sequence>
   *   </xsd:complexType>
   * </xsd:element>
   */
  public void testNextSubstances_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    final String xmlString = 
      "<foo:D xmlns:foo='urn:foo'>\n" +
      "  <A/><A/><B/><C/><D/><B/><E/><F/>\n" +
      "</foo:D>\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      encoder.encode(new InputSource(new StringReader(xmlString)));
      
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
      
      Assert.assertEquals(20, n_events);
  
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
      Assert.assertEquals("D", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("A", exiEvent.getName());
      Assert.assertEquals("", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      Assert.assertNull(eventTypeList.getEE());
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
      
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("A", exiEvent.getName());
      Assert.assertEquals("", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      Assert.assertNull(eventTypeList.getEE());
  
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
  
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("B", exiEvent.getName());
      Assert.assertEquals("", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      Assert.assertNull(eventTypeList.getEE());
      
      exiEvent = exiEventList.get(7);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
  
      exiEvent = exiEventList.get(8);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("C", exiEvent.getName());
      Assert.assertEquals("", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("C", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(6, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("D", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("E", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("F", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
  
      exiEvent = exiEventList.get(9);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
    
      exiEvent = exiEventList.get(10);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("D", exiEvent.getName());
      Assert.assertEquals("", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("D", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("E", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("F", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
  
      exiEvent = exiEventList.get(11);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
  
      exiEvent = exiEventList.get(12);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("B", exiEvent.getName());
      Assert.assertEquals("", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(4, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("E", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("F", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      
      exiEvent = exiEventList.get(13);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
  
      exiEvent = exiEventList.get(14);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("E", exiEvent.getName());
      Assert.assertEquals("", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("E", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("C", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("D", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("F", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      
      exiEvent = exiEventList.get(15);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
  
      exiEvent = exiEventList.get(16);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("F", exiEvent.getName());
      Assert.assertEquals("", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("F", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      
      exiEvent = exiEventList.get(17);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
  
      exiEvent = exiEventList.get(18);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
  
      exiEvent = exiEventList.get(19);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
    }
  }
  
  /**
   * Test getNextSubstanceParticles() method with nested sequences/choices. 
   * 
   * <xsd:element name="E">
   *   <xsd:complexType>
   *     <xsd:sequence>
   *       <xsd:choice>
   *         <xsd:sequence maxOccurs="2">
   *           <xsd:element name="A" minOccurs="0" maxOccurs="2" />
   *           <xsd:element name="B" />
   *           <xsd:element name="C" minOccurs="0" />
   *         </xsd:sequence>
   *         <xsd:sequence minOccurs="0">
   *           <xsd:element name="D" />
   *           <xsd:element name="E" />
   *           <xsd:element name="F" />
   *         </xsd:sequence>
   *       </xsd:choice>
   *       <xsd:element name="G" minOccurs="0" />
   *       <xsd:element name="H" minOccurs="0" />
   *     </xsd:sequence>
   *   </xsd:complexType>
   * </xsd:element>
   */
  public void testNextSubstances_02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    final String xmlString = 
      "<foo:E xmlns:foo='urn:foo'>\n" +
      "  <A/><A/><B/><C/><B/><G/><H/>\n" +
      "</foo:E>\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      encoder.encode(new InputSource(new StringReader(xmlString)));
      
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
      
      Assert.assertEquals(18, n_events);
  
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
      Assert.assertEquals("E", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("A", exiEvent.getName());
      Assert.assertEquals("", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(6, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("D", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("G", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("H", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
      
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("A", exiEvent.getName());
      Assert.assertEquals("", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getLength());
      Assert.assertNull(eventTypeList.getEE());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
  
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
  
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("B", exiEvent.getName());
      Assert.assertEquals("", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      Assert.assertNull(eventTypeList.getEE());
      
      exiEvent = exiEventList.get(7);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
  
      exiEvent = exiEventList.get(8);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("C", exiEvent.getName());
      Assert.assertEquals("", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("C", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(6, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("G", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("H", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
  
      exiEvent = exiEventList.get(9);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
    
      exiEvent = exiEventList.get(10);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("B", exiEvent.getName());
      Assert.assertEquals("", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("G", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("H", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
  
      exiEvent = exiEventList.get(11);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
  
      exiEvent = exiEventList.get(12);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("G", exiEvent.getName());
      Assert.assertEquals("", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("G", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(4, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("C", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("H", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
  
      exiEvent = exiEventList.get(13);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
  
      exiEvent = exiEventList.get(14);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("H", exiEvent.getName());
      Assert.assertEquals("", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("H", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      
      exiEvent = exiEventList.get(15);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
  
      exiEvent = exiEventList.get(16);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
  
      exiEvent = exiEventList.get(17);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
    }
  }

  /**
   * Schema:
   * <xsd:element name="F" nillable="true">
   *  <xsd:complexType>
   *    <xsd:sequence>
   *      <xsd:element ref="foo:AB"/>
   *    </xsd:sequence>
   *    <xsd:attribute ref="foo:aA" use="required"/>
   *    <xsd:attribute ref="foo:aB" />
   *  </xsd:complexType>
   *</xsd:element>
   */
  public void testAcceptanceForF() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    ByteArrayOutputStream baos;

    String xmlString;
    
    xmlString = 
      "<foo:F xmlns:foo='urn:foo' foo:aA='' foo:aB=''>\n" +
      "  <foo:AB/>\n" +
      "</foo:F>\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
  
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
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
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("F", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
  
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
      Assert.assertEquals("aA", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aA", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getLength());
      Assert.assertNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
      Assert.assertEquals("aB", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getLength());
      Assert.assertNull(eventTypeList.getEE());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AB", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      Assert.assertNull(eventTypeList.getEE());
  
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex()); // because of xsi:type
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);
      
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      
      exiEvent = exiEventList.get(7);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
  
      exiEvent = exiEventList.get(8);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
    }
    
    xmlString = 
      "<foo:F xmlns:foo='urn:foo' foo:aA='' foo:aB=''>\n" +
      "</foo:F>\n";

    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
  
      try {
        encoder.encode(new InputSource(new StringReader(xmlString)));
      }
      catch (TransmogrifierException te) {
        Assert.assertEquals(TransmogrifierException.UNEXPECTED_END_ELEM, te.getCode());
        return;
      }
      Assert.fail();
    }
  }
  
  /**
   * Schema:
   * <xsd:element name="F" nillable="true">
   *  <xsd:complexType>
   *    <xsd:sequence>
   *      <xsd:element ref="foo:AB"/>
   *    </xsd:sequence>
   *    <xsd:attribute ref="foo:aA" use="required"/>
   *    <xsd:attribute ref="foo:aB" />
   *  </xsd:complexType>
   *</xsd:element>
   */
  public void testAcceptanceForNilledF() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    ByteArrayOutputStream baos;
    
    String xmlString;
    
    xmlString = 
      "<foo:F xmlns:foo='urn:foo' foo:aA='' foo:aB='' xsi:nil='true' \n" +
      "       xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'> \n" +
      "</foo:F>\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
  
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
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
      
      Assert.assertEquals(7, n_events);
  
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
      Assert.assertEquals("F", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
  
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_NL, exiEvent.getEventVariety());
      Assert.assertEquals("nil", exiEvent.getName());
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      Assert.assertEquals("nil", eventType.getName());
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.getURI());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getLength());
      Assert.assertNull(eventTypeList.getEE());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aA", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
      Assert.assertEquals("aA", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aA", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      Assert.assertNull(eventTypeList.getEE());
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
      Assert.assertEquals("aB", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
  
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
    }
    
    xmlString = 
      "<foo:F xmlns:foo='urn:foo' foo:aA='' foo:aB='' xsi:nil='true' \n" +
      "       xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'> \n" +
      "  <foo:AB/>\n" +
      "</foo:F>\n";

    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      
      encoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
  
      try {
        encoder.encode(new InputSource(new StringReader(xmlString)));
      }
      catch (TransmogrifierException eee) {
        Assert.assertEquals(TransmogrifierException.UNEXPECTED_ELEM, eee.getCode());
        return;
      }
      Assert.fail();
    }
  }
  
  /**
   * Schema:
   * <xsd:element name="G" nillable="true">
   *  <xsd:complexType>
   *    <xsd:sequence>
   *      <xsd:element ref="foo:AB" minOccurs="0"/>
   *    </xsd:sequence>
   *    <xsd:attribute ref="foo:aA" use="required"/>
   *    <xsd:attribute ref="foo:aB" />
   *  </xsd:complexType>
   *</xsd:element>
   */
  public void testAcceptanceForG() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    ByteArrayOutputStream baos;

    String xmlString;
    byte[] bts;
    int n_events;

    ArrayList<EXIEvent> exiEventList;

    EXIEvent exiEvent;

    EventType eventType;
    EventTypeList eventTypeList;

    xmlString = 
      "<foo:G xmlns:foo='urn:foo' foo:aA='' foo:aB=''>\n" +
      "  <foo:AB/>\n" +
      "</foo:G>\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setEXISchema(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      exiEventList = new ArrayList<EXIEvent>();
      
      n_events = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        exiEventList.add(exiEvent);
      }
      
      Assert.assertEquals(9, n_events);
  
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
      Assert.assertEquals("G", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
  
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
      Assert.assertEquals("aA", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aA", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getLength());
      Assert.assertNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
      Assert.assertEquals("aB", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AB", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
  
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex()); // because of xsi:type
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);
      
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      
      exiEvent = exiEventList.get(7);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
  
      exiEvent = exiEventList.get(8);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
    }
    
    xmlString = 
      "<foo:G xmlns:foo='urn:foo' foo:aA='' foo:aB=''>\n" +
      "</foo:G>\n";

    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setEXISchema(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      exiEventList = new ArrayList<EXIEvent>();
      n_events = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        exiEventList.add(exiEvent);
      }
      
      Assert.assertEquals(6, n_events);
  
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
      Assert.assertEquals("G", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
  
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
      Assert.assertEquals("aA", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aA", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getLength());
      Assert.assertNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
      Assert.assertEquals("aB", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
  
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
    }
  }

  /**
   * Schema:
   * <xsd:element name="H">
   *   <xsd:complexType>
   *     <xsd:sequence>
   *       <xsd:element name="A" minOccurs="0"/>
   *       <xsd:any namespace="urn:eoo urn:goo" />
   *       <xsd:element name="B" />
   *       <xsd:any namespace="##other" />
   *     </xsd:sequence>
   *   </xsd:complexType>
   * </xsd:element>
   */
  public void testAcceptanceForH() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    final String xmlString;
    
    xmlString = 
      "<foo:H xmlns:foo='urn:foo' xmlns:goo='urn:goo'>\n" +
      "  <goo:AB/>\n" +
      "  <B/>\n" +
      "  <goo:AB/>\n" +
      "</foo:H>\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      encoder.encode(new InputSource(new StringReader(xmlString)));
      
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
      
      Assert.assertEquals(12, n_events);
  
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
      Assert.assertEquals("H", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
  
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AB", exiEvent.getName());
      Assert.assertEquals("urn:goo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:goo", eventType.getURI());
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3, eventTypeList.getLength());
      Assert.assertNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:eoo", eventType.getURI());
  
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
      Assert.assertEquals("B", exiEvent.getName());
      Assert.assertEquals("", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      Assert.assertNull(eventTypeList.getEE());
      
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
      
      exiEvent = exiEventList.get(7);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AB", exiEvent.getName());
      Assert.assertEquals("urn:goo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      Assert.assertNull(eventTypeList.getEE());
  
      exiEvent = exiEventList.get(8);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex()); // because of xsi:type
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);
      
      exiEvent = exiEventList.get(9);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
  
      exiEvent = exiEventList.get(10);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      
      exiEvent = exiEventList.get(11);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
    }
  }
  
  /**
   * Schema:
   * <xsd:element name="H">
   *   <xsd:complexType>
   *     <xsd:sequence>
   *       <xsd:any namespace="##other" minOccurs="0" />
   *       <xsd:any namespace="##targetNamespace ##local" minOccurs="0" />
   *     </xsd:sequence>
   *   </xsd:complexType>
   * </xsd:element>
   * 
   * After the occurrence of "##other" wildcard, "##targetNamespace" wildcard
   * can come, but not "##other" wildcard again.
   */
  public void testAcceptanceForH2_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    String xmlString;
    byte[] bts;
    int n_events;
    
    xmlString = 
      "<foo:H2 xmlns:foo='urn:foo' xmlns:goo='urn:goo'>\n" +
      "  <goo:AB/>\n" + // <xsd:any namespace="##other" minOccurs="0" />
      "  <foo:AB/>\n" + // <xsd:any namespace="##targetNamespace ##local" minOccurs="0" />
      "</foo:H2>\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      encoder.encode(new InputSource(new StringReader(xmlString)));
      
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
      
      Assert.assertEquals(10, n_events);
  
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
      Assert.assertEquals("H2", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AB", exiEvent.getName());
      Assert.assertEquals("urn:goo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3, eventTypeList.getEE().getIndex());
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex());
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
      Assert.assertEquals("AB", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getEE().getIndex());
  
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);
      
      exiEvent = exiEventList.get(7);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
  
      exiEvent = exiEventList.get(8);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
  
      exiEvent = exiEventList.get(9);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
    }
    
    xmlString = 
      "<foo:H2 xmlns:foo='urn:foo' xmlns:goo='urn:goo'>\n" +
      "  <goo:AB/>\n" +
      "  <goo:AB/>\n" + // unexpected
      "</foo:H2>\n";

    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      
      encoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      boolean caught;
  
      caught = false;
      try {
        encoder.encode(new InputSource(new StringReader(xmlString)));
      }
      catch (TransmogrifierException eee) {
        caught = true;
        Assert.assertEquals(TransmogrifierException.UNEXPECTED_ELEM, eee.getCode());
      }
      Assert.assertTrue(caught);
    }
  }
  
  /**
   * Schema:
   * <xsd:element name="H">
   *   <xsd:complexType>
   *     <xsd:sequence>
   *       <xsd:any namespace="##other" minOccurs="0" />
   *       <xsd:any namespace="##targetNamespace ##local" minOccurs="0" />
   *     </xsd:sequence>
   *   </xsd:complexType>
   * </xsd:element>
   * 
   * After the occurrence of "##other" wildcard, "##local" wildcard
   * can come only once, but not twice.
   */
  public void testAcceptanceForH2_02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    String xmlString;
    byte[] bts;
    int n_events;

    xmlString = 
      "<foo:H2 xmlns:foo='urn:foo' xmlns:goo='urn:goo'>\n" +
      "  <goo:AB/>\n" +
      "  <AB/>\n" +
      "</foo:H2>\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      encoder.encode(new InputSource(new StringReader(xmlString)));
  
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
  
      Assert.assertEquals(10, n_events);
  
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
      Assert.assertEquals("H2", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AB", exiEvent.getName());
      Assert.assertEquals("urn:goo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3, eventTypeList.getEE().getIndex());
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex());
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
      Assert.assertEquals("AB", exiEvent.getName());
      Assert.assertEquals("", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("", eventType.getURI());
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getEE().getIndex());
  
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);
      
      exiEvent = exiEventList.get(7);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
  
      exiEvent = exiEventList.get(8);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
  
      exiEvent = exiEventList.get(9);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
    }  
    
    xmlString = 
      "<foo:H2 xmlns:foo='urn:foo' xmlns:goo='urn:goo'>\n" +
      "  <goo:AB/>\n" +
      "  <AB/>\n" +
      "  <AB/>\n" + // unexpected
      "</foo:H2>\n";

    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      
      encoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      boolean caught;
  
      caught = false;
      try {
        encoder.encode(new InputSource(new StringReader(xmlString)));
      }
      catch (TransmogrifierException eee) {
        caught = true;
        Assert.assertEquals(TransmogrifierException.UNEXPECTED_ELEM, eee.getCode());
      }
      Assert.assertTrue(caught);
    }
  }
  
  /**
   * Schema:
   * <xsd:element name="H">
   *   <xsd:complexType>
   *     <xsd:sequence>
   *       <xsd:any namespace="##other" minOccurs="0" />
   *       <xsd:any namespace="##targetNamespace ##local" minOccurs="0" />
   *     </xsd:sequence>
   *   </xsd:complexType>
   * </xsd:element>
   * 
   * "##other" wildcard can be omitted. 
   * "##other" wildcard cannot occur after "##local" wildcard.
   */
  public void testAcceptanceForH2_03() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    String xmlString;
    byte[] bts;
    int n_events;

    xmlString = 
      "<foo:H2 xmlns:foo='urn:foo' xmlns:goo='urn:goo'>\n" +
      "  <foo:AB/>\n" +
      "</foo:H2>\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      encoder.encode(new InputSource(new StringReader(xmlString)));
  
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
  
      Assert.assertEquals(7, n_events);
  
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
      Assert.assertEquals("H2", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AB", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3, eventTypeList.getEE().getIndex());
  
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
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
  
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
    }
    
    xmlString = 
      "<foo:H2 xmlns:foo='urn:foo' xmlns:goo='urn:goo'>\n" +
      "  <foo:AB/>\n" +
      "  <goo:AB/>\n" + // unexpected
      "</foo:H2>\n";

    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      
      encoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      boolean caught;
  
      caught = false;
      try {
        encoder.encode(new InputSource(new StringReader(xmlString)));
      }
      catch (TransmogrifierException eee) {
        caught = true;
        Assert.assertEquals(TransmogrifierException.UNEXPECTED_ELEM, eee.getCode());
      }
      Assert.assertTrue(caught);
    }
  }

  /**
   * Schema:
   * <xsd:element name="H">
   *   <xsd:complexType>
   *     <xsd:sequence>
   *       <xsd:any namespace="##other" minOccurs="0" />
   *       <xsd:any namespace="##targetNamespace ##local" minOccurs="0" />
   *     </xsd:sequence>
   *   </xsd:complexType>
   * </xsd:element>
   * 
   * The 2nd wildcard ("##targetNamespace ##local") can be omitted. 
   * characters cannot occur after "##other" wildcard.
   */
  public void testAcceptanceForH2_04() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    String xmlString;
    byte[] bts;
    int n_events;
    
    xmlString = 
      "<foo:H2 xmlns:foo='urn:foo' xmlns:goo='urn:goo'>\n" +
      "  <goo:AB/>\n" +
      "</foo:H2>\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      encoder.encode(new InputSource(new StringReader(xmlString)));
  
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
  
      Assert.assertEquals(7, n_events);
  
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
      Assert.assertEquals("H2", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AB", exiEvent.getName());
      Assert.assertEquals("urn:goo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3, eventTypeList.getEE().getIndex());
  
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
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(2, eventType.getIndex());
  
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
    }  
    
    xmlString = 
      "<foo:H2 xmlns:foo='urn:foo' xmlns:goo='urn:goo'>\n" +
      "  <goo:AB/>\n" + 
      "  xyz" + // unexpected
      "</foo:H2>\n";

    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      
      encoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      boolean caught;
  
      caught = false;
      try {
        encoder.encode(new InputSource(new StringReader(xmlString)));
      }
      catch (TransmogrifierException eee) {
        caught = true;
        Assert.assertEquals(TransmogrifierException.UNEXPECTED_CHARS, eee.getCode());
      }
      Assert.assertTrue(caught);
    }
  }

  /**
   * Schema:
   * <xsd:element name="H3">
   *   <xsd:complexType>
   *     <xsd:sequence>
   *       <xsd:sequence>
   *         <xsd:any namespace="##local ##targetNamespace" minOccurs="0" maxOccurs="2"/>
   *         <xsd:element ref="hoo:AC" minOccurs="0"/>
   *         <xsd:sequence>
   *           <xsd:any namespace="urn:goo" minOccurs="0" />
   *           <xsd:element ref="hoo:AB" minOccurs="0"/>
   *         </xsd:sequence>
   *       </xsd:sequence>
   *       <xsd:any namespace="urn:ioo" minOccurs="0" />
   *     </xsd:sequence>
   *   </xsd:complexType>
   * </xsd:element>
   * 
   * Use of wildcards in a context with structures.
   */
  public void testAcceptanceForH3_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    String xmlString;
    byte[] bts;
    int n_events;
    
    xmlString = 
      "<foo:H3 xmlns:foo='urn:foo' xmlns:goo='urn:goo' xmlns:hoo='urn:hoo' xmlns:ioo='urn:ioo'>\n" +
      "  <foo:AB/>\n" + // <xsd:any namespace="##local ##targetNamespace" minOccurs="0" maxOccurs="2"/>
      "  <AB/>\n" + // same as above
      "  <hoo:AC/>\n" + // <xsd:element ref="hoo:AC" minOccurs="0"/>
      "  <goo:AB/>\n" + // <xsd:any namespace="urn:goo" minOccurs="0" />
      "  <hoo:AB/>\n" + // <xsd:element ref="hoo:AB" minOccurs="0"/>
      "  <ioo:AB/>\n" + // <xsd:any namespace="urn:ioo" minOccurs="0" />
      "</foo:H3>\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      encoder.encode(new InputSource(new StringReader(xmlString)));
      
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
      
      Assert.assertEquals(22, n_events);
  
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
      Assert.assertEquals("H3", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("H3", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AB", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(6, eventTypeList.getEE().getIndex());
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);
      
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
  
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AB", exiEvent.getName());
      Assert.assertEquals("", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("", eventType.getURI());
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(6, eventTypeList.getEE().getIndex());
  
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);
      
      exiEvent = exiEventList.get(7);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
  
      exiEvent = exiEventList.get(8);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AC", exiEvent.getName());
      Assert.assertEquals("urn:hoo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AC", eventType.getName());
      Assert.assertEquals("urn:hoo", eventType.getURI());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(4, eventTypeList.getEE().getIndex());
  
      exiEvent = exiEventList.get(9);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);
      
      exiEvent = exiEventList.get(10);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
  
      exiEvent = exiEventList.get(11);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AB", exiEvent.getName());
      Assert.assertEquals("urn:goo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:goo", eventType.getURI());
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3, eventTypeList.getEE().getIndex());
  
      exiEvent = exiEventList.get(12);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);
      
      exiEvent = exiEventList.get(13);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
  
      exiEvent = exiEventList.get(14);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AB", exiEvent.getName());
      Assert.assertEquals("urn:hoo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AB", eventType.getName());
      Assert.assertEquals("urn:hoo", eventType.getURI());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getEE().getIndex());
  
      exiEvent = exiEventList.get(15);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);
      
      exiEvent = exiEventList.get(16);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
  
      exiEvent = exiEventList.get(17);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AB", exiEvent.getName());
      Assert.assertEquals("urn:ioo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:ioo", eventType.getURI());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getEE().getIndex());
  
      exiEvent = exiEventList.get(18);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);
      
      exiEvent = exiEventList.get(19);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
  
      exiEvent = exiEventList.get(20);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
  
      exiEvent = exiEventList.get(21);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
    }
    
    xmlString = 
      "<foo:H3 xmlns:foo='urn:foo' xmlns:goo='urn:goo' xmlns:hoo='urn:hoo' xmlns:ioo='urn:ioo'>\n" +
      "  <foo:AB/>\n" + // <xsd:any namespace="##targetNamespace ##local" minOccurs="0" maxOccurs="2"/>
      "  <hoo:AD/>\n" + // unexpected
      "</foo:H3>\n";

    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      
      encoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      boolean caught;
  
      caught = false;
      try {
        encoder.encode(new InputSource(new StringReader(xmlString)));
      }
      catch (TransmogrifierException eee) {
        caught = true;
        Assert.assertEquals(TransmogrifierException.UNEXPECTED_ELEM, eee.getCode());
      }
      Assert.assertTrue(caught);
    }
  }
  
  /**
   * OPENGIS schema and instance.
   * There are nested groups in this example.
   */
  public void testOpenGisExample01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/opengis/openGis.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      URL url = resolveSystemIdAsURL("/opengis/openGis.xml");
      InputSource inputSource = new InputSource(url.toString());
      inputSource.setByteStream(url.openStream());
      
      encoder.encode(inputSource);
      
      byte[] bts = baos.toByteArray();
      
      decoder.setEXISchema(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      int n_events = 0;
      while (scanner.nextEvent() != null) {
        ++n_events;
      }
      
      Assert.assertEquals(77, n_events);
    }
  }

  /**
   * Docbook 4.3 schema and instance.
   */
  public void testDocbook43ExampleVerySimple01() throws Exception {
    EXISchema corpus = Docbook43Schema.getEXISchema(); 
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String xmlString;
    
    xmlString = 
      "<book>\n" +
      "  <part>\n" +
      "    <title>XYZ</title>\n" +
      "    <chapter>\n" +
      "      <title>YZX</title>\n" +
      "      <sect1>\n" +
      "        <title>ZXY</title>\n" +
      "        <para>abcde</para>\n" +
      "      </sect1>\n" +
      "    </chapter>\n" +
      "  </part>\n" +
      "</book>\n";

    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
  
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      byte[] bts = baos.toByteArray();
      
      decoder.setEXISchema(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      int n_events = 0;
      while (scanner.nextEvent() != null) {
        ++n_events;
      }
      
      Assert.assertEquals(22, n_events);
    }
  }

  /**
   * FPML 4.0 schema and instance.
   * xsi:type is used in this example.
   */
  public void testFpmlExample01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/fpml-4.0/fpml-main-4-0.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      URL url = resolveSystemIdAsURL("/fpml-4.0/msg_ex01_request_confirmation.xml");
      InputSource inputSource = new InputSource(url.toString());
      inputSource.setByteStream(url.openStream());
      
      encoder.encode(inputSource);
      
      byte[] bts = baos.toByteArray();
      
      decoder.setEXISchema(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      int n_events = 0;
      while (scanner.nextEvent() != null) {
        ++n_events;
      }
      
      Assert.assertEquals(102, n_events);
    }
  }

  /**
   * Schema:
   * <xsd:element name="ANY" type="xsd:anyType"/>
   * 
   * All attributes and child elements are defined in schema. 
   */
  public void testAcceptanceForANY_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    final String xmlString;
    
    xmlString = 
      "<foo:ANY xmlns:foo='urn:foo' xmlns:goo='urn:goo'\n" + 
      "  foo:aA='a' foo:aB='b' foo:aC='c' >\n" +
      "  TEXT 1 " +
      "  <goo:AB/>\n" +
      "  TEXT 2 " +
      "  <goo:AC/>\n" +
      "  TEXT 3 " +
      "</foo:ANY>\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      byte[] bts = baos.toByteArray();
      
      decoder.setEXISchema(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();
      
      int n_events = 0;
      EXIEvent exiEvent;
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        exiEventList.add(exiEvent);
      }
      
      Assert.assertEquals(16, n_events);
      
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
      Assert.assertEquals("ANY", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
      Assert.assertEquals("aA", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      Assert.assertEquals("a", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertEquals(null, eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
      Assert.assertEquals("aB", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      Assert.assertEquals("b", exiEvent.getCharacters().makeString());
      eventType = (EventTypeSchema)exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertEquals(null, eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
      Assert.assertEquals("aC", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      Assert.assertEquals("c", exiEvent.getCharacters().makeString());
      eventType = (EventTypeSchema)exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertEquals(null, eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
  
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("\n  TEXT 1   ", exiEvent.getCharacters().makeString());
      eventType = (EventTypeSchema)exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
      Assert.assertEquals(4, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
  
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AB", exiEvent.getName());
      Assert.assertEquals("urn:goo", exiEvent.getURI());
      eventType = (EventTypeSchema)exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      Assert.assertEquals(null, eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
  
      exiEvent = exiEventList.get(7);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getLength());
      Assert.assertNull(eventTypeList.getEE());
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);
      
      exiEvent = exiEventList.get(8);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      Assert.assertEquals(1, eventType.getEventTypeList().getLength());
  
      exiEvent = exiEventList.get(9);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("\n  TEXT 2   ", exiEvent.getCharacters().makeString());
      eventType = (EventTypeSchema)exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
  
      exiEvent = exiEventList.get(10);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AC", exiEvent.getName());
      Assert.assertEquals("urn:goo", exiEvent.getURI());
      eventType = (EventTypeSchema)exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      Assert.assertEquals(null, eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
  
      exiEvent = exiEventList.get(11);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getLength());
      Assert.assertNull(eventTypeList.getEE());
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);
      
      exiEvent = exiEventList.get(12);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      Assert.assertEquals(1, eventType.getEventTypeList().getLength());
  
      exiEvent = exiEventList.get(13);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("\n  TEXT 3 ", exiEvent.getCharacters().makeString());
      eventType = (EventTypeSchema)exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
  
      exiEvent = exiEventList.get(14);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = (EventTypeSchema)exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
    }
  }
  
  /**
   * Schema:
   * <xsd:element name="ANY" type="xsd:anyType"/>
   * 
   * Use of elements that are not defined in schema.
   */
  public void testAcceptanceForANY_02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    final String xmlString;
    
    xmlString = 
      "<foo:ANY xmlns:foo='urn:foo' xmlns:goo='urn:goo'>\n" + 
      "  <goo:NONE>abc</goo:NONE>\n" + 
      "  <goo:NONE>\n" + 
      "    <foo:NONE>def</foo:NONE>\n" + 
      "  </goo:NONE>\n" + 
      "</foo:ANY>"; 
    
    for (AlignmentType alignment : Alignments) {
      for (boolean preserveWhitespaces : new boolean[] { true, false }) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;
        
        encoder.setAlignmentType(alignment);
        decoder.setAlignmentType(alignment);
  
        encoder.setEXISchema(grammarCache);
        encoder.setPreserveWhitespaces(preserveWhitespaces);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        encoder.setOutputStream(baos);
  
        encoder.encode(new InputSource(new StringReader(xmlString)));
        
        byte[] bts = baos.toByteArray();
        
        decoder.setEXISchema(grammarCache);
        decoder.setInputStream(new ByteArrayInputStream(bts));
        scanner = decoder.processHeader();
        
        int n_events = 0;
        EXIEvent exiEvent;
        
        EventType eventType;
        EventTypeList eventTypeList;
    
        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
          eventType = exiEvent.getEventType();
          Assert.assertSame(exiEvent, eventType);
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertNull(eventTypeList.getEE());
        }
        
        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
          Assert.assertEquals("ANY", eventType.getName());
          Assert.assertEquals("urn:foo", eventType.getURI());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertNull(eventTypeList.getEE());
        }
        
        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
          Assert.assertEquals("\n  ", exiEvent.getCharacters().makeString());
          eventType = (EventTypeSchema)exiEvent.getEventType();
          Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
          Assert.assertEquals(4, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(5, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
        }
        
        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
          Assert.assertEquals("NONE", exiEvent.getName());
          Assert.assertEquals("urn:goo", exiEvent.getURI());
          eventType = (EventTypeSchema)exiEvent.getEventType();
          Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
          Assert.assertEquals(null, eventType.getName());
          Assert.assertEquals("", eventType.getURI());
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(3, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
        }
        
        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
          Assert.assertEquals("abc", exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.assertEquals(4, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertEquals(5, eventTypeList.getLength());
            Assert.assertNotNull(eventTypeList.getEE());
            eventType = eventTypeList.item(0);
            Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
            Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
            eventType = eventTypeList.item(1);
            Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
            Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
            eventType = eventTypeList.item(2);
            Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            eventType = eventTypeList.item(3);
            Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
          }
        }
        
        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.assertEquals(0, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertEquals(3, eventTypeList.getLength());
            eventType = eventTypeList.item(1);
            Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(2);
            Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
          }
        }
        
        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
          Assert.assertEquals("\n  ", exiEvent.getCharacters().makeString());
          eventType = (EventTypeSchema)exiEvent.getEventType();
          Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
          Assert.assertEquals(2, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(3, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
        }
        
        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
          Assert.assertEquals("NONE", exiEvent.getName());
          Assert.assertEquals("urn:goo", exiEvent.getURI());
          eventType = (EventTypeSchema)exiEvent.getEventType();
          Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
          Assert.assertEquals(null, eventType.getName());
          Assert.assertEquals("", eventType.getURI());
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(3, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
        }
        
        if (preserveWhitespaces && (exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
          Assert.assertEquals("\n    ", exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.assertEquals(0, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertEquals(5, eventTypeList.getLength());
            Assert.assertNotNull(eventTypeList.getEE());
            eventType = eventTypeList.item(1);
            Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
            Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
            eventType = eventTypeList.item(2);
            Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            eventType = eventTypeList.item(3);
            Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(4);
            Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
            Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          }
        }
        
        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
          Assert.assertEquals("NONE", exiEvent.getName());
          Assert.assertEquals("urn:foo", exiEvent.getURI());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
          Assert.assertEquals(null, eventType.getName());
          Assert.assertEquals("", eventType.getURI());
          if (preserveWhitespaces && (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned)) {
            Assert.assertEquals(2, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertEquals(4, eventTypeList.getLength());
            Assert.assertNotNull(eventTypeList.getEE());
            eventType = eventTypeList.item(0);
            Assert.assertEquals(EventCode.ITEM_SE, eventType.itemType);
            Assert.assertEquals("urn:foo", eventType.getURI());
            Assert.assertEquals("NONE", eventType.getName());
            eventType = eventTypeList.item(1);
            Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
            Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
            eventType = eventTypeList.item(3);
            Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
          }
        }
        
        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
          Assert.assertEquals("def", exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          Assert.assertEquals(4, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(5, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        }
        
        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(3, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
        }
        
        if (preserveWhitespaces && (exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
          Assert.assertEquals("\n  ", exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          Assert.assertEquals(4, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(5, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventCode.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:foo", eventType.getURI());
          Assert.assertEquals("NONE", eventType.getName());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        }
        
        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
          if (preserveWhitespaces) {
            Assert.assertEquals(2, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertEquals(5, eventTypeList.getLength());
            Assert.assertNotNull(eventTypeList.getEE());
            eventType = eventTypeList.item(0);
            Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
            Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
            eventType = eventTypeList.item(1);
            Assert.assertEquals(EventCode.ITEM_SE, eventType.itemType);
            Assert.assertEquals("urn:foo", eventType.getURI());
            Assert.assertEquals("NONE", eventType.getName());
            eventType = eventTypeList.item(3);
            Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(4);
            Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
            Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          }
        }
        
        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
          Assert.assertEquals("\n", exiEvent.getCharacters().makeString());
          eventType = (EventTypeSchema)exiEvent.getEventType();
          Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
          Assert.assertEquals(2, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(3, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
        }
        
        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
          Assert.assertEquals(1, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(3, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
        }
        
        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
          eventType = exiEvent.getEventType();
          Assert.assertSame(exiEvent, eventType);
          Assert.assertEquals(0, eventType.getIndex());
        }
        
        Assert.assertEquals(preserveWhitespaces ? 17 : 15, n_events);
      }
    }
  }

  /**
   * Schema:
   * <xsd:element name="J">
   *   <xsd:complexType>
   *     <xsd:sequence maxOccurs="2">
   *       <xsd:element ref="foo:AB"/>
   *       <xsd:element ref="foo:AC" minOccurs="0" maxOccurs="2"/>
   *     </xsd:sequence>
   *   </xsd:complexType>
   * </xsd:element>
   * 
   * Instance:
   * <J>
   *   <AB/><AC/><AC/><AB/><AC/>
   * </J>
   */
  public void testAcceptanceForJ_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    String xmlString;
    byte[] bts;
    int n_events;
    
    xmlString = 
      "<J xmlns='urn:foo'>\n" +
      "  <AB/><AC/><AC/><AB/><AC/>\n" +
      "</J>\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
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
      Assert.assertEquals("J", eventType.getName());
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
      Assert.assertEquals(1, eventTypeList.getLength());
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
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals("AB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertNotNull(eventTypeList.getEE());
  
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
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals("AB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertNotNull(eventTypeList.getEE());
  
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
      Assert.assertEquals("AB", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
  
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
      Assert.assertEquals("AC", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AC", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
  
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
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals("AC", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
  
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
      Transmogrifier encoder = new Transmogrifier();
      
      encoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      boolean caught;
  
      caught = false;
      try {
        encoder.encode(new InputSource(new StringReader(xmlString)));
      }
      catch (TransmogrifierException eee) {
        caught = true;
        Assert.assertEquals(TransmogrifierException.UNEXPECTED_ELEM, eee.getCode());
      }
      Assert.assertTrue(caught);
    }
  }

  /**
   * Schema:
   * None available
   * 
   * Instance:
   * <None>ABC</None>
   */
  public void testAcceptanceForNone() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String xmlString;
    byte[] bts;
    int n_events;
    
    xmlString = "<None xmlns='urn:foo'>ABC</None>\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setEXISchema(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EXIEvent exiEvent;
      n_events = 0;
  
      EventType eventType;
      EventTypeList eventTypeList;
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertNull(eventTypeList.getEE());
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
        Assert.assertEquals("None", exiEvent.getName());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = exiEvent.getEventType();
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(eventTypeList.getLength() - 1, eventType.getIndex());
        Assert.assertNull(eventTypeList.getEE());
        
        eventType = eventTypeList.item(0);
        int i;
        for (i = 1; i < eventTypeList.getLength() - 1; i++) {
          EventType ith = eventTypeList.item(i);
          if (!(eventType.getName().compareTo(ith.getName()) < 0)) {
            Assert.assertEquals(eventType.getName(), ith.getName());
            Assert.assertTrue(eventType.getURI().compareTo(ith.getURI()) < 0);
          }
          eventType = ith;
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
        Assert.assertEquals("ABC", exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        Assert.assertEquals(4, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(5, eventTypeList.getLength());
        Assert.assertNotNull(eventTypeList.getEE());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(3, eventTypeList.getLength());
        Assert.assertNotNull(eventTypeList.getEE());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);
        Assert.assertEquals(0, eventType.getIndex());
      }
      
      Assert.assertEquals(5, n_events);
    }
  }

  /**
   * Schema:
   * <xsd:element name="Z">
   *   <xsd:complexType>
   *     <xsd:sequence>
   *       <xsd:element ref="foo:Z" minOccurs="0" />
   *       <xsd:sequence>
   *         <xsd:element ref="foo:C" minOccurs="0" maxOccurs="unbounded" />
   *       </xsd:sequence>
   *     </xsd:sequence>
   *   </xsd:complexType>
   * </xsd:element>
   * 
   * Instance:
   * <foo:Z>
   *   <goo:A />
   *   <goo:C />
   *   <aoo:B />
   *   <aoo:C />
   *   <foo:C />
   *   <foo:D />
   * </foo:Z>
   */
  public void testSubstitutionGroup_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/substitutionGroup.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String xmlString;
    byte[] bts;
    int n_events;
    
    xmlString =
      "<foo:Z xmlns:aoo='urn:aoo' xmlns:foo='urn:foo' xmlns:goo='urn:goo'>" +
        "<goo:A />" +
        "<goo:C />" +
        "<aoo:B />" +
        "<aoo:C />" +
        "<foo:C />" +
        "<foo:D />" +
      "</foo:Z>";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
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
      
      Assert.assertEquals(16, n_events);
  
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
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("A", exiEvent.getName());
      Assert.assertEquals("urn:goo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(8, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Z", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:aoo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("C", eventType.getName());
      Assert.assertEquals("urn:aoo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("C", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("C", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("D", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("C", exiEvent.getName());
      Assert.assertEquals("urn:goo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("C", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      Assert.assertEquals(4, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(7, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:aoo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("C", eventType.getName());
      Assert.assertEquals("urn:aoo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("C", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("D", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
  
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
  
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("B", exiEvent.getName());
      Assert.assertEquals("urn:aoo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:aoo", eventType.getURI());
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(7, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("C", eventType.getName());
      Assert.assertEquals("urn:aoo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("C", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("C", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("D", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
  
      exiEvent = exiEventList.get(7);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
  
      exiEvent = exiEventList.get(8);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("C", exiEvent.getName());
      Assert.assertEquals("urn:aoo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("C", eventType.getName());
      Assert.assertEquals("urn:aoo", eventType.getURI());
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(7, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:aoo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("C", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("C", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("D", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
  
      exiEvent = exiEventList.get(9);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
  
      exiEvent = exiEventList.get(10);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("C", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("C", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(7, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:aoo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("C", eventType.getName());
      Assert.assertEquals("urn:aoo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("C", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("D", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
  
      exiEvent = exiEventList.get(11);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
  
      exiEvent = exiEventList.get(12);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("D", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("D", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(5, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(7, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:aoo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("C", eventType.getName());
      Assert.assertEquals("urn:aoo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("C", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("C", eventType.getName());
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
  
      exiEvent = exiEventList.get(13);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
  
      exiEvent = exiEventList.get(14);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(6, eventType.getIndex());
  
      exiEvent = exiEventList.get(15);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
    }
  }
  
  /**
   * Schema: 
   * <xsd:element name="A" nillable="true">
   *   <xsd:complexType>
   *     <xsd:sequence>
   *       <xsd:element name="B"/>
   *     </xsd:sequence>
   *   </xsd:complexType>
   * </xsd:element>
   *
   * Instance:
   * <A xsi:nil='false'>
   *   <B/>
   * </A>
   */
  public void testNilFalse() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/nillable01.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    final String xmlString;
    byte[] bts;
    ByteArrayOutputStream baos;
    
    xmlString = 
      "<A xmlns='urn:foo' xsi:nil='false' \n" + 
      "   xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>\n" +
      "  <B/>\n" +
      "</A>\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      encoder.encode(new InputSource(new StringReader(xmlString)));
  
      bts = baos.toByteArray();

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
      
      Assert.assertEquals(8, n_events);
  
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
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
  
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_NL, exiEvent.getEventVariety());
      Assert.assertEquals("nil", exiEvent.getName());
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
      Assert.assertFalse(((EXIEventSchemaNil)exiEvent).isNilled());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      Assert.assertEquals("nil", eventType.getName());
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.getURI());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("B", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      Assert.assertEquals("nil", eventType.getName());
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.getURI());
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex()); // because of xsi:type
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
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
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
    }
  }

}
