package org.openexi.sax;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.ArrayList;

import org.openexi.proc.EXIDecoder;
import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EXIOptionsException;
import org.openexi.proc.common.EventDescription;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.common.XmlUriConst;
import org.openexi.proc.events.EXIEventSchemaType;
import org.openexi.proc.grammars.EventTypeSchema;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.proc.io.Scanner;
import org.openexi.schema.EXISchema;
import org.openexi.schema.EXISchemaConst;
import org.openexi.schema.EmptySchema;
import org.openexi.schema.TestBase;
import org.openexi.scomp.EXISchemaFactoryErrorMonitor;
import org.openexi.scomp.EXISchemaFactoryTestUtil;
import org.xml.sax.InputSource;

import junit.framework.Assert;

public class FloatValueEncodingTest extends TestBase {

  public FloatValueEncodingTest(String name) {
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
   * A valid float value matching ITEM_SCHEMA_CH where the associated
   * datatype is xsd:float.
   */
  public void testValidFloat() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/float.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String[] xmlStrings;
    final String[] values = {
        "  -1E4 ", 
        " \t 1267.43233E12 \r\n ", 
        "12.78e-2", 
        "1200.00e+2", 
        "12", 
        "0", 
        "-0", 
        "INF", 
        "-INF", 
        "NaN",
        "-9223372036854775808",
        "9223372036854775807"
        
    };
    final String[] resultValues = {
        "-1E4", 
        "126743233E7", 
        "1278E-4", 
        "120000E0", 
        "12E0", 
        "0E0", 
        "0E0", 
        "INF", 
        "-INF", 
        "NaN",
        "-9223372036854775808E0",
        "9223372036854775807E0"
    };
    final String[] resultCanonicalValues = {
        "-1E4", 
        "126743233E7", 
        "1278E-4", 
        "12E4", 
        "12E0", 
        "0E0", 
        "0E0", 
        "INF", 
        "-INF", 
        "NaN",
        "-9223372036854775808E0",
        "9223372036854775807E0"
    };

    int i;
    xmlStrings = new String[values.length];
    for (i = 0; i < values.length; i++) {
      xmlStrings[i] = "<foo:Float xmlns:foo='urn:foo'>" + values[i] + "</foo:Float>\n";
    };
    
    for (AlignmentType alignment : Alignments) {
      for (i = 0; i < xmlStrings.length; i++) {
        for (boolean observeC14N : new boolean[] { true, false }) {
          Transmogrifier encoder = new Transmogrifier();
          EXIDecoder decoder = new EXIDecoder();
          Scanner scanner;
          
          encoder.setAlignmentType(alignment);
          decoder.setAlignmentType(alignment);
    
          encoder.setGrammarCache(grammarCache);
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          encoder.setOutputStream(baos);
          
          byte[] bts;
          int n_events;
          
          try {
            encoder.setObserveC14N(observeC14N);
          }
          catch (EXIOptionsException eoe) {
            Assert.assertEquals(AlignmentType.compress, alignment);
            observeC14N = false;
          }
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
          Assert.assertEquals("Float", eventType.name);
          Assert.assertEquals("urn:foo", eventType.uri);
          ++n_events;
          
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
          Assert.assertEquals(observeC14N ? resultCanonicalValues[i] : resultValues[i], exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            int tp = scanner.currentState.contentDatatype;
            Assert.assertEquals(EXISchemaConst.FLOAT_TYPE, corpus.getSerialOfType(tp));
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
   * A valid float value matching ITEM_SCHEMA_AT where the associated
   * datatype is xsd:float.
   */
  public void testCanonicalFloatAttribute() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/float.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String[] xmlStrings;
    final String[] values = {
        "1200.00e+2" 
    };
    final String[] resultValues = {
        "120000E0"
    };
    final String[] resultCanonicalValues = {
        "12E4"
    };

    int i;
    xmlStrings = new String[values.length];
    for (i = 0; i < values.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' float='" + values[i] + "'></foo:A>\n";
    };
    
    for (AlignmentType alignment : Alignments) {
      for (i = 0; i < xmlStrings.length; i++) {
        for (boolean observeC14N : new boolean[] { true, false }) {
          Transmogrifier encoder = new Transmogrifier();
          EXIDecoder decoder = new EXIDecoder();
          Scanner scanner;
          
          encoder.setAlignmentType(alignment);
          decoder.setAlignmentType(alignment);
    
          encoder.setGrammarCache(grammarCache);
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          encoder.setOutputStream(baos);
          
          byte[] bts;
          int n_events;
          
          try {
            encoder.setObserveC14N(observeC14N);
          }
          catch (EXIOptionsException eoe) {
            Assert.assertEquals(AlignmentType.compress, alignment);
            observeC14N = false;
          }
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
          Assert.assertEquals("A", eventType.name);
          Assert.assertEquals("urn:foo", eventType.uri);
          ++n_events;
          
          exiEvent = scanner.nextEvent();
          Assert.assertEquals(EventDescription.EVENT_AT, exiEvent.getEventKind());
          Assert.assertEquals(observeC14N ? resultCanonicalValues[i] : resultValues[i], exiEvent.getCharacters().makeString());
          eventType = exiEvent.getEventType();
          Assert.assertEquals(EventType.ITEM_SCHEMA_AT, eventType.itemType);
          int tp = ((EventTypeSchema)eventType).nd;
          Assert.assertEquals(EXISchemaConst.FLOAT_TYPE, corpus.getSerialOfType(tp));
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
   * A valid float value matching ITEM_SCHEMA_CH where the associated
   * datatype is xsd:double.
   */
  public void testValidDouble() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/float.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    final String[] xmlStrings;
    final String[] values = {
        "  -1E4 ", 
        " \t 1267.43233E12 \r\n ", 
        "12.78e-2", 
        "12", 
        "0", 
        "-0", 
        "INF", 
        "-INF", 
        "NaN"
    };
    final String[] resultValues = {
        "-1E4", 
        "126743233E7", 
        "1278E-4", 
        "12E0", 
        "0E0", 
        "0E0", 
        "INF", 
        "-INF", 
        "NaN"
    };

    int i;
    xmlStrings = new String[values.length];
    for (i = 0; i < values.length; i++) {
      xmlStrings[i] = "<foo:Double xmlns:foo='urn:foo'>" + values[i] + "</foo:Double>\n";
    };
    
    for (AlignmentType alignment : Alignments) {
      for (i = 0; i < xmlStrings.length; i++) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;
        
        encoder.setAlignmentType(alignment);
        decoder.setAlignmentType(alignment);
  
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
        Assert.assertEquals("Double", eventType.name);
        Assert.assertEquals("urn:foo", eventType.uri);
        ++n_events;
        
        exiEvent = scanner.nextEvent();
        Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
        Assert.assertEquals(resultValues[i], exiEvent.getCharacters().makeString());
        eventType = exiEvent.getEventType();
        Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          int tp = scanner.currentState.contentDatatype;
          Assert.assertEquals(EXISchemaConst.DOUBLE_TYPE, corpus.getSerialOfType(tp));
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

  /**
   * Preserve lexical float values by turning on Preserve.lexicalValues.
   */
  public void testFloatRCS() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/float.xsd", getClass(), m_compilerErrors);

    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    final String[] xmlStrings;
    final String[] originalValues = {
        " \t\r *-1*E*4*\n", // '*' will be encoded as an escaped character 
    };
    final String[] parsedOriginalValues = {
        " \t\n *-1*E*4*\n", 
    };

    int i;
    xmlStrings = new String[originalValues.length];
    for (i = 0; i < originalValues.length; i++) {
      xmlStrings[i] = "<foo:Float xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:Float>\n";
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
        int n_events, n_texts;
        
        encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
        
        bts = baos.toByteArray();
        
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
            Assert.assertEquals(parsedOriginalValues[i], stringValue);
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

  /**
   * Use EmptySchema, with xsi:type to explicitly specify the type.
   * 
   * <value xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'
   *        xmlns:xs='http://www.w3.org/2001/XMLSchema'
   *        xsi:type='xs:float'> 1.0 </value>
   */
  public void testDecodeValidFloat() throws Exception {

    EXISchema corpus = EmptySchema.getEXISchema();
    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
    
    EXIDecoder decoder;
    Scanner scanner;
    
    decoder = new EXIDecoder();
    decoder.setAlignmentType(AlignmentType.byteAligned);
    
    decoder.setGrammarCache(grammarCache);
    
    /**
     * <value xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'
     *        xmlns:xs='http://www.w3.org/2001/XMLSchema'
     *        xsi:type='xs:float'> 1.0 </value>
     */
    byte[][] bts = new byte[][] { 
        new byte[] { 
            (byte)0x80, 0x01, 0x06, 0x76, 0x61, 0x6c, 0x75, 0x65, 0x01, 
            0x03, 0x00, 0x01, 0x04, 0x00, 0x16, 0x00, 0x01, 0x00, 0x00 },
        new byte[] { 
            (byte)0x80, 0x01, 0x06, 0x76, 0x61, 0x6c, 0x75, 0x65, 0x01, 
            0x03, 0x00, 0x01, 0x04, 0x00, 0x16, 0x00, 0x0a, 0x01, 0x00 },
    };
    
    String stringValues[] = new String[] {
      "1E0",
      "10E-1"  
    };

    for (int i = 0; i < bts.length; i++) {
      decoder.setInputStream(new ByteArrayInputStream(bts[i]));
      scanner = decoder.processHeader();
      
      int n_events;
  
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
      Assert.assertEquals("", exiEvent.getURI());
      Assert.assertEquals("value", exiEvent.getName());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SE_WC, eventType.itemType);
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_TP, exiEvent.getEventKind());
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI, ((EXIEventSchemaType)exiEvent).getTypeURI());
      Assert.assertEquals("float", ((EXIEventSchemaType)exiEvent).getTypeName());
      ++n_events;
  
      exiEvent = scanner.nextEvent();
      Assert.assertEquals(EventDescription.EVENT_CH, exiEvent.getEventKind());
      Assert.assertEquals(stringValues[i], exiEvent.getCharacters().makeString());
      eventType = exiEvent.getEventType();
      Assert.assertEquals(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      int tp = scanner.currentState.contentDatatype;
      Assert.assertEquals(EXISchemaConst.FLOAT_TYPE, corpus.getSerialOfType(tp));
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

      Assert.assertEquals(6, n_events);
    }
  }

}
