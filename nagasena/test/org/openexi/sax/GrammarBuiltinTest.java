package org.openexi.sax;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;

import junit.framework.Assert;

import org.xml.sax.InputSource;

import org.openexi.proc.EXIDecoder;
import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EventCode;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventTypeList;
import org.openexi.proc.common.EventDescription;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.common.XmlUriConst;
import org.openexi.proc.events.EXIEventDTD;
import org.openexi.proc.events.EXIEventNS;
import org.openexi.proc.events.EXIEventSchemaType;
import org.openexi.proc.grammars.Grammar;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.proc.io.Scanner;
import org.openexi.schema.EXISchema;
import org.openexi.schema.EmptySchema;
import org.openexi.schema.TestBase;

import org.openexi.scomp.EXISchemaFactoryErrorMonitor;
import org.openexi.scomp.EXISchemaFactoryTestUtil;

public class GrammarBuiltinTest extends TestBase {

  public GrammarBuiltinTest(String name) {
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
   * None available
   * 
   * Instance:
   * <None><!-- abc --><!-- def --></None>
   */
  public void testBuiltinComment_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, 
        GrammarOptions.addCM(GrammarOptions.DEFAULT_OPTIONS));
    
    final String xmlString;
    byte[] bts;
    int n_events;
    
    xmlString = "<None xmlns='urn:foo'><!-- abc --><!-- def --></None>\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setGrammarCache(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      ArrayList<EventDescription> exiEventList = new ArrayList<EventDescription>();
      
      EventDescription exiEvent;
      n_events = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        exiEventList.add(exiEvent);
      }
      
      Assert.assertEquals(6, n_events);
  
      EventType eventType;
      EventTypeList eventTypeList;
      int n_eventTypes;
  
      exiEvent = exiEventList.get(0);
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      
      exiEvent = exiEventList.get(1);
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("None", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        eventTypeList = eventType.getEventTypeList();
        n_eventTypes = eventTypeList.getLength();
        Assert.assertEquals(n_eventTypes - 2, eventType.getIndex());
        eventType = eventTypeList.item(n_eventTypes - 1);
        Assert.assertEquals(EventType.ITEM_CM, eventType.itemType);
      }
      
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EventDescription.EVENT_CM, exiEvent.getEventKind());
      Assert.assertEquals(" abc ", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_CM, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        Assert.assertEquals(4, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(5, eventTypeList.getLength());
        Assert.assertNotNull(eventTypeList.getEE());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      }
      
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EventDescription.EVENT_CM, exiEvent.getEventKind());
      Assert.assertEquals(" def ", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_CM, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        Assert.assertEquals(3, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(4, eventTypeList.getLength());
        Assert.assertNotNull(eventTypeList.getEE());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      }
      
      exiEvent = exiEventList.get(4);
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(4, eventTypeList.getLength());
        Assert.assertNotNull(eventTypeList.getEE());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventType.ITEM_CM, eventType.itemType);
      }
      
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(2, eventTypeList.getLength());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventType.ITEM_CM, eventType.itemType);
      }
    }
  }

  /**
   * Schema:
   * None available
   * 
   * Instance:
   * <None>  <!-- abc -->   </None>
   */
  public void testBuiltinComment_02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, 
        GrammarOptions.addCM(GrammarOptions.DEFAULT_OPTIONS));
    
    final String xmlString;
    byte[] bts;
    
    xmlString = "<None xmlns='urn:foo'>  <!-- abc -->   </None>\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setGrammarCache(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EventDescription exiEvent;
  
      EventType eventType;
      EventTypeList eventTypeList;
      int n_eventTypes;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("None", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        eventTypeList = eventType.getEventTypeList();
        n_eventTypes = eventTypeList.getLength();
        Assert.assertEquals(n_eventTypes - 2, eventType.getIndex());
        eventType = eventTypeList.item(n_eventTypes - 1);
        Assert.assertEquals(EventType.ITEM_CM, eventType.itemType);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("  ", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        Assert.assertEquals(4, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(6, eventTypeList.getLength());
        Assert.assertNotNull(eventTypeList.getEE());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventType.ITEM_CM, eventType.itemType);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CM, exiEvent.getEventKind());
      Assert.assertEquals(" abc ", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_CM, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        Assert.assertEquals(3, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(4, eventTypeList.getLength());
        Assert.assertNotNull(eventTypeList.getEE());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventType.ITEM_CM, eventType.itemType);
      }

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("   ", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        Assert.assertEquals(3, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(5, eventTypeList.getLength());
        Assert.assertNotNull(eventTypeList.getEE());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventType.ITEM_CM, eventType.itemType);
      }

      exiEvent = scanner.nextEvent();
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        Assert.assertEquals(1, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(5, eventTypeList.getLength());
        Assert.assertNotNull(eventTypeList.getEE());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventType.ITEM_CM, eventType.itemType);
      }
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(2, eventTypeList.getLength());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventType.ITEM_CM, eventType.itemType);
      }
    }
  }
  
  /**
   * Schema:
   * None available
   * 
   * Instance:
   * <!DOCTYPE books SYSTEM "dtdComments.dtd"><A><!-- XYZ --><B/></A>
   */
  public void testBuiltinCommentDTD() throws Exception {
    EXISchema corpus = EmptySchema.getEXISchema(); 

    GrammarCache grammarCache = new GrammarCache(corpus, 
        GrammarOptions.addCM(GrammarOptions.DEFAULT_OPTIONS));
    
    byte[] bts;
    int n_events;
    
    InputSource inputSource;
    URL url = resolveSystemIdAsURL("/dtdComments.xml");
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setGrammarCache(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      inputSource = new InputSource(url.toString());
      inputSource.setByteStream(url.openStream());

      encoder.encode(inputSource);
      
      bts = baos.toByteArray();
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      ArrayList<EventDescription> exiEventList = new ArrayList<EventDescription>();
      
      EventDescription exiEvent;
      n_events = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        exiEventList.add(exiEvent);
      }
      
      Assert.assertEquals(7, n_events);
  
      EventType eventType;
  
      exiEvent = exiEventList.get(0);
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      
      exiEvent = exiEventList.get(1);
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("A", exiEvent.getName());
      Assert.assertEquals("", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);

      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EventDescription.EVENT_CM, exiEvent.getEventKind());
      Assert.assertEquals(" XYZ ", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_CM, eventType.itemType);

      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("B", exiEvent.getName());
      Assert.assertEquals("", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);

      exiEvent = exiEventList.get(4);
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      
      exiEvent = exiEventList.get(5);
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
    }
  }
  
  /**
   * Schema:
   * None available
   * 
   * Instance:
   * <None><?abc uvw?><?def xyz?></None>
   */
  public void testBuiltinPI() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, 
        GrammarOptions.addPI(GrammarOptions.DEFAULT_OPTIONS));
    
    final String xmlString;
    byte[] bts;
    int n_events;
    
    xmlString = "<None xmlns='urn:foo'><?abc uvw?><?def xyz?></None>\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setGrammarCache(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      ArrayList<EventDescription> exiEventList = new ArrayList<EventDescription>();
      
      EventDescription exiEvent;
      n_events = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        exiEventList.add(exiEvent);
      }
      
      Assert.assertEquals(6, n_events);
  
      EventType eventType;
      EventTypeList eventTypeList;
      int n_eventTypes;
  
      exiEvent = exiEventList.get(0);
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      
      exiEvent = exiEventList.get(1);
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("None", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        eventTypeList = eventType.getEventTypeList();
        n_eventTypes = eventTypeList.getLength();
        Assert.assertEquals(n_eventTypes - 2, eventType.getIndex());
        eventType = eventTypeList.item(n_eventTypes - 1);
        Assert.assertEquals(EventType.ITEM_PI, eventType.itemType);
      }
      
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EventDescription.EVENT_PI, exiEvent.getEventKind());
      Assert.assertEquals("abc", exiEvent.getName());
      Assert.assertEquals("uvw", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_PI, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        Assert.assertEquals(4, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(5, eventTypeList.getLength());
        Assert.assertNotNull(eventTypeList.getEE());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      }
      
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EventDescription.EVENT_PI, exiEvent.getEventKind());
      Assert.assertEquals("def", exiEvent.getName());
      Assert.assertEquals("xyz", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_PI, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        Assert.assertEquals(3, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(4, eventTypeList.getLength());
        Assert.assertNotNull(eventTypeList.getEE());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      }
      
      exiEvent = exiEventList.get(4);
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(4, eventTypeList.getLength());
        Assert.assertNotNull(eventTypeList.getEE());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventType.ITEM_PI, eventType.itemType);
      }
      
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(2, eventTypeList.getLength());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventType.ITEM_PI, eventType.itemType);
      }
    }
  }

  /**
   * Schema:
   * None available
   * 
   * Instance:
   * <None>&abc;&def;</None>
   */
  public void testBuiltinEntityRef() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, 
        GrammarOptions.addDTD(GrammarOptions.DEFAULT_OPTIONS));
    
    final String xmlString;
    byte[] bts;
    int n_events;
    
    xmlString = "<!DOCTYPE None [ <!ENTITY ent SYSTEM 'er-entity.xml'> ]><None xmlns='urn:foo'>&ent;&ent;</None>\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      encoder.setResolveExternalGeneralEntities(false);
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setGrammarCache(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      ArrayList<EventDescription> exiEventList = new ArrayList<EventDescription>();
      
      EventDescription exiEvent;
      n_events = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        exiEventList.add(exiEvent);
      }
      
      Assert.assertEquals(7, n_events);
  
      EventType eventType;
      EventTypeList eventTypeList;
      int n_eventTypes;
  
      exiEvent = exiEventList.get(0);
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
      
      exiEvent = exiEventList.get(1);
      Assert.assertEquals(EventDescription.EVENT_DTD, exiEvent.getEventKind());
      Assert.assertEquals("None", exiEvent.getName());
      Assert.assertNull(((EXIEventDTD)exiEvent).getPublicId());
      Assert.assertNull(((EXIEventDTD)exiEvent).getSystemId());
      Assert.assertEquals("", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_DTD, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        eventTypeList = eventType.getEventTypeList();
        n_eventTypes = eventTypeList.getLength();
        Assert.assertEquals(n_eventTypes - 1, eventType.getIndex());
        eventType = eventTypeList.item(n_eventTypes - 2);
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      }
      
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("None", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        eventTypeList = eventType.getEventTypeList();
        n_eventTypes = eventTypeList.getLength();
        Assert.assertEquals(n_eventTypes - 2, eventType.getIndex());
        eventType = eventTypeList.item(n_eventTypes - 1);
        Assert.assertEquals(EventType.ITEM_DTD, eventType.itemType);
      }
      
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EventDescription.EVENT_ER, exiEvent.getEventKind());
      Assert.assertEquals("ent", exiEvent.getName());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_ER, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        Assert.assertEquals(4, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(5, eventTypeList.getLength());
        Assert.assertNotNull(eventTypeList.getEE());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      }
      
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EventDescription.EVENT_ER, exiEvent.getEventKind());
      Assert.assertEquals("ent", exiEvent.getName());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_ER, eventType.itemType);
      Assert.assertEquals(3, eventType.getIndex());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(4, eventTypeList.getLength());
        Assert.assertNotNull(eventTypeList.getEE());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      }
      
      exiEvent = exiEventList.get(5);
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(4, eventTypeList.getLength());
        Assert.assertNotNull(eventTypeList.getEE());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventType.ITEM_ER, eventType.itemType);
      }
      
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(1, eventTypeList.getLength());
      }
    }
  }

  /**
   * Schema:
   * None available
   * 
   * Instance:
   * <None>&ent;<!-- abc --><?def uvw?></None>
   */
  public void testBuiltinEntityRefCommentPI() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    
    short options = GrammarOptions.DEFAULT_OPTIONS;
    options = GrammarOptions.addDTD(options);
    options = GrammarOptions.addPI(options);
    options = GrammarOptions.addCM(options);
    
    GrammarCache grammarCache = new GrammarCache(corpus, options);
    
    final String xmlString;
    byte[] bts;
    int n_events;
    
    xmlString = "<!DOCTYPE None [ <!ENTITY ent SYSTEM 'er-entity.xml'> ]>" + 
      "<None xmlns='urn:foo'>&ent;<!-- abc --><?def uvw?></None>\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      encoder.setResolveExternalGeneralEntities(false);
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setGrammarCache(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      ArrayList<EventDescription> exiEventList = new ArrayList<EventDescription>();
      
      EventDescription exiEvent;
      n_events = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        exiEventList.add(exiEvent);
      }
      
      Assert.assertEquals(8, n_events);
  
      EventType eventType;
      EventTypeList eventTypeList;
      int n_eventTypes;
  
      exiEvent = exiEventList.get(0);
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertNull(eventTypeList.getEE());
      }
      
      exiEvent = exiEventList.get(1);
      Assert.assertEquals(EventDescription.EVENT_DTD, exiEvent.getEventKind());
      Assert.assertEquals("None", exiEvent.getName());
      Assert.assertNull(((EXIEventDTD)exiEvent).getPublicId());
      Assert.assertNull(((EXIEventDTD)exiEvent).getSystemId());
      Assert.assertEquals("", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_DTD, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        eventTypeList = eventType.getEventTypeList();
        n_eventTypes = eventTypeList.getLength();
        Assert.assertEquals(n_eventTypes - 3, eventType.getIndex());
        eventType = eventTypeList.item(n_eventTypes - 4);
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(n_eventTypes - 2);
        Assert.assertEquals(EventType.ITEM_CM, eventType.itemType);
        eventType = eventTypeList.item(n_eventTypes - 1);
        Assert.assertEquals(EventType.ITEM_PI, eventType.itemType);
      }
      
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("None", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        eventTypeList = eventType.getEventTypeList();
        n_eventTypes = eventTypeList.getLength();
        Assert.assertEquals(n_eventTypes - 4, eventType.getIndex());
        eventType = eventTypeList.item(n_eventTypes - 3);
        Assert.assertEquals(EventType.ITEM_DTD, eventType.itemType);
        eventType = eventTypeList.item(n_eventTypes - 2);
        Assert.assertEquals(EventType.ITEM_CM, eventType.itemType);
        eventType = eventTypeList.item(n_eventTypes - 1);
        Assert.assertEquals(EventType.ITEM_PI, eventType.itemType);
      }
      
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EventDescription.EVENT_ER, exiEvent.getEventKind());
      Assert.assertEquals("ent", exiEvent.getName());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_ER, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        Assert.assertEquals(4, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(7, eventTypeList.getLength());
        Assert.assertNotNull(eventTypeList.getEE());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventType.ITEM_CM, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.assertEquals(EventType.ITEM_PI, eventType.itemType);
      }
      
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EventDescription.EVENT_CM, exiEvent.getEventKind());
      Assert.assertEquals(" abc ", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_CM, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        Assert.assertEquals(4, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(6, eventTypeList.getLength());
        Assert.assertNotNull(eventTypeList.getEE());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventType.ITEM_ER, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventType.ITEM_PI, eventType.itemType);
      }
      
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EventDescription.EVENT_PI, exiEvent.getEventKind());
      Assert.assertEquals("def", exiEvent.getName());
      Assert.assertEquals("uvw", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_PI, eventType.itemType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        Assert.assertEquals(5, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(6, eventTypeList.getLength());
        Assert.assertNotNull(eventTypeList.getEE());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventType.ITEM_ER, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventType.ITEM_CM, eventType.itemType);
      }
      
      exiEvent = exiEventList.get(6);
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(6, eventTypeList.getLength());
        Assert.assertNotNull(eventTypeList.getEE());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventType.ITEM_ER, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventType.ITEM_CM, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventType.ITEM_PI, eventType.itemType);
      }
      
      exiEvent = exiEventList.get(7);
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(3, eventTypeList.getLength());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventType.ITEM_CM, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventType.ITEM_PI, eventType.itemType);
      }
    }
  }

  /**
   * Test SE(*), SE(qname), EE and CH in both states (i.e. STATE_IN_TAG and STATE_IN_CONTENT)
   * of BuiltinElementGrammar with prefix preservation on.
   */
  public void testBuiltinSE_EE_CH_Prefixed() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    short options = GrammarOptions.DEFAULT_OPTIONS; 
    options = GrammarOptions.addNS(options);
    
    GrammarCache grammarCache = new GrammarCache(corpus, options);
    
    final String xmlString;
    byte[] bts;
    
    xmlString = 
      "<foo:None xmlns:foo='urn:foo' xmlns:goo='urn:goo' xmlns:hoo='urn:hoo' xmlns:ioo='urn:ioo'>" + 
        "<goo:None/>" +              // SE(*) in STATE_IN_TAG  
        "<foo:None>" +               // SE(*) in STATE_IN_CONTENT   
          "<goo:None/>" +            // SE(goo:None) in STATE_IN_TAG
          "<goo:None/>" +            // SE(*) in STATE_IN_CONTENT
          "<foo:None>" +             // SE(foo:None) in STATE_IN_CONTENT
            "<goo:None/>" +          // SE(goo:None) in STATE_IN_TAG
            "<goo:None/>" +          // SE(goo:None) encounter in STATE_IN_CONTENT
          "</foo:None>" + 
        "</foo:None>" + 
        "<goo:None/>" +              // SE(goo:None) encounter in STATE_IN_CONTENT
        "<foo:None>" +               // SE(foo:None) in STATE_IN_CONTENT 
          "<foo:AB>abc</foo:AB>" +   // SE(*) in STATE_IN_TAG
          "<foo:AC>def</foo:AC>" +   // SE(*) in STATE_IN_CONTENT
          "<goo:None>" +             // SE(goo:None) encounter in STATE_IN_CONTENT
            "<hoo:None>123</hoo:None>" +            // SE(*) in STATE_IN_TAG
            "<ioo:None><goo:None/>456</ioo:None>" + // SE(*) in STATE_IN_CONTENT
          "</goo:None>" +
          "<foo:None>" +             // SE(foo:None) in STATE_IN_CONTENT
            "<foo:AB>ghi</foo:AB>" + // SE(foo:AB) in STATE_IN_TAG
            "<foo:AC>jkl</foo:AC>" + // SE(foo:AC) in STATE_IN_CONTENT
            "<goo:None>" +           // SE(goo:None) encounter in STATE_IN_CONTENT
              "<hoo:None>789</hoo:None>" +            // SE(hoo:None) in STATE_IN_TAG
              "<ioo:None><goo:None/>012</ioo:None>" + // SE(ioo:None) in STATE_IN_CONTENT
            "</goo:None>" +
          "</foo:None>" + 
        "</foo:None>" + 
      "</foo:None>\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setGrammarCache(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EventDescription exiEvent;
      
      EventType eventType;
      EventTypeList eventTypeList;
      
      int n_events = 0;
      int i;
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertNull(eventTypeList.getEE());
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        Assert.assertEquals("None", exiEvent.getName());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = exiEvent.getEventType();
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(eventTypeList.getLength() - 1, eventType.getIndex());
        Assert.assertNull(eventTypeList.getEE());
        
        eventType = eventTypeList.item(0);
        for (i = 1; i < eventTypeList.getLength() - 1; i++) {
          EventType ith = eventTypeList.item(i);
          if (!(eventType.name.compareTo(ith.name) < 0)) {
            Assert.assertEquals(eventType.name, ith.name);
            Assert.assertTrue(eventType.uri.compareTo(ith.uri) < 0);
          }
          eventType = ith;
        }
      }
  
      for (i = 0; i < 4; i++) {
        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
          switch (i) {
            case 0:
              Assert.assertEquals("foo", exiEvent.getPrefix());
              Assert.assertEquals("urn:foo", exiEvent.getURI());
              Assert.assertTrue(((EXIEventNS)exiEvent).getLocalElementNs());
              break;
            case 1:
              Assert.assertEquals("goo", exiEvent.getPrefix());
              Assert.assertEquals("urn:goo", exiEvent.getURI());
              Assert.assertFalse(((EXIEventNS)exiEvent).getLocalElementNs());
              break;
            case 2:
              Assert.assertEquals("hoo", exiEvent.getPrefix());
              Assert.assertEquals("urn:hoo", exiEvent.getURI());
              Assert.assertFalse(((EXIEventNS)exiEvent).getLocalElementNs());
              break;
            case 3:
              Assert.assertEquals("ioo", exiEvent.getPrefix());
              Assert.assertEquals("urn:ioo", exiEvent.getURI());
              Assert.assertFalse(((EXIEventNS)exiEvent).getLocalElementNs());
              break;
            default:
              break;
          }
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
          Assert.assertNull(eventType.name);
          Assert.assertNull(eventType.uri);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.assertEquals(2, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertEquals(5, eventTypeList.getLength());
            eventType = eventTypeList.item(0);
            Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
            Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
            eventType = eventTypeList.item(1);
            Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
            eventType = eventTypeList.item(3);
            Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(4);
            Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
          }
        }
      }
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        Assert.assertEquals("urn:goo", exiEvent.getURI());
        Assert.assertEquals("None", exiEvent.getName());
        Assert.assertEquals("goo", exiEvent.getPrefix());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(4, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(6, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:goo", eventType.uri);
          Assert.assertEquals("None", eventType.name);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
          eventType = eventTypeList.item(5);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(1, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(6, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(5);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        Assert.assertEquals("None", exiEvent.getName());
        Assert.assertEquals("foo", exiEvent.getPrefix());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(2, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(4, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:foo", eventType.uri);
          Assert.assertEquals("None", eventType.name);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        Assert.assertEquals("urn:goo", exiEvent.getURI());
        Assert.assertEquals("None", exiEvent.getName());
        Assert.assertEquals("goo", exiEvent.getPrefix());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
        Assert.assertEquals("urn:goo", eventType.uri);
        Assert.assertEquals("None", eventType.name);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(6, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(5);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(6, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(5);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        Assert.assertEquals("urn:goo", exiEvent.getURI());
        Assert.assertEquals("None", exiEvent.getName());
        Assert.assertEquals("goo", exiEvent.getPrefix());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(3, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(5, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:goo", eventType.uri);
          Assert.assertEquals("None", eventType.name);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:foo", eventType.uri);
          Assert.assertEquals("None", eventType.name);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(6, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(5);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        Assert.assertEquals("None", exiEvent.getName());
        Assert.assertEquals("foo", exiEvent.getPrefix());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
        Assert.assertEquals("urn:foo", eventType.uri);
        Assert.assertEquals("None", eventType.name);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(1, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(5, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:goo", eventType.uri);
          Assert.assertEquals("None", eventType.name);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        Assert.assertEquals("urn:goo", exiEvent.getURI());
        Assert.assertEquals("None", exiEvent.getName());
        Assert.assertEquals("goo", exiEvent.getPrefix());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
        Assert.assertEquals("urn:goo", eventType.uri);
        Assert.assertEquals("None", eventType.name);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(6, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(5);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(6, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(5);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        Assert.assertEquals("urn:goo", exiEvent.getURI());
        Assert.assertEquals("None", exiEvent.getName());
        Assert.assertEquals("goo", exiEvent.getPrefix());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
        Assert.assertEquals("urn:goo", eventType.uri);
        Assert.assertEquals("None", eventType.name);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(5, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:foo", eventType.uri);
          Assert.assertEquals("None", eventType.name);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(6, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(5);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(2, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(5, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:goo", eventType.uri);
          Assert.assertEquals("None", eventType.name);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:foo", eventType.uri);
          Assert.assertEquals("None", eventType.name);
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(2, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(5, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:goo", eventType.uri);
          Assert.assertEquals("None", eventType.name);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:foo", eventType.uri);
          Assert.assertEquals("None", eventType.name);
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        Assert.assertEquals("urn:goo", exiEvent.getURI());
        Assert.assertEquals("None", exiEvent.getName());
        Assert.assertEquals("goo", exiEvent.getPrefix());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
        Assert.assertEquals("urn:goo", eventType.uri);
        Assert.assertEquals("None", eventType.name);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(5, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:foo", eventType.uri);
          Assert.assertEquals("None", eventType.name);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(6, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(5);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        Assert.assertEquals("None", exiEvent.getName());
        Assert.assertEquals("foo", exiEvent.getPrefix());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
        Assert.assertEquals("urn:foo", eventType.uri);
        Assert.assertEquals("None", eventType.name);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(1, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(5, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:goo", eventType.uri);
          Assert.assertEquals("None", eventType.name);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        Assert.assertEquals("AB", exiEvent.getName());
        Assert.assertEquals("foo", exiEvent.getPrefix());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(5, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(7, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:foo", eventType.uri);
          Assert.assertEquals("AB", eventType.name);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:goo", eventType.uri);
          Assert.assertEquals("None", eventType.name);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
          eventType = eventTypeList.item(6);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
        Assert.assertEquals("abc", exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(2, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(9, eventTypeList.getLength());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
          eventType = eventTypeList.item(5);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
          eventType = eventTypeList.item(6);
          Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
          eventType = eventTypeList.item(7);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(8);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(3, eventTypeList.getLength());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        Assert.assertEquals("AC", exiEvent.getName());
        Assert.assertEquals("foo", exiEvent.getPrefix());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(4, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(6, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:foo", eventType.uri);
          Assert.assertEquals("AC", eventType.name);
          eventType = eventTypeList.item(1);
          Assert.assertEquals("urn:goo", eventType.uri);
          Assert.assertEquals("None", eventType.name);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:foo", eventType.uri);
          Assert.assertEquals("None", eventType.name);
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
          eventType = eventTypeList.item(5);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
        Assert.assertEquals("def", exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(2, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(9, eventTypeList.getLength());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
          eventType = eventTypeList.item(5);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
          eventType = eventTypeList.item(6);
          Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
          eventType = eventTypeList.item(7);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(8);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(3, eventTypeList.getLength());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        Assert.assertEquals("urn:goo", exiEvent.getURI());
        Assert.assertEquals("None", exiEvent.getName());
        Assert.assertEquals("goo", exiEvent.getPrefix());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
        Assert.assertEquals("urn:goo", eventType.uri);
        Assert.assertEquals("None", eventType.name);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(1, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(6, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:foo", eventType.uri);
          Assert.assertEquals("AC", eventType.name);
          eventType = eventTypeList.item(2);
          Assert.assertEquals("urn:foo", eventType.uri);
          Assert.assertEquals("None", eventType.name);
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(5);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        Assert.assertEquals("urn:hoo", exiEvent.getURI());
        Assert.assertEquals("None", exiEvent.getName());
        Assert.assertEquals("hoo", exiEvent.getPrefix());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(5, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(7, eventTypeList.getLength());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:hoo", eventType.uri);
          Assert.assertEquals("None", eventType.name);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
          eventType = eventTypeList.item(6);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
        Assert.assertEquals("123", exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(5, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(6, eventTypeList.getLength());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        }
      }
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(3, eventTypeList.getLength());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        }
      }
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        Assert.assertEquals("urn:ioo", exiEvent.getURI());
        Assert.assertEquals("None", exiEvent.getName());
        Assert.assertEquals("ioo", exiEvent.getPrefix());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(2, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(4, eventTypeList.getLength());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:ioo", eventType.uri);
          Assert.assertEquals("None", eventType.name);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        Assert.assertEquals("urn:goo", exiEvent.getURI());
        Assert.assertEquals("None", exiEvent.getName());
        Assert.assertEquals("goo", exiEvent.getPrefix());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(4, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(6, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:goo", eventType.uri);
          Assert.assertEquals("None", eventType.name);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
          eventType = eventTypeList.item(5);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(1, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(7, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:hoo", eventType.uri);
          Assert.assertEquals("None", eventType.name);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
          eventType = eventTypeList.item(5);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(6);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
        Assert.assertEquals("456", exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(3, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(4, eventTypeList.getLength());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        }
      }
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(1, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(4, eventTypeList.getLength());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        }
      }
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(1, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(4, eventTypeList.getLength());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:ioo", eventType.uri);
          Assert.assertEquals("None", eventType.name);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        Assert.assertEquals("None", exiEvent.getName());
        Assert.assertEquals("foo", exiEvent.getPrefix());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
        Assert.assertEquals("urn:foo", eventType.uri);
        Assert.assertEquals("None", eventType.name);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(2, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(6, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:foo", eventType.uri);
          Assert.assertEquals("AC", eventType.name);
          eventType = eventTypeList.item(1);
          Assert.assertEquals("urn:goo", eventType.uri);
          Assert.assertEquals("None", eventType.name);
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(5);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        Assert.assertEquals("AB", exiEvent.getName());
        Assert.assertEquals("foo", exiEvent.getPrefix());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
        Assert.assertEquals("urn:foo", eventType.uri);
        Assert.assertEquals("AB", eventType.name);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(7, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:goo", eventType.uri);
          Assert.assertEquals("None", eventType.name);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
          eventType = eventTypeList.item(5);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(6);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
        Assert.assertEquals("ghi", exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(2, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(9, eventTypeList.getLength());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
          eventType = eventTypeList.item(5);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
          eventType = eventTypeList.item(6);
          Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
          eventType = eventTypeList.item(7);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(8);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(3, eventTypeList.getLength());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        Assert.assertEquals("AC", exiEvent.getName());
        Assert.assertEquals("foo", exiEvent.getPrefix());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
        Assert.assertEquals("urn:foo", eventType.uri);
        Assert.assertEquals("AC", eventType.name);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(6, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(1);
          Assert.assertEquals("urn:goo", eventType.uri);
          Assert.assertEquals("None", eventType.name);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:foo", eventType.uri);
          Assert.assertEquals("None", eventType.name);
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(5);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
        Assert.assertEquals("jkl", exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(2, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(9, eventTypeList.getLength());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
          eventType = eventTypeList.item(5);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
          eventType = eventTypeList.item(6);
          Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
          eventType = eventTypeList.item(7);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(8);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(3, eventTypeList.getLength());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        Assert.assertEquals("urn:goo", exiEvent.getURI());
        Assert.assertEquals("None", exiEvent.getName());
        Assert.assertEquals("goo", exiEvent.getPrefix());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
        Assert.assertEquals("urn:goo", eventType.uri);
        Assert.assertEquals("None", eventType.name);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(1, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(6, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:foo", eventType.uri);
          Assert.assertEquals("AC", eventType.name);
          eventType = eventTypeList.item(2);
          Assert.assertEquals("urn:foo", eventType.uri);
          Assert.assertEquals("None", eventType.name);
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(5);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        Assert.assertEquals("urn:hoo", exiEvent.getURI());
        Assert.assertEquals("None", exiEvent.getName());
        Assert.assertEquals("hoo", exiEvent.getPrefix());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
        Assert.assertEquals("urn:hoo", eventType.uri);
        Assert.assertEquals("None", eventType.name);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(7, eventTypeList.getLength());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
          eventType = eventTypeList.item(5);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(6);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
        Assert.assertEquals("789", exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(6, eventTypeList.getLength());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(5);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        }
      }
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(3, eventTypeList.getLength());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        }
      }
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        Assert.assertEquals("urn:ioo", exiEvent.getURI());
        Assert.assertEquals("None", exiEvent.getName());
        Assert.assertEquals("ioo", exiEvent.getPrefix());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
        Assert.assertEquals("urn:ioo", eventType.uri);
        Assert.assertEquals("None", eventType.name);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(4, eventTypeList.getLength());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        Assert.assertEquals("urn:goo", exiEvent.getURI());
        Assert.assertEquals("None", exiEvent.getName());
        Assert.assertEquals("goo", exiEvent.getPrefix());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
        Assert.assertEquals("urn:goo", eventType.uri);
        Assert.assertEquals("None", eventType.name);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(6, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(5);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(1, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(7, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:hoo", eventType.uri);
          Assert.assertEquals("None", eventType.name);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
          eventType = eventTypeList.item(5);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(6);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
        Assert.assertEquals("012", exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(4, eventTypeList.getLength());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        }
      }
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(1, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(4, eventTypeList.getLength());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        }
      }
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(1, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(4, eventTypeList.getLength());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:ioo", eventType.uri);
          Assert.assertEquals("None", eventType.name);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(3, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(6, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:foo", eventType.uri);
          Assert.assertEquals("AC", eventType.name);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:goo", eventType.uri);
          Assert.assertEquals("None", eventType.name);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:foo", eventType.uri);
          Assert.assertEquals("None", eventType.name);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(5);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(3, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(6, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:foo", eventType.uri);
          Assert.assertEquals("AC", eventType.name);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:goo", eventType.uri);
          Assert.assertEquals("None", eventType.name);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:foo", eventType.uri);
          Assert.assertEquals("None", eventType.name);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(5);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(3, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(6, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:foo", eventType.uri);
          Assert.assertEquals("AC", eventType.name);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:goo", eventType.uri);
          Assert.assertEquals("None", eventType.name);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:foo", eventType.uri);
          Assert.assertEquals("None", eventType.name);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(5);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(1, eventTypeList.getLength());
        }
      }
      
      Assert.assertEquals(60, n_events);
    }
  }

  /**
   * Test AT(*), AT(qname) of BuiltinElementGrammar with prefix preservation on.
   */
  public void testBuiltinAT_Prefixed() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/attributes.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    short options = GrammarOptions.DEFAULT_OPTIONS; 
    options = GrammarOptions.addNS(options);
    
    GrammarCache grammarCache = new GrammarCache(corpus, options);
    
    final String xmlString;
    byte[] bts;
    
    xmlString = 
      "<foo:None xmlns:foo='urn:foo' xmlns:goo='urn:goo' " +
      "          goo:none='abc' foo:aA='true' >" +
        "<foo:None foo:aA='false' goo:none='def' />" + // SE(*) in STATE_IN_TAG
      "</foo:None>\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setGrammarCache(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EventDescription exiEvent;
      
      EventType eventType;
      EventTypeList eventTypeList;
      
      int n_events = 0;
      int i;
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertNull(eventTypeList.getEE());
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        Assert.assertEquals("None", exiEvent.getName());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = exiEvent.getEventType();
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(eventTypeList.getLength() - 1, eventType.getIndex());
        Assert.assertNull(eventTypeList.getEE());
        
        eventType = eventTypeList.item(0);
        for (i = 1; i < eventTypeList.getLength() - 1; i++) {
          EventType ith = eventTypeList.item(i);
          if (!(eventType.name.compareTo(ith.name) < 0)) {
            Assert.assertEquals(eventType.name, ith.name);
            Assert.assertTrue(eventType.uri.compareTo(ith.uri) < 0);
          }
          eventType = ith;
        }
      }
  
      for (i = 0; i < 2; i++) {
        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
          switch (i) {
            case 0:
              Assert.assertEquals("foo", exiEvent.getPrefix());
              Assert.assertEquals("urn:foo", exiEvent.getURI());
              Assert.assertTrue(((EXIEventNS)exiEvent).getLocalElementNs());
              break;
            case 1:
              Assert.assertEquals("goo", exiEvent.getPrefix());
              Assert.assertEquals("urn:goo", exiEvent.getURI());
              Assert.assertFalse(((EXIEventNS)exiEvent).getLocalElementNs());
              break;
            default:
              break;
          }
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
          Assert.assertNull(eventType.name);
          Assert.assertNull(eventType.uri);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.assertEquals(2, eventType.getIndex());
            eventTypeList = eventType.getEventTypeList();
            Assert.assertEquals(5, eventTypeList.getLength());
            eventType = eventTypeList.item(0);
            Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
            Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
            eventType = eventTypeList.item(1);
            Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
            eventType = eventTypeList.item(3);
            Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(4);
            Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
          }
        }
      }
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        Assert.assertEquals("aA", exiEvent.getName());
        Assert.assertEquals("foo", exiEvent.getPrefix());
        Assert.assertEquals("true", exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(2, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(6, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_AT, eventType.itemType);
          Assert.assertEquals("urn:foo", eventType.uri);
          Assert.assertEquals("aA", eventType.name);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(5);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }

      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
        Assert.assertEquals("urn:goo", exiEvent.getURI());
        Assert.assertEquals("none", exiEvent.getName());
        Assert.assertEquals("goo", exiEvent.getPrefix());
        Assert.assertEquals("abc", exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(3, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(7, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_AT, eventType.itemType);
          Assert.assertEquals("urn:goo", eventType.uri);
          Assert.assertEquals("none", eventType.name);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_AT, eventType.itemType);
          Assert.assertEquals("urn:foo", eventType.uri);
          Assert.assertEquals("aA", eventType.name);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
          eventType = eventTypeList.item(5);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(6);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        Assert.assertEquals("None", exiEvent.getName());
        Assert.assertEquals("foo", exiEvent.getPrefix());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(6, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(8, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:foo", eventType.uri);
          Assert.assertEquals("None", eventType.name);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_AT, eventType.itemType);
          Assert.assertEquals("urn:goo", eventType.uri);
          Assert.assertEquals("none", eventType.name);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_AT, eventType.itemType);
          Assert.assertEquals("urn:foo", eventType.uri);
          Assert.assertEquals("aA", eventType.name);
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(5);
          Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
          eventType = eventTypeList.item(7);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        Assert.assertEquals("aA", exiEvent.getName());
        Assert.assertEquals("foo", exiEvent.getPrefix());
        Assert.assertEquals("false", exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_AT, eventType.itemType);
        Assert.assertEquals("urn:foo", eventType.uri);
        Assert.assertEquals("aA", eventType.name);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(2, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(8, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:foo", eventType.uri);
          Assert.assertEquals("None", eventType.name);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_AT, eventType.itemType);
          Assert.assertEquals("urn:goo", eventType.uri);
          Assert.assertEquals("none", eventType.name);
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(5);
          Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
          eventType = eventTypeList.item(6);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(7);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
        Assert.assertEquals("urn:goo", exiEvent.getURI());
        Assert.assertEquals("none", exiEvent.getName());
        Assert.assertEquals("goo", exiEvent.getPrefix());
        Assert.assertEquals("def", exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_AT, eventType.itemType);
        Assert.assertEquals("urn:goo", eventType.uri);
        Assert.assertEquals("none", eventType.name);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(1, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(8, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:foo", eventType.uri);
          Assert.assertEquals("None", eventType.name);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_AT, eventType.itemType);
          Assert.assertEquals("urn:foo", eventType.uri);
          Assert.assertEquals("aA", eventType.name);
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(5);
          Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
          eventType = eventTypeList.item(6);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(7);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(4, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(9, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:foo", eventType.uri);
          Assert.assertEquals("None", eventType.name);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_AT, eventType.itemType);
          Assert.assertEquals("urn:goo", eventType.uri);
          Assert.assertEquals("none", eventType.name);
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_AT, eventType.itemType);
          Assert.assertEquals("urn:foo", eventType.uri);
          Assert.assertEquals("aA", eventType.name);
          eventType = eventTypeList.item(5);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(6);
          Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
          eventType = eventTypeList.item(7);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(8);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(3, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(1, eventTypeList.getLength());
        }
      }
      
      Assert.assertEquals(12, n_events);
    }
  }
  
  /**
   * Initially no schema definition associated, switching to "foo:finalString" via xsi:type.
   */
  public void testBuiltinXsiType_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String xmlString;
    byte[] bts;
    
    xmlString =
      "<foo:None xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' >" +
        "<foo:None2 xsi:type='foo:finalString'>abc</foo:None2>" +
        "<foo:None2 xsi:type='foo:finalString'>abc</foo:None2>" + 
      "</foo:None>";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setGrammarCache(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EventDescription exiEvent;
      
      EventType eventType;
      EventTypeList eventTypeList;
      
      int n_events = 0;
      int i;
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertNull(eventTypeList.getEE());
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        Assert.assertEquals("None", exiEvent.getName());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = exiEvent.getEventType();
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(eventTypeList.getLength() - 1, eventType.getIndex());
        Assert.assertNull(eventTypeList.getEE());
        
        eventType = eventTypeList.item(0);
        for (i = 1; i < eventTypeList.getLength() - 1; i++) {
          EventType ith = eventTypeList.item(i);
          if (!(eventType.name.compareTo(ith.name) < 0)) {
            Assert.assertEquals(eventType.name, ith.name);
            Assert.assertTrue(eventType.uri.compareTo(ith.uri) < 0);
          }
          eventType = ith;
        }
      }
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        Assert.assertEquals("None2", exiEvent.getName());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(3, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(5, eventTypeList.getLength());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:foo", eventType.uri);
          Assert.assertEquals("None2", eventType.name);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_TP, exiEvent.getEventKind());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
        Assert.assertEquals("type", exiEvent.getName());
        Assert.assertNull(exiEvent.getPrefix());
        Assert.assertEquals(null, exiEvent.getCharacters());
        Assert.assertEquals("finalString", ((EXIEventSchemaType)exiEvent).getTypeName());
        Assert.assertEquals("urn:foo", ((EXIEventSchemaType)exiEvent).getTypeURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(2, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(5, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_AT, eventType.itemType);
          Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
          Assert.assertEquals("type", eventType.name);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
        Assert.assertEquals("abc", exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(1, eventTypeList.getLength());
        }
      }

      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(1, eventTypeList.getLength());
        }
      }
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        Assert.assertEquals("None2", exiEvent.getName());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(2, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(4, eventTypeList.getLength());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:foo", eventType.uri);
          Assert.assertEquals("None2", eventType.name);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_TP, exiEvent.getEventKind());
        Assert.assertEquals("type", exiEvent.getName());
        Assert.assertNull(exiEvent.getPrefix());
        Assert.assertEquals(null, exiEvent.getCharacters());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
        Assert.assertEquals("finalString", ((EXIEventSchemaType)exiEvent).getTypeName());
        Assert.assertEquals("urn:foo", ((EXIEventSchemaType)exiEvent).getTypeURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_AT, eventType.itemType);
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
        Assert.assertEquals("type", eventType.name);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(5, eventTypeList.getLength());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
        Assert.assertEquals("abc", exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(1, eventTypeList.getLength());
        }
      }

      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(1, eventTypeList.getLength());
        }
      }
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(1, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(4, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:foo", eventType.uri);
          Assert.assertEquals("None2", eventType.name);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(1, eventTypeList.getLength());
        }
      }
      
      Assert.assertEquals(12, n_events);
    }
  }

  /**
   * From EXI interoperability test suite.
   * Schema-Informed Declared Production tests - document
   * schemaInformed.declared.document-01
   */
  public void testBuiltinXsiType_02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/interop/schemaInformedGrammar/declaredProductions/document.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String xmlString;
    byte[] bts;
    
    xmlString =
    "<None xmlns:xsd='http://www.w3.org/2001/XMLSchema' "+ 
      "xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " +
      "xmlns='urn:foo' xsi:type='xsd:anyType' />";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setGrammarCache(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EventDescription exiEvent;
      
      EventType eventType;
      EventTypeList eventTypeList;
      
      int n_events = 0;
      int i;
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertNull(eventTypeList.getEE());
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        Assert.assertEquals("None", exiEvent.getName());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = exiEvent.getEventType();
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(eventTypeList.getLength() - 1, eventType.getIndex());
        Assert.assertNull(eventTypeList.getEE());
        eventType = eventTypeList.item(0);
        for (i = 1; i < eventTypeList.getLength() - 1; i++) {
          EventType ith = eventTypeList.item(i);
          if (!(eventType.name.compareTo(ith.name) < 0)) {
            Assert.assertEquals(eventType.name, ith.name);
            Assert.assertTrue(eventType.uri.compareTo(ith.uri) < 0);
          }
          eventType = ith;
        }
      }
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_TP, exiEvent.getEventKind());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
        Assert.assertEquals("type", exiEvent.getName());
        Assert.assertNull(exiEvent.getPrefix());
        Assert.assertEquals(null, exiEvent.getCharacters());
        Assert.assertEquals("anyType", ((EXIEventSchemaType)exiEvent).getTypeName());
        Assert.assertEquals("http://www.w3.org/2001/XMLSchema", ((EXIEventSchemaType)exiEvent).getTypeURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(2, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(5, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_AT, eventType.itemType);
          Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
          Assert.assertEquals("type", eventType.name);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        Assert.assertEquals(2, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(4, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
      }
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(1, eventTypeList.getLength());
        }
      }
      
      Assert.assertEquals(5, n_events);
    }
  }

  /**
   * From EXI interoperability test suite.
   * Schema-Informed Declared Production tests - document
   * schemaInformed.declared.document-01
   */
  public void testBuiltinXsiType_02_docodeOnly() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/interop/schemaInformedGrammar/declaredProductions/document.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));
    
    EXIDecoder decoder = new EXIDecoder();
    Scanner scanner;
    
    decoder.setGrammarCache(grammarCache);
    decoder.setAlignmentType(AlignmentType.byteAligned);

    /**
     * <None xmlns:xsd="http://www.w3.org/2001/XMLSchema" 
     *   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
     *   xmlns='urn:foo' xsi:type="xsd:anyType" />
     */
    URL url = resolveSystemIdAsURL("/interop/schemaInformedGrammar/declaredProductions/document-01.byteAligned");

    decoder.setInputStream(url.openStream());
    scanner = decoder.processHeader();
    
    EventDescription exiEvent;
    
    EventType eventType;
    EventTypeList eventTypeList;
    
    int n_events = 0;
    int i;

    if ((exiEvent = scanner.nextEvent()) != null) {
      ++n_events;
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertNull(eventTypeList.getEE());
    }
    
    if ((exiEvent = scanner.nextEvent()) != null) {
      ++n_events;
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("None", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = exiEvent.getEventType();
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(eventTypeList.getLength() - 1, eventType.getIndex());
      Assert.assertNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      for (i = 1; i < eventTypeList.getLength() - 1; i++) {
        EventType ith = eventTypeList.item(i);
        if (!(eventType.name.compareTo(ith.name) < 0)) {
          Assert.assertEquals(eventType.name, ith.name);
          Assert.assertTrue(eventType.uri.compareTo(ith.uri) < 0);
        }
        eventType = ith;
      }
    }

    for (i = 0; i < 3; i++) {
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
        switch (i) {
          case 0:
            Assert.assertEquals("xsd", exiEvent.getPrefix());
            Assert.assertEquals("http://www.w3.org/2001/XMLSchema", exiEvent.getURI());
            Assert.assertFalse(((EXIEventNS)exiEvent).getLocalElementNs());
            break;
          case 1:
            Assert.assertEquals("xsi", exiEvent.getPrefix());
            Assert.assertEquals("http://www.w3.org/2001/XMLSchema-instance", exiEvent.getURI());
            Assert.assertFalse(((EXIEventNS)exiEvent).getLocalElementNs());
            break;
          case 2:
            Assert.assertEquals("", exiEvent.getPrefix());
            Assert.assertEquals("urn:foo", exiEvent.getURI());
            Assert.assertTrue(((EXIEventNS)exiEvent).getLocalElementNs());
            break;
          default:
            break;
        }
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
        Assert.assertNull(eventType.name);
        Assert.assertNull(eventType.uri);
        Assert.assertEquals(2, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(5, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      }
    }

    if ((exiEvent = scanner.nextEvent()) != null) {
      ++n_events;
      Assert.assertEquals(EventDescription.EVENT_TP, exiEvent.getEventKind());
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
      Assert.assertEquals("type", exiEvent.getName());
      Assert.assertEquals("xsi", exiEvent.getPrefix());
      Assert.assertEquals(null, exiEvent.getCharacters());
      Assert.assertEquals("anyType", ((EXIEventSchemaType)exiEvent).getTypeName());
      Assert.assertEquals("http://www.w3.org/2001/XMLSchema", ((EXIEventSchemaType)exiEvent).getTypeURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      Assert.assertEquals(2, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(6, eventTypeList.getLength());
      Assert.assertNotNull(eventTypeList.getEE());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_AT, eventType.itemType);
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
      Assert.assertEquals("type", eventType.name);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
      eventType = eventTypeList.item(4);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
    }

    if ((exiEvent = scanner.nextEvent()) != null) {
      ++n_events;
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      Assert.assertEquals(4, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(11, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
      eventType = eventTypeList.item(2);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      eventType = eventTypeList.item(3);
      Assert.assertEquals(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
      eventType = eventTypeList.item(5);
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
      eventType = eventTypeList.item(6);
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      eventType = eventTypeList.item(7);
      Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
      eventType = eventTypeList.item(8);
      Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
      eventType = eventTypeList.item(9);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      eventType = eventTypeList.item(10);
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
    }

    if ((exiEvent = scanner.nextEvent()) != null) {
      ++n_events;
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(1, eventTypeList.getLength());
    }
    
    Assert.assertEquals(8, n_events);
  }
  
  /**
   * Use of xsi:type in schema-less EXI stream.
   */
  public void testBuiltinXsiType_03() throws Exception {

    GrammarCache grammarCache = new GrammarCache((EXISchema)null, GrammarOptions.STRICT_OPTIONS);
    
    final String xmlString;
    byte[] bts;
    
    xmlString =
    "<None xmlns:xsd='http://www.w3.org/2001/XMLSchema' "+ 
      "xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " +
      "xmlns='urn:foo' xsi:type='xsd:anyType' />";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setGrammarCache(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EventDescription exiEvent;
      
      EventType eventType;
      EventTypeList eventTypeList;
      
      int n_events = 0;
      int i;
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertNull(eventTypeList.getEE());
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        Assert.assertEquals("None", exiEvent.getName());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = exiEvent.getEventType();
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(eventTypeList.getLength() - 1, eventType.getIndex());
        Assert.assertNull(eventTypeList.getEE());
        eventType = eventTypeList.item(0);
        for (i = 1; i < eventTypeList.getLength() - 1; i++) {
          EventType ith = eventTypeList.item(i);
          if (!(eventType.name.compareTo(ith.name) < 0)) {
            Assert.assertEquals(eventType.name, ith.name);
            Assert.assertTrue(eventType.uri.compareTo(ith.uri) < 0);
          }
          eventType = ith;
        }
      }
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_TP, exiEvent.getEventKind());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
        Assert.assertEquals("type", exiEvent.getName());
        Assert.assertNull(exiEvent.getPrefix());
        Assert.assertEquals(null, exiEvent.getCharacters());
        Assert.assertEquals("anyType", ((EXIEventSchemaType)exiEvent).getTypeName());
        Assert.assertEquals("http://www.w3.org/2001/XMLSchema", ((EXIEventSchemaType)exiEvent).getTypeURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(2, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(5, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_AT, eventType.itemType);
          Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
          Assert.assertEquals("type", eventType.name);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        Assert.assertEquals(2, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(6, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventType.ITEM_AT, eventType.itemType);
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
        Assert.assertEquals("type", eventType.name);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      }
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(1, eventTypeList.getLength());
        }
      }
      
      Assert.assertEquals(5, n_events);
    }
  }

  /**
   */
  public void testBuiltinXsiNil_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String xmlString;
    byte[] bts;
    
    xmlString =
      "<foo:None xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' >" +
        "<foo:None2 xsi:nil='true'>abc</foo:None2>" +
        "<foo:None2 foo:aA='xyz' xsi:nil='true'>abc</foo:None2>" +
      "</foo:None>";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setGrammarCache(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EventDescription exiEvent;
      
      EventType eventType;
      EventTypeList eventTypeList;
      
      int n_events = 0;
      int i;
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertNull(eventTypeList.getEE());
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        Assert.assertEquals("None", exiEvent.getName());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = exiEvent.getEventType();
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(eventTypeList.getLength() - 1, eventType.getIndex());
        Assert.assertNull(eventTypeList.getEE());
        
        eventType = eventTypeList.item(0);
        for (i = 1; i < eventTypeList.getLength() - 1; i++) {
          EventType ith = eventTypeList.item(i);
          if (!(eventType.name.compareTo(ith.name) < 0)) {
            Assert.assertEquals(eventType.name, ith.name);
            Assert.assertTrue(eventType.uri.compareTo(ith.uri) < 0);
          }
          eventType = ith;
        }
      }
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        Assert.assertEquals("None2", exiEvent.getName());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(3, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(5, eventTypeList.getLength());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:foo", eventType.uri);
          Assert.assertEquals("None2", eventType.name);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
        Assert.assertEquals("nil", exiEvent.getName());
        Assert.assertNull(exiEvent.getPrefix());
        Assert.assertEquals("true", exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(2, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(5, eventTypeList.getLength());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_AT, eventType.itemType);
          Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
          Assert.assertEquals("nil", eventType.name);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
        Assert.assertEquals("abc", exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(5, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(6, eventTypeList.getLength());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_AT, eventType.itemType);
          Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
          Assert.assertEquals("nil", eventType.name);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        }
      }

      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(3, eventTypeList.getLength());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        }
      }
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        Assert.assertEquals("None2", exiEvent.getName());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(2, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(4, eventTypeList.getLength());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:foo", eventType.uri);
          Assert.assertEquals("None2", eventType.name);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
        Assert.assertEquals("nil", exiEvent.getName());
        Assert.assertNull(exiEvent.getPrefix());
        Assert.assertEquals("true", exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_AT, eventType.itemType);
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
        Assert.assertEquals("nil", eventType.name);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(1, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(6, eventTypeList.getLength());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(5);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        Assert.assertEquals("aA", exiEvent.getName());
        Assert.assertEquals(null, exiEvent.getPrefix());
        Assert.assertEquals("xyz", exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(4, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(7, eventTypeList.getLength());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_AT, eventType.itemType);
          Assert.assertEquals("urn:foo", eventType.uri);
          Assert.assertEquals("aA", eventType.name);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_AT, eventType.itemType);
          Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
          Assert.assertEquals("nil", eventType.name);
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(5);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(6);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        }
      }

      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
        Assert.assertEquals("abc", exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(1, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(7, eventTypeList.getLength());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_AT, eventType.itemType);
          Assert.assertEquals("urn:foo", eventType.uri);
          Assert.assertEquals("aA", eventType.name);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_AT, eventType.itemType);
          Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
          Assert.assertEquals("nil", eventType.name);
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(5);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(6);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        }
      }

      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(3, eventTypeList.getLength());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        }
      }
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(1, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(4, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("urn:foo", eventType.uri);
          Assert.assertEquals("None2", eventType.name);
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(1, eventTypeList.getLength());
        }
      }
      
      Assert.assertEquals(13, n_events);
    }
  }

  /**
   * Schema:
   * None available
   * 
   * Instance:
   * <!-- abc --><None/><!-- def -->
   */
  public void testBuiltinDocumentComment() throws Exception {

    GrammarCache grammarCache = new GrammarCache(
        GrammarOptions.addCM(GrammarOptions.DEFAULT_OPTIONS));
    
    final String xmlString;
    byte[] bts;
    int n_events;
    
    xmlString = "<!-- abc --><None xmlns='urn:foo' /><!-- def -->\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setGrammarCache(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
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
      Assert.assertEquals(1, eventTypeList.getLength());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CM, exiEvent.getEventKind());
      Assert.assertEquals(" abc ", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_CM, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("None", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_CM, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
        Assert.assertEquals(1, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(6, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventType.ITEM_CM, eventType.itemType);
      }
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CM, exiEvent.getEventKind());
      Assert.assertEquals(" def ", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_CM, eventType.itemType);
      Assert.assertEquals(1, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getLength());
      eventType = eventTypeList.item(0);
      Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      eventTypeList = eventType.getEventTypeList();
      Assert.assertEquals(2, eventTypeList.getLength());
      eventType = eventTypeList.item(1);
      Assert.assertEquals(EventType.ITEM_CM, eventType.itemType);
      ++n_events;

      Assert.assertEquals(6, n_events);
    }
  }

  /**
   * The localName of an element is well-known from schema.
   * However, the element is processed using a built-in element grammar
   * because the element is not declared in schema. Make sure the
   * built-in element grammar is reset across runs.
   * 
   * Schema:
   * <xsd:simpleType name="B">
   *   <xsd:restriction base="xsd:string" />
   * </xsd:simpleType>
   * 
   * Instance:
   * <B/>
   */
  public void testBuiltinElement_With_KnownLocalName() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/oneSimpleType.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String xmlString = "<B/>";

    Transmogrifier encoder = new Transmogrifier();
    EXIDecoder decoder = new EXIDecoder();

    encoder.setGrammarCache(grammarCache);
    decoder.setGrammarCache(grammarCache);
    
    for (AlignmentType alignment : Alignments) {
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      encoder.encode(new InputSource(new StringReader(xmlString)));
      final byte[] bts = baos.toByteArray();
      
      // Repeat twice to make sure Built-in Element Grammars with schema-indexed localNames
      // are reset properly across runs.
      for (int i = 0; i < 2; i++) {
        decoder.setInputStream(new ByteArrayInputStream(bts));
        Scanner scanner = decoder.processHeader();
        
        EventDescription exiEvent;
        EventType eventType;
        EventTypeList eventTypeList;
  
        int n_events = 0;
  
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
        Assert.assertEquals("B", exiEvent.getName());
        Assert.assertEquals("", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(1, eventTypeList.getLength());
        ++n_events;
  
        exiEvent = scanner.nextEvent();
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(1, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(5, eventTypeList.getLength());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
        ++n_events;
  
        exiEvent = scanner.nextEvent();
        Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(1, eventTypeList.getLength());
        ++n_events;
  
        Assert.assertEquals(4, n_events);
      }
    }
  }

  /**
   */
  public void testDecodeAntExample01() throws Exception {

    GrammarCache grammarCache = new GrammarCache(null, GrammarOptions.DEFAULT_OPTIONS);
    
    EXIDecoder decoder = new EXIDecoder();
    Scanner scanner;
        
    decoder.setAlignmentType(AlignmentType.bitPacked);
    decoder.setGrammarCache(grammarCache);
    URL url = resolveSystemIdAsURL("/Ant/build-build.bitPacked");
    decoder.setInputStream(url.openStream());
    scanner = decoder.processHeader();
        
    EventDescription exiEvent;
    
    int n_events = 0;
    int n_undeclaredCharacters = 0;
    int n_attributes = 0;
    while ((exiEvent = scanner.nextEvent()) != null) {
      final byte eventVariety;
      if ((eventVariety = exiEvent.getEventKind()) == EventDescription.EVENT_CH) {
        if (exiEvent.getEventType().itemType == EventType.ITEM_CH) {
          ++n_undeclaredCharacters;
          continue;
        }
      }
      else if (eventVariety == EventDescription.EVENT_AT) {
        ++n_attributes;
      }
      ++n_events;
    }
    
    Assert.assertEquals(401, n_events);
    Assert.assertEquals(0, n_undeclaredCharacters);
    Assert.assertEquals(171, n_attributes);
  }

  /**
   * Simple-content whitespaces are preserved.
   */
  public void testWhitespaces_01() throws Exception {

    GrammarCache grammarCache = new GrammarCache((EXISchema)null, GrammarOptions.DEFAULT_OPTIONS); 
    
    final String xmlString;
    byte[] bts;
    int n_events;
    
    xmlString = "<None xmlns='urn:foo'> \n\t</None>\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setGrammarCache(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      ArrayList<EventDescription> exiEventList = new ArrayList<EventDescription>();
      
      EventDescription exiEvent;
      n_events = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        exiEventList.add(exiEvent);
      }
      
      Assert.assertEquals(5, n_events);
  
      EventType eventType;
  
      exiEvent = exiEventList.get(0);
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      
      exiEvent = exiEventList.get(1);
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("None", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals(" \n\t", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      
      exiEvent = exiEventList.get(3);
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
    }
  }

  /**
   * Complex-content whitespaces are *not* preserved unless there is any
   * non-whitespace content.
   */
  public void testWhitespaces_02() throws Exception {

    GrammarCache grammarCache = new GrammarCache((EXISchema)null, GrammarOptions.DEFAULT_OPTIONS); 
    
    final String xmlString;
    byte[] bts;
    int n_events;
    
    xmlString = "<None xmlns='urn:foo'> <!-- comment --><A/> </None>\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setGrammarCache(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      ArrayList<EventDescription> exiEventList = new ArrayList<EventDescription>();
      
      EventDescription exiEvent;
      n_events = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        exiEventList.add(exiEvent);
      }
      
      Assert.assertEquals(6, n_events);
  
      EventType eventType;
  
      exiEvent = exiEventList.get(0);
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      
      exiEvent = exiEventList.get(1);
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("None", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);

      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("A", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);

      exiEvent = exiEventList.get(3);
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      
      exiEvent = exiEventList.get(4);
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
    }
  }

  /**
   * Complex-content whitespaces are preserved when there is some
   * non-whitespace content such as a comment.
   */
  public void testWhitespaces_03() throws Exception {

    GrammarCache grammarCache = new GrammarCache((EXISchema)null, 
        GrammarOptions.addCM(GrammarOptions.DEFAULT_OPTIONS)); 
    
    final String xmlString;
    byte[] bts;
    int n_events;
    
    xmlString = "<None xmlns='urn:foo'>\t<!-- comment --><A/> </None>\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setGrammarCache(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      ArrayList<EventDescription> exiEventList = new ArrayList<EventDescription>();
      
      EventDescription exiEvent;
      n_events = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        exiEventList.add(exiEvent);
      }
      
      Assert.assertEquals(8, n_events);
  
      EventType eventType;
  
      exiEvent = exiEventList.get(0);
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      
      exiEvent = exiEventList.get(1);
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("None", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);

      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("\t", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);

      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EventDescription.EVENT_CM, exiEvent.getEventKind());
      Assert.assertEquals(" comment ", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_CM, eventType.itemType);
      
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("A", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);

      exiEvent = exiEventList.get(5);
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      
      exiEvent = exiEventList.get(6);
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      
      exiEvent = exiEventList.get(7);
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
    }
  }

  /**
   * Complex-content whitespaces are *not* preserved unless there is any
   * non-whitespace content.
   */
  public void testWhitespaces_04() throws Exception {

    GrammarCache grammarCache = new GrammarCache((EXISchema)null, GrammarOptions.DEFAULT_OPTIONS); 
    
    final String xmlString;
    byte[] bts;
    int n_events;
    
    xmlString = "<None xmlns='urn:foo'> <A/><!-- comment --> </None>\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setGrammarCache(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      ArrayList<EventDescription> exiEventList = new ArrayList<EventDescription>();
      
      EventDescription exiEvent;
      n_events = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        exiEventList.add(exiEvent);
      }
      
      Assert.assertEquals(6, n_events);
  
      EventType eventType;
  
      exiEvent = exiEventList.get(0);
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      
      exiEvent = exiEventList.get(1);
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("None", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);

      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("A", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);

      exiEvent = exiEventList.get(3);
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
      
      exiEvent = exiEventList.get(4);
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      
      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
    }
  }

  /**
   * Complex-content whitespaces are preserved when there is some
   * non-whitespace content such as a comment.
   */
  public void testWhitespaces_05() throws Exception {

    GrammarCache grammarCache = new GrammarCache((EXISchema)null, 
        GrammarOptions.addCM(GrammarOptions.DEFAULT_OPTIONS)); 
    
    final String xmlString;
    byte[] bts;
    int n_events;
    
    xmlString = "<None xmlns='urn:foo'> <A/><!-- comment -->\t</None>\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setGrammarCache(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      ArrayList<EventDescription> exiEventList = new ArrayList<EventDescription>();
      
      EventDescription exiEvent;
      n_events = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        exiEventList.add(exiEvent);
      }
      
      Assert.assertEquals(8, n_events);
  
      EventType eventType;
  
      exiEvent = exiEventList.get(0);
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());
      
      exiEvent = exiEventList.get(1);
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("None", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);

      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      Assert.assertEquals("A", exiEvent.getName());
      Assert.assertEquals("urn:foo", exiEvent.getURI());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);

      exiEvent = exiEventList.get(3);
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());

      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EventDescription.EVENT_CM, exiEvent.getEventKind());
      Assert.assertEquals(" comment ", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_CM, eventType.itemType);

      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("\t", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);

      exiEvent = exiEventList.get(6);
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
      Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
      
      exiEvent = exiEventList.get(7);
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
    }
  }
  
  /**
   * <foo foo='foo at value' /> consists of event sequence:
   * SD SE(foo) AT(foo) EE ED
   * Local-name "foo" is literally encoded twice in the stream on purpose.
   */
  public void testDecodeDuplicateLocalNames_01() throws Exception {

    GrammarCache grammarCache = new GrammarCache(null, GrammarOptions.DEFAULT_OPTIONS);
    
    EXIDecoder decoder = new EXIDecoder();
    Scanner scanner;
        
    decoder.setAlignmentType(AlignmentType.byteAligned);
    decoder.setGrammarCache(grammarCache);
    URL url = resolveSystemIdAsURL("/perversion/duplicateLocalNames_01.byteAligned");
    decoder.setInputStream(url.openStream());
    scanner = decoder.processHeader();

    ArrayList<EventDescription> exiEventList = new ArrayList<EventDescription>();
    
    EventDescription exiEvent;
    int n_events = 0;
    while ((exiEvent = scanner.nextEvent()) != null) {
      ++n_events;
      exiEventList.add(exiEvent);
    }
    
    Assert.assertEquals(5, n_events);
    
    EventType eventType;

    exiEvent = exiEventList.get(0);
    Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SD, eventType.itemType);
    
    exiEvent = exiEventList.get(1);
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("foo", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
    
    exiEvent = exiEventList.get(2);
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("foo", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("foo at value", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
    
    exiEvent = exiEventList.get(3);
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    
    exiEvent = exiEventList.get(4);
    Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
  }

  /**
   * <bla:foo xmlns:bla="uri:bla" bla:foo="bla:foo at value"/> consists of event sequence:
   * SD SE(bla:foo) AT(bla:foo) EE ED
   * URI "uri:bla" and local-name "foo" are both literally encoded twice 
   * in the stream on purpose.
   */
  public void testDecodeDuplicateURIsLocalNames_01() throws Exception {

    GrammarCache grammarCache = new GrammarCache(null, GrammarOptions.DEFAULT_OPTIONS);
    
    EXIDecoder decoder = new EXIDecoder();
    Scanner scanner;
        
    decoder.setAlignmentType(AlignmentType.byteAligned);
    decoder.setGrammarCache(grammarCache);
    URL url = resolveSystemIdAsURL("/perversion/duplicateURIsLocalNames_01.byteAligned");
    decoder.setInputStream(url.openStream());
    scanner = decoder.processHeader();

    ArrayList<EventDescription> exiEventList = new ArrayList<EventDescription>();
    
    EventDescription exiEvent;
    int n_events = 0;
    while ((exiEvent = scanner.nextEvent()) != null) {
      ++n_events;
      exiEventList.add(exiEvent);
    }
    
    Assert.assertEquals(5, n_events);
    
    EventType eventType;

    exiEvent = exiEventList.get(0);
    Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SD, eventType.itemType);
    
    exiEvent = exiEventList.get(1);
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("foo", exiEvent.getName());
    Assert.assertEquals("uri:bla", exiEvent.getURI());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
    
    exiEvent = exiEventList.get(2);
    Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
    Assert.assertEquals("foo", exiEvent.getName());
    Assert.assertEquals("uri:bla", exiEvent.getURI());
    Assert.assertEquals("bla:foo at value", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
    
    exiEvent = exiEventList.get(3);
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
    
    exiEvent = exiEventList.get(4);
    Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
  }
  
  /**
   * Excercise EXI Profile's grammar usage.
   */
  public void testProfile_01() throws Exception {
    GrammarCache grammarCache = new GrammarCache(EmptySchema.getEXISchema(), GrammarOptions.DEFAULT_OPTIONS);
    
    final String xmlString;
    byte[] bts;
    
    xmlString =
      "<foo:None xmlns:foo='urn:foo'>" +
        "<foo:None2></foo:None2>" +
        "<foo:None2></foo:None2>" + 
      "</foo:None>";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setDivertBuiltinGrammarToAnyType(true);
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setGrammarCache(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EventDescription exiEvent;
      
      EventType eventType;
      EventTypeList eventTypeList;
      
      int n_events = 0;
      int i;
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertNull(eventTypeList.getEE());
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        Assert.assertEquals("None", exiEvent.getName());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = exiEvent.getEventType();
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(eventTypeList.getLength() - 1, eventType.getIndex());
        Assert.assertNull(eventTypeList.getEE());
        
        eventType = eventTypeList.item(0);
        for (i = 1; i < eventTypeList.getLength() - 1; i++) {
          EventType ith = eventTypeList.item(i);
          if (!(eventType.name.compareTo(ith.name) < 0)) {
            Assert.assertEquals(eventType.name, ith.name);
            Assert.assertTrue(eventType.uri.compareTo(ith.uri) < 0);
          }
          eventType = ith;
        }
      }
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_TP, exiEvent.getEventKind());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
        Assert.assertEquals("type", exiEvent.getName());
        Assert.assertNull(exiEvent.getPrefix());
        Assert.assertEquals(null, exiEvent.getCharacters());
        Assert.assertEquals("anyType", ((EXIEventSchemaType)exiEvent).getTypeName());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI, ((EXIEventSchemaType)exiEvent).getTypeURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(2, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(5, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_AT, eventType.itemType);
          Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
          Assert.assertEquals("type", eventType.name);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      // First None2
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        Assert.assertEquals("None2", exiEvent.getName());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        Assert.assertEquals(3, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(10, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
        eventType = eventTypeList.item(8);
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(Grammar.BUILTIN_GRAMMAR_ELEMENT, scanner.currentState.targetGrammar.grammarType);
          eventTypeList = scanner.currentState.targetGrammar.getNextEventTypes(scanner.peekState());
          Assert.assertEquals(4, eventTypeList.getLength());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }

      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_TP, exiEvent.getEventKind());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
        Assert.assertEquals("type", exiEvent.getName());
        Assert.assertNull(exiEvent.getPrefix());
        Assert.assertEquals(null, exiEvent.getCharacters());
        Assert.assertEquals("anyType", ((EXIEventSchemaType)exiEvent).getTypeName());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI, ((EXIEventSchemaType)exiEvent).getTypeURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_AND_TYPE, scanner.currentState.targetGrammar.grammarType);
          Assert.assertEquals(2, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(5, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_AT, eventType.itemType);
          Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
          Assert.assertEquals("type", eventType.name);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        Assert.assertEquals(4, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(10, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
        eventType = eventTypeList.item(8);
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      }

      // Second None2
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        Assert.assertEquals("None2", exiEvent.getName());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(5, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
          
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(Grammar.BUILTIN_GRAMMAR_ELEMENT, scanner.currentState.targetGrammar.grammarType);
          eventTypeList = scanner.currentState.targetGrammar.getNextEventTypes(scanner.peekState());
          Assert.assertEquals(5, eventTypeList.getLength());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_AT, eventType.itemType);
          Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
          Assert.assertEquals("type", eventType.name);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }

      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_TP, exiEvent.getEventKind());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
        Assert.assertEquals("type", exiEvent.getName());
        Assert.assertNull(exiEvent.getPrefix());
        Assert.assertEquals(null, exiEvent.getCharacters());
        Assert.assertEquals("anyType", ((EXIEventSchemaType)exiEvent).getTypeName());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI, ((EXIEventSchemaType)exiEvent).getTypeURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_AND_TYPE, scanner.currentState.targetGrammar.grammarType);
          Assert.assertEquals(2, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(5, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_AT, eventType.itemType);
          Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
          Assert.assertEquals("type", eventType.name);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }

      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        Assert.assertEquals(4, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(10, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
        eventType = eventTypeList.item(8);
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      }

      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        Assert.assertEquals(1, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(5, eventTypeList.getLength());
        Assert.assertNotNull(eventTypeList.getEE());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(1, eventTypeList.getLength());
        }
      }

      Assert.assertNull(scanner.nextEvent());
      
      Assert.assertEquals(11, n_events);
    }
  }

  /**
   * Exercise NS production with EXI Profile's grammar usage.
   */
  public void testProfile_02() throws Exception {
    GrammarCache grammarCache = new GrammarCache(EmptySchema.getEXISchema(), GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));
    
    final String xmlString;
    byte[] bts;
    
    xmlString =
      "<foo:None xmlns:foo='urn:foo'>" +
        "<foo:None2></foo:None2>" +
        "<foo:None2></foo:None2>" + 
      "</foo:None>";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setDivertBuiltinGrammarToAnyType(true);
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setGrammarCache(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EventDescription exiEvent;
      
      EventType eventType;
      EventTypeList eventTypeList;
      
      int n_events = 0;
      int i;
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertNull(eventTypeList.getEE());
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        Assert.assertEquals("None", exiEvent.getName());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = exiEvent.getEventType();
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(eventTypeList.getLength() - 1, eventType.getIndex());
        Assert.assertNull(eventTypeList.getEE());
        
        eventType = eventTypeList.item(0);
        for (i = 1; i < eventTypeList.getLength() - 1; i++) {
          EventType ith = eventTypeList.item(i);
          if (!(eventType.name.compareTo(ith.name) < 0)) {
            Assert.assertEquals(eventType.name, ith.name);
            Assert.assertTrue(eventType.uri.compareTo(ith.uri) < 0);
          }
          eventType = ith;
        }
      }
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        Assert.assertEquals("foo", exiEvent.getPrefix());
        Assert.assertNull(exiEvent.getName());
        Assert.assertEquals(null, exiEvent.getCharacters());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(2, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(5, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_TP, exiEvent.getEventKind());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
        Assert.assertEquals("type", exiEvent.getName());
        Assert.assertEquals("xsi", exiEvent.getPrefix());
        Assert.assertEquals(null, exiEvent.getCharacters());
        Assert.assertEquals("anyType", ((EXIEventSchemaType)exiEvent).getTypeName());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI, ((EXIEventSchemaType)exiEvent).getTypeURI());
        Assert.assertNull(((EXIEventSchemaType)exiEvent).getTypePrefix());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(2, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(6, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_AT, eventType.itemType);
          Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
          Assert.assertEquals("type", eventType.name);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(5);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      // First None2
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        Assert.assertEquals("None2", exiEvent.getName());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        Assert.assertEquals(3, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(11, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
        eventType = eventTypeList.item(8);
        Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(10);
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(Grammar.BUILTIN_GRAMMAR_ELEMENT, scanner.currentState.targetGrammar.grammarType);
          eventTypeList = scanner.currentState.targetGrammar.getNextEventTypes(scanner.peekState());
          Assert.assertEquals(5, eventTypeList.getLength());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }

      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_TP, exiEvent.getEventKind());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
        Assert.assertEquals("type", exiEvent.getName());
        Assert.assertEquals("xsi", exiEvent.getPrefix());
        Assert.assertEquals(null, exiEvent.getCharacters());
        Assert.assertEquals("anyType", ((EXIEventSchemaType)exiEvent).getTypeName());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI, ((EXIEventSchemaType)exiEvent).getTypeURI());
        Assert.assertNull(((EXIEventSchemaType)exiEvent).getTypePrefix());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_AND_TYPE, scanner.currentState.targetGrammar.grammarType);
          Assert.assertEquals(2, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(6, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_AT, eventType.itemType);
          Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
          Assert.assertEquals("type", eventType.name);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(5);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        Assert.assertEquals(4, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(11, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
        eventType = eventTypeList.item(8);
        Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(10);
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      }

      // Second None2
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        Assert.assertEquals("None2", exiEvent.getName());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(5, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
          
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(Grammar.BUILTIN_GRAMMAR_ELEMENT, scanner.currentState.targetGrammar.grammarType);
          eventTypeList = scanner.currentState.targetGrammar.getNextEventTypes(scanner.peekState());
          Assert.assertEquals(6, eventTypeList.getLength());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_AT, eventType.itemType);
          Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
          Assert.assertEquals("type", eventType.name);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(5);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }

      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_TP, exiEvent.getEventKind());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
        Assert.assertEquals("type", exiEvent.getName());
        Assert.assertEquals("xsi", exiEvent.getPrefix());
        Assert.assertEquals(null, exiEvent.getCharacters());
        Assert.assertEquals("anyType", ((EXIEventSchemaType)exiEvent).getTypeName());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI, ((EXIEventSchemaType)exiEvent).getTypeURI());
        Assert.assertNull(((EXIEventSchemaType)exiEvent).getTypePrefix());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_AND_TYPE, scanner.currentState.targetGrammar.grammarType);
          Assert.assertEquals(2, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(6, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_AT, eventType.itemType);
          Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
          Assert.assertEquals("type", eventType.name);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(5);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }

      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        Assert.assertEquals(4, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(11, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
        eventType = eventTypeList.item(8);
        Assert.assertEquals(EventType.ITEM_NS, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(10);
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      }

      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        Assert.assertEquals(1, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(5, eventTypeList.getLength());
        Assert.assertNotNull(eventTypeList.getEE());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(1, eventTypeList.getLength());
        }
      }

      Assert.assertNull(scanner.nextEvent());
      
      Assert.assertEquals(12, n_events);
    }
  }

  /**
   * Excercise EXI Profile's grammar usage where type-cast is explicitly specified.
   */
  public void testProfile_03() throws Exception {
    GrammarCache grammarCache = new GrammarCache(EmptySchema.getEXISchema(), GrammarOptions.DEFAULT_OPTIONS);
    
    final String xmlString;
    byte[] bts;
    
    xmlString =
      "<foo:None xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " +
        "xmlns:xsd='http://www.w3.org/2001/XMLSchema'>" +
        "<foo:None2 xsi:type='xsd:int'></foo:None2>" +
        "<foo:None2 xsi:type='xsd:boolean'></foo:None2>" + 
      "</foo:None>";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setDivertBuiltinGrammarToAnyType(true);
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setGrammarCache(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EventDescription exiEvent;
      
      EventType eventType;
      EventTypeList eventTypeList;
      
      int n_events = 0;
      int i;
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertNull(eventTypeList.getEE());
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        Assert.assertEquals("None", exiEvent.getName());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = exiEvent.getEventType();
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(eventTypeList.getLength() - 1, eventType.getIndex());
        Assert.assertNull(eventTypeList.getEE());
        
        eventType = eventTypeList.item(0);
        for (i = 1; i < eventTypeList.getLength() - 1; i++) {
          EventType ith = eventTypeList.item(i);
          if (!(eventType.name.compareTo(ith.name) < 0)) {
            Assert.assertEquals(eventType.name, ith.name);
            Assert.assertTrue(eventType.uri.compareTo(ith.uri) < 0);
          }
          eventType = ith;
        }
      }
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_TP, exiEvent.getEventKind());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
        Assert.assertEquals("type", exiEvent.getName());
        Assert.assertNull(exiEvent.getPrefix());
        Assert.assertEquals(null, exiEvent.getCharacters());
        Assert.assertEquals("anyType", ((EXIEventSchemaType)exiEvent).getTypeName());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI, ((EXIEventSchemaType)exiEvent).getTypeURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(2, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(5, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_AT, eventType.itemType);
          Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
          Assert.assertEquals("type", eventType.name);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      // First None2
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        Assert.assertEquals("None2", exiEvent.getName());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        Assert.assertEquals(3, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(10, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
        eventType = eventTypeList.item(8);
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(Grammar.BUILTIN_GRAMMAR_ELEMENT, scanner.currentState.targetGrammar.grammarType);
          eventTypeList = scanner.currentState.targetGrammar.getNextEventTypes(scanner.peekState());
          Assert.assertEquals(4, eventTypeList.getLength());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }

      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_TP, exiEvent.getEventKind());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
        Assert.assertEquals("type", exiEvent.getName());
        Assert.assertNull(exiEvent.getPrefix());
        Assert.assertEquals(null, exiEvent.getCharacters());
        Assert.assertEquals("int", ((EXIEventSchemaType)exiEvent).getTypeName());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI, ((EXIEventSchemaType)exiEvent).getTypeURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_AND_TYPE, scanner.currentState.targetGrammar.grammarType);
          Assert.assertEquals(2, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(5, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_AT, eventType.itemType);
          Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
          Assert.assertEquals("type", eventType.name);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        Assert.assertEquals(3, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(8, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
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
      }

      // Second None2
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        Assert.assertEquals("None2", exiEvent.getName());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(5, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
          
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(Grammar.BUILTIN_GRAMMAR_ELEMENT, scanner.currentState.targetGrammar.grammarType);
          eventTypeList = scanner.currentState.targetGrammar.getNextEventTypes(scanner.peekState());
          Assert.assertEquals(5, eventTypeList.getLength());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_AT, eventType.itemType);
          Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
          Assert.assertEquals("type", eventType.name);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }

      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_TP, exiEvent.getEventKind());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
        Assert.assertEquals("type", exiEvent.getName());
        Assert.assertNull(exiEvent.getPrefix());
        Assert.assertEquals(null, exiEvent.getCharacters());
        Assert.assertEquals("boolean", ((EXIEventSchemaType)exiEvent).getTypeName());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI, ((EXIEventSchemaType)exiEvent).getTypeURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_AND_TYPE, scanner.currentState.targetGrammar.grammarType);
          Assert.assertEquals(2, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(5, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_AT, eventType.itemType);
          Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
          Assert.assertEquals("type", eventType.name);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }

      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        Assert.assertEquals(3, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(8, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
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
      }

      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        Assert.assertEquals(1, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(5, eventTypeList.getLength());
        Assert.assertNotNull(eventTypeList.getEE());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(1, eventTypeList.getLength());
        }
      }

      Assert.assertNull(scanner.nextEvent());
      
      Assert.assertEquals(11, n_events);
    }
  }

  /**
   * Excercise xsi:nil with EXI Profile's grammar usage.
   */
  public void testProfile_04() throws Exception {
    GrammarCache grammarCache = new GrammarCache(EmptySchema.getEXISchema(), GrammarOptions.DEFAULT_OPTIONS);
    
    final String xmlString;
    byte[] bts;
    
    xmlString =
      "<foo:None xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>" +
        "<foo:None2 xsi:nil='true'></foo:None2>" +
        "<foo:None2 xsi:nil='true'></foo:None2>" + 
      "</foo:None>";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      
      encoder.setDivertBuiltinGrammarToAnyType(true);
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setGrammarCache(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setGrammarCache(grammarCache);
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EventDescription exiEvent;
      
      EventType eventType;
      EventTypeList eventTypeList;
      
      int n_events = 0;
      int i;
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertNull(eventTypeList.getEE());
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        Assert.assertEquals("None", exiEvent.getName());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = exiEvent.getEventType();
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(eventTypeList.getLength() - 1, eventType.getIndex());
        Assert.assertNull(eventTypeList.getEE());
        
        eventType = eventTypeList.item(0);
        for (i = 1; i < eventTypeList.getLength() - 1; i++) {
          EventType ith = eventTypeList.item(i);
          if (!(eventType.name.compareTo(ith.name) < 0)) {
            Assert.assertEquals(eventType.name, ith.name);
            Assert.assertTrue(eventType.uri.compareTo(ith.uri) < 0);
          }
          eventType = ith;
        }
      }
  
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_TP, exiEvent.getEventKind());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
        Assert.assertEquals("type", exiEvent.getName());
        Assert.assertNull(exiEvent.getPrefix());
        Assert.assertEquals(null, exiEvent.getCharacters());
        Assert.assertEquals("anyType", ((EXIEventSchemaType)exiEvent).getTypeName());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI, ((EXIEventSchemaType)exiEvent).getTypeURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(2, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(5, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_AT, eventType.itemType);
          Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
          Assert.assertEquals("type", eventType.name);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      // First None2
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        Assert.assertEquals("None2", exiEvent.getName());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        Assert.assertEquals(3, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(10, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
        eventType = eventTypeList.item(8);
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(Grammar.BUILTIN_GRAMMAR_ELEMENT, scanner.currentState.targetGrammar.grammarType);
          eventTypeList = scanner.currentState.targetGrammar.getNextEventTypes(scanner.peekState());
          Assert.assertEquals(4, eventTypeList.getLength());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }

      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_TP, exiEvent.getEventKind());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
        Assert.assertEquals("type", exiEvent.getName());
        Assert.assertNull(exiEvent.getPrefix());
        Assert.assertEquals(null, exiEvent.getCharacters());
        Assert.assertEquals("anyType", ((EXIEventSchemaType)exiEvent).getTypeName());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI, ((EXIEventSchemaType)exiEvent).getTypeURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_AND_TYPE, scanner.currentState.targetGrammar.grammarType);
          Assert.assertEquals(2, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(5, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_AT, eventType.itemType);
          Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
          Assert.assertEquals("type", eventType.name);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_NL, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        Assert.assertEquals(1, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(10, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
        eventType = eventTypeList.item(8);
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        Assert.assertEquals(3, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(8, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
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
      }

      // Second None2
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        Assert.assertEquals("None2", exiEvent.getName());
        Assert.assertEquals("urn:foo", exiEvent.getURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        Assert.assertEquals(0, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(5, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
          
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(Grammar.BUILTIN_GRAMMAR_ELEMENT, scanner.currentState.targetGrammar.grammarType);
          eventTypeList = scanner.currentState.targetGrammar.getNextEventTypes(scanner.peekState());
          Assert.assertEquals(5, eventTypeList.getLength());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_AT, eventType.itemType);
          Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
          Assert.assertEquals("type", eventType.name);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }

      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_TP, exiEvent.getEventKind());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
        Assert.assertEquals("type", exiEvent.getName());
        Assert.assertNull(exiEvent.getPrefix());
        Assert.assertEquals(null, exiEvent.getCharacters());
        Assert.assertEquals("anyType", ((EXIEventSchemaType)exiEvent).getTypeName());
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI, ((EXIEventSchemaType)exiEvent).getTypeURI());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(Grammar.SCHEMA_GRAMMAR_ELEMENT_AND_TYPE, scanner.currentState.targetGrammar.grammarType);
          Assert.assertEquals(2, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(5, eventTypeList.getLength());
          Assert.assertNotNull(eventTypeList.getEE());
          eventType = eventTypeList.item(0);
          Assert.assertEquals(EventType.ITEM_AT, eventType.itemType);
          Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
          Assert.assertEquals("type", eventType.name);
          eventType = eventTypeList.item(1);
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(2);
          Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
          eventType = eventTypeList.item(3);
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
      }

      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_NL, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        Assert.assertEquals(1, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(10, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.assertEquals(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_TWO, eventType.getDepth());
        eventType = eventTypeList.item(7);
        Assert.assertEquals(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_THREE, eventType.getDepth());
        eventType = eventTypeList.item(8);
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        Assert.assertEquals(3, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(8, eventTypeList.getLength());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
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
      }

      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        Assert.assertEquals(1, eventType.getIndex());
        eventTypeList = eventType.getEventTypeList();
        Assert.assertEquals(5, eventTypeList.getLength());
        Assert.assertNotNull(eventTypeList.getEE());
        eventType = eventTypeList.item(0);
        Assert.assertEquals(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
        Assert.assertEquals(EventCode.EVENT_CODE_DEPTH_ONE, eventType.getDepth());
        eventType = eventTypeList.item(2);
        Assert.assertEquals(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      }
      
      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(1, eventTypeList.getLength());
        }
      }

      Assert.assertNull(scanner.nextEvent());
      
      Assert.assertEquals(13, n_events);
    }
  }
  
}
