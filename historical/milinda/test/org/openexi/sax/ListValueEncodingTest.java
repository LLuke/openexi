package org.openexi.sax;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.ArrayList;

import org.openexi.proc.EXIDecoder;
import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EXIEvent;
import org.openexi.proc.common.EventCode;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.grammars.EventTypeSchema;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.proc.io.Scanner;
import org.openexi.sax.Transmogrifier;
import org.openexi.schema.EXISchema;
import org.openexi.scomp.EXISchemaFactoryErrorMonitor;
import org.openexi.scomp.EXISchemaFactoryTestUtil;
import org.xml.sax.InputSource;

import junit.framework.Assert;
import junit.framework.TestCase;

public class ListValueEncodingTest extends TestCase {

  public ListValueEncodingTest(String name) {
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
   * A list of valid int values matching ITEM_SCHEMA_CH where the associated
   * datatype is a list of xsd:int.
   */
  public void testValidIntList() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/int.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String xmlString = "<foo:WorkingAges xmlns:foo='urn:foo'> \t\t 15\r 65  \n78\n</foo:WorkingAges>\n";
    
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
  
      exiEvent = exiEventList.get(0);
      Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SD, eventType.itemType);
      
      exiEvent = exiEventList.get(1);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("WorkingAges", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("15 65 78", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      int tp = ((EventTypeSchema)eventType).getSchemaSubstance();
      assert corpus.getVarietyOfSimpleType(tp) == EXISchema.LIST_SIMPLE_TYPE;
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_ED, eventType.itemType);
    }
  }

  /**
   * Repeating list of valid int values matching ITEM_SCHEMA_CH where the 
   * associated datatype is a list of xsd:int.
   */
  public void testValidIntListRepeated() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/int.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String xmlString = 
      "<foo:WorkingAgesRepeated xmlns:foo='urn:foo'>" + 
        "<foo:WorkingAges>15 65  78</foo:WorkingAges>" + 
        "<foo:WorkingAges>16 60  77</foo:WorkingAges>" + 
      "</foo:WorkingAgesRepeated>\n";
    
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
      int tp;
  
      exiEvent = exiEventList.get(0);
      Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SD, eventType.itemType);

      exiEvent = exiEventList.get(1);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("WorkingAgesRepeated", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());

      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("WorkingAges", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("15 65 78", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      tp = ((EventTypeSchema)eventType).getSchemaSubstance();
      assert corpus.getVarietyOfSimpleType(tp) == EXISchema.LIST_SIMPLE_TYPE;
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);

      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("WorkingAges", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("16 60 77", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      tp = ((EventTypeSchema)eventType).getSchemaSubstance();
      assert corpus.getVarietyOfSimpleType(tp) == EXISchema.LIST_SIMPLE_TYPE;
  
      exiEvent = exiEventList.get(7);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);

      exiEvent = exiEventList.get(8);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      
      exiEvent = exiEventList.get(9);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_ED, eventType.itemType);
    }
  }
  
  /**
   * A list of zero values matching ITEM_SCHEMA_CH where the associated
   * datatype is a list of xsd:int.
   */
  public void testValidIntEmptyList_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/int.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String xmlString = "<foo:WorkingAges xmlns:foo='urn:foo'> </foo:WorkingAges>\n";
    
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
  
      exiEvent = exiEventList.get(0);
      Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SD, eventType.itemType);
      
      exiEvent = exiEventList.get(1);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("WorkingAges", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      int tp = ((EventTypeSchema)eventType).getSchemaSubstance();
      assert corpus.getVarietyOfSimpleType(tp) == EXISchema.LIST_SIMPLE_TYPE;
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_ED, eventType.itemType);
    }
  }

  /**
   * A list of zero values matching ITEM_SCHEMA_CH where the associated
   * datatype is a list of xsd:int.
   */
  public void testValidIntEmptyList_02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/int.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String xmlString = "<foo:WorkingAges xmlns:foo='urn:foo'/>\n";
    
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
  
      exiEvent = exiEventList.get(0);
      Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SD, eventType.itemType);
      
      exiEvent = exiEventList.get(1);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("WorkingAges", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      int tp = ((EventTypeSchema)eventType).getSchemaSubstance();
      assert corpus.getVarietyOfSimpleType(tp) == EXISchema.LIST_SIMPLE_TYPE;
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_ED, eventType.itemType);
    }
  }

  /**
   * A list of valid decimal values matching ITEM_SCHEMA_CH where the associated
   * datatype is a list of xsd:decimal.
   */
  public void testValidDecimalList() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/decimal.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String xmlString = "<foo:ListOfDecimals xmlns:foo='urn:foo'>" +
    		"-1267.89675 92233720368547758070000000000.00000000002233720368547758079  1267.00675" +
    		"</foo:ListOfDecimals>\n";
    
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
  
      exiEvent = exiEventList.get(0);
      Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SD, eventType.itemType);
      
      exiEvent = exiEventList.get(1);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("ListOfDecimals", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("-1267.89675 92233720368547758070000000000.00000000002233720368547758079 1267.00675", 
          exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      int tp = ((EventTypeSchema)eventType).getSchemaSubstance();
      assert corpus.getVarietyOfSimpleType(tp) == EXISchema.LIST_SIMPLE_TYPE;
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_ED, eventType.itemType);
    }
  }

  /**
   * Repeating list of valid decimal values matching ITEM_SCHEMA_CH where the 
   * associated datatype is a list of xsd:decimal.
   */
  public void testValidDecimalListRepeated() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/decimal.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String xmlString = 
      "<foo:ListOfDecimalsRepeated xmlns:foo='urn:foo'>" + 
        "<foo:ListOfDecimals>-1267.89675 92233720368547758070000000000.00000000002233720368547758079  1267.00675</foo:ListOfDecimals>" + 
        "<foo:ListOfDecimals>1267.89675  -1267.00675 -92233720368547758070000000000.00000000002233720368547758079</foo:ListOfDecimals>" + 
      "</foo:ListOfDecimalsRepeated>\n";

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
      int tp;
  
      exiEvent = exiEventList.get(0);
      Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SD, eventType.itemType);

      exiEvent = exiEventList.get(1);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("ListOfDecimalsRepeated", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());

      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("ListOfDecimals", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("-1267.89675 92233720368547758070000000000.00000000002233720368547758079 1267.00675",
          exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      tp = ((EventTypeSchema)eventType).getSchemaSubstance();
      assert corpus.getVarietyOfSimpleType(tp) == EXISchema.LIST_SIMPLE_TYPE;
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);

      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("ListOfDecimals", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("1267.89675 -1267.00675 -92233720368547758070000000000.00000000002233720368547758079", 
          exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      tp = ((EventTypeSchema)eventType).getSchemaSubstance();
      assert corpus.getVarietyOfSimpleType(tp) == EXISchema.LIST_SIMPLE_TYPE;
  
      exiEvent = exiEventList.get(7);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);

      exiEvent = exiEventList.get(8);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      
      exiEvent = exiEventList.get(9);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_ED, eventType.itemType);
    }
  }

  /**
   * A list of valid dateTime values matching ITEM_SCHEMA_CH where the associated
   * datatype is a list of xsd:dateTime.
   */
  public void testValidDateTimeList() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/dateTime.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String xmlString = "<foo:ListOfDateTimes xmlns:foo='urn:foo'>" +
                "2003-04-25T11:41:30.45+09:00 2003-04-25T11:41:30.45+14:00  1997-07-16T19:20:30.45-12:00" +
                "</foo:ListOfDateTimes>\n";

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
  
      exiEvent = exiEventList.get(0);
      Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SD, eventType.itemType);
      
      exiEvent = exiEventList.get(1);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("ListOfDateTimes", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("2003-04-25T11:41:30.45+09:00 2003-04-25T11:41:30.45+14:00 1997-07-16T19:20:30.45-12:00", 
          exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      int tp = ((EventTypeSchema)eventType).getSchemaSubstance();
      assert corpus.getVarietyOfSimpleType(tp) == EXISchema.LIST_SIMPLE_TYPE;
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_ED, eventType.itemType);
    }
  }

  /**
   * Repeating list of valid dateTime values matching ITEM_SCHEMA_CH where the 
   * associated datatype is a list of xsd:dateTime.
   */
  public void testValidDateTimeListRepeated() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/dateTime.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String xmlString = 
      "<foo:ListOfDateTimesRepeated xmlns:foo='urn:foo'>" + 
        "<foo:ListOfDateTimes>2003-04-25T11:41:30.45+09:00 2003-04-25T11:41:30.45+14:00  1997-07-16T19:20:30.45-12:00</foo:ListOfDateTimes>" + 
        "<foo:ListOfDateTimes>1997-07-16T19:20:30.45Z  1999-12-31T24:00:00 -0601-07-16T19:20:30.45-05:09</foo:ListOfDateTimes>" + 
      "</foo:ListOfDateTimesRepeated>\n";

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
      int tp;
  
      exiEvent = exiEventList.get(0);
      Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SD, eventType.itemType);

      exiEvent = exiEventList.get(1);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("ListOfDateTimesRepeated", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());

      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("ListOfDateTimes", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("2003-04-25T11:41:30.45+09:00 2003-04-25T11:41:30.45+14:00 1997-07-16T19:20:30.45-12:00",
          exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      tp = ((EventTypeSchema)eventType).getSchemaSubstance();
      assert corpus.getVarietyOfSimpleType(tp) == EXISchema.LIST_SIMPLE_TYPE;
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);

      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("ListOfDateTimes", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("1997-07-16T19:20:30.45Z 1999-12-31T24:00:00 -0601-07-16T19:20:30.45-05:09", 
          exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      tp = ((EventTypeSchema)eventType).getSchemaSubstance();
      assert corpus.getVarietyOfSimpleType(tp) == EXISchema.LIST_SIMPLE_TYPE;
  
      exiEvent = exiEventList.get(7);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);

      exiEvent = exiEventList.get(8);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      
      exiEvent = exiEventList.get(9);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_ED, eventType.itemType);
    }
  }

  /**
   * A list of valid base64Binary values matching ITEM_SCHEMA_CH where the associated
   * datatype is a list of xsd:base64Binary.
   */
  public void testValidBase64BinaryList() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/base64Binary.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String xmlString = "<foo:ListOfBase64Binaries xmlns:foo='urn:foo'>" +
                "aGVsbG8NCndvcmxk RVhj QmFzZTY0" +
                "</foo:ListOfBase64Binaries>\n";

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
  
      exiEvent = exiEventList.get(0);
      Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SD, eventType.itemType);
      
      exiEvent = exiEventList.get(1);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("ListOfBase64Binaries", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("aGVsbG8NCndvcmxk RVhj QmFzZTY0", 
          exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      int tp = ((EventTypeSchema)eventType).getSchemaSubstance();
      assert corpus.getVarietyOfSimpleType(tp) == EXISchema.LIST_SIMPLE_TYPE;
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_ED, eventType.itemType);
    }
  }
  
  /**
   * Repeating list of valid base64Binary values matching ITEM_SCHEMA_CH where the 
   * associated datatype is a list of xsd:base64Binary.
   */
  public void testValidBase64BinaryListRepeated() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/base64Binary.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    final String xmlString = 
      "<foo:ListOfBase64BinariesRepeated xmlns:foo='urn:foo'>" + 
        "<foo:ListOfBase64Binaries>aGVsbG8NCndvcmxk RVhj QmFzZTY0</foo:ListOfBase64Binaries>" + 
        "<foo:ListOfBase64Binaries>QUJDREVGR0hJSg== S0xNTk9QUVJTVA== VVZXWFla</foo:ListOfBase64Binaries>" + 
      "</foo:ListOfBase64BinariesRepeated>\n";

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
      int tp;
  
      exiEvent = exiEventList.get(0);
      Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SD, eventType.itemType);

      exiEvent = exiEventList.get(1);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("ListOfBase64BinariesRepeated", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());

      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("ListOfBase64Binaries", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("aGVsbG8NCndvcmxk RVhj QmFzZTY0",
          exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      tp = ((EventTypeSchema)eventType).getSchemaSubstance();
      assert corpus.getVarietyOfSimpleType(tp) == EXISchema.LIST_SIMPLE_TYPE;
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);

      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("ListOfBase64Binaries", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("QUJDREVGR0hJSg== S0xNTk9QUVJTVA== VVZXWFla", 
          exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      tp = ((EventTypeSchema)eventType).getSchemaSubstance();
      assert corpus.getVarietyOfSimpleType(tp) == EXISchema.LIST_SIMPLE_TYPE;
  
      exiEvent = exiEventList.get(7);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);

      exiEvent = exiEventList.get(8);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      
      exiEvent = exiEventList.get(9);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_ED, eventType.itemType);
    }
  }
  
  /**
   * A list of valid boolean values matching ITEM_SCHEMA_CH where the associated
   * datatype is a list of xsd:boolean.
   */
  public void testValidBooleanList() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/boolean.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String xmlString = "<foo:Booleans xmlns:foo='urn:foo'> \t\t true\r false  \ntrue true\n</foo:Booleans>\n";
    
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
  
      exiEvent = exiEventList.get(0);
      Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SD, eventType.itemType);
      
      exiEvent = exiEventList.get(1);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("Booleans", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("true false true true", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      int tp = ((EventTypeSchema)eventType).getSchemaSubstance();
      assert corpus.getVarietyOfSimpleType(tp) == EXISchema.LIST_SIMPLE_TYPE;
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_ED, eventType.itemType);
    }
  }

  /**
   * A list of valid token values matching ITEM_SCHEMA_CH where the associated
   * datatype is a list of xsd:token.
   */
  public void testValidTokenList() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/token.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String xmlString = "<foo:ListOfTokens xmlns:foo='urn:foo'>  en fr it de br</foo:ListOfTokens>\n";
    
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
  
      exiEvent = exiEventList.get(0);
      Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SD, eventType.itemType);
      
      exiEvent = exiEventList.get(1);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("ListOfTokens", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      
      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("en fr it de br", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      int tp = ((EventTypeSchema)eventType).getSchemaSubstance();
      assert corpus.getVarietyOfSimpleType(tp) == EXISchema.LIST_SIMPLE_TYPE;
  
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_ED, eventType.itemType);
    }
  }

  /**
   * Repeating list of valid token values matching ITEM_SCHEMA_CH where the 
   * associated datatype is a list of xsd:token.
   */
  public void testValidTokenListRepeated() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/token.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    final String xmlString = 
      "<foo:ListOfTokensRepeated xmlns:foo='urn:foo'>" + 
        "<foo:ListOfTokens>en fr it de br</foo:ListOfTokens>" + 
        "<foo:ListOfTokens>ja kr cn</foo:ListOfTokens>" + 
      "</foo:ListOfTokensRepeated>\n";

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
      int tp;
  
      exiEvent = exiEventList.get(0);
      Assert.assertEquals(EXIEvent.EVENT_SD, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SD, eventType.itemType);

      exiEvent = exiEventList.get(1);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("ListOfTokensRepeated", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());

      exiEvent = exiEventList.get(2);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("ListOfTokens", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      
      exiEvent = exiEventList.get(3);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("en fr it de br", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      tp = ((EventTypeSchema)eventType).getSchemaSubstance();
      assert corpus.getVarietyOfSimpleType(tp) == EXISchema.LIST_SIMPLE_TYPE;
  
      exiEvent = exiEventList.get(4);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);

      exiEvent = exiEventList.get(5);
      Assert.assertEquals(EXIEvent.EVENT_SE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_SE, eventType.itemType);
      Assert.assertEquals("ListOfTokens", eventType.getName());
      Assert.assertEquals("urn:foo", eventType.getURI());
      
      exiEvent = exiEventList.get(6);
      Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
      Assert.assertEquals("ja kr cn", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
      tp = ((EventTypeSchema)eventType).getSchemaSubstance();
      assert corpus.getVarietyOfSimpleType(tp) == EXISchema.LIST_SIMPLE_TYPE;
  
      exiEvent = exiEventList.get(7);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);

      exiEvent = exiEventList.get(8);
      Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_SCHEMA_EE, eventType.itemType);
      
      exiEvent = exiEventList.get(9);
      Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventCode.ITEM_ED, eventType.itemType);
    }
  }
  
  /**
   * Preserve lexical values of int list by turning on Preserve.lexicalValues.
   */
  public void testIntListRCS() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/int.xsd", getClass(), m_compilerErrors);

    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\t *15*\r *65*  \n*78*\n", // '*' will be encoded as an escaped character 
    };
    final String[] parsedOriginalValues = {
        " \t\t *15*\n *65*  \n*78*\n", 
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:WorkingAges xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:WorkingAges>\n";
    };
    
    Transmogrifier encoder = new Transmogrifier();
    EXIDecoder decoder = new EXIDecoder();
    
    encoder.setEXISchema(grammarCache);
    decoder.setEXISchema(grammarCache);

    encoder.setPreserveLexicalValues(true);
    decoder.setPreserveLexicalValues(true);

    for (AlignmentType alignment : Alignments) {
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);
      for (i = 0; i < xmlStrings.length; i++) {
        Scanner scanner;
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        encoder.setOutputStream(baos);
        
        byte[] bts;
        int n_events, n_texts;
        
        encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
        
        bts = baos.toByteArray();
        
        decoder.setInputStream(new ByteArrayInputStream(bts));
        scanner = decoder.processHeader();
        
        ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();

        EXIEvent exiEvent;
        n_events = 0;
        n_texts = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          if (exiEvent.getEventVariety() == EXIEvent.EVENT_CH) {
            String stringValue = exiEvent.getCharacters().makeString();
            Assert.assertEquals(parsedOriginalValues[i], stringValue);
            Assert.assertTrue(exiEvent.getEventType().isSchemaInformed());
            ++n_texts;
          }
          exiEventList.add(exiEvent);
        }
        Assert.assertEquals(1, n_texts);
        Assert.assertEquals(5, n_events);
      }
    }
  }

  /**
   * Preserve lexical values of enumerated gMonthDay list by turning on Preserve.lexicalValues.
   */
  public void testlistOfEnumerationRCS() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/list.xsd", getClass(), m_compilerErrors);

    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\t *--09-18*\r *--09-20*  \n*--09-16*\n", // '*' will be encoded as an escaped character 
    };
    final String[] parsedOriginalValues = {
        " \t\t *--09-18*\n *--09-20*  \n*--09-16*\n", // '*' will be encoded as an escaped character 
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:ListOfEnumeratedGMonthDay xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:ListOfEnumeratedGMonthDay>\n";
    };
    
    Transmogrifier encoder = new Transmogrifier();
    EXIDecoder decoder = new EXIDecoder();
    
    encoder.setEXISchema(grammarCache);
    decoder.setEXISchema(grammarCache);

    encoder.setPreserveLexicalValues(true);
    decoder.setPreserveLexicalValues(true);

    for (AlignmentType alignment : Alignments) {
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);
      for (i = 0; i < xmlStrings.length; i++) {
        Scanner scanner;
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        encoder.setOutputStream(baos);
        
        byte[] bts;
        int n_events, n_texts;
        
        encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
        
        bts = baos.toByteArray();
        
        decoder.setInputStream(new ByteArrayInputStream(bts));
        scanner = decoder.processHeader();
        
        ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();

        EXIEvent exiEvent;
        n_events = 0;
        n_texts = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          if (exiEvent.getEventVariety() == EXIEvent.EVENT_CH) {
            String stringValue = exiEvent.getCharacters().makeString();
            Assert.assertEquals(parsedOriginalValues[i], stringValue);
            Assert.assertTrue(exiEvent.getEventType().isSchemaInformed());
            ++n_texts;
          }
          exiEventList.add(exiEvent);
        }
        Assert.assertEquals(1, n_texts);
        Assert.assertEquals(5, n_events);
      }
    }
  }
  
}
