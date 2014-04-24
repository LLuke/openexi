using System;
using System.IO;
using System.Collections.Generic;
using NUnit.Framework;

using Org.System.Xml.Sax;


namespace Nagasena.Sax {

  using EXIDecoder = Nagasena.Proc.EXIDecoder;
  using AlignmentType = Nagasena.Proc.Common.AlignmentType;
  using EventDescription = Nagasena.Proc.Common.EventDescription;
  using EventDescription_Fields = Nagasena.Proc.Common.EventDescription_Fields;
  using EventType = Nagasena.Proc.Common.EventType;
  using GrammarOptions = Nagasena.Proc.Common.GrammarOptions;
  using GrammarCache = Nagasena.Proc.Grammars.GrammarCache;
  using Scanner = Nagasena.Proc.IO.Scanner;
  using EXISchema = Nagasena.Schema.EXISchema;
  using EXISchemaConst = Nagasena.Schema.EXISchemaConst;
  using TestBase = Nagasena.Schema.TestBase;
  using EXISchemaFactoryTestUtil = Nagasena.Scomp.EXISchemaFactoryTestUtil;

  [TestFixture]
  [Category("Enable_Compression")]
  public class IntegerValueEncodingTest : TestBase {

    private static readonly AlignmentType[] Alignments = new AlignmentType[] { 
      AlignmentType.bitPacked, 
      AlignmentType.byteAligned, 
      //AlignmentType.preCompress, 
      //AlignmentType.compress 
    };

    ///////////////////////////////////////////////////////////////////////////
    // Test cases
    ///////////////////////////////////////////////////////////////////////////

    /// <summary>
    /// A valid integer value matching ITEM_SCHEMA_CH where the associated
    /// datatype is xsd:integer.
    /// </summary>
    [Test]
    public virtual void testValidInteger() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/integer.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string[] xmlStrings;
      String[] values = {
          "  9223372036854775807 ",
          " \t -9223372036854775808 \r\n ",
          "  98765432109876543210 ", // 20 digits do not fit into long        
          "987654321098765432", // 18 digits fit into long
          "-987654321098765432",
          "-68168168468468435168168468468468846" // a value from EXI interop test
      };
      String[] resultValues = {
          "9223372036854775807",
          "-9223372036854775808",
          "98765432109876543210",
          "987654321098765432",
          "-987654321098765432",
          "-68168168468468435168168468468468846"
      };

      int i;
      xmlStrings = new string[values.Length];
      for (i = 0; i < values.Length; i++) {
        xmlStrings[i] = "<foo:Integer xmlns:foo='urn:foo'>" + values[i] + "</foo:Integer>\n";
      };

      foreach (AlignmentType alignment in Alignments) {
        for (i = 0; i < xmlStrings.Length; i++) {
          Transmogrifier encoder = new Transmogrifier();
          EXIDecoder decoder = new EXIDecoder();
          Scanner scanner;

          encoder.AlignmentType = alignment;
          decoder.AlignmentType = alignment;

          encoder.GrammarCache = grammarCache;
          MemoryStream baos = new MemoryStream();
          encoder.OutputStream = baos;

          byte[] bts;
          int n_events;

          encoder.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));

          bts = baos.ToArray();

          decoder.GrammarCache = grammarCache;
          decoder.InputStream = new MemoryStream(bts);
          scanner = decoder.processHeader();

          EventDescription exiEvent;
          n_events = 0;

          EventType eventType;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SD, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("Integer", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
          Assert.AreEqual(resultValues[i], exiEvent.Characters.makeString());
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            int tp = scanner.currentState.contentDatatype;
            Assert.AreEqual(EXISchemaConst.INTEGER_TYPE, corpus.getSerialOfType(tp));
          }
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
          ++n_events;

          Assert.AreEqual(5, n_events);
        }
      }
    }

    [Test]
    public virtual void testValidNBitInteger_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/integer.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string[] xmlStrings;
      String[] values = {
          "  20 ",
          "70",
      };
      String[] resultValues = {
          "20",
          "70",
      };

      int i;
      xmlStrings = new string[values.Length];
      for (i = 0; i < values.Length; i++) {
        xmlStrings[i] = "<foo:NBitInteger_A xmlns:foo='urn:foo'>" + values[i] + "</foo:NBitInteger_A>\n";
      };

      foreach (AlignmentType alignment in Alignments) {
        for (i = 0; i < xmlStrings.Length; i++) {
          Transmogrifier encoder = new Transmogrifier();
          EXIDecoder decoder = new EXIDecoder();
          Scanner scanner;

          encoder.AlignmentType = alignment;
          decoder.AlignmentType = alignment;

          encoder.GrammarCache = grammarCache;
          MemoryStream baos = new MemoryStream();
          encoder.OutputStream = baos;

          byte[] bts;
          int n_events;

          encoder.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));

          bts = baos.ToArray();

          decoder.GrammarCache = grammarCache;
          decoder.InputStream = new MemoryStream(bts);
          scanner = decoder.processHeader();

          EventDescription exiEvent;
          n_events = 0;

          EventType eventType;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SD, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("NBitInteger_A", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
          Assert.AreEqual(resultValues[i], exiEvent.Characters.makeString());
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            int tp = scanner.currentState.contentDatatype;
            int builtinType = corpus.getBaseTypeOfSimpleType(tp);
            Assert.AreEqual(EXISchemaConst.INTEGER_TYPE, corpus.getSerialOfType(builtinType));
          }
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
          ++n_events;

          Assert.AreEqual(5, n_events);
        }
      }
    }

    [Test]
    public virtual void testValidNBitInteger_02() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/integer.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string[] xmlStrings;
      String[] values = {
          "  12678967543253 ",
          "12678967543285"
      };
      String[] resultValues = {
          "12678967543253",
          "12678967543285"
      };

      int i;
      xmlStrings = new string[values.Length];
      for (i = 0; i < values.Length; i++) {
        xmlStrings[i] = "<foo:NBitInteger_B xmlns:foo='urn:foo'>" + values[i] + "</foo:NBitInteger_B>\n";
      };

      foreach (AlignmentType alignment in Alignments) {
        for (i = 0; i < xmlStrings.Length; i++) {
          Transmogrifier encoder = new Transmogrifier();
          EXIDecoder decoder = new EXIDecoder();
          Scanner scanner;

          encoder.AlignmentType = alignment;
          decoder.AlignmentType = alignment;

          encoder.GrammarCache = grammarCache;
          MemoryStream baos = new MemoryStream();
          encoder.OutputStream = baos;

          byte[] bts;
          int n_events;

          encoder.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));

          bts = baos.ToArray();

          decoder.GrammarCache = grammarCache;
          decoder.InputStream = new MemoryStream(bts);
          scanner = decoder.processHeader();

          EventDescription exiEvent;
          n_events = 0;

          EventType eventType;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SD, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("NBitInteger_B", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
          Assert.AreEqual(resultValues[i], exiEvent.Characters.makeString());
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            int tp = scanner.currentState.contentDatatype;
            int builtinType = corpus.getBaseTypeOfSimpleType(tp);
            Assert.AreEqual(EXISchemaConst.INTEGER_TYPE, corpus.getSerialOfType(builtinType));
          }
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
          ++n_events;

          Assert.AreEqual(5, n_events);
        }
      }
    }

    /// <summary>
    /// Preserve lexical integer values by turning on Preserve.lexicalValues.
    /// </summary>
    [Test]
    public virtual void testIntegerRCS() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/integer.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string[] xmlStrings;
      String[] originalValues = {
          " \t\r *9223372036854775807*\n", // '*' will be encoded as an escaped character 
      };
      String[] parsedOriginalValues = {
          " \t\n *9223372036854775807*\n", 
      };

      int i;
      xmlStrings = new string[originalValues.Length];
      for (i = 0; i < originalValues.Length; i++) {
        xmlStrings[i] = "<foo:Integer xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:Integer>\n";
      };

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      encoder.PreserveLexicalValues = true;
      decoder.PreserveLexicalValues = true;

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

          List<EventDescription> exiEventList = new List<EventDescription>();

          EventDescription exiEvent;
          n_events = 0;
          n_texts = 0;
          while ((exiEvent = scanner.nextEvent()) != null) {
            ++n_events;
            if (exiEvent.EventKind == EventDescription_Fields.EVENT_CH) {
              string stringValue = exiEvent.Characters.makeString();
              Assert.AreEqual(parsedOriginalValues[i], stringValue);
              Assert.AreEqual(EventType.ITEM_SCHEMA_CH, exiEvent.getEventType().itemType);
              ++n_texts;
            }
            exiEventList.Add(exiEvent);
          }
          Assert.AreEqual(1, n_texts);
          Assert.AreEqual(5, n_events);
        }
      }
    }

  }

}