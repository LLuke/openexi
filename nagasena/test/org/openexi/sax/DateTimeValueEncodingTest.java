package org.openexi.sax;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.ArrayList;

import org.openexi.proc.EXIDecoder;
import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EventDescription;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.proc.io.Scanner;
import org.openexi.schema.EXISchema;
import org.openexi.schema.EXISchemaConst;
import org.openexi.scomp.EXISchemaFactoryErrorMonitor;
import org.openexi.scomp.EXISchemaFactoryTestUtil;
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
    
    encoder.setGrammarCache(grammarCache);
    decoder.setGrammarCache(grammarCache);

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
        
        ArrayList<EventDescription> exiEventList = new ArrayList<EventDescription>();

        EventDescription exiEvent;
        n_events = 0;
        n_texts = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          if (exiEvent.getEventKind() == EventDescription.EVENT_CH) {
            String stringValue = exiEvent.getCharacters().makeString();
            Assert.assertEquals(parsedOriginalValues[i], stringValue);
            Assert.assertEquals(EventType.ITEM_SCHEMA_CH, exiEvent.getEventType().itemType);
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
  public void testDateTime() throws Exception {
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
       "-0601-07-16T19:20:30.45-05:09",
       "1972-06-30T23:59:60", // valid leap second (1972-06-30)
       "2013-06-30T23:59:60", // invalid
       "2009-04-01T12:34:56.0001234",
       "----------",
       "1997-07-16Z", // not a valid xsd:dateTime value
       "xyz", // an absurd value
    };
    final String[] parsedOriginalValues = {
        " \t\n 2003-04-25T11:41:30.45+09:00\n",
        "2003-04-25T11:41:30.45+14:00", 
        "1997-07-16T19:20:30.45-12:00", 
        "1997-07-16T19:20:30.45Z", 
        "1999-12-31T24:00:00",
        "-0601-07-16T19:20:30.45-05:09",
        "1972-06-30T23:59:60",
        "2013-06-30T23:59:60",
        "2009-04-01T12:34:56.0001234",
        "----------",
        "1997-07-16Z",
        "xyz",
    };
    final String[] resultValues = {
        "2003-04-25T11:41:30.45+09:00",
        "2003-04-25T11:41:30.45+14:00", 
        "1997-07-16T19:20:30.45-12:00", 
        "1997-07-16T19:20:30.45Z", 
        "1999-12-31T24:00:00",
        "-0601-07-16T19:20:30.45-05:09",
        "1972-06-30T23:59:60",
        "2013-06-30T23:59:60",
        "2009-04-01T12:34:56.0001234",
        "----------",
        "1997-07-16Z",
        "xyz",
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:DateTime xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:DateTime>\n";
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
          Assert.assertEquals("DateTime", eventType.name);
          Assert.assertEquals("urn:foo", eventType.uri);
          ++n_events;
          
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals(values[i], exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            int tp = scanner.currentState.contentDatatype; 
            Assert.assertEquals(EXISchemaConst.DATETIME_TYPE, corpus.getSerialOfType(tp));
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
   * datatype is xsd:date.
   */
  public void testDate() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/dateTime.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\r 2003-04-25+09:00\n",
        "-0601-07-16-05:00",
        "1997-07-16",
        "1997-07-16Z",
        "2012-02-29", // 02-29 is permitted in leap years
        "----------",
        "1997-07Z", // not a valid xsd:date value
        "xyz", // an absurd value
        "2013-02-29", // 2013 is not a leap year
    };
    final String[] parsedOriginalValues = {
        " \t\n 2003-04-25+09:00\n",
        "-0601-07-16-05:00",
        "1997-07-16",
        "1997-07-16Z",
        "2012-02-29",
        "----------",
        "1997-07Z",
        "xyz",
        "2013-02-29",
    };
    final String[] resultValues = {
        "2003-04-25+09:00",
        "-0601-07-16-05:00",
        "1997-07-16",
        "1997-07-16Z",
        "2012-02-29",
        "----------",
        "1997-07Z",
        "xyz",
        "2013-02-29",
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:Date xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:Date>\n";
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
            encoder.encode(new InputSource(new StringReader(originalValue)));
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
          Assert.assertEquals("Date", eventType.name);
          Assert.assertEquals("urn:foo", eventType.uri);
          ++n_events;
          
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals(values[i], exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            int tp = scanner.currentState.contentDatatype; 
            Assert.assertEquals(EXISchemaConst.DATE_TYPE, corpus.getSerialOfType(tp));
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
   * A valid time value matching ITEM_SCHEMA_CH where the associated
   * datatype is xsd:time.
   */
  public void testTime() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/dateTime.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\r 13:20:00+09:00\n", 
        "13:20:30.455-09:45", 
        "13:20:00", 
        "13:20:00Z",
        "24:00:00",
        "----------",
        "1997-07-16Z", // not a valid xsd:time value
        "xyz", // an absurd value
        "24:00:01",
    };
    final String[] parsedOriginalValues = {
        " \t\n 13:20:00+09:00\n", 
        "13:20:30.455-09:45", 
        "13:20:00", 
        "13:20:00Z",
        "24:00:00",
        "----------",
        "1997-07-16Z",        
        "xyz",
        "24:00:01",
    };
    final String[] resultValues = {
        "13:20:00+09:00", 
        "13:20:30.455-09:45", 
        "13:20:00", 
        "13:20:00Z",
        "24:00:00",
        "----------",
        "1997-07-16Z",        
        "xyz",
        "24:00:01",
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:Time xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:Time>\n";
    };
    
    for (AlignmentType alignment : Alignments) {
      for (boolean preserveLexicalValues : new boolean[] { true, false }) {
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
          Assert.assertEquals("Time", eventType.name);
          Assert.assertEquals("urn:foo", eventType.uri);
          ++n_events;
          
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals(values[i], exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            int tp = scanner.currentState.contentDatatype; 
            Assert.assertEquals(EXISchemaConst.TIME_TYPE, corpus.getSerialOfType(tp));
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
   * A valid gregorian day value matching ITEM_SCHEMA_CH where the associated
   * datatype is xsd:gDay.
   */
  public void testGDay() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/dateTime.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\r ---16\n",
        "---16+09:00",
        "---16Z",
        "---31+04:32",
        "----------",
        "1997-07-16Z", // not a valid xsd:gDay value
        "xyz", // an absurd value
        "---4", // #digits != 2
        "---32", // too big
        "---004", // #digits != 2
    };
    final String[] parsedOriginalValues = {
        " \t\n ---16\n",
        "---16+09:00",
        "---16Z",
        "---31+04:32",
        "----------",
        "1997-07-16Z",
        "xyz",
        "---4",
        "---32",
        "---004",
    };
    final String[] resultValues = {
        "---16",
        "---16+09:00",
        "---16Z",
        "---31+04:32",
        "----------",
        "1997-07-16Z",
        "xyz",
        "---4",
        "---32",
        "---004",
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:GDay xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:GDay>\n";
    };
    
    for (AlignmentType alignment : Alignments) {
      for (boolean preserveLexicalValues : new boolean[] { true, false }) {
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
          Assert.assertEquals("GDay", eventType.name);
          Assert.assertEquals("urn:foo", eventType.uri);
          ++n_events;
          
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals(values[i], exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            int tp = scanner.currentState.contentDatatype; 
            Assert.assertEquals(EXISchemaConst.G_DAY_TYPE, corpus.getSerialOfType(tp));
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
   * A valid gregorian month value matching ITEM_SCHEMA_CH where the associated
   * datatype is xsd:gMonth.
   */
  public void testGMonth() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/dateTime.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\r --09\n",
        "--09+09:00",
        "--09Z",
        "----------",
        "1997-07-16Z", // not a valid xsd:gMonth value
        "xyz", // an absurd value
        "--4", // #digits != 2
        "--13", // too big
        "--004", // #digits != 2
    };
    final String[] parsedOriginalValues = {
        " \t\n --09\n",
        "--09+09:00",
        "--09Z",
        "----------",
        "1997-07-16Z",
        "xyz",
        "--4",
        "--13",
        "--004", 
    };
    final String[] resultValues = {
        "--09",
        "--09+09:00",
        "--09Z",
        "----------",
        "1997-07-16Z",
        "xyz",
        "--4",
        "--13",
        "--004", 
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:GMonth xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:GMonth>\n";
    };
    
    for (AlignmentType alignment : Alignments) {
      for (boolean preserveLexicalValues : new boolean[] { true, false }) {
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
          Assert.assertEquals("GMonth", eventType.name);
          Assert.assertEquals("urn:foo", eventType.uri);
          ++n_events;
          
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals(values[i], exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            int tp = scanner.currentState.contentDatatype; 
            Assert.assertEquals(EXISchemaConst.G_MONTH_TYPE, corpus.getSerialOfType(tp));
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
   * datatype is xsd:gMonthDay.
   */
  public void testGMonthDay() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/dateTime.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\r --09-16\n",
        "--09-16+09:00",
        "--09-16Z",
        "--02-29",
        "----------",
        "1997-07-16Z", // not a valid xsd:gMonthDay value
        "xyz", // an absurd value
        "--02-30",
        "--04-31",
    };
    final String[] parsedOriginalValues = {
        " \t\n --09-16\n",
        "--09-16+09:00",
        "--09-16Z",
        "--02-29",
        "----------",
        "1997-07-16Z",
        "xyz",
        "--02-30",
        "--04-31",
    };
    final String[] resultValues = {
        "--09-16",
        "--09-16+09:00",
        "--09-16Z",
        "--02-29",
        "----------",
        "1997-07-16Z",
        "xyz",
        "--02-30",
        "--04-31",
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:GMonthDay xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:GMonthDay>\n";
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
          Assert.assertEquals("GMonthDay", eventType.name);
          Assert.assertEquals("urn:foo", eventType.uri);
          ++n_events;
          
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals(values[i], exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            int tp = scanner.currentState.contentDatatype; 
            Assert.assertEquals(EXISchemaConst.G_MONTHDAY_TYPE, corpus.getSerialOfType(tp));
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
   * A valid gregorian calendar year value matching ITEM_SCHEMA_CH where the associated
   * datatype is xsd:gYear.
   */
  public void testGYear() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/dateTime.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\r 1969\n",
        "1969+09:00",
        "1969Z",
        "0001",
        "----------",
        "1997-07-16Z", // not a valid xsd:gYear value
        "xyz", // an absurd value
        "001", // # of digits < 4
    };
    final String[] parsedOriginalValues = {
        " \t\n 1969\n",
        "1969+09:00",
        "1969Z",
        "0001",
        "----------",
        "1997-07-16Z",
        "xyz",
        "001",
    };
    final String[] resultValues = {
        "1969",
        "1969+09:00",
        "1969Z",
        "0001",
        "----------",
        "1997-07-16Z",
        "xyz",
        "001",
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:GYear xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:GYear>\n";
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
          Assert.assertEquals("GYear", eventType.name);
          Assert.assertEquals("urn:foo", eventType.uri);
          ++n_events;
          
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals(values[i], exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            int tp = scanner.currentState.contentDatatype; 
            Assert.assertEquals(EXISchemaConst.G_YEAR_TYPE, corpus.getSerialOfType(tp));
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
   * A valid value representing a specific gregorian month in a specific 
   * gregorian year value matching ITEM_SCHEMA_CH where the associated datatype 
   * is xsd:gYearMonth.
   */
  public void testGYearMonth() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/dateTime.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\r 1999-05\n",
        "1999-05+09:00",
        "1999-05Z",
        "1997-07-16Z", // not a valid xsd:gYearMonth value
        "xyz", // an absurd value
    };
    final String[] parsedOriginalValues = {
        " \t\n 1999-05\n",
        "1999-05+09:00",
        "1999-05Z",
        "1997-07-16Z",
        "xyz",
    };
    final String[] resultValues = {
        "1999-05",
        "1999-05+09:00",
        "1999-05Z",
        "1997-07-16Z",
        "xyz",
    };
    final int n_validValues = 3;

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
          
          encoder.setGrammarCache(grammarCache);
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          encoder.setOutputStream(baos);
          
          byte[] bts;
          int n_events;
          
          try {
            encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
          }
          catch (TransmogrifierException te) {
            Assert.assertTrue(!preserveLexicalValues && n_validValues <= i);
            continue;
          }
          Assert.assertTrue(preserveLexicalValues || i < n_validValues);
          
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
          Assert.assertEquals("GYearMonth", eventType.name);
          Assert.assertEquals("urn:foo", eventType.uri);
          ++n_events;
          
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals(values[i], exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            int tp = scanner.currentState.contentDatatype; 
            Assert.assertEquals(EXISchemaConst.G_YEARMONTH_TYPE, corpus.getSerialOfType(tp));
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
  
}
