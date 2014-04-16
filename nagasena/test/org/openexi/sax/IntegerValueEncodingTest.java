package org.openexi.sax;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.ArrayList;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.openexi.proc.EXIDecoder;
import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EventDescription;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.proc.io.Scanner;
import org.openexi.sax.Transmogrifier;
import org.openexi.schema.EXISchema;
import org.openexi.schema.EXISchemaConst;
import org.openexi.scomp.EXISchemaFactoryErrorMonitor;
import org.openexi.scomp.EXISchemaFactoryTestUtil;
import org.xml.sax.InputSource;

public class IntegerValueEncodingTest extends TestCase {

  public IntegerValueEncodingTest(String name) {
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
   * A valid integer value matching ITEM_SCHEMA_CH where the associated
   * datatype is xsd:integer.
   */
  public void testValidInteger() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/integer.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String[] xmlStrings;
    final String[] values = {
        "  9223372036854775807 ",
        " \t -9223372036854775808 \r\n ",
        "  98765432109876543210 ", // 20 digits do not fit into long        
        "987654321098765432", // 18 digits fit into long
        "-987654321098765432",
        "-68168168468468435168168468468468846" // a value from EXI interop test
    };
    final String[] resultValues = {
        "9223372036854775807",
        "-9223372036854775808",
        "98765432109876543210",
        "987654321098765432",
        "-987654321098765432",
        "-68168168468468435168168468468468846"
    };

    int i;
    xmlStrings = new String[values.length];
    for (i = 0; i < values.length; i++) {
      xmlStrings[i] = "<foo:Integer xmlns:foo='urn:foo'>" + values[i] + "</foo:Integer>\n";
    };
    
    for (AlignmentType alignment : Alignments) {
      for (i = 0; i < xmlStrings.length; i++) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;
        
        encoder.setAlignmentType(alignment);
        decoder.setAlignmentType(alignment);
  
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
        Assert.assertEquals("Integer", eventType.name);
        Assert.assertEquals("urn:foo", eventType.uri);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
        Assert.assertEquals(resultValues[i], exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          int tp = scanner.currentState.contentDatatype; 
          Assert.assertEquals(EXISchemaConst.INTEGER_TYPE, corpus.getSerialOfType(tp));
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

  /**
   */
  public void testValidNBitInteger_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/integer.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String[] xmlStrings;
    final String[] values = {
        "  20 ",
        "70",
    };
    final String[] resultValues = {
        "20",
        "70",
    };

    int i;
    xmlStrings = new String[values.length];
    for (i = 0; i < values.length; i++) {
      xmlStrings[i] = "<foo:NBitInteger_A xmlns:foo='urn:foo'>" + values[i] + "</foo:NBitInteger_A>\n";
    };
    
    for (AlignmentType alignment : Alignments) {
      for (i = 0; i < xmlStrings.length; i++) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;
        
        encoder.setAlignmentType(alignment);
        decoder.setAlignmentType(alignment);
  
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
        Assert.assertEquals("NBitInteger_A", eventType.name);
        Assert.assertEquals("urn:foo", eventType.uri);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
        Assert.assertEquals(resultValues[i], exiEvent.getCharacters().makeString());
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

  /**
   */
  public void testValidNBitInteger_02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/integer.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String[] xmlStrings;
    final String[] values = {
        "  12678967543253 ",
        "12678967543285"
    };
    final String[] resultValues = {
        "12678967543253",
        "12678967543285"
    };

    int i;
    xmlStrings = new String[values.length];
    for (i = 0; i < values.length; i++) {
      xmlStrings[i] = "<foo:NBitInteger_B xmlns:foo='urn:foo'>" + values[i] + "</foo:NBitInteger_B>\n";
    };
    
    for (AlignmentType alignment : Alignments) {
      for (i = 0; i < xmlStrings.length; i++) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;
        
        encoder.setAlignmentType(alignment);
        decoder.setAlignmentType(alignment);
  
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
        Assert.assertEquals("NBitInteger_B", eventType.name);
        Assert.assertEquals("urn:foo", eventType.uri);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
        Assert.assertEquals(resultValues[i], exiEvent.getCharacters().makeString());
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

  /**
   * Preserve lexical integer values by turning on Preserve.lexicalValues.
   */
  public void testIntegerRCS() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/integer.xsd", getClass(), m_compilerErrors);

    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\r *9223372036854775807*\n", // '*' will be encoded as an escaped character 
    };
    final String[] parsedOriginalValues = {
        " \t\n *9223372036854775807*\n", 
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:Integer xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:Integer>\n";
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
  
}
