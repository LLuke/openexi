package org.openexi.sax;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.ArrayList;

import junit.framework.Assert;
import junit.framework.TestCase;

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
        "-987654321098765432"
    };
    final String[] resultValues = {
        "9223372036854775807",
        "-9223372036854775808",
        "98765432109876543210",
        "987654321098765432",
        "-987654321098765432"
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
        Assert.assertEquals("Integer", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        
        exiEvent = exiEventList.get(2);
        Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
        Assert.assertEquals(resultValues[i], exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
        int tp = ((EventTypeSchema)eventType).getSchemaSubstance();
        Assert.assertEquals(EXISchemaConst.INTEGER_TYPE, corpus.getSerialOfType(tp));
    
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
        Assert.assertEquals("NBitInteger_A", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        
        exiEvent = exiEventList.get(2);
        Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
        Assert.assertEquals(resultValues[i], exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
        int tp = ((EventTypeSchema)eventType).getSchemaSubstance();
        int builtinType = corpus.getBaseTypeOfAtomicSimpleType(tp);
        Assert.assertEquals(EXISchemaConst.INTEGER_TYPE, corpus.getSerialOfType(builtinType));
    
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
        Assert.assertEquals("NBitInteger_B", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        
        exiEvent = exiEventList.get(2);
        Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
        Assert.assertEquals(resultValues[i], exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
        int tp = ((EventTypeSchema)eventType).getSchemaSubstance();
        int builtinType = corpus.getBaseTypeOfAtomicSimpleType(tp);
        Assert.assertEquals(EXISchemaConst.INTEGER_TYPE, corpus.getSerialOfType(builtinType));
    
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
