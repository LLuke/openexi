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
import org.openexi.fujitsu.sax.TransmogrifierException;
import org.openexi.fujitsu.schema.EXISchema;
import org.openexi.fujitsu.schema.EXISchemaConst;
import org.openexi.fujitsu.scomp.EXISchemaFactoryErrorMonitor;
import org.openexi.fujitsu.scomp.EXISchemaFactoryTestUtil;
import org.xml.sax.InputSource;

import junit.framework.Assert;
import junit.framework.TestCase;

public class LongEncodingTest extends TestCase {

  public LongEncodingTest(String name) {
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
   * Preserve lexical long values by turning on Preserve.lexicalValues.
   */
  public void testLongRCS() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/long.xsd", getClass(), m_compilerErrors);

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
      xmlStrings[i] = "<foo:Long xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:Long>\n";
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
   * A valid long value matching ITEM_SCHEMA_CH where the associated
   * datatype is xsd:long.
   */
  public void testValidLong() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/long.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String[] xmlStrings;
    final String[] values = {
        "  9223372036854775807 ", 
        " \t -9223372036854775808 \r\n "
    };
    final String[] resultValues = {
        "9223372036854775807", 
        "-9223372036854775808"
    };

    int i;
    xmlStrings = new String[values.length];
    for (i = 0; i < values.length; i++) {
      xmlStrings[i] = "<foo:Long xmlns:foo='urn:foo'>" + values[i] + "</foo:Long>\n";
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
        Assert.assertEquals("Long", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        
        exiEvent = exiEventList.get(2);
        Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
        Assert.assertEquals(resultValues[i], exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
        int tp = ((EventTypeSchema)eventType).getSchemaSubstance();
        int builtinType = corpus.getBuiltinTypeOfAtomicSimpleType(tp);
        Assert.assertEquals(EXISchemaConst.LONG_TYPE, corpus.getSerialOfType(builtinType));
    
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
   * An invalid long value matching ITEM_CH.
   */
  public void testInvalidLong() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/long.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
    
    final String[] xmlStrings;
    final String[] values = {
        "  1234567890123456789A ", 
    };
    final String[] resultValues = {
        "  1234567890123456789A ", 
    };

    int i;
    xmlStrings = new String[values.length];
    for (i = 0; i < values.length; i++) {
      xmlStrings[i] = "<foo:Long xmlns:foo='urn:foo'>" + values[i] + "</foo:Long>\n";
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
        Assert.assertEquals("Long", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        
        exiEvent = exiEventList.get(2);
        Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
        Assert.assertEquals(resultValues[i], exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_CH, eventType.itemType);
    
        exiEvent = exiEventList.get(3);
        Assert.assertEquals(EXIEvent.EVENT_EE, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_EE, eventType.itemType);
    
        exiEvent = exiEventList.get(4);
        Assert.assertEquals(EXIEvent.EVENT_ED, exiEvent.getEventVariety());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_ED, eventType.itemType);
      }
    }
  }

  /**
   * A valid long value matching ITEM_SCHEMA_CH where the associated
   * datatype is xsd:long.
   */
  public void testValidNBitLong_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/long.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String[] xmlStrings;
    final String[] values = {
        "  12678967543233 ", 
        "  12678967547233 ", // 12678967543233 + 4000
        "  12678967547328 ", // 12678967543233 + 4095
    };
    final String[] resultValues = {
        "12678967543233", 
        "12678967547233", 
        "12678967547328", 
    };

    int i;
    xmlStrings = new String[values.length];
    for (i = 0; i < values.length; i++) {
      xmlStrings[i] = "<foo:NBitLong_A xmlns:foo='urn:foo'>" + values[i] + "</foo:NBitLong_A>\n";
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
        Assert.assertEquals("NBitLong_A", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        
        exiEvent = exiEventList.get(2);
        Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
        Assert.assertEquals(resultValues[i], exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
        int tp = ((EventTypeSchema)eventType).getSchemaSubstance();
        int builtinType = corpus.getBuiltinTypeOfAtomicSimpleType(tp);
        Assert.assertEquals(EXISchemaConst.LONG_TYPE, corpus.getSerialOfType(builtinType));
    
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
   * Long values that are not in the n-bit representation range.
   */
  public void testInvalidNBitLong_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/long.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String[] xmlStrings;
    final String[] values = {
        "12678967543232", // 12678967543233 - 1
        "12678967547329", // 12678967543233 + 4096
    };

    int i;
    xmlStrings = new String[values.length];
    for (i = 0; i < values.length; i++) {
      xmlStrings[i] = "<foo:NBitLong_A xmlns:foo='urn:foo'>" + values[i] + "</foo:NBitLong_A>\n";
    };
    
      for (i = 0; i < xmlStrings.length; i++) {
        Transmogrifier encoder = new Transmogrifier();
  
        encoder.setEXISchema(grammarCache);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        encoder.setOutputStream(baos);
      
        boolean caught = false;
        try {
          encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
        }
        catch (TransmogrifierException eee) {
          caught = true;
        }
        finally {
          Assert.assertTrue(caught);
        }
      }
  }

  /**
   * A valid long value matching ITEM_SCHEMA_CH where the associated
   * datatype is xsd:long.
   */
  public void testValidNBitLong_02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/long.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String[] xmlStrings;
    final String[] values = {
        "  -12678967547233 ",
        "  -12678967543233 ", // -12678967547233 + 4000
        "  -12678967543138 ", // -12678967547233 + 4095
    };
    final String[] resultValues = {
        "-12678967547233",
        "-12678967543233",
        "-12678967543138",
    };

    int i;
    xmlStrings = new String[values.length];
    for (i = 0; i < values.length; i++) {
      xmlStrings[i] = "<foo:NBitLong_B xmlns:foo='urn:foo'>" + values[i] + "</foo:NBitLong_B>\n";
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
        Assert.assertEquals("NBitLong_B", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        
        exiEvent = exiEventList.get(2);
        Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
        Assert.assertEquals(resultValues[i], exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
        int tp = ((EventTypeSchema)eventType).getSchemaSubstance();
        int builtinType = corpus.getBuiltinTypeOfAtomicSimpleType(tp);
        Assert.assertEquals(EXISchemaConst.LONG_TYPE, corpus.getSerialOfType(builtinType));
    
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
   * Long values that are not in the n-bit representation range.
   */
  public void testInvalidNBitLong_02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/long.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String[] xmlStrings;
    final String[] values = {
        " -12678967547234", // -12678967547233 - 1
        " -12678967543137", // -12678967547233 + 4096
    };

    int i;
    xmlStrings = new String[values.length];
    for (i = 0; i < values.length; i++) {
      xmlStrings[i] = "<foo:NBitLong_B xmlns:foo='urn:foo'>" + values[i] + "</foo:NBitLong_B>\n";
    };
    
    for (i = 0; i < xmlStrings.length; i++) {
      Transmogrifier encoder = new Transmogrifier();

      encoder.setEXISchema(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
    
      boolean caught = false;
      try {
        encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
      }
      catch (TransmogrifierException eee) {
        caught = true;
      }
      finally {
        Assert.assertTrue(caught);
      }
    }
  }

  /**
   * A valid long value matching ITEM_SCHEMA_CH where the associated
   * datatype is xsd:long.
   */
  public void testValidNBitLong_03() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/long.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String[] xmlStrings;
    final String[] values = {
        "  2147480000 ",
        "2147484001",
    };
    final String[] resultValues = {
        "2147480000",
        "2147484001",
    };

    int i;
    xmlStrings = new String[values.length];
    for (i = 0; i < values.length; i++) {
      xmlStrings[i] = "<foo:NBitLong_C xmlns:foo='urn:foo'>" + values[i] + "</foo:NBitLong_C>\n";
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
        Assert.assertEquals("NBitLong_C", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        
        exiEvent = exiEventList.get(2);
        Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
        Assert.assertEquals(resultValues[i], exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
        int tp = ((EventTypeSchema)eventType).getSchemaSubstance();
        int builtinType = corpus.getBuiltinTypeOfAtomicSimpleType(tp);
        Assert.assertEquals(EXISchemaConst.LONG_TYPE, corpus.getSerialOfType(builtinType));
    
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
