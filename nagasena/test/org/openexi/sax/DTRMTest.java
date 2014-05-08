package org.openexi.sax;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;

import junit.framework.Assert;

import org.openexi.proc.EXIDecoder;
import org.openexi.proc.HeaderOptionsOutputType;
import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventDescription;
import org.openexi.proc.common.EXIOptionsException;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.common.QName;
import org.openexi.proc.common.XmlUriConst;
import org.openexi.proc.grammars.Apparatus;
import org.openexi.proc.grammars.ApparatusUtil;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.proc.io.Scanner;
import org.openexi.proc.util.ExiUriConst;
import org.openexi.sax.Transmogrifier;
import org.openexi.sax.TransmogrifierException;
import org.openexi.schema.EXISchemaConst;
import org.openexi.schema.EmptySchema;
import org.openexi.schema.EXISchema;
import org.openexi.schema.TestBase;
import org.openexi.scomp.EXISchemaFactoryErrorMonitor;
import org.openexi.scomp.EXISchemaFactoryTestUtil;
import org.xml.sax.InputSource;

public class DTRMTest extends TestBase  {

  public DTRMTest(String name) {
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
   * Preserve.lexicalValues option and DTRM cannot be specified together in EXI header options.
   */
  public void testPreserveLexicalValues() throws Exception {

    GrammarCache grammarCache = new GrammarCache(EmptySchema.getEXISchema(), GrammarOptions.STRICT_OPTIONS);

    final QName[] datatypeRepresentationMap = new QName[2];
    datatypeRepresentationMap[0] = new QName("xsd:boolean", XmlUriConst.W3C_2001_XMLSCHEMA_URI);
    datatypeRepresentationMap[1] = new QName("exi:integer", ExiUriConst.W3C_2009_EXI_URI);

    Transmogrifier encoder;
    boolean caught;
    
    encoder = new Transmogrifier();
    encoder.setGrammarCache(grammarCache);

    encoder.setOutputOptions(HeaderOptionsOutputType.none);
    encoder.setPreserveLexicalValues(true);
    encoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);

    /* ------------------------------------------------------------------------------- */

    encoder = new Transmogrifier();
    encoder.setGrammarCache(grammarCache);

    encoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);
    encoder.setPreserveLexicalValues(true);
    encoder.setOutputOptions(HeaderOptionsOutputType.none);

    /* ------------------------------------------------------------------------------- */
    
    encoder = new Transmogrifier();
    encoder.setGrammarCache(grammarCache);
    
    encoder.setOutputOptions(HeaderOptionsOutputType.lessSchemaId);
    encoder.setPreserveLexicalValues(true);

    caught = false;
    try {
      encoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);
    }
    catch (EXIOptionsException eoe) {
      caught = true;
    }
    Assert.assertTrue(caught);

    /* ------------------------------------------------------------------------------- */
    
    encoder = new Transmogrifier();
    encoder.setGrammarCache(grammarCache);

    encoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);
    encoder.setPreserveLexicalValues(true);

    caught = false;
    try {
      encoder.setOutputOptions(HeaderOptionsOutputType.lessSchemaId);
    }
    catch (EXIOptionsException eoe) {
      caught = true;
    }
    Assert.assertTrue(caught);
    
    /* ------------------------------------------------------------------------------- */
    
    encoder = new Transmogrifier();
    encoder.setGrammarCache(grammarCache);

    encoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);
    encoder.setOutputOptions(HeaderOptionsOutputType.lessSchemaId);

    caught = false;
    try {
      encoder.setPreserveLexicalValues(true);
    }
    catch (EXIOptionsException eoe) {
      caught = true;
    }
    Assert.assertTrue(caught);
  }

  /**
   * Use DTRM to represent xsd:boolean using exi:integer.
   */
  public void testBooleanToInteger_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/boolean.xsd", getClass(), m_compilerErrors);

    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    final String[] xmlStrings;
    final String[] values = {
        "+012345", // not a boolean, to be encoded using exi:integer 
    };
    final String[] resultValues = {
        "12345", 
    };

    int i;
    xmlStrings = new String[values.length];
    for (i = 0; i < values.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + values[i] + "</foo:A>\n";
    };
    
    Transmogrifier encoder = new Transmogrifier();
    EXIDecoder decoder = new EXIDecoder();
    
    encoder.setGrammarCache(grammarCache);
    decoder.setGrammarCache(grammarCache);
    
    QName[] datatypeRepresentationMap = new QName[2];
    datatypeRepresentationMap[0] = new QName("xsd:boolean", XmlUriConst.W3C_2001_XMLSCHEMA_URI);
    datatypeRepresentationMap[1] = new QName("exi:integer", ExiUriConst.W3C_2009_EXI_URI);

    encoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);
    decoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);

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
        
        EventDescription exiEvent;
        n_events = 0;
        n_texts = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          if (exiEvent.getEventKind() == EventDescription.EVENT_CH) {
            String stringValue = exiEvent.getCharacters().makeString();
            Assert.assertEquals(resultValues[i], stringValue);
            Assert.assertEquals(EventType.ITEM_SCHEMA_CH, exiEvent.getEventType().itemType);
            ++n_texts;
          }
        }
        Assert.assertEquals(1, n_texts);
        Assert.assertEquals(5, n_events);
      }
    }
  }

  /**
   * Use DTRM to represent xsd:int using exi:decimal.
   */
  public void testIntToDecimal_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/int.xsd", getClass(), m_compilerErrors);

    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    final String[] xmlStrings;
    final String[] values = {
        "+012345.67", // not an int, to be encoded using exi:decimal 
    };
    final String[] resultValues = {
        "12345.67", 
    };

    int i;
    xmlStrings = new String[values.length];
    for (i = 0; i < values.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + values[i] + "</foo:A>\n";
    };
    
    Transmogrifier encoder = new Transmogrifier();
    EXIDecoder decoder = new EXIDecoder();
    
    encoder.setGrammarCache(grammarCache);
    decoder.setGrammarCache(grammarCache);
    
    QName[] datatypeRepresentationMap = new QName[2];
    datatypeRepresentationMap[0] = new QName("xsd:int", XmlUriConst.W3C_2001_XMLSCHEMA_URI);
    datatypeRepresentationMap[1] = new QName("exi:decimal", ExiUriConst.W3C_2009_EXI_URI);

    encoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);
    decoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);

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
        
        EventDescription exiEvent;
        n_events = 0;
        n_texts = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          if (exiEvent.getEventKind() == EventDescription.EVENT_CH) {
            String stringValue = exiEvent.getCharacters().makeString();
            Assert.assertEquals(resultValues[i], stringValue);
            Assert.assertEquals(EventType.ITEM_SCHEMA_CH, exiEvent.getEventType().itemType);
            ++n_texts;
          }
        }
        Assert.assertEquals(1, n_texts);
        Assert.assertEquals(5, n_events);
      }
    }
  }

  /**
   * Use DTRM to represent xsd:byte using exi:decimal by sticking exi:decimal to xsd:int.
   */
  public void testIntToDecimal_02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/int.xsd", getClass(), m_compilerErrors);

    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    final String[] xmlStrings;
    final String[] values = {
        "+012345.67", // not an int, to be encoded using exi:decimal 
    };
    final String[] resultValues = {
        "12345.67", 
    };

    int i;
    xmlStrings = new String[values.length];
    for (i = 0; i < values.length; i++) {
      xmlStrings[i] = "<foo:Byte xmlns:foo='urn:foo'>" + values[i] + "</foo:Byte>\n";
    };
    
    Transmogrifier encoder = new Transmogrifier();
    EXIDecoder decoder = new EXIDecoder();
    
    encoder.setGrammarCache(grammarCache);
    decoder.setGrammarCache(grammarCache);
    
    QName[] datatypeRepresentationMap = new QName[2];
    datatypeRepresentationMap[0] = new QName("xsd:int", XmlUriConst.W3C_2001_XMLSCHEMA_URI);
    datatypeRepresentationMap[1] = new QName("exi:decimal", ExiUriConst.W3C_2009_EXI_URI);

    encoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);
    decoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);

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
        
        EventDescription exiEvent;
        n_events = 0;
        n_texts = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          if (exiEvent.getEventKind() == EventDescription.EVENT_CH) {
            String stringValue = exiEvent.getCharacters().makeString();
            Assert.assertEquals(resultValues[i], stringValue);
            Assert.assertEquals(EventType.ITEM_SCHEMA_CH, exiEvent.getEventType().itemType);
            ++n_texts;
          }
        }
        Assert.assertEquals(1, n_texts);
        Assert.assertEquals(5, n_events);
      }
    }
  }

  /**
   * Use DTRM entry does not affect the codecs of ancestor types. 
   */
  public void testIntToDecimal_03() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/long.xsd", getClass(), m_compilerErrors);

    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    final String xmlString = "<foo:Long xmlns:foo='urn:foo'>+012345.67</foo:Long>\n";
    
    Transmogrifier encoder = new Transmogrifier();
    
    encoder.setGrammarCache(grammarCache);
    
    QName[] datatypeRepresentationMap = new QName[2];
    datatypeRepresentationMap[0] = new QName("xsd:int", XmlUriConst.W3C_2001_XMLSCHEMA_URI);
    datatypeRepresentationMap[1] = new QName("exi:decimal", ExiUriConst.W3C_2009_EXI_URI);

    encoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);

    encoder.setOutputStream(new ByteArrayOutputStream());
    
    try {
      encoder.encode(new InputSource(new StringReader(xmlString)));
    }
    catch (TransmogrifierException eee) {
      Assert.assertEquals(TransmogrifierException.UNEXPECTED_CHARS, eee.getCode());
      return;
    }
    Assert.fail();
  }

  /**
   * Use DTRM to represent an enumerated value using exi:integer.
   */
  public void testEnumerationToInteger_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/enumeration.xsd", getClass(), m_compilerErrors);

    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    final String xmlString;
    final String value = "+012345"; // not any one of the enumerated values, to be encoded using exi:integer 
    final String resultValue = "12345"; 

    xmlString = "<foo:A xmlns:foo='urn:foo' xsi:type='foo:stringDerived' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>" + 
        value + "</foo:A>\n";
    
    Transmogrifier encoder = new Transmogrifier();
    EXIDecoder decoder = new EXIDecoder();
    
    encoder.setGrammarCache(grammarCache);
    decoder.setGrammarCache(grammarCache);
    
    QName[] datatypeRepresentationMap = new QName[2];
    datatypeRepresentationMap[0] = new QName("foo:stringDerived", "urn:foo");
    datatypeRepresentationMap[1] = new QName("exi:integer", ExiUriConst.W3C_2009_EXI_URI);

    encoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);
    decoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);

    for (AlignmentType alignment : Alignments) {
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      Scanner scanner;
      
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      byte[] bts;
      int n_events, n_texts;
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EventDescription exiEvent;
      n_events = 0;
      n_texts = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        if (exiEvent.getEventKind() == EventDescription.EVENT_CH) {
          String stringValue = exiEvent.getCharacters().makeString();
          Assert.assertEquals(resultValue, stringValue);
          Assert.assertEquals(EventType.ITEM_SCHEMA_CH, exiEvent.getEventType().itemType);
          ++n_texts;
        }
      }
      Assert.assertEquals(1, n_texts);
      Assert.assertEquals(6, n_events);
    }
  }
  
  /**
   * The codec used for an enumerated type is not *generally* affected by 
   * DTRM entry attached to its ancestral type. When such an ancestral type 
   * has enumerated values, however, it *does* affect the codec used for 
   * the enumerated type in question.
   */
  public void testEnumerationToInteger_02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/enumeration.xsd", getClass(), m_compilerErrors);

    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    final String xmlString;
    final String value = "+012345"; // not any one of the enumerated values, to be encoded using exi:integer 
    final String resultValue = "12345"; 

    xmlString = "<foo:A xmlns:foo='urn:foo' xsi:type='foo:stringDerived2' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>" + 
        value + "</foo:A>\n";
    
    Transmogrifier encoder = new Transmogrifier();
    EXIDecoder decoder = new EXIDecoder();
    
    encoder.setGrammarCache(grammarCache);
    decoder.setGrammarCache(grammarCache);
    
    QName[] datatypeRepresentationMap = new QName[2];
    datatypeRepresentationMap[0] = new QName("foo:stringDerived", "urn:foo");
    datatypeRepresentationMap[1] = new QName("exi:integer", ExiUriConst.W3C_2009_EXI_URI);

    encoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);
    decoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);

    for (AlignmentType alignment : Alignments) {
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      Scanner scanner;
      
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      byte[] bts;
      int n_events, n_texts;
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EventDescription exiEvent;
      n_events = 0;
      n_texts = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        if (exiEvent.getEventKind() == EventDescription.EVENT_CH) {
          String stringValue = exiEvent.getCharacters().makeString();
          Assert.assertEquals(resultValue, stringValue);
          Assert.assertEquals(EventType.ITEM_SCHEMA_CH, exiEvent.getEventType().itemType);
          ++n_texts;
        }
      }
      Assert.assertEquals(1, n_texts);
      Assert.assertEquals(6, n_events);
    }
  }

  /**
   * The codec used for an enumerated type is not affected by DTRM entry attached to
   * an ancester type.
   */
  public void testEnumerationToInteger_03() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/enumeration.xsd", getClass(), m_compilerErrors);

    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    final String xmlString;
    final String value = "Nagoya"; 
    final String resultValue = "Nagoya"; 

    xmlString = "<foo:A xmlns:foo='urn:foo' xsi:type='foo:stringDerived' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>" + 
        value + "</foo:A>\n";
    
    Transmogrifier encoder = new Transmogrifier();
    EXIDecoder decoder = new EXIDecoder();
    
    encoder.setGrammarCache(grammarCache);
    decoder.setGrammarCache(grammarCache);
    
    QName[] datatypeRepresentationMap = new QName[2];
    datatypeRepresentationMap[0] = new QName("xsd:string", XmlUriConst.W3C_2001_XMLSCHEMA_URI);
    datatypeRepresentationMap[1] = new QName("exi:integer", ExiUriConst.W3C_2009_EXI_URI);

    encoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);
    decoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);

    for (AlignmentType alignment : Alignments) {
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      Scanner scanner;
      
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      byte[] bts;
      int n_events, n_texts;
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EventDescription exiEvent;
      n_events = 0;
      n_texts = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        if (exiEvent.getEventKind() == EventDescription.EVENT_CH) {
          String stringValue = exiEvent.getCharacters().makeString();
          Assert.assertEquals(resultValue, stringValue);
          Assert.assertEquals(EventType.ITEM_SCHEMA_CH, exiEvent.getEventType().itemType);
          ++n_texts;
        }
      }
      Assert.assertEquals(1, n_texts);
      Assert.assertEquals(6, n_events);
    }
  }
  
  /**
   * Use header options to communicate DTRM.
   */
  public void testHeaderOptions() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/boolean.xsd", getClass(), m_compilerErrors);

    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    final String xmlString;
    final String[] values = {
        "+012345", // not a boolean, to be encoded using exi:integer
        "1267.43233E12", // not a boolean, to be encoded using exi:float
    };
    final String[] resultValues = {
        "12345", 
        "126743233E7",
    };

    xmlString = 
      "<Z>" + 
        "<foo:A xmlns:foo='urn:foo'>" + values[0] + "</foo:A>" +
        "<foo:C xmlns:foo='urn:foo'>" + values[1] + "</foo:C>" +
      "</Z>"; 
    
    Transmogrifier encoder = new Transmogrifier();
    EXIDecoder decoder = new EXIDecoder();
    
    encoder.setGrammarCache(grammarCache);
    decoder.setGrammarCache(grammarCache);
    
    QName[] datatypeRepresentationMap = new QName[4];
    datatypeRepresentationMap[0] = new QName("xsd:boolean", XmlUriConst.W3C_2001_XMLSCHEMA_URI);
    datatypeRepresentationMap[1] = new QName("exi:integer", ExiUriConst.W3C_2009_EXI_URI);
    datatypeRepresentationMap[2] = new QName("foo:trueType", "urn:foo");
    datatypeRepresentationMap[3] = new QName("exi:double", ExiUriConst.W3C_2009_EXI_URI);

    encoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 2);
    
    encoder.setOutputOptions(HeaderOptionsOutputType.lessSchemaId);

    for (AlignmentType alignment : Alignments) {
      encoder.setAlignmentType(alignment);
      
      Scanner scanner;
      
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      byte[] bts;
      int n_events, n_texts;
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();
      
      decoder.setInputStream(new ByteArrayInputStream(bts));
      scanner = decoder.processHeader();
      
      EventDescription exiEvent;
      n_events = 0;
      n_texts = 0;
      int i = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        if (exiEvent.getEventKind() == EventDescription.EVENT_CH) {
          String stringValue = exiEvent.getCharacters().makeString();
          Assert.assertEquals(resultValues[i++], stringValue);
          Assert.assertEquals(EventType.ITEM_SCHEMA_CH, exiEvent.getEventType().itemType);
          ++n_texts;
        }
      }
      Assert.assertEquals(2, n_texts);
      Assert.assertEquals(10, n_events);
    }
  }
  
  /**
   * Use DTRM to represent a list of decimals using exi:string.
   */
  public void testDecimalListToString_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/list.xsd", getClass(), m_compilerErrors);
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    
    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    final String[] xmlStrings;
    final String[] values = {
        "abc \n123", // not a list of decimal 
    };
    final String[] resultValues = {
        "abc \n123", 
    };

    int i;
    xmlStrings = new String[values.length];
    for (i = 0; i < values.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " + 
          "xsi:type='foo:listOfDecimal8Len4' >" + values[i] + "</foo:A>\n";
    };
    
    Transmogrifier encoder = new Transmogrifier();
    EXIDecoder decoder = new EXIDecoder();
    
    encoder.setGrammarCache(grammarCache);
    decoder.setGrammarCache(grammarCache);
    
    QName[] datatypeRepresentationMap = new QName[2];
    datatypeRepresentationMap[0] = new QName("foo:listOfDecimal8Len4", "urn:foo");
    datatypeRepresentationMap[1] = new QName("exi:string", ExiUriConst.W3C_2009_EXI_URI);

    // Try encoding without DTRM, which should fail.
    boolean caught = false;
    try {
      encoder.setOutputStream(new ByteArrayOutputStream());
      encoder.encode(new InputSource(new StringReader(xmlStrings[0])));
    }
    catch (TransmogrifierException te) {
      caught = true;
      Assert.assertEquals(TransmogrifierException.UNEXPECTED_CHARS, te.getCode());
    }
    Assert.assertTrue(caught);
    
    // Set DTRM
    encoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);
    decoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);

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
        
        EventDescription exiEvent;
        n_events = 0;
        n_texts = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          if (exiEvent.getEventKind() == EventDescription.EVENT_CH) {
            String stringValue = exiEvent.getCharacters().makeString();
            Assert.assertEquals(resultValues[i], stringValue);
            Assert.assertEquals(EventType.ITEM_SCHEMA_CH, exiEvent.getEventType().itemType);
            if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
              Assert.assertTrue(corpus.isSimpleType(scanner.currentState.contentDatatype));
              Assert.assertEquals("listOfDecimal8Len4", corpus.getNameOfType(scanner.currentState.contentDatatype));
              Assert.assertEquals("urn:foo", corpus.uris[corpus.getUriOfType(scanner.currentState.contentDatatype)]);
            }
            ++n_texts;
          }
        }
        Assert.assertEquals(1, n_texts);
        Assert.assertEquals(6, n_events);
      }
    }
  }

  /**
   * Use DTRM to represent a list of decimals using exi:string.
   * A DTRM entry is set to the base type of the type used in the document. 
   */
  public void testDecimalListToString_02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/list.xsd", getClass(), m_compilerErrors);
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    
    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    final String[] xmlStrings;
    final String[] values = {
        "abc \n123", // not a list of decimal 
    };
    final String[] resultValues = {
        "abc \n123", 
    };

    int i;
    xmlStrings = new String[values.length];
    for (i = 0; i < values.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " + 
          "xsi:type='foo:listOfDecimal8Len4' >" + values[i] + "</foo:A>\n";
    };
    
    Transmogrifier encoder = new Transmogrifier();
    EXIDecoder decoder = new EXIDecoder();
    
    encoder.setGrammarCache(grammarCache);
    decoder.setGrammarCache(grammarCache);
    
    QName[] datatypeRepresentationMap = new QName[2];
    datatypeRepresentationMap[0] = new QName("foo:listOfDecimal8", "urn:foo");
    datatypeRepresentationMap[1] = new QName("exi:string", ExiUriConst.W3C_2009_EXI_URI);

    // Try encoding without DTRM, which should fail.
    boolean caught = false;
    try {
      encoder.setOutputStream(new ByteArrayOutputStream());
      encoder.encode(new InputSource(new StringReader(xmlStrings[0])));
    }
    catch (TransmogrifierException te) {
      caught = true;
      Assert.assertEquals(TransmogrifierException.UNEXPECTED_CHARS, te.getCode());
    }
    Assert.assertTrue(caught);
    
    // Set DTRM
    encoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);
    decoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);

    for (AlignmentType alignment : Alignments) {
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);
      for (i = 0; i < xmlStrings.length; i++) {
        Scanner scanner;
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        encoder.setOutputStream(baos);
        
        encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
        
        final byte[] bts = baos.toByteArray();
        
        decoder.setInputStream(new ByteArrayInputStream(bts));
        scanner = decoder.processHeader();
        
        EventDescription exiEvent;
        int n_events = 0;
        int n_texts = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          if (exiEvent.getEventKind() == EventDescription.EVENT_CH) {
            String stringValue = exiEvent.getCharacters().makeString();
            Assert.assertEquals(resultValues[i], stringValue);
            Assert.assertEquals(EventType.ITEM_SCHEMA_CH, exiEvent.getEventType().itemType);
            if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
              final int contentDatatype = scanner.currentState.contentDatatype;
              Assert.assertTrue(corpus.isSimpleType(contentDatatype));
              Assert.assertEquals("listOfDecimal8Len4", corpus.getNameOfType(contentDatatype));
              Assert.assertEquals("urn:foo", corpus.uris[corpus.getUriOfType(contentDatatype)]);
            }
            ++n_texts;
          }
        }
        Assert.assertEquals(1, n_texts);
        Assert.assertEquals(6, n_events);
      }
    }
  }

  /**
   * Use DTRM to try to represent a value of union datatype exi:decimal.
   */
  public void testUnionToDecimal_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/union.xsd", getClass(), m_compilerErrors);
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    
    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    final String[] xmlStrings;
    final String[] values = {
        "abc", // not a decimal 
    };
    final String[] resultValues = {
        "abc", 
    };

    int i;
    xmlStrings = new String[values.length];
    for (i = 0; i < values.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " + 
          "xsi:type='foo:refType' >" + values[i] + "</foo:A>\n";
    };
    
    Transmogrifier encoder = new Transmogrifier();
    EXIDecoder decoder = new EXIDecoder();
    
    encoder.setGrammarCache(grammarCache);
    decoder.setGrammarCache(grammarCache);
    
    // Try encoding without DTRM, which succeeds
    for (AlignmentType alignment : Alignments) {
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);
      for (i = 0; i < xmlStrings.length; i++) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        encoder.setOutputStream(baos);
        encoder.encode(new InputSource(new StringReader(xmlStrings[0])));
    
        byte[] bts = baos.toByteArray();
    
        decoder.setInputStream(new ByteArrayInputStream(bts));
        Scanner scanner = decoder.processHeader();
    
        EventDescription exiEvent;
        int n_events = 0;
        int n_texts = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          if (exiEvent.getEventKind() == EventDescription.EVENT_CH) {
            String stringValue = exiEvent.getCharacters().makeString();
            Assert.assertEquals(resultValues[i], stringValue);
            Assert.assertEquals(EventType.ITEM_SCHEMA_CH, exiEvent.getEventType().itemType);
            if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
              Assert.assertTrue(corpus.isSimpleType(scanner.currentState.contentDatatype));
              Assert.assertEquals("refType", corpus.getNameOfType(scanner.currentState.contentDatatype));
              Assert.assertEquals("urn:foo", corpus.uris[corpus.getUriOfType(scanner.currentState.contentDatatype)]);
            }
            ++n_texts;
          }
        }
        Assert.assertEquals(1, n_texts);
        Assert.assertEquals(6, n_events);
      }
    }

    // Set DTRM
    QName[] datatypeRepresentationMap = new QName[2];
    datatypeRepresentationMap[0] = new QName("foo:refType", "urn:foo");
    datatypeRepresentationMap[1] = new QName("exi:decimal", ExiUriConst.W3C_2009_EXI_URI);
    
    encoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);
    decoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);

    for (AlignmentType alignment : Alignments) {
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);
      for (i = 0; i < xmlStrings.length; i++) {
        boolean caught = false;
        try {
          encoder.setOutputStream(new ByteArrayOutputStream());
          encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
        }
        catch (TransmogrifierException te) {
          caught = true;
          Assert.assertEquals(TransmogrifierException.UNEXPECTED_CHARS, te.getCode());
        }
        Assert.assertTrue(caught);
      }
    }
  }

  /**
   * Use DTRM to try to represent a value of union datatype exi:decimal.
   * A DTRM entry is set to the base type of the type used in the document.
   */
  public void testUnionToDecimal_02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/union.xsd", getClass(), m_compilerErrors);
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    
    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    final String[] xmlStrings;
    final String[] values = {
        "abc", // not a decimal 
    };
    final String[] resultValues = {
        "abc", 
    };

    int i;
    xmlStrings = new String[values.length];
    for (i = 0; i < values.length; i++) {
      xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " + 
          "xsi:type='foo:unionedEnum2' >" + values[i] + "</foo:A>\n";
    };
    
    Transmogrifier encoder = new Transmogrifier();
    EXIDecoder decoder = new EXIDecoder();
    
    encoder.setGrammarCache(grammarCache);
    decoder.setGrammarCache(grammarCache);
    
    // Try encoding without DTRM, which succeeds
    for (AlignmentType alignment : Alignments) {
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);
      for (i = 0; i < xmlStrings.length; i++) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        encoder.setOutputStream(baos);
        encoder.encode(new InputSource(new StringReader(xmlStrings[0])));
    
        byte[] bts = baos.toByteArray();
    
        decoder.setInputStream(new ByteArrayInputStream(bts));
        Scanner scanner = decoder.processHeader();
    
        EventDescription exiEvent;
        int n_events = 0;
        int n_texts = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          if (exiEvent.getEventKind() == EventDescription.EVENT_CH) {
            String stringValue = exiEvent.getCharacters().makeString();
            Assert.assertEquals(resultValues[i], stringValue);
            Assert.assertEquals(EventType.ITEM_SCHEMA_CH, exiEvent.getEventType().itemType);
            if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
              Assert.assertTrue(corpus.isSimpleType(scanner.currentState.contentDatatype));
              Assert.assertEquals("unionedEnum2", corpus.getNameOfType(scanner.currentState.contentDatatype));
              Assert.assertEquals("urn:foo", corpus.uris[corpus.getUriOfType(scanner.currentState.contentDatatype)]);
            }
            ++n_texts;
          }
        }
        Assert.assertEquals(1, n_texts);
        Assert.assertEquals(6, n_events);
      }
    }

    // Set DTRM
    QName[] datatypeRepresentationMap = new QName[2];
    datatypeRepresentationMap[0] = new QName("foo:refType", "urn:foo");
    datatypeRepresentationMap[1] = new QName("exi:decimal", ExiUriConst.W3C_2009_EXI_URI);
    
    encoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);
    decoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);

    for (AlignmentType alignment : Alignments) {
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);
      for (i = 0; i < xmlStrings.length; i++) {
        boolean caught = false;
        try {
          encoder.setOutputStream(new ByteArrayOutputStream());
          encoder.encode(new InputSource(new StringReader(xmlStrings[i])));
        }
        catch (TransmogrifierException te) {
          caught = true;
          Assert.assertEquals(TransmogrifierException.UNEXPECTED_CHARS, te.getCode());
        }
        Assert.assertTrue(caught);
      }
    }
  }

  /**
   * A DTRM entry at xsd:decimal does not affect xsd:byte encoding. 
   */
  public void testImperviousnessOfByte_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/interop/datatypes/nbitInteger/nbitInteger.xsd", getClass(), m_compilerErrors);

    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

    final String[] xmlStrings;
    final String[] values = {
        "33",  
    };
    final String[] resultValues = {
        "33", 
    };

    int i;
    xmlStrings = new String[values.length];
    for (i = 0; i < values.length; i++) {
      xmlStrings[i] = "<root><byte>" + values[i] + "</byte></root>\n";
    };
    
    Transmogrifier encoder = new Transmogrifier();
    EXIDecoder decoder = new EXIDecoder();
    
    encoder.setGrammarCache(grammarCache);
    decoder.setGrammarCache(grammarCache);
    
    QName[] datatypeRepresentationMap = new QName[2];
    datatypeRepresentationMap[0] = new QName("xsd:decimal", XmlUriConst.W3C_2001_XMLSCHEMA_URI);
    datatypeRepresentationMap[1] = new QName("exi:string", ExiUriConst.W3C_2009_EXI_URI);

    encoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);
    decoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);

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
        
        EventDescription exiEvent;
        n_events = 0;
        n_texts = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          if (exiEvent.getEventKind() == EventDescription.EVENT_CH) {
            String stringValue = exiEvent.getCharacters().makeString();
            Assert.assertEquals(resultValues[i], stringValue);
            Assert.assertEquals(EventType.ITEM_SCHEMA_CH, exiEvent.getEventType().itemType);
            if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
              int tp = scanner.currentState.contentDatatype;
              Assert.assertEquals(EXISchemaConst.BYTE_TYPE, corpus.getSerialOfType(tp));
            }
            Assert.assertEquals(Apparatus.CODEC_INTEGER, ApparatusUtil.getCodecID(scanner, EXISchemaConst.BYTE_TYPE));
            ++n_texts;
          }
        }
        Assert.assertEquals(1, n_texts);
        Assert.assertEquals(7, n_events);
      }
    }
  }
  
}
