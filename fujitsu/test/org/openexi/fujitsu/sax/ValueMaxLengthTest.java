package org.openexi.fujitsu.sax;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.ArrayList;

import junit.framework.Assert;

import org.openexi.fujitsu.proc.EXIDecoder;
import org.openexi.fujitsu.proc.HeaderOptionsOutputType;
import org.openexi.fujitsu.proc.common.AlignmentType;
import org.openexi.fujitsu.proc.common.EXIEvent;
import org.openexi.fujitsu.proc.common.GrammarOptions;
import org.openexi.fujitsu.proc.grammars.GrammarCache;
import org.openexi.fujitsu.proc.io.Scanner;
import org.openexi.fujitsu.sax.Transmogrifier;
import org.openexi.fujitsu.schema.EmptySchema;
import org.openexi.fujitsu.schema.TestBase;
import org.xml.sax.InputSource;

public class ValueMaxLengthTest extends TestBase {

  public ValueMaxLengthTest(String name) {
    super(name);
  }

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
   * The stream can be decoded without a proper valueMaxLength value being set. 
   */
  public void testNoAddition_01() throws Exception {

    GrammarCache grammarCache = new GrammarCache(EmptySchema.getEXISchema(), GrammarOptions.DEFAULT_OPTIONS);

    Transmogrifier encoder = new Transmogrifier();
    EXIDecoder decoder = new EXIDecoder();

    encoder.setEXISchema(grammarCache);
    decoder.setEXISchema(grammarCache);

    encoder.setValueMaxLength(3);

    String xmlString;
    
    xmlString = 
      "<A xmlns='urn:foo'>" + 
        "<AB>abcd</AB><AC>abcd</AC><AB>abcd</AB><AC>abcd</AC>" +
      "</A>\n";

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
      String stringValue= null;
      n_events = 0;
      n_texts = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        if (exiEvent.getEventVariety() == EXIEvent.EVENT_CH) {
          stringValue = exiEvent.getCharacters().makeString();
          Assert.assertEquals("abcd", stringValue);
          ++n_texts;
        }
        exiEventList.add(exiEvent);
      }
      Assert.assertEquals(4, n_texts);
      Assert.assertEquals(16, n_events);
    }
  }

  /**
   * The stream *cannot* be decoded without a proper valueMaxLength value being set. 
   */
  public void testNoAddition_02() throws Exception {

    GrammarCache grammarCache = new GrammarCache(EmptySchema.getEXISchema(), GrammarOptions.DEFAULT_OPTIONS);

    Transmogrifier encoder = new Transmogrifier();
    EXIDecoder decoder = new EXIDecoder();

    encoder.setEXISchema(grammarCache);
    decoder.setEXISchema(grammarCache);

    encoder.setValueMaxLength(3);
    encoder.setOutputOptions(HeaderOptionsOutputType.lessSchemaId);

    for (AlignmentType alignment : Alignments) {
      
      encoder.setAlignmentType(alignment);

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
  
      String[] values = { "abcd", "abc", "abc" };
  
      String xmlString = 
        "<A xmlns='urn:foo'>" + 
          "<AB>abcd</AB><AB>abc</AB><AC>abc</AC>" +
        "</A>\n";
  
      byte[] bts;
      int n_events, n_texts;
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
  
      Scanner scanner;
  
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      Assert.assertEquals(3, scanner.getHeaderOptions().getValueMaxLength());
      Assert.assertEquals(alignment, scanner.getHeaderOptions().getAlignmentType());
      
      ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();
      
      EXIEvent exiEvent;
      String stringValue= null;
      n_events = 0;
      n_texts = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        if (exiEvent.getEventVariety() == EXIEvent.EVENT_CH) {
          stringValue = exiEvent.getCharacters().makeString();
          Assert.assertEquals(values[n_texts++], stringValue);
        }
        exiEventList.add(exiEvent);
      }
      Assert.assertEquals(3, n_texts);
      Assert.assertEquals(13, n_events);
    }
  }

}
