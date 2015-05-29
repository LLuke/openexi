package org.openexi.sax;

import java.io.StringReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.ArrayList;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.LocatorImpl;

import junit.framework.Assert;

import org.w3c.exi.ttf.Event;
import org.w3c.exi.ttf.sax.SAXRecorder;

import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.common.XmlUriConst;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.schema.EXISchema;
import org.openexi.schema.EmptySchema;
import org.openexi.schema.TestBase;

import org.openexi.scomp.EXISchemaFactoryErrorMonitor;
import org.openexi.scomp.EXISchemaFactoryTestUtil;

public class EXIReaderTest extends TestBase {

  public EXIReaderTest(String name) {
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
   * Make use of ITEM_SCHEMA_NS that belongs to an ElementGrammar and ElementTagGrammar.
   * Note that the ITEM_SCHEMA_NS event in ElementTagGrammar cannot be exercised since
   * it never matches an namespace declaration instance. 
   * 
   * Schema:
   * <xsd:complexType name="F">
   *   <xsd:sequence>
   *   ...
   *   </xsd:sequence>
   *   <xsd:attribute ref="foo:aA" use="required"/>
   *   ...
   * </xsd:complexType>
   * 
   * <xsd:element name="F" type="foo:F" nillable="true"/>
   */
  public void testNamespaceDeclaration_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, 
        GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));
    
    final String xmlString = 
      "<F xsi:type='F' xmlns='urn:foo' xmlns:foo='urn:foo' " + 
      "   xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'" + 
      "   foo:aA='abc'>" + 
      "</F>\n";
    
    for (AlignmentType alignment : Alignments) {
      for (boolean preserveLexicalValues : new boolean[] { true, false }) {
        Transmogrifier encoder = new Transmogrifier();
        EXIReader decoder = new EXIReader();
        
        encoder.setAlignmentType(alignment);
        decoder.setAlignmentType(alignment);
  
        encoder.setPreserveLexicalValues(preserveLexicalValues);
        decoder.setPreserveLexicalValues(preserveLexicalValues);
  
        encoder.setGrammarCache(grammarCache);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        encoder.setOutputStream(baos);
        
        byte[] bts;
        
        encoder.encode(new InputSource(new StringReader(xmlString)));
        
        bts = baos.toByteArray();
        
        decoder.setGrammarCache(grammarCache);
        
        ArrayList<Event> exiEventList = new ArrayList<Event>();

        SAXRecorder saxRecorder = new SAXRecorder(exiEventList, true);
        decoder.setContentHandler(saxRecorder);
        decoder.setLexicalHandler(saxRecorder);
        decoder.parse(new InputSource(new ByteArrayInputStream(bts)));

        Assert.assertEquals(10, exiEventList.size());
    
        Event saxEvent;
    
        saxEvent = exiEventList.get(0);
        Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
        Assert.assertEquals("urn:foo", saxEvent.namespace);
        Assert.assertEquals("", saxEvent.name);

        saxEvent = exiEventList.get(1);
        Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
        Assert.assertEquals("urn:foo", saxEvent.namespace);
        Assert.assertEquals("foo", saxEvent.name);

        saxEvent = exiEventList.get(2);
        Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
        Assert.assertEquals("http://www.w3.org/2001/XMLSchema-instance", saxEvent.namespace);
        Assert.assertEquals("xsi", saxEvent.name);

        saxEvent = exiEventList.get(3);
        Assert.assertEquals(Event.START_ELEMENT, saxEvent.type);
        Assert.assertEquals("urn:foo", saxEvent.namespace);
        Assert.assertEquals("F", saxEvent.localName);
        Assert.assertEquals("F", saxEvent.name);

        saxEvent = exiEventList.get(4);
        Assert.assertEquals(Event.ATTRIBUTE, saxEvent.type);
        Assert.assertEquals("http://www.w3.org/2001/XMLSchema-instance", saxEvent.namespace);
        Assert.assertEquals("type", saxEvent.localName);
        Assert.assertEquals("xsi:type", saxEvent.name);
        Assert.assertEquals("F", saxEvent.stringValue);

        saxEvent = exiEventList.get(5);
        Assert.assertEquals(Event.ATTRIBUTE, saxEvent.type);
        Assert.assertEquals("urn:foo", saxEvent.namespace);
        Assert.assertEquals("aA", saxEvent.localName);
        Assert.assertEquals("foo:aA", saxEvent.name);
        Assert.assertEquals("abc", saxEvent.stringValue);

        saxEvent = exiEventList.get(6);
        Assert.assertEquals(Event.END_ELEMENT, saxEvent.type);
        Assert.assertEquals("urn:foo", saxEvent.namespace);
        Assert.assertEquals("F", saxEvent.localName);
        Assert.assertEquals("F", saxEvent.name);
        
        saxEvent = exiEventList.get(7);
        Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
        Assert.assertEquals("xsi", saxEvent.name);
        
        saxEvent = exiEventList.get(8);
        Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
        Assert.assertEquals("foo", saxEvent.name);
        
        saxEvent = exiEventList.get(9);
        Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
        Assert.assertEquals("", saxEvent.name);
      }
    }
  }
    
  /**
   * Use an unqualified attribute.
   */
  public void testNamespaceDeclaration_02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    final String xmlString = 
        "<B xmlns='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " + 
        "   xsi:type='extended_B2' aA='xyz'><AB>abc</AB>" +
        "</B>\n";

    for (EXISchema _corpus : new EXISchema[] { corpus, (EXISchema)null }) {
      
      final GrammarCache grammarCache = new GrammarCache(_corpus, 
          GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));
      
      for (AlignmentType alignment : Alignments) {
        for (boolean preserveLexicalValues : new boolean[] { true, false }) {
          Transmogrifier encoder = new Transmogrifier();
          EXIReader decoder = new EXIReader();
          
          encoder.setAlignmentType(alignment);
          decoder.setAlignmentType(alignment);
    
          encoder.setPreserveLexicalValues(preserveLexicalValues);
          decoder.setPreserveLexicalValues(preserveLexicalValues);
    
          encoder.setGrammarCache(grammarCache);
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          encoder.setOutputStream(baos);
          
          byte[] bts;
          
          encoder.encode(new InputSource(new StringReader(xmlString)));
          
          bts = baos.toByteArray();
          
          decoder.setGrammarCache(grammarCache);
          
          ArrayList<Event> exiEventList = new ArrayList<Event>();
  
          SAXRecorder saxRecorder = new SAXRecorder(exiEventList, true);
          decoder.setContentHandler(saxRecorder);
          decoder.setLexicalHandler(saxRecorder);
          decoder.parse(new InputSource(new ByteArrayInputStream(bts)));
  
          Assert.assertEquals(11, exiEventList.size());
      
          Event saxEvent;
      
          saxEvent = exiEventList.get(0);
          Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
          Assert.assertEquals("urn:foo", saxEvent.namespace);
          Assert.assertEquals("", saxEvent.name);
  
          saxEvent = exiEventList.get(1);
          Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
          Assert.assertEquals("http://www.w3.org/2001/XMLSchema-instance", saxEvent.namespace);
          Assert.assertEquals("xsi", saxEvent.name);
  
          saxEvent = exiEventList.get(2);
          Assert.assertEquals(Event.START_ELEMENT, saxEvent.type);
          Assert.assertEquals("urn:foo", saxEvent.namespace);
          Assert.assertEquals("B", saxEvent.localName);
          Assert.assertEquals("B", saxEvent.name);
  
          saxEvent = exiEventList.get(3);
          Assert.assertEquals(Event.ATTRIBUTE, saxEvent.type);
          Assert.assertEquals("http://www.w3.org/2001/XMLSchema-instance", saxEvent.namespace);
          Assert.assertEquals("type", saxEvent.localName);
          Assert.assertEquals("xsi:type", saxEvent.name);
          Assert.assertEquals("extended_B2", saxEvent.stringValue);
  
          saxEvent = exiEventList.get(4);
          Assert.assertEquals(Event.ATTRIBUTE, saxEvent.type);
          Assert.assertEquals("", saxEvent.namespace);
          Assert.assertEquals("aA", saxEvent.localName);
          Assert.assertEquals("aA", saxEvent.name);
          Assert.assertEquals("xyz", saxEvent.stringValue);

          saxEvent = exiEventList.get(5);
          Assert.assertEquals(Event.START_ELEMENT, saxEvent.type);
          Assert.assertEquals("urn:foo", saxEvent.namespace);
          Assert.assertEquals("AB", saxEvent.localName);
          Assert.assertEquals("AB", saxEvent.name);

          saxEvent = exiEventList.get(6);
          Assert.assertEquals(Event.CHARACTERS, saxEvent.type);
          Assert.assertEquals("abc", new String(saxEvent.charValue));

          saxEvent = exiEventList.get(7);
          Assert.assertEquals(Event.END_ELEMENT, saxEvent.type);
          Assert.assertEquals("urn:foo", saxEvent.namespace);
          Assert.assertEquals("AB", saxEvent.localName);
          Assert.assertEquals("AB", saxEvent.name);
          
          saxEvent = exiEventList.get(8);
          Assert.assertEquals(Event.END_ELEMENT, saxEvent.type);
          Assert.assertEquals("urn:foo", saxEvent.namespace);
          Assert.assertEquals("B", saxEvent.localName);
          Assert.assertEquals("B", saxEvent.name);
          
          saxEvent = exiEventList.get(9);
          Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
          Assert.assertEquals("xsi", saxEvent.name);
          
          saxEvent = exiEventList.get(10);
          Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
          Assert.assertEquals("", saxEvent.name);
        }
      }
    }
  }

  /**
   * Prefix "goo" that is used by an attribute is *not* bound.
   * 
   * <B xmlns='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'
   *    xsi:type='extended_B3' goo:aA='xyz'><AB>abc</AB></B>
   */
  public void testNamespaceDeclaration_03() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    
    for (EXISchema _corpus : new EXISchema[] { corpus, (EXISchema)null }) {
      final GrammarCache grammarCache = new GrammarCache(_corpus, 
          GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));
      
      Transmogrifier encoder = new Transmogrifier();
      
      encoder.setGrammarCache(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      SAXTransmogrifier saxEncoder = encoder.getSAXTransmogrifier();
      
      saxEncoder.setDocumentLocator(new LocatorImpl());
      saxEncoder.startDocument();
      saxEncoder.startPrefixMapping("", "urn:foo");
      saxEncoder.startPrefixMapping("xsi", XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI);
      
      AttributesImpl attributes = new AttributesImpl();
      attributes.addAttribute(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, "type", "xsi:type", "CDATA", "extended_B3");
      // prefix "goo" is not bound.
      attributes.addAttribute("urn:goo", "aA", "goo:aA", "CDATA", "xyz");
      try {
        saxEncoder.startElement("urn:foo", "B", "B", attributes);
      }
      catch (SAXException se) {
        TransmogrifierException te = (TransmogrifierException)se.getException();
        Assert.assertEquals(TransmogrifierException.PREFIX_NOT_BOUND, te.getCode());
        continue;
      }
      Assert.fail();
    }
  }
  
  /**
   * Prefix "xsi" that is used by an attribute (aA) is bound to a wrong namespace.
   * 
   * <B xmlns='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'
   *    xsi:type='extended_B3' goo:aA='xyz'><AB>abc</AB></B>
   */
  public void testNamespaceDeclaration_04() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());
    
    for (EXISchema _corpus : new EXISchema[] { corpus, (EXISchema)null }) {
      final GrammarCache grammarCache = new GrammarCache(_corpus, 
          GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));
      
      Transmogrifier encoder = new Transmogrifier();
      
      encoder.setGrammarCache(grammarCache);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      SAXTransmogrifier saxEncoder = encoder.getSAXTransmogrifier();
      
      saxEncoder.setDocumentLocator(new LocatorImpl());
      saxEncoder.startDocument();
      saxEncoder.startPrefixMapping("", "urn:foo");
      saxEncoder.startPrefixMapping("xsi", XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI);
      
      AttributesImpl attributes = new AttributesImpl();
      attributes.addAttribute(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, "type", "xsi:type", "CDATA", "extended_B3");
      // prefix "goo" is bound to "http://www.w3.org/2001/XMLSchema-instance" instead of "urn:goo".
      attributes.addAttribute("urn:goo", "aA", "xsi:aA", "CDATA", "xyz");
      try {
        saxEncoder.startElement("urn:foo", "B", "B", attributes);
      }
      catch (SAXException se) {
        TransmogrifierException te = (TransmogrifierException)se.getException();
        Assert.assertEquals(TransmogrifierException.PREFIX_BOUND_TO_ANOTHER_NAMESPACE, te.getCode());
        continue;
      }
      Assert.fail();
    }
  }
  
  /**
   * Schema:
   * <xsd:element name="AB" type="xsd:anySimpleType"/>
   *
   * Instance:
   * <AB xmlns="urn:foo" xsi:type="xsd:string" foo:aA="abc">xyz</AB>
   */
  public void testUndeclaredAttrWildcardAnyOfElementTagGrammar_withNS() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, 
        GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));
    
    final String xmlString = 
      "<foo:AB xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' \n" +
      "  xmlns:xsd='http://www.w3.org/2001/XMLSchema' \n" +
      "  xmlns:foo='urn:foo' xsi:type='xsd:string' foo:aA='abc'>" +
      "xyz</foo:AB>";
  
    for (AlignmentType alignment : Alignments) {
      for (boolean preserveLexicalValues : new boolean[] { true, false }) {
        Transmogrifier encoder = new Transmogrifier();
        EXIReader decoder = new EXIReader();
        
        encoder.setAlignmentType(alignment);
        decoder.setAlignmentType(alignment);
  
        encoder.setPreserveLexicalValues(preserveLexicalValues);
        decoder.setPreserveLexicalValues(preserveLexicalValues);
  
        encoder.setGrammarCache(grammarCache);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        encoder.setOutputStream(baos);
        
        byte[] bts;
        
        encoder.encode(new InputSource(new StringReader(xmlString)));
        
        bts = baos.toByteArray();
        
        decoder.setGrammarCache(grammarCache);
        
        ArrayList<Event> exiEventList = new ArrayList<Event>();
  
        SAXRecorder saxRecorder = new SAXRecorder(exiEventList, true);
        decoder.setContentHandler(saxRecorder);
        decoder.setLexicalHandler(saxRecorder);
        decoder.parse(new InputSource(new ByteArrayInputStream(bts)));
  
        Assert.assertEquals(11, exiEventList.size());
  
        Event saxEvent;
        
        saxEvent = exiEventList.get(0);
        Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
        Assert.assertEquals("http://www.w3.org/2001/XMLSchema-instance", saxEvent.namespace);
        Assert.assertEquals("xsi", saxEvent.name);
  
        saxEvent = exiEventList.get(1);
        Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
        Assert.assertEquals("http://www.w3.org/2001/XMLSchema", saxEvent.namespace);
        Assert.assertEquals("xsd", saxEvent.name);
  
        saxEvent = exiEventList.get(2);
        Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
        Assert.assertEquals("urn:foo", saxEvent.namespace);
        Assert.assertEquals("foo", saxEvent.name);
  
        saxEvent = exiEventList.get(3);
        Assert.assertEquals(Event.START_ELEMENT, saxEvent.type);
        Assert.assertEquals("urn:foo", saxEvent.namespace);
        Assert.assertEquals("AB", saxEvent.localName);
        Assert.assertEquals("foo:AB", saxEvent.name);
  
        saxEvent = exiEventList.get(4);
        Assert.assertEquals(Event.ATTRIBUTE, saxEvent.type);
        Assert.assertEquals("http://www.w3.org/2001/XMLSchema-instance", saxEvent.namespace);
        Assert.assertEquals("type", saxEvent.localName);
        Assert.assertEquals("xsi:type", saxEvent.name);
        Assert.assertEquals("xsd:string", saxEvent.stringValue);
  
        saxEvent = exiEventList.get(5);
        Assert.assertEquals(Event.ATTRIBUTE, saxEvent.type);
        Assert.assertEquals("urn:foo", saxEvent.namespace);
        Assert.assertEquals("aA", saxEvent.localName);
        Assert.assertEquals("foo:aA", saxEvent.name);
        Assert.assertEquals("abc", saxEvent.stringValue);
  
        saxEvent = exiEventList.get(6);
        Assert.assertEquals(Event.CHARACTERS, saxEvent.type);
        Assert.assertEquals("xyz", new String(saxEvent.charValue));
  
        saxEvent = exiEventList.get(7);
        Assert.assertEquals(Event.END_ELEMENT, saxEvent.type);
        Assert.assertEquals("urn:foo", saxEvent.namespace);
        Assert.assertEquals("AB", saxEvent.localName);
        Assert.assertEquals("foo:AB", saxEvent.name);
        
        saxEvent = exiEventList.get(8);
        Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
        Assert.assertEquals("foo", saxEvent.name);
        
        saxEvent = exiEventList.get(9);
        Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
        Assert.assertEquals("xsd", saxEvent.name);
        
        saxEvent = exiEventList.get(10);
        Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
        Assert.assertEquals("xsi", saxEvent.name);
      }
    }
  }

  /**
   * Schema:
   * <xsd:element name="AB" type="xsd:anySimpleType"/>
   *
   * Instance:
   * <AB xmlns="urn:foo" xsi:type="xsd:string" foo:aA="abc">xyz</AB>
   */
  public void testUndeclaredAttrWildcardAnyOfElementTagGrammar_withoutNS() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
    
    final String xmlString = 
      "<foo:AB xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' \n" +
      "  xmlns:xsd='http://www.w3.org/2001/XMLSchema' \n" +
      "  xmlns:foo='urn:foo' xsi:type='xsd:string' foo:aA='abc'>" +
      "xyz</foo:AB>";
  
    for (AlignmentType alignment : Alignments) {
      for (boolean preserveLexicalValues : new boolean[] { true, false }) {
        Transmogrifier encoder = new Transmogrifier();
        EXIReader decoder = new EXIReader();
        
        encoder.setAlignmentType(alignment);
        decoder.setAlignmentType(alignment);
  
        encoder.setPreserveLexicalValues(preserveLexicalValues);
        decoder.setPreserveLexicalValues(preserveLexicalValues);
  
        encoder.setGrammarCache(grammarCache);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        encoder.setOutputStream(baos);
        
        byte[] bts;
        
        encoder.encode(new InputSource(new StringReader(xmlString)));
        
        bts = baos.toByteArray();
        
        decoder.setGrammarCache(grammarCache);
        
        ArrayList<Event> exiEventList = new ArrayList<Event>();
  
        SAXRecorder saxRecorder = new SAXRecorder(exiEventList, true);
        decoder.setContentHandler(saxRecorder);
        decoder.setLexicalHandler(saxRecorder);
        decoder.parse(new InputSource(new ByteArrayInputStream(bts)));
  
        Assert.assertEquals(preserveLexicalValues ? 21 : 23, exiEventList.size());
  
        Event saxEvent;

        int n = 0;
        
        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
        Assert.assertEquals(XmlUriConst.W3C_XML_1998_URI, saxEvent.namespace);
        Assert.assertEquals("xml", saxEvent.name);

        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, saxEvent.namespace);
        Assert.assertEquals("xsi", saxEvent.name);

        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI, saxEvent.namespace);
        Assert.assertEquals("xsd", saxEvent.name);

        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
        Assert.assertEquals("urn:eoo", saxEvent.namespace);
        Assert.assertEquals("s0", saxEvent.name);
        
        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
        Assert.assertEquals("urn:foo", saxEvent.namespace);
        Assert.assertEquals("s1", saxEvent.name);

        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
        Assert.assertEquals("urn:goo", saxEvent.namespace);
        Assert.assertEquals("s2", saxEvent.name);

        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
        Assert.assertEquals("urn:hoo", saxEvent.namespace);
        Assert.assertEquals("s3", saxEvent.name);

        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
        Assert.assertEquals("urn:ioo", saxEvent.namespace);
        Assert.assertEquals("s4", saxEvent.name);

        if (!preserveLexicalValues) {
          // REVISIT: This is unnecessary
          saxEvent = exiEventList.get(n++);
          Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
          Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI, saxEvent.namespace);
          Assert.assertEquals("p0", saxEvent.name);
        }

        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.START_ELEMENT, saxEvent.type);
        Assert.assertEquals("urn:foo", saxEvent.namespace);
        Assert.assertEquals("AB", saxEvent.localName);
        Assert.assertEquals("s1:AB", saxEvent.name);
  
        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.ATTRIBUTE, saxEvent.type);
        Assert.assertEquals("http://www.w3.org/2001/XMLSchema-instance", saxEvent.namespace);
        Assert.assertEquals("type", saxEvent.localName);
        Assert.assertEquals("xsi:type", saxEvent.name);
        Assert.assertEquals(preserveLexicalValues ? "xsd:string" : "p0:string", saxEvent.stringValue);
  
        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.ATTRIBUTE, saxEvent.type);
        Assert.assertEquals("urn:foo", saxEvent.namespace);
        Assert.assertEquals("aA", saxEvent.localName);
        Assert.assertEquals("s1:aA", saxEvent.name);
        Assert.assertEquals("abc", saxEvent.stringValue);
  
        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.CHARACTERS, saxEvent.type);
        Assert.assertEquals("xyz", new String(saxEvent.charValue));
  
        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.END_ELEMENT, saxEvent.type);
        Assert.assertEquals("urn:foo", saxEvent.namespace);
        Assert.assertEquals("AB", saxEvent.localName);
        Assert.assertEquals("s1:AB", saxEvent.name);

        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
        Assert.assertEquals("xml", saxEvent.name);

        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
        Assert.assertEquals("xsi", saxEvent.name);

        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
        Assert.assertEquals("xsd", saxEvent.name);

        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
        Assert.assertEquals("s0", saxEvent.name);
        
        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
        Assert.assertEquals("s1", saxEvent.name);

        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
        Assert.assertEquals("s2", saxEvent.name);

        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
        Assert.assertEquals("s3", saxEvent.name);

        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
        Assert.assertEquals("s4", saxEvent.name);

        if (!preserveLexicalValues) {
          // REVISIT: This is unnecessary
          saxEvent = exiEventList.get(n++);
          Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
          Assert.assertEquals("p0", saxEvent.name);
        }
        
        Assert.assertEquals(exiEventList.size(), n);
      }
    }
  }

  /**
   * Schema: 
   * <xsd:complexType name="restricted_B">
   *   <xsd:complexContent>
   *     <xsd:restriction base="foo:B">
   *       <xsd:sequence>
   *         <xsd:element ref="foo:AB"/>
   *         <xsd:element ref="foo:AC" minOccurs="0"/>
   *         <xsd:element ref="foo:AD" minOccurs="0"/>
   *       </xsd:sequence>
   *     </xsd:restriction>
   *   </xsd:complexContent>
   * </xsd:complexType>
   *
   * <xsd:element name="nillable_B" type="foo:B" nillable="true" />
   * 
   * Instance:
   * <nillable_B xmlns='urn:foo' xsi:nil='true' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'/>
   */
  public void testAcceptanceForNillableB() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, 
        GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));
    
    final String xmlString = 
      "<foo:nillable_B xmlns:foo='urn:foo' xsi:nil='  true   ' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'/>";

    for (AlignmentType alignment : Alignments) {
      for (boolean preserveLexicalValues : new boolean[] { true, false }) {

        Transmogrifier encoder = new Transmogrifier();
        EXIReader decoder = new EXIReader();
        
        encoder.setAlignmentType(alignment);
        decoder.setAlignmentType(alignment);
  
        encoder.setPreserveLexicalValues(preserveLexicalValues);
        decoder.setPreserveLexicalValues(preserveLexicalValues);
  
        encoder.setGrammarCache(grammarCache);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        encoder.setOutputStream(baos);
        
        byte[] bts;
        
        encoder.encode(new InputSource(new StringReader(xmlString)));
        
        bts = baos.toByteArray();
        
        decoder.setGrammarCache(grammarCache);
        
        ArrayList<Event> exiEventList = new ArrayList<Event>();
  
        SAXRecorder saxRecorder = new SAXRecorder(exiEventList, true);
        decoder.setContentHandler(saxRecorder);
        decoder.setLexicalHandler(saxRecorder);
        decoder.parse(new InputSource(new ByteArrayInputStream(bts)));
  
        Assert.assertEquals(7, exiEventList.size());
  
        Event saxEvent;
  
        saxEvent = exiEventList.get(0);
        Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
        Assert.assertEquals("urn:foo", saxEvent.namespace);
        Assert.assertEquals("foo", saxEvent.name);
  
        saxEvent = exiEventList.get(1);
        Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
        Assert.assertEquals("http://www.w3.org/2001/XMLSchema-instance", saxEvent.namespace);
        Assert.assertEquals("xsi", saxEvent.name);
  
        saxEvent = exiEventList.get(2);
        Assert.assertEquals(Event.START_ELEMENT, saxEvent.type);
        Assert.assertEquals("urn:foo", saxEvent.namespace);
        Assert.assertEquals("nillable_B", saxEvent.localName);
        Assert.assertEquals("foo:nillable_B", saxEvent.name);
  
        saxEvent = exiEventList.get(3);
        Assert.assertEquals(Event.ATTRIBUTE, saxEvent.type);
        Assert.assertEquals("http://www.w3.org/2001/XMLSchema-instance", saxEvent.namespace);
        Assert.assertEquals("nil", saxEvent.localName);
        Assert.assertEquals("xsi:nil", saxEvent.name);
        Assert.assertEquals(preserveLexicalValues ? "  true   " : "true", saxEvent.stringValue);
  
        saxEvent = exiEventList.get(4);
        Assert.assertEquals(Event.END_ELEMENT, saxEvent.type);
        Assert.assertEquals("urn:foo", saxEvent.namespace);
        Assert.assertEquals("nillable_B", saxEvent.localName);
        Assert.assertEquals("foo:nillable_B", saxEvent.name);
        
        saxEvent = exiEventList.get(5);
        Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
        Assert.assertEquals("xsi", saxEvent.name);
        
        saxEvent = exiEventList.get(6);
        Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
        Assert.assertEquals("foo", saxEvent.name);
      }
    }
  }
  
  /**
   * Exercise CM and PI in "all" group.
   * 
   * Schema:
   * <xsd:element name="C">
   *   <xsd:complexType>
   *     <xsd:all>
   *       <xsd:element ref="foo:AB" minOccurs="0" />
   *       <xsd:element ref="foo:AC" minOccurs="0" />
   *     </xsd:all>
   *   </xsd:complexType>
   * </xsd:element>
   *
   * Instance:
   * <C><AC/><!-- Good? --><?eg Good! ?></C><?eg Good? ?><!-- Good! -->
   */
  public void testCommentPI_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    short options = GrammarOptions.DEFAULT_OPTIONS; 
    options = GrammarOptions.addCM(options);
    options = GrammarOptions.addPI(options);
    
    GrammarCache grammarCache = new GrammarCache(corpus, options);
    
    final String xmlString = 
      "<C xmlns='urn:foo'><AC/><!-- Good? --><?eg Good! ?></C><?eg Good? ?><!-- Good! -->";
  
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      EXIReader decoder = new EXIReader();
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setGrammarCache(grammarCache);
      decoder.setGrammarCache(grammarCache);

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
  
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      byte[] bts = baos.toByteArray();

      ArrayList<Event> exiEventList = new ArrayList<Event>();
      SAXRecorder saxRecorder = new SAXRecorder(exiEventList, true);
      decoder.setContentHandler(saxRecorder);
      decoder.setLexicalHandler(saxRecorder);

      decoder.parse(new InputSource(new ByteArrayInputStream(bts)));
      
      Assert.assertEquals(24, exiEventList.size());
  
      Event saxEvent;
  
      int n = 0;
      
      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
      Assert.assertEquals(XmlUriConst.W3C_XML_1998_URI, saxEvent.namespace);
      Assert.assertEquals("xml", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, saxEvent.namespace);
      Assert.assertEquals("xsi", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI, saxEvent.namespace);
      Assert.assertEquals("xsd", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
      Assert.assertEquals("urn:eoo", saxEvent.namespace);
      Assert.assertEquals("s0", saxEvent.name);
      
      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
      Assert.assertEquals("urn:foo", saxEvent.namespace);
      Assert.assertEquals("s1", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
      Assert.assertEquals("urn:goo", saxEvent.namespace);
      Assert.assertEquals("s2", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
      Assert.assertEquals("urn:hoo", saxEvent.namespace);
      Assert.assertEquals("s3", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
      Assert.assertEquals("urn:ioo", saxEvent.namespace);
      Assert.assertEquals("s4", saxEvent.name);
      
      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.START_ELEMENT, saxEvent.type);
      Assert.assertEquals("urn:foo", saxEvent.namespace);
      Assert.assertEquals("C", saxEvent.localName);
      Assert.assertEquals("s1:C", saxEvent.name);
      
      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.START_ELEMENT, saxEvent.type);
      Assert.assertEquals("urn:foo", saxEvent.namespace);
      Assert.assertEquals("AC", saxEvent.localName);
      Assert.assertEquals("s1:AC", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_ELEMENT, saxEvent.type);
      Assert.assertEquals("urn:foo", saxEvent.namespace);
      Assert.assertEquals("AC", saxEvent.localName);
      Assert.assertEquals("s1:AC", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.COMMENT, saxEvent.type);
      Assert.assertEquals(" Good? ", new String(saxEvent.charValue));

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.PROCESSING_INSTRUCTION, saxEvent.type);
      Assert.assertEquals("eg", saxEvent.name);
      Assert.assertEquals("Good! ", saxEvent.stringValue);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_ELEMENT, saxEvent.type);
      Assert.assertEquals("urn:foo", saxEvent.namespace);
      Assert.assertEquals("C", saxEvent.localName);
      Assert.assertEquals("s1:C", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
      Assert.assertEquals("xml", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
      Assert.assertEquals("xsi", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
      Assert.assertEquals("xsd", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
      Assert.assertEquals("s0", saxEvent.name);
      
      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
      Assert.assertEquals("s1", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
      Assert.assertEquals("s2", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
      Assert.assertEquals("s3", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
      Assert.assertEquals("s4", saxEvent.name);
      
      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.PROCESSING_INSTRUCTION, saxEvent.type);
      Assert.assertEquals("eg", saxEvent.name);
      Assert.assertEquals("Good? ", saxEvent.stringValue);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.COMMENT, saxEvent.type);
      Assert.assertEquals(" Good! ", new String(saxEvent.charValue));
      
      Assert.assertEquals(exiEventList.size(), n);
    }
  }

  /**
   * Schema:
   * None available
   * 
   * Instance:
   * <None>&abc;&def;</None>
   */
  public void testBuiltinEntityRef() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/testStates/acceptance.xsd", getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, 
        GrammarOptions.addDTD(GrammarOptions.DEFAULT_OPTIONS));
    
    final String xmlString;
    byte[] bts;
    
    xmlString = "<!DOCTYPE None [ <!ENTITY ent SYSTEM 'er-entity.xml'> ]><None xmlns='urn:foo'>&ent;&ent;</None>\n";
    
    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      encoder.setResolveExternalGeneralEntities(false);
      EXIReader decoder = new EXIReader();
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setGrammarCache(grammarCache);
      decoder.setGrammarCache(grammarCache);
      
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      encoder.encode(new InputSource(new StringReader(xmlString)));
      
      bts = baos.toByteArray();

      ArrayList<Event> exiEventList = new ArrayList<Event>();
      SAXRecorder saxRecorder = new SAXRecorder(exiEventList, true);
      decoder.setContentHandler(saxRecorder);
      decoder.setLexicalHandler(saxRecorder);

      decoder.parse(new InputSource(new ByteArrayInputStream(bts)));
      
      Assert.assertEquals(22, exiEventList.size());
  
      Event saxEvent;
  
      int n = 0;
      
      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.DOCTYPE, saxEvent.type);
      Assert.assertEquals("None", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_DTD, saxEvent.type);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
      Assert.assertEquals(XmlUriConst.W3C_XML_1998_URI, saxEvent.namespace);
      Assert.assertEquals("xml", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, saxEvent.namespace);
      Assert.assertEquals("xsi", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI, saxEvent.namespace);
      Assert.assertEquals("xsd", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
      Assert.assertEquals("urn:eoo", saxEvent.namespace);
      Assert.assertEquals("s0", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
      Assert.assertEquals("urn:foo", saxEvent.namespace);
      Assert.assertEquals("s1", saxEvent.name);
      
      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
      Assert.assertEquals("urn:goo", saxEvent.namespace);
      Assert.assertEquals("s2", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
      Assert.assertEquals("urn:hoo", saxEvent.namespace);
      Assert.assertEquals("s3", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
      Assert.assertEquals("urn:ioo", saxEvent.namespace);
      Assert.assertEquals("s4", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.START_ELEMENT, saxEvent.type);
      Assert.assertEquals("urn:foo", saxEvent.namespace);
      Assert.assertEquals("None", saxEvent.localName);
      Assert.assertEquals("s1:None", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.UNEXPANDED_ENTITY, saxEvent.type);
      Assert.assertEquals("ent", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.UNEXPANDED_ENTITY, saxEvent.type);
      Assert.assertEquals("ent", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_ELEMENT, saxEvent.type);
      Assert.assertEquals("urn:foo", saxEvent.namespace);
      Assert.assertEquals("None", saxEvent.localName);
      Assert.assertEquals("s1:None", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
      Assert.assertEquals("xml", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
      Assert.assertEquals("xsi", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
      Assert.assertEquals("xsd", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
      Assert.assertEquals("s0", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
      Assert.assertEquals("s1", saxEvent.name);
      
      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
      Assert.assertEquals("s2", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
      Assert.assertEquals("s3", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
      Assert.assertEquals("s4", saxEvent.name);

      Assert.assertEquals(exiEventList.size(), n);
    }
  }
  
  /**
   * Schema:
   * None available
   * 
   * Instance:
   * <None>&abc;&def;</None>
   */
  public void testBuiltinEntityRefResolution() throws Exception {
    EXISchema corpus = EmptySchema.getEXISchema();

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addDTD(GrammarOptions.DEFAULT_OPTIONS));

    String xmlString;
    byte[] bts;

    xmlString = "<!DOCTYPE None [ <!ENTITY ent SYSTEM 'entity01.ent'> ]><None xmlns='urn:foo'>&ent;&ent;</None>\n";

    for (AlignmentType alignment : Alignments) {
      Transmogrifier encoder = new Transmogrifier();
      encoder.setResolveExternalGeneralEntities(true);
      EXIReader decoder = new EXIReader();
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setGrammarCache(grammarCache);
      decoder.setGrammarCache(grammarCache);
      
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);
      
      InputSource inputSource = new InputSource(new StringReader(xmlString));
      URL url = resolveSystemIdAsURL("/file.txt");
      inputSource.setSystemId(url.toString());
      encoder.encode(inputSource);
      
      bts = baos.toByteArray();

      ArrayList<Event> exiEventList = new ArrayList<Event>();
      SAXRecorder saxRecorder = new SAXRecorder(exiEventList, true);
      decoder.setContentHandler(saxRecorder);
      decoder.setLexicalHandler(saxRecorder);

      decoder.parse(new InputSource(new ByteArrayInputStream(bts)));
      
      Assert.assertEquals(13, exiEventList.size());

      Event saxEvent;

      int n = 0;

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.DOCTYPE, saxEvent.type);
      Assert.assertEquals("None", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_DTD, saxEvent.type);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
      Assert.assertEquals(XmlUriConst.W3C_XML_1998_URI, saxEvent.namespace);
      Assert.assertEquals("xml", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, saxEvent.namespace);
      Assert.assertEquals("xsi", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI, saxEvent.namespace);
      Assert.assertEquals("xsd", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
      Assert.assertEquals("urn:foo", saxEvent.namespace);
      Assert.assertEquals("p0", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.START_ELEMENT, saxEvent.type);
      Assert.assertEquals("urn:foo", saxEvent.namespace);
      Assert.assertEquals("None", saxEvent.localName);
      Assert.assertEquals("p0:None", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.CHARACTERS, saxEvent.type);
      Assert.assertEquals("ABCABC", new String(saxEvent.charValue));

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_ELEMENT, saxEvent.type);
      Assert.assertEquals("urn:foo", saxEvent.namespace);
      Assert.assertEquals("None", saxEvent.localName);
      Assert.assertEquals("p0:None", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
      Assert.assertEquals("xml", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
      Assert.assertEquals("xsi", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
      Assert.assertEquals("xsd", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
      Assert.assertEquals("p0", saxEvent.name);

      Assert.assertEquals(exiEventList.size(), n);
    }
  }

  /**
   */
  public void testBlockSize_01() throws Exception {

    GrammarCache grammarCache = new GrammarCache((EXISchema)null, GrammarOptions.DEFAULT_OPTIONS);
    
    for (AlignmentType alignment : new AlignmentType[] { AlignmentType.preCompress, AlignmentType.compress }) {
      Transmogrifier encoder = new Transmogrifier();
      EXIReader decoder = new EXIReader();
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setBlockSize(1);
      
      encoder.setGrammarCache(grammarCache);
      decoder.setGrammarCache(grammarCache);

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      URL url = resolveSystemIdAsURL("/interop/datatypes/string/indexed-10.xml");
      InputSource inputSource = new InputSource(url.toString());
      inputSource.setByteStream(url.openStream());

      encoder.encode(inputSource);
      
      byte[] bts = baos.toByteArray();

      ArrayList<Event> exiEventList = new ArrayList<Event>();
      SAXRecorder saxRecorder = new SAXRecorder(exiEventList, true);
      decoder.setContentHandler(saxRecorder);

      try {
        decoder.parse(new InputSource(new ByteArrayInputStream(bts)));
      }
      catch (Exception e) {
        continue;
      }
      Assert.fail();
    }
    
    for (AlignmentType alignment : new AlignmentType[] { AlignmentType.preCompress, AlignmentType.compress }) {
      Transmogrifier encoder = new Transmogrifier();
      EXIReader decoder = new EXIReader();
      
      encoder.setAlignmentType(alignment);
      decoder.setAlignmentType(alignment);

      encoder.setBlockSize(1);
      decoder.setBlockSize(1);
      
      encoder.setGrammarCache(grammarCache);
      decoder.setGrammarCache(grammarCache);

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      encoder.setOutputStream(baos);

      URL url = resolveSystemIdAsURL("/interop/datatypes/string/indexed-10.xml");
      InputSource inputSource = new InputSource(url.toString());
      inputSource.setByteStream(url.openStream());

      encoder.encode(inputSource);
      
      byte[] bts = baos.toByteArray();

      ArrayList<Event> exiEventList = new ArrayList<Event>();
      SAXRecorder saxRecorder = new SAXRecorder(exiEventList, true);
      decoder.setContentHandler(saxRecorder);

      decoder.parse(new InputSource(new ByteArrayInputStream(bts)));
      
      Assert.assertEquals(306, exiEventList.size());
  
      Event saxEvent;
  
      int n = 0;
      
      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
      Assert.assertEquals(XmlUriConst.W3C_XML_1998_URI, saxEvent.namespace);
      Assert.assertEquals("xml", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
      Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, saxEvent.namespace);
      Assert.assertEquals("xsi", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.START_ELEMENT, saxEvent.type);
      Assert.assertEquals("", saxEvent.namespace);
      Assert.assertEquals("root", saxEvent.localName);
      Assert.assertEquals("root", saxEvent.name);
      
      for (int i = 0; i < 100; i++) {
        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.START_ELEMENT, saxEvent.type);
        Assert.assertEquals("", saxEvent.namespace);
        Assert.assertEquals("a", saxEvent.localName);
        Assert.assertEquals("a", saxEvent.name);
        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.CHARACTERS, saxEvent.type);
        Assert.assertEquals(String.format("test%1$02d", i), new String(saxEvent.charValue));
        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.END_ELEMENT, saxEvent.type);
        Assert.assertEquals("", saxEvent.namespace);
        Assert.assertEquals("a", saxEvent.localName);
        Assert.assertEquals("a", saxEvent.name);
      }
      
      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_ELEMENT, saxEvent.type);
      Assert.assertEquals("", saxEvent.namespace);
      Assert.assertEquals("root", saxEvent.localName);
      Assert.assertEquals("root", saxEvent.name);
      
      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
      Assert.assertEquals("xml", saxEvent.name);

      saxEvent = exiEventList.get(n++);
      Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
      Assert.assertEquals("xsi", saxEvent.name);
      
      Assert.assertEquals(exiEventList.size(), n);
    }
  }

  /**
   * Test attributes.
   * Note SAXRecorder exercises SAX Attributes's getValue methods.
   */
  public void testAttributes() throws Exception {
    GrammarCache grammarCache = new GrammarCache(GrammarOptions.DEFAULT_OPTIONS);
    
    final String xmlString = 
      "<foo:A xmlns:foo='urn:foo' xmlns:goo='urn:goo' xmlns:hoo='urn:hoo' \n" + 
      "  foo:z='abc' goo:y='def' hoo:x='ghi' a='jkl'></foo:A>";
  
    for (AlignmentType alignment : Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIReader decoder = new EXIReader();
        
        encoder.setAlignmentType(alignment);
        decoder.setAlignmentType(alignment);
  
        encoder.setGrammarCache(grammarCache);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        encoder.setOutputStream(baos);
        
        byte[] bts;
        
        encoder.encode(new InputSource(new StringReader(xmlString)));
        
        bts = baos.toByteArray();
        
        decoder.setGrammarCache(grammarCache);
        
        ArrayList<Event> exiEventList = new ArrayList<Event>();
  
        SAXRecorder saxRecorder = new SAXRecorder(exiEventList, false);
        decoder.setContentHandler(saxRecorder);
        decoder.setLexicalHandler(saxRecorder);
        decoder.parse(new InputSource(new ByteArrayInputStream(bts)));
  
        Assert.assertEquals(16, exiEventList.size());
  
        Event saxEvent;

        int n = 0;
        
        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
        Assert.assertEquals(XmlUriConst.W3C_XML_1998_URI, saxEvent.namespace);
        Assert.assertEquals("xml", saxEvent.name);

        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
        Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, saxEvent.namespace);
        Assert.assertEquals("xsi", saxEvent.name);

        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
        Assert.assertEquals("urn:foo", saxEvent.namespace);
        Assert.assertEquals("p0", saxEvent.name);

        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
        Assert.assertEquals("urn:hoo", saxEvent.namespace);
        Assert.assertEquals("p1", saxEvent.name);
        
        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.NAMESPACE, saxEvent.type);
        Assert.assertEquals("urn:goo", saxEvent.namespace);
        Assert.assertEquals("p2", saxEvent.name);

        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.START_ELEMENT, saxEvent.type);
        Assert.assertEquals("urn:foo", saxEvent.namespace);
        Assert.assertEquals("A", saxEvent.localName);
        Assert.assertEquals("p0:A", saxEvent.name);
  
        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.ATTRIBUTE, saxEvent.type);
        Assert.assertEquals("", saxEvent.namespace);
        Assert.assertEquals("a", saxEvent.localName);
        Assert.assertEquals("a", saxEvent.name);
        Assert.assertEquals("jkl", saxEvent.stringValue);
  
        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.ATTRIBUTE, saxEvent.type);
        Assert.assertEquals("urn:hoo", saxEvent.namespace);
        Assert.assertEquals("x", saxEvent.localName);
        Assert.assertEquals("p1:x", saxEvent.name);
        Assert.assertEquals("ghi", saxEvent.stringValue);

        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.ATTRIBUTE, saxEvent.type);
        Assert.assertEquals("urn:goo", saxEvent.namespace);
        Assert.assertEquals("y", saxEvent.localName);
        Assert.assertEquals("p2:y", saxEvent.name);
        Assert.assertEquals("def", saxEvent.stringValue);

        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.ATTRIBUTE, saxEvent.type);
        Assert.assertEquals("urn:foo", saxEvent.namespace);
        Assert.assertEquals("z", saxEvent.localName);
        Assert.assertEquals("p0:z", saxEvent.name);
        Assert.assertEquals("abc", saxEvent.stringValue);

        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.END_ELEMENT, saxEvent.type);
        Assert.assertEquals("urn:foo", saxEvent.namespace);
        Assert.assertEquals("A", saxEvent.localName);
        Assert.assertEquals("p0:A", saxEvent.name);

        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
        Assert.assertEquals("xml", saxEvent.name);

        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
        Assert.assertEquals("xsi", saxEvent.name);

        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
        Assert.assertEquals("p2", saxEvent.name);

        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
        Assert.assertEquals("p1", saxEvent.name);
        
        saxEvent = exiEventList.get(n++);
        Assert.assertEquals(Event.END_NAMESPACE, saxEvent.type);
        Assert.assertEquals("p0", saxEvent.name);

        Assert.assertEquals(exiEventList.size(), n);
    }
  }
  
}
