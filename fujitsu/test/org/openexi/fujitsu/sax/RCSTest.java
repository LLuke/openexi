package org.openexi.fujitsu.sax;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.ArrayList;

import junit.framework.Assert;

import org.openexi.fujitsu.proc.EXIDecoder;
import org.openexi.fujitsu.proc.common.AlignmentType;
import org.openexi.fujitsu.proc.common.EXIEvent;
import org.openexi.fujitsu.proc.common.GrammarOptions;
import org.openexi.fujitsu.proc.grammars.GrammarCache;
import org.openexi.fujitsu.proc.io.Scanner;
import org.openexi.fujitsu.sax.Transmogrifier;
import org.openexi.fujitsu.schema.EXISchema;
import org.openexi.fujitsu.schema.TestBase;
import org.openexi.fujitsu.scomp.EXISchemaFactoryErrorMonitor;
import org.openexi.fujitsu.scomp.EXISchemaFactoryTestUtil;
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

    encoder.setEXISchema(grammarCache);
    decoder.setEXISchema(grammarCache);

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
      
      ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();
      
      EXIEvent exiEvent;
      n_events = 0;
      n_texts = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        if (exiEvent.getEventVariety() == EXIEvent.EVENT_CH) {
          String stringValue = exiEvent.getCharacters().makeString();
          Assert.assertEquals("XYZ", stringValue);
          Assert.assertTrue(exiEvent.getEventType().isSchemaInformed());
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

    encoder.setEXISchema(grammarCache);
    decoder.setEXISchema(grammarCache);

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
      
      ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();
      
      EXIEvent exiEvent;
      n_events = 0;
      n_texts = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        if (exiEvent.getEventVariety() == EXIEvent.EVENT_CH) {
          String stringValue = exiEvent.getCharacters().makeString();
          Assert.assertEquals("XYZ", stringValue);
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
