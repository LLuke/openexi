package org.openexi.sax;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;

import junit.framework.Assert;

import org.openexi.proc.EXIDecoder;
import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EventDescription;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.common.XmlUriConst;
import org.openexi.proc.events.EXIEventNS;
import org.openexi.proc.events.EXIEventSchemaType;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.proc.io.Scanner;
import org.openexi.schema.EXISchema;
import org.openexi.schema.EXISchemaConst;
import org.openexi.schema.EXISchemaUtil;
import org.openexi.schema.TestBase;
import org.openexi.scomp.EXISchemaFactoryErrorMonitor;
import org.openexi.scomp.EXISchemaFactoryTestUtil;
import org.xml.sax.InputSource;

public class EnumerationTest extends TestBase {

  public EnumerationTest(String name) {
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
   * A valid dateTime value matching ITEM_SCHEMA_CH where the associated
   * datatype is an enumeration of xsd:dateTime.
   */
  public void testDateTime() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/enumeration.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\r 2003-03-19T12:20:00-06:00\n",
        "2003-03-20T14:20:00-04:00",
        "2003-03-21T18:20:00Z",
        "2013-06-04T05:00:00Z",
        "2013-06-03T24:00:00-06:00",
        "2012-06-30T23:59:60Z",
        "----------",
        "xyz",
    };
    final String[] parsedOriginalValues = {
        " \t\n 2003-03-19T12:20:00-06:00\n",
        "2003-03-20T14:20:00-04:00",
        "2003-03-21T18:20:00Z",
        "2013-06-04T05:00:00Z",
        "2013-06-03T24:00:00-06:00",
        "2012-06-30T23:59:60Z",
        "----------",
        "xyz",
    };
    final String[] resultValues = {
        "2003-03-19T13:20:00-05:00",
        "2003-03-20T13:20:00-05:00",
        "2003-03-21T13:20:00-05:00",
        "2013-06-03T24:00:00-05:00",
        "2013-06-04T06:00:00Z",
        "2012-07-01T00:00:00Z",
        "----------",
        "xyz",
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:DateTimeDerived xmlns:foo='urn:foo'>" +
        originalValues[i] + "</foo:DateTimeDerived>\n";
    };
    
    for (AlignmentType alignment : Alignments) {
      for (boolean preserveLexicalValues : new boolean[] { false, true }) {
        String[] values = preserveLexicalValues ? parsedOriginalValues : resultValues;
        boolean isValidValue = true;
        for (i = 0; i < xmlStrings.length; i++) {
          final String originalValue = xmlStrings[i];
          if (originalValue.contains("----------")) {
            isValidValue = false;
            continue;
          }
          
          Transmogrifier encoder = new Transmogrifier();
          EXIDecoder decoder = new EXIDecoder();
          Scanner scanner;
          
          encoder.setAlignmentType(alignment);
          decoder.setAlignmentType(alignment);
    
          encoder.setPreserveLexicalValues(preserveLexicalValues);
          decoder.setPreserveLexicalValues(preserveLexicalValues);
          
          encoder.setGrammarCache(grammarCache);
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          encoder.setOutputStream(baos);
          
          byte[] bts;
          int n_events;
          
          try {
            encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
          }
          catch (TransmogrifierException te) {
            Assert.assertTrue(!preserveLexicalValues && !isValidValue);
            continue;
          }
          Assert.assertTrue(preserveLexicalValues || isValidValue);
          
          bts = baos.toByteArray();
          
          decoder.setGrammarCache(grammarCache);
          decoder.setInputStream(new ByteArrayInputStream(bts));
          scanner = decoder.processHeader();
          
          EventDescription exiEvent;
          n_events = 0;
      
          EventType eventType;
      
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SD, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("DateTimeDerived", eventType.name);
          Assert.assertEquals("urn:foo", eventType.uri);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals(values[i], exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            int tp = scanner.currentState.contentDatatype; 
            int builtinType = corpus.getBaseTypeOfSimpleType(tp);
            Assert.assertEquals(EXISchemaConst.DATETIME_TYPE, corpus.getSerialOfType(builtinType));
          }
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
          ++n_events;

          Assert.assertEquals(5, n_events);
        }
      }
    }
  }

  /**
   * A valid date value matching ITEM_SCHEMA_CH where the associated
   * datatype is an enumeration of xsd:date.
   */
  public void testDate() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/enumeration.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\r 2003-03-19-05:00\n",
        "2003-03-21-05:00",
        "2003-03-23-05:00"
    };
    final String[] parsedOriginalValues = {
        " \t\n 2003-03-19-05:00\n",
        "2003-03-21-05:00",
        "2003-03-23-05:00"
    };
    final String[] resultValues = {
        "2003-03-19-05:00",
        "2003-03-21-05:00",
        "2003-03-23-05:00"
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " +
        "xsi:type='foo:dateDerived' >" + originalValues[i] + "</foo:A>\n";
    };
    
    for (AlignmentType alignment : Alignments) {
      for (boolean preserveLexicalValues : new boolean[] { true, false }) {
        String[] values = preserveLexicalValues ? parsedOriginalValues : resultValues;
        for (i = 0; i < xmlStrings.length; i++) {
          Transmogrifier encoder = new Transmogrifier();
          EXIDecoder decoder = new EXIDecoder();
          Scanner scanner;
          
          encoder.setAlignmentType(alignment);
          decoder.setAlignmentType(alignment);
    
          encoder.setPreserveLexicalValues(preserveLexicalValues);
          decoder.setPreserveLexicalValues(preserveLexicalValues);
          
          encoder.setGrammarCache(grammarCache);
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          encoder.setOutputStream(baos);
          
          byte[] bts;
          int n_events;
          
          encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
          
          bts = baos.toByteArray();
          
          decoder.setGrammarCache(grammarCache);
          decoder.setInputStream(new ByteArrayInputStream(bts));
          scanner = decoder.processHeader();
          
          EventDescription exiEvent;
          n_events = 0;
      
          EventType eventType;
      
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SD, eventType.itemType);
          ++n_events;
          
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("A", eventType.name);
          Assert.assertEquals("urn:foo", eventType.uri);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
          Assert.assertEquals("foo", exiEvent.getPrefix());
          Assert.assertEquals("urn:foo", exiEvent.getURI());
          Assert.assertTrue(((EXIEventNS)exiEvent).getLocalElementNs());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
          Assert.assertEquals("xsi", exiEvent.getPrefix());
          Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
          Assert.assertFalse(((EXIEventNS)exiEvent).getLocalElementNs());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_TP, exiEvent.getEventKind());
          Assert.assertEquals("dateDerived", ((EXIEventSchemaType)exiEvent).getTypeName());
          Assert.assertEquals("urn:foo", ((EXIEventSchemaType)exiEvent).getTypeURI());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals(values[i], exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            int tp = scanner.currentState.contentDatatype; 
            int builtinType = corpus.getBaseTypeOfSimpleType(tp);
            Assert.assertEquals(EXISchemaConst.DATE_TYPE, corpus.getSerialOfType(builtinType));
          }
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
          ++n_events;

          Assert.assertEquals(8, n_events);
        }
      }
    }
  }
  
  /**
   * A valid time value matching ITEM_SCHEMA_CH where the associated
   * datatype is an enumeration of xsd:time.
   */
  public void testTime() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/enumeration.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));
    
    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\r 12:20:00-06:00\n",
        "14:22:00-04:00",
        "18:24:00Z"
    };
    final String[] parsedOriginalValues = {
        " \t\n 12:20:00-06:00\n",
        "14:22:00-04:00",
        "18:24:00Z"
    };
    final String[] resultValues = {
        "13:20:00-05:00",
        "13:22:00-05:00",
        "13:24:00-05:00"
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " +
        "xsi:type='foo:timeDerived' >" + originalValues[i] + "</foo:A>\n";
    };
    
    for (AlignmentType alignment : Alignments) {
      for (boolean preserveLexicalValues : new boolean[] { true, false }) {
        String[] values = preserveLexicalValues ? parsedOriginalValues : resultValues;
        for (i = 0; i < xmlStrings.length; i++) {
          Transmogrifier encoder = new Transmogrifier();
          EXIDecoder decoder = new EXIDecoder();
          Scanner scanner;
          
          encoder.setAlignmentType(alignment);
          decoder.setAlignmentType(alignment);
    
          encoder.setPreserveLexicalValues(preserveLexicalValues);
          decoder.setPreserveLexicalValues(preserveLexicalValues);
          
          encoder.setGrammarCache(grammarCache);
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          encoder.setOutputStream(baos);
          
          byte[] bts;
          int n_events;
          
          encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
          
          bts = baos.toByteArray();
          
          decoder.setGrammarCache(grammarCache);
          decoder.setInputStream(new ByteArrayInputStream(bts));
          scanner = decoder.processHeader();
          
          EventDescription exiEvent;
          n_events = 0;
      
          EventType eventType;
      
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SD, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("A", eventType.name);
          Assert.assertEquals("urn:foo", eventType.uri);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
          Assert.assertEquals("foo", exiEvent.getPrefix());
          Assert.assertEquals("urn:foo", exiEvent.getURI());
          Assert.assertTrue(((EXIEventNS)exiEvent).getLocalElementNs());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
          Assert.assertEquals("xsi", exiEvent.getPrefix());
          Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
          Assert.assertFalse(((EXIEventNS)exiEvent).getLocalElementNs());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_TP, exiEvent.getEventKind());
          Assert.assertEquals("timeDerived", ((EXIEventSchemaType)exiEvent).getTypeName());
          Assert.assertEquals("urn:foo", ((EXIEventSchemaType)exiEvent).getTypeURI());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals(values[i], exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            int tp = scanner.currentState.contentDatatype; 
            int builtinType = corpus.getBaseTypeOfSimpleType(tp);
            Assert.assertEquals(EXISchemaConst.TIME_TYPE, corpus.getSerialOfType(builtinType));
          }
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
          ++n_events;

          Assert.assertEquals(8, n_events);
        }
      }
    }
  }
  
  /**
   * A valid gregorian month value matching ITEM_SCHEMA_CH where the associated
   * datatype is an enumeration of xsd:gYearMonth.
   */
  public void testGYearMonth() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/enumeration.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));
    
    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\r 2003-04-05:00\n",
        "2003-06-05:00",
        "2003-08-05:00"
    };
    final String[] parsedOriginalValues = {
        " \t\n 2003-04-05:00\n",
        "2003-06-05:00",
        "2003-08-05:00"
    };
    final String[] resultValues = {
        "2003-04-05:00",
        "2003-06-05:00",
        "2003-08-05:00"
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " +
        "xsi:type='foo:gYearMonthDerived' >" + originalValues[i] + "</foo:A>\n";
    };
    
    for (AlignmentType alignment : Alignments) {
      for (boolean preserveLexicalValues : new boolean[] { true, false }) {
        String[] values = preserveLexicalValues ? parsedOriginalValues : resultValues;
        for (i = 0; i < xmlStrings.length; i++) {
          Transmogrifier encoder = new Transmogrifier();
          EXIDecoder decoder = new EXIDecoder();
          Scanner scanner;
          
          encoder.setAlignmentType(alignment);
          decoder.setAlignmentType(alignment);
    
          encoder.setPreserveLexicalValues(preserveLexicalValues);
          decoder.setPreserveLexicalValues(preserveLexicalValues);
          
          encoder.setGrammarCache(grammarCache);
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          encoder.setOutputStream(baos);
          
          byte[] bts;
          int n_events;
          
          encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
          
          bts = baos.toByteArray();
          
          decoder.setGrammarCache(grammarCache);
          decoder.setInputStream(new ByteArrayInputStream(bts));
          scanner = decoder.processHeader();
          
          EventDescription exiEvent;
          n_events = 0;
      
          EventType eventType;
      
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SD, eventType.itemType);
          ++n_events;
          
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("A", eventType.name);
          Assert.assertEquals("urn:foo", eventType.uri);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
          Assert.assertEquals("foo", exiEvent.getPrefix());
          Assert.assertEquals("urn:foo", exiEvent.getURI());
          Assert.assertTrue(((EXIEventNS)exiEvent).getLocalElementNs());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
          Assert.assertEquals("xsi", exiEvent.getPrefix());
          Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
          Assert.assertFalse(((EXIEventNS)exiEvent).getLocalElementNs());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_TP, exiEvent.getEventKind());
          Assert.assertEquals("gYearMonthDerived", ((EXIEventSchemaType)exiEvent).getTypeName());
          Assert.assertEquals("urn:foo", ((EXIEventSchemaType)exiEvent).getTypeURI());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals(values[i], exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            int tp = scanner.currentState.contentDatatype; 
            int builtinType = corpus.getBaseTypeOfSimpleType(tp);
            Assert.assertEquals(EXISchemaConst.G_YEARMONTH_TYPE, corpus.getSerialOfType(builtinType));
          }
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
          ++n_events;

          Assert.assertEquals(8, n_events);
        }
      }
    }
  }

  /**
   * A valid gregorian year value matching ITEM_SCHEMA_CH where the associated
   * datatype is an enumeration of xsd:gYear.
   */
  public void testGYear() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/enumeration.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));
    
    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\r 1969+09:00\n",
        "1971+09:00",
        "1973+09:00",
        "0001",
        "0012",
        "0123",
        "12345",
    };
    final String[] parsedOriginalValues = {
        " \t\n 1969+09:00\n",
        "1971+09:00",
        "1973+09:00",
        "0001",
        "0012",
        "0123",
        "12345",
    };
    final String[] resultValues = {
        "1969+09:00",
        "1971+09:00",
        "1973+09:00",
        "0001",
        "0012",
        "0123",
        "12345",
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " +
        "xsi:type='foo:gYearDerived' >" + originalValues[i] + "</foo:A>\n";
    };
    
    for (AlignmentType alignment : Alignments) {
      for (boolean preserveLexicalValues : new boolean[] { true, false }) {
        String[] values = preserveLexicalValues ? parsedOriginalValues : resultValues;
        for (i = 0; i < xmlStrings.length; i++) {
          Transmogrifier encoder = new Transmogrifier();
          EXIDecoder decoder = new EXIDecoder();
          Scanner scanner;
          
          encoder.setAlignmentType(alignment);
          decoder.setAlignmentType(alignment);
    
          encoder.setPreserveLexicalValues(preserveLexicalValues);
          decoder.setPreserveLexicalValues(preserveLexicalValues);
          
          encoder.setGrammarCache(grammarCache);
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          encoder.setOutputStream(baos);
          
          byte[] bts;
          int n_events;
          
          encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
          
          bts = baos.toByteArray();
          
          decoder.setGrammarCache(grammarCache);
          decoder.setInputStream(new ByteArrayInputStream(bts));
          scanner = decoder.processHeader();
          
          EventDescription exiEvent;
          n_events = 0;
      
          EventType eventType;
      
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SD, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("A", eventType.name);
          Assert.assertEquals("urn:foo", eventType.uri);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
          Assert.assertEquals("foo", exiEvent.getPrefix());
          Assert.assertEquals("urn:foo", exiEvent.getURI());
          Assert.assertTrue(((EXIEventNS)exiEvent).getLocalElementNs());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
          Assert.assertEquals("xsi", exiEvent.getPrefix());
          Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
          Assert.assertFalse(((EXIEventNS)exiEvent).getLocalElementNs());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_TP, exiEvent.getEventKind());
          Assert.assertEquals("gYearDerived", ((EXIEventSchemaType)exiEvent).getTypeName());
          Assert.assertEquals("urn:foo", ((EXIEventSchemaType)exiEvent).getTypeURI());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals(values[i], exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            int tp = scanner.currentState.contentDatatype; 
            int builtinType = corpus.getBaseTypeOfSimpleType(tp);
            Assert.assertEquals(EXISchemaConst.G_YEAR_TYPE, corpus.getSerialOfType(builtinType));
          }
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
          ++n_events;

          Assert.assertEquals(8, n_events);
        }
      }
    }
  }

  /**
   * A valid gregorian month value matching ITEM_SCHEMA_CH where the associated
   * datatype is an enumeration of xsd:gMonth.
   */
  public void testGMonth() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/enumeration.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));
    
    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\r --07+09:00\n",
        "--09+09:00",
        "--11+09:00"
    };
    final String[] parsedOriginalValues = {
        " \t\n --07+09:00\n",
        "--09+09:00",
        "--11+09:00"
    };
    final String[] resultValues = {
        "--07+09:00",
        "--09+09:00",
        "--11+09:00"
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " +
        "xsi:type='foo:gMonthDerived' >" + originalValues[i] + "</foo:A>\n";
    };
    
    for (AlignmentType alignment : Alignments) {
      for (boolean preserveLexicalValues : new boolean[] { true, false }) {
        String[] values = preserveLexicalValues ? parsedOriginalValues : resultValues;
        for (i = 0; i < xmlStrings.length; i++) {
          Transmogrifier encoder = new Transmogrifier();
          EXIDecoder decoder = new EXIDecoder();
          Scanner scanner;
          
          encoder.setAlignmentType(alignment);
          decoder.setAlignmentType(alignment);
    
          encoder.setPreserveLexicalValues(preserveLexicalValues);
          decoder.setPreserveLexicalValues(preserveLexicalValues);
          
          encoder.setGrammarCache(grammarCache);
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          encoder.setOutputStream(baos);
          
          byte[] bts;
          int n_events;
          
          encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
          
          bts = baos.toByteArray();
          
          decoder.setGrammarCache(grammarCache);
          decoder.setInputStream(new ByteArrayInputStream(bts));
          scanner = decoder.processHeader();
          
          EventDescription exiEvent;
          n_events = 0;
      
          EventType eventType;
      
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SD, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("A", eventType.name);
          Assert.assertEquals("urn:foo", eventType.uri);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
          Assert.assertEquals("foo", exiEvent.getPrefix());
          Assert.assertEquals("urn:foo", exiEvent.getURI());
          Assert.assertTrue(((EXIEventNS)exiEvent).getLocalElementNs());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
          Assert.assertEquals("xsi", exiEvent.getPrefix());
          Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
          Assert.assertFalse(((EXIEventNS)exiEvent).getLocalElementNs());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_TP, exiEvent.getEventKind());
          Assert.assertEquals("gMonthDerived", ((EXIEventSchemaType)exiEvent).getTypeName());
          Assert.assertEquals("urn:foo", ((EXIEventSchemaType)exiEvent).getTypeURI());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals(values[i], exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            int tp = scanner.currentState.contentDatatype; 
            int builtinType = corpus.getBaseTypeOfSimpleType(tp);
            Assert.assertEquals(EXISchemaConst.G_MONTH_TYPE, corpus.getSerialOfType(builtinType));
          }
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
          ++n_events;

          Assert.assertEquals(8, n_events);
        }
      }
    }
  }
  
  /**
   * A valid gregorian date value matching ITEM_SCHEMA_CH where the associated
   * datatype is an enumeration of xsd:gMonthDay.
   */
  public void testGMonthDay() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/enumeration.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\r --09-16+09:00\n",
        "--09-18+09:00",
        "--09-20+09:00",
        "--02-29+14:00",
        "--04-01+14:00",
        "--03-01+14:00",
    };
    final String[] parsedOriginalValues = {
        " \t\n --09-16+09:00\n",
        "--09-18+09:00",
        "--09-20+09:00",
        "--02-29+14:00",
        "--04-01+14:00",
        "--03-01+14:00",
    };
    final String[] resultValues = {
        "--09-16+09:00",
        "--09-18+09:00",
        "--09-20+09:00",
        "--02-28-10:00",
        "--03-31-10:00",
        "--02-29-10:00",
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:GMonthDayDerived xmlns:foo='urn:foo'>" +
        originalValues[i] + "</foo:GMonthDayDerived>\n";
    };
    
    for (AlignmentType alignment : Alignments) {
      for (boolean preserveLexicalValues : new boolean[] { false, true }) {
        String[] values = preserveLexicalValues ? parsedOriginalValues : resultValues;
        for (i = 0; i < xmlStrings.length; i++) {
          Transmogrifier encoder = new Transmogrifier();
          EXIDecoder decoder = new EXIDecoder();
          Scanner scanner;
          
          encoder.setAlignmentType(alignment);
          decoder.setAlignmentType(alignment);
    
          encoder.setPreserveLexicalValues(preserveLexicalValues);
          decoder.setPreserveLexicalValues(preserveLexicalValues);
          
          encoder.setGrammarCache(grammarCache);
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          encoder.setOutputStream(baos);
          
          byte[] bts;
          int n_events;
          
          encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
          
          bts = baos.toByteArray();
          
          decoder.setGrammarCache(grammarCache);
          decoder.setInputStream(new ByteArrayInputStream(bts));
          scanner = decoder.processHeader();
          
          EventDescription exiEvent;
          n_events = 0;
      
          EventType eventType;
      
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SD, eventType.itemType);
          ++n_events;
          
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("GMonthDayDerived", eventType.name);
          Assert.assertEquals("urn:foo", eventType.uri);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals(values[i], exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            int tp = scanner.currentState.contentDatatype; 
            int builtinType = corpus.getBaseTypeOfSimpleType(tp);
            Assert.assertEquals(EXISchemaConst.G_MONTHDAY_TYPE, corpus.getSerialOfType(builtinType));
          }
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
          ++n_events;

          Assert.assertEquals(5, n_events);
        }
      }
    }
  }

  /**
   * A valid gregorian date value matching ITEM_SCHEMA_CH where the associated
   * datatype is an enumeration of xsd:gDay.
   */
  public void testGDay() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/enumeration.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\r ---16+09:00\n",
        "---18+09:00",
        "---20+09:00"
    };
    final String[] parsedOriginalValues = {
        " \t\n ---16+09:00\n",
        "---18+09:00",
        "---20+09:00"
    };
    final String[] resultValues = {
        "---16+09:00",
        "---18+09:00",
        "---20+09:00"
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:GDayDerived xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:GDayDerived>\n";
    };
    
    for (AlignmentType alignment : Alignments) {
      for (boolean preserveLexicalValues : new boolean[] { true, false }) {
        String[] values = preserveLexicalValues ? parsedOriginalValues : resultValues;
        for (i = 0; i < xmlStrings.length; i++) {
          Transmogrifier encoder = new Transmogrifier();
          EXIDecoder decoder = new EXIDecoder();
          Scanner scanner;
          
          encoder.setAlignmentType(alignment);
          decoder.setAlignmentType(alignment);
    
          encoder.setPreserveLexicalValues(preserveLexicalValues);
          decoder.setPreserveLexicalValues(preserveLexicalValues);
          
          encoder.setGrammarCache(grammarCache);
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          encoder.setOutputStream(baos);
          
          byte[] bts;
          int n_events;
          
          encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
          
          bts = baos.toByteArray();
          
          decoder.setGrammarCache(grammarCache);
          decoder.setInputStream(new ByteArrayInputStream(bts));
          scanner = decoder.processHeader();
          
          EventDescription exiEvent;
          n_events = 0;
      
          EventType eventType;
      
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SD, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("GDayDerived", eventType.name);
          Assert.assertEquals("urn:foo", eventType.uri);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals(values[i], exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            int tp = scanner.currentState.contentDatatype; 
            int builtinType = corpus.getBaseTypeOfSimpleType(tp);
            Assert.assertEquals(EXISchemaConst.G_DAY_TYPE, corpus.getSerialOfType(builtinType));
          }
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
          ++n_events;

          Assert.assertEquals(5, n_events);
        }
      }
    }
  }

  /**
   * A valid duration value matching ITEM_SCHEMA_CH where the associated
   * datatype is an enumeration of xsd:duration.
   */
  public void testDuration() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/enumeration.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\r P14M3DT10H30M\n",
        "P1Y2M4DT9H90M",
        "P1Y2M5DT8H150M"
    };
    final String[] parsedOriginalValues = {
        " \t\n P14M3DT10H30M\n",
        "P1Y2M4DT9H90M",
        "P1Y2M5DT8H150M"
    };
    final String[] resultValues = {
        "P1Y2M3DT10H30M",
        "P1Y2M4DT10H30M",
        "P1Y2M5DT10H30M"
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " +
        "xsi:type='foo:durationDerived' >" + originalValues[i] + "</foo:A>\n";
    };
    
    for (AlignmentType alignment : Alignments) {
      for (boolean preserveLexicalValues : new boolean[] { true, false }) {
        String[] values = preserveLexicalValues ? parsedOriginalValues : resultValues;
        for (i = 0; i < xmlStrings.length; i++) {
          Transmogrifier encoder = new Transmogrifier();
          EXIDecoder decoder = new EXIDecoder();
          Scanner scanner;
          
          encoder.setAlignmentType(alignment);
          decoder.setAlignmentType(alignment);
    
          encoder.setPreserveLexicalValues(preserveLexicalValues);
          decoder.setPreserveLexicalValues(preserveLexicalValues);
          
          encoder.setGrammarCache(grammarCache);
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          encoder.setOutputStream(baos);
          
          byte[] bts;
          int n_events;
          
          encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
          
          bts = baos.toByteArray();
          
          decoder.setGrammarCache(grammarCache);
          decoder.setInputStream(new ByteArrayInputStream(bts));
          scanner = decoder.processHeader();
          
          EventDescription exiEvent;
          n_events = 0;
      
          EventType eventType;
      
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SD, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("A", eventType.name);
          Assert.assertEquals("urn:foo", eventType.uri);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
          Assert.assertEquals("foo", exiEvent.getPrefix());
          Assert.assertEquals("urn:foo", exiEvent.getURI());
          Assert.assertTrue(((EXIEventNS)exiEvent).getLocalElementNs());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
          Assert.assertEquals("xsi", exiEvent.getPrefix());
          Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
          Assert.assertFalse(((EXIEventNS)exiEvent).getLocalElementNs());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_TP, exiEvent.getEventKind());
          Assert.assertEquals("durationDerived", ((EXIEventSchemaType)exiEvent).getTypeName());
          Assert.assertEquals("urn:foo", ((EXIEventSchemaType)exiEvent).getTypeURI());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals(values[i], exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            int tp = scanner.currentState.contentDatatype; 
            int builtinType = corpus.getBaseTypeOfSimpleType(tp);
            Assert.assertEquals(EXISchemaConst.DURATION_TYPE, corpus.getSerialOfType(builtinType));
          }
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
          ++n_events;

          Assert.assertEquals(8, n_events);
        }
      }
    }
  }

  /**
   * A valid decimal value matching ITEM_SCHEMA_CH where the associated
   * datatype is an enumeration of xsd:decimal.
   */
  public void testDecimal() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/enumeration.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));
    
    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\r 100.1234567\n",
        "101.2345678",
        "102.3456789",
        "-0",
        "103.abcdefg"
    };
    final String[] parsedOriginalValues = {
        " \t\n 100.1234567\n",
        "101.2345678",
        "102.3456789",
        "-0",
        "103.abcdefg"
    };
    final String[] resultValues = {
        "100.1234567",
        "101.2345678",
        "102.3456789",
        "0",
        "103.abcdefg"
    };
    final int n_validDecimals = 4;

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " +
        "xsi:type='foo:decimalDerived' >" + originalValues[i] + "</foo:A>\n";
    };
    
    for (AlignmentType alignment : Alignments) {
      for (boolean preserveLexicalValues : new boolean[] { true, false }) {
        String[] values = preserveLexicalValues ? parsedOriginalValues : resultValues;
        for (i = 0; i < xmlStrings.length; i++) {
          Transmogrifier encoder = new Transmogrifier();
          EXIDecoder decoder = new EXIDecoder();
          Scanner scanner;
          
          encoder.setAlignmentType(alignment);
          decoder.setAlignmentType(alignment);
    
          encoder.setPreserveLexicalValues(preserveLexicalValues);
          decoder.setPreserveLexicalValues(preserveLexicalValues);
          
          encoder.setGrammarCache(grammarCache);
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          encoder.setOutputStream(baos);
          
          byte[] bts;
          int n_events;
          
          encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
          
          bts = baos.toByteArray();
          
          decoder.setGrammarCache(grammarCache);
          decoder.setInputStream(new ByteArrayInputStream(bts));
          scanner = decoder.processHeader();
          
          EventDescription exiEvent;
          n_events = 0;
      
          EventType eventType;
      
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SD, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("A", eventType.name);
          Assert.assertEquals("urn:foo", eventType.uri);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
          Assert.assertEquals("foo", exiEvent.getPrefix());
          Assert.assertEquals("urn:foo", exiEvent.getURI());
          Assert.assertTrue(((EXIEventNS)exiEvent).getLocalElementNs());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
          Assert.assertEquals("xsi", exiEvent.getPrefix());
          Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
          Assert.assertFalse(((EXIEventNS)exiEvent).getLocalElementNs());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_TP, exiEvent.getEventKind());
          Assert.assertEquals("decimalDerived", ((EXIEventSchemaType)exiEvent).getTypeName());
          Assert.assertEquals("urn:foo", ((EXIEventSchemaType)exiEvent).getTypeURI());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          final String stringValue = exiEvent.getCharacters().makeString();
          Assert.assertEquals(values[i], stringValue);
          eventType = exiEvent.getEventType();
          Assert.assertEquals(i < n_validDecimals || preserveLexicalValues ? 
              EventType.ITEM_SCHEMA_CH : EventType.ITEM_CH, eventType.itemType); 
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            int tp = scanner.currentState.contentDatatype; 
            int builtinType = corpus.getBaseTypeOfSimpleType(tp);
            Assert.assertEquals(EXISchemaConst.DECIMAL_TYPE, corpus.getSerialOfType(builtinType));
          }
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
          ++n_events;

          Assert.assertEquals(8, n_events);
        }
      }
    }
  }

  /**
   * A valid integer value matching ITEM_SCHEMA_CH where the associated
   * datatype is an enumeration of xsd:integer.
   */
  public void testInteger() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/enumeration.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\r 9223372036854775807\n",
        "-9223372036854775808",
        "98765432109876543210",
        "987654321098765432",
        "-987654321098765432",
        "+115",
        "----------",
        "ABCDE", // not even a decimal
        "12345.67" // is a decimal, but not an integer
    };
    final String[] parsedOriginalValues = {
        " \t\n 9223372036854775807\n",
        "-9223372036854775808",
        "98765432109876543210",
        "987654321098765432",
        "-987654321098765432",
        "+115",
        "----------",
        "ABCDE",
        "12345.67"
    };
    final String[] resultValues = {
        "9223372036854775807",
        "-9223372036854775808",
        "98765432109876543210",
        "987654321098765432",
        "-987654321098765432",
        "115",
        "----------",
        "ABCDE",
        "12345.67"
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:IntegerDerived xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:IntegerDerived>\n";
    };
    
    for (AlignmentType alignment : Alignments) {
      for (boolean preserveLexicalValues : new boolean[] { true, false }) {
        String[] values = preserveLexicalValues ? parsedOriginalValues : resultValues;
        boolean isValidValue = true;
        for (i = 0; i < xmlStrings.length; i++) {
          if (xmlStrings[i].contains("----------")) {
            isValidValue = false;
            continue;
          }
          
          Transmogrifier encoder = new Transmogrifier();
          EXIDecoder decoder = new EXIDecoder();
          Scanner scanner;
          
          encoder.setAlignmentType(alignment);
          decoder.setAlignmentType(alignment);
    
          encoder.setPreserveLexicalValues(preserveLexicalValues);
          decoder.setPreserveLexicalValues(preserveLexicalValues);
          
          encoder.setGrammarCache(grammarCache);
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          encoder.setOutputStream(baos);
          
          byte[] bts;
          int n_events;
          
          try {
            encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
          }
          catch (TransmogrifierException te) {
            Assert.assertTrue(!preserveLexicalValues && !isValidValue);
            continue;
          }
          Assert.assertTrue(preserveLexicalValues || isValidValue);
          
          bts = baos.toByteArray();
          
          decoder.setGrammarCache(grammarCache);
          decoder.setInputStream(new ByteArrayInputStream(bts));
          scanner = decoder.processHeader();
          
          EventDescription exiEvent;
          n_events = 0;
      
          EventType eventType;
      
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SD, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("IntegerDerived", eventType.name);
          Assert.assertEquals("urn:foo", eventType.uri);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals(values[i], exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            int tp = scanner.currentState.contentDatatype; 
            int builtinType = corpus.getBaseTypeOfSimpleType(tp);
            Assert.assertEquals(EXISchemaConst.INTEGER_TYPE, corpus.getSerialOfType(builtinType));
          }
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
          ++n_events;

          Assert.assertEquals(5, n_events);
        }
      }
    }
  }

  /**
   * A valid long value matching ITEM_SCHEMA_CH where the associated
   * datatype is an enumeration of xsd:long.
   */
  public void testLong() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/enumeration.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));
    
    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\r 112\n",
        "113",
        "114"
    };
    final String[] parsedOriginalValues = {
        " \t\n 112\n",
        "113",
        "114"
    };
    final String[] resultValues = {
        "112",
        "113",
        "114"
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " +
        "xsi:type='foo:longDerived' >" + originalValues[i] + "</foo:A>\n";
    };
    
    for (AlignmentType alignment : Alignments) {
      for (boolean preserveLexicalValues : new boolean[] { true, false }) {
        String[] values = preserveLexicalValues ? parsedOriginalValues : resultValues;
        for (i = 0; i < xmlStrings.length; i++) {
          Transmogrifier encoder = new Transmogrifier();
          EXIDecoder decoder = new EXIDecoder();
          Scanner scanner;
          
          encoder.setAlignmentType(alignment);
          decoder.setAlignmentType(alignment);
    
          encoder.setPreserveLexicalValues(preserveLexicalValues);
          decoder.setPreserveLexicalValues(preserveLexicalValues);
          
          encoder.setGrammarCache(grammarCache);
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          encoder.setOutputStream(baos);
          
          byte[] bts;
          int n_events;
          
          encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
          
          bts = baos.toByteArray();
          
          decoder.setGrammarCache(grammarCache);
          decoder.setInputStream(new ByteArrayInputStream(bts));
          scanner = decoder.processHeader();
          
          EventDescription exiEvent;
          n_events = 0;
      
          EventType eventType;
      
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SD, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("A", eventType.name);
          Assert.assertEquals("urn:foo", eventType.uri);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
          Assert.assertEquals("foo", exiEvent.getPrefix());
          Assert.assertEquals("urn:foo", exiEvent.getURI());
          Assert.assertTrue(((EXIEventNS)exiEvent).getLocalElementNs());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
          Assert.assertEquals("xsi", exiEvent.getPrefix());
          Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
          Assert.assertFalse(((EXIEventNS)exiEvent).getLocalElementNs());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_TP, exiEvent.getEventKind());
          Assert.assertEquals("longDerived", ((EXIEventSchemaType)exiEvent).getTypeName());
          Assert.assertEquals("urn:foo", ((EXIEventSchemaType)exiEvent).getTypeURI());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals(values[i], exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            int tp = scanner.currentState.contentDatatype; 
            int builtinType = corpus.getBaseTypeOfSimpleType(tp);
            Assert.assertEquals(EXISchemaConst.LONG_TYPE, corpus.getSerialOfType(builtinType));
          }
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
          ++n_events;

          Assert.assertEquals(8, n_events);
        }
      }
    }
  }

  /**
   * A valid int value matching ITEM_SCHEMA_CH where the associated
   * datatype is an enumeration of xsd:int.
   */
  public void testInt() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/enumeration.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));
    
    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\r 109\n",
        "110",
        "111"
    };
    final String[] parsedOriginalValues = {
        " \t\n 109\n",
        "110",
        "111"
    };
    final String[] resultValues = {
        "109",
        "110",
        "111"
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " +
        "xsi:type='foo:intDerived' >" + originalValues[i] + "</foo:A>\n";
    };
    
    for (AlignmentType alignment : Alignments) {
      for (boolean preserveLexicalValues : new boolean[] { true, false }) {
        String[] values = preserveLexicalValues ? parsedOriginalValues : resultValues;
        for (i = 0; i < xmlStrings.length; i++) {
          Transmogrifier encoder = new Transmogrifier();
          EXIDecoder decoder = new EXIDecoder();
          Scanner scanner;
          
          encoder.setAlignmentType(alignment);
          decoder.setAlignmentType(alignment);
    
          encoder.setPreserveLexicalValues(preserveLexicalValues);
          decoder.setPreserveLexicalValues(preserveLexicalValues);
          
          encoder.setGrammarCache(grammarCache);
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          encoder.setOutputStream(baos);
          
          byte[] bts;
          int n_events;
          
          encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
          
          bts = baos.toByteArray();
          
          decoder.setGrammarCache(grammarCache);
          decoder.setInputStream(new ByteArrayInputStream(bts));
          scanner = decoder.processHeader();
          
          EventDescription exiEvent;
          n_events = 0;
      
          EventType eventType;
      
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SD, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("A", eventType.name);
          Assert.assertEquals("urn:foo", eventType.uri);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
          Assert.assertEquals("foo", exiEvent.getPrefix());
          Assert.assertEquals("urn:foo", exiEvent.getURI());
          Assert.assertTrue(((EXIEventNS)exiEvent).getLocalElementNs());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
          Assert.assertEquals("xsi", exiEvent.getPrefix());
          Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
          Assert.assertFalse(((EXIEventNS)exiEvent).getLocalElementNs());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_TP, exiEvent.getEventKind());
          Assert.assertEquals("intDerived", ((EXIEventSchemaType)exiEvent).getTypeName());
          Assert.assertEquals("urn:foo", ((EXIEventSchemaType)exiEvent).getTypeURI());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals(values[i], exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            int tp = scanner.currentState.contentDatatype; 
            int builtinType = corpus.getBaseTypeOfSimpleType(tp);
            Assert.assertEquals(EXISchemaConst.INT_TYPE, corpus.getSerialOfType(builtinType));
          }
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
          ++n_events;

          Assert.assertEquals(8, n_events);
        }
      }
    }
  }

  /**
   * A valid byte value matching ITEM_SCHEMA_CH where the associated
   * datatype is an enumeration of xsd:byte.
   */
  public void testByte() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/enumeration.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));
    
    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\r 126\n",
        "127",
        "-128"
    };
    final String[] parsedOriginalValues = {
        " \t\n 126\n",
        "127",
        "-128"
    };
    final String[] resultValues = {
        "126",
        "127",
        "-128"
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " +
        "xsi:type='foo:byteDerived' >" + originalValues[i] + "</foo:A>\n";
    };
    
    for (AlignmentType alignment : Alignments) {
      for (boolean preserveLexicalValues : new boolean[] { true, false }) {
        String[] values = preserveLexicalValues ? parsedOriginalValues : resultValues;
        for (i = 0; i < xmlStrings.length; i++) {
          Transmogrifier encoder = new Transmogrifier();
          EXIDecoder decoder = new EXIDecoder();
          Scanner scanner;
          
          encoder.setAlignmentType(alignment);
          decoder.setAlignmentType(alignment);
    
          encoder.setPreserveLexicalValues(preserveLexicalValues);
          decoder.setPreserveLexicalValues(preserveLexicalValues);
          
          encoder.setGrammarCache(grammarCache);
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          encoder.setOutputStream(baos);
          
          byte[] bts;
          int n_events;
          
          encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
          
          bts = baos.toByteArray();
          
          decoder.setGrammarCache(grammarCache);
          decoder.setInputStream(new ByteArrayInputStream(bts));
          scanner = decoder.processHeader();
          
          EventDescription exiEvent;
          n_events = 0;
      
          EventType eventType;
      
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SD, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("A", eventType.name);
          Assert.assertEquals("urn:foo", eventType.uri);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
          Assert.assertEquals("foo", exiEvent.getPrefix());
          Assert.assertEquals("urn:foo", exiEvent.getURI());
          Assert.assertTrue(((EXIEventNS)exiEvent).getLocalElementNs());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
          Assert.assertEquals("xsi", exiEvent.getPrefix());
          Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
          Assert.assertFalse(((EXIEventNS)exiEvent).getLocalElementNs());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_TP, exiEvent.getEventKind());
          Assert.assertEquals("byteDerived", ((EXIEventSchemaType)exiEvent).getTypeName());
          Assert.assertEquals("urn:foo", ((EXIEventSchemaType)exiEvent).getTypeURI());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals(values[i], exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            int tp = scanner.currentState.contentDatatype; 
            int builtinType = corpus.getBaseTypeOfSimpleType(tp);
            Assert.assertEquals(EXISchemaConst.BYTE_TYPE, corpus.getSerialOfType(builtinType));
          }
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
          ++n_events;

          Assert.assertEquals(8, n_events);
        }
      }
    }
  }

  /**
   * A valid float value matching ITEM_SCHEMA_CH where the associated
   * datatype is an enumeration of xsd:float.
   */
  public void testFloat() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/enumeration.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\r 1.0301E2\n",
        "10501E-2",
        "107.01"
    };
    final String[] parsedOriginalValues = {
        " \t\n 1.0301E2\n",
        "10501E-2",
        "107.01"
    };
    final String[] resultValues = {
        "10301E-2",
        "10501E-2",
        "10701E-2"
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " +
        "xsi:type='foo:floatDerived' >" + originalValues[i] + "</foo:A>\n";
    };
    
    for (AlignmentType alignment : Alignments) {
      for (boolean preserveLexicalValues : new boolean[] { true, false }) {
        String[] values = preserveLexicalValues ? parsedOriginalValues : resultValues;
        for (i = 0; i < xmlStrings.length; i++) {
          Transmogrifier encoder = new Transmogrifier();
          EXIDecoder decoder = new EXIDecoder();
          Scanner scanner;
          
          encoder.setAlignmentType(alignment);
          decoder.setAlignmentType(alignment);
    
          encoder.setPreserveLexicalValues(preserveLexicalValues);
          decoder.setPreserveLexicalValues(preserveLexicalValues);
          
          encoder.setGrammarCache(grammarCache);
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          encoder.setOutputStream(baos);
          
          byte[] bts;
          int n_events;
          
          encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
          
          bts = baos.toByteArray();
          
          decoder.setGrammarCache(grammarCache);
          decoder.setInputStream(new ByteArrayInputStream(bts));
          scanner = decoder.processHeader();
          
          EventDescription exiEvent;
          n_events = 0;
      
          EventType eventType;
      
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SD, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("A", eventType.name);
          Assert.assertEquals("urn:foo", eventType.uri);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
          Assert.assertEquals("foo", exiEvent.getPrefix());
          Assert.assertEquals("urn:foo", exiEvent.getURI());
          Assert.assertTrue(((EXIEventNS)exiEvent).getLocalElementNs());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
          Assert.assertEquals("xsi", exiEvent.getPrefix());
          Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
          Assert.assertFalse(((EXIEventNS)exiEvent).getLocalElementNs());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_TP, exiEvent.getEventKind());
          Assert.assertEquals("floatDerived", ((EXIEventSchemaType)exiEvent).getTypeName());
          Assert.assertEquals("urn:foo", ((EXIEventSchemaType)exiEvent).getTypeURI());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals(values[i], exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            int tp = scanner.currentState.contentDatatype; 
            int builtinType = corpus.getBaseTypeOfSimpleType(tp);
            Assert.assertEquals(EXISchemaConst.FLOAT_TYPE, corpus.getSerialOfType(builtinType));
          }
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
          ++n_events;

          Assert.assertEquals(8, n_events);
        }
      }
    }
  }

  /**
   * A valid double value matching ITEM_SCHEMA_CH where the associated
   * datatype is an enumeration of xsd:double.
   */
  public void testDouble() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/enumeration.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\r -1E4\n",
        "1267.43233E12",
        "12.78e-2",
        "12",
        "0",
        "-0",
        "INF",
        "-INF",
        "NaN"
    };
    final String[] parsedOriginalValues = {
        " \t\n -1E4\n",
        "1267.43233E12",
        "12.78e-2",
        "12",
        "0",
        "-0",
        "INF",
        "-INF",
        "NaN"
    };
    final double[] resultValues = {
        Double.parseDouble("-1E4"),
        Double.parseDouble("1267.43233E12"),
        Double.parseDouble("12.78e-2"),
        Double.parseDouble("12"),
        Double.parseDouble("0"),
        Double.parseDouble("-0"),
        Double.POSITIVE_INFINITY,
        Double.NEGATIVE_INFINITY,
        Double.NaN
    };

    xmlStrings = new String[originalValues.length];
    for (int i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:DoubleDerived xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:DoubleDerived>\n";
    };
    
    for (AlignmentType alignment : Alignments) {
      for (boolean preserveLexicalValues : new boolean[] { true, false }) {
        for (int i = 0; i < xmlStrings.length; i++) {
          Transmogrifier encoder = new Transmogrifier();
          EXIDecoder decoder = new EXIDecoder();
          Scanner scanner;
          
          encoder.setAlignmentType(alignment);
          decoder.setAlignmentType(alignment);
    
          encoder.setPreserveLexicalValues(preserveLexicalValues);
          decoder.setPreserveLexicalValues(preserveLexicalValues);
          
          encoder.setGrammarCache(grammarCache);
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          encoder.setOutputStream(baos);
          
          byte[] bts;
          int n_events;
          
          encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
          
          bts = baos.toByteArray();
          
          decoder.setGrammarCache(grammarCache);
          decoder.setInputStream(new ByteArrayInputStream(bts));
          scanner = decoder.processHeader();
          
          EventDescription exiEvent;
          n_events = 0;
      
          EventType eventType;
      
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SD, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("DoubleDerived", eventType.name);
          Assert.assertEquals("urn:foo", eventType.uri);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          if (preserveLexicalValues) {
            Assert.assertEquals(parsedOriginalValues[i], exiEvent.getCharacters().makeString());
          }
          else {
            double expectedValue = resultValues[i];
            if (Double.isNaN(expectedValue))
              Assert.assertEquals("NaN", exiEvent.getCharacters().makeString());
            else if (Double.isInfinite(expectedValue))
              Assert.assertEquals(expectedValue > 0 ? "INF" : "-INF", exiEvent.getCharacters().makeString());
            else
              Assert.assertEquals(expectedValue, Double.parseDouble(exiEvent.getCharacters().makeString()), 0.0);
          }
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            int tp = scanner.currentState.contentDatatype; 
            int builtinType = corpus.getBaseTypeOfSimpleType(tp);
            Assert.assertEquals(EXISchemaConst.DOUBLE_TYPE, corpus.getSerialOfType(builtinType));
          }
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
          ++n_events;

          Assert.assertEquals(5, n_events);
        }
      }
    }
  }

  /**
   * A valid QName value matching ITEM_SCHEMA_CH where the associated
   * datatype is an enumeration of xsd:QName.
   */
  public void testQName() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/enumeration.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\r foo:A\n",
        "goo:A",
    };
    final String[] parsedOriginalValues = {
        " \t\n foo:A\n",
        "goo:A",
    };
    final String[] resultValues = {
        " \t\n foo:A\n",
        "goo:A",
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " +
        "xsi:type='foo:qNameDerived' >" + originalValues[i] + "</foo:A>\n";
    };
    
    for (AlignmentType alignment : Alignments) {
      for (boolean preserveLexicalValues : new boolean[] { true, false }) {
        String[] values = preserveLexicalValues ? parsedOriginalValues : resultValues;
        for (i = 0; i < xmlStrings.length; i++) {
          Transmogrifier encoder = new Transmogrifier();
          EXIDecoder decoder = new EXIDecoder();
          Scanner scanner;
          
          encoder.setAlignmentType(alignment);
          decoder.setAlignmentType(alignment);
    
          encoder.setPreserveLexicalValues(preserveLexicalValues);
          decoder.setPreserveLexicalValues(preserveLexicalValues);
          
          encoder.setGrammarCache(grammarCache);
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          encoder.setOutputStream(baos);
          
          byte[] bts;
          int n_events;
          
          encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
          
          bts = baos.toByteArray();
          
          decoder.setGrammarCache(grammarCache);
          decoder.setInputStream(new ByteArrayInputStream(bts));
          scanner = decoder.processHeader();
          
          EventDescription exiEvent;
          n_events = 0;
      
          EventType eventType;
      
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SD, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("A", eventType.name);
          Assert.assertEquals("urn:foo", eventType.uri);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
          Assert.assertEquals("foo", exiEvent.getPrefix());
          Assert.assertEquals("urn:foo", exiEvent.getURI());
          Assert.assertTrue(((EXIEventNS)exiEvent).getLocalElementNs());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
          Assert.assertEquals("xsi", exiEvent.getPrefix());
          Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
          Assert.assertFalse(((EXIEventNS)exiEvent).getLocalElementNs());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_TP, exiEvent.getEventKind());
          Assert.assertEquals("qNameDerived", ((EXIEventSchemaType)exiEvent).getTypeName());
          Assert.assertEquals("urn:foo", ((EXIEventSchemaType)exiEvent).getTypeURI());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals(values[i], exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            int tp = scanner.currentState.contentDatatype; 
            int builtinType = corpus.getBaseTypeOfSimpleType(tp);
            Assert.assertEquals(EXISchemaConst.QNAME_TYPE, corpus.getSerialOfType(builtinType));
          }
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
          ++n_events;

          Assert.assertEquals(8, n_events);
        }
      }
    }
  }

  /**
   * A valid Notation value matching ITEM_SCHEMA_CH where the associated
   * datatype is an enumeration of xsd:Notation.
   */
  public void testNotation() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/enumeration.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\r foo:cat\n",
        "foo:dog",
        "foo:pig"
    };
    final String[] parsedOriginalValues = {
        " \t\n foo:cat\n",
        "foo:dog",
        "foo:pig"
    };
    final String[] resultValues = {
        " \t\n foo:cat\n",
        "foo:dog",
        "foo:pig"
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " +
        "xsi:type='foo:notationDerived' >" + originalValues[i] + "</foo:A>\n";
    };
    
    for (AlignmentType alignment : Alignments) {
      for (boolean preserveLexicalValues : new boolean[] { true, false }) {
        String[] values = preserveLexicalValues ? parsedOriginalValues : resultValues;
        for (i = 0; i < xmlStrings.length; i++) {
          Transmogrifier encoder = new Transmogrifier();
          EXIDecoder decoder = new EXIDecoder();
          Scanner scanner;
          
          encoder.setAlignmentType(alignment);
          decoder.setAlignmentType(alignment);
    
          encoder.setPreserveLexicalValues(preserveLexicalValues);
          decoder.setPreserveLexicalValues(preserveLexicalValues);
          
          encoder.setGrammarCache(grammarCache);
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          encoder.setOutputStream(baos);
          
          byte[] bts;
          int n_events;
          
          encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
          
          bts = baos.toByteArray();
          
          decoder.setGrammarCache(grammarCache);
          decoder.setInputStream(new ByteArrayInputStream(bts));
          scanner = decoder.processHeader();
          
          EventDescription exiEvent;
          n_events = 0;
      
          EventType eventType;
      
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SD, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("A", eventType.name);
          Assert.assertEquals("urn:foo", eventType.uri);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
          Assert.assertEquals("foo", exiEvent.getPrefix());
          Assert.assertEquals("urn:foo", exiEvent.getURI());
          Assert.assertTrue(((EXIEventNS)exiEvent).getLocalElementNs());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
          Assert.assertEquals("xsi", exiEvent.getPrefix());
          Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
          Assert.assertFalse(((EXIEventNS)exiEvent).getLocalElementNs());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_TP, exiEvent.getEventKind());
          Assert.assertEquals("notationDerived", ((EXIEventSchemaType)exiEvent).getTypeName());
          Assert.assertEquals("urn:foo", ((EXIEventSchemaType)exiEvent).getTypeURI());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals(values[i], exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            int tp = scanner.currentState.contentDatatype; 
            int builtinType = corpus.getBaseTypeOfSimpleType(tp);
            Assert.assertEquals(EXISchemaConst.NOTATION_TYPE, corpus.getSerialOfType(builtinType));
          }
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
          ++n_events;

          Assert.assertEquals(8, n_events);
        }
      }
    }
  }

  /**
   * A valid union-typed value matching ITEM_SCHEMA_CH where the associated
   * datatype is an enumeration of unioned-type.
   */
  public void testUnion() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/enumeration.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\r 100\n",
        "Tokyo",
        "101"
    };
    final String[] parsedOriginalValues = {
        " \t\n 100\n",
        "Tokyo",
        "101"
    };
    final String[] resultValues = {
        " \t\n 100\n",
        "Tokyo",
        "101"
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " +
        "xsi:type='foo:unionedEnum' >" + originalValues[i] + "</foo:A>\n";
    };
    
    for (AlignmentType alignment : Alignments) {
      for (boolean preserveLexicalValues : new boolean[] { true, false }) {
        String[] values = preserveLexicalValues ? parsedOriginalValues : resultValues;
        for (i = 0; i < xmlStrings.length; i++) {
          Transmogrifier encoder = new Transmogrifier();
          EXIDecoder decoder = new EXIDecoder();
          Scanner scanner;
          
          encoder.setAlignmentType(alignment);
          decoder.setAlignmentType(alignment);
    
          encoder.setPreserveLexicalValues(preserveLexicalValues);
          decoder.setPreserveLexicalValues(preserveLexicalValues);
          
          encoder.setGrammarCache(grammarCache);
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          encoder.setOutputStream(baos);
          
          byte[] bts;
          int n_events;
          
          encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
          
          bts = baos.toByteArray();
          
          decoder.setGrammarCache(grammarCache);
          decoder.setInputStream(new ByteArrayInputStream(bts));
          scanner = decoder.processHeader();
          
          EventDescription exiEvent;
          n_events = 0;
      
          EventType eventType;
      
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SD, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("A", eventType.name);
          Assert.assertEquals("urn:foo", eventType.uri);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
          Assert.assertEquals("foo", exiEvent.getPrefix());
          Assert.assertEquals("urn:foo", exiEvent.getURI());
          Assert.assertTrue(((EXIEventNS)exiEvent).getLocalElementNs());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
          Assert.assertEquals("xsi", exiEvent.getPrefix());
          Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
          Assert.assertFalse(((EXIEventNS)exiEvent).getLocalElementNs());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_TP, exiEvent.getEventKind());
          Assert.assertEquals("unionedEnum", ((EXIEventSchemaType)exiEvent).getTypeName());
          Assert.assertEquals("urn:foo", ((EXIEventSchemaType)exiEvent).getTypeURI());
          ++n_events;
          
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals(values[i], exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            int tp = scanner.currentState.contentDatatype; 
            Assert.assertEquals(EXISchema.UNION_SIMPLE_TYPE, corpus.getVarietyOfSimpleType(tp));
          }
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
          ++n_events;

          Assert.assertEquals(8, n_events);
        }
      }
    }
  }

  /**
   * A valid base64Binary value matching ITEM_SCHEMA_CH where the associated
   * datatype is an enumeration of xsd:base64Binary.
   */
  public void testBase64Binary() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/enumeration.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\r YWFhYWE=\n",
        "Y2NjY2M=",
        "ZWVlZWU="
    };
    final String[] parsedOriginalValues = {
        " \t\n YWFhYWE=\n",
        "Y2NjY2M=",
        "ZWVlZWU="
    };
    final String[] resultValues = {
        "YWFhYWE=",
        "Y2NjY2M=",
        "ZWVlZWU="
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " +
        "xsi:type='foo:base64BinaryDerived' >" + originalValues[i] + "</foo:A>\n";
    };
    
    for (AlignmentType alignment : Alignments) {
      for (boolean preserveLexicalValues : new boolean[] { true, false }) {
        String[] values = preserveLexicalValues ? parsedOriginalValues : resultValues;
        for (i = 0; i < xmlStrings.length; i++) {
          Transmogrifier encoder = new Transmogrifier();
          EXIDecoder decoder = new EXIDecoder();
          Scanner scanner;
          
          encoder.setAlignmentType(alignment);
          decoder.setAlignmentType(alignment);
    
          encoder.setPreserveLexicalValues(preserveLexicalValues);
          decoder.setPreserveLexicalValues(preserveLexicalValues);
          
          encoder.setGrammarCache(grammarCache);
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          encoder.setOutputStream(baos);
          
          byte[] bts;
          int n_events;
          
          encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
          
          bts = baos.toByteArray();
          
          decoder.setGrammarCache(grammarCache);
          decoder.setInputStream(new ByteArrayInputStream(bts));
          scanner = decoder.processHeader();
          
          EventDescription exiEvent;
          n_events = 0;
      
          EventType eventType;
      
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SD, eventType.itemType);
          ++n_events;
          
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("A", eventType.name);
          Assert.assertEquals("urn:foo", eventType.uri);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
          Assert.assertEquals("foo", exiEvent.getPrefix());
          Assert.assertEquals("urn:foo", exiEvent.getURI());
          Assert.assertTrue(((EXIEventNS)exiEvent).getLocalElementNs());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
          Assert.assertEquals("xsi", exiEvent.getPrefix());
          Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
          Assert.assertFalse(((EXIEventNS)exiEvent).getLocalElementNs());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_TP, exiEvent.getEventKind());
          Assert.assertEquals("base64BinaryDerived", ((EXIEventSchemaType)exiEvent).getTypeName());
          Assert.assertEquals("urn:foo", ((EXIEventSchemaType)exiEvent).getTypeURI());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals(values[i], exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            int tp = scanner.currentState.contentDatatype; 
            int builtinType = corpus.getBaseTypeOfSimpleType(tp);
            Assert.assertEquals(EXISchemaConst.BASE64BINARY_TYPE, corpus.getSerialOfType(builtinType));
          }
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
          ++n_events;

          Assert.assertEquals(8, n_events);
        }
      }
    }
  }

  /**
   * A valid hexBinary value matching ITEM_SCHEMA_CH where the associated
   * datatype is an enumeration of xsd:hexBinary.
   */
  public void testHexBinary() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/enumeration.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\r 6161616161\n",
        "6363636363",
        "6565656565"
    };
    final String[] parsedOriginalValues = {
        " \t\n 6161616161\n",
        "6363636363",
        "6565656565"
    };
    final String[] resultValues = {
        "6161616161",
        "6363636363",
        "6565656565"
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " +
        "xsi:type='foo:hexBinaryDerived' >" + originalValues[i] + "</foo:A>\n";
    };
    
    for (AlignmentType alignment : Alignments) {
      for (boolean preserveLexicalValues : new boolean[] { true, false }) {
        String[] values = preserveLexicalValues ? parsedOriginalValues : resultValues;
        for (i = 0; i < xmlStrings.length; i++) {
          Transmogrifier encoder = new Transmogrifier();
          EXIDecoder decoder = new EXIDecoder();
          Scanner scanner;
          
          encoder.setAlignmentType(alignment);
          decoder.setAlignmentType(alignment);
    
          encoder.setPreserveLexicalValues(preserveLexicalValues);
          decoder.setPreserveLexicalValues(preserveLexicalValues);
          
          encoder.setGrammarCache(grammarCache);
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          encoder.setOutputStream(baos);
          
          byte[] bts;
          int n_events;
          
          encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
          
          bts = baos.toByteArray();
          
          decoder.setGrammarCache(grammarCache);
          decoder.setInputStream(new ByteArrayInputStream(bts));
          scanner = decoder.processHeader();
          
          EventDescription exiEvent;
          n_events = 0;
      
          EventType eventType;
      
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SD, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("A", eventType.name);
          Assert.assertEquals("urn:foo", eventType.uri);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
          Assert.assertEquals("foo", exiEvent.getPrefix());
          Assert.assertEquals("urn:foo", exiEvent.getURI());
          Assert.assertTrue(((EXIEventNS)exiEvent).getLocalElementNs());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
          Assert.assertEquals("xsi", exiEvent.getPrefix());
          Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
          Assert.assertFalse(((EXIEventNS)exiEvent).getLocalElementNs());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_TP, exiEvent.getEventKind());
          Assert.assertEquals("hexBinaryDerived", ((EXIEventSchemaType)exiEvent).getTypeName());
          Assert.assertEquals("urn:foo", ((EXIEventSchemaType)exiEvent).getTypeURI());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals(values[i], exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            int tp = scanner.currentState.contentDatatype; 
            int builtinType = corpus.getBaseTypeOfSimpleType(tp);
            Assert.assertEquals(EXISchemaConst.HEXBINARY_TYPE, corpus.getSerialOfType(builtinType));
          }
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
          ++n_events;

          Assert.assertEquals(8, n_events);
        }
      }
    }
  }

  /**
   * A valid anyURI value matching ITEM_SCHEMA_CH where the associated
   * datatype is an enumeration of xsd:anyURI.
   */
  public void testAnyURI() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/enumeration.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\r urn:foo\n",
        "urn:goo",
        "urn:hoo"
    };
    final String[] parsedOriginalValues = {
        " \t\n urn:foo\n",
        "urn:goo",
        "urn:hoo"
    };
    final String[] resultValues = {
        "urn:foo",
        "urn:goo",
        "urn:hoo"
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " +
        "xsi:type='foo:anyURIDerived' >" + originalValues[i] + "</foo:A>\n";
    };
    
    for (AlignmentType alignment : Alignments) {
      for (boolean preserveLexicalValues : new boolean[] { true, false }) {
        String[] values = preserveLexicalValues ? parsedOriginalValues : resultValues;
        for (i = 0; i < xmlStrings.length; i++) {
          Transmogrifier encoder = new Transmogrifier();
          EXIDecoder decoder = new EXIDecoder();
          Scanner scanner;
          
          encoder.setAlignmentType(alignment);
          decoder.setAlignmentType(alignment);
    
          encoder.setPreserveLexicalValues(preserveLexicalValues);
          decoder.setPreserveLexicalValues(preserveLexicalValues);
          
          encoder.setGrammarCache(grammarCache);
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          encoder.setOutputStream(baos);
          
          byte[] bts;
          int n_events;
          
          encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
          
          bts = baos.toByteArray();
          
          decoder.setGrammarCache(grammarCache);
          decoder.setInputStream(new ByteArrayInputStream(bts));
          scanner = decoder.processHeader();
          
          EventDescription exiEvent;
          n_events = 0;
      
          EventType eventType;
      
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SD, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("A", eventType.name);
          Assert.assertEquals("urn:foo", eventType.uri);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
          Assert.assertEquals("foo", exiEvent.getPrefix());
          Assert.assertEquals("urn:foo", exiEvent.getURI());
          Assert.assertTrue(((EXIEventNS)exiEvent).getLocalElementNs());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
          Assert.assertEquals("xsi", exiEvent.getPrefix());
          Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
          Assert.assertFalse(((EXIEventNS)exiEvent).getLocalElementNs());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_TP, exiEvent.getEventKind());
          Assert.assertEquals("anyURIDerived", ((EXIEventSchemaType)exiEvent).getTypeName());
          Assert.assertEquals("urn:foo", ((EXIEventSchemaType)exiEvent).getTypeURI());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals(values[i], exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            int tp = scanner.currentState.contentDatatype; 
            int builtinType = corpus.getBaseTypeOfSimpleType(tp);
            Assert.assertEquals(EXISchemaConst.ANYURI_TYPE, corpus.getSerialOfType(builtinType));
          }
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
          ++n_events;

          Assert.assertEquals(8, n_events);
        }
      }
    }
  }

  /**
   * A valid list value matching ITEM_SCHEMA_CH where the associated
   * datatype is an enumeration of a list of xsd:ID.
   */
  public void testlistOfIDs() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/enumeration.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\r AB BC CD\n",
        "EF FG GH",
        "IJ JK KL"
    };
    final String[] parsedOriginalValues = {
        " \t\n AB BC CD\n",
        "EF FG GH",
        "IJ JK KL"
    };
    final String[] resultValues = {
        "AB BC CD",
        "EF FG GH",
        "IJ JK KL"
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " +
        "xsi:type='foo:listOfIDsEnum' >" + originalValues[i] + "</foo:A>\n";
    };
    
    for (AlignmentType alignment : Alignments) {
      for (boolean preserveLexicalValues : new boolean[] { true, false }) {
        String[] values = preserveLexicalValues ? parsedOriginalValues : resultValues;
        for (i = 0; i < xmlStrings.length; i++) {
          Transmogrifier encoder = new Transmogrifier();
          EXIDecoder decoder = new EXIDecoder();
          Scanner scanner;
          
          encoder.setAlignmentType(alignment);
          decoder.setAlignmentType(alignment);
    
          encoder.setPreserveLexicalValues(preserveLexicalValues);
          decoder.setPreserveLexicalValues(preserveLexicalValues);
          
          encoder.setGrammarCache(grammarCache);
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          encoder.setOutputStream(baos);
          
          byte[] bts;
          int n_events;
          
          encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
          
          bts = baos.toByteArray();
          
          decoder.setGrammarCache(grammarCache);
          decoder.setInputStream(new ByteArrayInputStream(bts));
          scanner = decoder.processHeader();
          
          EventDescription exiEvent;
          n_events = 0;
      
          EventType eventType;
      
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SD, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          Assert.assertEquals("A", eventType.name);
          Assert.assertEquals("urn:foo", eventType.uri);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
          Assert.assertEquals("foo", exiEvent.getPrefix());
          Assert.assertEquals("urn:foo", exiEvent.getURI());
          Assert.assertTrue(((EXIEventNS)exiEvent).getLocalElementNs());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_NS, exiEvent.getEventKind());
          Assert.assertEquals("xsi", exiEvent.getPrefix());
          Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.getURI());
          Assert.assertFalse(((EXIEventNS)exiEvent).getLocalElementNs());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_TP, exiEvent.getEventKind());
          Assert.assertEquals("listOfIDsEnum", ((EXIEventSchemaType)exiEvent).getTypeName());
          Assert.assertEquals("urn:foo", ((EXIEventSchemaType)exiEvent).getTypeURI());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals(values[i], exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            int tp = scanner.currentState.contentDatatype; 
            Assert.assertEquals("urn:foo", EXISchemaUtil.getTargetNamespaceNameOfType(tp, corpus));
            Assert.assertEquals("listOfIDsEnum", corpus.getNameOfType(tp));
          }
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_ED, eventType.itemType);
          ++n_events;

          Assert.assertEquals(8, n_events);
        }
      }
    }
  }

}
