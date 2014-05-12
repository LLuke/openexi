using System;
using System.IO;
using NUnit.Framework;

using Org.System.Xml.Sax;

using ICSharpCode.SharpZipLib.Zip.Compression;

using EXIDecoder = Nagasena.Proc.EXIDecoder;
using HeaderOptionsOutputType = Nagasena.Proc.HeaderOptionsOutputType;
using AlignmentType = Nagasena.Proc.Common.AlignmentType;
using EventDescription = Nagasena.Proc.Common.EventDescription;
using EventDescription_Fields = Nagasena.Proc.Common.EventDescription_Fields;
using EventType = Nagasena.Proc.Common.EventType;
using EventTypeList = Nagasena.Proc.Common.EventTypeList;
using GrammarOptions = Nagasena.Proc.Common.GrammarOptions;
using EXIEventSchemaNil = Nagasena.Proc.Events.EXIEventSchemaNil;
using EXIEventSchemaType = Nagasena.Proc.Events.EXIEventSchemaType;
using GrammarCache = Nagasena.Proc.Grammars.GrammarCache;
using Scanner = Nagasena.Proc.IO.Scanner;
using ChannellingScanner = Nagasena.Proc.IO.Compression.ChannellingScanner;
using EXIEventValueReference = Nagasena.Proc.IO.Compression.EXIEventValueReference;
using ExiUriConst = Nagasena.Proc.Util.ExiUriConst;
using EXISchema = Nagasena.Schema.EXISchema;
using TestBase = Nagasena.Schema.TestBase;
using EXISchemaFactoryTestUtil = Nagasena.Scomp.EXISchemaFactoryTestUtil;

namespace Nagasena.Sax {

  [TestFixture]
  public class CompressionTest : TestBase {

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
    /// Do not use value partitions by setting the value of valueMaxLength
    /// to zero.
    /// </summary>
    [Test]
    public virtual void testForgoValuePartitions_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema((string)null, this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveWhitespaces in new bool[] { false, true }) {
          Transmogrifier encoder = new Transmogrifier();
          encoder.ValueMaxLength = 0;
          EXIDecoder decoder = new EXIDecoder(31);
          decoder.ValueMaxLength = 0;
          Scanner scanner;
          InputSource inputSource;

          encoder.AlignmentType = alignment;
          decoder.AlignmentType = alignment;

          encoder.GrammarCache = grammarCache;
          encoder.PreserveWhitespaces = preserveWhitespaces;
          MemoryStream baos = new MemoryStream();
          encoder.OutputStream = baos;

          Uri url = resolveSystemIdAsURL("/compression/duplicateValues-01.xml");
          FileStream inputStream = new FileStream(url.LocalPath, FileMode.Open);
          inputSource = new InputSource<Stream>(inputStream, url.ToString());

          byte[] bts;
          int n_events;

          encoder.encode(inputSource);
          inputStream.Close();

          bts = baos.ToArray();

          decoder.GrammarCache = grammarCache;
          decoder.InputStream = new MemoryStream(bts);
          scanner = decoder.processHeader();

          EventDescription exiEvent;
          EventType eventType;
          EventTypeList eventTypeList;

          n_events = 0;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreSame(exiEvent, eventType);
          Assert.IsNull(eventType.uri);
          Assert.IsNull(eventType.name);
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.IsNull(eventTypeList.EE);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("", exiEvent.URI);
          Assert.AreEqual("root", exiEvent.Name);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          Assert.IsNull(eventType.uri);
          Assert.IsNull(eventType.name);
          ++n_events;

          if (preserveWhitespaces) {
            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual("\n   ", exiEvent.Characters.makeString());
            ++n_events;
          }

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("a", exiEvent.Name);
          Assert.AreEqual("", exiEvent.URI);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
          Assert.AreEqual("1", exiEvent.Characters.makeString());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          ++n_events;

          if (preserveWhitespaces) {
            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual("\n   ", exiEvent.Characters.makeString());
            ++n_events;
          }

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("b", exiEvent.Name);
          Assert.AreEqual("", exiEvent.URI);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
          Assert.AreEqual("2", exiEvent.Characters.makeString());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          ++n_events;

          if (preserveWhitespaces) {
            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual("\n   ", exiEvent.Characters.makeString());
            ++n_events;
          }

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("a", exiEvent.Name);
          Assert.AreEqual("", exiEvent.URI);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
          Assert.AreEqual("3", exiEvent.Characters.makeString());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          ++n_events;

          if (preserveWhitespaces) {
            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual("\n   ", exiEvent.Characters.makeString());
            ++n_events;
          }

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("b", exiEvent.Name);
          Assert.AreEqual("", exiEvent.URI);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
          Assert.AreEqual("4", exiEvent.Characters.makeString());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          ++n_events;

          if (preserveWhitespaces) {
            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual("\n   ", exiEvent.Characters.makeString());
            ++n_events;
          }

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("a", exiEvent.Name);
          Assert.AreEqual("", exiEvent.URI);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
          Assert.AreEqual("1", exiEvent.Characters.makeString());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          ++n_events;

          if (preserveWhitespaces) {
            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual("\n   ", exiEvent.Characters.makeString());
            ++n_events;
          }

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("b", exiEvent.Name);
          Assert.AreEqual("", exiEvent.URI);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
          Assert.AreEqual("2", exiEvent.Characters.makeString());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          ++n_events;

          if (preserveWhitespaces) {
            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual("\n", exiEvent.Characters.makeString());
            ++n_events;
          }


          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
          ++n_events;

          Assert.AreEqual(preserveWhitespaces ? 29 : 22, n_events);
        }
      }
    }

    /// <summary>
    /// EXI compression changes the order in which values are read and
    /// written to and from an EXI stream.
    /// </summary>
    [Test]
    public virtual void testValueOrder_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema((string)null, this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveWhitespaces in new bool[] { true, false }) {
          Transmogrifier encoder = new Transmogrifier();
          EXIDecoder decoder = new EXIDecoder(31);
          Scanner scanner;
          InputSource inputSource;

          encoder.AlignmentType = alignment;
          decoder.AlignmentType = alignment;

          encoder.GrammarCache = grammarCache;
          encoder.PreserveWhitespaces = preserveWhitespaces;
          MemoryStream baos = new MemoryStream();
          encoder.OutputStream = baos;

          Uri url = resolveSystemIdAsURL("/compression/valueOrder-01.xml");
          FileStream inputStream = new FileStream(url.LocalPath, FileMode.Open);
          inputSource = new InputSource<Stream>(inputStream, url.ToString());

          byte[] bts;
          int n_events;

          encoder.encode(inputSource);
          inputStream.Close();

          bts = baos.ToArray();

          decoder.GrammarCache = grammarCache;
          decoder.InputStream = new MemoryStream(bts);
          scanner = decoder.processHeader();

          EventDescription exiEvent;
          EventType eventType;
          EventTypeList eventTypeList;

          n_events = 0;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreSame(exiEvent, eventType);
          Assert.IsNull(eventType.uri);
          Assert.IsNull(eventType.name);
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.IsNull(eventTypeList.EE);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("", exiEvent.URI);
          Assert.AreEqual("root", exiEvent.Name);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          Assert.IsNull(eventType.uri);
          Assert.IsNull(eventType.name);
          ++n_events;

          if (preserveWhitespaces) {
            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual("\n   ", exiEvent.Characters.makeString());
            ++n_events;
          }

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("a", exiEvent.Name);
          Assert.AreEqual("", exiEvent.URI);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
          Assert.AreEqual("XXX", exiEvent.Characters.makeString());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          ++n_events;

          if (preserveWhitespaces) {
            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual("\n   ", exiEvent.Characters.makeString());
            ++n_events;
          }

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("b", exiEvent.Name);
          Assert.AreEqual("", exiEvent.URI);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
          Assert.AreEqual("bla", exiEvent.Characters.makeString());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          ++n_events;

          if (preserveWhitespaces) {
            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual("\n   ", exiEvent.Characters.makeString());
            ++n_events;
          }

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("c", exiEvent.Name);
          Assert.AreEqual("", exiEvent.URI);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
          Assert.AreEqual("foo", exiEvent.Characters.makeString());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          ++n_events;

          if (preserveWhitespaces) {
            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual("\n   ", exiEvent.Characters.makeString());
            ++n_events;
          }

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("b", exiEvent.Name);
          Assert.AreEqual("", exiEvent.URI);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
          Assert.AreEqual("XXX", exiEvent.Characters.makeString());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          ++n_events;

          for (int i = 0; i < 110; i++) {
            if (preserveWhitespaces) {
              exiEvent = scanner.nextEvent();
              Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
              Assert.AreEqual("\n   ", exiEvent.Characters.makeString());
              ++n_events;
            }

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
            Assert.AreEqual("a", exiEvent.Name);
            Assert.AreEqual("", exiEvent.URI);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual(Convert.ToString(i + 1), exiEvent.Characters.makeString());
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
            ++n_events;
          }

          if (preserveWhitespaces) {
            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual("\n", exiEvent.Characters.makeString());
            ++n_events;
          }

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
          ++n_events;

          Assert.AreEqual(preserveWhitespaces ? 461 : 346, n_events);
        }
      }
    }

    /// <summary>
    /// Values of xsi:nil attributes matching AT(xsi:nil) in schema-informed 
    /// grammars are stored in structure channels whereas those that occur
    /// in the context of built-in grammars are stored in value channels. 
    /// </summary>
    [Test]
    public virtual void testXsiNil_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema((string)null, this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString =
        "<A xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:nil='true'>" +
        "  <B xmlns:xsd='http://www.w3.org/2001/XMLSchema' xsi:type='xsd:boolean' xsi:nil='true' />" +
        "  <A xsi:nil='true' />" +
        "</A>\n";

      AlignmentType[] alignments = 
        new AlignmentType[] { AlignmentType.preCompress, AlignmentType.compress };

      foreach (AlignmentType alignment in alignments) {
        foreach (bool preserveWhitespaces in new bool[] { true, false }) {
          Transmogrifier encoder = new Transmogrifier();
          EXIDecoder decoder = new EXIDecoder(31);
          Scanner scanner;

          encoder.AlignmentType = alignment;
          decoder.AlignmentType = alignment;

          encoder.GrammarCache = grammarCache;
          encoder.PreserveWhitespaces = preserveWhitespaces;
          MemoryStream baos = new MemoryStream();
          encoder.OutputStream = baos;

          byte[] bts;
          int n_events;

          encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

          bts = baos.ToArray();

          decoder.GrammarCache = grammarCache;
          decoder.InputStream = new MemoryStream(bts);
          scanner = decoder.processHeader();

          EventDescription exiEvent;

          n_events = 0;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("A", exiEvent.Name);
          Assert.AreEqual("", exiEvent.URI);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
          Assert.AreEqual("nil", exiEvent.Name);
          Assert.AreEqual("http://www.w3.org/2001/XMLSchema-instance", exiEvent.URI);
          Assert.AreEqual("true", exiEvent.Characters.makeString());
          Assert.IsTrue(exiEvent is EXIEventValueReference); // was in value channel
          ++n_events;

          if (preserveWhitespaces) {
            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual("  ", exiEvent.Characters.makeString());
            ++n_events;
          }

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("B", exiEvent.Name);
          Assert.AreEqual("", exiEvent.URI);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
          Assert.AreEqual("type", ((EXIEventSchemaType)exiEvent).Name);
          Assert.AreEqual("http://www.w3.org/2001/XMLSchema-instance", ((EXIEventSchemaType)exiEvent).URI);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_NL, exiEvent.EventKind);
          Assert.IsTrue(((EXIEventSchemaNil)exiEvent).Nilled);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          ++n_events;

          if (preserveWhitespaces) {
            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual("  ", exiEvent.Characters.makeString());
            ++n_events;
          }

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("A", exiEvent.Name);
          Assert.AreEqual("", exiEvent.URI);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
          Assert.AreEqual("nil", exiEvent.Name);
          Assert.AreEqual("http://www.w3.org/2001/XMLSchema-instance", exiEvent.URI);
          Assert.AreEqual("true", exiEvent.Characters.makeString());
          Assert.IsTrue(exiEvent is EXIEventValueReference); // was in value channel
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
          ++n_events;

          Assert.AreEqual(preserveWhitespaces ? 14 : 12, n_events);
        }
      }
    }

    /// <summary>
    /// EXI test cases of National Library of Medicine (NLM) XML formats.
    /// </summary>
    [Test]
    public virtual void testNLM_default_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/NLM/nlmcatalogrecord_060101.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder(31);
        Scanner scanner;
        InputSource inputSource;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        encoder.PreserveWhitespaces = true;
        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        Uri url = resolveSystemIdAsURL("/NLM/catplussamp2006.xml");
        FileStream inputStream = new FileStream(url.LocalPath, FileMode.Open);
        inputSource = new InputSource<Stream>(inputStream, url.ToString());

        byte[] bts;
        int n_events;

        encoder.encode(inputSource);
        inputStream.Close();

        bts = baos.ToArray();

        decoder.GrammarCache = grammarCache;
        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

        EventDescription exiEvent;
        n_events = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          if (n_events == 47024) {
            // Check the last value in the last value channel
            Assert.AreEqual("Interdisciplinary Studies", exiEvent.Characters.makeString());
          }
          ++n_events;
        }
        Assert.AreEqual(50071, n_events);
      }
    }

    /// <summary>
    /// EXI test cases of National Library of Medicine (NLM) XML formats.
    /// </summary>
    [Test]
    public virtual void testNLM_strict_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/NLM/nlmcatalogrecord_060101.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder(31);
        Scanner scanner;
        InputSource inputSource;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        Uri url = resolveSystemIdAsURL("/NLM/catplussamp2006.xml");
        FileStream inputStream = new FileStream(url.LocalPath, FileMode.Open);
        inputSource = new InputSource<Stream>(inputStream, url.ToString());

        byte[] bts;
        int n_events;

        encoder.encode(inputSource);
        inputStream.Close();

        bts = baos.ToArray();

        decoder.GrammarCache = grammarCache;
        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

        EventDescription exiEvent;
        n_events = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          if (n_events == 33009) {
            // Check the last value in the last value channel
            Assert.AreEqual("Interdisciplinary Studies", exiEvent.Characters.makeString());
          }
          ++n_events;
        }
        Assert.AreEqual(35176, n_events);
      }
    }

    /// <summary>
    /// Only a handful of values in a stream.
    /// </summary>
    [Test]
    public virtual void testSequence_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/interop/schemaInformedGrammar/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      AlignmentType[] alignments = new AlignmentType[] { AlignmentType.preCompress, AlignmentType.compress };

      DeflateStrategy[] strategies = new DeflateStrategy[] { DeflateStrategy.Default, DeflateStrategy.Filtered, DeflateStrategy.HuffmanOnly };

      foreach (AlignmentType alignment in alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder(31);
        Scanner scanner;
        InputSource inputSource;

        encoder.OutputOptions = HeaderOptionsOutputType.lessSchemaId;
        encoder.AlignmentType = alignment;

        encoder.DeflateLevel = Deflater.BEST_COMPRESSION;

        bool isCompress = alignment == AlignmentType.compress;
        byte[][] resultBytes = isCompress ? new byte[3][] : null;

        for (int i = 0; i < strategies.Length; i++) {

          encoder.DeflateStrategy = strategies[i];

          encoder.GrammarCache = grammarCache;
          MemoryStream baos = new MemoryStream();
          encoder.OutputStream = baos;

          Uri url = resolveSystemIdAsURL("/interop/schemaInformedGrammar/declaredProductions/sequence-01.xml");
          FileStream inputStream = new FileStream(url.LocalPath, FileMode.Open);
          inputSource = new InputSource<Stream>(inputStream, url.ToString());

          byte[] bts;
          int n_events;

          encoder.encode(inputSource);
          inputStream.Close();

          bts = baos.ToArray();
          if (isCompress) {
            resultBytes[i] = bts;
          }

          decoder.GrammarCache = grammarCache;
          decoder.InputStream = new MemoryStream(bts);
          scanner = decoder.processHeader();
          Assert.AreEqual(alignment, scanner.HeaderOptions.AlignmentType);

          EventDescription exiEvent;
          EventType eventType;
          EventTypeList eventTypeList;

          n_events = 0;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreSame(exiEvent, eventType);
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(1, eventTypeList.Length);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("A", exiEvent.Name);
          Assert.AreEqual("urn:foo", exiEvent.URI);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("AB", exiEvent.Name);
          Assert.AreEqual("urn:foo", exiEvent.URI);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
          Assert.AreEqual("", exiEvent.Characters.makeString());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("AC", exiEvent.Name);
          Assert.AreEqual("urn:foo", exiEvent.URI);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
          Assert.AreEqual("", exiEvent.Characters.makeString());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("AC", exiEvent.Name);
          Assert.AreEqual("urn:foo", exiEvent.URI);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
          Assert.AreEqual("", exiEvent.Characters.makeString());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("AD", exiEvent.Name);
          Assert.AreEqual("urn:foo", exiEvent.URI);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
          Assert.AreEqual("", exiEvent.Characters.makeString());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("AE", exiEvent.Name);
          Assert.AreEqual("urn:foo", exiEvent.URI);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
          Assert.AreEqual("", exiEvent.Characters.makeString());
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
          ++n_events;

          Assert.AreEqual(19, n_events);
        }
        if (isCompress) {
          Assert.IsTrue(resultBytes[0].Length < resultBytes[1].Length);
          Assert.IsTrue(resultBytes[1].Length < resultBytes[2].Length);
        }
      }
    }

    /// <summary>
    /// EXI test cases of Joint Theater Logistics Management format.
    /// </summary>
    [Test]
    public virtual void testJTLM_publish911() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/JTLM/schemas/TLMComposite.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      // This test case takes a while to run, so just use "compress".
      AlignmentType alignment = AlignmentType.compress;

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      InputSource inputSource;

      encoder.AlignmentType = alignment;
      decoder.AlignmentType = alignment;

      encoder.GrammarCache = grammarCache;
      MemoryStream baos = new MemoryStream();
      encoder.OutputStream = baos;

      Uri url = resolveSystemIdAsURL("/JTLM/publish911.xml");
      FileStream inputStream = new FileStream(url.LocalPath, FileMode.Open);
      inputSource = new InputSource<Stream>(inputStream, url.ToString());

      byte[] bts;
      int n_events, n_texts;

      encoder.encode(inputSource);
      inputStream.Close();

      bts = baos.ToArray();

      decoder.GrammarCache = grammarCache;
      decoder.InputStream = new MemoryStream(bts);
      scanner = decoder.processHeader();

      EventDescription exiEvent;
      n_events = 0;
      n_texts = 0;

      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        if (exiEvent.EventKind == EventDescription_Fields.EVENT_CH) {
          if (exiEvent.Characters.length == 0) {
            --n_events;
            continue;
          }
          if (n_texts % 100 == 0) {
            int n = n_texts / 100;
            Assert.AreEqual(JTLMTest.publish911_centennials_1[n], exiEvent.Characters.makeString());
          }
          ++n_texts;
        }
      }
      Assert.AreEqual(96576, n_events);
    }

    /// <summary>
    /// EXI test cases of Joint Theater Logistics Management format.
    /// </summary>
    [Test]
    public virtual void testJTLM_publish100_blockSize() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/JTLM/schemas/TLMComposite.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      AlignmentType[] alignments = new AlignmentType[] { AlignmentType.preCompress, AlignmentType.compress };
      int[] blockSizes = new int[] { 1, 100, 101 };

      Transmogrifier encoder = new Transmogrifier();

      encoder.OutputOptions = HeaderOptionsOutputType.lessSchemaId;
      encoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in alignments) {
        for (int i = 0; i < blockSizes.Length; i++) {
          EXIDecoder decoder = new EXIDecoder(999);
          decoder.GrammarCache = grammarCache;
          Scanner scanner;
          InputSource inputSource;

          encoder.AlignmentType = alignment;
          encoder.BlockSize = blockSizes[i];

          MemoryStream baos = new MemoryStream();
          encoder.OutputStream = baos;

          Uri url = resolveSystemIdAsURL("/JTLM/publish100.xml");
          FileStream inputStream = new FileStream(url.LocalPath, FileMode.Open);
          inputSource = new InputSource<Stream>(inputStream, url.ToString());

          byte[] bts;
          int n_events, n_texts;

          encoder.encode(inputSource);
          inputStream.Close();

          bts = baos.ToArray();

          decoder.InputStream = new MemoryStream(bts);
          scanner = decoder.processHeader();

          EventDescription exiEvent;
          n_events = 0;
          n_texts = 0;

          while ((exiEvent = scanner.nextEvent()) != null) {
            ++n_events;
            if (exiEvent.EventKind == EventDescription_Fields.EVENT_CH) {
              if (exiEvent.Characters.length == 0) {
                --n_events;
                continue;
              }
              if (n_texts % 100 == 0) {
                int n = n_texts / 100;
                Assert.AreEqual(JTLMTest.publish100_centennials_1[n], exiEvent.Characters.makeString());
              }
              ++n_texts;
            }
          }
          Assert.AreEqual(10610, n_events);
        }
      }
    }

    [Test]
    public virtual void testEmptyBlock_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/compression/emptyBlock_01.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder(31);
      Scanner scanner;
      InputSource inputSource;

      encoder.OutputOptions = HeaderOptionsOutputType.lessSchemaId;

      encoder.AlignmentType = AlignmentType.compress;
      encoder.BlockSize = 1;

      encoder.GrammarCache = grammarCache;
      MemoryStream baos = new MemoryStream();
      encoder.OutputStream = baos;

      Uri url = resolveSystemIdAsURL("/compression/emptyBlock_01.xml");
      FileStream inputStream = new FileStream(url.LocalPath, FileMode.Open);
      inputSource = new InputSource<Stream>(inputStream, url.ToString());

      byte[] bts;
      int n_events;

      encoder.encode(inputSource);
      inputStream.Close();

      bts = baos.ToArray();

      decoder.GrammarCache = grammarCache;
      decoder.InputStream = new MemoryStream(bts);
      scanner = decoder.processHeader();

      EventDescription exiEvent;
      EventType eventType;
      EventTypeList eventTypeList;

      n_events = 0;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.IsNull(eventType.uri);
      Assert.IsNull(eventType.name);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.IsNull(eventTypeList.EE);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      Assert.AreEqual("", exiEvent.URI);
      Assert.AreEqual("root", exiEvent.Name);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      Assert.AreEqual("", eventType.uri);
      Assert.AreEqual("root", eventType.name);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      Assert.AreEqual("parent", exiEvent.Name);
      Assert.AreEqual("", eventType.uri);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      Assert.AreEqual("child", exiEvent.Name);
      Assert.AreEqual("", eventType.uri);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
      Assert.AreEqual("42", exiEvent.Characters.makeString());
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      Assert.AreEqual("adjunct", exiEvent.Name);
      Assert.AreEqual("", exiEvent.URI);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
      ++n_events;

      Assert.AreEqual(11, n_events);
      Assert.AreEqual(1, ((ChannellingScanner)scanner).BlockCount);
    }
    
    [Test]
    public virtual void testCompressionOption_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/optionsSchema.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier encoder = new Transmogrifier();
      encoder.GrammarCache = grammarCache;
      encoder.AlignmentType = AlignmentType.compress;
      MemoryStream baos = new MemoryStream();
      encoder.OutputStream = baos;
      encoder.OutputOptions = HeaderOptionsOutputType.lessSchemaId;

      string xmlString;
      byte[] bts;
      EXIDecoder decoder;
      Scanner scanner;
      int n_events;

      xmlString = "<header xmlns='http://www.w3.org/2009/exi'><strict/></header>\n";

      encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

      bts = baos.ToArray();

      decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;
      decoder.AlignmentType = AlignmentType.bitPacked; // try to confuse decoder.
      decoder.InputStream = new MemoryStream(bts);
      scanner = decoder.processHeader();

      EventDescription exiEvent;
      n_events = 0;

      EventType eventType;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      Assert.AreEqual("header", exiEvent.Name);
      Assert.AreEqual(ExiUriConst.W3C_2009_EXI_URI, exiEvent.URI);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      Assert.AreEqual("strict", exiEvent.Name);
      Assert.AreEqual(ExiUriConst.W3C_2009_EXI_URI, exiEvent.URI);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      ++n_events;

      Assert.AreEqual(6, n_events);
    }

  }

}