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
using EXISchema = Nagasena.Schema.EXISchema;
using TestBase = Nagasena.Schema.TestBase;

using EXISchemaFactoryTestUtil = Nagasena.Scomp.EXISchemaFactoryTestUtil;

namespace Nagasena.Sax {

  [TestFixture]
  public class XMLifierTest : TestBase {

    private static readonly AlignmentType[] Alignments = new AlignmentType[] { 
      AlignmentType.bitPacked, 
      AlignmentType.byteAligned, 
      AlignmentType.preCompress, 
      AlignmentType.compress 
    };

    ///////////////////////////////////////////////////////////////////////////
    // Test cases
    ///////////////////////////////////////////////////////////////////////////

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
          XMLifier decoder = new XMLifier();

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

          baos = new MemoryStream();
          decoder.Convert(new MemoryStream(bts), baos);

          String str = bytesToString(baos.ToArray());
          //System.Console.WriteLine(str);

          Assert.AreEqual(
            "<F xmlns=\"urn:foo\" xmlns:foo=\"urn:foo\" " + 
               "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " + 
               "xsi:type=\"F\" foo:aA=\"abc\"></F>", str);
        }
      }
    }

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
            XMLifier decoder = new XMLifier();

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

            baos = new MemoryStream();
            decoder.Convert(new MemoryStream(bts), baos);

            String str = bytesToString(baos.ToArray());
            //System.Console.WriteLine(str);

            Assert.AreEqual(
              "<B xmlns=\"urn:foo\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " + 
                 "xsi:type=\"extended_B2\" aA=\"xyz\"><AB>abc</AB></B>", str);
          }
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
          XMLifier decoder = new XMLifier();

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

          baos = new MemoryStream();
          decoder.Convert(new MemoryStream(bts), baos);

          String str = bytesToString(baos.ToArray());
          //System.Console.WriteLine(str);

          Assert.AreEqual(
            "<foo:AB xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " + 
              "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " + 
              "xmlns:foo=\"urn:foo\" xsi:type=\"xsd:string\" foo:aA=\"abc\">" + 
              "xyz</foo:AB>", str);
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
          XMLifier decoder = new XMLifier();

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

          baos = new MemoryStream();
          decoder.Convert(new MemoryStream(bts), baos);

          String str = bytesToString(baos.ToArray());
          //System.Console.WriteLine(str);

          Assert.AreEqual(preserveLexicalValues ? 
            "<s1:AB xmlns:xml=\"http://www.w3.org/XML/1998/namespace\" " + 
              "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " + 
              "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " + 
              "xmlns:s0=\"urn:eoo\" xmlns:s1=\"urn:foo\" xmlns:s2=\"urn:goo\" xmlns:s3=\"urn:hoo\" xmlns:s4=\"urn:ioo\" " + 
              "xsi:type=\"xsd:string\" s1:aA=\"abc\">xyz</s1:AB>" :
            // REVISIT: xmlns:p0="http://www.w3.org/2001/XMLSchema" is not necessary.
            "<s1:AB xmlns:xml=\"http://www.w3.org/XML/1998/namespace\" " + 
              "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " + 
              "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " + 
              "xmlns:s0=\"urn:eoo\" xmlns:s1=\"urn:foo\" xmlns:s2=\"urn:goo\" xmlns:s3=\"urn:hoo\" xmlns:s4=\"urn:ioo\" " + 
              "xmlns:p0=\"http://www.w3.org/2001/XMLSchema\" " + 
              "xsi:type=\"p0:string\" s1:aA=\"abc\">xyz</s1:AB>", str);
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
          XMLifier decoder = new XMLifier();

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

          baos = new MemoryStream();
          decoder.Convert(new MemoryStream(bts), baos);

          String str = bytesToString(baos.ToArray());
          //System.Console.WriteLine(str);

          Assert.AreEqual(preserveLexicalValues ?
            "<foo:nillable_B xmlns:foo=\"urn:foo\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " + 
              "xsi:nil=\"  true   \"></foo:nillable_B>" :
            "<foo:nillable_B xmlns:foo=\"urn:foo\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " + 
              "xsi:nil=\"true\"></foo:nillable_B>", str);
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
        XMLifier decoder = new XMLifier();

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        decoder.GrammarCache = grammarCache;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        byte[] bts = baos.ToArray();

        baos = new MemoryStream();
        decoder.Convert(new MemoryStream(bts), baos);

        String str = bytesToString(baos.ToArray());
        //System.Console.WriteLine(str);

        Assert.AreEqual(
          "<s1:C xmlns:xml=\"http://www.w3.org/XML/1998/namespace\" " + 
            "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " + 
            "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " + 
            "xmlns:s0=\"urn:eoo\" xmlns:s1=\"urn:foo\" xmlns:s2=\"urn:goo\" xmlns:s3=\"urn:hoo\" xmlns:s4=\"urn:ioo\">" + 
            "<s1:AC></s1:AC><!-- Good? --><?eg Good! ?></s1:C><?eg Good? ?><!-- Good! -->", str);
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
        XMLifier decoder = new XMLifier();

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

        baos = new MemoryStream();
        decoder.Convert(new MemoryStream(bts), baos);

        String str = bytesToString(baos.ToArray());
        //System.Console.WriteLine(str);

        // REVISIT: Recover entities.
        Assert.AreEqual(
          "<s1:None xmlns:xml=\"http://www.w3.org/XML/1998/namespace\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " + 
            "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " + 
            "xmlns:s0=\"urn:eoo\" xmlns:s1=\"urn:foo\" xmlns:s2=\"urn:goo\" xmlns:s3=\"urn:hoo\" xmlns:s4=\"urn:ioo\"></s1:None>", str);
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
        XMLifier decoder = new XMLifier();

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        byte[] bts;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

        decoder.GrammarCache = grammarCache;

        baos = new MemoryStream();
        decoder.Convert(new MemoryStream(bts), baos);

        String str = bytesToString(baos.ToArray());
        //System.Console.WriteLine(str);

        Assert.AreEqual(
          "<p0:A xmlns:xml=\"http://www.w3.org/XML/1998/namespace\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " + 
            "xmlns:p0=\"urn:foo\" xmlns:p1=\"urn:hoo\" xmlns:p2=\"urn:goo\" " + 
            "a=\"jkl\" p1:x=\"ghi\" p2:y=\"def\" p0:z=\"abc\"></p0:A>", str);
      }
    }

  }

}