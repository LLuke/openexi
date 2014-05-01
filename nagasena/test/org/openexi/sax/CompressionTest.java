package org.openexi.sax;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.zip.Deflater;

import org.xml.sax.InputSource;

import org.openexi.proc.EXIDecoder;
import org.openexi.proc.HeaderOptionsOutputType;
import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EventDescription;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventTypeList;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.events.EXIEventSchemaNil;
import org.openexi.proc.events.EXIEventSchemaType;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.proc.io.Scanner;
import org.openexi.proc.io.compression.ChannellingScanner;
import org.openexi.proc.io.compression.EXIEventValueReference;
import org.openexi.proc.util.ExiUriConst;
import org.openexi.sax.Transmogrifier;
import org.openexi.schema.EXISchema;
import org.openexi.schema.TestBase;
import org.openexi.scomp.EXISchemaFactoryErrorMonitor;
import org.openexi.scomp.EXISchemaFactoryTestUtil;

import junit.framework.Assert;

public class CompressionTest extends TestBase {
  
  public CompressionTest(String name) {
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
   * Do not use value partitions by setting the value of valueMaxLength
   * to zero.
   */
  public void testForgoValuePartitions_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        (String)null, getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
    
    for (AlignmentType alignment : Alignments) {
      for (boolean preserveWhitespaces : new boolean[] { false, true }) {
        for (boolean useThreadedInflater : new boolean[] { false, true }) {
          if (useThreadedInflater && alignment != AlignmentType.compress)
            continue;
          Transmogrifier encoder = new Transmogrifier();
          encoder.setValueMaxLength(0);
          EXIDecoder decoder = new EXIDecoder(31, useThreadedInflater);
          decoder.setValueMaxLength(0);
          Scanner scanner;
          InputSource inputSource;
          
          encoder.setAlignmentType(alignment);
          decoder.setAlignmentType(alignment);
    
          encoder.setGrammarCache(grammarCache);
          encoder.setPreserveWhitespaces(preserveWhitespaces);
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          encoder.setOutputStream(baos);
          
          URL url = resolveSystemIdAsURL("/compression/duplicateValues-01.xml");
          inputSource = new InputSource(url.toString());
          inputSource.setByteStream(url.openStream());
    
          byte[] bts;
          int n_events;
          
          encoder.encode(inputSource);
          
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
          Assert.assertNull(eventType.uri);
          Assert.assertNull(eventType.name);
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertNull(eventTypeList.getEE());
          ++n_events;
          
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
          Assert.assertEquals("", exiEvent.getURI());
          Assert.assertEquals("root", exiEvent.getName());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType); 
          Assert.assertNull(eventType.uri);
          Assert.assertNull(eventType.name);
          ++n_events;
  
          if (preserveWhitespaces) {
            exiEvent = scanner.nextEvent();
            Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
            Assert.assertEquals("\n   ", exiEvent.getCharacters().makeString());
            ++n_events;
          }
      
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
          Assert.assertEquals("a", exiEvent.getName());
          Assert.assertEquals("", exiEvent.getURI());
          ++n_events;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals("1", exiEvent.getCharacters().makeString());
          ++n_events;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
          ++n_events;
  
          if (preserveWhitespaces) {
            exiEvent = scanner.nextEvent();
            Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
            Assert.assertEquals("\n   ", exiEvent.getCharacters().makeString());
            ++n_events;
          }
          
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
          Assert.assertEquals("b", exiEvent.getName());
          Assert.assertEquals("", exiEvent.getURI());
          ++n_events;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals("2", exiEvent.getCharacters().makeString());
          ++n_events;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
          ++n_events;
  
          if (preserveWhitespaces) {
            exiEvent = scanner.nextEvent();
            Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
            Assert.assertEquals("\n   ", exiEvent.getCharacters().makeString());
            ++n_events;
          }
      
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
          Assert.assertEquals("a", exiEvent.getName());
          Assert.assertEquals("", exiEvent.getURI());
          ++n_events;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals("3", exiEvent.getCharacters().makeString());
          ++n_events;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
          ++n_events;
  
          if (preserveWhitespaces) {
            exiEvent = scanner.nextEvent();
            Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
            Assert.assertEquals("\n   ", exiEvent.getCharacters().makeString());
            ++n_events;
          }
      
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
          Assert.assertEquals("b", exiEvent.getName());
          Assert.assertEquals("", exiEvent.getURI());
          ++n_events;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals("4", exiEvent.getCharacters().makeString());
          ++n_events;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
          ++n_events;
  
          if (preserveWhitespaces) {
            exiEvent = scanner.nextEvent();
            Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
            Assert.assertEquals("\n   ", exiEvent.getCharacters().makeString());
            ++n_events;
          }

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
          Assert.assertEquals("a", exiEvent.getName());
          Assert.assertEquals("", exiEvent.getURI());
          ++n_events;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals("1", exiEvent.getCharacters().makeString());
          ++n_events;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
          ++n_events;
  
          if (preserveWhitespaces) {
            exiEvent = scanner.nextEvent();
            Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
            Assert.assertEquals("\n   ", exiEvent.getCharacters().makeString());
            ++n_events;
          }

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
          Assert.assertEquals("b", exiEvent.getName());
          Assert.assertEquals("", exiEvent.getURI());
          ++n_events;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals("2", exiEvent.getCharacters().makeString());
          ++n_events;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
          ++n_events;
  
          if (preserveWhitespaces) {
            exiEvent = scanner.nextEvent();
            Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
            Assert.assertEquals("\n", exiEvent.getCharacters().makeString());
            ++n_events;
          }


          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
          ++n_events;
          
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
          ++n_events;
  
          Assert.assertEquals(preserveWhitespaces ? 29 : 22, n_events);
        }
      }
    }
  }
  
  /**
   * EXI compression changes the order in which values are read and
   * written to and from an EXI stream.
   */
  public void testValueOrder_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        (String)null, getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
    
    for (AlignmentType alignment : Alignments) {
      for (boolean preserveWhitespaces : new boolean[] { true, false }) {
        for (boolean useThreadedInflater : new boolean[] { true, false }) {
          if (useThreadedInflater && alignment != AlignmentType.compress)
            continue;
          Transmogrifier encoder = new Transmogrifier();
          EXIDecoder decoder = new EXIDecoder(31, useThreadedInflater);
          Scanner scanner;
          InputSource inputSource;
          
          encoder.setAlignmentType(alignment);
          decoder.setAlignmentType(alignment);
    
          encoder.setGrammarCache(grammarCache);
          encoder.setPreserveWhitespaces(preserveWhitespaces);
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          encoder.setOutputStream(baos);
          
          URL url = resolveSystemIdAsURL("/compression/valueOrder-01.xml");
          inputSource = new InputSource(url.toString());
          inputSource.setByteStream(url.openStream());
    
          byte[] bts;
          int n_events;
          
          encoder.encode(inputSource);
          
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
          Assert.assertNull(eventType.uri);
          Assert.assertNull(eventType.name);
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertNull(eventTypeList.getEE());
          ++n_events;
          
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
          Assert.assertEquals("", exiEvent.getURI());
          Assert.assertEquals("root", exiEvent.getName());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType); 
          Assert.assertNull(eventType.uri);
          Assert.assertNull(eventType.name);
          ++n_events;
  
          if (preserveWhitespaces) {
            exiEvent = scanner.nextEvent();
            Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
            Assert.assertEquals("\n   ", exiEvent.getCharacters().makeString());
            ++n_events;
          }
      
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
          Assert.assertEquals("a", exiEvent.getName());
          Assert.assertEquals("", exiEvent.getURI());
          ++n_events;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals("XXX", exiEvent.getCharacters().makeString());
          ++n_events;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
          ++n_events;
  
          if (preserveWhitespaces) {
            exiEvent = scanner.nextEvent();
            Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
            Assert.assertEquals("\n   ", exiEvent.getCharacters().makeString());
            ++n_events;
          }
          
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
          Assert.assertEquals("b", exiEvent.getName());
          Assert.assertEquals("", exiEvent.getURI());
          ++n_events;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals("bla", exiEvent.getCharacters().makeString());
          ++n_events;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
          ++n_events;
  
          if (preserveWhitespaces) {
            exiEvent = scanner.nextEvent();
            Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
            Assert.assertEquals("\n   ", exiEvent.getCharacters().makeString());
            ++n_events;
          }
      
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
          Assert.assertEquals("c", exiEvent.getName());
          Assert.assertEquals("", exiEvent.getURI());
          ++n_events;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals("foo", exiEvent.getCharacters().makeString());
          ++n_events;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
          ++n_events;
  
          if (preserveWhitespaces) {
            exiEvent = scanner.nextEvent();
            Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
            Assert.assertEquals("\n   ", exiEvent.getCharacters().makeString());
            ++n_events;
          }
      
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
          Assert.assertEquals("b", exiEvent.getName());
          Assert.assertEquals("", exiEvent.getURI());
          ++n_events;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals("XXX", exiEvent.getCharacters().makeString());
          ++n_events;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
          ++n_events;
  
          for (int i = 0; i < 110; i++) {
            if (preserveWhitespaces) {
              exiEvent = scanner.nextEvent();
              Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
              Assert.assertEquals("\n   ", exiEvent.getCharacters().makeString());
              ++n_events;
            }
            
            exiEvent =scanner.nextEvent();
            Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
            Assert.assertEquals("a", exiEvent.getName());
            Assert.assertEquals("", exiEvent.getURI());
            ++n_events;
      
            exiEvent = scanner.nextEvent();
            Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
            Assert.assertEquals(Integer.toString(i + 1), exiEvent.getCharacters().makeString());
            ++n_events;
            
            exiEvent = scanner.nextEvent();
            Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
            ++n_events;
          }
          
          if (preserveWhitespaces) {
            exiEvent = scanner.nextEvent();
            Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
            Assert.assertEquals("\n", exiEvent.getCharacters().makeString());
            ++n_events;
          }
          
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
          ++n_events;
          
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
          ++n_events;
  
          Assert.assertEquals(preserveWhitespaces ? 461 : 346, n_events);
        }
      }
    }
  }

  /**
   * Values of xsi:nil attributes matching AT(xsi:nil) in schema-informed 
   * grammars are stored in structure channels whereas those that occur
   * in the context of built-in grammars are stored in value channels. 
   */
  public void testXsiNil_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        (String)null, getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

    final String xmlString = 
      "<A xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:nil='true'>" +
      "  <B xmlns:xsd='http://www.w3.org/2001/XMLSchema' xsi:type='xsd:boolean' xsi:nil='true' />" +
      "  <A xsi:nil='true' />" +
      "</A>\n";

    AlignmentType[] alignments = 
      new AlignmentType[] { AlignmentType.preCompress, AlignmentType.compress };

    for (AlignmentType alignment : alignments) {
      for (boolean preserveWhitespaces : new boolean[] { true, false }) {
        for (boolean useThreadedInflater : new boolean[] { true, false }) {
          if (useThreadedInflater && alignment != AlignmentType.compress)
            continue;
          Transmogrifier encoder = new Transmogrifier();
          EXIDecoder decoder = new EXIDecoder(31, useThreadedInflater);
          Scanner scanner;
          
          encoder.setAlignmentType(alignment);
          decoder.setAlignmentType(alignment);
    
          encoder.setGrammarCache(grammarCache);
          encoder.setPreserveWhitespaces(preserveWhitespaces);
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          encoder.setOutputStream(baos);
    
          byte[] bts;
          int n_events;
          
          encoder.encode(new InputSource(new StringReader(xmlString)));
          
          bts = baos.toByteArray();
          
          decoder.setGrammarCache(grammarCache);
          decoder.setInputStream(new ByteArrayInputStream(bts));
          scanner = decoder.processHeader();
          
          EventDescription exiEvent;
      
          n_events = 0;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
          ++n_events;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
          Assert.assertEquals("A", exiEvent.getName());
          Assert.assertEquals("", exiEvent.getURI());
          ++n_events;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
          Assert.assertEquals("nil", exiEvent.getName());
          Assert.assertEquals("http://www.w3.org/2001/XMLSchema-instance", exiEvent.getURI());
          Assert.assertEquals("true", exiEvent.getCharacters().makeString());
          Assert.assertTrue(exiEvent instanceof EXIEventValueReference); // was in value channel
          ++n_events;
  
          if (preserveWhitespaces) {
            exiEvent = scanner.nextEvent();
            Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
            Assert.assertEquals("  ", exiEvent.getCharacters().makeString());
            ++n_events;
          }
      
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
          Assert.assertEquals("B", exiEvent.getName());
          Assert.assertEquals("", exiEvent.getURI());
          ++n_events;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_TP, exiEvent.getEventKind());
          Assert.assertEquals("type", ((EXIEventSchemaType)exiEvent).getName());
          Assert.assertEquals("http://www.w3.org/2001/XMLSchema-instance", 
              ((EXIEventSchemaType)exiEvent).getURI());
          ++n_events;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_NL, exiEvent.getEventKind());
          Assert.assertTrue(((EXIEventSchemaNil)exiEvent).isNilled());
          ++n_events;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
          ++n_events;
  
          if (preserveWhitespaces) {
            exiEvent = scanner.nextEvent();
            Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
            Assert.assertEquals("  ", exiEvent.getCharacters().makeString());
            ++n_events;
          }
          
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
          Assert.assertEquals("A", exiEvent.getName());
          Assert.assertEquals("", exiEvent.getURI());
          ++n_events;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
          Assert.assertEquals("nil", exiEvent.getName());
          Assert.assertEquals("http://www.w3.org/2001/XMLSchema-instance", exiEvent.getURI());
          Assert.assertEquals("true", exiEvent.getCharacters().makeString());
          Assert.assertTrue(exiEvent instanceof EXIEventValueReference); // was in value channel
          ++n_events;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
          ++n_events;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
          ++n_events;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
          ++n_events;
          
          Assert.assertEquals(preserveWhitespaces ? 14 : 12, n_events);
        }
      }
    }
  }

  /**
   * EXI test cases of National Library of Medicine (NLM) XML formats.
   */
  public void testNLM_default_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/NLM/nlmcatalogrecord_060101.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

    for (AlignmentType alignment : Alignments) {
      for (boolean useThreadedInflater : new boolean[] { true, false }) {
        if (useThreadedInflater && alignment != AlignmentType.compress)
          continue;
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder(31, useThreadedInflater);
        Scanner scanner;
        InputSource inputSource;
        
        encoder.setAlignmentType(alignment);
        decoder.setAlignmentType(alignment);
  
        encoder.setGrammarCache(grammarCache);
        encoder.setPreserveWhitespaces(true);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        encoder.setOutputStream(baos);
        
        URL url = resolveSystemIdAsURL("/NLM/catplussamp2006.xml");
        inputSource = new InputSource(url.toString());
        inputSource.setByteStream(url.openStream());
  
        byte[] bts;
        int n_events;
        
        encoder.encode(inputSource);
        
        bts = baos.toByteArray();
  
        decoder.setGrammarCache(grammarCache);
        decoder.setInputStream(new ByteArrayInputStream(bts));
        scanner = decoder.processHeader();
        
        EventDescription exiEvent;
        n_events = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
        	if (n_events == 47024) {
            // Check the last value in the last value channel
            Assert.assertEquals("Interdisciplinary Studies", exiEvent.getCharacters().makeString());
        	}
          ++n_events;
        }
        Assert.assertEquals(50071, n_events);
      }
    }
  }

  /**
   * EXI test cases of National Library of Medicine (NLM) XML formats.
   */
  public void testNLM_strict_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/NLM/nlmcatalogrecord_060101.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    for (AlignmentType alignment : Alignments) {
      for (boolean useThreadedInflater : new boolean[] { true, false }) {
        if (useThreadedInflater && alignment != AlignmentType.compress)
          continue;
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder(31, useThreadedInflater);
        Scanner scanner;
        InputSource inputSource;
        
        encoder.setAlignmentType(alignment);
        decoder.setAlignmentType(alignment);
  
        encoder.setGrammarCache(grammarCache);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        encoder.setOutputStream(baos);
        
        URL url = resolveSystemIdAsURL("/NLM/catplussamp2006.xml");
        inputSource = new InputSource(url.toString());
        inputSource.setByteStream(url.openStream());
  
        byte[] bts;
        int n_events;
        
        encoder.encode(inputSource);
  
        bts = baos.toByteArray();
  
        decoder.setGrammarCache(grammarCache);
        decoder.setInputStream(new ByteArrayInputStream(bts));
        scanner = decoder.processHeader();
        
        EventDescription exiEvent;
        n_events = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
        	if (n_events == 33009) {
            // Check the last value in the last value channel
            Assert.assertEquals("Interdisciplinary Studies", exiEvent.getCharacters().makeString());
        	}
          ++n_events;
        }
        Assert.assertEquals(35176, n_events);
      }
    }
  }

  /**
   * Only a handful of values in a stream.
   */
  public void testSequence_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/interop/schemaInformedGrammar/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    AlignmentType[] alignments = new AlignmentType[] { AlignmentType.preCompress, AlignmentType.compress };

    int[] strategies = { Deflater.DEFAULT_STRATEGY, Deflater.FILTERED, Deflater.HUFFMAN_ONLY };  
    
    for (AlignmentType alignment : alignments) {
      for (boolean useThreadedInflater : new boolean[] { true, false }) {
        if (useThreadedInflater && alignment != AlignmentType.compress)
          continue;
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder(31, useThreadedInflater);
        Scanner scanner;
        InputSource inputSource;
        
        encoder.setOutputOptions(HeaderOptionsOutputType.lessSchemaId);
        encoder.setAlignmentType(alignment);
        
        encoder.setDeflateLevel(java.util.zip.Deflater.BEST_COMPRESSION);
        
        final boolean isCompress = alignment == AlignmentType.compress; 
        byte[][] resultBytes = isCompress ? new byte[3][] : null;
        
        for (int i = 0; i < strategies.length; i++) {
          
          encoder.setDeflateStrategy(strategies[i]);
          
          encoder.setGrammarCache(grammarCache);
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          encoder.setOutputStream(baos);
          
          URL url = resolveSystemIdAsURL("/interop/schemaInformedGrammar/declaredProductions/sequence-01.xml");
          inputSource = new InputSource(url.toString());
          inputSource.setByteStream(url.openStream());
    
          byte[] bts;
          int n_events;
          
          encoder.encode(inputSource);
          
          bts = baos.toByteArray();
          if (isCompress) 
            resultBytes[i] = bts;
          
          decoder.setGrammarCache(grammarCache);
          decoder.setInputStream(new ByteArrayInputStream(bts));
          scanner = decoder.processHeader();
          Assert.assertEquals(alignment, scanner.getHeaderOptions().getAlignmentType());
          
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
          Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
          Assert.assertEquals("A", exiEvent.getName());
          Assert.assertEquals("urn:foo", exiEvent.getURI());
          ++n_events;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
          Assert.assertEquals("AB", exiEvent.getName());
          Assert.assertEquals("urn:foo", exiEvent.getURI());
          ++n_events;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals("", exiEvent.getCharacters().makeString());
          ++n_events;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
          ++n_events;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
          Assert.assertEquals("AC", exiEvent.getName());
          Assert.assertEquals("urn:foo", exiEvent.getURI());
          ++n_events;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals("", exiEvent.getCharacters().makeString());
          ++n_events;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
          ++n_events;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
          Assert.assertEquals("AC", exiEvent.getName());
          Assert.assertEquals("urn:foo", exiEvent.getURI());
          ++n_events;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals("", exiEvent.getCharacters().makeString());
          ++n_events;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
          ++n_events;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
          Assert.assertEquals("AD", exiEvent.getName());
          Assert.assertEquals("urn:foo", exiEvent.getURI());
          ++n_events;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals("", exiEvent.getCharacters().makeString());
          ++n_events;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
          ++n_events;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
          Assert.assertEquals("AE", exiEvent.getName());
          Assert.assertEquals("urn:foo", exiEvent.getURI());
          ++n_events;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals("", exiEvent.getCharacters().makeString());
          ++n_events;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
          ++n_events;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
          ++n_events;
  
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
          ++n_events;
  
          Assert.assertEquals(19, n_events);
        }
        if (isCompress) {
          Assert.assertTrue(resultBytes[0].length < resultBytes[1].length);
          Assert.assertTrue(resultBytes[1].length < resultBytes[2].length);
        }
      }
    }
  }

  /**
   * EXI test cases of Joint Theater Logistics Management format.
   */
  public void testJTLM_publish911() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/JTLM/schemas/TLMComposite.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    // This test case takes a while to run, so just use "compress".
    AlignmentType alignment = AlignmentType.compress;
    
    Transmogrifier encoder = new Transmogrifier();
    EXIDecoder decoder = new EXIDecoder();
    Scanner scanner;
    InputSource inputSource;
    
    encoder.setAlignmentType(alignment);
    decoder.setAlignmentType(alignment);

    encoder.setGrammarCache(grammarCache);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    encoder.setOutputStream(baos);
    
    URL url = resolveSystemIdAsURL("/JTLM/publish911.xml");
    inputSource = new InputSource(url.toString());
    inputSource.setByteStream(url.openStream());

    byte[] bts;
    int n_events, n_texts;
    
    encoder.encode(inputSource);
    
    bts = baos.toByteArray();

    decoder.setGrammarCache(grammarCache);
    decoder.setInputStream(new ByteArrayInputStream(bts));
    scanner = decoder.processHeader();
    
    EventDescription exiEvent;
    n_events = 0;
    n_texts = 0;
    
    while ((exiEvent = scanner.nextEvent()) != null) {
      ++n_events;
      if (exiEvent.getEventKind() == EventDescription.EVENT_CH) {
        if (exiEvent.getCharacters().length == 0) {
          --n_events;
          continue;
        }
        if (n_texts % 100 == 0) {
          final int n = n_texts / 100;
          Assert.assertEquals(JTLMTest.publish911_centennials_1[n], exiEvent.getCharacters().makeString());
        }
        ++n_texts;
      }
    }
    Assert.assertEquals(96576, n_events);
  }

  /**
   * EXI test cases of Joint Theater Logistics Management format.
   */
  public void testJTLM_publish100_blockSize() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/JTLM/schemas/TLMComposite.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
   
    AlignmentType[] alignments = new AlignmentType[] { 
      AlignmentType.preCompress, 
      AlignmentType.compress 
    };
    int[] blockSizes = {
        1, 100, 101
     };

    Transmogrifier encoder = new Transmogrifier();

    encoder.setOutputOptions(HeaderOptionsOutputType.lessSchemaId);
    encoder.setGrammarCache(grammarCache);
    
    for (AlignmentType alignment : alignments) {
      for (int i = 0; i < blockSizes.length; i++) {
        for (boolean useThreadedInflater : new boolean[] { true, false }) {
          if (useThreadedInflater && alignment != AlignmentType.compress)
            continue;
          EXIDecoder decoder = new EXIDecoder(999, useThreadedInflater);
          decoder.setGrammarCache(grammarCache);
          Scanner scanner;
          InputSource inputSource;
          
          encoder.setAlignmentType(alignment);
          encoder.setBlockSize(blockSizes[i]);
    
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          encoder.setOutputStream(baos);
          
          URL url = resolveSystemIdAsURL("/JTLM/publish100.xml");
          inputSource = new InputSource(url.toString());
          inputSource.setByteStream(url.openStream());
    
          byte[] bts;
          int n_events, n_texts;
          
          encoder.encode(inputSource);
          
          bts = baos.toByteArray();
    
          decoder.setInputStream(new ByteArrayInputStream(bts));
          scanner = decoder.processHeader();
          
          EventDescription exiEvent;
          n_events = 0;
          n_texts = 0;
          
          while ((exiEvent = scanner.nextEvent()) != null) {
            ++n_events;
            if (exiEvent.getEventKind() == EventDescription.EVENT_CH) {
              if (exiEvent.getCharacters().length == 0) {
                --n_events;
                continue;
              }
              if (n_texts % 100 == 0) {
                final int n = n_texts / 100;
                Assert.assertEquals(JTLMTest.publish100_centennials_1[n], exiEvent.getCharacters().makeString());
              }
              ++n_texts;
            }
          }
          Assert.assertEquals(10610, n_events);
        }
      }
    }
  }

  /**
   */
  public void testEmptyBlock_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/compression/emptyBlock_01.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier encoder = new Transmogrifier();
    EXIDecoder decoder = new EXIDecoder(31, false);
    Scanner scanner;
    InputSource inputSource;

    encoder.setOutputOptions(HeaderOptionsOutputType.lessSchemaId);

    encoder.setAlignmentType(AlignmentType.compress);
    encoder.setBlockSize(1);

    encoder.setGrammarCache(grammarCache);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    encoder.setOutputStream(baos);
    
    URL url = resolveSystemIdAsURL("/compression/emptyBlock_01.xml");
    inputSource = new InputSource(url.toString());
    inputSource.setByteStream(url.openStream());

    byte[] bts;
    int n_events;
    
    encoder.encode(inputSource);
    
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
    Assert.assertNull(eventType.uri);
    Assert.assertNull(eventType.name);
    Assert.assertEquals(0, eventType.getIndex());
    eventTypeList = eventType.getEventTypeList();
    Assert.assertNull(eventTypeList.getEE());
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("", exiEvent.getURI());
    Assert.assertEquals("root", exiEvent.getName());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SE, eventType.itemType); 
    Assert.assertEquals("", eventType.uri);
    Assert.assertEquals("root", eventType.name);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("parent", exiEvent.getName());
    Assert.assertEquals("", eventType.uri);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("child", exiEvent.getName());
    Assert.assertEquals("", eventType.uri);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
    Assert.assertEquals("42", exiEvent.getCharacters().makeString());
    eventType = exiEvent.getEventType();
    Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("adjunct", exiEvent.getName());
    Assert.assertEquals("", exiEvent.getURI());
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
    ++n_events;

    Assert.assertEquals(11, n_events);
    Assert.assertEquals(1, ((ChannellingScanner)scanner).getBlockCount());
  }

  /**
   */
  public void testCompressionOption_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/optionsSchema.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    
    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier encoder = new Transmogrifier();
    encoder.setGrammarCache(grammarCache);
    encoder.setAlignmentType(AlignmentType.compress);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    encoder.setOutputStream(baos);
    encoder.setOutputOptions(HeaderOptionsOutputType.lessSchemaId);
    
    String xmlString;
    byte[] bts;
    EXIDecoder decoder;
    Scanner scanner;
    int n_events;
    
    xmlString = "<header xmlns='http://www.w3.org/2009/exi'><strict/></header>\n";
    
    encoder.encode(new InputSource(new StringReader(xmlString)));
    
    bts = baos.toByteArray();
    
    decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);
    decoder.setAlignmentType(AlignmentType.bitPacked); // try to confuse decoder.
    decoder.setInputStream(new ByteArrayInputStream(bts));
    scanner = decoder.processHeader();
    
    EventDescription exiEvent;
    n_events = 0;

    EventType eventType;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
    ++n_events;
    
    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("header", exiEvent.getName());
    Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
    ++n_events;
    
    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
    Assert.assertEquals("strict", exiEvent.getName());
    Assert.assertEquals(ExiUriConst.W3C_2009_EXI_URI, exiEvent.getURI());
    ++n_events;
    
    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
    ++n_events;

    exiEvent = scanner.nextEvent();
    Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
    eventType = exiEvent.getEventType();
    Assert.assertSame(exiEvent, eventType);
    Assert.assertEquals(0, eventType.getIndex());
    ++n_events;
    
    Assert.assertEquals(6, n_events);
  }
  
}
