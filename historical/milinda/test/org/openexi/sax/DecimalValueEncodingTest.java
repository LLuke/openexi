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
import org.openexi.schema.EXISchemaConst;
import org.openexi.scomp.EXISchemaFactoryErrorMonitor;
import org.openexi.scomp.EXISchemaFactoryTestUtil;
import org.xml.sax.InputSource;

import junit.framework.Assert;
import junit.framework.TestCase;

public class DecimalValueEncodingTest extends TestCase {
  
  public DecimalValueEncodingTest(String name) {
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
   * A valid decimal value matching ITEM_SCHEMA_CH where the associated
   * datatype is xsd:decimal.
   */
  public void testValidDecimal_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/decimal.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    int i;
    
    final String[] xmlStrings;
    final String[] numbers = { 
        " \t\r 1267.89675\n", 
        "-1267.89675", 
        "1267.00675", 
        "-1267.00675" 
    };
    final String[] resultValues = {
        "1267.89675", 
        "-1267.89675", 
        "1267.00675", 
        "-1267.00675" 
    };
    xmlStrings = new String[numbers.length];
    for (i = 0; i < numbers.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + numbers[i] + "</foo:A>\n";
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
        Assert.assertEquals("A", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        
        exiEvent = exiEventList.get(2);
        Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
        Assert.assertEquals(resultValues[i], exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
        int tp = ((EventTypeSchema)eventType).getSchemaSubstance();
        int builtinType = corpus.getBaseTypeOfAtomicSimpleType(tp);
        Assert.assertEquals(EXISchemaConst.DECIMAL_TYPE, corpus.getSerialOfType(builtinType));
    
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
   * A valid decimal value matching ITEM_SCHEMA_CH where the associated
   * datatype is xsd:decimal.
   */
  public void testValidDecimal_02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/decimal.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    int i;
    
    final String[] xmlStrings;
    final String[] numbers = { 
        " \t\r 92233720368547758070000000000.00000000002233720368547758079\n",
        "-92233720368547758070000000000.00000000002233720368547758079" };
    final String[] resultValues = { 
        "92233720368547758070000000000.00000000002233720368547758079",
        "-92233720368547758070000000000.00000000002233720368547758079" };
    xmlStrings = new String[numbers.length];
    for (i = 0; i < numbers.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + numbers[i] + "</foo:A>\n";
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
        Assert.assertEquals("A", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        
        exiEvent = exiEventList.get(2);
        Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
        Assert.assertEquals(resultValues[i], exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
        int tp = ((EventTypeSchema)eventType).getSchemaSubstance();
        int builtinType = corpus.getBaseTypeOfAtomicSimpleType(tp);
        Assert.assertEquals(EXISchemaConst.DECIMAL_TYPE, corpus.getSerialOfType(builtinType));
    
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
   * Preserve lexical decimal values by turning on Preserve.lexicalValues.
   */
  public void testDecimalRCS() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/decimal.xsd", getClass(), m_compilerErrors);

    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\r *1267*.*89675*\n", // '*' will be encoded as an escaped character 
    };
    final String[] parsedOriginalValues = {
        " \t\n *1267*.*89675*\n", 
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:A>\n";
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
