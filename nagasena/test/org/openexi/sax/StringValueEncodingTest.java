package org.openexi.sax;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.openexi.proc.EXIDecoder;
import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EventDescription;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventTypeList;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.proc.io.Scanner;
import org.openexi.schema.EXISchema;
import org.openexi.schema.EXISchemaConst;
import org.openexi.scomp.EXISchemaFactoryErrorMonitor;
import org.openexi.scomp.EXISchemaFactoryTestUtil;
import org.xml.sax.InputSource;

public class StringValueEncodingTest extends TestCase {
  
  public StringValueEncodingTest(String name) {
    super(name);
    m_compilerErrors = new EXISchemaFactoryErrorMonitor();
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
   * Encode then decoder non-BMP characters.
   */
  public void testNonBMPCharacters_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/verySimpleDefault.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    
    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
    
    final String[] xmlStrings;
    final String[] values = {
        "\uD840\uDC0B\uD844\uDE3D", // characters in SIP (U+2000B) and (U+2123D)
        "a \uD840\uDC0B\uD844\uDE3D b",
    };
    final String[] resultValues = {
        "\uD840\uDC0B\uD844\uDE3D",
        "a \uD840\uDC0B\uD844\uDE3D b",
    };

    int i;
    xmlStrings = new String[values.length];
    for (i = 0; i < values.length; i++) {
      xmlStrings[i] = "<B>" + values[i] + "</B>\n";
    };

    Transmogrifier encoder = new Transmogrifier();
    encoder.setGrammarCache(grammarCache);

    EXIDecoder decoder = new EXIDecoder();
    decoder.setGrammarCache(grammarCache);

    for (AlignmentType alignment : Alignments) {
      for (boolean preserveLexicalValues : new boolean[] { true, false }) {
        for (i = 0; i < xmlStrings.length; i++) {
          Scanner scanner;

          encoder.setAlignmentType(alignment);
          decoder.setAlignmentType(alignment);
          
          encoder.setPreserveLexicalValues(preserveLexicalValues);
          decoder.setPreserveLexicalValues(preserveLexicalValues);
    
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          encoder.setOutputStream(baos);
          
          byte[] bts;
          int n_events;
          
          encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
          
          bts = baos.toByteArray();
          
          decoder.setInputStream(new ByteArrayInputStream(bts));
          scanner = decoder.processHeader();
          
          EventDescription exiEvent;
          n_events = 0;
      
          EventType eventType;
          EventTypeList eventTypeList;
      
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SD, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertSame(exiEvent, eventType);
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertNull(eventTypeList.getEE());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_SE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SE, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals(resultValues[i], exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) 
            Assert.assertEquals(EXISchemaConst.STRING_TYPE, corpus.getSerialOfType(scanner.currentState.contentDatatype));
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_EE, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_EE, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_ED, exiEvent.getEventKind());
          eventType = exiEvent.getEventType();
          Assert.assertSame(exiEvent, eventType);
          Assert.assertEquals(0, eventType.getIndex());
          eventTypeList = eventType.getEventTypeList();
          Assert.assertEquals(1, eventTypeList.getLength());
          ++n_events;
          
          Assert.assertEquals(5, n_events);
        }
      }
    }
  }
  
}
