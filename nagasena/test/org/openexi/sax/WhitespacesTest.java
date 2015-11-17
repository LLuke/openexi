package org.openexi.sax;

import java.io.StringReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.xml.sax.InputSource;

import junit.framework.Assert;

import org.openexi.proc.EXIDecoder;
import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EventDescription;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.common.XmlUriConst;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.proc.io.Scanner;
import org.openexi.schema.EXISchema;
import org.openexi.schema.TestBase;

import org.openexi.scomp.EXISchemaFactoryErrorMonitor;
import org.openexi.scomp.EXISchemaFactoryTestUtil;

public class WhitespacesTest extends TestBase {

  public WhitespacesTest(String name) {
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
   * Whitespaces in element-only contexts are not preserved regardless of 
   * PreserveWhitespaces setting in strict mode.
   */
  public void testElementOnlyStrict_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/whiteSpace.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    final String xmlString = 
      "<foo:B xmlns:foo='urn:foo'>\n" +
      "  <C/>\n" +
      "  <D/>\n" +
      "</foo:B>\n";

    Transmogrifier encoder = new Transmogrifier();
    encoder.setGrammarCache(grammarCache);
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);

    for (AlignmentType alignment : Alignments) {
      for (boolean preserveWhitespaces : new boolean[] { true, false }) {
        encoder.setPreserveWhitespaces(preserveWhitespaces);
        
        encoder.setAlignmentType(alignment);
        decoder.setAlignmentType(alignment);
  
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        encoder.setOutputStream(baos);
        
        byte[] bts;
        
        encoder.encode(new InputSource(new StringReader(xmlString)));
        
        bts = baos.toByteArray();
        
        decoder.setInputStream(new ByteArrayInputStream(bts));
        Scanner scanner = decoder.processHeader();

        EventType eventType;
        EventDescription exiEvent;
        
        exiEvent = scanner.nextEvent();
        Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);
        Assert.assertEquals(0, eventType.getIndex());

        exiEvent = scanner.nextEvent();
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
        Assert.assertEquals("B", eventType.name);
        Assert.assertEquals("urn:foo", eventType.uri);
        
        exiEvent = scanner.nextEvent();
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
        Assert.assertEquals("C", eventType.name);
        Assert.assertEquals("", eventType.uri);
    
        exiEvent = scanner.nextEvent();
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());

        exiEvent = scanner.nextEvent();
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
        Assert.assertEquals("D", eventType.name);
        Assert.assertEquals("", eventType.uri);

        exiEvent = scanner.nextEvent();
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());

        exiEvent = scanner.nextEvent();
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());

        exiEvent = scanner.nextEvent();
        Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);

        Assert.assertNull(scanner.nextEvent());
      }
    }
  }

  /**
   * Whitespaces in element-only contexts are not preserved regardless of 
   * PreserveWhitespaces setting in strict mode.
   */
  public void testElementOnlyStrict_02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/whiteSpace.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    final String xmlString = 
        "<foo:B xmlns:foo='urn:foo' xmlns:xml='http://www.w3.org/XML/1998/namespace' " + 
        "       xml:space='preserve'>\n" +
        "  <C/>\n" +
        "  <D/>\n" +
        "</foo:B>\n";

    Transmogrifier encoder = new Transmogrifier();
    encoder.setGrammarCache(grammarCache);
    
    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);

    for (AlignmentType alignment : Alignments) {
      for (boolean preserveWhitespaces : new boolean[] { true, false }) {
        encoder.setPreserveWhitespaces(preserveWhitespaces);
        
        encoder.setAlignmentType(alignment);
        decoder.setAlignmentType(alignment);
  
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        encoder.setOutputStream(baos);
        
        byte[] bts;
        
        try {
          encoder.encode(new InputSource(new StringReader(xmlString)));
        }
        catch (TransmogrifierException te) {
          Assert.assertTrue(preserveWhitespaces);
          continue;
        }
        Assert.assertFalse(preserveWhitespaces);
        
        bts = baos.toByteArray();
        
        decoder.setInputStream(new ByteArrayInputStream(bts));
        Scanner scanner = decoder.processHeader();

        EventType eventType;
        EventDescription exiEvent;
        
        exiEvent = scanner.nextEvent();
        Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);
        Assert.assertEquals(0, eventType.getIndex());

        exiEvent = scanner.nextEvent();
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
        Assert.assertEquals("B", eventType.name);
        Assert.assertEquals("urn:foo", eventType.uri);
        
        exiEvent = scanner.nextEvent();
        Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
        Assert.assertEquals(XmlUriConst.W3C_XML_1998_URI, exiEvent.getURI());
        Assert.assertEquals("space", exiEvent.getName());
        Assert.assertEquals("preserve", exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        
        exiEvent = scanner.nextEvent();
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
        Assert.assertEquals("C", eventType.name);
        Assert.assertEquals("", eventType.uri);
    
        exiEvent = scanner.nextEvent();
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());

        exiEvent = scanner.nextEvent();
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
        Assert.assertEquals("D", eventType.name);
        Assert.assertEquals("", eventType.uri);

        exiEvent = scanner.nextEvent();
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());

        exiEvent = scanner.nextEvent();
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());

        exiEvent = scanner.nextEvent();
        Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);

        Assert.assertNull(scanner.nextEvent());
      }
    }
  }

  /**
   * Whitespaces in element-only contexts are preserved if PreserveWhitespaces is 
   * set to true in default mode.
   */
  public void testElementOnlyDefault_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/whiteSpace.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

    final String xmlString = 
      "<foo:B xmlns:foo='urn:foo'>\n" +
      "  <C/>\n" +
      "  <D/>\n" +
      "</foo:B>\n";

    Transmogrifier encoder = new Transmogrifier();
    EXIDecoder decoder = new EXIDecoder();

    for (AlignmentType alignment : Alignments) {
      for (boolean preserveWhitespaces : new boolean[] { true, false }) {
        encoder.setPreserveWhitespaces(preserveWhitespaces);
        
        encoder.setAlignmentType(alignment);
        decoder.setAlignmentType(alignment);
  
        encoder.setGrammarCache(grammarCache);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        encoder.setOutputStream(baos);
        
        byte[] bts;
        
        encoder.encode(new InputSource(new StringReader(xmlString)));
        
        bts = baos.toByteArray();
        
        decoder.setGrammarCache(grammarCache);
        
        decoder.setInputStream(new ByteArrayInputStream(bts));
        Scanner scanner = decoder.processHeader();

        EventType eventType;
        EventDescription exiEvent;
        
        exiEvent = scanner.nextEvent();
        Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);
        Assert.assertEquals(0, eventType.getIndex());

        exiEvent = scanner.nextEvent();
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
        Assert.assertEquals("B", eventType.name);
        Assert.assertEquals("urn:foo", eventType.uri);
        
        if (preserveWhitespaces) {
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals("\n  ", exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
        
        exiEvent = scanner.nextEvent();
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
        Assert.assertEquals("C", eventType.name);
        Assert.assertEquals("", eventType.uri);
    
        exiEvent = scanner.nextEvent();
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());

        if (preserveWhitespaces) {
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals("\n  ", exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }
        
        exiEvent = scanner.nextEvent();
        Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
        Assert.assertEquals("D", eventType.name);
        Assert.assertEquals("", eventType.uri);

        exiEvent = scanner.nextEvent();
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());

        if (preserveWhitespaces) {
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals("\n", exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
        }

        exiEvent = scanner.nextEvent();
        Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());

        exiEvent = scanner.nextEvent();
        Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
        eventType = exiEvent.getEventType();
        Assert.assertSame(exiEvent, eventType);

        Assert.assertNull(scanner.nextEvent());
      }
    }
  }

  /**
   * Whitespaces in element-only contexts are preserved if current xml:space  
   * setting is preserve in default mode.
   */
  public void testElementOnlyDefault_02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/whiteSpace.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

    final String xmlString = 
      "<foo:B xmlns:foo='urn:foo' xmlns:xml='http://www.w3.org/XML/1998/namespace' " + 
      "       xml:space='preserve'>\n" +
      "  <C/>\n" +
      "  <D/>\n" +
      "</foo:B>\n";

    Transmogrifier encoder = new Transmogrifier();
    encoder.setPreserveWhitespaces(false);
    encoder.setGrammarCache(grammarCache);

    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);

    for (AlignmentType alignment : Alignments) {
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      byte[] bts = baos.toByteArray();
      
      decoder.setInputStream(new ByteArrayInputStream(bts));
      Scanner scanner = decoder.processHeader();

      EventType eventType;
      EventDescription exiEvent;
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);
      Assert.assertEquals(0, eventType.getIndex());

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("B", eventType.name);
      Assert.assertEquals("urn:foo", eventType.uri);
      
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
      Assert.assertEquals(XmlUriConst.W3C_XML_1998_URI, exiEvent.getURI());
      Assert.assertEquals("space", exiEvent.getName());
      Assert.assertEquals("preserve", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("\n  ", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("C", eventType.name);
      Assert.assertEquals("", eventType.uri);
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("\n  ", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);
      
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
      Assert.assertEquals("D", eventType.name);
      Assert.assertEquals("", eventType.uri);

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals("\n", exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_CH, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());

      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
      eventType = exiEvent.getEventType();
      Assert.assertSame(exiEvent, eventType);

      Assert.assertNull(scanner.nextEvent());
    }
  }
  
}
