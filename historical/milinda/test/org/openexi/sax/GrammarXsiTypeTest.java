package org.openexi.sax;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;

import junit.framework.Assert;

import org.openexi.proc.EXIDecoder;
import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EXIEvent;
import org.openexi.proc.common.EventCode;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventTypeList;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.common.XmlUriConst;
import org.openexi.proc.events.EXIEventSchemaNil;
import org.openexi.proc.events.EXIEventSchemaType;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.proc.io.Scanner;
import org.openexi.schema.EXISchema;
import org.openexi.schema.TestBase;
import org.openexi.scomp.EXISchemaFactoryErrorMonitor;
import org.openexi.scomp.EXISchemaFactoryTestUtil;
import org.xml.sax.InputSource;

public class GrammarXsiTypeTest extends TestBase {

  public GrammarXsiTypeTest(String name) {
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
   * <xsd:complexType name="restricted_B">
   *   <xsd:complexContent>
   *     <xsd:restriction base="foo:B">
   *       <xsd:sequence>
   *         <xsd:element ref="foo:AB"/>
   *         <xsd:element ref="foo:AC" minOccurs="0"/>
   *         <xsd:element ref="foo:AD" minOccurs="0"/>
   *       </xsd:sequence>
   *     </xsd:restriction>
   *   </xsd:complexContent>
   * </xsd:complexType>
   *
   * Instance:
   * <B xsi:type='restricted_B'>
   *   <AB/><AC/><AD/>
   * </B>
   */
  public void testXsiTypeStrict() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    String xmlString;
    
    xmlString = 
      "<B xmlns='urn:foo' xsi:type='restricted_B' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>\n" +
      "  <AB/><AC/><AD/>\n" +
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
      
      Assert.assertEquals(14, n_events);
      
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
      Assert.assertEquals(EXIEvent.EVENT_TP, exiEvent.getEventVariety());
      Assert.assertEquals("type", exiEvent.getName());
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
      Assert.assertEquals("restricted_B", ((EXIEventSchemaType)exiEvent).getTypeName());
      Assert.assertEquals("urn:foo", ((EXIEventSchemaType)exiEvent).getTypeURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      Assert.assertEquals("type", eventType.getName());
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.getURI());
      
      exiEvent = exiEventList.get(3);
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
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex()); // because of xsi:type
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);
      
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
  
      exiEvent = exiEventList.get(6);
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
  
      exiEvent = exiEventList.get(7);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);
      
      exiEvent = exiEventList.get(8);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
  
      exiEvent = exiEventList.get(9);
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
  
      exiEvent = exiEventList.get(10);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);
      
      exiEvent = exiEventList.get(11);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
  
      exiEvent = exiEventList.get(12);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      
      exiEvent = exiEventList.get(13);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
    }
    
    xmlString = 
      "<B xmlns='urn:foo' xsi:type='restricted_B' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>\n" +
      "  <AB/><AC/><AC/><AD/>\n" +
      "</B>\n";

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
   * <xsd:complexType name="restricted_B">
   *   <xsd:complexContent>
   *     <xsd:restriction base="foo:B">
   *       <xsd:sequence>
   *         <xsd:element ref="foo:AB"/>
   *         <xsd:element ref="foo:AC" minOccurs="0"/>
   *         <xsd:element ref="foo:AD" minOccurs="0"/>
   *       </xsd:sequence>
   *     </xsd:restriction>
   *   </xsd:complexContent>
   * </xsd:complexType>
   *
   * Instance:
   * <B xsi:type='restricted_B'>
   *   <AB/><AC/><AD/>
   * </B>
   */
  public void testXsiTypeDefault() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

    final String xmlString = 
      "<B xmlns='urn:foo' xsi:type='restricted_B' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>\n" +
      "  <AB> </AB><AC> </AC><AD> </AD>" +
      "</B>\n";
    
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
        
        ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();
    
        EXIEvent exiEvent;
        int n_events = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          exiEventList.add(exiEvent);
        }
        
        Assert.assertEquals(preserveWhitespaces ? 15 : 14, n_events);
        
        EventType eventType;
        EventTypeList eventTypeList;
        int n = 0;
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(1, eventTypeList.getLength());
        
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("B", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertNull(eventTypeList.getEE());
        
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_TP, exiEvent.getEventVariety());
        Assert.assertEquals("type", exiEvent.getName());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
        Assert.assertEquals("restricted_B", ((EXIEventSchemaType)exiEvent).getTypeName());
        Assert.assertEquals("urn:foo", ((EXIEventSchemaType)exiEvent).getTypeURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
        Assert.assertEquals("type", eventType.getName());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.getURI());
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(8, eventTypeList.getLength());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("AB", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
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
    
        if (preserveWhitespaces) {
          exiEvent = exiEventList.get(n++);
          Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
          Assert.assertEquals("\n  ", exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
          Assert.assertEquals(7, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(8, eventTypeList.getLength());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
          Assert.assertEquals("AB", eventType.getName());
          Assert.assertEquals("urn:foo", eventType.getURI());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(5);
          Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
          eventType = eventTypeList.item(6);
          Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        }
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
        Assert.assertEquals("AB", exiEvent.getName());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("AB", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        if (preserveWhitespaces) {
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(4, eventTypeList.getLength());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
        }
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
        Assert.assertEquals(" ", exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
        Assert.assertEquals(2, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(8, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
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
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(3, eventTypeList.getLength());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
        Assert.assertEquals("AC", exiEvent.getName());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("AC", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(5, eventTypeList.getLength());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("AD", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
        Assert.assertEquals(" ", exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
        Assert.assertEquals(2, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(8, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
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
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(3, eventTypeList.getLength());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
        Assert.assertEquals("AD", exiEvent.getName());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("AD", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(4, eventTypeList.getLength());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
        Assert.assertEquals(" ", exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
        Assert.assertEquals(2, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(8, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
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
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(3, eventTypeList.getLength());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(3, eventTypeList.getLength());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
        
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(1, eventTypeList.getLength());
      }
    }
  }
  
  /**
   * Schema: 
   * <xsd:complexType name="restricted_B">
   *   <xsd:complexContent>
   *     <xsd:restriction base="foo:B">
   *       <xsd:sequence>
   *         <xsd:element ref="foo:AB"/>
   *         <xsd:element ref="foo:AC" minOccurs="0"/>
   *         <xsd:element ref="foo:AD" minOccurs="0"/>
   *       </xsd:sequence>
   *     </xsd:restriction>
   *   </xsd:complexContent>
   * </xsd:complexType>
   *
   * <xsd:element name="nillable_B" type="foo:B" nillable="true" />
   * 
   * Instance:
   * <nillable_B xmlns='urn:foo' xsi:type='restricted_B' xsi:nil='true' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'/>
   * 
   * xsi:type and xsi:nil='true' cannot co-occur in strict schema mode.
   */
  public void testXsiTypeNillableStrict() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    String xmlString;
    byte[] bts;
    ByteArrayOutputStream baos;
    boolean caught;
    ArrayList<EXIEvent> exiEventList;
    EXIEvent exiEvent;
    int n_events;
    EventType eventType;
    EventTypeList eventTypeList;
    
    xmlString = 
      "<nillable_B xmlns='urn:foo' xsi:type='restricted_B' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>\n" +
      "  <AB/>\n" +
      "</nillable_B>\n";
    
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
      
      Assert.assertEquals(8, n_events);
  
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
      Assert.assertEquals("nillable_B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
  
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_TP, exiEvent.getEventVariety());
      Assert.assertEquals("type", exiEvent.getName());
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
      Assert.assertEquals("restricted_B", ((EXIEventSchemaType)exiEvent).getTypeName());
      Assert.assertEquals("urn:foo", ((EXIEventSchemaType)exiEvent).getTypeURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      Assert.assertEquals("type", eventType.getName());
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.getURI());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      
      exiEvent = exiEventList.get(3);
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
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AC", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AD", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      
      exiEvent = exiEventList.get(7);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
    }

    xmlString = 
      "<nillable_B xmlns='urn:foo' xsi:nil='true' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>\n" +
      "  <AB/>\n" +
      "</nillable_B>\n";

    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      
      encoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      baos = new ByteArrayOutputStream();
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

    xmlString = 
      "<nillable_B xmlns='urn:foo' xsi:nil='true' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>\n" +
      "</nillable_B>\n";

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
      
      n_events = 0;
      exiEventList = new ArrayList<EXIEvent>();
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        exiEventList.add(exiEvent);
      }
      
      Assert.assertEquals(5, n_events);
  
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
      Assert.assertEquals("nillable_B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
  
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_NL, exiEvent.getEventVariety());
      Assert.assertEquals("nil", exiEvent.getName());
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
      Assert.assertTrue(((EXIEventSchemaNil)exiEvent).isNilled());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      Assert.assertEquals("nil", eventType.getName());
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.getURI());
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
    }
    
    // xsi:type and xsi:nil cannot co-occur in strict schema mode.
    xmlString = 
      "<nillable_B xmlns='urn:foo' xsi:type='restricted_B' xsi:nil='true' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>\n" +
      "</nillable_B>\n";

    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      
      encoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
  
      caught = false;
      try {
        encoder.encode(new InputSource(new StringReader(xmlString)));
      }
      catch (TransmogrifierException eee) {
        caught = true;
        Assert.assertEquals(TransmogrifierException.UNEXPECTED_ATTR, eee.getCode());
      }
      Assert.assertTrue(caught);
    }
  }

  /**
   * Schema: 
   * <xsd:complexType name="restricted_B">
   *   <xsd:complexContent>
   *     <xsd:restriction base="foo:B">
   *       <xsd:sequence>
   *         <xsd:element ref="foo:AB"/>
   *         <xsd:element ref="foo:AC" minOccurs="0"/>
   *         <xsd:element ref="foo:AD" minOccurs="0"/>
   *       </xsd:sequence>
   *     </xsd:restriction>
   *   </xsd:complexContent>
   * </xsd:complexType>
   *
   * <xsd:element name="nillable_B" type="foo:B" nillable="true" />
   * 
   * Instance:
   * <nillable_B xmlns='urn:foo' xsi:type='restricted_B' xsi:nil='true' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'/>
   * 
   * xsi:type and xsi:nil='true' can occur together in default (i.e. non-strict) schema mode.
   */
  public void testXsiTypeNillableDefault() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

    String xmlString;
    byte[] bts;
    ByteArrayOutputStream baos;

    EXIEvent exiEvent;
    int n_events = 0;
    
    ArrayList<EXIEvent> exiEventList;

    EventType eventType;
    EventTypeList eventTypeList;

    for (AlignmentType alignment : Alignments) {
      for (boolean preserveWhitespaces : new boolean[] { true, false }) {
        xmlString = 
          "<nillable_B xmlns='urn:foo' xsi:type='restricted_B' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>\n" +
          "  <AB> </AB>" +
          "</nillable_B>\n";
        
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;
        
        encoder.setAlignmentType(alignment);
        decoder.setAlignmentType(alignment);
  
        encoder.setEXISchema(grammarCache);
        encoder.setPreserveWhitespaces(preserveWhitespaces);
        
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
        
        Assert.assertEquals(preserveWhitespaces ? 9 : 8, n_events);
    
        int n = 0;
        
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(1, eventTypeList.getLength());
        
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("nillable_B", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertNull(eventTypeList.getEE());
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_TP, exiEvent.getEventVariety());
        Assert.assertEquals("type", exiEvent.getName());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
        Assert.assertEquals("restricted_B", ((EXIEventSchemaType)exiEvent).getTypeName());
        Assert.assertEquals("urn:foo", ((EXIEventSchemaType)exiEvent).getTypeURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
        Assert.assertEquals("type", eventType.getName());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.getURI());
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(8, eventTypeList.getLength());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("AB", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
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
    
        if (preserveWhitespaces) {
          exiEvent = exiEventList.get(n++);
          Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
          Assert.assertEquals("\n  ", exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
          Assert.assertEquals(7, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(8, eventTypeList.getLength());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
          Assert.assertEquals("AB", eventType.getName());
          Assert.assertEquals("urn:foo", eventType.getURI());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(5);
          Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
          eventType = eventTypeList.item(6);
          Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        }
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
        Assert.assertEquals("AB", exiEvent.getName());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("AB", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        if (preserveWhitespaces) {
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(4, eventTypeList.getLength());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
        }
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
        Assert.assertEquals(" ", exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
        Assert.assertEquals(2, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(8, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
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
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(3, eventTypeList.getLength());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
        Assert.assertEquals(2, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(5, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("AC", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("AD", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(1, eventTypeList.getLength());
      }
    }
    
    for (AlignmentType alignment : Alignments) {
      for (boolean preserveWhitespaces : new boolean[] { true, false }) {
        xmlString = 
          "<nillable_B xmlns='urn:foo' xsi:nil='true' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>\n" +
          "</nillable_B>\n";
    
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;
        
        encoder.setAlignmentType(alignment);
        decoder.setAlignmentType(alignment);
  
        encoder.setEXISchema(grammarCache);
        encoder.setPreserveWhitespaces(preserveWhitespaces);
  
        baos = new ByteArrayOutputStream();
        encoder.setOutputStream(baos);
  
        encoder.encode(new InputSource(new StringReader(xmlString)));
    
        bts = baos.toByteArray();
        
        decoder.setEXISchema(grammarCache);
        decoder.setInputStream(new ByteArrayInputStream(bts));
        scanner = decoder.processHeader();
        
        n_events = 0;
        exiEventList = new ArrayList<EXIEvent>();
        while ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          exiEventList.add(exiEvent);
        }
        
        Assert.assertEquals(preserveWhitespaces ? 6 : 5, n_events);
        
        int n = 0;
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(1, eventTypeList.getLength());
        
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("nillable_B", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertNull(eventTypeList.getEE());
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_NL, exiEvent.getEventVariety());
        Assert.assertEquals("nil", exiEvent.getName());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
        Assert.assertTrue(((EXIEventSchemaNil)exiEvent).isNilled());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
        Assert.assertEquals("nil", eventType.getName());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.getURI());
        Assert.assertEquals(1, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(8, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("AB", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
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
    
        if (preserveWhitespaces) {
          exiEvent = exiEventList.get(n++);
          Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
          Assert.assertEquals("\n", exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
          Assert.assertEquals(6, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(7, eventTypeList.getLength());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
          eventType = eventTypeList.item(5);
          Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        }
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
        if (preserveWhitespaces) {
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(3, eventTypeList.getLength());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
        }
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(1, eventTypeList.getLength());
      }
    }
    
    for (AlignmentType alignment : Alignments) {
      for (boolean preserveWhitespaces : new boolean[] { true, false }) {
        xmlString = 
          "<nillable_B xmlns='urn:foo' xsi:type='restricted_B' xsi:nil='true' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>\n" +
          "</nillable_B>\n";
        
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;
        
        encoder.setAlignmentType(alignment);
        decoder.setAlignmentType(alignment);
  
        encoder.setEXISchema(grammarCache);
        encoder.setPreserveWhitespaces(preserveWhitespaces);
  
        baos = new ByteArrayOutputStream();
        encoder.setOutputStream(baos);
    
        // xsi:type and xsi:nil cannot co-occur in strict schema mode, but are permitted to
        // occur together otherwise.
        encoder.encode(new InputSource(new StringReader(xmlString)));
    
        bts = baos.toByteArray();
        
        decoder.setEXISchema(grammarCache);
        decoder.setInputStream(new ByteArrayInputStream(bts));
        scanner = decoder.processHeader();
        
        n_events = 0;
        exiEventList = new ArrayList<EXIEvent>();
        while ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          exiEventList.add(exiEvent);
        }
        
        Assert.assertEquals(preserveWhitespaces ? 7 : 6, n_events);
        
        int n = 0;
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(1, eventTypeList.getLength());
        
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("nillable_B", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertNull(eventTypeList.getEE());
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_TP, exiEvent.getEventVariety());
        Assert.assertEquals("type", exiEvent.getName());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
        Assert.assertEquals("restricted_B", ((EXIEventSchemaType)exiEvent).getTypeName());
        Assert.assertEquals("urn:foo", ((EXIEventSchemaType)exiEvent).getTypeURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
        Assert.assertEquals("type", eventType.getName());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.getURI());
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(8, eventTypeList.getLength());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("AB", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
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
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_NL, exiEvent.getEventVariety());
        Assert.assertEquals("nil", exiEvent.getName());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
        Assert.assertTrue(((EXIEventSchemaNil)exiEvent).isNilled());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
        Assert.assertEquals("nil", eventType.getName());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.getURI());
        Assert.assertEquals(1, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(8, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("AB", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
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
    
        if (preserveWhitespaces) {
          exiEvent = exiEventList.get(n++);
          Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
          Assert.assertEquals(6, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(7, eventTypeList.getLength());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
          eventType = eventTypeList.item(5);
          Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        }
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
        if (preserveWhitespaces) {
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(3, eventTypeList.getLength());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      
          exiEvent = exiEventList.get(n++);
          Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
          eventType = exiEvent.getEventType();
          Assert.assertSame(exiEvent, eventType);
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(1, eventTypeList.getLength());
        }
      }
    }
  }
  
  /**
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
   * <C xsi:type="foo:B" xmlns="urn:foo">
   *   <AB/><AD/>
   * </C>
   * 
   * where
   * 
   * <xsd:complexType name="B">
   *   <xsd:sequence>
   *     <xsd:element ref="foo:AB"/>
   *     <xsd:element ref="foo:AC" minOccurs="0" maxOccurs="2"/>
   *     <xsd:element ref="foo:AD" minOccurs="0"/>
   *   </xsd:sequence>
   * </xsd:complexType>
   * 
   * Use of xsi:type is permitted in default mode, even though it would
   * not have been permitted in strict mode. 
   */
  public void testLenientXsiType_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache;
    
    grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
    
    final String xmlString = 
      "<C xsi:type='B' xmlns='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>" +
        "<AB> </AB><AD> </AD>" +
      "</C>\n";
  
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
      
      Assert.assertEquals(11, n_events);
  
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
      Assert.assertEquals(EXIEvent.EVENT_TP, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AC", eventType.getName());
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
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AB", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(8, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
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
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals(" ", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(8, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
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
  
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AD", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AD", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AC", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(7);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals(" ", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(8, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
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
  
      exiEvent = exiEventList.get(8);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(9);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(10);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
    }
     
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      
      encoder.setAlignmentType(alignment);

      boolean caught;
  
      grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
      
      encoder.setEXISchema(grammarCache);
      encoder.setOutputStream(new ByteArrayOutputStream());
  
      caught = false;
      try {
        encoder.encode(new InputSource(new StringReader(xmlString)));
      }
      catch (TransmogrifierException eee) {
        caught = true;
        Assert.assertEquals(TransmogrifierException.UNEXPECTED_ATTR, eee.getCode());
      }
      Assert.assertTrue(caught);
    }
  }
  
  /**
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
   * <C xsi:type="finalString" xmlns="urn:foo">xyz</C>
   * 
   * where
   * 
   * <xsd:simpleType name="finalString" final="#all">
   *   <xsd:restriction base="xsd:string" />
   * </xsd:simpleType>
   * 
   * Use of xsi:type is permitted in default mode, even though it would
   * not have been permitted in strict mode. 
   */
  public void testLenientXsiType_02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache;
    
    grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
    
    final String xmlString = 
      "<C xsi:type='finalString' xmlns='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>" +
        "xyz" +
      "</C>\n";
  
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
      
      Assert.assertEquals(6, n_events);
  
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
      Assert.assertEquals(EXIEvent.EVENT_TP, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AC", eventType.getName());
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
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("xyz", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      Assert.assertEquals("finalString", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(8, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
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
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
    }
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      
      encoder.setAlignmentType(alignment);

      boolean caught;
  
      grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
      
      encoder.setEXISchema(grammarCache);
      encoder.setOutputStream(new ByteArrayOutputStream());
  
      caught = false;
      try {
        encoder.encode(new InputSource(new StringReader(xmlString)));
      }
      catch (TransmogrifierException eee) {
        caught = true;
        Assert.assertEquals(TransmogrifierException.UNEXPECTED_ATTR, eee.getCode());
      }
      Assert.assertTrue(caught);
    }
  }
  
  /**
   * Schema:
   * <xsd:element name="K">
   *   <xsd:simpleType>
   *     <xsd:restriction base="xsd:string" />
   *   </xsd:simpleType>
   * </xsd:element>
   *
   * Instance:
   * <K xsi:type="foo:C" xmlns="urn:foo">
   *   <AB/><AD/>
   * </K>
   * 
   * where
   * 
   * <xsd:complexType name="C">
   *   <xsd:all>
   *     <xsd:element ref="foo:AB" minOccurs="0" />
   *     <xsd:element ref="foo:AC" minOccurs="0" />
   *   </xsd:all>
   * </xsd:complexType>
   * 
   * Use of xsi:type is permitted in default mode, even though it would
   * not have been permitted in strict mode. 
   */
  public void testLenientXsiType_03() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache;
    
    grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
    
    final String xmlString = 
      "<K xsi:type='C' xmlns='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>\n" +
      "</K>\n";

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
        
        ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();
        
        EXIEvent exiEvent;
        int n_events = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          exiEventList.add(exiEvent);
        }
        
        Assert.assertEquals(preserveWhitespaces ? 6 : 5, n_events);
    
        EventType eventType;
        EventTypeList eventTypeList;
        int n = 0;
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(1, eventTypeList.getLength());
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("K", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertNull(eventTypeList.getEE());
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_TP, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(8, eventTypeList.getLength());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
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
    
        if (preserveWhitespaces) {
          exiEvent = exiEventList.get(n++);
          Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
          Assert.assertEquals("\n", exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
          Assert.assertEquals(8, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(9, eventTypeList.getLength());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
          Assert.assertEquals("AB", eventType.getName());
          Assert.assertEquals("urn:foo", eventType.getURI());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
          Assert.assertEquals("AC", eventType.getName());
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
        }
        
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
        if (preserveWhitespaces) {
          Assert.assertEquals(2, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(5, eventTypeList.getLength());
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
        }
        
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(1, eventTypeList.getLength());
      }
    }
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      
      encoder.setAlignmentType(alignment);
    
      boolean caught;
  
      grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
      
      encoder.setEXISchema(grammarCache);
      encoder.setOutputStream(new ByteArrayOutputStream());
  
      caught = false;
      try {
        encoder.encode(new InputSource(new StringReader(xmlString)));
      }
      catch (TransmogrifierException eee) {
        caught = true;
        Assert.assertEquals(TransmogrifierException.UNEXPECTED_ATTR, eee.getCode());
      }
      Assert.assertTrue(caught);
    }
  }
  
  /**
   * Schema: 
   * <xsd:simpleType name="unionedEnum">
   *   <xsd:restriction>
   *     <xsd:simpleType>
   *       <xsd:union memberTypes="xsd:int xsd:NMTOKEN"/>
   *     </xsd:simpleType>
   *     <xsd:enumeration value="100"/>
   *     <xsd:enumeration value="Tokyo"/>
   *     <xsd:enumeration value="101"/>
   *   </xsd:restriction>
   * </xsd:simpleType>
   * 
   * <xsd:element name="unionedEnum" type="foo:unionedEnum" />
   *
   * Instance:
   * <foo:unionedEnum xmlns:foo='urn:foo' xmlns:xsd='http://www.w3.org/2001/XMLSchema' 
   *   xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' 
   *   xsi:type='xsd:int'>12345</foo:unionedEnum>
   */
  public void testXsiTypeOnElementOfUnion() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/interop/schemaInformedGrammar/undeclaredProductions/union.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    byte[] bts;
    ByteArrayOutputStream baos;
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      URL url = resolveSystemIdAsURL("/interop/schemaInformedGrammar/undeclaredProductions/xsiTypeStrict-03.xml");
      InputSource inputSource = new InputSource(url.toString());
      inputSource.setByteStream(url.openStream());

      encoder.encode(inputSource);
  
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
      
      Assert.assertEquals(6, n_events);
  
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
      Assert.assertEquals("unionedEnum", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());

      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_TP, exiEvent.getEventVariety());
      Assert.assertEquals("type", exiEvent.getName());
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
      Assert.assertEquals("int", ((EXIEventSchemaType)exiEvent).getTypeName());
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI, ((EXIEventSchemaType)exiEvent).getTypeURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      Assert.assertEquals("type", eventType.getName());
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.getURI());
      
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("12345", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex()); // because of xsi:type
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
  
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
    }
  }

  /**
   * EXI interoperability test case "qname.invalid-02" 
   */
  public void testInvalidQName_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/exi/qname-invalid.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

    final String xmlString = 
      "<root QName='xsi:type' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:xsd='http://www.w3.org/2001/XMLSchema'>\n" +
      "  <QName xmlns:test='http://example.org/test'>test:qname</QName>\n" +
      "  <QName xsi:type='xsd:QName'>test:qname</QName>\n" +
      "  <QName xmlns:test='http://example.org/test' xsi:type='test:QName'>test:qname</QName>\n" +
      "</root>";
      
    for (AlignmentType alignment : Alignments) {
      for (boolean preserveWhitespaces : new boolean[] { true, false }) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;
        
        encoder.setAlignmentType(alignment);
        decoder.setAlignmentType(alignment);
  
        encoder.setEXISchema(grammarCache);
        encoder.setPreserveWhitespaces(preserveWhitespaces);
        byte[] bts;
        ByteArrayOutputStream baos;
        
        baos = new ByteArrayOutputStream();
        encoder.setOutputStream(baos);
    
        encoder.encode(new InputSource(new StringReader(xmlString)));
    
        bts = baos.toByteArray();
        
        EXISchemaFactoryTestUtil.serializeBytes(bts, "/exi/qname-invalid.xsd", "qname-invalid-00.xml.exi", getClass());
        
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
        
        Assert.assertEquals(preserveWhitespaces ? 20 : 16, n_events);
    
        EventType eventType;
        EventTypeList eventTypeList;
        int n = 0;
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(1, eventTypeList.getLength());
        
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("root", eventType.getName());
        Assert.assertEquals("", eventType.getURI());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertNull(eventTypeList.getEE());
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
        Assert.assertEquals("QName", exiEvent.getName());
        Assert.assertEquals("", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.assertEquals("QName", eventType.getName());
        Assert.assertEquals("", eventType.getURI());
        Assert.assertEquals(2, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(11, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("QName", eventType.getName());
        Assert.assertEquals("", eventType.getURI());
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
        Assert.assertEquals("QName", eventType.getName());
        Assert.assertEquals("", eventType.getURI());
        eventType = eventTypeList.item(8);
        Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
        eventType = eventTypeList.item(9);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(10);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
    
        n += preserveWhitespaces ? 2 : 1;
        
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
        Assert.assertEquals("test:qname", exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
        Assert.assertEquals(2, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(8, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
        eventType = eventTypeList.item(6);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
    
        n += preserveWhitespaces ? 3 : 2;
        
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_TP, exiEvent.getEventVariety());
        Assert.assertEquals("type", exiEvent.getName());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
        Assert.assertEquals("QName", ((EXIEventSchemaType)exiEvent).getTypeName());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI, ((EXIEventSchemaType)exiEvent).getTypeURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
        Assert.assertEquals("type", eventType.getName());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.getURI());
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(8, eventTypeList.getLength());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
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
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
        Assert.assertEquals("test:qname", exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
        Assert.assertEquals(2, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(8, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
        eventType = eventTypeList.item(6);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
    
        n += preserveWhitespaces ? 3 : 2;
        
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_TP, exiEvent.getEventVariety());
        Assert.assertEquals("type", exiEvent.getName());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
        Assert.assertEquals("QName", ((EXIEventSchemaType)exiEvent).getTypeName());
        Assert.assertEquals("http://example.org/test", ((EXIEventSchemaType)exiEvent).getTypeURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
        Assert.assertEquals("type", eventType.getName());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.getURI());
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(8, eventTypeList.getLength());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
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
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
        Assert.assertEquals("test:qname", exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
        Assert.assertEquals(2, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(8, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
        eventType = eventTypeList.item(6);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
    
        n += preserveWhitespaces ? 2 : 1;
        
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
        Assert.assertEquals(2, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(5, eventTypeList.getLength());
    
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("QName", eventType.getName());
        Assert.assertEquals("", eventType.getURI());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
        
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(1, eventTypeList.getLength());
      }
    }
  }

}
