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
import org.openexi.fujitsu.schema.EXISchema;
import org.openexi.fujitsu.schema.EXISchemaConst;
import org.openexi.fujitsu.scomp.EXISchemaFactoryErrorMonitor;
import org.openexi.fujitsu.scomp.EXISchemaFactoryTestUtil;
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
          Assert.assertEquals("HexBinary", eventType.getName());
          Assert.assertEquals("urn:foo", eventType.getURI());
          
          exiEvent = exiEventList.get(2);
          Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
          Assert.assertEquals(values[i], exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
          int tp = ((EventTypeSchema)eventType).getSchemaSubstance();
          int builtinType = corpus.getBuiltinTypeOfAtomicSimpleType(tp);
          Assert.assertEquals(EXISchemaConst.HEXBINARY_TYPE, corpus.getSerialOfType(builtinType));
      
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
        int n_events;
        
        encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
        
        bts = baos.toByteArray();
        
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
        Assert.assertEquals("HexBinary", eventType.getName());
        Assert.assertEquals("urn:foo", eventType.getURI());
        
        exiEvent = exiEventList.get(2);
        Assert.assertEquals(EXIEvent.EVENT_CH, exiEvent.getEventVariety());
        Assert.assertEquals(parsedOriginalValues[i], exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventCode.ITEM_SCHEMA_CH, eventType.itemType);
        int tp = ((EventTypeSchema)eventType).getSchemaSubstance();
        int builtinType = corpus.getBuiltinTypeOfAtomicSimpleType(tp);
        Assert.assertEquals(EXISchemaConst.HEXBINARY_TYPE, corpus.getSerialOfType(builtinType));
    
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
