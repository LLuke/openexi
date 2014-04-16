package org.openexi.sax;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;

import org.openexi.proc.EXIDecoder;
import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EventDescription;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.proc.io.Scanner;
import org.openexi.schema.EXISchema;
import org.openexi.schema.EXISchemaConst;
import org.openexi.scomp.EXISchemaFactoryErrorMonitor;
import org.openexi.scomp.EXISchemaFactoryTestUtil;
import org.xml.sax.InputSource;

import junit.framework.Assert;
import junit.framework.TestCase;

public class HexBinaryValueEncodingTest extends TestCase {

  public HexBinaryValueEncodingTest(String name) {
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
   * A valid hexBinary value matching ITEM_SCHEMA_CH where the associated
   * datatype is xsd:hexBinary.
   */
  public void testValidHexBinary() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/hexBinary.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\r 0\tF b7\n",
        " \t\r\n ",
        " 0123456789ABCDEFabcdef0123456789ABCDEFabcdef0123456789ABCDEFabcdef0123456789\n" + 
        "ABCDEFabcdef0123456789ABCDEFabcdef0123456789ABCDEFabcdef0123456789ABCDEFabcd\n" + 
        "ef0123456789ABCDEFabcdef0123456789ABCDEFabcdef0123456789ABCDEFabcdef01234567\n" + 
        "89ABCDEFabcdef0123456789ABCDEFabcdef0123456789ABCDEFabcdef0123456789ABCDEFab\n" + 
        "cdef0123456789ABCDEFabcdef0123456789ABCDEFabcdef0123456789ABCDEFabcdef012345\n" + 
        "6789ABCDEFabcdef0123456789ABCDEFabcdef0123456789ABCDEFabcdef0123456789ABCDEF\n" + 
        "abcdef\t\r\n"
    };
    final String[] parsedOriginalValues = {
        " \t\n 0\tF b7\n", 
        " \t\n ",
        " 0123456789ABCDEFabcdef0123456789ABCDEFabcdef0123456789ABCDEFabcdef0123456789\n" + 
        "ABCDEFabcdef0123456789ABCDEFabcdef0123456789ABCDEFabcdef0123456789ABCDEFabcd\n" + 
        "ef0123456789ABCDEFabcdef0123456789ABCDEFabcdef0123456789ABCDEFabcdef01234567\n" + 
        "89ABCDEFabcdef0123456789ABCDEFabcdef0123456789ABCDEFabcdef0123456789ABCDEFab\n" + 
        "cdef0123456789ABCDEFabcdef0123456789ABCDEFabcdef0123456789ABCDEFabcdef012345\n" + 
        "6789ABCDEFabcdef0123456789ABCDEFabcdef0123456789ABCDEFabcdef0123456789ABCDEF\n" + 
        "abcdef\t\n"
    };
    final String[] resultValues = {
        "0FB7",
        "",
        "0123456789ABCDEFABCDEF0123456789ABCDEFABCDEF0123456789ABCDEFABCDEF0123456789" + 
        "ABCDEFABCDEF0123456789ABCDEFABCDEF0123456789ABCDEFABCDEF0123456789ABCDEFABCD" + 
        "EF0123456789ABCDEFABCDEF0123456789ABCDEFABCDEF0123456789ABCDEFABCDEF01234567" + 
        "89ABCDEFABCDEF0123456789ABCDEFABCDEF0123456789ABCDEFABCDEF0123456789ABCDEFAB" + 
        "CDEF0123456789ABCDEFABCDEF0123456789ABCDEFABCDEF0123456789ABCDEFABCDEF012345" + 
        "6789ABCDEFABCDEF0123456789ABCDEFABCDEF0123456789ABCDEFABCDEF0123456789ABCDEF" + 
        "ABCDEF"
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:HexBinary xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:HexBinary>\n";
    };
    
    Transmogrifier encoder = new Transmogrifier();
    EXIDecoder decoder = new EXIDecoder();
    
    for (AlignmentType alignment : Alignments) {
      for (boolean preserveLexicalValues : new boolean[] { true, false }) {
        String[] values = preserveLexicalValues ? 
            parsedOriginalValues : resultValues;
        for (i = 0; i < xmlStrings.length; i++) {
          Scanner scanner;
          
          encoder.setAlignmentType(alignment);
          decoder.setAlignmentType(alignment);
          
          encoder.setPreserveLexicalValues(preserveLexicalValues);
          decoder.setPreserveLexicalValues(preserveLexicalValues);
          
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
          Assert.assertEquals("HexBinary", eventType.name);
          Assert.assertEquals("urn:foo", eventType.uri);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals(values[i], exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            int tp = scanner.currentState.contentDatatype; 
            Assert.assertEquals(EXISchemaConst.HEXBINARY_TYPE, corpus.getSerialOfType(tp));
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
  }
  
  /**
   * Preserve lexical hexBinary values by turning on Preserve.lexicalValues.
   */
  public void testValidHexBinaryRCS() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/hexBinary.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\r 0\tF b@7\n", // '@' will be encoded as an escaped character
    };
    final String[] parsedOriginalValues = {
        " \t\n 0\tF b@7\n", 
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:HexBinary xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:HexBinary>\n";
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
        int n_events;
        
        encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
        
        bts = baos.toByteArray();
        
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
        Assert.assertEquals("HexBinary", eventType.name);
        Assert.assertEquals("urn:foo", eventType.uri);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
        Assert.assertEquals(parsedOriginalValues[i], exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          int tp = scanner.currentState.contentDatatype; 
          Assert.assertEquals(EXISchemaConst.HEXBINARY_TYPE, corpus.getSerialOfType(tp));
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
  
}
