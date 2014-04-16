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
import org.openexi.fujitsu.proc.common.EXIOptionsException;
import org.openexi.fujitsu.proc.common.GrammarOptions;
import org.openexi.fujitsu.proc.common.QName;
import org.openexi.fujitsu.proc.grammars.GrammarCache;
import org.openexi.fujitsu.proc.io.Scanner;
import org.openexi.fujitsu.proc.util.URIConst;
import org.openexi.fujitsu.sax.Transmogrifier;
import org.openexi.fujitsu.sax.TransmogrifierException;
import org.openexi.fujitsu.schema.EmptySchema;
import org.openexi.fujitsu.schema.EXISchema;
import org.openexi.fujitsu.schema.TestBase;
import org.openexi.fujitsu.scomp.EXISchemaFactoryErrorMonitor;
import org.openexi.fujitsu.scomp.EXISchemaFactoryTestUtil;
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
    datatypeRepresentationMap[0] = new QName("xsd:boolean", URIConst.W3C_2001_XMLSCHEMA_URI);
    datatypeRepresentationMap[1] = new QName("exi:integer", URIConst.W3C_2009_EXI_URI);

    Transmogrifier encoder;
    boolean caught;
    
    encoder = new Transmogrifier();
    encoder.setEXISchema(grammarCache);

    encoder.setOutputOptions(HeaderOptionsOutputType.none);
    encoder.setPreserveLexicalValues(true);
    encoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);

    /* ------------------------------------------------------------------------------- */

    encoder = new Transmogrifier();
    encoder.setEXISchema(grammarCache);

    encoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);
    encoder.setPreserveLexicalValues(true);
    encoder.setOutputOptions(HeaderOptionsOutputType.none);

    /* ------------------------------------------------------------------------------- */
    
    encoder = new Transmogrifier();
    encoder.setEXISchema(grammarCache);
    
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
    encoder.setEXISchema(grammarCache);

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
    encoder.setEXISchema(grammarCache);

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
    
    encoder.setEXISchema(grammarCache);
    decoder.setEXISchema(grammarCache);
    
    QName[] datatypeRepresentationMap = new QName[2];
    datatypeRepresentationMap[0] = new QName("xsd:boolean", URIConst.W3C_2001_XMLSCHEMA_URI);
    datatypeRepresentationMap[1] = new QName("exi:integer", URIConst.W3C_2009_EXI_URI);

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
        
        ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();

        EXIEvent exiEvent;
        n_events = 0;
        n_texts = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          if (exiEvent.getEventVariety() == EXIEvent.EVENT_CH) {
            String stringValue = exiEvent.getCharacters().makeString();
            Assert.assertEquals(resultValues[i], stringValue);
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
    
    encoder.setEXISchema(grammarCache);
    decoder.setEXISchema(grammarCache);
    
    QName[] datatypeRepresentationMap = new QName[2];
    datatypeRepresentationMap[0] = new QName("xsd:int", URIConst.W3C_2001_XMLSCHEMA_URI);
    datatypeRepresentationMap[1] = new QName("exi:decimal", URIConst.W3C_2009_EXI_URI);

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
        
        ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();

        EXIEvent exiEvent;
        n_events = 0;
        n_texts = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          if (exiEvent.getEventVariety() == EXIEvent.EVENT_CH) {
            String stringValue = exiEvent.getCharacters().makeString();
            Assert.assertEquals(resultValues[i], stringValue);
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
    
    encoder.setEXISchema(grammarCache);
    decoder.setEXISchema(grammarCache);
    
    QName[] datatypeRepresentationMap = new QName[2];
    datatypeRepresentationMap[0] = new QName("xsd:int", URIConst.W3C_2001_XMLSCHEMA_URI);
    datatypeRepresentationMap[1] = new QName("exi:decimal", URIConst.W3C_2009_EXI_URI);

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
        
        ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();

        EXIEvent exiEvent;
        n_events = 0;
        n_texts = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          if (exiEvent.getEventVariety() == EXIEvent.EVENT_CH) {
            String stringValue = exiEvent.getCharacters().makeString();
            Assert.assertEquals(resultValues[i], stringValue);
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
    
    encoder.setEXISchema(grammarCache);
    
    QName[] datatypeRepresentationMap = new QName[2];
    datatypeRepresentationMap[0] = new QName("xsd:int", URIConst.W3C_2001_XMLSCHEMA_URI);
    datatypeRepresentationMap[1] = new QName("exi:decimal", URIConst.W3C_2009_EXI_URI);

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
    
    encoder.setEXISchema(grammarCache);
    decoder.setEXISchema(grammarCache);
    
    QName[] datatypeRepresentationMap = new QName[2];
    datatypeRepresentationMap[0] = new QName("foo:stringDerived", "urn:foo");
    datatypeRepresentationMap[1] = new QName("exi:integer", URIConst.W3C_2009_EXI_URI);

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
      
      ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();

      EXIEvent exiEvent;
      n_events = 0;
      n_texts = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        if (exiEvent.getEventVariety() == EXIEvent.EVENT_CH) {
          String stringValue = exiEvent.getCharacters().makeString();
          Assert.assertEquals(resultValue, stringValue);
          Assert.assertTrue(exiEvent.getEventType().isSchemaInformed());
          ++n_texts;
        }
        exiEventList.add(exiEvent);
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
    
    encoder.setEXISchema(grammarCache);
    decoder.setEXISchema(grammarCache);
    
    QName[] datatypeRepresentationMap = new QName[2];
    datatypeRepresentationMap[0] = new QName("foo:stringDerived", "urn:foo");
    datatypeRepresentationMap[1] = new QName("exi:integer", URIConst.W3C_2009_EXI_URI);

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
      
      ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();

      EXIEvent exiEvent;
      n_events = 0;
      n_texts = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        if (exiEvent.getEventVariety() == EXIEvent.EVENT_CH) {
          String stringValue = exiEvent.getCharacters().makeString();
          Assert.assertEquals(resultValue, stringValue);
          Assert.assertTrue(exiEvent.getEventType().isSchemaInformed());
          ++n_texts;
        }
        exiEventList.add(exiEvent);
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
    
    encoder.setEXISchema(grammarCache);
    decoder.setEXISchema(grammarCache);
    
    QName[] datatypeRepresentationMap = new QName[2];
    datatypeRepresentationMap[0] = new QName("xsd:string", URIConst.W3C_2001_XMLSCHEMA_URI);
    datatypeRepresentationMap[1] = new QName("exi:integer", URIConst.W3C_2009_EXI_URI);

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
      
      ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();

      EXIEvent exiEvent;
      n_events = 0;
      n_texts = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        if (exiEvent.getEventVariety() == EXIEvent.EVENT_CH) {
          String stringValue = exiEvent.getCharacters().makeString();
          Assert.assertEquals(resultValue, stringValue);
          Assert.assertTrue(exiEvent.getEventType().isSchemaInformed());
          ++n_texts;
        }
        exiEventList.add(exiEvent);
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
    
    encoder.setEXISchema(grammarCache);
    decoder.setEXISchema(grammarCache);
    
    QName[] datatypeRepresentationMap = new QName[4];
    datatypeRepresentationMap[0] = new QName("xsd:boolean", URIConst.W3C_2001_XMLSCHEMA_URI);
    datatypeRepresentationMap[1] = new QName("exi:integer", URIConst.W3C_2009_EXI_URI);
    datatypeRepresentationMap[2] = new QName("foo:trueType", "urn:foo");
    datatypeRepresentationMap[3] = new QName("exi:double", URIConst.W3C_2009_EXI_URI);

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
      
      ArrayList<EXIEvent> exiEventList = new ArrayList<EXIEvent>();

      EXIEvent exiEvent;
      n_events = 0;
      n_texts = 0;
      int i = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        if (exiEvent.getEventVariety() == EXIEvent.EVENT_CH) {
          String stringValue = exiEvent.getCharacters().makeString();
          Assert.assertEquals(resultValues[i++], stringValue);
          Assert.assertTrue(exiEvent.getEventType().isSchemaInformed());
          ++n_texts;
        }
        exiEventList.add(exiEvent);
      }
      Assert.assertEquals(2, n_texts);
      Assert.assertEquals(10, n_events);
    }
  }

}
