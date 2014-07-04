package org.openexi.sax;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.net.URL;

import junit.framework.Assert;

import org.openexi.proc.EXIDecoder;
import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EventDescription;
import org.openexi.proc.common.EventCode;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventTypeList;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.common.XmlUriConst;
import org.openexi.proc.events.EXIEventSchemaType;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.proc.io.Scanner;
import org.openexi.schema.EXISchema;
import org.openexi.schema.TestBase;
import org.openexi.scomp.EXISchemaFactoryErrorMonitor;
import org.openexi.scomp.EXISchemaFactoryTestUtil;
import org.xml.sax.InputSource;

public class GrammarAttributeWildcardTest extends TestBase {

  public GrammarAttributeWildcardTest(String name) {
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
   * <xsd:element name="I" nillable="true">
   *   <xsd:complexType>
   *     <xsd:sequence>
   *       <xsd:element name="A">
   *         <xsd:complexType>
   *           <xsd:simpleContent>
   *             <xsd:extension base="xsd:anySimpleType">
   *               <xsd:anyAttribute namespace="urn:eoo urn:goo ##local" />
   *             </xsd:extension>
   *           </xsd:simpleContent>
   *         </xsd:complexType>
   *       </xsd:element>
   *     </xsd:sequence>
   *     <xsd:attribute ref="foo:aB" />
   *     <xsd:attribute ref="foo:aD" use="required" />
   *     <xsd:anyAttribute namespace="##any" />
   *   </xsd:complexType>
   * </xsd:element>
   */
  public void testAcceptanceForI_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    ByteArrayOutputStream baos;
    
    String xmlString;
    byte[] bts;
    int n_events;

    EventDescription exiEvent;

    EventType eventType;
    EventTypeList eventTypeList;

    xmlString = 
      "<foo:I xmlns:foo='urn:foo' xmlns:goo='urn:goo' \n" +
      "       goo:aA='' foo:aB='' foo:aD=''  >\n" +
      "  <A aZ='' />\n" +
      "</foo:I>\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setGrammarCache(grammarCache);
      baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
  
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
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
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("I", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
      Assert.assertEquals("aA", exiEvent.getName());
      Assert.assertEquals("urn:goo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertNull(eventType.uri);
      Assert.assertNull(eventType.name);
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(4, eventTypeList.getLength());
      Assert.assertNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
      Assert.assertEquals("nil", eventType.name);
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aB", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aD", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
      Assert.assertEquals("aB", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aB", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(4, eventTypeList.getLength());
      Assert.assertNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
      Assert.assertEquals("nil", eventType.name);
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aD", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertNull(eventType.uri);
      Assert.assertNull(eventType.name);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
      Assert.assertEquals("aD", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aD", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getLength());
      Assert.assertNull(eventTypeList.getEE());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertNull(eventType.uri);
      Assert.assertNull(eventType.name);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("A", exiEvent.getName());
      Assert.assertEquals("", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("", eventType.uri);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getLength());
      Assert.assertNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertNull(eventType.uri);
      Assert.assertNull(eventType.name);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
      Assert.assertEquals("aZ", exiEvent.getName());
      Assert.assertEquals("", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
      Assert.assertEquals("", eventType.uri);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(4, eventTypeList.getLength());
      Assert.assertNull(eventTypeList.getEE());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:eoo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(4, eventTypeList.getLength());
      Assert.assertNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
      Assert.assertEquals("", eventType.uri);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:eoo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:goo", eventType.uri);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      ++n_events;
      
      Assert.assertEquals(11, n_events);
    }
    
    xmlString = 
      "<foo:I xmlns:foo='urn:foo' xmlns:goo='urn:goo' \n" +
      "       foo:aB='' goo:aC='' foo:aD=''  >\n" +
      "  <A aZ='' />\n" +
      "</foo:I>\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setGrammarCache(grammarCache);
      baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
  
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
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
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("I", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
      Assert.assertEquals("aB", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aB", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(4, eventTypeList.getLength());
      Assert.assertNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
      Assert.assertEquals("nil", eventType.name);
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aD", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertNull(eventType.uri);
      Assert.assertNull(eventType.name);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
      Assert.assertEquals("aC", exiEvent.getName());
      Assert.assertEquals("urn:goo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertNull(eventType.uri);
      Assert.assertNull(eventType.name);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getLength());
      Assert.assertNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aD", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
      Assert.assertEquals("aD", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aD", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getLength());
      Assert.assertNull(eventTypeList.getEE());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertNull(eventType.uri);
      Assert.assertNull(eventType.name);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("A", exiEvent.getName());
      Assert.assertEquals("", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("", eventType.uri);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getLength());
      Assert.assertNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertNull(eventType.uri);
      Assert.assertNull(eventType.name);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
      Assert.assertEquals("aZ", exiEvent.getName());
      Assert.assertEquals("", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
      Assert.assertEquals("", eventType.uri);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(4, eventTypeList.getLength());
      Assert.assertNull(eventTypeList.getEE());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:eoo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(4, eventTypeList.getLength());
      Assert.assertNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
      Assert.assertEquals("", eventType.uri);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:eoo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:goo", eventType.uri);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      ++n_events;

      Assert.assertEquals(11, n_events);
    }

    xmlString = 
      "<foo:I xmlns:foo='urn:foo' xmlns:goo='urn:goo' \n" +
      "       foo:aB='' foo:aD='' goo:aE='' >\n" +
      "  <A aZ='' />\n" +
      "</foo:I>\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setGrammarCache(grammarCache);
      baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
  
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
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
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("I", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
      Assert.assertEquals("aB", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aB", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(4, eventTypeList.getLength());
      Assert.assertNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
      Assert.assertEquals("nil", eventType.name);
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aD", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertNull(eventType.uri);
      Assert.assertNull(eventType.name);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
      Assert.assertEquals("aD", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aD", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getLength());
      Assert.assertNull(eventTypeList.getEE());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertNull(eventType.uri);
      Assert.assertNull(eventType.name);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
      Assert.assertEquals("aE", exiEvent.getName());
      Assert.assertEquals("urn:goo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertNull(eventType.uri);
      Assert.assertNull(eventType.name);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getLength());
      Assert.assertNull(eventTypeList.getEE());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("", eventType.uri);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("A", exiEvent.getName());
      Assert.assertEquals("", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("", eventType.uri);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getLength());
      Assert.assertNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertNull(eventType.uri);
      Assert.assertNull(eventType.name);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
      Assert.assertEquals("aZ", exiEvent.getName());
      Assert.assertEquals("", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
      Assert.assertEquals("", eventType.uri);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(4, eventTypeList.getLength());
      Assert.assertNull(eventTypeList.getEE());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:eoo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(4, eventTypeList.getLength());
      Assert.assertNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
      Assert.assertEquals("", eventType.uri);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:eoo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:goo", eventType.uri);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      ++n_events;
      
      Assert.assertEquals(11, n_events);
    }

    xmlString = 
      "<foo:I xmlns:foo='urn:foo' xmlns:goo='urn:goo' xsi:nil='true' \n" +
      "       xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' \n" +
      "       foo:aB='' foo:aD='' goo:aE='' >\n" +
      "</foo:I>\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setGrammarCache(grammarCache);
      baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
  
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
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
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("I", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_NL, exiEvent.getEventKind());
      Assert.assertEquals("nil", exiEvent.getName());
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
      Assert.assertEquals("nil", eventType.name);
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(4, eventTypeList.getLength());
      Assert.assertNull(eventTypeList.getEE());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aB", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aD", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertNull(eventType.uri);
      Assert.assertNull(eventType.name);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
      Assert.assertEquals("aB", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aB", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3, eventTypeList.getLength());
      Assert.assertNull(eventTypeList.getEE());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aD", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertNull(eventType.uri);
      Assert.assertNull(eventType.name);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
      Assert.assertEquals("aD", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aD", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getLength());
      Assert.assertNull(eventTypeList.getEE());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertNull(eventType.uri);
      Assert.assertNull(eventType.name);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
      Assert.assertEquals("aE", exiEvent.getName());
      Assert.assertEquals("urn:goo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertNull(eventType.uri);
      Assert.assertNull(eventType.name);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertNull(eventType.uri);
      Assert.assertNull(eventType.name);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      ++n_events;
      
      Assert.assertEquals(8, n_events);
    }
  }

  /**
   * Schema:
   * <xsd:element name="I" nillable="true">
   *   <xsd:complexType>
   *     <xsd:sequence>
   *       <xsd:element name="A">
   *         <xsd:complexType>
   *           <xsd:simpleContent>
   *             <xsd:extension base="xsd:anySimpleType">
   *               <xsd:anyAttribute namespace="urn:eoo urn:goo ##local" />
   *             </xsd:extension>
   *           </xsd:simpleContent>
   *         </xsd:complexType>
   *       </xsd:element>
   *     </xsd:sequence>
   *     <xsd:attribute ref="foo:aB" />
   *     <xsd:attribute ref="foo:aD" use="required" />
   *     <xsd:anyAttribute namespace="##any" />
   *   </xsd:complexType>
   * </xsd:element>
   */
  public void testAcceptanceForI_02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

    String xmlString;
    
    ByteArrayOutputStream baos;
    byte[] bts;
    int n_events;

    EventDescription exiEvent;

    EventType eventType;
    EventTypeList eventTypeList;

    for (AlignmentType alignment : Alignments) {
      xmlString = 
        "<foo:I xmlns:foo='urn:foo' xmlns:goo='urn:goo' \n" +
        "       goo:aA='' foo:aB='' foo:aD=''  >" +
          "<A aZ=''>xyz</A>" +
        "</foo:I>\n";
      
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setGrammarCache(grammarCache);
      
      baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
  
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      n_events = 0;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("I", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
      Assert.assertEquals("aA", exiEvent.getName());
      Assert.assertEquals("urn:goo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertNull(eventType.uri);
      Assert.assertNull(eventType.name);
      Assert.assertEquals(4, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(12, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
      Assert.assertEquals("nil", eventType.name);
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aB", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aD", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertSame(eventType, eventTypeList.getEE());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("aB", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("aD", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(11);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
      Assert.assertEquals("aB", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aB", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(12, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
      Assert.assertEquals("nil", eventType.name);
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aD", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertNull(eventType.uri);
      Assert.assertNull(eventType.name);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertSame(eventType, eventTypeList.getEE());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("aB", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("aD", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(11);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
      Assert.assertEquals("aD", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aD", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(8, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertNull(eventType.uri);
      Assert.assertNull(eventType.name);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertSame(eventType, eventTypeList.getEE());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("aD", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("A", exiEvent.getName());
      Assert.assertEquals("", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("", eventType.uri);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(7, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertNull(eventType.uri);
      Assert.assertNull(eventType.name);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertSame(eventType, eventTypeList.getEE());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
      Assert.assertEquals("aZ", exiEvent.getName());
      Assert.assertEquals("", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
      Assert.assertEquals("", eventType.uri);
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(11, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:eoo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("xyz", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      Assert.assertEquals(5, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(11, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
      Assert.assertEquals("", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:eoo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertSame(eventType, eventTypeList.getEE());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      ++n_events;
      
      Assert.assertEquals(11, n_events);
    }
    
    for (AlignmentType alignment : Alignments) {
      xmlString = 
        "<foo:I xmlns:foo='urn:foo' xmlns:goo='urn:goo' \n" +
        "       foo:aB='' goo:aC='' foo:aD=''  >" +
          "<A aZ=''>xyz</A>" +
        "</foo:I>\n";
      
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);
    
      baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      encoder.setGrammarCache(grammarCache);
  
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      n_events = 0;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("I", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
      Assert.assertEquals("aB", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aB", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(12, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
      Assert.assertEquals("nil", eventType.name);
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aD", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertNull(eventType.uri);
      Assert.assertNull(eventType.name);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertSame(eventType, eventTypeList.getEE());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("aB", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("aD", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(11);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
      Assert.assertEquals("aC", exiEvent.getName());
      Assert.assertEquals("urn:goo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertNull(eventType.uri);
      Assert.assertNull(eventType.name);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(8, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aD", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertSame(eventType, eventTypeList.getEE());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("aD", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
      Assert.assertEquals("aD", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aD", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(8, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertNull(eventType.uri);
      Assert.assertNull(eventType.name);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertSame(eventType, eventTypeList.getEE());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("aD", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("A", exiEvent.getName());
      Assert.assertEquals("", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("", eventType.uri);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(7, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertNull(eventType.uri);
      Assert.assertNull(eventType.name);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertSame(eventType, eventTypeList.getEE());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
      Assert.assertEquals("aZ", exiEvent.getName());
      Assert.assertEquals("", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
      Assert.assertEquals("", eventType.uri);
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(11, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:eoo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertSame(eventType, eventTypeList.getEE());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("xyz", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      Assert.assertEquals(5, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(11, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
      Assert.assertEquals("", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:eoo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertSame(eventType, eventTypeList.getEE());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      ++n_events;
      
      Assert.assertEquals(11, n_events);
    }

    for (AlignmentType alignment : Alignments) {
      xmlString = 
        "<foo:I xmlns:foo='urn:foo' xmlns:goo='urn:goo' \n" +
        "       foo:aB='' foo:aD='' goo:aE='' >" +
          "<A aZ=''>xyz</A>" +
        "</foo:I>\n";
      
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);
    
      baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
  
      encoder.setGrammarCache(grammarCache);
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      n_events = 0;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("I", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
      Assert.assertEquals("aB", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aB", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(12, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
      Assert.assertEquals("nil", eventType.name);
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aD", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertNull(eventType.uri);
      Assert.assertNull(eventType.name);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertSame(eventType, eventTypeList.getEE());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("aB", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("aD", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(11);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
      Assert.assertEquals("aD", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aD", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(8, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertNull(eventType.uri);
      Assert.assertNull(eventType.name);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertSame(eventType, eventTypeList.getEE());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("aD", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
      Assert.assertEquals("aE", exiEvent.getName());
      Assert.assertEquals("urn:goo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertNull(eventType.uri);
      Assert.assertNull(eventType.name);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(7, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertSame(eventType, eventTypeList.getEE());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("A", exiEvent.getName());
      Assert.assertEquals("", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.name);
      Assert.assertEquals("", eventType.uri);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(7, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertNull(eventType.uri);
      Assert.assertNull(eventType.name);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertSame(eventType, eventTypeList.getEE());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
      Assert.assertEquals("aZ", exiEvent.getName());
      Assert.assertEquals("", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
      Assert.assertEquals("", eventType.uri);
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(11, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:eoo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertSame(eventType, eventTypeList.getEE());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("xyz", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      Assert.assertEquals(5, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(11, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
      Assert.assertEquals("", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:eoo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:goo", eventType.uri);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertSame(eventType, eventTypeList.getEE());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      ++n_events;
      
      Assert.assertEquals(11, n_events);
    }

    for (AlignmentType alignment : Alignments) {
      xmlString = 
        "<foo:I xmlns:foo='urn:foo' xmlns:goo='urn:goo' xsi:nil='true' \n" +
        "       xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' \n" +
        "       foo:aB='' foo:aD='' goo:aE='' >" +
        "</foo:I>\n";
      
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);
    
      baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
  
      encoder.setGrammarCache(grammarCache);

      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      n_events = 0;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("I", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_NL, exiEvent.getEventKind());
      Assert.assertEquals("nil", exiEvent.getName());
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
      Assert.assertEquals("nil", eventType.name);
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(12, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aB", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aD", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertNull(eventType.uri);
      Assert.assertNull(eventType.name);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertSame(eventType, eventTypeList.getEE());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("aB", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("aD", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(11);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
      Assert.assertEquals("aB", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aB", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(12, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aD", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertNull(eventType.uri);
      Assert.assertNull(eventType.name);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertSame(eventType, eventTypeList.getEE());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("aB", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("aD", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(11);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
      Assert.assertEquals("aD", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aD", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(8, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertNull(eventType.uri);
      Assert.assertNull(eventType.name);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertSame(eventType, eventTypeList.getEE());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("aD", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
      Assert.assertEquals("aE", exiEvent.getName());
      Assert.assertEquals("urn:goo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertNull(eventType.uri);
      Assert.assertNull(eventType.name);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(6, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(6, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertNull(eventType.uri);
      Assert.assertNull(eventType.name);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      ++n_events;
      
      Assert.assertEquals(8, n_events);
    }
  }
 
  /**
   * Schema: 
   * <xsd:complexType name="F">
   *   <xsd:sequence>
   *     <xsd:element ref="foo:AB"/>
   *   </xsd:sequence>
   *   <xsd:attribute ref="foo:aB" />
   *   <xsd:attribute ref="foo:aC" />
   *   <xsd:attribute ref="foo:aA" use="required"/>
   * </xsd:complexType>
   * 
   * <xsd:complexType name="extended_F">
   *   <xsd:complexContent>
   *     <xsd:extension base="foo:F">
   *       <xsd:anyAttribute namespace="##any" />
   *     </xsd:extension>
   *   </xsd:complexContent>
   * </xsd:complexType>
   * 
   * <xsd:element name="F" type="foo:F" nillable="true"/>
   *
   * Instance:
   * <foo:F xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' 
   *    foo:aA="" xsi:kil='bad' xsi:type='extended_F' ><foo:AB/><foo:AC/></foo:F>
   */
  public void testXsiKill() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/interop/schemaInformedGrammar/acceptance.xsd", getClass(), m_compilerErrors);
    
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
  
      encoder.setGrammarCache(grammarCache);
      baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
  
      URL url = resolveSystemIdAsURL("/interop/schemaInformedGrammar/undeclaredProductions/xsiTypeStrict-07.xml");
      InputSource inputSource = new InputSource(url.toString());
      inputSource.setByteStream(url.openStream());
  
      encoder.encode(inputSource);
  
      bts = baos.toByteArray();
  
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
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
      Assert.assertEquals(1, eventTypeList.getLength());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("F", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_TP, exiEvent.getEventKind());
      Assert.assertEquals("type", exiEvent.getName());
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
      Assert.assertEquals("extended_F", ((EXIEventSchemaType)exiEvent).getTypeName());
      Assert.assertEquals("", ((EXIEventSchemaType)exiEvent).getTypeURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
      Assert.assertEquals("type", eventType.name);
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
      Assert.assertEquals("aA", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aA", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getLength());
      Assert.assertNull(eventTypeList.getEE());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
      Assert.assertEquals("kil", exiEvent.getName());
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(4, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aB", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aC", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("AB", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("AB", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("AB", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(4, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aB", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aC", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      ++n_events;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex()); // because of xsi:type
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getLength());
      Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);
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
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("AC", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("AC", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex()); // because of xsi:type
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getLength());
      Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);
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
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
      ++n_events;
      
      Assert.assertEquals(13, n_events);
    }
  }
  
  /**
   * Schema:
   * <xsd:element name="I" nillable="true">
   *   <xsd:complexType>
   *     <xsd:choice>
   *       <xsd:element name="A">
   *         <xsd:complexType>
   *           <xsd:simpleContent>
   *             <xsd:extension base="xsd:anySimpleType">
   *               <xsd:anyAttribute namespace="urn:hoo urn:none_02 urn:goo urn:foo urn:hoo urn:hoo ##local" />
   *             </xsd:extension>
   *           </xsd:simpleContent>
   *         </xsd:complexType>
   *       </xsd:element>
   *       <xsd:element name="B">
   *         <xsd:complexType>
   *           <xsd:simpleContent>
   *             <xsd:extension base="xsd:anySimpleType">
   *               <xsd:anyAttribute namespace="##other" />
   *             </xsd:extension>
   *           </xsd:simpleContent>
   *         </xsd:complexType>
   *       </xsd:element>
   *     </xsd:choice>
   *     <xsd:attribute ref="foo:aF" />
   *     <xsd:attribute ref="foo:aI" use="required" />
   *     <xsd:attribute ref="foo:aC" />
   *     <xsd:anyAttribute namespace="##any" />
   *   </xsd:complexType>
   * </xsd:element>
   * 
   * Instance:
   * <foo:I xmlns:foo='urn:foo' xmlns:goo='urn:goo' 
   *   goo:aB="wildcard AT(*)" 
   *   foo:aC="attribute use aC" 
   *   foo:aE="wildcard AT(*)" 
   *   foo:aF="attribute use aF" 
   *   aH="wildcard AT(*)" 
   *   foo:aI="attribute use aI" 
   *   foo:aJ="wildcard AT(*)">
   *   <A/>
   * </foo:I>
   */
  public void testDecodeComplexType_05() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/interop/schemaInformedGrammar/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    EXIDecoder decoder = new EXIDecoder();
    Scanner scanner;
    
    decoder.setAlignmentType(AlignmentType.byteAligned);

    URL url = resolveSystemIdAsURL("/interop/schemaInformedGrammar/declaredProductions/complexType-05.byteAligned");
    
    decoder.setGrammarCache(grammarCache);
    decoder.setInputStream(url.openStream());
    scanner = decoder.processHeader();
    
    EventDescription exiEvent;
    int n_events = 0;
    
    EventType eventType;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
    Assert.assertEquals("I", eventType.name);
    Assert.assertEquals("urn:foo", eventType.uri);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("aH", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("aC", exiEvent.getName());
    Assert.assertEquals("urn:foo", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
    Assert.assertEquals("aC", eventType.name);
    Assert.assertEquals("urn:foo", eventType.uri);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("aE", exiEvent.getName());
    Assert.assertEquals("urn:foo", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("aF", exiEvent.getName());
    Assert.assertEquals("urn:foo", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
    Assert.assertEquals("aF", eventType.name);
    Assert.assertEquals("urn:foo", eventType.uri);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("aI", exiEvent.getName());
    Assert.assertEquals("urn:foo", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
    Assert.assertEquals("aI", eventType.name);
    Assert.assertEquals("urn:foo", eventType.uri);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("aJ", exiEvent.getName());
    Assert.assertEquals("urn:foo", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("aB", exiEvent.getName());
    Assert.assertEquals("urn:goo", exiEvent.getURI());
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("A", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
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
    ++n_events;

    Assert.assertEquals(14, n_events);
  }

}
