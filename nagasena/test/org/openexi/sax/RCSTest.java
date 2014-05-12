package org.openexi.sax;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.ArrayList;

import junit.framework.Assert;

import org.openexi.proc.EXIDecoder;
import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventDescription;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.proc.io.Scanner;
import org.openexi.schema.EXISchema;
import org.openexi.schema.TestBase;
import org.openexi.scomp.EXISchemaFactoryErrorMonitor;
import org.openexi.scomp.EXISchemaFactoryTestUtil;
import org.xml.sax.InputSource;

public class RCSTest extends TestBase {

  public RCSTest(String name) {
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
   *  
   */
  public void testNoChars_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/patterns.xsd", getClass(), m_compilerErrors);

    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    Transmogrifier encoder = new Transmogrifier();
    EXIDecoder decoder = new EXIDecoder();

    encoder.setGrammarCache(grammarCache);
    decoder.setGrammarCache(grammarCache);

    encoder.setValueMaxLength(3);

    String xmlString;
    
    xmlString = "<NoChars xmlns='urn:foo'>XYZ</NoChars>\n";

    for (AlignmentType alignment : Alignments) {
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      byte[] bts;
      int n_events, n_texts;
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
    
      bts = baos.toByteArray();
  
      Scanner scanner;
  
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
          Assert.assertEquals("XYZ", stringValue);
          Assert.assertEquals(EventType.ITEM_SCHEMA_CH, exiEvent.getEventType().itemType);
          ++n_texts;
        }
        exiEventList.add(exiEvent);
      }
      Assert.assertEquals(1, n_texts);
      Assert.assertEquals(5, n_events);
    }
  }

  /**
   *  
   */
  public void testOneChar_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/patterns.xsd", getClass(), m_compilerErrors);

    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    Transmogrifier encoder = new Transmogrifier();
    EXIDecoder decoder = new EXIDecoder();

    encoder.setGrammarCache(grammarCache);
    decoder.setGrammarCache(grammarCache);

    encoder.setValueMaxLength(3);

    String xmlString;
    
    xmlString = "<OneChar xmlns='urn:foo'>XYZ</OneChar>\n";

    for (AlignmentType alignment : Alignments) {
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      byte[] bts;
      int n_events, n_texts;
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
    
      bts = baos.toByteArray();
  
      Scanner scanner;
  
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
          Assert.assertEquals("XYZ", stringValue);
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
