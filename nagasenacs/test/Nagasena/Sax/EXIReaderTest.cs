using System;
using System.Collections.Generic;
using System.IO;
using System.Xml;
using NUnit.Framework;

using Org.System.Xml.Sax;
using Org.System.Xml.Sax.Helpers;

using Event = Org.W3C.Exi.Ttf.Event;
using SAXRecorder = Org.W3C.Exi.Ttf.Sax.SAXRecorder;

using AlignmentType = Nagasena.Proc.Common.AlignmentType;
using GrammarOptions = Nagasena.Proc.Common.GrammarOptions;
using XmlUriConst = Nagasena.Proc.Common.XmlUriConst;
using GrammarCache = Nagasena.Proc.Grammars.GrammarCache;
using EmptySchema = Nagasena.Schema.EmptySchema;
using EXISchema = Nagasena.Schema.EXISchema;
using TestBase = Nagasena.Schema.TestBase;

using EXISchemaFactoryTestUtil = Nagasena.Scomp.EXISchemaFactoryTestUtil;

namespace Nagasena.Sax {

  [TestFixture]
  public class EXIReaderTest : TestBase {

    private static readonly AlignmentType[] Alignments = new AlignmentType[] { 
      AlignmentType.bitPacked, 
      AlignmentType.byteAligned, 
      AlignmentType.preCompress, 
      AlignmentType.compress 
    };

    ///////////////////////////////////////////////////////////////////////////
    // Test cases
    ///////////////////////////////////////////////////////////////////////////

    /// <summary>
    /// Make use of ITEM_SCHEMA_NS that belongs to an ElementGrammar and ElementTagGrammar.
    /// Note that the ITEM_SCHEMA_NS event in ElementTagGrammar cannot be exercised since
    /// it never matches an namespace declaration instance. 
    /// 
    /// Schema:
    /// <xsd:complexType name="F">
    ///   <xsd:sequence>
    ///   ...
    ///   </xsd:sequence>
    ///   <xsd:attribute ref="foo:aA" use="required"/>
    ///   ...
    /// </xsd:complexType>
    /// 
    /// <xsd:element name="F" type="foo:F" nillable="true"/>
    /// </summary>
    [Test]
    public virtual void testNamespaceDeclaration_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.gram", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

      const string xmlString = 
        "<F xsi:type='F' xmlns='urn:foo' xmlns:foo='urn:foo' " +
        "   xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'" +
        "   foo:aA='abc'>" +
        "</F>\n";

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveLexicalValues in new bool[] { true, false }) {
          Transmogrifier encoder = new Transmogrifier();
          EXIReader decoder = new EXIReader();

          encoder.AlignmentType = alignment;
          decoder.AlignmentType = alignment;

          encoder.PreserveLexicalValues = preserveLexicalValues;
          decoder.PreserveLexicalValues = preserveLexicalValues;

          encoder.GrammarCache = grammarCache;
          MemoryStream baos = new MemoryStream();
          encoder.OutputStream = baos;

          byte[] bts;

          encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

          bts = baos.ToArray();

          decoder.GrammarCache = grammarCache;

          List<Event> exiEventList = new List<Event>();

          SAXRecorder saxRecorder = new SAXRecorder(exiEventList, true);
          decoder.ContentHandler = saxRecorder;
          decoder.LexicalHandler = saxRecorder;
          decoder.Parse(new MemoryStream(bts));

          Assert.AreEqual(10, exiEventList.Count);

          Event saxEvent;

          saxEvent = exiEventList[0];
          Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
          Assert.AreEqual("urn:foo", saxEvent.@namespace);
          Assert.AreEqual("", saxEvent.name);

          saxEvent = exiEventList[1];
          Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
          Assert.AreEqual("urn:foo", saxEvent.@namespace);
          Assert.AreEqual("foo", saxEvent.name);

          saxEvent = exiEventList[2];
          Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
          Assert.AreEqual("http://www.w3.org/2001/XMLSchema-instance", saxEvent.@namespace);
          Assert.AreEqual("xsi", saxEvent.name);

          saxEvent = exiEventList[3];
          Assert.AreEqual(Event.START_ELEMENT, saxEvent.type);
          Assert.AreEqual("urn:foo", saxEvent.@namespace);
          Assert.AreEqual("F", saxEvent.localName);
          Assert.AreEqual("F", saxEvent.name);

          saxEvent = exiEventList[4];
          Assert.AreEqual(Event.ATTRIBUTE, saxEvent.type);
          Assert.AreEqual("http://www.w3.org/2001/XMLSchema-instance", saxEvent.@namespace);
          Assert.AreEqual("type", saxEvent.localName);
          Assert.AreEqual("xsi:type", saxEvent.name);
          Assert.AreEqual("F", saxEvent.stringValue);

          saxEvent = exiEventList[5];
          Assert.AreEqual(Event.ATTRIBUTE, saxEvent.type);
          Assert.AreEqual("urn:foo", saxEvent.@namespace);
          Assert.AreEqual("aA", saxEvent.localName);
          Assert.AreEqual("foo:aA", saxEvent.name);
          Assert.AreEqual("abc", saxEvent.stringValue);

          saxEvent = exiEventList[6];
          Assert.AreEqual(Event.END_ELEMENT, saxEvent.type);
          Assert.AreEqual("urn:foo", saxEvent.@namespace);
          Assert.AreEqual("F", saxEvent.localName);
          Assert.AreEqual("F", saxEvent.name);

          saxEvent = exiEventList[7];
          Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
          Assert.AreEqual("xsi", saxEvent.name);

          saxEvent = exiEventList[8];
          Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
          Assert.AreEqual("foo", saxEvent.name);

          saxEvent = exiEventList[9];
          Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
          Assert.AreEqual("", saxEvent.name);
        }
      }
    }

    /// <summary>
    /// Use an unqualified attribute.
    /// </summary>
    [Test]
    public virtual void testNamespaceDeclaration_02() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.gram", this);

      const string xmlString = 
        "<B xmlns='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " +
        "   xsi:type='extended_B2' aA='xyz'><AB>abc</AB>" +
        "</B>\n";

      foreach (EXISchema _corpus in new EXISchema[] { corpus, (EXISchema)null }) {

        GrammarCache grammarCache = new GrammarCache(_corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

        foreach (AlignmentType alignment in Alignments) {
          foreach (bool preserveLexicalValues in new bool[] { true, false }) {
            Transmogrifier encoder = new Transmogrifier();
            EXIReader decoder = new EXIReader();

            encoder.AlignmentType = alignment;
            decoder.AlignmentType = alignment;

            encoder.PreserveLexicalValues = preserveLexicalValues;
            decoder.PreserveLexicalValues = preserveLexicalValues;

            encoder.GrammarCache = grammarCache;
            MemoryStream baos = new MemoryStream();
            encoder.OutputStream = baos;

            byte[] bts;

            encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

            bts = baos.ToArray();

            decoder.GrammarCache = grammarCache;

            List<Event> exiEventList = new List<Event>();

            SAXRecorder saxRecorder = new SAXRecorder(exiEventList, true);
            decoder.ContentHandler = saxRecorder;
            decoder.LexicalHandler = saxRecorder;
            decoder.Parse(new MemoryStream(bts));

            Assert.AreEqual(11, exiEventList.Count);

            Event saxEvent;

            saxEvent = exiEventList[0];
            Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
            Assert.AreEqual("urn:foo", saxEvent.@namespace);
            Assert.AreEqual("", saxEvent.name);

            saxEvent = exiEventList[1];
            Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
            Assert.AreEqual("http://www.w3.org/2001/XMLSchema-instance", saxEvent.@namespace);
            Assert.AreEqual("xsi", saxEvent.name);

            saxEvent = exiEventList[2];
            Assert.AreEqual(Event.START_ELEMENT, saxEvent.type);
            Assert.AreEqual("urn:foo", saxEvent.@namespace);
            Assert.AreEqual("B", saxEvent.localName);
            Assert.AreEqual("B", saxEvent.name);

            saxEvent = exiEventList[3];
            Assert.AreEqual(Event.ATTRIBUTE, saxEvent.type);
            Assert.AreEqual("http://www.w3.org/2001/XMLSchema-instance", saxEvent.@namespace);
            Assert.AreEqual("type", saxEvent.localName);
            Assert.AreEqual("xsi:type", saxEvent.name);
            Assert.AreEqual("extended_B2", saxEvent.stringValue);

            saxEvent = exiEventList[4];
            Assert.AreEqual(Event.ATTRIBUTE, saxEvent.type);
            Assert.AreEqual("", saxEvent.@namespace);
            Assert.AreEqual("aA", saxEvent.localName);
            Assert.AreEqual("aA", saxEvent.name);
            Assert.AreEqual("xyz", saxEvent.stringValue);

            saxEvent = exiEventList[5];
            Assert.AreEqual(Event.START_ELEMENT, saxEvent.type);
            Assert.AreEqual("urn:foo", saxEvent.@namespace);
            Assert.AreEqual("AB", saxEvent.localName);
            Assert.AreEqual("AB", saxEvent.name);

            saxEvent = exiEventList[6];
            Assert.AreEqual(Event.CHARACTERS, saxEvent.type);
            Assert.AreEqual("abc", new string(saxEvent.charValue));

            saxEvent = exiEventList[7];
            Assert.AreEqual(Event.END_ELEMENT, saxEvent.type);
            Assert.AreEqual("urn:foo", saxEvent.@namespace);
            Assert.AreEqual("AB", saxEvent.localName);
            Assert.AreEqual("AB", saxEvent.name);

            saxEvent = exiEventList[8];
            Assert.AreEqual(Event.END_ELEMENT, saxEvent.type);
            Assert.AreEqual("urn:foo", saxEvent.@namespace);
            Assert.AreEqual("B", saxEvent.localName);
            Assert.AreEqual("B", saxEvent.name);

            saxEvent = exiEventList[9];
            Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
            Assert.AreEqual("xsi", saxEvent.name);

            saxEvent = exiEventList[10];
            Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
            Assert.AreEqual("", saxEvent.name);
          }
        }
      }
    }

    /// <summary>
    /// Prefix "goo" that is used by an attribute is *not* bound.
    /// 
    /// <B xmlns='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'
    ///    xsi:type='extended_B3' goo:aA='xyz'><AB>abc</AB></B>
    /// </summary>
    [Test]
    public virtual void testNamespaceDeclaration_03() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.gram", this);

      foreach (EXISchema _corpus in new EXISchema[] { corpus, (EXISchema)null }) {
        GrammarCache grammarCache = new GrammarCache(_corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

        Transmogrifier encoder = new Transmogrifier();

        encoder.GrammarCache = grammarCache;
        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        SAXTransmogrifier saxEncoder = encoder.SAXTransmogrifier;

        saxEncoder.SetDocumentLocator(new LocatorImpl());
        saxEncoder.StartDocument();
        saxEncoder.StartPrefixMapping("", "urn:foo");
        saxEncoder.StartPrefixMapping("xsi", XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI);

        AttributesImpl attributes = new AttributesImpl();
        attributes.AddAttribute(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, "type", "xsi:type", "CDATA", "extended_B3", true);
        // prefix "goo" is not bound.
        attributes.AddAttribute("urn:goo", "aA", "goo:aA", "CDATA", "xyz", true);
        try {
          saxEncoder.StartElement("urn:foo", "B", "B", attributes);
        }
        catch (SaxException se) {
          TransmogrifierException te = (TransmogrifierException)se.InnerException;
          Assert.AreEqual(TransmogrifierException.PREFIX_NOT_BOUND, te.Code);
          continue;
        }
        Assert.Fail();
      }
    }

    /// <summary>
    /// Prefix "xsi" that is used by an attribute (aA) is bound to a wrong namespace.
    /// 
    /// <B xmlns='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'
    ///    xsi:type='extended_B3' goo:aA='xyz'><AB>abc</AB></B>
    /// </summary>
    [Test]
    public virtual void testNamespaceDeclaration_04() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.gram", this);

      foreach (EXISchema _corpus in new EXISchema[] { corpus, (EXISchema)null }) {
        GrammarCache grammarCache = new GrammarCache(_corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

        Transmogrifier encoder = new Transmogrifier();

        encoder.GrammarCache = grammarCache;
        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        SAXTransmogrifier saxEncoder = encoder.SAXTransmogrifier;

        saxEncoder.SetDocumentLocator(new LocatorImpl());
        saxEncoder.StartDocument();
        saxEncoder.StartPrefixMapping("", "urn:foo");
        saxEncoder.StartPrefixMapping("xsi", XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI);

        AttributesImpl attributes = new AttributesImpl();
        attributes.AddAttribute(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, "type", "xsi:type", "CDATA", "extended_B3", true);
        // prefix "goo" is bound to "http://www.w3.org/2001/XMLSchema-instance" instead of "urn:goo".
        attributes.AddAttribute("urn:goo", "aA", "xsi:aA", "CDATA", "xyz", true);
        try {
          saxEncoder.StartElement("urn:foo", "B", "B", attributes);
        }
        catch (SaxException se) {
          TransmogrifierException te = (TransmogrifierException)se.InnerException;
          Assert.AreEqual(TransmogrifierException.PREFIX_BOUND_TO_ANOTHER_NAMESPACE, te.Code);
          continue;
        }
        Assert.Fail();
      }
    }

    /// <summary>
    /// Schema:
    /// <xsd:element name="AB" type="xsd:anySimpleType"/>
    /// 
    /// Instance:
    /// <AB xmlns="urn:foo" xsi:type="xsd:string" foo:aA="abc">xyz</AB>
    /// </summary>
    [Test]
    public virtual void testUndeclaredAttrWildcardAnyOfElementTagGrammar_withNS() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.gram", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

      const string xmlString = 
        "<foo:AB xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' \n" +
        "  xmlns:xsd='http://www.w3.org/2001/XMLSchema' \n" +
        "  xmlns:foo='urn:foo' xsi:type='xsd:string' foo:aA='abc'>" +
        "xyz</foo:AB>";

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveLexicalValues in new bool[] { true, false }) {
          Transmogrifier encoder = new Transmogrifier();
          EXIReader decoder = new EXIReader();

          encoder.AlignmentType = alignment;
          decoder.AlignmentType = alignment;

          encoder.PreserveLexicalValues = preserveLexicalValues;
          decoder.PreserveLexicalValues = preserveLexicalValues;

          encoder.GrammarCache = grammarCache;
          MemoryStream baos = new MemoryStream();
          encoder.OutputStream = baos;

          byte[] bts;

          encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

          bts = baos.ToArray();

          decoder.GrammarCache = grammarCache;

          List<Event> exiEventList = new List<Event>();

          SAXRecorder saxRecorder = new SAXRecorder(exiEventList, true);
          decoder.ContentHandler = saxRecorder;
          decoder.LexicalHandler = saxRecorder;
          decoder.Parse(new MemoryStream(bts));

          Assert.AreEqual(11, exiEventList.Count);

          Event saxEvent;

          saxEvent = exiEventList[0];
          Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
          Assert.AreEqual("http://www.w3.org/2001/XMLSchema-instance", saxEvent.@namespace);
          Assert.AreEqual("xsi", saxEvent.name);

          saxEvent = exiEventList[1];
          Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
          Assert.AreEqual("http://www.w3.org/2001/XMLSchema", saxEvent.@namespace);
          Assert.AreEqual("xsd", saxEvent.name);

          saxEvent = exiEventList[2];
          Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
          Assert.AreEqual("urn:foo", saxEvent.@namespace);
          Assert.AreEqual("foo", saxEvent.name);

          saxEvent = exiEventList[3];
          Assert.AreEqual(Event.START_ELEMENT, saxEvent.type);
          Assert.AreEqual("urn:foo", saxEvent.@namespace);
          Assert.AreEqual("AB", saxEvent.localName);
          Assert.AreEqual("foo:AB", saxEvent.name);

          saxEvent = exiEventList[4];
          Assert.AreEqual(Event.ATTRIBUTE, saxEvent.type);
          Assert.AreEqual("http://www.w3.org/2001/XMLSchema-instance", saxEvent.@namespace);
          Assert.AreEqual("type", saxEvent.localName);
          Assert.AreEqual("xsi:type", saxEvent.name);
          Assert.AreEqual("xsd:string", saxEvent.stringValue);

          saxEvent = exiEventList[5];
          Assert.AreEqual(Event.ATTRIBUTE, saxEvent.type);
          Assert.AreEqual("urn:foo", saxEvent.@namespace);
          Assert.AreEqual("aA", saxEvent.localName);
          Assert.AreEqual("foo:aA", saxEvent.name);
          Assert.AreEqual("abc", saxEvent.stringValue);

          saxEvent = exiEventList[6];
          Assert.AreEqual(Event.CHARACTERS, saxEvent.type);
          Assert.AreEqual("xyz", new string(saxEvent.charValue));

          saxEvent = exiEventList[7];
          Assert.AreEqual(Event.END_ELEMENT, saxEvent.type);
          Assert.AreEqual("urn:foo", saxEvent.@namespace);
          Assert.AreEqual("AB", saxEvent.localName);
          Assert.AreEqual("foo:AB", saxEvent.name);

          saxEvent = exiEventList[8];
          Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
          Assert.AreEqual("foo", saxEvent.name);

          saxEvent = exiEventList[9];
          Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
          Assert.AreEqual("xsd", saxEvent.name);

          saxEvent = exiEventList[10];
          Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
          Assert.AreEqual("xsi", saxEvent.name);
        }
      }
    }

    /// <summary>
    /// Schema:
    /// <xsd:element name="AB" type="xsd:anySimpleType"/>
    /// 
    /// Instance:
    /// <AB xmlns="urn:foo" xsi:type="xsd:string" foo:aA="abc">xyz</AB>
    /// </summary>
    [Test]
    public virtual void testUndeclaredAttrWildcardAnyOfElementTagGrammar_withoutNS() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.gram", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString = 
        "<foo:AB xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' \n" +
        "  xmlns:xsd='http://www.w3.org/2001/XMLSchema' \n" +
        "  xmlns:foo='urn:foo' xsi:type='xsd:string' foo:aA='abc'>" +
        "xyz</foo:AB>";

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveLexicalValues in new bool[] { true, false }) {
          Transmogrifier encoder = new Transmogrifier();
          EXIReader decoder = new EXIReader();

          encoder.AlignmentType = alignment;
          decoder.AlignmentType = alignment;

          encoder.PreserveLexicalValues = preserveLexicalValues;
          decoder.PreserveLexicalValues = preserveLexicalValues;

          encoder.GrammarCache = grammarCache;
          MemoryStream baos = new MemoryStream();
          encoder.OutputStream = baos;

          byte[] bts;

          encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

          bts = baos.ToArray();

          decoder.GrammarCache = grammarCache;

          List<Event> exiEventList = new List<Event>();

          SAXRecorder saxRecorder = new SAXRecorder(exiEventList, true);
          decoder.ContentHandler = saxRecorder;
          decoder.LexicalHandler = saxRecorder;
          decoder.Parse(new MemoryStream(bts));

          Assert.AreEqual(preserveLexicalValues ? 21 : 23, exiEventList.Count);

          Event saxEvent;

          int n = 0;

          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
          Assert.AreEqual(XmlUriConst.W3C_XML_1998_URI, saxEvent.@namespace);
          Assert.AreEqual("xml", saxEvent.name);

          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, saxEvent.@namespace);
          Assert.AreEqual("xsi", saxEvent.name);

          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_URI, saxEvent.@namespace);
          Assert.AreEqual("xsd", saxEvent.name);

          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
          Assert.AreEqual("urn:eoo", saxEvent.@namespace);
          Assert.AreEqual("s0", saxEvent.name);

          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
          Assert.AreEqual("urn:foo", saxEvent.@namespace);
          Assert.AreEqual("s1", saxEvent.name);

          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
          Assert.AreEqual("urn:goo", saxEvent.@namespace);
          Assert.AreEqual("s2", saxEvent.name);

          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
          Assert.AreEqual("urn:hoo", saxEvent.@namespace);
          Assert.AreEqual("s3", saxEvent.name);

          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
          Assert.AreEqual("urn:ioo", saxEvent.@namespace);
          Assert.AreEqual("s4", saxEvent.name);

          if (!preserveLexicalValues) {
            // REVISIT: This is unnecessary
            saxEvent = exiEventList[n++];
            Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
            Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_URI, saxEvent.@namespace);
            Assert.AreEqual("p0", saxEvent.name);
          }

          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.START_ELEMENT, saxEvent.type);
          Assert.AreEqual("urn:foo", saxEvent.@namespace);
          Assert.AreEqual("AB", saxEvent.localName);
          Assert.AreEqual("s1:AB", saxEvent.name);

          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.ATTRIBUTE, saxEvent.type);
          Assert.AreEqual("http://www.w3.org/2001/XMLSchema-instance", saxEvent.@namespace);
          Assert.AreEqual("type", saxEvent.localName);
          Assert.AreEqual("xsi:type", saxEvent.name);
          Assert.AreEqual(preserveLexicalValues ? "xsd:string" : "p0:string", saxEvent.stringValue);

          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.ATTRIBUTE, saxEvent.type);
          Assert.AreEqual("urn:foo", saxEvent.@namespace);
          Assert.AreEqual("aA", saxEvent.localName);
          Assert.AreEqual("s1:aA", saxEvent.name);
          Assert.AreEqual("abc", saxEvent.stringValue);

          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.CHARACTERS, saxEvent.type);
          Assert.AreEqual("xyz", new string(saxEvent.charValue));

          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.END_ELEMENT, saxEvent.type);
          Assert.AreEqual("urn:foo", saxEvent.@namespace);
          Assert.AreEqual("AB", saxEvent.localName);
          Assert.AreEqual("s1:AB", saxEvent.name);

          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
          Assert.AreEqual("xml", saxEvent.name);

          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
          Assert.AreEqual("xsi", saxEvent.name);

          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
          Assert.AreEqual("xsd", saxEvent.name);

          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
          Assert.AreEqual("s0", saxEvent.name);

          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
          Assert.AreEqual("s1", saxEvent.name);

          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
          Assert.AreEqual("s2", saxEvent.name);

          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
          Assert.AreEqual("s3", saxEvent.name);

          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
          Assert.AreEqual("s4", saxEvent.name);

          if (!preserveLexicalValues) {
            // REVISIT: This is unnecessary
            saxEvent = exiEventList[n++];
            Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
            Assert.AreEqual("p0", saxEvent.name);
          }

          Assert.AreEqual(exiEventList.Count, n);
        }
      }
    }

    /// <summary>
    /// Schema: 
    /// <xsd:complexType name="restricted_B">
    ///   <xsd:complexContent>
    ///     <xsd:restriction base="foo:B">
    ///       <xsd:sequence>
    ///         <xsd:element ref="foo:AB"/>
    ///         <xsd:element ref="foo:AC" minOccurs="0"/>
    ///         <xsd:element ref="foo:AD" minOccurs="0"/>
    ///       </xsd:sequence>
    ///     </xsd:restriction>
    ///   </xsd:complexContent>
    /// </xsd:complexType>
    /// 
    /// <xsd:element name="nillable_B" type="foo:B" nillable="true" />
    /// 
    /// Instance:
    /// <nillable_B xmlns='urn:foo' xsi:nil='true' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'/>
    /// </summary>
    [Test]
    public virtual void testAcceptanceForNillableB() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.gram", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

      const string xmlString = 
        "<foo:nillable_B xmlns:foo='urn:foo' xsi:nil='  true   ' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'/>";

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveLexicalValues in new bool[] { true, false }) {

          Transmogrifier encoder = new Transmogrifier();
          EXIReader decoder = new EXIReader();

          encoder.AlignmentType = alignment;
          decoder.AlignmentType = alignment;

          encoder.PreserveLexicalValues = preserveLexicalValues;
          decoder.PreserveLexicalValues = preserveLexicalValues;

          encoder.GrammarCache = grammarCache;
          MemoryStream baos = new MemoryStream();
          encoder.OutputStream = baos;

          byte[] bts;

          encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

          bts = baos.ToArray();

          decoder.GrammarCache = grammarCache;

          List<Event> exiEventList = new List<Event>();

          SAXRecorder saxRecorder = new SAXRecorder(exiEventList, true);
          decoder.ContentHandler = saxRecorder;
          decoder.LexicalHandler = saxRecorder;
          decoder.Parse(new MemoryStream(bts));

          Assert.AreEqual(7, exiEventList.Count);

          Event saxEvent;

          saxEvent = exiEventList[0];
          Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
          Assert.AreEqual("urn:foo", saxEvent.@namespace);
          Assert.AreEqual("foo", saxEvent.name);

          saxEvent = exiEventList[1];
          Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
          Assert.AreEqual("http://www.w3.org/2001/XMLSchema-instance", saxEvent.@namespace);
          Assert.AreEqual("xsi", saxEvent.name);

          saxEvent = exiEventList[2];
          Assert.AreEqual(Event.START_ELEMENT, saxEvent.type);
          Assert.AreEqual("urn:foo", saxEvent.@namespace);
          Assert.AreEqual("nillable_B", saxEvent.localName);
          Assert.AreEqual("foo:nillable_B", saxEvent.name);

          saxEvent = exiEventList[3];
          Assert.AreEqual(Event.ATTRIBUTE, saxEvent.type);
          Assert.AreEqual("http://www.w3.org/2001/XMLSchema-instance", saxEvent.@namespace);
          Assert.AreEqual("nil", saxEvent.localName);
          Assert.AreEqual("xsi:nil", saxEvent.name);
          Assert.AreEqual(preserveLexicalValues ? "  true   " : "true", saxEvent.stringValue);

          saxEvent = exiEventList[4];
          Assert.AreEqual(Event.END_ELEMENT, saxEvent.type);
          Assert.AreEqual("urn:foo", saxEvent.@namespace);
          Assert.AreEqual("nillable_B", saxEvent.localName);
          Assert.AreEqual("foo:nillable_B", saxEvent.name);

          saxEvent = exiEventList[5];
          Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
          Assert.AreEqual("xsi", saxEvent.name);

          saxEvent = exiEventList[6];
          Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
          Assert.AreEqual("foo", saxEvent.name);
        }
      }
    }

    /// <summary>
    /// Exercise CM and PI in "all" group.
    /// 
    /// Schema:
    /// <xsd:element name="C">
    ///   <xsd:complexType>
    ///     <xsd:all>
    ///       <xsd:element ref="foo:AB" minOccurs="0" />
    ///       <xsd:element ref="foo:AC" minOccurs="0" />
    ///     </xsd:all>
    ///   </xsd:complexType>
    /// </xsd:element>
    /// 
    /// Instance:
    /// <C><AC/><!-- Good? --><?eg Good! ?></C><?eg Good? ?><!-- Good! -->
    /// </summary>
    [Test]
    public virtual void testCommentPI_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.gram", this);

      short options = GrammarOptions.DEFAULT_OPTIONS;
      options = GrammarOptions.addCM(options);
      options = GrammarOptions.addPI(options);

      GrammarCache grammarCache = new GrammarCache(corpus, options);

      const string xmlString = 
        "<C xmlns='urn:foo'><AC/><!-- Good? --><?eg Good! ?></C><?eg Good? ?><!-- Good! -->";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIReader decoder = new EXIReader();

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        decoder.GrammarCache = grammarCache;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        byte[] bts = baos.ToArray();

        List<Event> exiEventList = new List<Event>();
        SAXRecorder saxRecorder = new SAXRecorder(exiEventList, true);
        decoder.ContentHandler = saxRecorder;
        decoder.LexicalHandler = saxRecorder;

        decoder.Parse(new MemoryStream(bts));

        Assert.AreEqual(24, exiEventList.Count);

        Event saxEvent;

        int n = 0;

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
        Assert.AreEqual(XmlUriConst.W3C_XML_1998_URI, saxEvent.@namespace);
        Assert.AreEqual("xml", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
        Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, saxEvent.@namespace);
        Assert.AreEqual("xsi", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
        Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_URI, saxEvent.@namespace);
        Assert.AreEqual("xsd", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
        Assert.AreEqual("urn:eoo", saxEvent.@namespace);
        Assert.AreEqual("s0", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
        Assert.AreEqual("urn:foo", saxEvent.@namespace);
        Assert.AreEqual("s1", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
        Assert.AreEqual("urn:goo", saxEvent.@namespace);
        Assert.AreEqual("s2", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
        Assert.AreEqual("urn:hoo", saxEvent.@namespace);
        Assert.AreEqual("s3", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
        Assert.AreEqual("urn:ioo", saxEvent.@namespace);
        Assert.AreEqual("s4", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.START_ELEMENT, saxEvent.type);
        Assert.AreEqual("urn:foo", saxEvent.@namespace);
        Assert.AreEqual("C", saxEvent.localName);
        Assert.AreEqual("s1:C", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.START_ELEMENT, saxEvent.type);
        Assert.AreEqual("urn:foo", saxEvent.@namespace);
        Assert.AreEqual("AC", saxEvent.localName);
        Assert.AreEqual("s1:AC", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.END_ELEMENT, saxEvent.type);
        Assert.AreEqual("urn:foo", saxEvent.@namespace);
        Assert.AreEqual("AC", saxEvent.localName);
        Assert.AreEqual("s1:AC", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.COMMENT, saxEvent.type);
        Assert.AreEqual(" Good? ", new string(saxEvent.charValue));

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.PROCESSING_INSTRUCTION, saxEvent.type);
        Assert.AreEqual("eg", saxEvent.name);
        Assert.AreEqual("Good! ", saxEvent.stringValue);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.END_ELEMENT, saxEvent.type);
        Assert.AreEqual("urn:foo", saxEvent.@namespace);
        Assert.AreEqual("C", saxEvent.localName);
        Assert.AreEqual("s1:C", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
        Assert.AreEqual("xml", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
        Assert.AreEqual("xsi", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
        Assert.AreEqual("xsd", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
        Assert.AreEqual("s0", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
        Assert.AreEqual("s1", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
        Assert.AreEqual("s2", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
        Assert.AreEqual("s3", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
        Assert.AreEqual("s4", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.PROCESSING_INSTRUCTION, saxEvent.type);
        Assert.AreEqual("eg", saxEvent.name);
        Assert.AreEqual("Good? ", saxEvent.stringValue);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.COMMENT, saxEvent.type);
        Assert.AreEqual(" Good! ", new string(saxEvent.charValue));

        Assert.AreEqual(exiEventList.Count, n);
      }
    }

    /// <summary>
    /// Schema:
    /// None available
    /// 
    /// Instance:
    /// <None>&abc;&def;</None>
    /// </summary>
    [Test]
    public virtual void testBuiltinEntityRef() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.gram", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addDTD(GrammarOptions.DEFAULT_OPTIONS));

      string xmlString;
      byte[] bts;

      xmlString = "<!DOCTYPE None [ <!ENTITY ent SYSTEM 'er-entity.xml'> ]><None xmlns='urn:foo'>&ent;&ent;</None>\n";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIReader decoder = new EXIReader();

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        decoder.GrammarCache = grammarCache;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        String systemId = resolveSystemIdAsURL("/").ToString();

        XmlTextReader xmlTextReader;
        xmlTextReader = new XmlTextReader(systemId, string2Stream(xmlString));

        encoder.encode(xmlTextReader, systemId);

        bts = baos.ToArray();

        List<Event> exiEventList = new List<Event>();
        SAXRecorder saxRecorder = new SAXRecorder(exiEventList, true);
        decoder.ContentHandler = saxRecorder;
        decoder.LexicalHandler = saxRecorder;

        decoder.Parse(new MemoryStream(bts));

        Assert.AreEqual(22, exiEventList.Count);

        Event saxEvent;

        int n = 0;

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.DOCTYPE, saxEvent.type);
        Assert.AreEqual("None", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.END_DTD, saxEvent.type);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
        Assert.AreEqual(XmlUriConst.W3C_XML_1998_URI, saxEvent.@namespace);
        Assert.AreEqual("xml", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
        Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, saxEvent.@namespace);
        Assert.AreEqual("xsi", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
        Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_URI, saxEvent.@namespace);
        Assert.AreEqual("xsd", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
        Assert.AreEqual("urn:eoo", saxEvent.@namespace);
        Assert.AreEqual("s0", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
        Assert.AreEqual("urn:foo", saxEvent.@namespace);
        Assert.AreEqual("s1", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
        Assert.AreEqual("urn:goo", saxEvent.@namespace);
        Assert.AreEqual("s2", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
        Assert.AreEqual("urn:hoo", saxEvent.@namespace);
        Assert.AreEqual("s3", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
        Assert.AreEqual("urn:ioo", saxEvent.@namespace);
        Assert.AreEqual("s4", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.START_ELEMENT, saxEvent.type);
        Assert.AreEqual("urn:foo", saxEvent.@namespace);
        Assert.AreEqual("None", saxEvent.localName);
        Assert.AreEqual("s1:None", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.UNEXPANDED_ENTITY, saxEvent.type);
        Assert.AreEqual("ent", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.UNEXPANDED_ENTITY, saxEvent.type);
        Assert.AreEqual("ent", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.END_ELEMENT, saxEvent.type);
        Assert.AreEqual("urn:foo", saxEvent.@namespace);
        Assert.AreEqual("None", saxEvent.localName);
        Assert.AreEqual("s1:None", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
        Assert.AreEqual("xml", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
        Assert.AreEqual("xsi", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
        Assert.AreEqual("xsd", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
        Assert.AreEqual("s0", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
        Assert.AreEqual("s1", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
        Assert.AreEqual("s2", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
        Assert.AreEqual("s3", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
        Assert.AreEqual("s4", saxEvent.name);

        Assert.AreEqual(exiEventList.Count, n);
      }
    }

    /// <summary>
    /// Schema:
    /// None available
    /// 
    /// Instance:
    /// <None>&abc;&def;</None>
    /// </summary>
    [Test]
    public virtual void testBuiltinEntityRefResolution() {
      EXISchema corpus = EmptySchema.EXISchema;

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addDTD(GrammarOptions.DEFAULT_OPTIONS));

      string xmlString;
      byte[] bts;

      xmlString = "<!DOCTYPE None [ <!ENTITY ent SYSTEM 'entity01.ent'> ]><None xmlns='urn:foo'>&ent;&ent;</None>\n";

      XmlReaderSettings settings = new XmlReaderSettings();
      settings.ValidationType = System.Xml.ValidationType.None;
      settings.DtdProcessing = DtdProcessing.Parse;

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIReader decoder = new EXIReader();

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        decoder.GrammarCache = grammarCache;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        String systemId = resolveSystemIdAsURL("/whatever.xml").ToString();

        encoder.encode(XmlReader.Create(string2Stream(xmlString), settings, systemId), systemId);

        bts = baos.ToArray();

        List<Event> exiEventList = new List<Event>();
        SAXRecorder saxRecorder = new SAXRecorder(exiEventList, true);
        decoder.ContentHandler = saxRecorder;
        decoder.LexicalHandler = saxRecorder;

        decoder.Parse(new MemoryStream(bts));

        Assert.AreEqual(13, exiEventList.Count);

        Event saxEvent;

        int n = 0;

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.DOCTYPE, saxEvent.type);
        Assert.AreEqual("None", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.END_DTD, saxEvent.type);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
        Assert.AreEqual(XmlUriConst.W3C_XML_1998_URI, saxEvent.@namespace);
        Assert.AreEqual("xml", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
        Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, saxEvent.@namespace);
        Assert.AreEqual("xsi", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
        Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_URI, saxEvent.@namespace);
        Assert.AreEqual("xsd", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
        Assert.AreEqual("urn:foo", saxEvent.@namespace);
        Assert.AreEqual("p0", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.START_ELEMENT, saxEvent.type);
        Assert.AreEqual("urn:foo", saxEvent.@namespace);
        Assert.AreEqual("None", saxEvent.localName);
        Assert.AreEqual("p0:None", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.CHARACTERS, saxEvent.type);
        Assert.AreEqual("ABCABC", new String(saxEvent.charValue));

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.END_ELEMENT, saxEvent.type);
        Assert.AreEqual("urn:foo", saxEvent.@namespace);
        Assert.AreEqual("None", saxEvent.localName);
        Assert.AreEqual("p0:None", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
        Assert.AreEqual("xml", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
        Assert.AreEqual("xsi", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
        Assert.AreEqual("xsd", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
        Assert.AreEqual("p0", saxEvent.name);

        Assert.AreEqual(exiEventList.Count, n);
      }
    }

    [Test]
    public virtual void testBlockSize_01() {

      GrammarCache grammarCache = new GrammarCache((EXISchema)null, GrammarOptions.DEFAULT_OPTIONS);

      foreach (AlignmentType alignment in new AlignmentType[] { AlignmentType.preCompress, AlignmentType.compress }) {
        Transmogrifier encoder = new Transmogrifier();
        EXIReader decoder = new EXIReader();

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        encoder.BlockSize = 1;

        encoder.GrammarCache = grammarCache;
        decoder.GrammarCache = grammarCache;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        Uri url = resolveSystemIdAsURL("/interop/datatypes/string/indexed-10.xml");
        FileStream inputStream = new FileStream(url.LocalPath, FileMode.Open);
        InputSource inputSource = new InputSource<Stream>(inputStream, url.ToString());

        encoder.encode(inputSource);
        inputStream.Close();

        byte[] bts = baos.ToArray();

        List<Event> exiEventList = new List<Event>();
        SAXRecorder saxRecorder = new SAXRecorder(exiEventList, true);
        decoder.ContentHandler = saxRecorder;

        try {
          decoder.Parse(new MemoryStream(bts));
        }
        catch (Exception) {
          continue;
        }
        Assert.Fail();
      }

      foreach (AlignmentType alignment in new AlignmentType[] { AlignmentType.preCompress, AlignmentType.compress }) {
        Transmogrifier encoder = new Transmogrifier();
        EXIReader decoder = new EXIReader();

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        encoder.BlockSize = 1;
        decoder.BlockSize = 1;

        encoder.GrammarCache = grammarCache;
        decoder.GrammarCache = grammarCache;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        Uri url = resolveSystemIdAsURL("/interop/datatypes/string/indexed-10.xml");
        FileStream inputStream = new FileStream(url.LocalPath, FileMode.Open);
        InputSource inputSource = new InputSource<Stream>(inputStream, url.ToString());

        encoder.encode(inputSource);
        inputStream.Close();

        byte[] bts = baos.ToArray();

        List<Event> exiEventList = new List<Event>();
        SAXRecorder saxRecorder = new SAXRecorder(exiEventList, true);
        decoder.ContentHandler = saxRecorder;

        decoder.Parse(new MemoryStream(bts));

        Assert.AreEqual(306, exiEventList.Count);

        Event saxEvent;

        int n = 0;

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
        Assert.AreEqual(XmlUriConst.W3C_XML_1998_URI, saxEvent.@namespace);
        Assert.AreEqual("xml", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
        Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, saxEvent.@namespace);
        Assert.AreEqual("xsi", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.START_ELEMENT, saxEvent.type);
        Assert.AreEqual("", saxEvent.@namespace);
        Assert.AreEqual("root", saxEvent.localName);
        Assert.AreEqual("root", saxEvent.name);

        for (int i = 0; i < 100; i++) {
          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.START_ELEMENT, saxEvent.type);
          Assert.AreEqual("", saxEvent.@namespace);
          Assert.AreEqual("a", saxEvent.localName);
          Assert.AreEqual("a", saxEvent.name);
          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.CHARACTERS, saxEvent.type);
          Assert.AreEqual(string.Format("test{0:D2}", i), new string(saxEvent.charValue));
          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.END_ELEMENT, saxEvent.type);
          Assert.AreEqual("", saxEvent.@namespace);
          Assert.AreEqual("a", saxEvent.localName);
          Assert.AreEqual("a", saxEvent.name);
        }

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.END_ELEMENT, saxEvent.type);
        Assert.AreEqual("", saxEvent.@namespace);
        Assert.AreEqual("root", saxEvent.localName);
        Assert.AreEqual("root", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
        Assert.AreEqual("xml", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
        Assert.AreEqual("xsi", saxEvent.name);

        Assert.AreEqual(exiEventList.Count, n);
      }
    }

    /// <summary>
    /// Test attributes.
    /// Note SAXRecorder exercises SAX Attributes's getValue methods.
    /// </summary>
    [Test]
    public virtual void testAttributes() {
      GrammarCache grammarCache = new GrammarCache(GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString = 
        "<foo:A xmlns:foo='urn:foo' xmlns:goo='urn:goo' xmlns:hoo='urn:hoo' \n" +
        "  foo:z='abc' goo:y='def' hoo:x='ghi' a='jkl'></foo:A>";

      foreach (AlignmentType alignment in Alignments) {
          Transmogrifier encoder = new Transmogrifier();
          EXIReader decoder = new EXIReader();

          encoder.AlignmentType = alignment;
          decoder.AlignmentType = alignment;

          encoder.GrammarCache = grammarCache;
          MemoryStream baos = new MemoryStream();
          encoder.OutputStream = baos;

          byte[] bts;

          encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

          bts = baos.ToArray();

          decoder.GrammarCache = grammarCache;

          List<Event> exiEventList = new List<Event>();

          SAXRecorder saxRecorder = new SAXRecorder(exiEventList, false);
          decoder.ContentHandler = saxRecorder;
          decoder.LexicalHandler = saxRecorder;
          decoder.Parse(new MemoryStream(bts));

          Assert.AreEqual(16, exiEventList.Count);

          Event saxEvent;

          int n = 0;

          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
          Assert.AreEqual(XmlUriConst.W3C_XML_1998_URI, saxEvent.@namespace);
          Assert.AreEqual("xml", saxEvent.name);

          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, saxEvent.@namespace);
          Assert.AreEqual("xsi", saxEvent.name);

          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
          Assert.AreEqual("urn:foo", saxEvent.@namespace);
          Assert.AreEqual("p0", saxEvent.name);

          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
          Assert.AreEqual("urn:hoo", saxEvent.@namespace);
          Assert.AreEqual("p1", saxEvent.name);

          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
          Assert.AreEqual("urn:goo", saxEvent.@namespace);
          Assert.AreEqual("p2", saxEvent.name);

          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.START_ELEMENT, saxEvent.type);
          Assert.AreEqual("urn:foo", saxEvent.@namespace);
          Assert.AreEqual("A", saxEvent.localName);
          Assert.AreEqual("p0:A", saxEvent.name);

          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.ATTRIBUTE, saxEvent.type);
          Assert.AreEqual("", saxEvent.@namespace);
          Assert.AreEqual("a", saxEvent.localName);
          Assert.AreEqual("a", saxEvent.name);
          Assert.AreEqual("jkl", saxEvent.stringValue);

          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.ATTRIBUTE, saxEvent.type);
          Assert.AreEqual("urn:hoo", saxEvent.@namespace);
          Assert.AreEqual("x", saxEvent.localName);
          Assert.AreEqual("p1:x", saxEvent.name);
          Assert.AreEqual("ghi", saxEvent.stringValue);

          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.ATTRIBUTE, saxEvent.type);
          Assert.AreEqual("urn:goo", saxEvent.@namespace);
          Assert.AreEqual("y", saxEvent.localName);
          Assert.AreEqual("p2:y", saxEvent.name);
          Assert.AreEqual("def", saxEvent.stringValue);

          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.ATTRIBUTE, saxEvent.type);
          Assert.AreEqual("urn:foo", saxEvent.@namespace);
          Assert.AreEqual("z", saxEvent.localName);
          Assert.AreEqual("p0:z", saxEvent.name);
          Assert.AreEqual("abc", saxEvent.stringValue);

          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.END_ELEMENT, saxEvent.type);
          Assert.AreEqual("urn:foo", saxEvent.@namespace);
          Assert.AreEqual("A", saxEvent.localName);
          Assert.AreEqual("p0:A", saxEvent.name);

          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
          Assert.AreEqual("xml", saxEvent.name);

          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
          Assert.AreEqual("xsi", saxEvent.name);

          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
          Assert.AreEqual("p2", saxEvent.name);

          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
          Assert.AreEqual("p1", saxEvent.name);

          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
          Assert.AreEqual("p0", saxEvent.name);

          Assert.AreEqual(exiEventList.Count, n);
      }
    }

  }

}