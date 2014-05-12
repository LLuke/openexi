using System;
using System.IO;
using NUnit.Framework;

using Org.System.Xml.Sax;

using EXIDecoder = Nagasena.Proc.EXIDecoder;
using HeaderOptionsOutputType = Nagasena.Proc.HeaderOptionsOutputType;
using AlignmentType = Nagasena.Proc.Common.AlignmentType;
using EventType = Nagasena.Proc.Common.EventType;
using EventDescription = Nagasena.Proc.Common.EventDescription;
using EventDescription_Fields = Nagasena.Proc.Common.EventDescription_Fields;
using EXIOptionsException = Nagasena.Proc.Common.EXIOptionsException;
using GrammarOptions = Nagasena.Proc.Common.GrammarOptions;
using QName = Nagasena.Proc.Common.QName;
using XmlUriConst = Nagasena.Proc.Common.XmlUriConst;
using Apparatus = Nagasena.Proc.Grammars.Apparatus;
using ApparatusUtil = Nagasena.Proc.Grammars.ApparatusUtil;
using GrammarCache = Nagasena.Proc.Grammars.GrammarCache;
using Scanner = Nagasena.Proc.IO.Scanner;
using ExiUriConst = Nagasena.Proc.Util.ExiUriConst;
using EXISchemaConst = Nagasena.Schema.EXISchemaConst;
using EmptySchema = Nagasena.Schema.EmptySchema;
using EXISchema = Nagasena.Schema.EXISchema;
using TestBase = Nagasena.Schema.TestBase;
using EXISchemaFactoryTestUtil = Nagasena.Scomp.EXISchemaFactoryTestUtil;

namespace Nagasena.Sax {

  [TestFixture]
  public class DTRMTest : TestBase {

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
    /// Preserve.lexicalValues option and DTRM cannot be specified together in EXI header options.
    /// </summary>
    [Test]
    public virtual void testPreserveLexicalValues() {

      GrammarCache grammarCache = new GrammarCache(EmptySchema.EXISchema, GrammarOptions.STRICT_OPTIONS);

      QName[] datatypeRepresentationMap = new QName[2];
      datatypeRepresentationMap[0] = new QName("xsd:boolean", XmlUriConst.W3C_2001_XMLSCHEMA_URI);
      datatypeRepresentationMap[1] = new QName("exi:integer", ExiUriConst.W3C_2009_EXI_URI);

      Transmogrifier encoder;
      bool caught;

      encoder = new Transmogrifier();
      encoder.GrammarCache = grammarCache;

      encoder.OutputOptions = HeaderOptionsOutputType.none;
      encoder.PreserveLexicalValues = true;
      encoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);

      /* ------------------------------------------------------------------------------- */

      encoder = new Transmogrifier();
      encoder.GrammarCache = grammarCache;

      encoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);
      encoder.PreserveLexicalValues = true;
      encoder.OutputOptions = HeaderOptionsOutputType.none;

      /* ------------------------------------------------------------------------------- */

      encoder = new Transmogrifier();
      encoder.GrammarCache = grammarCache;

      encoder.OutputOptions = HeaderOptionsOutputType.lessSchemaId;
      encoder.PreserveLexicalValues = true;

      caught = false;
      try {
        encoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);
      }
      catch (EXIOptionsException) {
        caught = true;
      }
      Assert.IsTrue(caught);

      /* ------------------------------------------------------------------------------- */

      encoder = new Transmogrifier();
      encoder.GrammarCache = grammarCache;

      encoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);
      encoder.PreserveLexicalValues = true;

      caught = false;
      try {
        encoder.OutputOptions = HeaderOptionsOutputType.lessSchemaId;
      }
      catch (EXIOptionsException) {
        caught = true;
      }
      Assert.IsTrue(caught);

      /* ------------------------------------------------------------------------------- */

      encoder = new Transmogrifier();
      encoder.GrammarCache = grammarCache;

      encoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);
      encoder.OutputOptions = HeaderOptionsOutputType.lessSchemaId;

      caught = false;
      try {
        encoder.PreserveLexicalValues = true;
      }
      catch (EXIOptionsException) {
        caught = true;
      }
      Assert.IsTrue(caught);
    }

    /// <summary>
    /// Use DTRM to represent xsd:boolean using exi:integer.
    /// </summary>
    [Test]
    public virtual void testBooleanToInteger_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/boolean.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string[] xmlStrings;
      string[] values = new string[] { "+012345" };
      string[] resultValues = new string[] { "12345" };

      int i;
      xmlStrings = new string[values.Length];
      for (i = 0; i < values.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + values[i] + "</foo:A>\n";
      };

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      QName[] datatypeRepresentationMap = new QName[2];
      datatypeRepresentationMap[0] = new QName("xsd:boolean", XmlUriConst.W3C_2001_XMLSCHEMA_URI);
      datatypeRepresentationMap[1] = new QName("exi:integer", ExiUriConst.W3C_2009_EXI_URI);

      encoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);
      decoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);

      foreach (AlignmentType alignment in Alignments) {
        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;
        for (i = 0; i < xmlStrings.Length; i++) {
          Scanner scanner;

          MemoryStream baos = new MemoryStream();
          encoder.OutputStream = baos;

          byte[] bts;
          int n_events, n_texts;

          encoder.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));

          bts = baos.ToArray();

          decoder.InputStream = new MemoryStream(bts);
          scanner = decoder.processHeader();

          EventDescription exiEvent;
          n_events = 0;
          n_texts = 0;
          while ((exiEvent = scanner.nextEvent()) != null) {
            ++n_events;
            if (exiEvent.EventKind == EventDescription_Fields.EVENT_CH) {
              string stringValue = exiEvent.Characters.makeString();
              Assert.AreEqual(resultValues[i], stringValue);
              Assert.AreEqual(EventType.ITEM_SCHEMA_CH, exiEvent.getEventType().itemType);
              ++n_texts;
            }
          }
          Assert.AreEqual(1, n_texts);
          Assert.AreEqual(5, n_events);
        }
      }
    }

    /// <summary>
    /// Use DTRM to represent xsd:int using exi:decimal.
    /// </summary>
    [Test]
    public virtual void testIntToDecimal_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/int.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string[] xmlStrings;
      string[] values = new string[] { "+012345.67" };
      string[] resultValues = new string[] { "12345.67" };

      int i;
      xmlStrings = new string[values.Length];
      for (i = 0; i < values.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + values[i] + "</foo:A>\n";
      };

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      QName[] datatypeRepresentationMap = new QName[2];
      datatypeRepresentationMap[0] = new QName("xsd:int", XmlUriConst.W3C_2001_XMLSCHEMA_URI);
      datatypeRepresentationMap[1] = new QName("exi:decimal", ExiUriConst.W3C_2009_EXI_URI);

      encoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);
      decoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);

      foreach (AlignmentType alignment in Alignments) {
        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;
        for (i = 0; i < xmlStrings.Length; i++) {
          Scanner scanner;

          MemoryStream baos = new MemoryStream();
          encoder.OutputStream = baos;

          byte[] bts;
          int n_events, n_texts;

          encoder.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));

          bts = baos.ToArray();

          decoder.InputStream = new MemoryStream(bts);
          scanner = decoder.processHeader();

          EventDescription exiEvent;
          n_events = 0;
          n_texts = 0;
          while ((exiEvent = scanner.nextEvent()) != null) {
            ++n_events;
            if (exiEvent.EventKind == EventDescription_Fields.EVENT_CH) {
              string stringValue = exiEvent.Characters.makeString();
              Assert.AreEqual(resultValues[i], stringValue);
              Assert.AreEqual(EventType.ITEM_SCHEMA_CH, exiEvent.getEventType().itemType);
              ++n_texts;
            }
          }
          Assert.AreEqual(1, n_texts);
          Assert.AreEqual(5, n_events);
        }
      }
    }

    /// <summary>
    /// Use DTRM to represent xsd:byte using exi:decimal by sticking exi:decimal to xsd:int.
    /// </summary>
    [Test]
    public virtual void testIntToDecimal_02() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/int.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string[] xmlStrings;
      string[] values = new string[] { "+012345.67" };
      string[] resultValues = new string[] { "12345.67" };

      int i;
      xmlStrings = new string[values.Length];
      for (i = 0; i < values.Length; i++) {
        xmlStrings[i] = "<foo:Byte xmlns:foo='urn:foo'>" + values[i] + "</foo:Byte>\n";
      };

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      QName[] datatypeRepresentationMap = new QName[2];
      datatypeRepresentationMap[0] = new QName("xsd:int", XmlUriConst.W3C_2001_XMLSCHEMA_URI);
      datatypeRepresentationMap[1] = new QName("exi:decimal", ExiUriConst.W3C_2009_EXI_URI);

      encoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);
      decoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);

      foreach (AlignmentType alignment in Alignments) {
        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;
        for (i = 0; i < xmlStrings.Length; i++) {
          Scanner scanner;

          MemoryStream baos = new MemoryStream();
          encoder.OutputStream = baos;

          byte[] bts;
          int n_events, n_texts;

          encoder.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));

          bts = baos.ToArray();

          decoder.InputStream = new MemoryStream(bts);
          scanner = decoder.processHeader();

          EventDescription exiEvent;
          n_events = 0;
          n_texts = 0;
          while ((exiEvent = scanner.nextEvent()) != null) {
            ++n_events;
            if (exiEvent.EventKind == EventDescription_Fields.EVENT_CH) {
              string stringValue = exiEvent.Characters.makeString();
              Assert.AreEqual(resultValues[i], stringValue);
              Assert.AreEqual(EventType.ITEM_SCHEMA_CH, exiEvent.getEventType().itemType);
              ++n_texts;
            }
          }
          Assert.AreEqual(1, n_texts);
          Assert.AreEqual(5, n_events);
        }
      }
    }

    /// <summary>
    /// Use DTRM entry does not affect the codecs of ancestor types. 
    /// </summary>
    [Test]
    public virtual void testIntToDecimal_03() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/long.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      const string xmlString = "<foo:Long xmlns:foo='urn:foo'>+012345.67</foo:Long>\n";

      Transmogrifier encoder = new Transmogrifier();

      encoder.GrammarCache = grammarCache;

      QName[] datatypeRepresentationMap = new QName[2];
      datatypeRepresentationMap[0] = new QName("xsd:int", XmlUriConst.W3C_2001_XMLSCHEMA_URI);
      datatypeRepresentationMap[1] = new QName("exi:decimal", ExiUriConst.W3C_2009_EXI_URI);

      encoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);

      encoder.OutputStream = new MemoryStream();

      try {
        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));
      }
      catch (TransmogrifierException eee) {
        Assert.AreEqual(TransmogrifierException.UNEXPECTED_CHARS, eee.Code);
        return;
      }
      Assert.Fail();
    }

    /// <summary>
    /// Use DTRM to represent an enumerated value using exi:integer.
    /// </summary>
    [Test]
    public virtual void testEnumerationToInteger_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string xmlString;
      const string value = "+012345"; // not any one of the enumerated values, to be encoded using exi:integer
      const string resultValue = "12345";

      xmlString = "<foo:A xmlns:foo='urn:foo' xsi:type='foo:stringDerived' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>" +
          value + "</foo:A>\n";

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      QName[] datatypeRepresentationMap = new QName[2];
      datatypeRepresentationMap[0] = new QName("foo:stringDerived", "urn:foo");
      datatypeRepresentationMap[1] = new QName("exi:integer", ExiUriConst.W3C_2009_EXI_URI);

      encoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);
      decoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);

      foreach (AlignmentType alignment in Alignments) {
        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        Scanner scanner;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        byte[] bts;
        int n_events, n_texts;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

        EventDescription exiEvent;
        n_events = 0;
        n_texts = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          if (exiEvent.EventKind == EventDescription_Fields.EVENT_CH) {
            string stringValue = exiEvent.Characters.makeString();
            Assert.AreEqual(resultValue, stringValue);
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH, exiEvent.getEventType().itemType);
            ++n_texts;
          }
        }
        Assert.AreEqual(1, n_texts);
        Assert.AreEqual(6, n_events);
      }
    }

    /// <summary>
    /// The codec used for an enumerated type is not *generally* affected by 
    /// DTRM entry attached to its ancestral type. When such an ancestral type 
    /// has enumerated values, however, it *does* affect the codec used for 
    /// the enumerated type in question.
    /// </summary>
    [Test]
    public virtual void testEnumerationToInteger_02() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string xmlString;
      const string value = "+012345"; // not any one of the enumerated values, to be encoded using exi:integer
      const string resultValue = "12345";

      xmlString = "<foo:A xmlns:foo='urn:foo' xsi:type='foo:stringDerived2' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>" +
          value + "</foo:A>\n";

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      QName[] datatypeRepresentationMap = new QName[2];
      datatypeRepresentationMap[0] = new QName("foo:stringDerived", "urn:foo");
      datatypeRepresentationMap[1] = new QName("exi:integer", ExiUriConst.W3C_2009_EXI_URI);

      encoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);
      decoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);

      foreach (AlignmentType alignment in Alignments) {
        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        Scanner scanner;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        byte[] bts;
        int n_events, n_texts;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

        EventDescription exiEvent;
        n_events = 0;
        n_texts = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          if (exiEvent.EventKind == EventDescription_Fields.EVENT_CH) {
            string stringValue = exiEvent.Characters.makeString();
            Assert.AreEqual(resultValue, stringValue);
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH, exiEvent.getEventType().itemType);
            ++n_texts;
          }
        }
        Assert.AreEqual(1, n_texts);
        Assert.AreEqual(6, n_events);
      }
    }

    /// <summary>
    /// The codec used for an enumerated type is not affected by DTRM entry attached to
    /// an ancester type.
    /// </summary>
    [Test]
    public virtual void testEnumerationToInteger_03() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string xmlString;
      const string value = "Nagoya";
      const string resultValue = "Nagoya";

      xmlString = "<foo:A xmlns:foo='urn:foo' xsi:type='foo:stringDerived' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>" +
          value + "</foo:A>\n";

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      QName[] datatypeRepresentationMap = new QName[2];
      datatypeRepresentationMap[0] = new QName("xsd:string", XmlUriConst.W3C_2001_XMLSCHEMA_URI);
      datatypeRepresentationMap[1] = new QName("exi:integer", ExiUriConst.W3C_2009_EXI_URI);

      encoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);
      decoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);

      foreach (AlignmentType alignment in Alignments) {
        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        Scanner scanner;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        byte[] bts;
        int n_events, n_texts;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

        EventDescription exiEvent;
        n_events = 0;
        n_texts = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          if (exiEvent.EventKind == EventDescription_Fields.EVENT_CH) {
            string stringValue = exiEvent.Characters.makeString();
            Assert.AreEqual(resultValue, stringValue);
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH, exiEvent.getEventType().itemType);
            ++n_texts;
          }
        }
        Assert.AreEqual(1, n_texts);
        Assert.AreEqual(6, n_events);
      }
    }

    /// <summary>
    /// Use header options to communicate DTRM.
    /// </summary>
    [Test]
    public virtual void testHeaderOptions() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/boolean.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string xmlString;
      String[] values = {
          "+012345", // not a boolean, to be encoded using exi:integer
          "1267.43233E12", // not a boolean, to be encoded using exi:float
      };
      String[] resultValues = {
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

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      QName[] datatypeRepresentationMap = new QName[4];
      datatypeRepresentationMap[0] = new QName("xsd:boolean", XmlUriConst.W3C_2001_XMLSCHEMA_URI);
      datatypeRepresentationMap[1] = new QName("exi:integer", ExiUriConst.W3C_2009_EXI_URI);
      datatypeRepresentationMap[2] = new QName("foo:trueType", "urn:foo");
      datatypeRepresentationMap[3] = new QName("exi:double", ExiUriConst.W3C_2009_EXI_URI);

      encoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 2);

      encoder.OutputOptions = HeaderOptionsOutputType.lessSchemaId;

      foreach (AlignmentType alignment in Alignments) {
        encoder.AlignmentType = alignment;

        Scanner scanner;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        byte[] bts;
        int n_events, n_texts;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

        EventDescription exiEvent;
        n_events = 0;
        n_texts = 0;
        int i = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          if (exiEvent.EventKind == EventDescription_Fields.EVENT_CH) {
            string stringValue = exiEvent.Characters.makeString();
            Assert.AreEqual(resultValues[i++], stringValue);
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH, exiEvent.getEventType().itemType);
            ++n_texts;
          }
        }
        Assert.AreEqual(2, n_texts);
        Assert.AreEqual(10, n_events);
      }
    }

    /// <summary>
    /// Use DTRM to represent a list of decimals using exi:string.
    /// </summary>
    [Test]
    public virtual void testDecimalListToString_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/list.xsc", this);
      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string[] xmlStrings;
      String[] values = {
          "abc \n123", // not a list of decimal 
      };
      String[] resultValues = {
          "abc \n123", 
      };

      int i;
      xmlStrings = new string[values.Length];
      for (i = 0; i < values.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " +
            "xsi:type='foo:listOfDecimal8Len4' >" + values[i] + "</foo:A>\n";
      };

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      QName[] datatypeRepresentationMap = new QName[2];
      datatypeRepresentationMap[0] = new QName("foo:listOfDecimal8Len4", "urn:foo");
      datatypeRepresentationMap[1] = new QName("exi:string", ExiUriConst.W3C_2009_EXI_URI);

      // Try encoding without DTRM, which should fail.
      bool caught = false;
      try {
        encoder.OutputStream = new MemoryStream();
        encoder.encode(new InputSource<Stream>(string2Stream(xmlStrings[0])));
      }
      catch (TransmogrifierException te) {
        caught = true;
        Assert.AreEqual(TransmogrifierException.UNEXPECTED_CHARS, te.Code);
      }
      Assert.IsTrue(caught);

      // Set DTRM
      encoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);
      decoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);

      foreach (AlignmentType alignment in Alignments) {
        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;
        for (i = 0; i < xmlStrings.Length; i++) {
          Scanner scanner;

          MemoryStream baos = new MemoryStream();
          encoder.OutputStream = baos;

          byte[] bts;
          int n_events, n_texts;

          encoder.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));

          bts = baos.ToArray();

          decoder.InputStream = new MemoryStream(bts);
          scanner = decoder.processHeader();

          EventDescription exiEvent;
          n_events = 0;
          n_texts = 0;
          while ((exiEvent = scanner.nextEvent()) != null) {
            ++n_events;
            if (exiEvent.EventKind == EventDescription_Fields.EVENT_CH) {
              string stringValue = exiEvent.Characters.makeString();
              Assert.AreEqual(resultValues[i], stringValue);
              Assert.AreEqual(EventType.ITEM_SCHEMA_CH, exiEvent.getEventType().itemType);
              if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
                Assert.IsTrue(corpus.isSimpleType(scanner.currentState.contentDatatype));
                Assert.AreEqual("listOfDecimal8Len4", corpus.getNameOfType(scanner.currentState.contentDatatype));
                Assert.AreEqual("urn:foo", corpus.uris[corpus.getUriOfType(scanner.currentState.contentDatatype)]);
              }
              ++n_texts;
            }
          }
          Assert.AreEqual(1, n_texts);
          Assert.AreEqual(6, n_events);
        }
      }
    }

    /// <summary>
    /// Use DTRM to represent a list of decimals using exi:string.
    /// A DTRM entry is set to the base type of the type used in the document. 
    /// </summary>
    [Test]
    public virtual void testDecimalListToString_02() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/list.xsc", this);
      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string[] xmlStrings;
      String[] values = {
          "abc \n123", // not a list of decimal 
      };
      String[] resultValues = {
          "abc \n123", 
      };

      int i;
      xmlStrings = new string[values.Length];
      for (i = 0; i < values.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " +
            "xsi:type='foo:listOfDecimal8Len4' >" + values[i] + "</foo:A>\n";
      };

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      QName[] datatypeRepresentationMap = new QName[2];
      datatypeRepresentationMap[0] = new QName("foo:listOfDecimal8", "urn:foo");
      datatypeRepresentationMap[1] = new QName("exi:string", ExiUriConst.W3C_2009_EXI_URI);

      // Try encoding without DTRM, which should fail.
      bool caught = false;
      try {
        encoder.OutputStream = new MemoryStream();
        encoder.encode(new InputSource<Stream>(string2Stream(xmlStrings[0])));
      }
      catch (TransmogrifierException te) {
        caught = true;
        Assert.AreEqual(TransmogrifierException.UNEXPECTED_CHARS, te.Code);
      }
      Assert.IsTrue(caught);

      // Set DTRM
      encoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);
      decoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);

      foreach (AlignmentType alignment in Alignments) {
        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;
        for (i = 0; i < xmlStrings.Length; i++) {
          Scanner scanner;

          MemoryStream baos = new MemoryStream();
          encoder.OutputStream = baos;

          encoder.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));

          byte[] bts = baos.ToArray();

          decoder.InputStream = new MemoryStream(bts);
          scanner = decoder.processHeader();

          EventDescription exiEvent;
          int n_events = 0;
          int n_texts = 0;
          while ((exiEvent = scanner.nextEvent()) != null) {
            ++n_events;
            if (exiEvent.EventKind == EventDescription_Fields.EVENT_CH) {
              string stringValue = exiEvent.Characters.makeString();
              Assert.AreEqual(resultValues[i], stringValue);
              Assert.AreEqual(EventType.ITEM_SCHEMA_CH, exiEvent.getEventType().itemType);
              if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
                int contentDatatype = scanner.currentState.contentDatatype;
                Assert.IsTrue(corpus.isSimpleType(contentDatatype));
                Assert.AreEqual("listOfDecimal8Len4", corpus.getNameOfType(contentDatatype));
                Assert.AreEqual("urn:foo", corpus.uris[corpus.getUriOfType(contentDatatype)]);
              }
              ++n_texts;
            }
          }
          Assert.AreEqual(1, n_texts);
          Assert.AreEqual(6, n_events);
        }
      }
    }

    /// <summary>
    /// Use DTRM to try to represent a value of union datatype exi:decimal.
    /// </summary>
    [Test]
    public virtual void testUnionToDecimal_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/union.xsc", this);
      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string[] xmlStrings;
      String[] values = {
          "abc", // not a decimal 
      };
      String[] resultValues = {
          "abc", 
      };

      int i;
      xmlStrings = new string[values.Length];
      for (i = 0; i < values.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " +
            "xsi:type='foo:refType' >" + values[i] + "</foo:A>\n";
      };

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      // Try encoding without DTRM, which succeeds
      foreach (AlignmentType alignment in Alignments) {
        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;
        for (i = 0; i < xmlStrings.Length; i++) {
          MemoryStream baos = new MemoryStream();
          encoder.OutputStream = baos;
          encoder.encode(new InputSource<Stream>(string2Stream(xmlStrings[0])));

          byte[] bts = baos.ToArray();

          decoder.InputStream = new MemoryStream(bts);
          Scanner scanner = decoder.processHeader();

          EventDescription exiEvent;
          int n_events = 0;
          int n_texts = 0;
          while ((exiEvent = scanner.nextEvent()) != null) {
            ++n_events;
            if (exiEvent.EventKind == EventDescription_Fields.EVENT_CH) {
              string stringValue = exiEvent.Characters.makeString();
              Assert.AreEqual(resultValues[i], stringValue);
              Assert.AreEqual(EventType.ITEM_SCHEMA_CH, exiEvent.getEventType().itemType);
              if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
                Assert.IsTrue(corpus.isSimpleType(scanner.currentState.contentDatatype));
                Assert.AreEqual("refType", corpus.getNameOfType(scanner.currentState.contentDatatype));
                Assert.AreEqual("urn:foo", corpus.uris[corpus.getUriOfType(scanner.currentState.contentDatatype)]);
              }
              ++n_texts;
            }
          }
          Assert.AreEqual(1, n_texts);
          Assert.AreEqual(6, n_events);
        }
      }

      // Set DTRM
      QName[] datatypeRepresentationMap = new QName[2];
      datatypeRepresentationMap[0] = new QName("foo:refType", "urn:foo");
      datatypeRepresentationMap[1] = new QName("exi:decimal", ExiUriConst.W3C_2009_EXI_URI);

      encoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);
      decoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);

      foreach (AlignmentType alignment in Alignments) {
        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;
        for (i = 0; i < xmlStrings.Length; i++) {
          bool caught = false;
          try {
            encoder.OutputStream = new MemoryStream();
            encoder.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));
          }
          catch (TransmogrifierException te) {
            caught = true;
            Assert.AreEqual(TransmogrifierException.UNEXPECTED_CHARS, te.Code);
          }
          Assert.IsTrue(caught);
        }
      }
    }

    /// <summary>
    /// Use DTRM to try to represent a value of union datatype exi:decimal.
    /// A DTRM entry is set to the base type of the type used in the document.
    /// </summary>
    [Test]
    public virtual void testUnionToDecimal_02() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/union.xsc", this);
      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string[] xmlStrings;
      String[] values = {
          "abc", // not a decimal 
      };
      String[] resultValues = {
          "abc", 
      };

      int i;
      xmlStrings = new string[values.Length];
      for (i = 0; i < values.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " +
            "xsi:type='foo:unionedEnum2' >" + values[i] + "</foo:A>\n";
      };

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      // Try encoding without DTRM, which succeeds
      foreach (AlignmentType alignment in Alignments) {
        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;
        for (i = 0; i < xmlStrings.Length; i++) {
          MemoryStream baos = new MemoryStream();
          encoder.OutputStream = baos;
          encoder.encode(new InputSource<Stream>(string2Stream(xmlStrings[0])));

          byte[] bts = baos.ToArray();

          decoder.InputStream = new MemoryStream(bts);
          Scanner scanner = decoder.processHeader();

          EventDescription exiEvent;
          int n_events = 0;
          int n_texts = 0;
          while ((exiEvent = scanner.nextEvent()) != null) {
            ++n_events;
            if (exiEvent.EventKind == EventDescription_Fields.EVENT_CH) {
              string stringValue = exiEvent.Characters.makeString();
              Assert.AreEqual(resultValues[i], stringValue);
              Assert.AreEqual(EventType.ITEM_SCHEMA_CH, exiEvent.getEventType().itemType);
              if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
                Assert.IsTrue(corpus.isSimpleType(scanner.currentState.contentDatatype));
                Assert.AreEqual("unionedEnum2", corpus.getNameOfType(scanner.currentState.contentDatatype));
                Assert.AreEqual("urn:foo", corpus.uris[corpus.getUriOfType(scanner.currentState.contentDatatype)]);
              }
              ++n_texts;
            }
          }
          Assert.AreEqual(1, n_texts);
          Assert.AreEqual(6, n_events);
        }
      }

      // Set DTRM
      QName[] datatypeRepresentationMap = new QName[2];
      datatypeRepresentationMap[0] = new QName("foo:refType", "urn:foo");
      datatypeRepresentationMap[1] = new QName("exi:decimal", ExiUriConst.W3C_2009_EXI_URI);

      encoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);
      decoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);

      foreach (AlignmentType alignment in Alignments) {
        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;
        for (i = 0; i < xmlStrings.Length; i++) {
          bool caught = false;
          try {
            encoder.OutputStream = new MemoryStream();
            encoder.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));
          }
          catch (TransmogrifierException te) {
            caught = true;
            Assert.AreEqual(TransmogrifierException.UNEXPECTED_CHARS, te.Code);
          }
          Assert.IsTrue(caught);
        }
      }
    }

    /// <summary>
    /// A DTRM entry at xsd:decimal does not affect xsd:byte encoding. 
    /// </summary>
    [Test]
    public virtual void testImperviousnessOfByte_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/interop/datatypes/nbitInteger/nbitInteger.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string[] xmlStrings;
      string[] values = new string[] { "33" };
      string[] resultValues = new string[] { "33" };

      int i;
      xmlStrings = new string[values.Length];
      for (i = 0; i < values.Length; i++) {
        xmlStrings[i] = "<root><byte>" + values[i] + "</byte></root>\n";
      };

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      QName[] datatypeRepresentationMap = new QName[2];
      datatypeRepresentationMap[0] = new QName("xsd:decimal", XmlUriConst.W3C_2001_XMLSCHEMA_URI);
      datatypeRepresentationMap[1] = new QName("exi:string", ExiUriConst.W3C_2009_EXI_URI);

      encoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);
      decoder.setDatatypeRepresentationMap(datatypeRepresentationMap, 1);

      foreach (AlignmentType alignment in Alignments) {
        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;
        for (i = 0; i < xmlStrings.Length; i++) {
          Scanner scanner;

          MemoryStream baos = new MemoryStream();
          encoder.OutputStream = baos;

          byte[] bts;
          int n_events, n_texts;

          encoder.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));

          bts = baos.ToArray();

          decoder.InputStream = new MemoryStream(bts);
          scanner = decoder.processHeader();

          EventDescription exiEvent;
          n_events = 0;
          n_texts = 0;
          while ((exiEvent = scanner.nextEvent()) != null) {
            ++n_events;
            if (exiEvent.EventKind == EventDescription_Fields.EVENT_CH) {
              string stringValue = exiEvent.Characters.makeString();
              Assert.AreEqual(resultValues[i], stringValue);
              Assert.AreEqual(EventType.ITEM_SCHEMA_CH, exiEvent.getEventType().itemType);
              if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
                int tp = scanner.currentState.contentDatatype;
                Assert.AreEqual(EXISchemaConst.BYTE_TYPE, corpus.getSerialOfType(tp));
              }
              Assert.AreEqual(Apparatus.CODEC_INTEGER, ApparatusUtil.getCodecID(scanner, EXISchemaConst.BYTE_TYPE));
              ++n_texts;
            }
          }
          Assert.AreEqual(1, n_texts);
          Assert.AreEqual(7, n_events);
        }
      }
    }

  }

}