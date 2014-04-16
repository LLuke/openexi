package org.openexi.fujitsu.sax;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.ArrayList;

import org.openexi.fujitsu.proc.EXIDecoder;
import org.openexi.fujitsu.proc.common.AlignmentType;
import org.openexi.fujitsu.proc.common.EXIEvent;
import org.openexi.fujitsu.proc.common.EventCode;
import org.openexi.fujitsu.proc.common.EventType;
import org.openexi.fujitsu.proc.common.GrammarOptions;
import org.openexi.fujitsu.proc.grammars.EventTypeSchema;
import org.openexi.fujitsu.proc.grammars.GrammarCache;
import org.openexi.fujitsu.proc.io.Scanner;
import org.openexi.fujitsu.sax.Transmogrifier;
import org.openexi.fujitsu.schema.EXISchema;
import org.openexi.fujitsu.schema.EXISchemaConst;
import org.openexi.fujitsu.scomp.EXISchemaFactoryErrorMonitor;
import org.openexi.fujitsu.scomp.EXISchemaFactoryTestUtil;
import org.xml.sax.InputSource;

import junit.framework.Assert;
import junit.framework.TestCase;

public class DateTimeValueEncodingTest extends TestCase {

  public DateTimeValueEncodingTest(String name) {
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
   * Preserve lexical dateTime values by turning on Preserve.lexicalValues.
   */
  public void testDateTimeRCS() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/dateTime.xsd", getClass(), m_compilerErrors);

    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\r *2003-04-25*T*11:41:30.45+09:00*\n", // '*' will be encoded as an escaped character 
    };
    final String[] parsedOriginalValues = {
        " \t\n *2003-04-25*T*11:41:30.45+09:00*\n", 
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:DateTime xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:DateTime>\n";
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
   * A valid dateTime value matching ITEM_SCHEMA_CH where the associated
   * datatype is xsd:dateTime.
   */
  public void testValidDateTime() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/dateTime.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String[] xmlStrings;
    final String[] originalValues = {
       " \t\r 2003-04-25T11:41:30.45+09:00\n",
       "2003-04-25T11:41:30.45+14:00", 
       "1997-07-16T19:20:30.45-12:00", 
       "1997-07-16T19:20:30.45Z", 
       "1999-12-31T24:00:00",
       "-0601-07-16T19:20:30.45-05:09"
    };
    final String[] parsedOriginalValues = {
        " \t\n 2003-04-25T11:41:30.45+09:00\n",
        "2003-04-25T11:41:30.45+14:00", 
        "1997-07-16T19:20:30.45-12:00", 
        "1997-07-16T19:20:30.45Z", 
        "1999-12-31T24:00:00",
        "-0601-07-16T19:20:30.45-05:09"
    };
    final String[] resultValues = {
        "2003-04-25T11:41:30.45+09:00",
        "2003-04-25T11:41:30.45+14:00", 
        "1997-07-16T19:20:30.45-12:00", 
        "1997-07-16T19:20:30.45Z", 
        "1999-12-31T24:00:00",
        "-0601-07-16T19:20:30.45-05:09"
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:DateTime xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:DateTime>\n";
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

          encoder.setEXISchema(grammarCache);
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          encoder.setOutputStream(baos);
          
          byte[] bts;
          int n_events;
          
          encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
          
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
          Assert.assertEquals("DateTime", eventType.getName());
          Assert.assertEquals("urn:foo", eventType.getURI());
          
          exiEvent = exiEventList.get(2);
          Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
          Assert.assertEquals(values[i], exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
          int tp = ((EventTypeSchema)eventType).getSchemaSubstance();
          int builtinType = corpus.getBuiltinTypeOfAtomicSimpleType(tp);
          Assert.assertEquals(EXISchemaConst.DATETIME_TYPE, corpus.getSerialOfType(builtinType));
      
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
    }
  }

  /**
   * A valid date value matching ITEM_SCHEMA_CH where the associated
   * datatype is xsd:date.
   */
  public void testValidDate() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/dateTime.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\r 2003-04-25+09:00\n",
        "-0601-07-16-05:00",
        "1997-07-16",
        "1997-07-16Z"
    };
    final String[] parsedOriginalValues = {
        " \t\n 2003-04-25+09:00\n",
        "-0601-07-16-05:00",
        "1997-07-16",
        "1997-07-16Z"
    };
    final String[] resultValues = {
        "2003-04-25+09:00",
        "-0601-07-16-05:00",
        "1997-07-16",
        "1997-07-16Z"
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:Date xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:Date>\n";
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
          
          encoder.setEXISchema(grammarCache);
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          encoder.setOutputStream(baos);
          
          byte[] bts;
          int n_events;
          
          encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
          
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
          Assert.assertEquals("Date", eventType.getName());
          Assert.assertEquals("urn:foo", eventType.getURI());
          
          exiEvent = exiEventList.get(2);
          Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
          Assert.assertEquals(values[i], exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
          int tp = ((EventTypeSchema)eventType).getSchemaSubstance();
          int builtinType = corpus.getBuiltinTypeOfAtomicSimpleType(tp);
          Assert.assertEquals(EXISchemaConst.DATE_TYPE, corpus.getSerialOfType(builtinType));
      
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
    }
  }

  /**
   * A valid time value matching ITEM_SCHEMA_CH where the associated
   * datatype is xsd:time.
   */
  public void testValidTime() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/dateTime.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\r 13:20:00+09:00\n", 
        "13:20:30.455-09:45", 
        "13:20:00", 
        "13:20:00Z"
    };
    final String[] parsedOriginalValues = {
        " \t\n 13:20:00+09:00\n", 
        "13:20:30.455-09:45", 
        "13:20:00", 
        "13:20:00Z"
    };
    final String[] resultValues = {
        "13:20:00+09:00", 
        "13:20:30.455-09:45", 
        "13:20:00", 
        "13:20:00Z"
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:Time xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:Time>\n";
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

          encoder.setEXISchema(grammarCache);
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          encoder.setOutputStream(baos);
          
          byte[] bts;
          int n_events;
          
          encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
          
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
          Assert.assertEquals("Time", eventType.getName());
          Assert.assertEquals("urn:foo", eventType.getURI());
          
          exiEvent = exiEventList.get(2);
          Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
          Assert.assertEquals(values[i], exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
          int tp = ((EventTypeSchema)eventType).getSchemaSubstance();
          int builtinType = corpus.getBuiltinTypeOfAtomicSimpleType(tp);
          Assert.assertEquals(EXISchemaConst.TIME_TYPE, corpus.getSerialOfType(builtinType));
      
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
    }
  }

  /**
   * A valid gregorian day value matching ITEM_SCHEMA_CH where the associated
   * datatype is xsd:gDay.
   */
  public void testValidGDay() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/dateTime.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\r ---16\n",
        "---16+09:00",
        "---16Z",
    };
    final String[] parsedOriginalValues = {
        " \t\n ---16\n",
        "---16+09:00",
        "---16Z",
    };
    final String[] resultValues = {
        "---16",
        "---16+09:00",
        "---16Z",
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:GDay xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:GDay>\n";
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
          
          encoder.setEXISchema(grammarCache);
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          encoder.setOutputStream(baos);
          
          byte[] bts;
          int n_events;
          
          encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
          
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
          Assert.assertEquals("GDay", eventType.getName());
          Assert.assertEquals("urn:foo", eventType.getURI());
          
          exiEvent = exiEventList.get(2);
          Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
          Assert.assertEquals(values[i], exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
          int tp = ((EventTypeSchema)eventType).getSchemaSubstance();
          int builtinType = corpus.getBuiltinTypeOfAtomicSimpleType(tp);
          Assert.assertEquals(EXISchemaConst.G_DAY_TYPE, corpus.getSerialOfType(builtinType));
      
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
    }
  }

  /**
   * A valid gregorian month value matching ITEM_SCHEMA_CH where the associated
   * datatype is xsd:gMonth.
   */
  public void testValidGMonth() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/dateTime.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\r --09\n",
        "--09+09:00",
        /**
         * REVISIT: enable this once xerces bug was fixed. (see XERCESJ-1342) 
         * "--09Z", 
         */
    };
    final String[] parsedOriginalValues = {
        " \t\n --09\n",
        "--09+09:00",
        //"--09Z",
    };
    final String[] resultValues = {
        "--09",
        "--09+09:00",
        //"--09Z",
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:GMonth xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:GMonth>\n";
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
          
          encoder.setEXISchema(grammarCache);
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          encoder.setOutputStream(baos);
          
          byte[] bts;
          int n_events;
          
          encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
          
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
          Assert.assertEquals("GMonth", eventType.getName());
          Assert.assertEquals("urn:foo", eventType.getURI());
          
          exiEvent = exiEventList.get(2);
          Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
          Assert.assertEquals(values[i], exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
          int tp = ((EventTypeSchema)eventType).getSchemaSubstance();
          int builtinType = corpus.getBuiltinTypeOfAtomicSimpleType(tp);
          Assert.assertEquals(EXISchemaConst.G_MONTH_TYPE, corpus.getSerialOfType(builtinType));
      
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
    }
  }

  /**
   * A valid gregorian date value matching ITEM_SCHEMA_CH where the associated
   * datatype is xsd:gMonthDay.
   */
  public void testValidGMonthDay() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/dateTime.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\r --09-16\n",
        "--09-16+09:00",
        "--09-16Z",
    };
    final String[] parsedOriginalValues = {
        " \t\n --09-16\n",
        "--09-16+09:00",
        "--09-16Z",
    };
    final String[] resultValues = {
        "--09-16",
        "--09-16+09:00",
        "--09-16Z",
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:GMonthDay xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:GMonthDay>\n";
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
          
          encoder.setEXISchema(grammarCache);
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          encoder.setOutputStream(baos);
          
          byte[] bts;
          int n_events;
          
          encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
          
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
          Assert.assertEquals("GMonthDay", eventType.getName());
          Assert.assertEquals("urn:foo", eventType.getURI());
          
          exiEvent = exiEventList.get(2);
          Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
          Assert.assertEquals(values[i], exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
          int tp = ((EventTypeSchema)eventType).getSchemaSubstance();
          int builtinType = corpus.getBuiltinTypeOfAtomicSimpleType(tp);
          Assert.assertEquals(EXISchemaConst.G_MONTHDAY_TYPE, corpus.getSerialOfType(builtinType));
      
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
    }
  }

  /**
   * A valid gregorian calendar year value matching ITEM_SCHEMA_CH where the associated
   * datatype is xsd:gYear.
   */
  public void testValidGYear() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/dateTime.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\r 1969\n",
        "1969+09:00",
        "1969Z",
    };
    final String[] parsedOriginalValues = {
        " \t\n 1969\n",
        "1969+09:00",
        "1969Z",
    };
    final String[] resultValues = {
        "1969",
        "1969+09:00",
        "1969Z",
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:GYear xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:GYear>\n";
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
          
          encoder.setEXISchema(grammarCache);
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          encoder.setOutputStream(baos);
          
          byte[] bts;
          int n_events;
          
          encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
          
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
          Assert.assertEquals("GYear", eventType.getName());
          Assert.assertEquals("urn:foo", eventType.getURI());
          
          exiEvent = exiEventList.get(2);
          Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
          Assert.assertEquals(values[i], exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
          int tp = ((EventTypeSchema)eventType).getSchemaSubstance();
          int builtinType = corpus.getBuiltinTypeOfAtomicSimpleType(tp);
          Assert.assertEquals(EXISchemaConst.G_YEAR_TYPE, corpus.getSerialOfType(builtinType));
      
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
    }
  }

  /**
   * A valid value representing a specific gregorian month in a specific 
   * gregorian year value matching ITEM_SCHEMA_CH where the associated datatype 
   * is xsd:gYearMonth.
   */
  public void testValidGYearMonth() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/dateTime.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\r 1999-05\n",
        "1999-05+09:00",
        "1999-05Z",
    };
    final String[] parsedOriginalValues = {
        " \t\n 1999-05\n",
        "1999-05+09:00",
        "1999-05Z",
    };
    final String[] resultValues = {
        "1999-05",
        "1999-05+09:00",
        "1999-05Z",
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:GYearMonth xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:GYearMonth>\n";
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
          
          encoder.setEXISchema(grammarCache);
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          encoder.setOutputStream(baos);
          
          byte[] bts;
          int n_events;
          
          encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
          
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
          Assert.assertEquals("GYearMonth", eventType.getName());
          Assert.assertEquals("urn:foo", eventType.getURI());
          
          exiEvent = exiEventList.get(2);
          Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
          Assert.assertEquals(values[i], exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
          int tp = ((EventTypeSchema)eventType).getSchemaSubstance();
          int builtinType = corpus.getBuiltinTypeOfAtomicSimpleType(tp);
          Assert.assertEquals(EXISchemaConst.G_YEARMONTH_TYPE, corpus.getSerialOfType(builtinType));
      
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
    }
  }
  
}
