package org.openexi.sax;

import java.io.StringReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.ArrayList;

import org.xml.sax.InputSource;

import junit.framework.Assert;

import org.openexi.proc.EXIDecoder;
import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EXIOptionsException;
import org.openexi.proc.common.EventCode;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventTypeList;
import org.openexi.proc.common.EXIEvent;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.common.XmlUriConst;
import org.openexi.proc.events.EXIEventNS;
import org.openexi.proc.events.EXIEventSchemaNil;
import org.openexi.proc.events.EXIEventSchemaType;
import org.openexi.proc.grammars.EventTypeAccessor;
import org.openexi.proc.grammars.EventTypeSchema;
import org.openexi.proc.grammars.Grammar;
import org.openexi.proc.grammars.GrammarAccessor;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.proc.grammars.GroupGrammar;
import org.openexi.proc.grammars.SchemaInformedGrammar;
import org.openexi.proc.io.Scanner;
import org.openexi.proc.io.StringTable;
import org.openexi.sax.Transmogrifier;
import org.openexi.sax.TransmogrifierException;
import org.openexi.schema.EXISchema;
import org.openexi.schema.TestBase;

import org.openexi.scomp.Docbook43Schema;
import org.openexi.scomp.EXISchemaFactoryErrorMonitor;
import org.openexi.scomp.EXISchemaFactoryTestUtil;

public class GrammarSchemaInformedTest extends TestBase {

  public GrammarSchemaInformedTest(String name) {
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
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
    
    final String xmlString = 
      "<A xmlns='urn:foo'>\n" +
      "  <AB> </AB>\t<AC> </AC><AC/><AD> </AD><AE> </AE>\n" +
      "</A>\n";

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
        
        byte[] bts;
        int n_events;
        
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
        
        Assert.assertEquals(preserveWhitespaces ? 21 : 18, n_events);
    
        EventType eventType;
        EventTypeList eventTypeList;
        int n = 0;
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertNull(eventTypeList.getEE());
        
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("A", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertNull(eventTypeList.getEE());

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

        if (preserveWhitespaces) {
          exiEvent = exiEventList.get(n++);
          Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
          Assert.assertEquals("\t", exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
          Assert.assertEquals(4, eventType.getIndex());
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
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        }
    
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
        Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
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
        Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        Assert.assertEquals(3, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(8, eventTypeList.getLength());
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
        Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
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
        Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
        Assert.assertEquals("AE", exiEvent.getName());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("AE", eventType.getName());
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
    
        if (preserveWhitespaces) {
          exiEvent = exiEventList.get(n++);
          Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
          Assert.assertEquals("\n", exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
          Assert.assertEquals(2, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(3, eventTypeList.getLength());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        }
        
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
   * The 3rd <AC/> is not expected.
   * <A>
   *   <AB/><AC/><AC/><AC/><AD/>
   * </A>
   */
  public void testAcceptanceForA_01_with_UndeclaredElement_1() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
    
    final String xmlString = 
      "<A xmlns='urn:foo'>" +
        "<AB> </AB><AC> </AC><AC/><AC> </AC><AD> </AD>" +
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
      
      byte[] bts;
      int n_events;
      
      
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
      
      Assert.assertEquals(18, n_events);
  
      EventType eventType;
      EventTypeList eventTypeList;
  
      // events at position 0...9 are the same as testAcceptanceForA_01
  
      exiEvent = exiEventList.get(10);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AC", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(4, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AD", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(11);
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
  
      exiEvent = exiEventList.get(12);
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
  
      exiEvent = exiEventList.get(13);
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
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(14);
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
  
      exiEvent = exiEventList.get(15);
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
  
      exiEvent = exiEventList.get(16);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(4, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AE", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(17);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
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
   * The 1st <AC/> is not expected.
   * <A>
   *   <AC/><AB/><AD/>
   * </A>
   */
  public void testAcceptanceForA_01_with_UndeclaredElement_2() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
    
    final String xmlString = 
      "<A xmlns='urn:foo'>" +
        "<AC/><AB/><AD/>" +
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
      
      byte[] bts;
      int n_events;
      
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
      Assert.assertEquals("AC", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      Assert.assertEquals(6, eventType.getIndex());
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
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(8, eventTypeList.getLength());
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
      Assert.assertEquals(4, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(8, eventTypeList.getLength());
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
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(7);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(8, eventTypeList.getLength());
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
  
      exiEvent = exiEventList.get(8);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(4, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AE", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(9);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
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
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
    
    final String xmlString = 
      "<A xmlns='urn:foo'>" +
        "<AB> </AB><AD> </AD>" +
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
      
      byte[] bts;
      int n_events;
      
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
  
      exiEvent = exiEventList.get(3);
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
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(6);
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
  
      exiEvent = exiEventList.get(7);
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
  
      exiEvent = exiEventList.get(8);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(4, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AE", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(9);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
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
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

    final String xmlString = 
      "<B xmlns='urn:foo'>" +
        "<AB> </AB><AC> </AC><AC/><AD> </AD>" +
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
      
      Assert.assertEquals(15, n_events);
      
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
  
      exiEvent = exiEventList.get(3);
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
  
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals(" ", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
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
  
      exiEvent = exiEventList.get(7);
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
  
      exiEvent = exiEventList.get(9);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(8, eventTypeList.getLength());
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
  
      exiEvent = exiEventList.get(10);
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
  
      exiEvent = exiEventList.get(11);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals(" ", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
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
  
      exiEvent = exiEventList.get(12);
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
  
      exiEvent = exiEventList.get(13);
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
      
      exiEvent = exiEventList.get(14);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
    }
  }
  
  /**
   * Schema: 
   * <xsd:element name="nillable_B" type="foo:B" nillable="true" />
   * 
   * Instance:
   * The element <AB/> is unexpected.
   * <nillable_B xmlns='urn:foo' xsi:nil='true' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>
   *   <AB/>
   * </nillable_B>
   */
  public void testAcceptanceForNillableB_with_UndeclaredElement() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

    final String xmlString = 
      "<nillable_B xmlns='urn:foo' xsi:nil='true' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>" +
        "<AB/>" +
      "</nillable_B>\n";

    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      byte[] bts;
      ByteArrayOutputStream baos;
  
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
      exiEventList = new ArrayList<EXIEvent>();
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
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AB", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      Assert.assertEquals(5, eventType.getIndex());
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
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(8, eventTypeList.getLength());
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
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
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
   * <C>
   *   <AB/><AC/>
   * </C>
   */
  public void testAcceptanceForC_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
    
    final String xmlString = 
      "<C xmlns='urn:foo'>" +
        "<AB> </AB>\t<AC> </AC>\n" +
      "</C>\n";
  
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
        
        Assert.assertEquals(preserveWhitespaces ? 12 : 10, n_events);
    
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
        Assert.assertEquals("C", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertNull(eventTypeList.getEE());
        
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
        Assert.assertEquals("AB", exiEvent.getName());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("AB", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        Assert.assertEquals(2, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(9, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
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
    
        if (preserveWhitespaces) {
          exiEvent = exiEventList.get(n++);
          Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
          Assert.assertEquals("\t", exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
          Assert.assertEquals(4, eventType.getIndex());
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
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        }
        
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
        Assert.assertEquals("AC", exiEvent.getName());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("AC", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        Assert.assertEquals(1, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(5, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("AB", eventType.getName());
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
    
        if (preserveWhitespaces) {
          exiEvent = exiEventList.get(n++);
          Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
          Assert.assertEquals("\n", exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
          Assert.assertEquals(4, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(5, eventTypeList.getLength());
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
        }
        
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
        Assert.assertEquals(2, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(5, eventTypeList.getLength());
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
   * <xsd:element name="C">
   *   <xsd:complexType>
   *     <xsd:all>
   *       <xsd:element ref="foo:AB" minOccurs="0" />
   *       <xsd:element ref="foo:AC" minOccurs="0" />
   *     </xsd:all>
   *   </xsd:complexType>
   * </xsd:element>
   *
   * <C>
   *   <AC/><AB/><!-- reverse order -->  
   * </C>
   * where C has "all" group that consists of AC and AB.
   */
  public void testAcceptanceForC_02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
    
    final String xmlString = 
      "<C xmlns='urn:foo'>" +
        "<AC> </AC><AB> </AB>" +
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
      Assert.assertEquals(9, eventTypeList.getLength());
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
  
      exiEvent = exiEventList.get(3);
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
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AB", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
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
  
      exiEvent = exiEventList.get(6);
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
  
      exiEvent = exiEventList.get(7);
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
  
      exiEvent = exiEventList.get(8);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
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
  
      exiEvent = exiEventList.get(9);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
    }
  }
  
  /**
   * Schema:
   * <xsd:element name="AB" type="xsd:anySimpleType"/>
   * 
   * Instance:
   * The element <AC/> is not expected.
   * <AB><AC/>abc</AB>
   */
  public void testAcceptanceForAB_with_UndeclaredElement_1() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
    
    final String xmlString = "<AB xmlns='urn:foo'><AC/>abc</AB>\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      byte[] bts;
      int n_events;
      
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
      Assert.assertEquals("AB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AC", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      Assert.assertEquals(6, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(8, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
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
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(8, eventTypeList.getLength());
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
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("abc", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
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
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
    }
  }
  
  /**
   * Schema:
   * <xsd:element name="AB" type="xsd:anySimpleType"/>
   * 
   * Instance:
   * The element <AC/> is not expected.
   * <AB><AC/>abc</AB>
   */
  public void testAcceptanceForAB_with_UndeclaredElement_2() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
    
    final String xmlString = 
      "<AB xmlns='urn:foo'>abc<AC/>\n" + 
      "</AB>\n";
    
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
        
        byte[] bts;
        int n_events;
        
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
        
        Assert.assertEquals(preserveWhitespaces ? 8 : 7, n_events);
    
        EventType eventType;
        EventTypeList eventTypeList;
        int n = 0;
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertNull(eventTypeList.getEE());
        
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("AB", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertNull(eventTypeList.getEE());
        
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
        Assert.assertEquals("abc", exiEvent.getCharacters().makeString());
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
        Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
        Assert.assertEquals("AC", exiEvent.getName());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        Assert.assertEquals(1, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(3, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        Assert.assertEquals(3, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(8, eventTypeList.getLength());
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
    
        if (preserveWhitespaces) {
          exiEvent = exiEventList.get(n++);
          Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
          Assert.assertEquals("\n", exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
          Assert.assertEquals(2, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(3, eventTypeList.getLength());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        }
        
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
   * <xsd:complexType name="B">
   *   <xsd:sequence>
   *     <xsd:element ref="foo:AB"/>
   *     <xsd:element ref="foo:AC" minOccurs="0" maxOccurs="2"/>
   *     <xsd:element ref="foo:AD" minOccurs="0"/>
   *   </xsd:sequence>
   * </xsd:complexType>
   * 
   * <xsd:element name="B" type="foo:B"/>
   *
   * Instance:
   * <B xsi:nil="true" xmlns="urn:foo">
   * </B>
   * 
   * Use of xsi:nil is permitted in default mode, even though it would
   * not have been permitted in strict mode. 
   */
  public void testLenientXsiNil_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache;
    
    grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
    
    final String xmlString = 
      "<B xsi:nil='true' xmlns='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>" +
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
      
      Assert.assertEquals(5, n_events);
  
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
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
  
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_NL, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
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
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(7, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(4);
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
   * <xsd:simpleType name="finalString" final="#all">
   *   <xsd:restriction base="xsd:string" />
   * </xsd:simpleType>
   * 
   * <xsd:element name="K" type="foo:finalString"/>
   * 
   * Instance:
   * <K xsi:nil="true" xmlns="urn:foo"/>
   * 
   * Use of xsi:nil is permitted in default mode, even though it would
   * not have been permitted in strict mode. 
   */
  public void testLenientXsiNil_02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache;
    
    grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
    
    final String xmlString = 
      "<K xsi:nil='true' xmlns='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'/>";

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
      
      Assert.assertEquals(5, n_events);
  
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
      Assert.assertEquals("K", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
  
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_NL, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(8, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
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
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(7, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(4);
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
   * <xsd:simpleType name="finalString" final="#all">
   *   <xsd:restriction base="xsd:string" />
   * </xsd:simpleType>
   * 
   * <xsd:element name="K" type="foo:finalString"/>
   * 
   * Instance:
   * <K xsi:nil="troo" xmlns="urn:foo"/>
   * 
   * Use of xsi:nil (even with invalid value) is permitted in default mode. 
   */
  public void testLenientXsiNil_03() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache;
    
    grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
    
    final String xmlString = 
      "<K xsi:nil='troo' xmlns='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'/>";

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
      
      Assert.assertEquals(5, n_events);
  
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
      Assert.assertEquals("K", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
  
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      Assert.assertEquals(5, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(8, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
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
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(8, eventTypeList.getLength());
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
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
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
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

    final String xmlString = 
      "<foo:D xmlns:foo='urn:foo'>" +
        "<A/><A/><B/><C/><D/>\t<B/><E/><F/>" +
      "</foo:D>\n";
    
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
        
        Assert.assertEquals(preserveWhitespaces ? 21 : 20, n_events);
    
        EventType eventType;
        EventTypeList eventTypeList;
        int n = 0;
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertNull(eventTypeList.getEE());
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("D", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertNull(eventTypeList.getEE());
        
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
        Assert.assertEquals("A", exiEvent.getName());
        Assert.assertEquals("", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("A", eventType.getName());
        Assert.assertEquals("", eventType.getURI());
        Assert.assertEquals(2, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(9, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("B", eventType.getName());
        Assert.assertEquals("", eventType.getURI());
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
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
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
        Assert.assertEquals(4, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(10, eventTypeList.getLength());
        Assert.assertNotNull(eventTypeList.getEE());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
        eventType = eventTypeList.item(8);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
        Assert.assertEquals("A", exiEvent.getName());
        Assert.assertEquals("", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("A", eventType.getName());
        Assert.assertEquals("", eventType.getURI());
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(5, eventTypeList.getLength());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("B", eventType.getName());
        Assert.assertEquals("", eventType.getURI());
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
        Assert.assertEquals(4, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(10, eventTypeList.getLength());
        Assert.assertNotNull(eventTypeList.getEE());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
        eventType = eventTypeList.item(8);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
    
        exiEvent = exiEventList.get(n++);
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
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
        
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
        Assert.assertEquals(4, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(10, eventTypeList.getLength());
        Assert.assertNotNull(eventTypeList.getEE());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
        eventType = eventTypeList.item(8);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
        Assert.assertEquals("C", exiEvent.getName());
        Assert.assertEquals("", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("C", eventType.getName());
        Assert.assertEquals("", eventType.getURI());
        Assert.assertEquals(1, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(8, eventTypeList.getLength());
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
        eventType = eventTypeList.item(6);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
        Assert.assertEquals(4, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(10, eventTypeList.getLength());
        Assert.assertNotNull(eventTypeList.getEE());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
        eventType = eventTypeList.item(8);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
        Assert.assertEquals("D", exiEvent.getName());
        Assert.assertEquals("", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("D", eventType.getName());
        Assert.assertEquals("", eventType.getURI());
        Assert.assertEquals(1, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(7, eventTypeList.getLength());
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
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
        Assert.assertEquals(4, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(10, eventTypeList.getLength());
        Assert.assertNotNull(eventTypeList.getEE());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
        eventType = eventTypeList.item(8);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
    
        if (preserveWhitespaces) {
          exiEvent = exiEventList.get(n++);
          Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
          Assert.assertEquals("\t", exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
          Assert.assertEquals(5, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(6, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
          Assert.assertEquals("B", eventType.getName());
          Assert.assertEquals("", eventType.getURI());
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
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        }
        
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
        Assert.assertEquals("B", exiEvent.getName());
        Assert.assertEquals("", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("B", eventType.getName());
        Assert.assertEquals("", eventType.getURI());
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(6, eventTypeList.getLength());
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
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
        
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
        Assert.assertEquals(4, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(10, eventTypeList.getLength());
        Assert.assertNotNull(eventTypeList.getEE());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
        eventType = eventTypeList.item(8);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
        Assert.assertEquals("E", exiEvent.getName());
        Assert.assertEquals("", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("E", eventType.getName());
        Assert.assertEquals("", eventType.getURI());
        Assert.assertEquals(2, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(7, eventTypeList.getLength());
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
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
        
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
        Assert.assertEquals(4, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(10, eventTypeList.getLength());
        Assert.assertNotNull(eventTypeList.getEE());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
        eventType = eventTypeList.item(8);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
        Assert.assertEquals("F", exiEvent.getName());
        Assert.assertEquals("", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("F", eventType.getName());
        Assert.assertEquals("", eventType.getURI());
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(4, eventTypeList.getLength());
        Assert.assertNotNull(eventTypeList.getEE());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
        Assert.assertEquals(4, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(10, eventTypeList.getLength());
        Assert.assertNotNull(eventTypeList.getEE());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
        eventType = eventTypeList.item(8);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
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
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

    final String xmlString = 
      "<foo:E xmlns:foo='urn:foo'>" +
        "<A/><A/><B/><C/><B/><G/><H/>" +
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
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(12, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("D", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("G", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("H", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(11);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(4, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
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
      Assert.assertEquals(5, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertSame(eventType, eventTypeList.getEE());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(4, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
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
      Assert.assertEquals(4, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertSame(eventType, eventTypeList.getEE());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      
      exiEvent = exiEventList.get(7);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(4, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
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
      Assert.assertEquals(8, eventTypeList.getLength());
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
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(9);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(4, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
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
      Assert.assertEquals(7, eventTypeList.getLength());
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
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(11);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(4, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
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
      Assert.assertEquals(6, eventTypeList.getLength());
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
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(13);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(4, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
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
      Assert.assertEquals(4, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      
      exiEvent = exiEventList.get(15);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(4, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(16);
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
  
      exiEvent = exiEventList.get(17);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
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
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

    String xmlString;
    
    ByteArrayOutputStream baos;
    byte[] bts;
    
    ArrayList<EXIEvent> exiEventList;

    EXIEvent exiEvent;
    int n_events;
    
    EventType eventType;
    EventTypeList eventTypeList;

    for (AlignmentType alignment : Alignments) {
      xmlString = 
        "<foo:F xmlns:foo='urn:foo' foo:aA='' foo:aB=''>" +
          "<foo:AB>xyz</foo:AB>" +
        "</foo:F>\n";

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
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9, eventTypeList.getLength());
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
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("aA", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
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
      Assert.assertEquals(8, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("aB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
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
      Assert.assertEquals(6, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("xyz", exiEvent.getCharacters().makeString());
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
  
      exiEvent = exiEventList.get(6);
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
  
      exiEvent = exiEventList.get(7);
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
  
      exiEvent = exiEventList.get(8);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
    }
    
    for (AlignmentType alignment : Alignments) {
      xmlString = 
        "<foo:F xmlns:foo='urn:foo' foo:aA='' foo:aB=''>" +
        "</foo:F>\n";

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
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9, eventTypeList.getLength());
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
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("aA", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
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
      Assert.assertEquals(8, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("aB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
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
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(6, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      
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
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

    String xmlString;

    ByteArrayOutputStream baos;
    byte[] bts;

    ArrayList<EXIEvent> exiEventList;
    
    EXIEvent exiEvent;
    int n_events;

    EventType eventType;
    EventTypeList eventTypeList;

    for (AlignmentType alignment : Alignments) {
      xmlString = 
        "<foo:F xmlns:foo='urn:foo' foo:aA='' foo:aB='' xsi:nil='true' \n" +
        "       xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>" +
        "</foo:F>\n";
      
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
      
      Assert.assertEquals(7, n_events);
  
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
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aA", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("aA", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
      Assert.assertEquals("aA", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aA", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9, eventTypeList.getLength());
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
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("aA", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
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
      Assert.assertEquals(7, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("aB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
    }
    
    for (AlignmentType alignment : Alignments) {
      xmlString = 
        "<foo:F xmlns:foo='urn:foo' foo:aA='' foo:aB='' xsi:nil='true' \n" +
        "       xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>" +
          "<foo:AB/>" +
        "</foo:F>\n";
      
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
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aA", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("aA", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
      Assert.assertEquals("aA", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aA", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9, eventTypeList.getLength());
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
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("aA", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
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
      Assert.assertEquals(7, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("aB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AB", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(8, eventTypeList.getLength());
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
  
      exiEvent = exiEventList.get(7);
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
  
      exiEvent = exiEventList.get(8);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
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
  public void testAcceptanceForNilledF_with_UndeclaredElement() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

    final String xmlString = 
      "<foo:F xmlns:foo='urn:foo' foo:aA='' foo:aB='' xsi:nil='true' \n" +
      "       xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>" +
        "<foo:AB/>" +
      "</foo:F>\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos;
      
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
      Assert.assertEquals(EXIEvent.EVENT_NL, exiEvent.getEventVariety());
      Assert.assertEquals("nil", exiEvent.getName());
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      Assert.assertEquals("nil", eventType.getName());
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.getURI());
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aA", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("aA", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
      Assert.assertEquals("aA", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aA", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9, eventTypeList.getLength());
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
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("aA", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
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
      Assert.assertEquals(7, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("aB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AB", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(8, eventTypeList.getLength());
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
      
      exiEvent = exiEventList.get(7);
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
  
      exiEvent = exiEventList.get(8);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
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
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

    String xmlString; 

    ByteArrayOutputStream baos;

    byte[] bts;
    int n_events;
    
    EXIEvent exiEvent;

    EventType eventType;
    EventTypeList eventTypeList;

    ArrayList<EXIEvent> exiEventList;

    for (AlignmentType alignment : Alignments) {
      xmlString = 
        "<foo:G xmlns:foo='urn:foo' foo:aA='' foo:aB=''>" +
          "<foo:AB>xyz</foo:AB>" +
        "</foo:G>\n";

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
      Assert.assertEquals(1, eventTypeList.getLength());
  
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
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9, eventTypeList.getLength());
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
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("aA", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
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
      Assert.assertEquals(8, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("aB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
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
      Assert.assertEquals(6, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("xyz", exiEvent.getCharacters().makeString());
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
  
      exiEvent = exiEventList.get(6);
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
  
      exiEvent = exiEventList.get(7);
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
  
      exiEvent = exiEventList.get(8);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
    }
    
    for (AlignmentType alignment : Alignments) {
      xmlString = 
        "<foo:G xmlns:foo='urn:foo' foo:aA='' foo:aB=''>" +
        "</foo:G>\n";

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      encoder.setEXISchema(grammarCache);
      
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
      Assert.assertEquals(1, eventTypeList.getLength());
  
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
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9, eventTypeList.getLength());
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
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("aA", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
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
      Assert.assertEquals(8, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("aB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
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
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(6, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
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
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

    final String xmlString = 
      "<foo:H xmlns:foo='urn:foo' xmlns:goo='urn:goo'>" +
        "<goo:AB> </goo:AB>" +
        "<B/>" +
        "<goo:AB></goo:AB>" +
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
      Assert.assertEquals(4, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:eoo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(3);
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
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(4, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(7);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AB", exiEvent.getName());
      Assert.assertEquals("urn:goo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
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
  
      exiEvent = exiEventList.get(8);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(8, eventTypeList.getLength());
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
   */
  public void testAcceptanceForH2_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

    final String xmlString = 
      "<foo:H2 xmlns:foo='urn:foo' xmlns:goo='urn:goo'>" +
        "<goo:AB> </goo:AB>\n" + // <xsd:any namespace="##other" minOccurs="0" />
        "<foo:AB> </foo:AB>\n" + // <xsd:any namespace="##targetNamespace ##local" minOccurs="0" />
      "</foo:H2>\n";
    
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
    
        byte[] bts;
        int n_events;
        
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
        
        Assert.assertEquals(preserveWhitespaces ? 12 : 10, n_events);
    
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
        Assert.assertEquals("H2", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertNull(eventTypeList.getEE());
        
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
        Assert.assertEquals("AB", exiEvent.getName());
        Assert.assertEquals("urn:goo", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        Assert.assertEquals(4, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(10, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.assertEquals("urn:foo", eventType.getURI());
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.assertEquals("", eventType.getURI());
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
        eventType = eventTypeList.item(8);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
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
    
        if (preserveWhitespaces) {
          exiEvent = exiEventList.get(n++);
          Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
          Assert.assertEquals("\n", exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
          Assert.assertEquals(4, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(5, eventTypeList.getLength());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
          Assert.assertEquals("urn:foo", eventType.getURI());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
          Assert.assertEquals("", eventType.getURI());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        }
        
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
        Assert.assertEquals("AB", exiEvent.getName());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.assertEquals("urn:foo", eventType.getURI());
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(5, eventTypeList.getLength());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.assertEquals("", eventType.getURI());
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
    
        if (preserveWhitespaces) {
          exiEvent = exiEventList.get(n++);
          Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
          Assert.assertEquals("\n", exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
          Assert.assertEquals(2, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(3, eventTypeList.getLength());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        }
        
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
  public void testAcceptanceForH2_01_with_UndeclaredElement() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

    final String xmlString = 
      "<foo:H2 xmlns:foo='urn:foo' xmlns:goo='urn:goo'>" +
        "<goo:AB/>" +
        "<goo:AB/>" + // unexpected
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
  
      byte[] bts;
      int n_events;
      
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
      Assert.assertEquals(4, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(8, eventTypeList.getLength());
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
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AB", exiEvent.getName());
      Assert.assertEquals("urn:goo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(3, eventType.getIndex());
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(8, eventTypeList.getLength());
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
  
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(7);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
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
   */
  public void testAcceptanceForH2_02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

    final String xmlString = 
      "<foo:H2 xmlns:foo='urn:foo' xmlns:goo='urn:goo'>" +
        "<goo:AB> </goo:AB>" +
        "<AB> </AB>" +
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
  
      byte[] bts;
      int n_events;
      
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
      Assert.assertEquals(1, eventTypeList.getLength());
      
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
      Assert.assertEquals(4, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(3);
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
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AB", exiEvent.getName());
      Assert.assertEquals("", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("", eventType.getURI());
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(6);
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
  
      exiEvent = exiEventList.get(7);
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
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
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
   * After an occurrence of "##other", "##local" wildcard can come only once, 
   * but not twice.
   */
  public void testAcceptanceForH2_02_with_UndeclaredElement() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

    final String xmlString = 
      "<foo:H2 xmlns:foo='urn:foo' xmlns:goo='urn:goo'>" +
        "<goo:AB/>" +
        "<AB/>" +
        "<AB/>" + // unexpected
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
  
      byte[] bts;
      int n_events;
      
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
      Assert.assertEquals(1, eventTypeList.getLength());
      
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
      Assert.assertEquals(4, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(8, eventTypeList.getLength());
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
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AB", exiEvent.getName());
      Assert.assertEquals("", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("", eventType.getURI());
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(8, eventTypeList.getLength());
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
  
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AB", exiEvent.getName());
      Assert.assertEquals("", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(7);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(8, eventTypeList.getLength());
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
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
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
   */
  public void testAcceptanceForH2_03() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

    final String xmlString = 
      "<foo:H2 xmlns:foo='urn:foo' xmlns:goo='urn:goo'>" +
        "<foo:AB> </foo:AB>" +
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
  
      byte[] bts;
      int n_events;
      
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
      Assert.assertEquals(1, eventTypeList.getLength());
      
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
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(3);
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
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
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
  public void testAcceptanceForH2_03_with_UndeclaredElement() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

    final String xmlString = 
      "<foo:H2 xmlns:foo='urn:foo' xmlns:goo='urn:goo'>" +
        "<foo:AB/>" +
        "<goo:AB/>" + // unexpected
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
  
      byte[] bts;
      int n_events;
      
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
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(8, eventTypeList.getLength());
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
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AB", exiEvent.getName());
      Assert.assertEquals("urn:goo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(3, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(8, eventTypeList.getLength());
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
  
      exiEvent = exiEventList.get(6);
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
  
      exiEvent = exiEventList.get(7);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
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
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

    String xmlString; 
    
    ByteArrayOutputStream baos;
    
    byte[] bts;
    int n_events;
    
    ArrayList<EXIEvent> exiEventList;
    
    EXIEvent exiEvent;
    
    EventType eventType;
    EventTypeList eventTypeList;

    for (AlignmentType alignment : Alignments) {
      xmlString = 
        "<foo:H2 xmlns:foo='urn:foo' xmlns:goo='urn:goo'>" +
          "<goo:AB> </goo:AB>" +
        "</foo:H2>\n";
      
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
  
      Assert.assertEquals(7, n_events);
  
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
      Assert.assertEquals(4, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(3);
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
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
    }
    
    for (AlignmentType alignment : Alignments) {
      xmlString = 
        "<foo:H2 xmlns:foo='urn:foo' xmlns:goo='urn:goo'>" +
          "<goo:AB/>" + 
          "xyz" + // unexpected
        "</foo:H2>\n";

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      encoder.setEXISchema(grammarCache);
  
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
  
      Assert.assertEquals(7, n_events);
  
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
      Assert.assertEquals(4, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(8, eventTypeList.getLength());
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
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("xyz", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      Assert.assertEquals(4, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
  
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
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
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

    final String xmlString = 
      "<foo:H3 xmlns:foo='urn:foo' xmlns:goo='urn:goo' xmlns:hoo='urn:hoo' xmlns:ioo='urn:ioo'>" +
        "<foo:AB> </foo:AB>" + // <xsd:any namespace="##local ##targetNamespace" minOccurs="0" maxOccurs="2"/>
        "<AB> </AB>" + // same as above
        "<hoo:AC> </hoo:AC>" + // <xsd:element ref="hoo:AC" minOccurs="0"/>
        "<goo:AB> </goo:AB>\n" + // <xsd:any namespace="urn:goo" minOccurs="0" />
        "<hoo:AB> </hoo:AB>" + // <xsd:element ref="hoo:AB" minOccurs="0"/>
        "<ioo:AB> </ioo:AB>" + // <xsd:any namespace="urn:ioo" minOccurs="0" />
      "</foo:H3>\n";
    
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
    
        byte[] bts;
        int n_events;
        
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
        
        Assert.assertEquals(preserveWhitespaces ? 23 : 22, n_events);
    
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
        Assert.assertEquals("H3", exiEvent.getName());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("H3", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertNull(eventTypeList.getEE());
        
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
        Assert.assertEquals("AB", exiEvent.getName());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.assertEquals("urn:foo", eventType.getURI());
        Assert.assertEquals(5, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(13, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("AC", eventType.getName());
        Assert.assertEquals("urn:hoo", eventType.getURI());
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("AB", eventType.getName());
        Assert.assertEquals("urn:hoo", eventType.getURI());
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.assertEquals("", eventType.getURI());
        eventType = eventTypeList.item(6);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.assertEquals("urn:goo", eventType.getURI());
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.assertEquals("urn:ioo", eventType.getURI());
        eventType = eventTypeList.item(8);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(10);
        Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
        eventType = eventTypeList.item(11);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(12);
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
        Assert.assertEquals("AB", exiEvent.getName());
        Assert.assertEquals("", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.assertEquals("", eventType.getURI());
        Assert.assertEquals(2, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(9, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("AC", eventType.getName());
        Assert.assertEquals("urn:hoo", eventType.getURI());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("AB", eventType.getName());
        Assert.assertEquals("urn:hoo", eventType.getURI());
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.assertEquals("urn:foo", eventType.getURI());
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.assertEquals("urn:goo", eventType.getURI());
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.assertEquals("urn:ioo", eventType.getURI());
        eventType = eventTypeList.item(6);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
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
        Assert.assertEquals("AC", exiEvent.getName());
        Assert.assertEquals("urn:hoo", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("AC", eventType.getName());
        Assert.assertEquals("urn:hoo", eventType.getURI());
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(7, eventTypeList.getLength());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("AB", eventType.getName());
        Assert.assertEquals("urn:hoo", eventType.getURI());
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.assertEquals("urn:goo", eventType.getURI());
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.assertEquals("urn:ioo", eventType.getURI());
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(6);
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
        Assert.assertEquals("AB", exiEvent.getName());
        Assert.assertEquals("urn:goo", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.assertEquals("urn:goo", eventType.getURI());
        Assert.assertEquals(1, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(6, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("AB", eventType.getName());
        Assert.assertEquals("urn:hoo", eventType.getURI());
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.assertEquals("urn:ioo", eventType.getURI());
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(5);
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
    
        if (preserveWhitespaces) {
          exiEvent = exiEventList.get(n++);
          Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
          Assert.assertEquals("\n", exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
          Assert.assertEquals(4, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(5, eventTypeList.getLength());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
          Assert.assertEquals("AB", eventType.getName());
          Assert.assertEquals("urn:hoo", eventType.getURI());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
          Assert.assertEquals("urn:ioo", eventType.getURI());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        }
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
        Assert.assertEquals("AB", exiEvent.getName());
        Assert.assertEquals("urn:hoo", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("AB", eventType.getName());
        Assert.assertEquals("urn:hoo", eventType.getURI());
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(5, eventTypeList.getLength());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.assertEquals("urn:ioo", eventType.getURI());
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
        Assert.assertEquals("AB", exiEvent.getName());
        Assert.assertEquals("urn:ioo", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.assertEquals("urn:ioo", eventType.getURI());
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
   */
  public void testAcceptanceForH3_01_with_UndeclaredElement() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

    final String xmlString = 
      "<foo:H3 xmlns:foo='urn:foo' xmlns:goo='urn:goo' xmlns:hoo='urn:hoo' xmlns:ioo='urn:ioo'>" +
        "<foo:AB/>" + // <xsd:any namespace="##targetNamespace ##local" minOccurs="0" maxOccurs="2"/>
        "<hoo:AD/>" + // unexpected
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
  
      byte[] bts;
      int n_events;
      
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
      Assert.assertEquals(5, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(13, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AC", eventType.getName());
      Assert.assertEquals("urn:hoo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AB", eventType.getName());
      Assert.assertEquals("urn:hoo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:ioo", eventType.getURI());
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(11);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(12);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(8, eventTypeList.getLength());
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
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AD", exiEvent.getName());
      Assert.assertEquals("urn:hoo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      Assert.assertEquals(7, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AC", eventType.getName());
      Assert.assertEquals("urn:hoo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AB", eventType.getName());
      Assert.assertEquals("urn:hoo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:ioo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(8, eventTypeList.getLength());
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
  
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(6, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AC", eventType.getName());
      Assert.assertEquals("urn:hoo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AB", eventType.getName());
      Assert.assertEquals("urn:hoo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:ioo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(7);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
    }
  }
  
  /**
   * OPENGIS schema and instance.
   * There are nested groups in this example.
   */
  public void testOpenGisExample01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/opengis/openGis.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
    
    EXIEvent exiEvent;

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
        
        URL url = resolveSystemIdAsURL("/opengis/openGis.xml");
        InputSource inputSource = new InputSource(url.toString());
        inputSource.setByteStream(url.openStream());
        
        encoder.encode(inputSource);
        
        byte[] bts = baos.toByteArray();
        
        decoder.setEXISchema(grammarCache);
        decoder.setInputStream(new ByteArrayInputStream(bts));
        scanner = decoder.processHeader();
        
        int n_events = 0;
        int n_undeclaredCharacters = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          if (exiEvent.getEventVariety() == EXIEvent.EVENT_CH) {
            EventType eventType = exiEvent.getEventType();
            if (!eventType.isSchemaInformed()) {
              ++n_undeclaredCharacters;
              continue;
            }
          }
          ++n_events;
        }
        
        Assert.assertEquals(77, n_events);
        Assert.assertEquals(preserveWhitespaces ? 38 : 0, n_undeclaredCharacters);
      }
    }
    
    // Turn on preserve.prefixes
    grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(
        GrammarOptions.DEFAULT_OPTIONS));
    
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
        
        URL url = resolveSystemIdAsURL("/opengis/openGis.xml");
        InputSource inputSource = new InputSource(url.toString());
        inputSource.setByteStream(url.openStream());
        
        encoder.encode(inputSource);
        
        byte[] bts = baos.toByteArray();
        
        decoder.setEXISchema(grammarCache);
        decoder.setInputStream(new ByteArrayInputStream(bts));
        scanner = decoder.processHeader();
        
        ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();
        
        int n_events = 0;
        int n_undeclaredCharacters = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          if (exiEvent.getEventVariety() == EXIEvent.EVENT_CH) {
            EventType eventType = exiEvent.getEventType();
            if (!eventType.isSchemaInformed()) {
              ++n_undeclaredCharacters;
              continue;
            }
          }
          ++n_events;
          exiEventList.add(exiEvent);
        }
        
        // There are 7 namespace declarations.
        Assert.assertEquals(84, n_events);
        Assert.assertEquals(preserveWhitespaces ? 38 : 0, n_undeclaredCharacters);
        
        StringTable.URIPartition uriPartition = scanner.getStringTable().getURIPartition();
        Assert.assertEquals(11, uriPartition.n_strings);
        /*
         * URI  0 "" [empty string]
         * URI  1 "http://www.w3.org/XML/1998/namespace"
         * URI  2 "http://www.w3.org/2001/XMLSchema-instance"
         * URI  3 "http://www.w3.org/2001/XMLSchema"
         * URI  4 "http://www.opengis.net/gml"
         * URI  5 "http://www.opengis.net/ogc"
         * URI  6 "http://www.opengis.net/wfs"
         * URI  7 "http://www.ordnancesurvey.co.uk/xml/namespaces/osgb"
         * URI  8 "urn:myhub"
         * URI  9 "http://www.census.gov/geo/tiger"
         * URI 10 "http://www.w3.org/1999/xlink"
         * 
         */
        // Inspect the URI partition
        Assert.assertEquals("", uriPartition.getString(0));
        Assert.assertEquals(XmlUriConst.W3C_XML_1998_URI, uriPartition.getString(1));
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, uriPartition.getString(2));
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI, uriPartition.getString(3));
        Assert.assertEquals("http://www.opengis.net/gml", uriPartition.getString(4));
        Assert.assertEquals("http://www.opengis.net/ogc", uriPartition.getString(5));
        Assert.assertEquals("http://www.opengis.net/wfs", uriPartition.getString(6));
        Assert.assertEquals("http://www.ordnancesurvey.co.uk/xml/namespaces/osgb", uriPartition.getString(7));
        Assert.assertEquals("urn:myhub", uriPartition.getString(8));
        Assert.assertEquals("http://www.census.gov/geo/tiger", uriPartition.getString(9));
        Assert.assertEquals("http://www.w3.org/1999/xlink", uriPartition.getString(10));
    
        // Check to see if the right prefixe is used for each element and attribute. 
        for (int i = 0; i < n_events; i++) {
          EXIEvent event = exiEventList.get(i);
          short variety;
          if ((variety = event.getEventVariety()) == EXIEvent.EVENT_SE) {
            switch (uriPartition.getCompactId(event.getURI())) {
              case 4:
                Assert.assertEquals("gml", event.getPrefix());
                break;
              case 6:
                Assert.assertEquals(null, event.getPrefix());
                break;
              case 7:
                Assert.assertEquals("osgb", event.getPrefix());
                break;
              default:
                Assert.fail();
            }
          }
          else if (variety == EXIEvent.EVENT_AT) {
            switch (uriPartition.getCompactId(event.getURI())) {
              case 0:
                Assert.assertEquals("", event.getPrefix());
                break;
              default:
                Assert.fail();
            }
          }
        }
      }
    }
  }

  /**
   * Docbook 4.3 schema and instance.
   */
  public void testDocbook43ExampleVerySimple01() throws Exception {
    EXISchema corpus = Docbook43Schema.getEXISchema(); 
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
    
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
    
        String xmlString;
    
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
    
        encoder.encode(new InputSource(new StringReader(xmlString)));
        
        byte[] bts = baos.toByteArray();
        
        decoder.setEXISchema(grammarCache);
        decoder.setInputStream(new ByteArrayInputStream(bts));
        scanner = decoder.processHeader();
        
        EXIEvent exiEvent;
        int n_events = 0;
        int n_undeclaredCharacters = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          if (exiEvent.getEventVariety() == EXIEvent.EVENT_CH) {
            EventType eventType = exiEvent.getEventType();
            if (!eventType.isSchemaInformed()) {
              ++n_undeclaredCharacters;
              continue;
            }
          }
          ++n_events;
        }
        
        Assert.assertEquals(22, n_events);
        Assert.assertEquals(preserveWhitespaces ? 11 : 0, n_undeclaredCharacters);
      }
    }
  }

  /**
   * FPML 4.0 schema and instance.
   * xsi:type is used in this example.
   */
  public void testFpmlExample01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/fpml-4.0/fpml-main-4-0.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
    
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
        
        URL url = resolveSystemIdAsURL("/fpml-4.0/msg_ex01_request_confirmation.xml");
        InputSource inputSource = new InputSource(url.toString());
        inputSource.setByteStream(url.openStream());
        
        encoder.encode(inputSource);
        
        byte[] bts = baos.toByteArray();
        
        decoder.setEXISchema(grammarCache);
        decoder.setInputStream(new ByteArrayInputStream(bts));
        scanner = decoder.processHeader();
        
        EXIEvent exiEvent;
        
        int n_events = 0;
        int n_undeclaredCharacters = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          if (exiEvent.getEventVariety() == EXIEvent.EVENT_CH) {
            EventType eventType = exiEvent.getEventType();
            if (!eventType.isSchemaInformed()) {
              ++n_undeclaredCharacters;
              continue;
            }
          }
          ++n_events;
        }
        
        Assert.assertEquals(102, n_events);
        Assert.assertEquals(preserveWhitespaces ? 49 : 0, n_undeclaredCharacters);
      }
    }
  }

  /**
   * Schema:
   * <xsd:element name="ANY" type="xsd:anyType"/>
   * 
   * All attributes and child elements are defined in schema. 
   */
  public void testAcceptanceForANY_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

    final String xmlString = 
      "<foo:ANY xmlns:foo='urn:foo' xmlns:goo='urn:goo'\n" + 
      "  foo:aA='a' foo:aB='b' foo:aC='c' >\n" +
      "  TEXT 1 " +
      "  <goo:AB> </goo:AB>\n" +
      "  TEXT 2 " +
      "  <goo:AC> </goo:AC>\n" +
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
      Assert.assertEquals(1, eventTypeList.getLength());
      
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
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
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
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
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
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("\n  TEXT 1   ", exiEvent.getCharacters().makeString());
      eventType = (EventTypeSchema)exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
      Assert.assertEquals(5, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
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
      Assert.assertEquals(5, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
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
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("\n  TEXT 2   ", exiEvent.getCharacters().makeString());
      eventType = (EventTypeSchema)exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
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
      Assert.assertEquals(5, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(11);
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
  
      exiEvent = exiEventList.get(12);
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
  
      exiEvent = exiEventList.get(13);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("\n  TEXT 3 ", exiEvent.getCharacters().makeString());
      eventType = (EventTypeSchema)exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(14);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = (EventTypeSchema)exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(15);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
    }
  }
  
  /**
   * Schema:
   * <xsd:element name="ANY" type="xsd:anyType"/>
   * 
   * Use of elements that are not defined in schema.
   */
  public void testAcceptanceForANY_02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

    final String xmlString = 
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
          Assert.assertEquals(1, eventTypeList.getLength());
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
          Assert.assertEquals(5, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(10, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
          eventType = eventTypeList.item(6);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(7);
          Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
          eventType = eventTypeList.item(8);
          Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(9);
          Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
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
          Assert.assertEquals(5, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
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
          Assert.assertEquals(5, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
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
          Assert.assertEquals(5, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
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
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.assertEquals(4, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertEquals(5, eventTypeList.getLength());
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
          Assert.assertEquals(5, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
        }
    
        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
          Assert.assertEquals(1, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(5, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
        }
        
        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
          eventType = exiEvent.getEventType();
          Assert.assertSame(exiEvent, eventType);
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(1, eventTypeList.getLength());
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
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
    
    final String xmlString = 
      "<J xmlns='urn:foo'>" +
        "<AB> </AB><AC> </AC><AC> </AC><AB> </AB><AC> </AC>" +
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
      
      byte[] bts;
      int n_events;
      
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
      Assert.assertEquals(1, eventTypeList.getLength());
      
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
  
      exiEvent = exiEventList.get(3);
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
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AC", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AC", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(6);
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
  
      exiEvent = exiEventList.get(7);
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
      Assert.assertEquals(5, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(9);
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
  
      exiEvent = exiEventList.get(10);
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
      Assert.assertEquals(4, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(12);
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
  
      exiEvent = exiEventList.get(13);
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
      Assert.assertEquals(4, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(15);
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
  
      exiEvent = exiEventList.get(16);
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
  
      exiEvent = exiEventList.get(17);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(4, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AC", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(18);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
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
  public void testAcceptanceForJ_01_with_UndeclaredElement() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
    
    final String xmlString = 
      "<J xmlns='urn:foo'>" +
        "<AB/><AC/><AC/><AC/>" + // The 3rd <AC/> is not expected.
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
      
      byte[] bts;
      int n_events;
      
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
      
      Assert.assertEquals(12, n_events);
  
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
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(8, eventTypeList.getLength());
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
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AC", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AC", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(8, eventTypeList.getLength());
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
  
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AC", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AC", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(7);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(8, eventTypeList.getLength());
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
      
      exiEvent = exiEventList.get(8);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AC", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(4, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(9);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(8, eventTypeList.getLength());
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
  
      exiEvent = exiEventList.get(10);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(4, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(11);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
    }
  }

  /**
   * Make use of ITEM_UNDECLARED_EE that belongs to an ElementGrammar.
   * 
   * Schema:
   * <xsd:element name="A">
   *   <xsd:complexType>
   *     <xsd:sequence>
   *       <xsd:sequence>
   *         <xsd:element ref="foo:AB"/>
   *         ......
   *       </xsd:sequence>
   *       ......
   *     </xsd:sequence>
   *   </xsd:complexType>
   * </xsd:element>
   * 
   * Instance:
   * <A></A>
   */
  public void testUndeclaredEEOfElementGrammar() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
    
    final String xmlString = 
      "<A xmlns='urn:foo'>" +
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
      
      byte[] bts;
      int n_events;
      
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
      
      Assert.assertEquals(4, n_events);
  
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
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      // This ITEM_UNDECLARED_EE belongs to ElementGrammar  
      Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT, EventTypeAccessor.getGrammar(eventType).getGrammarType());
      Assert.assertEquals(3, eventType.getIndex());
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
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
    }
  }

  /**
   * Make use of ITEM_UNDECLARED_EE that belongs to an ElementTagGrammar.
   * 
   * Schema:
   * <xsd:complexType name="B">
   *   <xsd:sequence>
   *     <xsd:element ref="foo:AB"/>
   *     <xsd:element ref="foo:AC" minOccurs="0" maxOccurs="2"/>
   *     <xsd:element ref="foo:AD" minOccurs="0"/>
   *   </xsd:sequence>
   * </xsd:complexType>
   * 
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
   * <B xsi:type="restricted_B"></B>
   */
  public void testUndeclaredEEOfElementTagGrammar_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
    
    final String xmlString = 
      "<B xsi:type='restricted_B' xmlns='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>" +
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
      
      byte[] bts;
      int n_events;
      
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
      
      Assert.assertEquals(5, n_events);
  
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
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      // This ITEM_UNDECLARED_EE belongs to ElementTagGrammar  
      Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_TAG, EventTypeAccessor.getGrammar(eventType).getGrammarType());
      Assert.assertEquals(3, eventType.getIndex());
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
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
    }
  }

  /**
   * Make use of ITEM_UNDECLARED_EE that belongs to an ElementTagGrammar, 
   * together with xsi:nil="true" attribute.
   * 
   * Schema:
   * <xsd:complexType name="F">
   *   <xsd:sequence>
   *   ...
   *   </xsd:sequence>
   *   <xsd:attribute ref="foo:aA" use="required"/>
   *   ...
   * </xsd:complexType>
   * 
   * <xsd:element name="F" type="foo:F" nillable="true"/>
   * 
   * Instance:
   * <F xsi:type="F" xsi:nil="true"></F>
   */
  public void testUndeclaredEEOfElementTagGrammar_02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
    
    final String xmlString = 
      "<F xsi:type='F' xsi:nil='true' xmlns='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>" +
      "</F>\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      byte[] bts;
      int n_events;
      
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
      Assert.assertEquals("F", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_TP, exiEvent.getEventVariety());
      Assert.assertEquals("type", exiEvent.getName());
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
      Assert.assertEquals("F", ((EXIEventSchemaType)exiEvent).getTypeName());
      Assert.assertEquals("urn:foo", ((EXIEventSchemaType)exiEvent).getTypeURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      Assert.assertEquals("type", eventType.getName());
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.getURI());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aA", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("aA", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_NL, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aA", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("aA", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      // This ITEM_UNDECLARED_EE belongs to ElementTagGrammar  
      Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_TAG, EventTypeAccessor.getGrammar(eventType).getGrammarType());
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aA", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("aA", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
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
   * Make use of ITEM_UNDECLARED_EE that belongs to an ComplexContentGrammar.
   * 
   * Schema:
   * <xsd:element name="L">
   *   <xsd:complexType>
   *     <xsd:sequence minOccurs="2" maxOccurs="2">
   *       <xsd:element ref="foo:AB"/>
   *     </xsd:sequence>
   *   </xsd:complexType>
   * </xsd:element>
   * 
   * Instance:
   * <L>
   *   <AB>xyz</AB>
   * </L>
   */
  public void testUndeclaredEEOfComplexContentGrammar() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
    
    final String xmlString = 
      "<L xmlns='urn:foo'>" +
        "<AB>xyz</AB>" +
      "</L>\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      byte[] bts;
      int n_events;
      
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
      Assert.assertEquals(1, eventTypeList.getLength());
      
      exiEvent = exiEventList.get(1);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("L", eventType.getName());
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
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("xyz", exiEvent.getCharacters().makeString());
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
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      // This ITEM_UNDECLARED_EE belongs to ComplexContentGrammar  
      SchemaInformedGrammar grammar = (SchemaInformedGrammar)EventTypeAccessor.getGrammar(eventType);
      Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_CONTENT, grammar.getGrammarType());
      Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, GrammarAccessor.getEXISchema(grammar).getNodeType(GrammarAccessor.getNode(grammar)));
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(4, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
    }
  }

  /**
   * Make use of ITEM_UNDECLARED_EE that belongs to an SimpleContentGrammar.
   * 
   * Schema:
   * <xsd:element name="AB" type="xsd:anySimpleType"/>
   * 
   * Instance:
   * The element <AC/> is not expected.
   * <AB><AC/></AB>
   */
  public void testUndeclaredEEOfSimpleContentGrammar() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
    
    final String xmlString = "<AB xmlns='urn:foo'><AC/></AB>\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      byte[] bts;
      int n_events;
      
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
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AC", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      Assert.assertEquals(6, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(8, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
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
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(8, eventTypeList.getLength());
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
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      // This ITEM_UNDECLARED_EE belongs to SimpleContentGrammar  
      SchemaInformedGrammar grammar = (SchemaInformedGrammar)EventTypeAccessor.getGrammar(eventType);
      Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_CONTENT, grammar.getGrammarType());
      Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, GrammarAccessor.getEXISchema(grammar).getNodeType(GrammarAccessor.getNode(grammar)));
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(4, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
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
   * Schema:
   * <xsd:element name="AB" type="xsd:anySimpleType"/>
   *
   * Instance:
   * <AB xmlns="urn:foo" foo:aA="abc">xyz</AB>
   */
  public void testUndeclaredAttrWildcardAnyOfElementGrammar() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache;
    
    grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
    
    final String xmlString = 
      "<foo:AB xmlns:foo='urn:foo' foo:aA='abc'>xyz</foo:AB>";
  
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
      Assert.assertEquals("AB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
  
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      Assert.assertEquals("aA", exiEvent.getName());
      Assert.assertEquals("abc", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertEquals(4, eventType.getIndex());
      Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT, EventTypeAccessor.getGrammar(eventType).getGrammarType());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(8, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("xyz", exiEvent.getCharacters().makeString());
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
   * <xsd:element name="AB" type="xsd:anySimpleType"/>
   *
   * Instance:
   * <AB xmlns="urn:foo" xsi:type="xsd:string" foo:aA="abc">xyz</AB>
   */
  public void testUndeclaredAttrWildcardAnyOfElementTagGrammar_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache;
    
    grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
    
    final String xmlString = 
      "<foo:AB xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' \n" +
      "  xmlns:xsd='http://www.w3.org/2001/XMLSchema' \n" +
      "  xmlns:foo='urn:foo' xsi:type='xsd:string' foo:aA='abc'>" +
      "xyz</foo:AB>";
  
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
      
      Assert.assertEquals(7, n_events);
  
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
      Assert.assertEquals("AB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
  
      exiEvent = exiEventList.get(2);
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
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      Assert.assertEquals("aA", exiEvent.getName());
      Assert.assertEquals("abc", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertEquals(4, eventType.getIndex());
      Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_TAG, EventTypeAccessor.getGrammar(eventType).getGrammarType());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(8, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
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
      Assert.assertEquals("xyz", exiEvent.getCharacters().makeString());
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
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
    }
  }

  /**
   * Schema:
   * <xsd:element name="AB" type="xsd:anySimpleType"/>
   *
   * Instance:
   * <AB xmlns="urn:foo" xsi:type="xsd:string" xsi:nil="true" foo:aA="abc"></AB>
   */
  public void testUndeclaredAttrWildcardAnyOfElementTagGrammar_02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache;
    
    grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
    
    final String xmlString = 
      "<foo:AB xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' \n" +
      "  xmlns:xsd='http://www.w3.org/2001/XMLSchema' \n" +
      "  xmlns:foo='urn:foo' xsi:type='xsd:string' xsi:nil='true' foo:aA='abc'>" +
      "</foo:AB>";
  
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
      
      Assert.assertEquals(7, n_events);
  
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
      Assert.assertEquals("AB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
  
      exiEvent = exiEventList.get(2);
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
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_NL, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      Assert.assertTrue(((EXIEventSchemaNil)exiEvent).isNilled());
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(8, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
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
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      Assert.assertEquals("aA", exiEvent.getName());
      Assert.assertEquals("abc", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertEquals(3, eventType.getIndex());
      Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_TAG, EventTypeAccessor.getGrammar(eventType).getGrammarType());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(7, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(7, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
    }
  }

  /**
   * Test the uses of ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE and 
   * ITEM_AT_WC_ANY.
   * 
   * Schema:
   * <xsd:element name="M">
   *   <xsd:complexType>
   *     <xsd:attribute ref="foo:bA" />
   *     <xsd:attribute ref="foo:bB" use="required" />
   *     <xsd:attribute ref="foo:bC" />
   *     <xsd:attribute ref="foo:bD" />
   *   </xsd:complexType>
   * </xsd:element>
   */
  public void testUseInvalidAttributes() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

    final String xmlString = 
      "<foo:M xmlns:foo='urn:foo' xmlns:goo='urn:goo' " +
      "       goo:b0='' foo:bA='' goo:bA='' foo:bB='' goo:bB='' " + 
      "       foo:bC='' goo:bC='' foo:bD='' goo:bD='' >" +
      "</foo:M>\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos;
  
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
      
      Assert.assertEquals(13, n_events);
  
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
      Assert.assertEquals("M", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
  
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
      Assert.assertEquals("b0", exiEvent.getName());
      Assert.assertEquals("urn:goo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertEquals(5, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(11, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("bA", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("bB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("bA", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("bB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
      Assert.assertEquals("bA", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("bA", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(6, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(11, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("bA", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("bB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("bB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
      Assert.assertEquals("bA", exiEvent.getName());
      Assert.assertEquals("urn:goo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      Assert.assertEquals(4, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(7, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("bB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("bB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
      Assert.assertEquals("bB", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("bB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(7, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("bB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
      Assert.assertEquals("bB", exiEvent.getName());
      Assert.assertEquals("urn:goo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      Assert.assertEquals(6, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("bC", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("bD", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("bC", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("bD", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(7);
      Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
      Assert.assertEquals("bC", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("bC", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(4, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("bC", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("bD", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("bD", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(8);
      Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
      Assert.assertEquals("bC", exiEvent.getName());
      Assert.assertEquals("urn:goo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      Assert.assertEquals(4, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(7, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("bD", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("bD", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(9);
      Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
      Assert.assertEquals("bD", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("bD", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(7, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("bD", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(10);
      Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
      Assert.assertEquals("bD", exiEvent.getName());
      Assert.assertEquals("urn:goo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(11);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(12);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
    }
  }

  /**
   * Make use of ITEM_SCHEMA_NS that belongs to an ElementGrammar and ElementTagGrammar.
   * Note that the ITEM_SCHEMA_NS event in ElementTagGrammar cannot be exercised since
   * it never matches an namespace declaration instance. 
   * 
   * Schema:
   * <xsd:complexType name="F">
   *   <xsd:sequence>
   *   ...
   *   </xsd:sequence>
   *   <xsd:attribute ref="foo:aA" use="required"/>
   *   ...
   * </xsd:complexType>
   * 
   * <xsd:element name="F" type="foo:F" nillable="true"/>
   */
  public void testNamespaceDeclaration_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, 
        GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));
    
    final String xmlString = 
      "<F xsi:type='F' xmlns='urn:foo' xmlns:foo='urn:foo' " + 
      "   xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'" + 
      "   foo:aA='abc'>" + 
      "</F>\n";
    
    for (AlignmentType alignment : Alignments) {
      for (boolean preserveLexicalValues : new boolean[] { true, false }) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;
        
        encoder.setAlignmentType(alignment);
        decoder.setAlignmentType(alignment);
  
        encoder.setPreserveLexicalValues(preserveLexicalValues);
        decoder.setPreserveLexicalValues(preserveLexicalValues);
  
        encoder.setEXISchema(grammarCache);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        encoder.setOutputStream(baos);
        
        byte[] bts;
        int n_events;
        
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
        
        Assert.assertEquals(9, n_events);
    
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
        Assert.assertEquals("F", exiEvent.getName());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        Assert.assertNull(exiEvent.getPrefix());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("F", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertNull(eventTypeList.getEE());
    
        for (int i = 2; i < 5; i++) {
          exiEvent = exiEventList.get(i);
          Assert.assertEquals(EXIEvent.EVENT_NS, exiEvent.getEventVariety());
          switch (i) {
            case 2:
              Assert.assertEquals("", exiEvent.getPrefix());
              Assert.assertEquals("urn:foo", exiEvent.getURI());
              Assert.assertTrue(((EXIEventNS)exiEvent).getLocalElementNs());
              break;
            case 3:
              Assert.assertEquals("foo", exiEvent.getPrefix());
              Assert.assertEquals("urn:foo", exiEvent.getURI());
              Assert.assertFalse(((EXIEventNS)exiEvent).getLocalElementNs());
              break;
            case 4:
              Assert.assertEquals("xsi", exiEvent.getPrefix());
              Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
              Assert.assertFalse(((EXIEventNS)exiEvent).getLocalElementNs());
              break;
            default:
              break;
          }
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventCode.ITEM_NS, eventType.itemType);
          Assert.assertNull(eventType.getName());
          Assert.assertEquals(XmlUriConst.W3C_XMLNS_2000_URI, eventType.getURI());
          // This NS event type should belong to ElementGrammar
          Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT, EventTypeAccessor.getGrammar(eventType).getGrammarType());
          Assert.assertEquals(7, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(10, eventTypeList.getLength());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
          Assert.assertEquals("aA", eventType.getName());
          Assert.assertEquals("urn:foo", eventType.getURI());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(5);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
          Assert.assertEquals("aA", eventType.getName());
          Assert.assertEquals("urn:foo", eventType.getURI());
          eventType = eventTypeList.item(6);
          Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
          eventType = eventTypeList.item(8);
          Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(9);
          Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
        }
        
        exiEvent = exiEventList.get(5);
        Assert.assertEquals(EXIEvent.EVENT_TP, exiEvent.getEventVariety());
        Assert.assertEquals("type", exiEvent.getName());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
        Assert.assertEquals("F", ((EXIEventSchemaType)exiEvent).getTypeName());
        Assert.assertEquals("urn:foo", ((EXIEventSchemaType)exiEvent).getTypeURI());
        Assert.assertEquals("", ((EXIEventSchemaType)exiEvent).getTypePrefix());
        Assert.assertTrue(preserveLexicalValues && "F".equals(exiEvent.getCharacters().makeString()) ||
            !preserveLexicalValues && exiEvent.getCharacters() == null); 
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
        Assert.assertEquals("type", eventType.getName());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.getURI());
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(10, eventTypeList.getLength());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.assertEquals("aA", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
        Assert.assertEquals("aA", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        eventType = eventTypeList.item(6);
        Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventCode.ITEM_NS, eventType.itemType);
        Assert.assertEquals("http://www.w3.org/2000/xmlns/", eventType.getURI());
        // This NS event type should belong to ElementGrammar
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT, EventTypeAccessor.getGrammar(eventType).getGrammarType());
        eventType = eventTypeList.item(8);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
    
        exiEvent = exiEventList.get(6);
        Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
        Assert.assertEquals("aA", exiEvent.getName());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        Assert.assertEquals("foo", exiEvent.getPrefix());
        Assert.assertEquals("abc", exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.assertEquals("aA", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        Assert.assertEquals(2, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(10, eventTypeList.getLength());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.assertEquals("aA", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
        Assert.assertEquals("aA", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        eventType = eventTypeList.item(6);
        Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventCode.ITEM_NS, eventType.itemType);
        Assert.assertEquals("http://www.w3.org/2000/xmlns/", eventType.getURI());
        // This NS event type should belong to ElementTagGrammar
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_TAG, EventTypeAccessor.getGrammar(eventType).getGrammarType());
        eventType = eventTypeList.item(8);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
    
        exiEvent = exiEventList.get(7);
        Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        Assert.assertEquals(2, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(8, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.assertEquals("aB", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("AB", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
        Assert.assertEquals("aB", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
        eventType = eventTypeList.item(6);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
    
        exiEvent = exiEventList.get(8);
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
   * Nested elements with namespace declarations.
   * 
   * Schema:
   * <xsd:element name="H">
   *   <xsd:complexType>
   *     <xsd:sequence>
   *       <xsd:element name="A" minOccurs="0"/>
   *       <xsd:any namespace="urn:eoo urn:goo" />
   *       .....
   *     </xsd:sequence>
   *   </xsd:complexType>
   * </xsd:element>
   */
  public void testNamespaceDeclaration_02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, 
        GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));
    
    final String xmlString = 
      "<H xmlns='urn:foo' xmlns:goo1='urn:goo' xmlns:goo2='urn:goo' " + 
      "   goo1:aA='abc' >" +
        "<goo2:AB xmlns='urn:goo' xmlns:goo2='urn:goo' xmlns:foo='urn:foo'/>" +
      "</H>\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      byte[] bts;
      int n_events;
      
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
      
      Assert.assertEquals(13, n_events);
  
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
      Assert.assertEquals("H", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      Assert.assertNull(exiEvent.getPrefix());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("H", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
  
      for (int i = 2; i < 5; i++) {
        exiEvent = exiEventList.get(i);
        Assert.assertEquals(EXIEvent.EVENT_NS, exiEvent.getEventVariety());
        switch (i) {
          case 2:
            Assert.assertEquals("", exiEvent.getPrefix());
            Assert.assertEquals("urn:foo", exiEvent.getURI());
            Assert.assertTrue(((EXIEventNS)exiEvent).getLocalElementNs());
            break;
          case 3:
            Assert.assertEquals("goo1", exiEvent.getPrefix());
            Assert.assertEquals("urn:goo", exiEvent.getURI());
            Assert.assertFalse(((EXIEventNS)exiEvent).getLocalElementNs());
            break;
          case 4:
            Assert.assertEquals("goo2", exiEvent.getPrefix());
            Assert.assertEquals("urn:goo", exiEvent.getURI());
            Assert.assertFalse(((EXIEventNS)exiEvent).getLocalElementNs());
            break;
          default:
            break;
        }
      
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_NS, eventType.itemType);
        Assert.assertNull(eventType.getName());
        Assert.assertEquals(XmlUriConst.W3C_XMLNS_2000_URI, eventType.getURI());
        // This NS event type should belong to ElementGrammar
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT, EventTypeAccessor.getGrammar(eventType).getGrammarType());
        Assert.assertEquals(8, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(11, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("A", eventType.getName());
        Assert.assertEquals("", eventType.getURI());
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.assertEquals("urn:eoo", eventType.getURI());
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.assertEquals("urn:goo", eventType.getURI());
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(6);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
        eventType = eventTypeList.item(9);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(10);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      }
  
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
      Assert.assertEquals("aA", exiEvent.getName());
      Assert.assertEquals("urn:goo", exiEvent.getURI());
      Assert.assertEquals("goo1", exiEvent.getPrefix());
      Assert.assertEquals("abc", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertEquals(6, eventType.getIndex());
      Assert.assertEquals(11, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:eoo", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:goo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_NS, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(XmlUriConst.W3C_XMLNS_2000_URI, eventType.getURI());
      // This NS event type should belong to ElementGrammar
      Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT, EventTypeAccessor.getGrammar(eventType).getGrammarType());
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AB", exiEvent.getName());
      Assert.assertEquals("urn:goo", exiEvent.getURI());
      Assert.assertEquals("goo2", exiEvent.getPrefix());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:goo", eventType.getURI());
      Assert.assertEquals(4, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(11, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("A", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_NS, eventType.itemType);
      Assert.assertEquals("urn:eoo", eventType.getURI());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_NS, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(XmlUriConst.W3C_XMLNS_2000_URI, eventType.getURI());
      // This NS event type should belong to ElementGrammar
      Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT, EventTypeAccessor.getGrammar(eventType).getGrammarType());
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      for (int i = 7; i < 10; i++) {
        exiEvent = exiEventList.get(i);
        Assert.assertEquals(EXIEvent.EVENT_NS, exiEvent.getEventVariety());
        switch (i) {
          case 7:
            Assert.assertEquals("", exiEvent.getPrefix());
            Assert.assertEquals("urn:goo", exiEvent.getURI());
            Assert.assertFalse(((EXIEventNS)exiEvent).getLocalElementNs());
            break;
          case 8:
            Assert.assertEquals("goo2", exiEvent.getPrefix());
            Assert.assertEquals("urn:goo", exiEvent.getURI());
            Assert.assertTrue(((EXIEventNS)exiEvent).getLocalElementNs());
            break;
          case 9:
            Assert.assertEquals("foo", exiEvent.getPrefix());
            Assert.assertEquals("urn:foo", exiEvent.getURI());
            Assert.assertFalse(((EXIEventNS)exiEvent).getLocalElementNs());
            break;
          default:
            break;
        }
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_NS, eventType.itemType);
        Assert.assertNull(eventType.getName());
        Assert.assertEquals(XmlUriConst.W3C_XMLNS_2000_URI, eventType.getURI());
        // This NS event type should belong to ElementGrammar
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT, EventTypeAccessor.getGrammar(eventType).getGrammarType());
        Assert.assertEquals(6, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(9, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
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
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      }
      
      exiEvent = exiEventList.get(10);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9, eventTypeList.getLength());
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
      Assert.assertEquals(EventCode.ITEM_NS, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(XmlUriConst.W3C_XMLNS_2000_URI, eventType.getURI());
      // This NS event type should belong to ElementGrammar
      Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT, EventTypeAccessor.getGrammar(eventType).getGrammarType());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(11);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(4, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(12);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
    }
  }

  /**
   * Nested elements with namespace declarations, where two or more
   * prefixes are assigned to the same URI. 
   * 
   * Schema:
   * <xsd:complexType name="B">
   *   <xsd:sequence>
   *     <xsd:element ref="foo:AB"/>
   *     <xsd:element ref="foo:AC" minOccurs="0" maxOccurs="2"/>
   *     <xsd:element ref="foo:AD" minOccurs="0"/>
   *   </xsd:sequence>
   * </xsd:complexType>
   */
  public void testNamespaceDeclaration_03() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, 
        GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));
    
    final String xmlString = 
      "<B xmlns='urn:foo' xmlns:goo='urn:goo' xmlns:foo='urn:foo'>" +
        "<AB xmlns:hoo='urn:hoo'/>" +
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
      
      byte[] bts;
      int n_events;
      
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
      Assert.assertEquals(1, eventTypeList.getLength());
      
      exiEvent = exiEventList.get(1);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("B", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      Assert.assertNull(exiEvent.getPrefix());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
  
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_NS, exiEvent.getEventVariety());
      Assert.assertEquals("", exiEvent.getPrefix());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      Assert.assertTrue(((EXIEventNS)exiEvent).getLocalElementNs());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_NS, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(XmlUriConst.W3C_XMLNS_2000_URI, eventType.getURI());
      // This NS event type should belong to ElementGrammar
      Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT, EventTypeAccessor.getGrammar(eventType).getGrammarType());
      Assert.assertEquals(6, eventType.getIndex());
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
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_NS, exiEvent.getEventVariety());
      Assert.assertEquals("goo", exiEvent.getPrefix());
      Assert.assertEquals("urn:goo", exiEvent.getURI());
      Assert.assertFalse(((EXIEventNS)exiEvent).getLocalElementNs());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_NS, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(XmlUriConst.W3C_XMLNS_2000_URI, eventType.getURI());
      // This NS event type should belong to ElementGrammar
      Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT, EventTypeAccessor.getGrammar(eventType).getGrammarType());
      Assert.assertEquals(6, eventType.getIndex());
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
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_NS, exiEvent.getEventVariety());
      Assert.assertEquals("foo", exiEvent.getPrefix());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      Assert.assertFalse(((EXIEventNS)exiEvent).getLocalElementNs());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_NS, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(XmlUriConst.W3C_XMLNS_2000_URI, eventType.getURI());
      // This NS event type should belong to ElementGrammar
      Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT, EventTypeAccessor.getGrammar(eventType).getGrammarType());
      Assert.assertEquals(6, eventType.getIndex());
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
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AB", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      Assert.assertEquals("", exiEvent.getPrefix());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9, eventTypeList.getLength());
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
      Assert.assertEquals(EventCode.ITEM_NS, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(XmlUriConst.W3C_XMLNS_2000_URI, eventType.getURI());
      // This NS event type should belong to ElementGrammar
      Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT, EventTypeAccessor.getGrammar(eventType).getGrammarType());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_NS, exiEvent.getEventVariety());
      Assert.assertEquals("hoo", exiEvent.getPrefix());
      Assert.assertEquals("urn:hoo", exiEvent.getURI());
      Assert.assertFalse(((EXIEventNS)exiEvent).getLocalElementNs());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_NS, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(XmlUriConst.W3C_XMLNS_2000_URI, eventType.getURI());
      Assert.assertEquals(6, eventType.getIndex());
      // This NS event type should belong to ElementGrammar
      Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT, EventTypeAccessor.getGrammar(eventType).getGrammarType());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
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
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(7);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9, eventTypeList.getLength());
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
      Assert.assertEquals(EventCode.ITEM_NS, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(XmlUriConst.W3C_XMLNS_2000_URI, eventType.getURI());
      // This NS event type should belong to ElementGrammar
      Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT, EventTypeAccessor.getGrammar(eventType).getGrammarType());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(8);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
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
  
      exiEvent = exiEventList.get(9);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
    }
  }

  /**
   * xsi:nil attribute with a single corresponding namespace declaration.
   * 
   * Schema:
   * <xsd:complexType name="B">
   *   <xsd:sequence>
   *     <xsd:element ref="foo:AB"/>
   *     ...
   *   </xsd:sequence>
   * </xsd:complexType>
   */
  public void testNamespaceDeclaration_04() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, 
        GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));
    
    final String xmlString = 
      "<B xmlns='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " +
      "   xsi:nil='true' >" +
      "</B>\n";
    
    for (AlignmentType alignment : Alignments) {
      for (boolean preserveLexicalValues : new boolean[] { true, false }) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;
        
        encoder.setAlignmentType(alignment);
        decoder.setAlignmentType(alignment);
  
        encoder.setPreserveLexicalValues(preserveLexicalValues);
        decoder.setPreserveLexicalValues(preserveLexicalValues);
  
        encoder.setEXISchema(grammarCache);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        encoder.setOutputStream(baos);
        
        byte[] bts;
        int n_events;
        
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
        Assert.assertEquals(1, eventTypeList.getLength());
        
        exiEvent = exiEventList.get(1);
        Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
        Assert.assertEquals("B", exiEvent.getName());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        Assert.assertNull(exiEvent.getPrefix());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("B", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertNull(eventTypeList.getEE());
    
        exiEvent = exiEventList.get(2);
        Assert.assertEquals(EXIEvent.EVENT_NS, exiEvent.getEventVariety());
        Assert.assertEquals("", exiEvent.getPrefix());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        Assert.assertTrue(((EXIEventNS)exiEvent).getLocalElementNs());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_NS, eventType.itemType);
        Assert.assertNull(eventType.getName());
        Assert.assertEquals(XmlUriConst.W3C_XMLNS_2000_URI, eventType.getURI());
        // This NS event type should belong to ElementGrammar
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT, EventTypeAccessor.getGrammar(eventType).getGrammarType());
        Assert.assertEquals(6, eventType.getIndex());
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
        Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
    
        exiEvent = exiEventList.get(3);
        Assert.assertEquals(EXIEvent.EVENT_NS, exiEvent.getEventVariety());
        Assert.assertEquals("xsi", exiEvent.getPrefix());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
        Assert.assertFalse(((EXIEventNS)exiEvent).getLocalElementNs());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_NS, eventType.itemType);
        Assert.assertNull(eventType.getName());
        Assert.assertEquals(XmlUriConst.W3C_XMLNS_2000_URI, eventType.getURI());
        // This NS event type should belong to ElementGrammar
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT, EventTypeAccessor.getGrammar(eventType).getGrammarType());
        Assert.assertEquals(6, eventType.getIndex());
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
        Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
    
        exiEvent = exiEventList.get(4);
        Assert.assertEquals(EXIEvent.EVENT_NL, exiEvent.getEventVariety());
        Assert.assertEquals("nil", exiEvent.getName());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
        Assert.assertEquals("xsi", exiEvent.getPrefix());
        Assert.assertTrue(((EXIEventSchemaNil)exiEvent).isNilled());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
        Assert.assertEquals(1, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(9, eventTypeList.getLength());
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
        Assert.assertEquals(EventCode.ITEM_NS, eventType.itemType);
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
    
        exiEvent = exiEventList.get(5);
        Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(2, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(8, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventCode.ITEM_NS, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
    
        exiEvent = exiEventList.get(6);
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
   * xsi:nil attribute with two corresponding namespace declarations.
   * 
   * Schema:
   * <xsd:complexType name="B">
   *   <xsd:sequence>
   *     <xsd:element ref="foo:AB"/>
   *     ...
   *   </xsd:sequence>
   * </xsd:complexType>
   */
  public void testNamespaceDeclaration_05() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, 
        GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));
    
    final String xmlString = 
      "<B xmlns='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " +
      "   xmlns:xsh='http://www.w3.org/2001/XMLSchema-instance' " +
      "   xsh:nil='true' >" +
      "</B>\n";
    
    for (AlignmentType alignment : Alignments) {
      for (boolean preserveLexicalValues : new boolean[] { true, false }) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;
        
        encoder.setAlignmentType(alignment);
        decoder.setAlignmentType(alignment);
  
        encoder.setPreserveLexicalValues(preserveLexicalValues);
        decoder.setPreserveLexicalValues(preserveLexicalValues);
  
        encoder.setEXISchema(grammarCache);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        encoder.setOutputStream(baos);
        
        byte[] bts;
        int n_events;
        
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
        Assert.assertEquals("B", exiEvent.getName());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        Assert.assertNull(exiEvent.getPrefix());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("B", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertNull(eventTypeList.getEE());
    
        exiEvent = exiEventList.get(2);
        Assert.assertEquals(EXIEvent.EVENT_NS, exiEvent.getEventVariety());
        Assert.assertEquals("", exiEvent.getPrefix());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        Assert.assertTrue(((EXIEventNS)exiEvent).getLocalElementNs());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_NS, eventType.itemType);
        Assert.assertNull(eventType.getName());
        Assert.assertEquals(XmlUriConst.W3C_XMLNS_2000_URI, eventType.getURI());
        // This NS event type should belong to ElementGrammar
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT, EventTypeAccessor.getGrammar(eventType).getGrammarType());
        Assert.assertEquals(6, eventType.getIndex());
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
        Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
    
        exiEvent = exiEventList.get(3);
        Assert.assertEquals(EXIEvent.EVENT_NS, exiEvent.getEventVariety());
        Assert.assertEquals("xsi", exiEvent.getPrefix());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
        Assert.assertFalse(((EXIEventNS)exiEvent).getLocalElementNs());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_NS, eventType.itemType);
        Assert.assertNull(eventType.getName());
        Assert.assertEquals(XmlUriConst.W3C_XMLNS_2000_URI, eventType.getURI());
        // This NS event type should belong to ElementGrammar
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT, EventTypeAccessor.getGrammar(eventType).getGrammarType());
        Assert.assertEquals(6, eventType.getIndex());
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
        Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
    
        exiEvent = exiEventList.get(4);
        Assert.assertEquals(EXIEvent.EVENT_NS, exiEvent.getEventVariety());
        Assert.assertEquals("xsh", exiEvent.getPrefix());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
        Assert.assertFalse(((EXIEventNS)exiEvent).getLocalElementNs());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_NS, eventType.itemType);
        Assert.assertNull(eventType.getName());
        Assert.assertEquals(XmlUriConst.W3C_XMLNS_2000_URI, eventType.getURI());
        // This NS event type should belong to ElementGrammar
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT, EventTypeAccessor.getGrammar(eventType).getGrammarType());
        Assert.assertEquals(6, eventType.getIndex());
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
        Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
    
        exiEvent = exiEventList.get(5);
        Assert.assertEquals(EXIEvent.EVENT_NL, exiEvent.getEventVariety());
        Assert.assertEquals("nil", exiEvent.getName());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
        Assert.assertEquals("xsh", exiEvent.getPrefix());
        Assert.assertTrue(((EXIEventSchemaNil)exiEvent).isNilled());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
        Assert.assertEquals(1, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(9, eventTypeList.getLength());
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
        Assert.assertEquals(EventCode.ITEM_NS, eventType.itemType);
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
    
        exiEvent = exiEventList.get(6);
        Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(2, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(8, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventCode.ITEM_NS, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
    
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

  /**
   * xsi:type attribute with a single corresponding namespace declaration.
   * 
   * Schema: 
   * <xsd:complexType name="restricted_B">
   *   <xsd:complexContent>
   *     <xsd:restriction base="foo:B">
   *       <xsd:sequence>
   *         <xsd:element ref="foo:AB"/>
   *         ...
   *       </xsd:sequence>
   *     </xsd:restriction>
   *   </xsd:complexContent>
   * </xsd:complexType>
   */
  public void testNamespaceDeclaration_06() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, 
        GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

    final String xmlString = 
      "<B xmlns='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " + 
      "   xsi:type='restricted_B' xsi:nil='true'>" +
      "</B>\n";
    
    for (AlignmentType alignment : Alignments) {
      for (boolean preserveLexicalValues : new boolean[] { true, false }) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;
        
        encoder.setAlignmentType(alignment);
        decoder.setAlignmentType(alignment);
  
        encoder.setPreserveLexicalValues(preserveLexicalValues);
        decoder.setPreserveLexicalValues(preserveLexicalValues);
        
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
        Assert.assertEquals("B", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertNull(eventTypeList.getEE());
        
        exiEvent = exiEventList.get(2);
        Assert.assertEquals(EXIEvent.EVENT_NS, exiEvent.getEventVariety());
        Assert.assertEquals("", exiEvent.getPrefix());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        Assert.assertTrue(((EXIEventNS)exiEvent).getLocalElementNs());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_NS, eventType.itemType);
        Assert.assertNull(eventType.getName());
        Assert.assertEquals(XmlUriConst.W3C_XMLNS_2000_URI, eventType.getURI());
        // This NS event type should belong to ElementGrammar
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT, EventTypeAccessor.getGrammar(eventType).getGrammarType());
        Assert.assertEquals(6, eventType.getIndex());
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
        Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
    
        exiEvent = exiEventList.get(3);
        Assert.assertEquals(EXIEvent.EVENT_NS, exiEvent.getEventVariety());
        Assert.assertEquals("xsi", exiEvent.getPrefix());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
        Assert.assertFalse(((EXIEventNS)exiEvent).getLocalElementNs());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_NS, eventType.itemType);
        Assert.assertNull(eventType.getName());
        Assert.assertEquals(XmlUriConst.W3C_XMLNS_2000_URI, eventType.getURI());
        // This NS event type should belong to ElementGrammar
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT, EventTypeAccessor.getGrammar(eventType).getGrammarType());
        Assert.assertEquals(6, eventType.getIndex());
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
        Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
    
        exiEvent = exiEventList.get(4);
        Assert.assertEquals(EXIEvent.EVENT_TP, exiEvent.getEventVariety());
        Assert.assertEquals("type", exiEvent.getName());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
        Assert.assertEquals("xsi", exiEvent.getPrefix());
        Assert.assertEquals("restricted_B", ((EXIEventSchemaType)exiEvent).getTypeName());
        Assert.assertEquals("urn:foo", ((EXIEventSchemaType)exiEvent).getTypeURI());
        Assert.assertEquals("", ((EXIEventSchemaType)exiEvent).getTypePrefix());
        Assert.assertTrue(preserveLexicalValues && "restricted_B".equals(exiEvent.getCharacters().makeString()) ||
            !preserveLexicalValues && exiEvent.getCharacters() == null);
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
        Assert.assertEquals("type", eventType.getName());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.getURI());
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
        Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
        eventType = eventTypeList.item(6);
        Assert.assertEquals(EventCode.ITEM_NS, eventType.itemType);
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
    
        exiEvent = exiEventList.get(5);
        Assert.assertEquals(EXIEvent.EVENT_NL, exiEvent.getEventVariety());
        Assert.assertEquals("nil", exiEvent.getName());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
        Assert.assertEquals("xsi", exiEvent.getPrefix());
        Assert.assertTrue(((EXIEventSchemaNil)exiEvent).isNilled());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
        Assert.assertEquals("nil", eventType.getName());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.getURI());
        Assert.assertEquals(1, eventType.getIndex());
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
        Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
        eventType = eventTypeList.item(6);
        Assert.assertEquals(EventCode.ITEM_NS, eventType.itemType);
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
    
        exiEvent = exiEventList.get(6);
        Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
        Assert.assertEquals(2, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(8, eventTypeList.getLength());
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
        Assert.assertEquals(EventCode.ITEM_NS, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
    
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

  /**
   * xsi:type attribute with a single corresponding namespace declaration.
   * 
   * Schema: 
   * <xsd:complexType name="restricted_B">
   *   <xsd:complexContent>
   *     <xsd:restriction base="foo:B">
   *       <xsd:sequence>
   *         <xsd:element ref="foo:AB"/>
   *         ...
   *       </xsd:sequence>
   *     </xsd:restriction>
   *   </xsd:complexContent>
   * </xsd:complexType>
   */
  public void testNamespaceDeclaration_07() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, 
        GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

    final String xmlString = 
      "<B xmlns='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " + 
      "   xmlns:xsh='http://www.w3.org/2001/XMLSchema-instance' " +
      "   xsh:type='restricted_B' xsi:nil='true'>" +
      "</B>\n";
    
    for (AlignmentType alignment : Alignments) {
      for (boolean preserveLexicalValues : new boolean[] { true, false }) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;
        
        encoder.setAlignmentType(alignment);
        decoder.setAlignmentType(alignment);
  
        encoder.setPreserveLexicalValues(preserveLexicalValues);
        decoder.setPreserveLexicalValues(preserveLexicalValues);
        
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
        
        Assert.assertEquals(9, n_events);
        
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
        Assert.assertEquals("B", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertNull(eventTypeList.getEE());
        
        exiEvent = exiEventList.get(2);
        Assert.assertEquals(EXIEvent.EVENT_NS, exiEvent.getEventVariety());
        Assert.assertEquals("", exiEvent.getPrefix());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        Assert.assertTrue(((EXIEventNS)exiEvent).getLocalElementNs());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_NS, eventType.itemType);
        Assert.assertNull(eventType.getName());
        Assert.assertEquals(XmlUriConst.W3C_XMLNS_2000_URI, eventType.getURI());
        // This NS event type should belong to ElementGrammar
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT, EventTypeAccessor.getGrammar(eventType).getGrammarType());
        Assert.assertEquals(6, eventType.getIndex());
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
        Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
    
        exiEvent = exiEventList.get(3);
        Assert.assertEquals(EXIEvent.EVENT_NS, exiEvent.getEventVariety());
        Assert.assertEquals("xsi", exiEvent.getPrefix());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
        Assert.assertFalse(((EXIEventNS)exiEvent).getLocalElementNs());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_NS, eventType.itemType);
        Assert.assertNull(eventType.getName());
        Assert.assertEquals(XmlUriConst.W3C_XMLNS_2000_URI, eventType.getURI());
        // This NS event type should belong to ElementGrammar
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT, EventTypeAccessor.getGrammar(eventType).getGrammarType());
        Assert.assertEquals(6, eventType.getIndex());
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
        Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
    
        exiEvent = exiEventList.get(4);
        Assert.assertEquals(EXIEvent.EVENT_NS, exiEvent.getEventVariety());
        Assert.assertEquals("xsh", exiEvent.getPrefix());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
        Assert.assertFalse(((EXIEventNS)exiEvent).getLocalElementNs());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_NS, eventType.itemType);
        Assert.assertNull(eventType.getName());
        Assert.assertEquals(XmlUriConst.W3C_XMLNS_2000_URI, eventType.getURI());
        // This NS event type should belong to ElementGrammar
        Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT, EventTypeAccessor.getGrammar(eventType).getGrammarType());
        Assert.assertEquals(6, eventType.getIndex());
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
        Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
    
        exiEvent = exiEventList.get(5);
        Assert.assertEquals(EXIEvent.EVENT_TP, exiEvent.getEventVariety());
        Assert.assertEquals("type", exiEvent.getName());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
        Assert.assertEquals("xsh", exiEvent.getPrefix());
        Assert.assertEquals("restricted_B", ((EXIEventSchemaType)exiEvent).getTypeName());
        Assert.assertEquals("urn:foo", ((EXIEventSchemaType)exiEvent).getTypeURI());
        Assert.assertEquals("", ((EXIEventSchemaType)exiEvent).getTypePrefix());
        Assert.assertTrue(preserveLexicalValues && "restricted_B".equals(exiEvent.getCharacters().makeString()) ||
            !preserveLexicalValues && exiEvent.getCharacters() == null);
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
        Assert.assertEquals("type", eventType.getName());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.getURI());
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
        Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
        eventType = eventTypeList.item(6);
        Assert.assertEquals(EventCode.ITEM_NS, eventType.itemType);
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
    
        exiEvent = exiEventList.get(6);
        Assert.assertEquals(EXIEvent.EVENT_NL, exiEvent.getEventVariety());
        Assert.assertEquals("nil", exiEvent.getName());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
        Assert.assertEquals("xsi", exiEvent.getPrefix());
        Assert.assertTrue(((EXIEventSchemaNil)exiEvent).isNilled());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
        Assert.assertEquals("nil", eventType.getName());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.getURI());
        Assert.assertEquals(1, eventType.getIndex());
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
        Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
        eventType = eventTypeList.item(6);
        Assert.assertEquals(EventCode.ITEM_NS, eventType.itemType);
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
    
        exiEvent = exiEventList.get(7);
        Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
        Assert.assertEquals(2, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(8, eventTypeList.getLength());
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
        Assert.assertEquals(EventCode.ITEM_NS, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
    
        exiEvent = exiEventList.get(8);
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
   * <xsd:complexType name="B">
   *   <xsd:sequence>
   *     <xsd:element ref="foo:AB"/>
   *     <xsd:element ref="foo:AC" minOccurs="0" maxOccurs="2"/>
   *     <xsd:element ref="foo:AD" minOccurs="0"/>
   *   </xsd:sequence>
   * </xsd:complexType>
   * 
   * <xsd:complexType name="extended_B">
   *   <xsd:complexContent>
   *     <xsd:extension base="foo:B">
   *       <xsd:attribute ref="foo:aA" use="required"/>
   *     </xsd:extension>
   *   </xsd:complexContent>
   * </xsd:complexType>
   */
  public void testSelfContained_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, 
        GrammarOptions.addSC(GrammarOptions.DEFAULT_OPTIONS));

    final String xmlString = 
      "<foo:B xmlns:foo='urn:foo' xsi:type='foo:extended_B' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'" +
      "       foo:aA='xyz'>" +
        "<foo:AB> </foo:AB>" +
      "</foo:B>\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      try {
        encoder.setEXISchema(grammarCache);
      }
      catch (EXIOptionsException eoe) {
        Assert.assertTrue(alignment == AlignmentType.compress || alignment == AlignmentType.preCompress);
        continue;
      }
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
      
      Assert.assertEquals(9, n_events);
      
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
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_TP, exiEvent.getEventVariety());
      Assert.assertEquals("type", exiEvent.getName());
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
      Assert.assertEquals("extended_B", ((EXIEventSchemaType)exiEvent).getTypeName());
      Assert.assertEquals("urn:foo", ((EXIEventSchemaType)exiEvent).getTypeURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      Assert.assertEquals("type", eventType.getName());
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.getURI());
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
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SC, eventType.itemType);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
      Assert.assertEquals("aA", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      Assert.assertEquals("xyz", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aA", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aA", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("aA", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
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
      Assert.assertEquals(6, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals(" ", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9, eventTypeList.getLength());
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
      Assert.assertEquals(EventCode.ITEM_SC, eventType.itemType);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(6);
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
  
      exiEvent = exiEventList.get(7);
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
  
      exiEvent = exiEventList.get(8);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
    }
  }

  /**
   * Schema:
   * <xsd:complexType name="B">
   *   <xsd:sequence>
   *     <xsd:element ref="foo:AB"/>
   *     <xsd:element ref="foo:AC" minOccurs="0" maxOccurs="2"/>
   *     <xsd:element ref="foo:AD" minOccurs="0"/>
   *   </xsd:sequence>
   * </xsd:complexType>
   * 
   * <xsd:complexType name="extended_B">
   *   <xsd:complexContent>
   *     <xsd:extension base="foo:B">
   *       <xsd:attribute ref="foo:aA" use="required"/>
   *     </xsd:extension>
   *   </xsd:complexContent>
   * </xsd:complexType>
   * 
   * Same as testSelfContained_01 except that NS is enabled in addition to SC.
   */
  public void testSelfContained_02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    short grammarOptions;
    grammarOptions = GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS); 
    grammarOptions = GrammarOptions.addSC(grammarOptions); 
    
    GrammarCache grammarCache = new GrammarCache(corpus, grammarOptions);

    final String xmlString = 
      "<foo:B xmlns:foo='urn:foo' xsi:type='foo:extended_B' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'" +
      "       foo:aA='xyz'>" +
        "<foo:AB> </foo:AB>" +
      "</foo:B>\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      try {
        encoder.setEXISchema(grammarCache);
      }
      catch (EXIOptionsException eoe) {
        Assert.assertTrue(alignment == AlignmentType.compress || alignment == AlignmentType.preCompress);
        continue;
      }
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
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_NS, exiEvent.getEventVariety());
      Assert.assertEquals("foo", exiEvent.getPrefix());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      Assert.assertTrue(((EXIEventNS)exiEvent).getLocalElementNs());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_NS, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(XmlUriConst.W3C_XMLNS_2000_URI, eventType.getURI());
      // This NS event type should belong to ElementGrammar
      Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT, EventTypeAccessor.getGrammar(eventType).getGrammarType());
      Assert.assertEquals(6, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10, eventTypeList.getLength());
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
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_NS, exiEvent.getEventVariety());
      Assert.assertEquals("xsi", exiEvent.getPrefix());
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
      Assert.assertFalse(((EXIEventNS)exiEvent).getLocalElementNs());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_NS, eventType.itemType);
      Assert.assertNull(eventType.getName());
      Assert.assertEquals(XmlUriConst.W3C_XMLNS_2000_URI, eventType.getURI());
      // This NS event type should belong to ElementGrammar
      Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT, EventTypeAccessor.getGrammar(eventType).getGrammarType());
      Assert.assertEquals(6, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10, eventTypeList.getLength());
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
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_TP, exiEvent.getEventVariety());
      Assert.assertEquals("type", exiEvent.getName());
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
      Assert.assertEquals("extended_B", ((EXIEventSchemaType)exiEvent).getTypeName());
      Assert.assertEquals("urn:foo", ((EXIEventSchemaType)exiEvent).getTypeURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      Assert.assertEquals("type", eventType.getName());
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.getURI());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10, eventTypeList.getLength());
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
      Assert.assertEquals(EventCode.ITEM_NS, eventType.itemType);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_AT, exiEvent.getEventVariety());
      Assert.assertEquals("aA", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      Assert.assertEquals("xyz", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aA", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(11, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.assertEquals("aA", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE, eventType.itemType);
      Assert.assertEquals("aA", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_NS, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_SC, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AB", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(6, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(7);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals(" ", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10, eventTypeList.getLength());
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
      Assert.assertEquals(EventCode.ITEM_NS, eventType.itemType);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(9);
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
  
      exiEvent = exiEventList.get(10);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
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
   * Use of xsi:nil in the presence of SC.
   */
  public void testSelfContained_03() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, 
        GrammarOptions.addSC(GrammarOptions.DEFAULT_OPTIONS));

    final String xmlString = 
      "<nillable_B xmlns='urn:foo' xsi:nil='true' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>" +
      "</nillable_B>\n";

    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      try {
        encoder.setEXISchema(grammarCache);
      }
      catch (EXIOptionsException eoe) {
        Assert.assertTrue(alignment == AlignmentType.compress || alignment == AlignmentType.preCompress);
        continue;
      }
      byte[] bts;
      ByteArrayOutputStream baos;
      
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
      exiEventList = new ArrayList<EXIEvent>();
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        exiEventList.add(exiEvent);
      }
      
      Assert.assertEquals(5, n_events);
  
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
      Assert.assertEquals(9, eventTypeList.getLength());
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
      Assert.assertEquals(EventCode.ITEM_SC, eventType.itemType);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(8, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SC, eventType.itemType);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
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
  public void testCommentPI_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    short options = GrammarOptions.DEFAULT_OPTIONS; 
    options = GrammarOptions.addCM(options);
    options = GrammarOptions.addPI(options);
    
    GrammarCache grammarCache = new GrammarCache(corpus, options);
    
    final String xmlString = 
      "<C xmlns='urn:foo'><AC/><!-- Good? --><?eg Good! ?></C><?eg Good? ?><!-- Good! -->";
  
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
  
  /**
   * Exercise CM and PI in "sequence" group.
   * 
   * Schema:
   * <xsd:complexType name="B">
   *   <xsd:sequence>
   *     <xsd:element ref="foo:AB"/>
   *     <xsd:element ref="foo:AC" minOccurs="0" maxOccurs="2"/>
   *     <xsd:element ref="foo:AD" minOccurs="0"/>
   *   </xsd:sequence>
   * </xsd:complexType>
   *
   * Instance:
   * <B><AB/><!-- Good? --><?eg Good! ?></B>
   */
  public void testCommentPI_02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    short options = GrammarOptions.DEFAULT_OPTIONS; 
    options = GrammarOptions.addCM(options);
    options = GrammarOptions.addPI(options);
    
    GrammarCache grammarCache = new GrammarCache(corpus, options);
    
    final String xmlString = 
      "<B xmlns='urn:foo'><AB/><!-- Good? --><?eg Good! ?></B>";
  
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
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10, eventTypeList.getLength());
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
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_CM, eventType.itemType);
      eventType = eventTypeList.item(9);
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
      // make sure this ITEM_CM belongs to "sequence" GroupGrammar
      Assert.assertEquals(EXISchema.GROUP_SEQUENCE, ((GroupGrammar)EventTypeAccessor.getGrammar(eventType)).getCompositor()); 
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(7, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AC", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
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
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_PI, eventType.itemType);
  
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_PI, exiEvent.getEventVariety());
      Assert.assertEquals("eg", exiEvent.getName());
      Assert.assertEquals("Good! ", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_PI, eventType.itemType);
      Assert.assertEquals(6, eventType.getIndex());
      // make sure this ITEM_CM belongs to "sequence" GroupGrammar
      Assert.assertEquals(EXISchema.GROUP_SEQUENCE, ((GroupGrammar)EventTypeAccessor.getGrammar(eventType)).getCompositor()); 
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(7, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("AC", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
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
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_CM, eventType.itemType);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_PI, eventType.itemType);
  
      exiEvent = exiEventList.get(7);
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
  
  /**
   * Exercise CM and PI in "sequence" group.
   * 
   * Schema:
   * <xsd:element name="D">
   *   <xsd:complexType>
   *     <xsd:choice>
   *       <xsd:element name="E" minOccurs="2" maxOccurs="2"/>
   *       <xsd:element name="F"/>
   *     </xsd:choice>
   *   </xsd:complexType>
   * </xsd:element>
   *
   * Instance:
   * <D><E/><!-- Good? --><?eg Good! ?></D>
   */
  public void testCommentPI_03() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/choiceGroup.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    short options = GrammarOptions.DEFAULT_OPTIONS; 
    options = GrammarOptions.addCM(options);
    options = GrammarOptions.addPI(options);
    
    GrammarCache grammarCache = new GrammarCache(corpus, options);
    
    final String xmlString = 
      "<foo:D xmlns:foo='urn:foo'><E/><!-- Good? --><?eg Good! ?></foo:D>";
  
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
      Assert.assertEquals("D", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
  
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("E", exiEvent.getName());
      Assert.assertEquals("", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("E", eventType.getName());
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
      Assert.assertEquals("F", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
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
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(4, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(12, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventCode.ITEM_CM, eventType.itemType);
      eventType = eventTypeList.item(11);
      Assert.assertEquals(EventCode.ITEM_PI, eventType.itemType);
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_CM, exiEvent.getEventVariety());
      Assert.assertEquals(" Good? ", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_CM, eventType.itemType);
      Assert.assertEquals(4, eventType.getIndex());
      // make sure this ITEM_CM belongs to "choice" GroupGrammar
      Assert.assertEquals(EXISchema.GROUP_CHOICE, ((GroupGrammar)EventTypeAccessor.getGrammar(eventType)).getCompositor()); 
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(6, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("E", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_PI, eventType.itemType);
      
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_PI, exiEvent.getEventVariety());
      Assert.assertEquals("eg", exiEvent.getName());
      Assert.assertEquals("Good! ", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_PI, eventType.itemType);
      Assert.assertEquals(5, eventType.getIndex());
      // make sure this ITEM_CM belongs to "choice" GroupGrammar
      Assert.assertEquals(EXISchema.GROUP_CHOICE, ((GroupGrammar)EventTypeAccessor.getGrammar(eventType)).getCompositor()); 
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(6, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("E", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_CM, eventType.itemType);
      
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(6, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("E", eventType.getName());
      Assert.assertEquals("", eventType.getURI());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_CM, eventType.itemType);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_PI, eventType.itemType);
  
      exiEvent = exiEventList.get(7);
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
  
  /**
   * Exercise CM and PI in ComplexContentGrammar.
   * 
   * Schema:
   * <xsd:element name="D">
   *   <xsd:complexType>
   *     <xsd:choice>
   *       <xsd:element name="E" minOccurs="2" maxOccurs="2"/>
   *       <xsd:element name="F"/>
   *     </xsd:choice>
   *   </xsd:complexType>
   * </xsd:element>
   *
   * Instance:
   * <D> <!-- Good? --><F/><?eg Good! ?></D>
   */
  public void testCommentPI_04() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/choiceGroup.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    short options = GrammarOptions.DEFAULT_OPTIONS; 
    options = GrammarOptions.addCM(options);
    options = GrammarOptions.addPI(options);
    
    GrammarCache grammarCache = new GrammarCache(corpus, options);
    
    final String xmlString = 
      "<foo:D xmlns:foo='urn:foo'> <!-- Good? --><F/><?eg Good! ?></foo:D>";
  
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
        
        Assert.assertEquals(preserveWhitespaces ? 9 : 8, n_events);
    
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
        Assert.assertEquals("D", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertNull(eventTypeList.getEE());
    
        if (preserveWhitespaces) {
          exiEvent = exiEventList.get(n++);
          Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
          Assert.assertEquals(" ", exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
          Assert.assertEquals(8, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(11, eventTypeList.getLength());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
          Assert.assertEquals("E", eventType.getName());
          Assert.assertEquals("", eventType.getURI());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
          Assert.assertEquals("F", eventType.getName());
          Assert.assertEquals("", eventType.getURI());
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
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
        }
        
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_CM, exiEvent.getEventVariety());
        Assert.assertEquals(" Good? ", exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_CM, eventType.itemType);
        if (preserveWhitespaces) {
          Assert.assertEquals(5, eventType.getIndex());
          // make sure this ITEM_CM belongs to ComplexContentGrammar
          Assert.assertEquals("complex", EventTypeAccessor.getContentGrammarRegime(eventType)); 
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(7, eventTypeList.getLength());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
          Assert.assertEquals("E", eventType.getName());
          Assert.assertEquals("", eventType.getURI());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
          Assert.assertEquals("F", eventType.getName());
          Assert.assertEquals("", eventType.getURI());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
          eventType = eventTypeList.item(6);
          Assert.assertEquals(EventCode.ITEM_PI, eventType.itemType);
        }
        
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
        Assert.assertEquals("F", exiEvent.getName());
        Assert.assertEquals("", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("F", eventType.getName());
        Assert.assertEquals("", eventType.getURI());
        Assert.assertEquals(1, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(7, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
        Assert.assertEquals("E", eventType.getName());
        Assert.assertEquals("", eventType.getURI());
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventCode.ITEM_CM, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.assertEquals(EventCode.ITEM_PI, eventType.itemType);
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
        Assert.assertEquals(4, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(12, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventCode.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
        eventType = eventTypeList.item(8);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
        eventType = eventTypeList.item(10);
        Assert.assertEquals(EventCode.ITEM_CM, eventType.itemType);
        eventType = eventTypeList.item(11);
        Assert.assertEquals(EventCode.ITEM_PI, eventType.itemType);
    
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_PI, exiEvent.getEventVariety());
        Assert.assertEquals("eg", exiEvent.getName());
        Assert.assertEquals("Good! ", exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_PI, eventType.itemType);
        Assert.assertEquals(4, eventType.getIndex());
        // make sure this ITEM_CM belongs to ComplexContentGrammar
        Assert.assertEquals("complex", EventTypeAccessor.getContentGrammarRegime(eventType)); 
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(5, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_CM, eventType.itemType);
        
        exiEvent = exiEventList.get(n++);
        Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(5, eventTypeList.getLength());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventCode.ITEM_CM, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventCode.ITEM_PI, eventType.itemType);
    
        exiEvent = exiEventList.get(n++);
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

  /**
   * Exercise CM in ElementGrammar.
   * 
   * Schema:
   * <xsd:complexType name="B">
   *   <xsd:sequence>
   *     <xsd:element ref="foo:AB"/>
   *     <xsd:element ref="foo:AC" minOccurs="0" maxOccurs="2"/>
   *     <xsd:element ref="foo:AD" minOccurs="0"/>
   *   </xsd:sequence>
   * </xsd:complexType>
   *
   * Instance:
   * <B><!-- Good? --><AB/></B>
   */
  public void testCommentPI_05() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    short options = GrammarOptions.DEFAULT_OPTIONS; 
    options = GrammarOptions.addCM(options);
    options = GrammarOptions.addPI(options);
    
    GrammarCache grammarCache = new GrammarCache(corpus, options);
    
    final String xmlString = 
      "<B xmlns='urn:foo'><!-- Good? --><AB/></B>";
  
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
      
      Assert.assertEquals(7, n_events);
  
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
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
  
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_CM, exiEvent.getEventVariety());
      Assert.assertEquals(" Good? ", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_CM, eventType.itemType);
      Assert.assertEquals(8, eventType.getIndex());
      // make sure this ITEM_CM belongs to ElementGrammar
      Assert.assertSame(Grammar.SCHEMA_GRAMMAR_ELEMENT, EventTypeAccessor.getGrammar(eventType).getGrammarType()); 
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10, eventTypeList.getLength());
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
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_PI, eventType.itemType);
  
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
      Assert.assertEquals(6, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_CM, eventType.itemType);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_PI, eventType.itemType);
      
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
  
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
  
      exiEvent = exiEventList.get(6);
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
  
  /**
   * Exercise PI in ElementGrammar.
   * 
   * Schema:
   * <xsd:complexType name="B">
   *   <xsd:sequence>
   *     <xsd:element ref="foo:AB"/>
   *     <xsd:element ref="foo:AC" minOccurs="0" maxOccurs="2"/>
   *     <xsd:element ref="foo:AD" minOccurs="0"/>
   *   </xsd:sequence>
   * </xsd:complexType>
   *
   * Instance:
   * <B><?eg Good! ?><AB/></B>
   */
  public void testCommentPI_06() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    short options = GrammarOptions.DEFAULT_OPTIONS; 
    options = GrammarOptions.addCM(options);
    options = GrammarOptions.addPI(options);
    
    GrammarCache grammarCache = new GrammarCache(corpus, options);
    
    final String xmlString = 
      "<B xmlns='urn:foo'><?eg Good! ?><AB/></B>";
  
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
      
      Assert.assertEquals(7, n_events);
  
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
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
  
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_PI, exiEvent.getEventVariety());
      Assert.assertEquals("eg", exiEvent.getName()); 
      Assert.assertEquals("Good! ", exiEvent.getCharacters().makeString()); 
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_PI, eventType.itemType);
      Assert.assertEquals(9, eventType.getIndex());
      // make sure this ITEM_PI belongs to ElementGrammar
      Assert.assertSame(Grammar.SCHEMA_GRAMMAR_ELEMENT, EventTypeAccessor.getGrammar(eventType).getGrammarType()); 
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10, eventTypeList.getLength());
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
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_CM, eventType.itemType);
  
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
      Assert.assertEquals(6, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_CM, eventType.itemType);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_PI, eventType.itemType);
      
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
  
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
  
      exiEvent = exiEventList.get(6);
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

  /**
   * Exercise CM in ElementTagGrammar.
   * 
   * Schema:
   * <xsd:complexType name="B">
   *   <xsd:sequence>
   *     <xsd:element ref="foo:AB"/>
   *     <xsd:element ref="foo:AC" minOccurs="0" maxOccurs="2"/>
   *     <xsd:element ref="foo:AD" minOccurs="0"/>
   *   </xsd:sequence>
   * </xsd:complexType>
   *
   * Instance:
   * <B xsi:type='B'><!-- Good? --><AB/></B>
   */
  public void testCommentPI_07() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    short options = GrammarOptions.DEFAULT_OPTIONS; 
    options = GrammarOptions.addCM(options);
    options = GrammarOptions.addPI(options);
    
    GrammarCache grammarCache = new GrammarCache(corpus, options);
    
    final String xmlString = 
      "<foo:B xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:type='foo:B'>" + 
      "<!-- Good? --><foo:AB/></foo:B>";
  
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
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
  
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_TP, exiEvent.getEventVariety());
      Assert.assertEquals("type", exiEvent.getName());
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
      Assert.assertEquals("B", ((EXIEventSchemaType)exiEvent).getTypeName());
      Assert.assertEquals("urn:foo", ((EXIEventSchemaType)exiEvent).getTypeURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10, eventTypeList.getLength());
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
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_CM, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_PI, eventType.itemType);
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_CM, exiEvent.getEventVariety());
      Assert.assertEquals(" Good? ", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_CM, eventType.itemType);
      Assert.assertEquals(8, eventType.getIndex());
      // make sure this ITEM_CM belongs to ElementTagGrammar
      Assert.assertSame(Grammar.SCHEMA_GRAMMAR_ELEMENT_TAG, EventTypeAccessor.getGrammar(eventType).getGrammarType()); 
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10, eventTypeList.getLength());
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
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_PI, eventType.itemType);
  
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
      Assert.assertEquals(6, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_CM, eventType.itemType);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_PI, eventType.itemType);
      
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
  
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
  
      exiEvent = exiEventList.get(7);
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
  
  /**
   * Exercise CM in ElementTagGrammar.
   * 
   * Schema:
   * <xsd:complexType name="B">
   *   <xsd:sequence>
   *     <xsd:element ref="foo:AB"/>
   *     <xsd:element ref="foo:AC" minOccurs="0" maxOccurs="2"/>
   *     <xsd:element ref="foo:AD" minOccurs="0"/>
   *   </xsd:sequence>
   * </xsd:complexType>
   *
   * Instance:
   * <B xsi:type='B'><?eg Good! ?><AB/></B>
   */
  public void testCommentPI_08() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    short options = GrammarOptions.DEFAULT_OPTIONS; 
    options = GrammarOptions.addCM(options);
    options = GrammarOptions.addPI(options);
    
    GrammarCache grammarCache = new GrammarCache(corpus, options);
    
    final String xmlString = 
      "<foo:B xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:type='foo:B'>" + 
      "<?eg Good! ?><foo:AB/></foo:B>";
  
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
      Assert.assertEquals("B", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
  
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_TP, exiEvent.getEventVariety());
      Assert.assertEquals("type", exiEvent.getName());
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
      Assert.assertEquals("B", ((EXIEventSchemaType)exiEvent).getTypeName());
      Assert.assertEquals("urn:foo", ((EXIEventSchemaType)exiEvent).getTypeURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10, eventTypeList.getLength());
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
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_CM, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_PI, eventType.itemType);
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_PI, exiEvent.getEventVariety());
      Assert.assertEquals("eg", exiEvent.getName()); 
      Assert.assertEquals("Good! ", exiEvent.getCharacters().makeString()); 
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_PI, eventType.itemType);
      Assert.assertEquals(9, eventType.getIndex());
      // make sure this ITEM_CM belongs to ElementTagGrammar
      Assert.assertSame(Grammar.SCHEMA_GRAMMAR_ELEMENT_TAG, EventTypeAccessor.getGrammar(eventType).getGrammarType()); 
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10, eventTypeList.getLength());
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
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_CM, eventType.itemType);
  
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
      Assert.assertEquals(6, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_CM, eventType.itemType);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_PI, eventType.itemType);
      
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
  
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
  
      exiEvent = exiEventList.get(7);
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

  /**
   * Exercise CM and PI in ElementTagGrammar.
   * 
   * Schema:
   * <xsd:element name="AB" type="xsd:anySimpleType"/>
   * 
   * Instance:
   * The element <AC/> is not expected.
   * <AB><AC/><!-- Good? -->abc<?eg Good! ?></AB>
   */
  public void testCommentPI_09() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    short options = GrammarOptions.DEFAULT_OPTIONS; 
    options = GrammarOptions.addCM(options);
    options = GrammarOptions.addPI(options);
    
    GrammarCache grammarCache = new GrammarCache(corpus, options);
    
    final String xmlString = "<AB xmlns='urn:foo'><AC/><!-- Good? -->abc<?eg Good! ?></AB>\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      byte[] bts;
      int n_events;
      
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
      Assert.assertEquals("AB", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AC", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      Assert.assertEquals(6, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
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
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_CM, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_PI, eventType.itemType);
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_CM, exiEvent.getEventVariety());
      Assert.assertEquals(" Good? ", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_CM, eventType.itemType);
      Assert.assertEquals(4, eventType.getIndex());
      // make sure this ITEM_CM belongs to SimpleContentGrammar
      SchemaInformedGrammar grammar = (SchemaInformedGrammar)EventTypeAccessor.getGrammar(eventType);
      Assert.assertSame(Grammar.SCHEMA_GRAMMAR_ELEMENT_CONTENT, grammar.getGrammarType()); 
      Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, GrammarAccessor.getEXISchema(grammar).getNodeType(GrammarAccessor.getNode(grammar)));
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(6, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_PI, eventType.itemType);
      
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("abc", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(6, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_CM, eventType.itemType);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventCode.ITEM_PI, eventType.itemType);
  
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_PI, exiEvent.getEventVariety());
      Assert.assertEquals("eg", exiEvent.getName()); 
      Assert.assertEquals("Good! ", exiEvent.getCharacters().makeString()); 
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_PI, eventType.itemType);
      Assert.assertEquals(4, eventType.getIndex());
      // make sure this ITEM_PI belongs to SimpleContentGrammar
      grammar = (SchemaInformedGrammar)EventTypeAccessor.getGrammar(eventType);
      Assert.assertSame(Grammar.SCHEMA_GRAMMAR_ELEMENT_CONTENT, grammar.getGrammarType()); 
      Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, GrammarAccessor.getEXISchema(grammar).getNodeType(GrammarAccessor.getNode(grammar)));
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_CM, eventType.itemType);
  
      exiEvent = exiEventList.get(7);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_CM, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_PI, eventType.itemType);
  
      exiEvent = exiEventList.get(8);
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

  /**
   * Exercise CM and PI in EmptyContentGrammar.
   * 
   * Schema:
   * <xsd:element name="AB" type="xsd:anySimpleType"/>
   * 
   * Instance:
   * The element <AC/> is not expected.
   * <AB xsi:nil='true'><AC/><!-- Good? --><?eg Good! ?></AB>
   */
  public void testCommentPI_10() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    short options = GrammarOptions.DEFAULT_OPTIONS; 
    options = GrammarOptions.addCM(options);
    options = GrammarOptions.addPI(options);
    
    GrammarCache grammarCache = new GrammarCache(corpus, options);
    
    final String xmlString = 
      "<AB xmlns='urn:foo' xsi:nil='true' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>" + 
        "<AC/><!-- Good? --><?eg Good! ?>" + 
      "</AB>\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      byte[] bts;
      int n_events;
      
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
      Assert.assertEquals("AB", eventType.getName());
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
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(10, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
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
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_CM, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventCode.ITEM_PI, eventType.itemType);
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("AC", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      Assert.assertEquals(5, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(9, eventTypeList.getLength());
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
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventCode.ITEM_CM, eventType.itemType);
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventCode.ITEM_PI, eventType.itemType);
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
  
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_CM, exiEvent.getEventVariety());
      Assert.assertEquals(" Good? ", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_CM, eventType.itemType);
      Assert.assertEquals(3, eventType.getIndex());
      // make sure this ITEM_CM belongs to EmptyContentGrammar
      Assert.assertSame(SchemaInformedGrammar.SCHEMA_GRAMMAR_NIL_CONTENT, EventTypeAccessor.getGrammar(eventType).getGrammarType()); 
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_PI, eventType.itemType);
      
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_PI, exiEvent.getEventVariety());
      Assert.assertEquals("eg", exiEvent.getName()); 
      Assert.assertEquals("Good! ", exiEvent.getCharacters().makeString()); 
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_PI, eventType.itemType);
      Assert.assertEquals(4, eventType.getIndex());
      // make sure this ITEM_PI belongs to EmptyContentGrammar
      Assert.assertSame(Grammar.SCHEMA_GRAMMAR_NIL_CONTENT, EventTypeAccessor.getGrammar(eventType).getGrammarType()); 
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_CM, eventType.itemType);
  
      exiEvent = exiEventList.get(7);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(5, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventCode.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventCode.ITEM_CM, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventCode.ITEM_PI, eventType.itemType);
  
      exiEvent = exiEventList.get(8);
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
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/nillable01.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

    final String xmlString = 
      "<A xmlns='urn:foo' xsi:nil='false' " + 
      "   xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>" +
        "<B/>" +
      "</A>";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setEXISchema(grammarCache);
      byte[] bts;
      ByteArrayOutputStream baos;
      
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
      
      Assert.assertEquals(7, n_events);
  
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
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(8, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
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
      
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      Assert.assertEquals("B", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.getName());
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
      Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
      Assert.assertEquals(3, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(8, eventTypeList.getLength());
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
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
    }
  }

}
