using System;
using System.IO;
using NUnit.Framework;

using Org.System.Xml.Sax;

using EXIDecoder = Nagasena.Proc.EXIDecoder;
using HeaderOptionsOutputType = Nagasena.Proc.HeaderOptionsOutputType;
using AlignmentType = Nagasena.Proc.Common.AlignmentType;
using EventCode = Nagasena.Proc.Common.EventCode;
using EventType = Nagasena.Proc.Common.EventType;
using EventTypeList = Nagasena.Proc.Common.EventTypeList;
using EventDescription = Nagasena.Proc.Common.EventDescription;
using EventDescription_Fields = Nagasena.Proc.Common.EventDescription_Fields;
using GrammarOptions = Nagasena.Proc.Common.GrammarOptions;
using GrammarCache = Nagasena.Proc.Grammars.GrammarCache;
using Scanner = Nagasena.Proc.IO.Scanner;
using EXISchema = Nagasena.Schema.EXISchema;
using TestBase = Nagasena.Schema.TestBase;
using EXISchemaFactoryTestUtil = Nagasena.Scomp.EXISchemaFactoryTestUtil;

namespace Nagasena.Sax {

  [TestFixture]
  [Category("Enable_Compression")]
  public class BooleanValueEncodingTest : TestBase {

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
    /// A valid boolean value matching ITEM_SCHEMA_CH where the associated
    /// datatype is xsd:boolean.
    /// </summary>
    [Test]
    public virtual void testValidBoolean_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/boolean.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      string[] xmlStrings;
      String[] values = {
          " \t\r  true\n",
          "false",
          " \n 1  \t\r",
          "0"
      };
      String[] resultValues = {
          "true",
          "false",
          "true",
          "false"
      };

      int i;
      xmlStrings = new string[values.Length];
      for (i = 0; i < values.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + values[i] + "</foo:A>\n";
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
          EventType eventType;
          EventTypeList eventTypeList;

          n_events = 0;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreSame(exiEvent, eventType);
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.IsNull(eventTypeList.EE);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("A", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
          eventTypeList = eventType.EventTypeList;
          Assert.IsNull(eventTypeList.EE);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
          Assert.AreEqual(resultValues[i], exiEvent.Characters.makeString());
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          Assert.AreEqual(2, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(8, eventTypeList.Length);
          eventType = eventTypeList.item(0);
          Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(4);
          Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(5);
          Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
          eventType = eventTypeList.item(6);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(7);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(3, eventTypeList.Length);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreSame(exiEvent, eventType);
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(1, eventTypeList.Length);
          ++n_events;

          Assert.AreEqual(5, n_events);
        }
      }
    }

    /// <summary>
    /// An invalid boolean value matching ITEM_CH instead of ITEM_SCHEMA_CH.
    /// </summary>
    [Test]
    public virtual void testInvalidBoolean_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/boolean.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString = "<foo:A xmlns:foo='urn:foo'>tree</foo:A>\n";

      foreach (AlignmentType alignment in Alignments) {
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

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

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
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("tree", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        Assert.AreEqual(7, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(8, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(4, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        ++n_events;

        Assert.AreEqual(5, n_events);
      }
    }

    /// <summary>
    /// An attribute with a valid boolean value matching ITEM_SCHEMA_AT where 
    /// the associated datatype is xsd:boolean.
    /// </summary>
    [Test]
    public virtual void testValidBoolean_02() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/boolean.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString = "<foo:B xmlns:foo='urn:foo' foo:aA='false' />\n";

      foreach (AlignmentType alignment in Alignments) {
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

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

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
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
        Assert.AreEqual("false", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(10, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual("aA", eventType.name);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(6, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        ++n_events;

        Assert.AreEqual(5, n_events);
      }
    }

    /// <summary>
    /// An attribute with an invalid boolean value matching 
    /// ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE instead of ITEM_SCHEMA_AT.
    /// </summary>
    [Test]
    public virtual void testInvalidBoolean_02() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/boolean.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString = "<foo:B xmlns:foo='urn:foo' foo:aA='faith' />\n";

      foreach (AlignmentType alignment in Alignments) {
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

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

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
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
        Assert.AreEqual("faith", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual("aA", eventType.name);
        Assert.AreEqual(6, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(10, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(6, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        ++n_events;

        Assert.AreEqual(5, n_events);
      }
    }

    /// <summary>
    /// An attribute with a valid boolean value matching ITEM_SCHEMA_AT_WC_NS 
    /// where there is a global attribute declaration given for the attribute with
    /// datatype xsd:boolean. 
    /// </summary>
    [Test]
    public virtual void testValidBoolean_03() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/boolean.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString = "<foo:B xmlns:foo='urn:foo' foo:aB='true' />\n";

      foreach (AlignmentType alignment in Alignments) {
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

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

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
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
        Assert.AreEqual("true", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(10, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual("aA", eventType.name);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual("aA", eventType.name);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(4, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(10, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual("aA", eventType.name);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual("aA", eventType.name);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        ++n_events;

        Assert.AreEqual(5, n_events);
      }
    }

    /// <summary>
    /// An attribute with an invalid boolean value matching ITEM_AT_WC_ANY 
    /// where there is a global attribute declaration given for the attribute with
    /// datatype xsd:boolean. 
    /// </summary>
    [Test]
    public virtual void testInvalidBoolean_03() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/boolean.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString = "<foo:B xmlns:foo='urn:foo' foo:aB='tree' />\n";

      foreach (AlignmentType alignment in Alignments) {
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

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

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
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
        Assert.AreEqual("tree", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        Assert.AreEqual(7, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(10, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual("aA", eventType.name);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual("aA", eventType.name);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(4, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(10, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual("aA", eventType.name);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual("aA", eventType.name);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        ++n_events;

        Assert.AreEqual(5, n_events);
      }
    }

    /// <summary>
    /// An attribute with a valid boolean value matching undeclared ITEM_SCHEMA_AT_WC_ANY 
    /// where there is a global attribute declaration given for the attribute with
    /// datatype xsd:boolean. 
    /// </summary>
    [Test]
    public virtual void testValidBoolean_04() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/boolean.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString = "<foo:B xmlns:foo='urn:foo' xmlns:goo='urn:goo' goo:aX='true' />\n";

      foreach (AlignmentType alignment in Alignments) {
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

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

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
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
        Assert.AreEqual("true", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        Assert.AreEqual(5, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(10, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual("aA", eventType.name);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual("aA", eventType.name);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(4, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(10, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual("aA", eventType.name);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual("aA", eventType.name);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        ++n_events;

        Assert.AreEqual(5, n_events);
      }
    }

    /// <summary>
    /// An attribute with an invalid boolean value matching ITEM_AT_WC_ANY 
    /// where there is a global attribute declaration given for the attribute with
    /// datatype xsd:boolean. 
    /// </summary>
    [Test]
    public virtual void testInvalidBoolean_04() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/boolean.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString = "<foo:B xmlns:foo='urn:foo' xmlns:goo='urn:goo' goo:aX='tree' />\n";

      foreach (AlignmentType alignment in Alignments) {
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

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

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
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
        Assert.AreEqual("tree", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        Assert.AreEqual(7, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(10, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual("aA", eventType.name);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual("aA", eventType.name);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(4, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(10, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual("aA", eventType.name);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual("aA", eventType.name);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        ++n_events;

        Assert.AreEqual(5, n_events);
      }
    }

    /// <summary>
    /// Boolean representation takes 2 bits to distinguish lexical values
    /// when there is an associated pattern. 
    /// </summary>
    [Test]
    public virtual void testPatternedBoolean() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/boolean.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string[] xmlStrings;
      String[] originalValues = {
          " \t\r true\n", 
          " \t\r false\n", 
          " \t\r 1\n", 
          " \t\r 0\n", 
      };
      String[] resultValues = {
          "true", 
          "false", 
          "1", 
          "0", 
      };

      const string startTag = "<foo:C xmlns:foo='urn:foo'>";
      const string endTag = "</foo:C>\n";

      int i;
      xmlStrings = new string[originalValues.Length];
      for (i = 0; i < originalValues.Length; i++) {
        xmlStrings[i] = startTag + originalValues[i] + endTag;
      };

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;
        for (i = 0; i < xmlStrings.Length; i++) {
          Scanner scanner;

          MemoryStream _baos = new MemoryStream();
          encoder.OutputStream = _baos;

          byte[] bts;
          int n_events, n_texts;

          encoder.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));

          bts = _baos.ToArray();

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

      encoder.AlignmentType = AlignmentType.bitPacked;

      MemoryStream baos = new MemoryStream();
      encoder.OutputStream = baos;

      try {
        encoder.encode(new InputSource<Stream>(string2Stream(startTag + "a" + endTag)));
      }
      catch (TransmogrifierException eee) {
        Assert.AreEqual(TransmogrifierException.UNEXPECTED_CHARS, eee.Code);
        return;
      }
      Assert.Fail();
    }

    /// <summary>
    /// Preserve lexical boolean values by turning on Preserve.lexicalValues.
    /// </summary>
    [Test]
    public virtual void testBooleanRCS() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/boolean.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string[] xmlStrings;
      String[] originalValues = {
          " \t\r *t*r*u*e*\n", // '*' will be encoded as an escaped character 
      };
      String[] parsedOriginalValues = {
          " \t\n *t*r*u*e*\n", 
      };

      int i;
      xmlStrings = new string[originalValues.Length];
      for (i = 0; i < originalValues.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:A>\n";
      };

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      encoder.OutputOptions = HeaderOptionsOutputType.lessSchemaId;
      encoder.PreserveLexicalValues = true;

      foreach (AlignmentType alignment in Alignments) {
        encoder.AlignmentType = alignment;
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
              Assert.AreEqual(parsedOriginalValues[i], stringValue);
              Assert.AreEqual(EventType.ITEM_SCHEMA_CH, exiEvent.getEventType().itemType);
              ++n_texts;
            }
          }
          Assert.AreEqual(1, n_texts);
          Assert.AreEqual(5, n_events);
        }
      }
    }

    [Test]
    public virtual void test4BooleanStore() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/DataStore/DataStore.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string[] booleanValues4 = new string[] { "true", "false", "0", "1" };

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        foreach (bool useThreadedInflater in new bool[] { true, false }) {
          if (useThreadedInflater && alignment != AlignmentType.compress) {
            continue;
          }
          EXIDecoder decoder = new EXIDecoder(999, useThreadedInflater);
          Scanner scanner;
          InputSource inputSource;

          encoder.AlignmentType = alignment;
          decoder.AlignmentType = alignment;

          encoder.GrammarCache = grammarCache;
          MemoryStream baos = new MemoryStream();
          encoder.OutputStream = baos;

          Uri url = resolveSystemIdAsURL("/DataStore/instance/4BooleanStore.xml");
          FileStream inputStream = new FileStream(url.LocalPath, FileMode.Open);
          inputSource = new InputSource<Stream>(inputStream, url.ToString());

          byte[] bts;
          int n_texts;

          encoder.encode(inputSource);
          inputStream.Close();

          bts = baos.ToArray();

          decoder.GrammarCache = grammarCache;
          decoder.InputStream = new MemoryStream(bts);
          scanner = decoder.processHeader();

          EventDescription exiEvent;
          n_texts = 0;
          while ((exiEvent = scanner.nextEvent()) != null) {
            if (exiEvent.EventKind == EventDescription_Fields.EVENT_CH) {
              string expected = booleanValues4[n_texts];
              string val = exiEvent.Characters.makeString();
              if ("true".Equals(val)) {
                Assert.IsTrue("true".Equals(expected) || "1".Equals(expected));
              }
              else {
                Assert.AreEqual("false", val);
                Assert.IsTrue("false".Equals(expected) || "0".Equals(expected));
              }
              ++n_texts;
            }
          }
          Assert.AreEqual(4, n_texts);
        }
      }
    }

    /// <summary>
    /// Decode 1000BooleanStore.bitPacked
    /// </summary>
    [Test]
    public virtual void testDecode1000BooleanStore_BitPacked() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/DataStore/DataStore.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string[] booleanValues100 = new string[] { "false", "true", "false", "true", "0", "0", "1", "1", "1", "1" };

      AlignmentType alignment = AlignmentType.bitPacked;

      Scanner scanner;

      int n_texts;

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;
      decoder.AlignmentType = alignment;
      Uri url = resolveSystemIdAsURL("/DataStore/instance/1000BooleanStore.bitPacked");
      FileStream inputStream = new FileStream(url.LocalPath, FileMode.Open);
      decoder.InputStream = inputStream;
      scanner = decoder.processHeader();

      EventDescription exiEvent;
      n_texts = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        if (exiEvent.EventKind == EventDescription_Fields.EVENT_CH) {
          if (++n_texts % 100 == 0) {
            string expected = booleanValues100[(n_texts / 100) - 1];
            string val = exiEvent.Characters.makeString();
            if ("true".Equals(val)) {
              Assert.IsTrue("true".Equals(expected) || "1".Equals(expected));
            }
            else {
              Assert.AreEqual("false", val);
              Assert.IsTrue("false".Equals(expected) || "0".Equals(expected));
            }
          }
        }
      }
      Assert.AreEqual(1000, n_texts);
      inputStream.Close();
    }

  }

}