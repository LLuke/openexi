using System;
using System.IO;
using System.Collections.Generic;
using NUnit.Framework;

using Org.System.Xml.Sax;

using EXIDecoder = Nagasena.Proc.EXIDecoder;
using AlignmentType = Nagasena.Proc.Common.AlignmentType;
using EventDescription = Nagasena.Proc.Common.EventDescription;
using EventDescription_Fields = Nagasena.Proc.Common.EventDescription_Fields;
using EventType = Nagasena.Proc.Common.EventType;
using GrammarOptions = Nagasena.Proc.Common.GrammarOptions;
using XmlUriConst = Nagasena.Proc.Common.XmlUriConst;
using EXIEventSchemaType = Nagasena.Proc.Events.EXIEventSchemaType;
using GrammarCache = Nagasena.Proc.Grammars.GrammarCache;
using Scanner = Nagasena.Proc.IO.Scanner;
using EXISchema = Nagasena.Schema.EXISchema;
using EXISchemaConst = Nagasena.Schema.EXISchemaConst;
using EmptySchema = Nagasena.Schema.EmptySchema;
using TestBase = Nagasena.Schema.TestBase;
using EXISchemaFactoryTestUtil = Nagasena.Scomp.EXISchemaFactoryTestUtil;

namespace Nagasena.Sax {

  [TestFixture]
  [Category("Enable_Compression")]
  public class FloatValueEncodingTest : TestBase {

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
    /// A valid float value matching ITEM_SCHEMA_CH where the associated
    /// datatype is xsd:float.
    /// </summary>
    [Test]
    public virtual void testValidFloat() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/float.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string[] xmlStrings;
      String[] values = {
          "  -1E4 ", 
          " \t 1267.43233E12 \r\n ", 
          "12.78e-2", 
          "12", 
          "0", 
          "-0", 
          "INF", 
          "-INF", 
          "NaN",
          "-9223372036854775808",
          "9223372036854775807"
        
      };
      String[] resultValues = {
          "-1E4", 
          "126743233E7", 
          "1278E-4", 
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
      xmlStrings = new string[values.Length];
      for (i = 0; i < values.Length; i++) {
        xmlStrings[i] = "<foo:Float xmlns:foo='urn:foo'>" + values[i] + "</foo:Float>\n";
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
          Assert.AreEqual("Float", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
          Assert.AreEqual(resultValues[i], exiEvent.Characters.makeString());
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            int tp = scanner.currentState.contentDatatype;
            Assert.AreEqual(EXISchemaConst.FLOAT_TYPE, corpus.getSerialOfType(tp));
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
    /// A valid float value matching ITEM_SCHEMA_CH where the associated
    /// datatype is xsd:double.
    /// </summary>
    [Test]
    public virtual void testValidDouble() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/float.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string[] xmlStrings;
      String[] values = {
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
      String[] resultValues = {
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
      xmlStrings = new string[values.Length];
      for (i = 0; i < values.Length; i++) {
        xmlStrings[i] = "<foo:Double xmlns:foo='urn:foo'>" + values[i] + "</foo:Double>\n";
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
          Assert.AreEqual("Double", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
          Assert.AreEqual(resultValues[i], exiEvent.Characters.makeString());
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            int tp = scanner.currentState.contentDatatype;
            Assert.AreEqual(EXISchemaConst.DOUBLE_TYPE, corpus.getSerialOfType(tp));
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
    /// Preserve lexical float values by turning on Preserve.lexicalValues.
    /// </summary>
    [Test]
    public virtual void testFloatRCS() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/float.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string[] xmlStrings;
      String[] originalValues = {
          " \t\r *-1*E*4*\n", // '*' will be encoded as an escaped character 
      };
      String[] parsedOriginalValues = {
          " \t\n *-1*E*4*\n", 
      };

      int i;
      xmlStrings = new string[originalValues.Length];
      for (i = 0; i < originalValues.Length; i++) {
        xmlStrings[i] = "<foo:Float xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:Float>\n";
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

    /// <summary>
    /// Use EmptySchema, with xsi:type to explicitly specify the type.
    /// 
    /// <value xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'
    ///        xmlns:xs='http://www.w3.org/2001/XMLSchema'
    ///        xsi:type='xs:float'> 1.0 </value>
    /// </summary>
    [Test]
    public virtual void testDecodeValidFloat() {

      EXISchema corpus = EmptySchema.EXISchema;
      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      EXIDecoder decoder;
      Scanner scanner;

      decoder = new EXIDecoder();
      decoder.AlignmentType = AlignmentType.byteAligned;

      decoder.GrammarCache = grammarCache;

      /*
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

      string[] stringValues = new string[] { "1E0", "10E-1" };

      for (int i = 0; i < bts.Length; i++) {
        decoder.InputStream = new MemoryStream(bts[i]);
        scanner = decoder.processHeader();

        int n_events;

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
        Assert.AreEqual("", exiEvent.URI);
        Assert.AreEqual("value", exiEvent.Name);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
        Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_URI, ((EXIEventSchemaType)exiEvent).TypeURI);
        Assert.AreEqual("float", ((EXIEventSchemaType)exiEvent).TypeName);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual(stringValues[i], exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        int tp = scanner.currentState.contentDatatype;
        Assert.AreEqual(EXISchemaConst.FLOAT_TYPE, corpus.getSerialOfType(tp));
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

        Assert.AreEqual(6, n_events);
      }
    }

  }

}