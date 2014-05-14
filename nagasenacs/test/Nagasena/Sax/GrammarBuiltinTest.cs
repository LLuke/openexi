using System;
using System.IO;
using System.Collections.Generic;
using NUnit.Framework;

using Org.System.Xml.Sax;

using EXIDecoder = Nagasena.Proc.EXIDecoder;
using AlignmentType = Nagasena.Proc.Common.AlignmentType;
using EventCode = Nagasena.Proc.Common.EventCode;
using EventType = Nagasena.Proc.Common.EventType;
using EventTypeList = Nagasena.Proc.Common.EventTypeList;
using EventDescription = Nagasena.Proc.Common.EventDescription;
using EventDescription_Fields = Nagasena.Proc.Common.EventDescription_Fields;
using GrammarOptions = Nagasena.Proc.Common.GrammarOptions;
using XmlUriConst = Nagasena.Proc.Common.XmlUriConst;
using EXIEventDTD = Nagasena.Proc.Events.EXIEventDTD;
using EXIEventNS = Nagasena.Proc.Events.EXIEventNS;
using EXIEventSchemaType = Nagasena.Proc.Events.EXIEventSchemaType;
using GrammarCache = Nagasena.Proc.Grammars.GrammarCache;
using Scanner = Nagasena.Proc.IO.Scanner;
using EXISchema = Nagasena.Schema.EXISchema;
using EmptySchema = Nagasena.Schema.EmptySchema;
using TestBase = Nagasena.Schema.TestBase;

using EXISchemaFactoryTestUtil = Nagasena.Scomp.EXISchemaFactoryTestUtil;

namespace Nagasena.Sax {

  [TestFixture]
  public class GrammarBuiltinTest : TestBase {

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
    /// Schema:
    /// None available
    /// 
    /// Instance:
    /// <None><!-- abc --><!-- def --></None>
    /// </summary>
    [Test]
    public virtual void testBuiltinComment() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.gram", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addCM(GrammarOptions.DEFAULT_OPTIONS));

      string xmlString;
      byte[] bts;
      int n_events;

      xmlString = "<None xmlns='urn:foo'><!-- abc --><!-- def --></None>\n";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

        decoder.GrammarCache = grammarCache;
        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

        List<EventDescription> exiEventList = new List<EventDescription>();

        EventDescription exiEvent;
        n_events = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          exiEventList.Add(exiEvent);
        }

        Assert.AreEqual(6, n_events);

        EventType eventType;
        EventTypeList eventTypeList;
        int n_eventTypes;

        exiEvent = exiEventList[0];
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[1];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("None", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          eventTypeList = eventType.EventTypeList;
          n_eventTypes = eventTypeList.Length;
          Assert.AreEqual(n_eventTypes - 2, eventType.Index);
          eventType = eventTypeList.item(n_eventTypes - 1);
          Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        }

        exiEvent = exiEventList[2];
        Assert.AreEqual(EventDescription_Fields.EVENT_CM, exiEvent.EventKind);
        Assert.AreEqual(" abc ", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.AreEqual(4, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(5, eventTypeList.Length);
          Assert.IsNotNull(eventTypeList.EE);
          eventType = eventTypeList.item(0);
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        }

        exiEvent = exiEventList[3];
        Assert.AreEqual(EventDescription_Fields.EVENT_CM, exiEvent.EventKind);
        Assert.AreEqual(" def ", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.AreEqual(3, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(4, eventTypeList.Length);
          Assert.IsNotNull(eventTypeList.EE);
          eventType = eventTypeList.item(0);
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        }

        exiEvent = exiEventList[4];
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(4, eventTypeList.Length);
          Assert.IsNotNull(eventTypeList.EE);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        }

        exiEvent = exiEventList[5];
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(2, eventTypeList.Length);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        }
      }
    }

    /// <summary>
    /// Schema:
    /// None available
    /// 
    /// Instance:
    /// <!DOCTYPE books SYSTEM "dtdComments.dtd"><A><!-- XYZ --><B/></A>
    /// </summary>
    [Test]
    public virtual void testBuiltinCommentDTD() {
      EXISchema corpus = EmptySchema.EXISchema;

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addCM(GrammarOptions.DEFAULT_OPTIONS));

      byte[] bts;
      int n_events;

      InputSource inputSource;
      Uri url = resolveSystemIdAsURL("/dtdComments.xml");

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        FileStream inputStream = new FileStream(url.LocalPath, FileMode.Open);
        inputSource = new InputSource<Stream>(inputStream, url.ToString());

        encoder.encode(inputSource);

        bts = baos.ToArray();

        decoder.GrammarCache = grammarCache;
        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

        List<EventDescription> exiEventList = new List<EventDescription>();

        EventDescription exiEvent;
        n_events = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          exiEventList.Add(exiEvent);
        }
        inputStream.Close();

        Assert.AreEqual(7, n_events);

        EventType eventType;

        exiEvent = exiEventList[0];
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);

        exiEvent = exiEventList[1];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("A", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);

        exiEvent = exiEventList[2];
        Assert.AreEqual(EventDescription_Fields.EVENT_CM, exiEvent.EventKind);
        Assert.AreEqual(" XYZ ", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);

        exiEvent = exiEventList[3];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("B", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);

        exiEvent = exiEventList[4];
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);

        exiEvent = exiEventList[5];
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);

        exiEvent = exiEventList[6];
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
      }
    }

    /// <summary>
    /// Schema:
    /// None available
    /// 
    /// Instance:
    /// <None><?abc uvw?><?def xyz?></None>
    /// </summary>
    [Test]
    public virtual void testBuiltinPI() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.gram", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addPI(GrammarOptions.DEFAULT_OPTIONS));

      string xmlString;
      byte[] bts;
      int n_events;

      xmlString = "<None xmlns='urn:foo'><?abc uvw?><?def xyz?></None>\n";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

        decoder.GrammarCache = grammarCache;
        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

        List<EventDescription> exiEventList = new List<EventDescription>();

        EventDescription exiEvent;
        n_events = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          exiEventList.Add(exiEvent);
        }

        Assert.AreEqual(6, n_events);

        EventType eventType;
        EventTypeList eventTypeList;
        int n_eventTypes;

        exiEvent = exiEventList[0];
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[1];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("None", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          eventTypeList = eventType.EventTypeList;
          n_eventTypes = eventTypeList.Length;
          Assert.AreEqual(n_eventTypes - 2, eventType.Index);
          eventType = eventTypeList.item(n_eventTypes - 1);
          Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
        }

        exiEvent = exiEventList[2];
        Assert.AreEqual(EventDescription_Fields.EVENT_PI, exiEvent.EventKind);
        Assert.AreEqual("abc", exiEvent.Name);
        Assert.AreEqual("uvw", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.AreEqual(4, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(5, eventTypeList.Length);
          Assert.IsNotNull(eventTypeList.EE);
          eventType = eventTypeList.item(0);
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        }

        exiEvent = exiEventList[3];
        Assert.AreEqual(EventDescription_Fields.EVENT_PI, exiEvent.EventKind);
        Assert.AreEqual("def", exiEvent.Name);
        Assert.AreEqual("xyz", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.AreEqual(3, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(4, eventTypeList.Length);
          Assert.IsNotNull(eventTypeList.EE);
          eventType = eventTypeList.item(0);
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        }

        exiEvent = exiEventList[4];
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(4, eventTypeList.Length);
          Assert.IsNotNull(eventTypeList.EE);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
        }

        exiEvent = exiEventList[5];
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(2, eventTypeList.Length);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
        }
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
      int n_events;

      xmlString = "<!DOCTYPE None [ <!ENTITY ent SYSTEM 'er-entity.xml'> ]><None xmlns='urn:foo'>&ent;&ent;</None>\n";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        encoder.ResolveExternalGeneralEntities = false;
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        InputSource inputSource = new InputSource<Stream>(string2Stream(xmlString));
        inputSource.SystemId = resolveSystemIdAsURL("/").ToString();
        encoder.encode(inputSource);

        bts = baos.ToArray();

        decoder.GrammarCache = grammarCache;
        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

        List<EventDescription> exiEventList = new List<EventDescription>();

        EventDescription exiEvent;
        n_events = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          exiEventList.Add(exiEvent);
        }

        Assert.AreEqual(7, n_events);

        EventType eventType;
        EventTypeList eventTypeList;
        int n_eventTypes;

        exiEvent = exiEventList[0];
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[1];
        Assert.AreEqual(EventDescription_Fields.EVENT_DTD, exiEvent.EventKind);
        Assert.AreEqual("None", exiEvent.Name);
        Assert.IsNull(((EXIEventDTD)exiEvent).PublicId);
        Assert.IsNull(((EXIEventDTD)exiEvent).SystemId);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_DTD, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          eventTypeList = eventType.EventTypeList;
          n_eventTypes = eventTypeList.Length;
          Assert.AreEqual(n_eventTypes - 1, eventType.Index);
          eventType = eventTypeList.item(n_eventTypes - 2);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        }

        exiEvent = exiEventList[2];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("None", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          eventTypeList = eventType.EventTypeList;
          n_eventTypes = eventTypeList.Length;
          Assert.AreEqual(n_eventTypes - 2, eventType.Index);
          eventType = eventTypeList.item(n_eventTypes - 1);
          Assert.AreEqual(EventType.ITEM_DTD, eventType.itemType);
        }

        exiEvent = exiEventList[3];
        Assert.AreEqual(EventDescription_Fields.EVENT_ER, exiEvent.EventKind);
        Assert.AreEqual("ent", exiEvent.Name);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_ER, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.AreEqual(4, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(5, eventTypeList.Length);
          Assert.IsNotNull(eventTypeList.EE);
          eventType = eventTypeList.item(0);
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        }

        exiEvent = exiEventList[4];
        Assert.AreEqual(EventDescription_Fields.EVENT_ER, exiEvent.EventKind);
        Assert.AreEqual("ent", exiEvent.Name);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_ER, eventType.itemType);
        Assert.AreEqual(3, eventType.Index);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(4, eventTypeList.Length);
          Assert.IsNotNull(eventTypeList.EE);
          eventType = eventTypeList.item(0);
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        }

        exiEvent = exiEventList[5];
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(4, eventTypeList.Length);
          Assert.IsNotNull(eventTypeList.EE);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_ER, eventType.itemType);
        }

        exiEvent = exiEventList[6];
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(1, eventTypeList.Length);
        }
      }
    }

    /// <summary>
    /// Schema:
    /// None available
    /// 
    /// Instance:
    /// <None>&ent;<!-- abc --><?def uvw?></None>
    /// </summary>
    [Test]
    public virtual void testBuiltinEntityRefCommentPI() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.gram", this);

      short options = GrammarOptions.DEFAULT_OPTIONS;
      options = GrammarOptions.addDTD(options);
      options = GrammarOptions.addPI(options);
      options = GrammarOptions.addCM(options);

      GrammarCache grammarCache = new GrammarCache(corpus, options);

      string xmlString;
      byte[] bts;
      int n_events;

      xmlString = "<!DOCTYPE None [ <!ENTITY ent SYSTEM 'er-entity.xml'> ]>" +
        "<None xmlns='urn:foo'>&ent;<!-- abc --><?def uvw?></None>\n";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        encoder.ResolveExternalGeneralEntities = false;
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        InputSource inputSource = new InputSource<Stream>(string2Stream(xmlString));
        inputSource.SystemId = resolveSystemIdAsURL("/").ToString();
        encoder.encode(inputSource);

        bts = baos.ToArray();

        decoder.GrammarCache = grammarCache;
        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

        List<EventDescription> exiEventList = new List<EventDescription>();

        EventDescription exiEvent;
        n_events = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          exiEventList.Add(exiEvent);
        }

        Assert.AreEqual(8, n_events);

        EventType eventType;
        EventTypeList eventTypeList;
        int n_eventTypes;

        exiEvent = exiEventList[0];
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.IsNull(eventTypeList.EE);
        }

        exiEvent = exiEventList[1];
        Assert.AreEqual(EventDescription_Fields.EVENT_DTD, exiEvent.EventKind);
        Assert.AreEqual("None", exiEvent.Name);
        Assert.IsNull(((EXIEventDTD)exiEvent).PublicId);
        Assert.IsNull(((EXIEventDTD)exiEvent).SystemId);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_DTD, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          eventTypeList = eventType.EventTypeList;
          n_eventTypes = eventTypeList.Length;
          Assert.AreEqual(n_eventTypes - 3, eventType.Index);
          eventType = eventTypeList.item(n_eventTypes - 4);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(n_eventTypes - 2);
          Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
          eventType = eventTypeList.item(n_eventTypes - 1);
          Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
        }

        exiEvent = exiEventList[2];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("None", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          eventTypeList = eventType.EventTypeList;
          n_eventTypes = eventTypeList.Length;
          Assert.AreEqual(n_eventTypes - 4, eventType.Index);
          eventType = eventTypeList.item(n_eventTypes - 3);
          Assert.AreEqual(EventType.ITEM_DTD, eventType.itemType);
          eventType = eventTypeList.item(n_eventTypes - 2);
          Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
          eventType = eventTypeList.item(n_eventTypes - 1);
          Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
        }

        exiEvent = exiEventList[3];
        Assert.AreEqual(EventDescription_Fields.EVENT_ER, exiEvent.EventKind);
        Assert.AreEqual("ent", exiEvent.Name);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_ER, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.AreEqual(4, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(7, eventTypeList.Length);
          Assert.IsNotNull(eventTypeList.EE);
          eventType = eventTypeList.item(0);
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          eventType = eventTypeList.item(5);
          Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
          eventType = eventTypeList.item(6);
          Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
        }

        exiEvent = exiEventList[4];
        Assert.AreEqual(EventDescription_Fields.EVENT_CM, exiEvent.EventKind);
        Assert.AreEqual(" abc ", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.AreEqual(4, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(6, eventTypeList.Length);
          Assert.IsNotNull(eventTypeList.EE);
          eventType = eventTypeList.item(0);
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_ER, eventType.itemType);
          eventType = eventTypeList.item(5);
          Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
        }

        exiEvent = exiEventList[5];
        Assert.AreEqual(EventDescription_Fields.EVENT_PI, exiEvent.EventKind);
        Assert.AreEqual("def", exiEvent.Name);
        Assert.AreEqual("uvw", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.AreEqual(5, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(6, eventTypeList.Length);
          Assert.IsNotNull(eventTypeList.EE);
          eventType = eventTypeList.item(0);
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_ER, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        }

        exiEvent = exiEventList[6];
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(6, eventTypeList.Length);
          Assert.IsNotNull(eventTypeList.EE);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_ER, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
          eventType = eventTypeList.item(5);
          Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
        }

        exiEvent = exiEventList[7];
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(3, eventTypeList.Length);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
        }
      }
    }

    /// <summary>
    /// Test SE(*), SE(qname), EE and CH in both states (i.e. STATE_IN_TAG and STATE_IN_CONTENT)
    /// of BuiltinElementGrammar with prefix preservation on.
    /// </summary>
    [Test]
    public virtual void testBuiltinSE_EE_CH_Prefixed() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.gram", this);

      short options = GrammarOptions.DEFAULT_OPTIONS;
      options = GrammarOptions.addNS(options);

      GrammarCache grammarCache = new GrammarCache(corpus, options);

      string xmlString;
      byte[] bts;

      xmlString =
        "<foo:None xmlns:foo='urn:foo' xmlns:goo='urn:goo' xmlns:hoo='urn:hoo' xmlns:ioo='urn:ioo'>" +
          "<goo:None/>" +              // SE(*) in STATE_IN_TAG  
          "<foo:None>" +               // SE(*) in STATE_IN_CONTENT   
            "<goo:None/>" +            // SE(goo:None) in STATE_IN_TAG
            "<goo:None/>" +            // SE(*) in STATE_IN_CONTENT
            "<foo:None>" +             // SE(foo:None) in STATE_IN_CONTENT
              "<goo:None/>" +          // SE(goo:None) in STATE_IN_TAG
              "<goo:None/>" +          // SE(goo:None) encounter in STATE_IN_CONTENT
            "</foo:None>" +
          "</foo:None>" +
          "<goo:None/>" +              // SE(goo:None) encounter in STATE_IN_CONTENT
          "<foo:None>" +               // SE(foo:None) in STATE_IN_CONTENT 
            "<foo:AB>abc</foo:AB>" +   // SE(*) in STATE_IN_TAG
            "<foo:AC>def</foo:AC>" +   // SE(*) in STATE_IN_CONTENT
            "<goo:None>" +             // SE(goo:None) encounter in STATE_IN_CONTENT
              "<hoo:None>123</hoo:None>" +            // SE(*) in STATE_IN_TAG
              "<ioo:None><goo:None/>456</ioo:None>" + // SE(*) in STATE_IN_CONTENT
            "</goo:None>" +
            "<foo:None>" +             // SE(foo:None) in STATE_IN_CONTENT
              "<foo:AB>ghi</foo:AB>" + // SE(foo:AB) in STATE_IN_TAG
              "<foo:AC>jkl</foo:AC>" + // SE(foo:AC) in STATE_IN_CONTENT
              "<goo:None>" +           // SE(goo:None) encounter in STATE_IN_CONTENT
                "<hoo:None>789</hoo:None>" +            // SE(hoo:None) in STATE_IN_TAG
                "<ioo:None><goo:None/>012</ioo:None>" + // SE(ioo:None) in STATE_IN_CONTENT
              "</goo:None>" +
            "</foo:None>" +
          "</foo:None>" +
        "</foo:None>\n";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

        decoder.GrammarCache = grammarCache;
        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

        EventDescription exiEvent;

        EventType eventType;
        EventTypeList eventTypeList;

        int n_events = 0;
        int i;

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreSame(exiEvent, eventType);
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.IsNull(eventTypeList.EE);
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("None", exiEvent.Name);
          Assert.AreEqual("urn:foo", exiEvent.URI);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = exiEvent.getEventType();
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(eventTypeList.Length - 1, eventType.Index);
          Assert.IsNull(eventTypeList.EE);

          eventType = eventTypeList.item(0);
          for (i = 1; i < eventTypeList.Length - 1; i++) {
            EventType ith = eventTypeList.item(i);
            if (!(String.Compare(eventType.name, ith.name) < 0)) {
              Assert.AreEqual(eventType.name, ith.name);
              Assert.IsTrue(String.Compare(eventType.uri, ith.uri) < 0);
            }
            eventType = ith;
          }
        }

        for (i = 0; i < 4; i++) {
          if ((exiEvent = scanner.nextEvent()) != null) {
            ++n_events;
            Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
            switch (i) {
              case 0:
                Assert.AreEqual("foo", exiEvent.Prefix);
                Assert.AreEqual("urn:foo", exiEvent.URI);
                Assert.IsTrue(((EXIEventNS)exiEvent).LocalElementNs);
                break;
              case 1:
                Assert.AreEqual("goo", exiEvent.Prefix);
                Assert.AreEqual("urn:goo", exiEvent.URI);
                Assert.IsFalse(((EXIEventNS)exiEvent).LocalElementNs);
                break;
              case 2:
                Assert.AreEqual("hoo", exiEvent.Prefix);
                Assert.AreEqual("urn:hoo", exiEvent.URI);
                Assert.IsFalse(((EXIEventNS)exiEvent).LocalElementNs);
                break;
              case 3:
                Assert.AreEqual("ioo", exiEvent.Prefix);
                Assert.AreEqual("urn:ioo", exiEvent.URI);
                Assert.IsFalse(((EXIEventNS)exiEvent).LocalElementNs);
                break;
              default:
                break;
            }
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
            Assert.IsNull(eventType.name);
            Assert.IsNull(eventType.uri);
            if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
              Assert.AreEqual(2, eventType.Index);
              eventTypeList = eventType.EventTypeList;
              Assert.AreEqual(5, eventTypeList.Length);
              eventType = eventTypeList.item(0);
              Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
              Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
              eventType = eventTypeList.item(1);
              Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
              Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
              eventType = eventTypeList.item(3);
              Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
              eventType = eventTypeList.item(4);
              Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
            }
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("urn:goo", exiEvent.URI);
          Assert.AreEqual("None", exiEvent.Name);
          Assert.AreEqual("goo", exiEvent.Prefix);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(4, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(6, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:goo", eventType.uri);
            Assert.AreEqual("None", eventType.name);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
            eventType = eventTypeList.item(5);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(1, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(6, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(5);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("urn:foo", exiEvent.URI);
          Assert.AreEqual("None", exiEvent.Name);
          Assert.AreEqual("foo", exiEvent.Prefix);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(2, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(4, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:foo", eventType.uri);
            Assert.AreEqual("None", eventType.name);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("urn:goo", exiEvent.URI);
          Assert.AreEqual("None", exiEvent.Name);
          Assert.AreEqual("goo", exiEvent.Prefix);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("urn:goo", eventType.uri);
          Assert.AreEqual("None", eventType.name);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(0, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(6, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(5);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(0, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(6, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(5);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("urn:goo", exiEvent.URI);
          Assert.AreEqual("None", exiEvent.Name);
          Assert.AreEqual("goo", exiEvent.Prefix);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(3, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(5, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:goo", eventType.uri);
            Assert.AreEqual("None", eventType.name);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:foo", eventType.uri);
            Assert.AreEqual("None", eventType.name);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(0, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(6, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(5);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("urn:foo", exiEvent.URI);
          Assert.AreEqual("None", exiEvent.Name);
          Assert.AreEqual("foo", exiEvent.Prefix);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("urn:foo", eventType.uri);
          Assert.AreEqual("None", eventType.name);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(1, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(5, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:goo", eventType.uri);
            Assert.AreEqual("None", eventType.name);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("urn:goo", exiEvent.URI);
          Assert.AreEqual("None", exiEvent.Name);
          Assert.AreEqual("goo", exiEvent.Prefix);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("urn:goo", eventType.uri);
          Assert.AreEqual("None", eventType.name);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(0, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(6, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(5);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(0, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(6, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(5);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("urn:goo", exiEvent.URI);
          Assert.AreEqual("None", exiEvent.Name);
          Assert.AreEqual("goo", exiEvent.Prefix);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("urn:goo", eventType.uri);
          Assert.AreEqual("None", eventType.name);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(0, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(5, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:foo", eventType.uri);
            Assert.AreEqual("None", eventType.name);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(0, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(6, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(5);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(2, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(5, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:goo", eventType.uri);
            Assert.AreEqual("None", eventType.name);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:foo", eventType.uri);
            Assert.AreEqual("None", eventType.name);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(2, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(5, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:goo", eventType.uri);
            Assert.AreEqual("None", eventType.name);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:foo", eventType.uri);
            Assert.AreEqual("None", eventType.name);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("urn:goo", exiEvent.URI);
          Assert.AreEqual("None", exiEvent.Name);
          Assert.AreEqual("goo", exiEvent.Prefix);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("urn:goo", eventType.uri);
          Assert.AreEqual("None", eventType.name);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(0, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(5, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:foo", eventType.uri);
            Assert.AreEqual("None", eventType.name);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(0, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(6, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(5);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("urn:foo", exiEvent.URI);
          Assert.AreEqual("None", exiEvent.Name);
          Assert.AreEqual("foo", exiEvent.Prefix);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("urn:foo", eventType.uri);
          Assert.AreEqual("None", eventType.name);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(1, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(5, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:goo", eventType.uri);
            Assert.AreEqual("None", eventType.name);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("urn:foo", exiEvent.URI);
          Assert.AreEqual("AB", exiEvent.Name);
          Assert.AreEqual("foo", exiEvent.Prefix);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(5, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(7, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:foo", eventType.uri);
            Assert.AreEqual("AB", eventType.name);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:goo", eventType.uri);
            Assert.AreEqual("None", eventType.name);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
            eventType = eventTypeList.item(6);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
          Assert.AreEqual("abc", exiEvent.Characters.makeString());
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(2, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(9, eventTypeList.Length);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
            eventType = eventTypeList.item(5);
            Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
            eventType = eventTypeList.item(6);
            Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
            eventType = eventTypeList.item(7);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(8);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(0, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(3, eventTypeList.Length);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("urn:foo", exiEvent.URI);
          Assert.AreEqual("AC", exiEvent.Name);
          Assert.AreEqual("foo", exiEvent.Prefix);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(4, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(6, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:foo", eventType.uri);
            Assert.AreEqual("AC", eventType.name);
            eventType = eventTypeList.item(1);
            Assert.AreEqual("urn:goo", eventType.uri);
            Assert.AreEqual("None", eventType.name);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:foo", eventType.uri);
            Assert.AreEqual("None", eventType.name);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
            eventType = eventTypeList.item(5);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
          Assert.AreEqual("def", exiEvent.Characters.makeString());
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(2, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(9, eventTypeList.Length);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
            eventType = eventTypeList.item(5);
            Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
            eventType = eventTypeList.item(6);
            Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
            eventType = eventTypeList.item(7);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(8);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(0, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(3, eventTypeList.Length);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("urn:goo", exiEvent.URI);
          Assert.AreEqual("None", exiEvent.Name);
          Assert.AreEqual("goo", exiEvent.Prefix);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("urn:goo", eventType.uri);
          Assert.AreEqual("None", eventType.name);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(1, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(6, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:foo", eventType.uri);
            Assert.AreEqual("AC", eventType.name);
            eventType = eventTypeList.item(2);
            Assert.AreEqual("urn:foo", eventType.uri);
            Assert.AreEqual("None", eventType.name);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(5);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("urn:hoo", exiEvent.URI);
          Assert.AreEqual("None", exiEvent.Name);
          Assert.AreEqual("hoo", exiEvent.Prefix);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(5, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(7, eventTypeList.Length);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:hoo", eventType.uri);
            Assert.AreEqual("None", eventType.name);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
            eventType = eventTypeList.item(6);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
          Assert.AreEqual("123", exiEvent.Characters.makeString());
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(5, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(6, eventTypeList.Length);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(0, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(3, eventTypeList.Length);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("urn:ioo", exiEvent.URI);
          Assert.AreEqual("None", exiEvent.Name);
          Assert.AreEqual("ioo", exiEvent.Prefix);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(2, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(4, eventTypeList.Length);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:ioo", eventType.uri);
            Assert.AreEqual("None", eventType.name);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("urn:goo", exiEvent.URI);
          Assert.AreEqual("None", exiEvent.Name);
          Assert.AreEqual("goo", exiEvent.Prefix);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(4, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(6, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:goo", eventType.uri);
            Assert.AreEqual("None", eventType.name);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
            eventType = eventTypeList.item(5);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(1, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(7, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:hoo", eventType.uri);
            Assert.AreEqual("None", eventType.name);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
            eventType = eventTypeList.item(5);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(6);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
          Assert.AreEqual("456", exiEvent.Characters.makeString());
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(3, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(4, eventTypeList.Length);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(1, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(4, eventTypeList.Length);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(1, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(4, eventTypeList.Length);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:ioo", eventType.uri);
            Assert.AreEqual("None", eventType.name);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("urn:foo", exiEvent.URI);
          Assert.AreEqual("None", exiEvent.Name);
          Assert.AreEqual("foo", exiEvent.Prefix);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("urn:foo", eventType.uri);
          Assert.AreEqual("None", eventType.name);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(2, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(6, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:foo", eventType.uri);
            Assert.AreEqual("AC", eventType.name);
            eventType = eventTypeList.item(1);
            Assert.AreEqual("urn:goo", eventType.uri);
            Assert.AreEqual("None", eventType.name);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(5);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("urn:foo", exiEvent.URI);
          Assert.AreEqual("AB", exiEvent.Name);
          Assert.AreEqual("foo", exiEvent.Prefix);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("urn:foo", eventType.uri);
          Assert.AreEqual("AB", eventType.name);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(0, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(7, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:goo", eventType.uri);
            Assert.AreEqual("None", eventType.name);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
            eventType = eventTypeList.item(5);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(6);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
          Assert.AreEqual("ghi", exiEvent.Characters.makeString());
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(2, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(9, eventTypeList.Length);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
            eventType = eventTypeList.item(5);
            Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
            eventType = eventTypeList.item(6);
            Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
            eventType = eventTypeList.item(7);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(8);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(0, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(3, eventTypeList.Length);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("urn:foo", exiEvent.URI);
          Assert.AreEqual("AC", exiEvent.Name);
          Assert.AreEqual("foo", exiEvent.Prefix);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("urn:foo", eventType.uri);
          Assert.AreEqual("AC", eventType.name);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(0, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(6, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(1);
            Assert.AreEqual("urn:goo", eventType.uri);
            Assert.AreEqual("None", eventType.name);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:foo", eventType.uri);
            Assert.AreEqual("None", eventType.name);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(5);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
          Assert.AreEqual("jkl", exiEvent.Characters.makeString());
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(2, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(9, eventTypeList.Length);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
            eventType = eventTypeList.item(5);
            Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
            eventType = eventTypeList.item(6);
            Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
            eventType = eventTypeList.item(7);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(8);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(0, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(3, eventTypeList.Length);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("urn:goo", exiEvent.URI);
          Assert.AreEqual("None", exiEvent.Name);
          Assert.AreEqual("goo", exiEvent.Prefix);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("urn:goo", eventType.uri);
          Assert.AreEqual("None", eventType.name);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(1, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(6, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:foo", eventType.uri);
            Assert.AreEqual("AC", eventType.name);
            eventType = eventTypeList.item(2);
            Assert.AreEqual("urn:foo", eventType.uri);
            Assert.AreEqual("None", eventType.name);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(5);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("urn:hoo", exiEvent.URI);
          Assert.AreEqual("None", exiEvent.Name);
          Assert.AreEqual("hoo", exiEvent.Prefix);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("urn:hoo", eventType.uri);
          Assert.AreEqual("None", eventType.name);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(0, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(7, eventTypeList.Length);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
            eventType = eventTypeList.item(5);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(6);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
          Assert.AreEqual("789", exiEvent.Characters.makeString());
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(0, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(6, eventTypeList.Length);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(5);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(0, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(3, eventTypeList.Length);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("urn:ioo", exiEvent.URI);
          Assert.AreEqual("None", exiEvent.Name);
          Assert.AreEqual("ioo", exiEvent.Prefix);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("urn:ioo", eventType.uri);
          Assert.AreEqual("None", eventType.name);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(0, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(4, eventTypeList.Length);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("urn:goo", exiEvent.URI);
          Assert.AreEqual("None", exiEvent.Name);
          Assert.AreEqual("goo", exiEvent.Prefix);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("urn:goo", eventType.uri);
          Assert.AreEqual("None", eventType.name);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(0, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(6, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(5);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(1, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(7, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:hoo", eventType.uri);
            Assert.AreEqual("None", eventType.name);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
            eventType = eventTypeList.item(5);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(6);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
          Assert.AreEqual("012", exiEvent.Characters.makeString());
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(0, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(4, eventTypeList.Length);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(1, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(4, eventTypeList.Length);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(1, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(4, eventTypeList.Length);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:ioo", eventType.uri);
            Assert.AreEqual("None", eventType.name);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(3, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(6, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:foo", eventType.uri);
            Assert.AreEqual("AC", eventType.name);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:goo", eventType.uri);
            Assert.AreEqual("None", eventType.name);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:foo", eventType.uri);
            Assert.AreEqual("None", eventType.name);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(5);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(3, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(6, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:foo", eventType.uri);
            Assert.AreEqual("AC", eventType.name);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:goo", eventType.uri);
            Assert.AreEqual("None", eventType.name);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:foo", eventType.uri);
            Assert.AreEqual("None", eventType.name);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(5);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(3, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(6, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:foo", eventType.uri);
            Assert.AreEqual("AC", eventType.name);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:goo", eventType.uri);
            Assert.AreEqual("None", eventType.name);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:foo", eventType.uri);
            Assert.AreEqual("None", eventType.name);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(5);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreSame(exiEvent, eventType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(0, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(1, eventTypeList.Length);
          }
        }

        Assert.AreEqual(60, n_events);
      }
    }

    /// <summary>
    /// Test AT(*), AT(qname) of BuiltinElementGrammar with prefix preservation on.
    /// </summary>
    [Test]
    public virtual void testBuiltinAT_Prefixed() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributes.gram", this);

      short options = GrammarOptions.DEFAULT_OPTIONS;
      options = GrammarOptions.addNS(options);

      GrammarCache grammarCache = new GrammarCache(corpus, options);

      string xmlString;
      byte[] bts;

      xmlString =
        "<foo:None xmlns:foo='urn:foo' xmlns:goo='urn:goo' " +
        "          goo:none='abc' foo:aA='true' >" +
          "<foo:None foo:aA='false' goo:none='def' />" + // SE(*) in STATE_IN_TAG
        "</foo:None>\n";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

        decoder.GrammarCache = grammarCache;
        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

        EventDescription exiEvent;

        EventType eventType;
        EventTypeList eventTypeList;

        int n_events = 0;
        int i;

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreSame(exiEvent, eventType);
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.IsNull(eventTypeList.EE);
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("None", exiEvent.Name);
          Assert.AreEqual("urn:foo", exiEvent.URI);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = exiEvent.getEventType();
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(eventTypeList.Length - 1, eventType.Index);
          Assert.IsNull(eventTypeList.EE);

          eventType = eventTypeList.item(0);
          for (i = 1; i < eventTypeList.Length - 1; i++) {
            EventType ith = eventTypeList.item(i);
            if (!(String.Compare(eventType.name, ith.name) < 0)) {
              Assert.AreEqual(eventType.name, ith.name);
              Assert.IsTrue(String.Compare(eventType.uri, ith.uri) < 0);
            }
            eventType = ith;
          }
        }

        for (i = 0; i < 2; i++) {
          if ((exiEvent = scanner.nextEvent()) != null) {
            ++n_events;
            Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
            switch (i) {
              case 0:
                Assert.AreEqual("foo", exiEvent.Prefix);
                Assert.AreEqual("urn:foo", exiEvent.URI);
                Assert.IsTrue(((EXIEventNS)exiEvent).LocalElementNs);
                break;
              case 1:
                Assert.AreEqual("goo", exiEvent.Prefix);
                Assert.AreEqual("urn:goo", exiEvent.URI);
                Assert.IsFalse(((EXIEventNS)exiEvent).LocalElementNs);
                break;
              default:
                break;
            }
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
            Assert.IsNull(eventType.name);
            Assert.IsNull(eventType.uri);
            if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
              Assert.AreEqual(2, eventType.Index);
              eventTypeList = eventType.EventTypeList;
              Assert.AreEqual(5, eventTypeList.Length);
              eventType = eventTypeList.item(0);
              Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
              Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
              eventType = eventTypeList.item(1);
              Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
              Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
              eventType = eventTypeList.item(3);
              Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
              eventType = eventTypeList.item(4);
              Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
            }
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
          Assert.AreEqual("urn:foo", exiEvent.URI);
          Assert.AreEqual("aA", exiEvent.Name);
          Assert.AreEqual("foo", exiEvent.Prefix);
          Assert.AreEqual("true", exiEvent.Characters.makeString());
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(2, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(6, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_AT, eventType.itemType);
            Assert.AreEqual("urn:foo", eventType.uri);
            Assert.AreEqual("aA", eventType.name);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(5);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
          Assert.AreEqual("urn:goo", exiEvent.URI);
          Assert.AreEqual("none", exiEvent.Name);
          Assert.AreEqual("goo", exiEvent.Prefix);
          Assert.AreEqual("abc", exiEvent.Characters.makeString());
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(3, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(7, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_AT, eventType.itemType);
            Assert.AreEqual("urn:goo", eventType.uri);
            Assert.AreEqual("none", eventType.name);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_AT, eventType.itemType);
            Assert.AreEqual("urn:foo", eventType.uri);
            Assert.AreEqual("aA", eventType.name);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
            eventType = eventTypeList.item(5);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(6);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("urn:foo", exiEvent.URI);
          Assert.AreEqual("None", exiEvent.Name);
          Assert.AreEqual("foo", exiEvent.Prefix);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(6, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(8, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:foo", eventType.uri);
            Assert.AreEqual("None", eventType.name);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_AT, eventType.itemType);
            Assert.AreEqual("urn:goo", eventType.uri);
            Assert.AreEqual("none", eventType.name);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_AT, eventType.itemType);
            Assert.AreEqual("urn:foo", eventType.uri);
            Assert.AreEqual("aA", eventType.name);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(5);
            Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
            eventType = eventTypeList.item(7);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
          Assert.AreEqual("urn:foo", exiEvent.URI);
          Assert.AreEqual("aA", exiEvent.Name);
          Assert.AreEqual("foo", exiEvent.Prefix);
          Assert.AreEqual("false", exiEvent.Characters.makeString());
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_AT, eventType.itemType);
          Assert.AreEqual("urn:foo", eventType.uri);
          Assert.AreEqual("aA", eventType.name);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(2, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(8, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:foo", eventType.uri);
            Assert.AreEqual("None", eventType.name);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_AT, eventType.itemType);
            Assert.AreEqual("urn:goo", eventType.uri);
            Assert.AreEqual("none", eventType.name);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(5);
            Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
            eventType = eventTypeList.item(6);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(7);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
          Assert.AreEqual("urn:goo", exiEvent.URI);
          Assert.AreEqual("none", exiEvent.Name);
          Assert.AreEqual("goo", exiEvent.Prefix);
          Assert.AreEqual("def", exiEvent.Characters.makeString());
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_AT, eventType.itemType);
          Assert.AreEqual("urn:goo", eventType.uri);
          Assert.AreEqual("none", eventType.name);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(1, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(8, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:foo", eventType.uri);
            Assert.AreEqual("None", eventType.name);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_AT, eventType.itemType);
            Assert.AreEqual("urn:foo", eventType.uri);
            Assert.AreEqual("aA", eventType.name);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(5);
            Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
            eventType = eventTypeList.item(6);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(7);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(4, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(9, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:foo", eventType.uri);
            Assert.AreEqual("None", eventType.name);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_AT, eventType.itemType);
            Assert.AreEqual("urn:goo", eventType.uri);
            Assert.AreEqual("none", eventType.name);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_AT, eventType.itemType);
            Assert.AreEqual("urn:foo", eventType.uri);
            Assert.AreEqual("aA", eventType.name);
            eventType = eventTypeList.item(5);
            Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(6);
            Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
            eventType = eventTypeList.item(7);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(8);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(0, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(3, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreSame(exiEvent, eventType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(0, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(1, eventTypeList.Length);
          }
        }

        Assert.AreEqual(12, n_events);
      }
    }

    /// <summary>
    /// Initially no schema definition associated, switching to "foo:finalString" via xsi:type.
    /// </summary>
    [Test]
    public virtual void testBuiltinXsiType_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.gram", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string xmlString;
      byte[] bts;

      xmlString =
        "<foo:None xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' >" +
          "<foo:None2 xsi:type='foo:finalString'>abc</foo:None2>" +
          "<foo:None2 xsi:type='foo:finalString'>abc</foo:None2>" +
        "</foo:None>";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

        decoder.GrammarCache = grammarCache;
        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

        EventDescription exiEvent;

        EventType eventType;
        EventTypeList eventTypeList;

        int n_events = 0;
        int i;

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreSame(exiEvent, eventType);
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.IsNull(eventTypeList.EE);
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("None", exiEvent.Name);
          Assert.AreEqual("urn:foo", exiEvent.URI);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = exiEvent.getEventType();
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(eventTypeList.Length - 1, eventType.Index);
          Assert.IsNull(eventTypeList.EE);

          eventType = eventTypeList.item(0);
          for (i = 1; i < eventTypeList.Length - 1; i++) {
            EventType ith = eventTypeList.item(i);
            if (!(String.Compare(eventType.name, ith.name) < 0)) {
              Assert.AreEqual(eventType.name, ith.name);
              Assert.IsTrue(String.Compare(eventType.uri, ith.uri) < 0);
            }
            eventType = ith;
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("None2", exiEvent.Name);
          Assert.AreEqual("urn:foo", exiEvent.URI);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(3, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(5, eventTypeList.Length);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:foo", eventType.uri);
            Assert.AreEqual("None2", eventType.name);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
          Assert.AreEqual("type", exiEvent.Name);
          Assert.IsNull(exiEvent.Prefix);
          Assert.AreEqual(null, exiEvent.Characters);
          Assert.AreEqual("finalString", ((EXIEventSchemaType)exiEvent).TypeName);
          Assert.AreEqual("urn:foo", ((EXIEventSchemaType)exiEvent).TypeURI);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(2, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(5, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_AT, eventType.itemType);
            Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
            Assert.AreEqual("type", eventType.name);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
          Assert.AreEqual("abc", exiEvent.Characters.makeString());
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(0, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(1, eventTypeList.Length);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(0, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(1, eventTypeList.Length);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("None2", exiEvent.Name);
          Assert.AreEqual("urn:foo", exiEvent.URI);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(2, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(4, eventTypeList.Length);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:foo", eventType.uri);
            Assert.AreEqual("None2", eventType.name);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
          Assert.AreEqual("type", exiEvent.Name);
          Assert.IsNull(exiEvent.Prefix);
          Assert.AreEqual(null, exiEvent.Characters);
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
          Assert.AreEqual("finalString", ((EXIEventSchemaType)exiEvent).TypeName);
          Assert.AreEqual("urn:foo", ((EXIEventSchemaType)exiEvent).TypeURI);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_AT, eventType.itemType);
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
          Assert.AreEqual("type", eventType.name);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(0, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(5, eventTypeList.Length);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
          Assert.AreEqual("abc", exiEvent.Characters.makeString());
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(0, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(1, eventTypeList.Length);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(0, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(1, eventTypeList.Length);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(1, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(4, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:foo", eventType.uri);
            Assert.AreEqual("None2", eventType.name);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreSame(exiEvent, eventType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(0, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(1, eventTypeList.Length);
          }
        }

        Assert.AreEqual(12, n_events);
      }
    }

    /// <summary>
    /// From EXI interoperability test suite.
    /// Schema-Informed Declared Production tests - document
    /// schemaInformed.declared.document-01
    /// </summary>
    [Test]
    public virtual void testBuiltinXsiType_02() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/interop/schemaInformedGrammar/declaredProductions/document.gram", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string xmlString;
      byte[] bts;

      xmlString =
      "<None xmlns:xsd='http://www.w3.org/2001/XMLSchema' " +
        "xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " +
        "xmlns='urn:foo' xsi:type='xsd:anyType' />";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

        decoder.GrammarCache = grammarCache;
        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

        EventDescription exiEvent;

        EventType eventType;
        EventTypeList eventTypeList;

        int n_events = 0;
        int i;

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreSame(exiEvent, eventType);
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.IsNull(eventTypeList.EE);
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("None", exiEvent.Name);
          Assert.AreEqual("urn:foo", exiEvent.URI);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = exiEvent.getEventType();
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(eventTypeList.Length - 1, eventType.Index);
          Assert.IsNull(eventTypeList.EE);
          eventType = eventTypeList.item(0);
          for (i = 1; i < eventTypeList.Length - 1; i++) {
            EventType ith = eventTypeList.item(i);
            if (!(String.Compare(eventType.name, ith.name) < 0)) {
              Assert.AreEqual(eventType.name, ith.name);
              Assert.IsTrue(String.Compare(eventType.uri, ith.uri) < 0);
            }
            eventType = ith;
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
          Assert.AreEqual("type", exiEvent.Name);
          Assert.IsNull(exiEvent.Prefix);
          Assert.AreEqual(null, exiEvent.Characters);
          Assert.AreEqual("anyType", ((EXIEventSchemaType)exiEvent).TypeName);
          Assert.AreEqual("http://www.w3.org/2001/XMLSchema", ((EXIEventSchemaType)exiEvent).TypeURI);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(2, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(5, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_AT, eventType.itemType);
            Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
            Assert.AreEqual("type", eventType.name);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
          Assert.AreEqual(2, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(4, eventTypeList.Length);
          eventType = eventTypeList.item(0);
          Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreSame(exiEvent, eventType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(0, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(1, eventTypeList.Length);
          }
        }

        Assert.AreEqual(5, n_events);
      }
    }

    /// <summary>
    /// From EXI interoperability test suite.
    /// Schema-Informed Declared Production tests - document
    /// schemaInformed.declared.document-01
    /// </summary>
    [Test]
    public virtual void testBuiltinXsiType_02_docodeOnly() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/interop/schemaInformedGrammar/declaredProductions/document.gram", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;

      decoder.GrammarCache = grammarCache;
      decoder.AlignmentType = AlignmentType.byteAligned;

      /*
       * <None xmlns:xsd="http://www.w3.org/2001/XMLSchema" 
       *   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       *   xmlns='urn:foo' xsi:type="xsd:anyType" />
       */
      Uri url = resolveSystemIdAsURL("/interop/schemaInformedGrammar/declaredProductions/document-01.byteAligned");

      FileStream inputStream = new FileStream(url.LocalPath, FileMode.Open);
      decoder.InputStream = inputStream;
      scanner = decoder.processHeader();

      EventDescription exiEvent;

      EventType eventType;
      EventTypeList eventTypeList;

      int n_events = 0;
      int i;

      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
      }

      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("None", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = exiEvent.getEventType();
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(eventTypeList.Length - 1, eventType.Index);
        Assert.IsNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        for (i = 1; i < eventTypeList.Length - 1; i++) {
          EventType ith = eventTypeList.item(i);
          if (!(String.Compare(eventType.name, ith.name) < 0)) {
            Assert.AreEqual(eventType.name, ith.name);
            Assert.IsTrue(String.Compare(eventType.uri, ith.uri) < 0);
          }
          eventType = ith;
        }
      }

      for (i = 0; i < 3; i++) {
        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
          switch (i) {
            case 0:
              Assert.AreEqual("xsd", exiEvent.Prefix);
              Assert.AreEqual("http://www.w3.org/2001/XMLSchema", exiEvent.URI);
              Assert.IsFalse(((EXIEventNS)exiEvent).LocalElementNs);
              break;
            case 1:
              Assert.AreEqual("xsi", exiEvent.Prefix);
              Assert.AreEqual("http://www.w3.org/2001/XMLSchema-instance", exiEvent.URI);
              Assert.IsFalse(((EXIEventNS)exiEvent).LocalElementNs);
              break;
            case 2:
              Assert.AreEqual("", exiEvent.Prefix);
              Assert.AreEqual("urn:foo", exiEvent.URI);
              Assert.IsTrue(((EXIEventNS)exiEvent).LocalElementNs);
              break;
            default:
              break;
          }
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
          Assert.IsNull(eventType.name);
          Assert.IsNull(eventType.uri);
          Assert.AreEqual(2, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(5, eventTypeList.Length);
          eventType = eventTypeList.item(0);
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        }
      }

      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
        Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
        Assert.AreEqual("type", exiEvent.Name);
        Assert.AreEqual("xsi", exiEvent.Prefix);
        Assert.AreEqual(null, exiEvent.Characters);
        Assert.AreEqual("anyType", ((EXIEventSchemaType)exiEvent).TypeName);
        Assert.AreEqual("http://www.w3.org/2001/XMLSchema", ((EXIEventSchemaType)exiEvent).TypeURI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(6, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_AT, eventType.itemType);
        Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
        Assert.AreEqual("type", eventType.name);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
      }

      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        Assert.AreEqual(4, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(11, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(10);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
      }

      if ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
      }

      Assert.AreEqual(8, n_events);

      inputStream.Close();
    }

    /// <summary>
    /// Use of xsi:type in schema-less EXI stream.
    /// </summary>
    [Test]
    public virtual void testBuiltinXsiType_03() {

      GrammarCache grammarCache = new GrammarCache((EXISchema)null, GrammarOptions.STRICT_OPTIONS);

      string xmlString;
      byte[] bts;

      xmlString =
      "<None xmlns:xsd='http://www.w3.org/2001/XMLSchema' " +
        "xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " +
        "xmlns='urn:foo' xsi:type='xsd:anyType' />";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

        decoder.GrammarCache = grammarCache;
        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

        EventDescription exiEvent;

        EventType eventType;
        EventTypeList eventTypeList;

        int n_events = 0;
        int i;

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreSame(exiEvent, eventType);
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.IsNull(eventTypeList.EE);
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("None", exiEvent.Name);
          Assert.AreEqual("urn:foo", exiEvent.URI);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = exiEvent.getEventType();
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(eventTypeList.Length - 1, eventType.Index);
          Assert.IsNull(eventTypeList.EE);
          eventType = eventTypeList.item(0);
          for (i = 1; i < eventTypeList.Length - 1; i++) {
            EventType ith = eventTypeList.item(i);
            if (!(String.Compare(eventType.name, ith.name) < 0)) {
              Assert.AreEqual(eventType.name, ith.name);
              Assert.IsTrue(String.Compare(eventType.uri, ith.uri) < 0);
            }
            eventType = ith;
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
          Assert.AreEqual("type", exiEvent.Name);
          Assert.IsNull(exiEvent.Prefix);
          Assert.AreEqual(null, exiEvent.Characters);
          Assert.AreEqual("anyType", ((EXIEventSchemaType)exiEvent).TypeName);
          Assert.AreEqual("http://www.w3.org/2001/XMLSchema", ((EXIEventSchemaType)exiEvent).TypeURI);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(2, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(5, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_AT, eventType.itemType);
            Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
            Assert.AreEqual("type", eventType.name);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          Assert.AreEqual(2, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(6, eventTypeList.Length);
          eventType = eventTypeList.item(0);
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_AT, eventType.itemType);
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
          Assert.AreEqual("type", eventType.name);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(5);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreSame(exiEvent, eventType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(0, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(1, eventTypeList.Length);
          }
        }

        Assert.AreEqual(5, n_events);
      }
    }

    [Test]
    public virtual void testBuiltinXsiNil_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.gram", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string xmlString;
      byte[] bts;

      xmlString =
        "<foo:None xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' >" +
          "<foo:None2 xsi:nil='true'>abc</foo:None2>" +
          "<foo:None2 foo:aA='xyz' xsi:nil='true'>abc</foo:None2>" +
        "</foo:None>";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

        decoder.GrammarCache = grammarCache;
        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

        EventDescription exiEvent;

        EventType eventType;
        EventTypeList eventTypeList;

        int n_events = 0;
        int i;

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreSame(exiEvent, eventType);
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.IsNull(eventTypeList.EE);
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("None", exiEvent.Name);
          Assert.AreEqual("urn:foo", exiEvent.URI);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = exiEvent.getEventType();
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(eventTypeList.Length - 1, eventType.Index);
          Assert.IsNull(eventTypeList.EE);

          eventType = eventTypeList.item(0);
          for (i = 1; i < eventTypeList.Length - 1; i++) {
            EventType ith = eventTypeList.item(i);
            if (!(String.Compare(eventType.name, ith.name) < 0)) {
              Assert.AreEqual(eventType.name, ith.name);
              Assert.IsTrue(String.Compare(eventType.uri, ith.uri) < 0);
            }
            eventType = ith;
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("None2", exiEvent.Name);
          Assert.AreEqual("urn:foo", exiEvent.URI);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(3, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(5, eventTypeList.Length);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:foo", eventType.uri);
            Assert.AreEqual("None2", eventType.name);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
          Assert.AreEqual("nil", exiEvent.Name);
          Assert.IsNull(exiEvent.Prefix);
          Assert.AreEqual("true", exiEvent.Characters.makeString());
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(2, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(5, eventTypeList.Length);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_AT, eventType.itemType);
            Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
            Assert.AreEqual("nil", eventType.name);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
          Assert.AreEqual("abc", exiEvent.Characters.makeString());
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(5, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(6, eventTypeList.Length);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_AT, eventType.itemType);
            Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
            Assert.AreEqual("nil", eventType.name);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(0, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(3, eventTypeList.Length);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("None2", exiEvent.Name);
          Assert.AreEqual("urn:foo", exiEvent.URI);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(2, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(4, eventTypeList.Length);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:foo", eventType.uri);
            Assert.AreEqual("None2", eventType.name);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
          Assert.AreEqual("nil", exiEvent.Name);
          Assert.IsNull(exiEvent.Prefix);
          Assert.AreEqual("true", exiEvent.Characters.makeString());
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_AT, eventType.itemType);
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
          Assert.AreEqual("nil", eventType.name);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(1, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(6, eventTypeList.Length);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(5);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
          Assert.AreEqual("urn:foo", exiEvent.URI);
          Assert.AreEqual("aA", exiEvent.Name);
          Assert.AreEqual(null, exiEvent.Prefix);
          Assert.AreEqual("xyz", exiEvent.Characters.makeString());
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(4, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(7, eventTypeList.Length);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_AT, eventType.itemType);
            Assert.AreEqual("urn:foo", eventType.uri);
            Assert.AreEqual("aA", eventType.name);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_AT, eventType.itemType);
            Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
            Assert.AreEqual("nil", eventType.name);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(5);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(6);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
          Assert.AreEqual("abc", exiEvent.Characters.makeString());
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(1, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(7, eventTypeList.Length);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_AT, eventType.itemType);
            Assert.AreEqual("urn:foo", eventType.uri);
            Assert.AreEqual("aA", eventType.name);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_AT, eventType.itemType);
            Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
            Assert.AreEqual("nil", eventType.name);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(5);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(6);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(0, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(3, eventTypeList.Length);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(1, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(4, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:foo", eventType.uri);
            Assert.AreEqual("None2", eventType.name);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreSame(exiEvent, eventType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(0, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(1, eventTypeList.Length);
          }
        }

        Assert.AreEqual(13, n_events);
      }
    }

    /// <summary>
    /// Schema:
    /// None available
    /// 
    /// Instance:
    /// <!-- abc --><None/><!-- def -->
    /// </summary>
    [Test]
    public virtual void testBuiltinDocumentComment() {

      GrammarCache grammarCache = new GrammarCache(GrammarOptions.addCM(GrammarOptions.DEFAULT_OPTIONS));

      string xmlString;
      byte[] bts;
      int n_events;

      xmlString = "<!-- abc --><None xmlns='urn:foo' /><!-- def -->\n";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

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
        Assert.AreEqual(1, eventTypeList.Length);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CM, exiEvent.EventKind);
        Assert.AreEqual(" abc ", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(2, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("None", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(2, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          Assert.AreEqual(1, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(6, eventTypeList.Length);
          eventType = eventTypeList.item(0);
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          eventType = eventTypeList.item(5);
          Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        }
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CM, exiEvent.EventKind);
        Assert.AreEqual(" def ", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(2, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(2, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        ++n_events;

        Assert.AreEqual(6, n_events);
      }
    }

    /// <summary>
    /// The localName of an element is well-known from schema.
    /// However, the element is processed using a built-in element grammar
    /// because the element is not declared in schema. Make sure the
    /// built-in element grammar is reset across runs.
    /// 
    /// Schema:
    /// <xsd:simpleType name="B">
    ///   <xsd:restriction base="xsd:string" />
    /// </xsd:simpleType>
    /// 
    /// Instance:
    /// <B/>
    /// </summary>
    [Test]
    public virtual void testBuiltinElement_With_KnownLocalName() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/oneSimpleType.gram", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      const string xmlString = "<B/>";

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;
        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));
        byte[] bts = baos.ToArray();

        // Repeat twice to make sure Built-in Element Grammars with schema-indexed localNames
        // are reset properly across runs.
        for (int i = 0; i < 2; i++) {
          decoder.InputStream = new MemoryStream(bts);
          Scanner scanner = decoder.processHeader();

          EventDescription exiEvent;
          EventType eventType;
          EventTypeList eventTypeList;

          int n_events = 0;

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
          Assert.AreEqual("B", exiEvent.Name);
          Assert.AreEqual("", exiEvent.URI);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(1, eventTypeList.Length);
          ++n_events;

          exiEvent = scanner.nextEvent();
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(1, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(5, eventTypeList.Length);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreSame(exiEvent, eventType);
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(1, eventTypeList.Length);
          ++n_events;

          Assert.AreEqual(4, n_events);
        }
      }
    }

    [Test]
    public virtual void testDecodeAntExample01() {

      GrammarCache grammarCache = new GrammarCache(null, GrammarOptions.DEFAULT_OPTIONS);

      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;

      decoder.AlignmentType = AlignmentType.bitPacked;
      decoder.GrammarCache = grammarCache;
      Uri url = resolveSystemIdAsURL("/Ant/build-build.bitPacked");
      FileStream inputStream = new FileStream(url.LocalPath, FileMode.Open);
      decoder.InputStream = inputStream;
      scanner = decoder.processHeader();

      EventDescription exiEvent;

      int n_events = 0;
      int n_undeclaredCharacters = 0;
      int n_attributes = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        sbyte eventVariety;
        if ((eventVariety = exiEvent.EventKind) == EventDescription_Fields.EVENT_CH) {
          if (exiEvent.getEventType().itemType == EventType.ITEM_CH) {
            ++n_undeclaredCharacters;
            continue;
          }
        }
        else if (eventVariety == EventDescription_Fields.EVENT_AT) {
          ++n_attributes;
        }
        ++n_events;
      }
      inputStream.Close();

      Assert.AreEqual(401, n_events);
      Assert.AreEqual(0, n_undeclaredCharacters);
      Assert.AreEqual(171, n_attributes);
    }

    /// <summary>
    /// Simple-content whitespaces are preserved.
    /// </summary>
    [Test]
    public virtual void testWhitespaces_01() {

      GrammarCache grammarCache = new GrammarCache((EXISchema)null, GrammarOptions.DEFAULT_OPTIONS);

      string xmlString;
      byte[] bts;
      int n_events;

      xmlString = "<None xmlns='urn:foo'> \n\t</None>\n";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

        decoder.GrammarCache = grammarCache;
        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

        List<EventDescription> exiEventList = new List<EventDescription>();

        EventDescription exiEvent;
        n_events = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          exiEventList.Add(exiEvent);
        }

        Assert.AreEqual(5, n_events);

        EventType eventType;

        exiEvent = exiEventList[0];
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);

        exiEvent = exiEventList[1];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("None", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);

        exiEvent = exiEventList[2];
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual(" \n\t", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);

        exiEvent = exiEventList[3];
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);

        exiEvent = exiEventList[4];
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
      }
    }

    /// <summary>
    /// Complex-content whitespaces are *not* preserved unless there is any
    /// non-whitespace content.
    /// </summary>
    [Test]
    public virtual void testWhitespaces_02() {

      GrammarCache grammarCache = new GrammarCache((EXISchema)null, GrammarOptions.DEFAULT_OPTIONS);

      string xmlString;
      byte[] bts;
      int n_events;

      xmlString = "<None xmlns='urn:foo'> <!-- comment --><A/> </None>\n";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

        decoder.GrammarCache = grammarCache;
        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

        List<EventDescription> exiEventList = new List<EventDescription>();

        EventDescription exiEvent;
        n_events = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          exiEventList.Add(exiEvent);
        }

        Assert.AreEqual(6, n_events);

        EventType eventType;

        exiEvent = exiEventList[0];
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);

        exiEvent = exiEventList[1];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("None", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);

        exiEvent = exiEventList[2];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("A", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);

        exiEvent = exiEventList[3];
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);

        exiEvent = exiEventList[4];
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);

        exiEvent = exiEventList[5];
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
      }
    }

    /// <summary>
    /// Complex-content whitespaces are preserved when there is some
    /// non-whitespace content such as a comment.
    /// </summary>
    [Test]
    public virtual void testWhitespaces_03() {

      GrammarCache grammarCache = new GrammarCache((EXISchema)null, GrammarOptions.addCM(GrammarOptions.DEFAULT_OPTIONS));

      string xmlString;
      byte[] bts;
      int n_events;

      xmlString = "<None xmlns='urn:foo'>\t<!-- comment --><A/> </None>\n";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

        decoder.GrammarCache = grammarCache;
        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

        List<EventDescription> exiEventList = new List<EventDescription>();

        EventDescription exiEvent;
        n_events = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          exiEventList.Add(exiEvent);
        }

        Assert.AreEqual(8, n_events);

        EventType eventType;

        exiEvent = exiEventList[0];
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);

        exiEvent = exiEventList[1];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("None", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);

        exiEvent = exiEventList[2];
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("\t", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);

        exiEvent = exiEventList[3];
        Assert.AreEqual(EventDescription_Fields.EVENT_CM, exiEvent.EventKind);
        Assert.AreEqual(" comment ", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);

        exiEvent = exiEventList[4];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("A", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);

        exiEvent = exiEventList[5];
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);

        exiEvent = exiEventList[6];
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);

        exiEvent = exiEventList[7];
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
      }
    }

    /// <summary>
    /// Complex-content whitespaces are *not* preserved unless there is any
    /// non-whitespace content.
    /// </summary>
    [Test]
    public virtual void testWhitespaces_04() {

      GrammarCache grammarCache = new GrammarCache((EXISchema)null, GrammarOptions.DEFAULT_OPTIONS);

      string xmlString;
      byte[] bts;
      int n_events;

      xmlString = "<None xmlns='urn:foo'> <A/><!-- comment --> </None>\n";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

        decoder.GrammarCache = grammarCache;
        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

        List<EventDescription> exiEventList = new List<EventDescription>();

        EventDescription exiEvent;
        n_events = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          exiEventList.Add(exiEvent);
        }

        Assert.AreEqual(6, n_events);

        EventType eventType;

        exiEvent = exiEventList[0];
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);

        exiEvent = exiEventList[1];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("None", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);

        exiEvent = exiEventList[2];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("A", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);

        exiEvent = exiEventList[3];
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);

        exiEvent = exiEventList[4];
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);

        exiEvent = exiEventList[5];
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
      }
    }

    /// <summary>
    /// Complex-content whitespaces are preserved when there is some
    /// non-whitespace content such as a comment.
    /// </summary>
    [Test]
    public virtual void testWhitespaces_05() {

      GrammarCache grammarCache = new GrammarCache((EXISchema)null, GrammarOptions.addCM(GrammarOptions.DEFAULT_OPTIONS));

      string xmlString;
      byte[] bts;
      int n_events;

      xmlString = "<None xmlns='urn:foo'> <A/><!-- comment -->\t</None>\n";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

        decoder.GrammarCache = grammarCache;
        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

        List<EventDescription> exiEventList = new List<EventDescription>();

        EventDescription exiEvent;
        n_events = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          exiEventList.Add(exiEvent);
        }

        Assert.AreEqual(8, n_events);

        EventType eventType;

        exiEvent = exiEventList[0];
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);

        exiEvent = exiEventList[1];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("None", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);

        exiEvent = exiEventList[2];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("A", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);

        exiEvent = exiEventList[3];
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);

        exiEvent = exiEventList[4];
        Assert.AreEqual(EventDescription_Fields.EVENT_CM, exiEvent.EventKind);
        Assert.AreEqual(" comment ", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);

        exiEvent = exiEventList[5];
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("\t", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);

        exiEvent = exiEventList[6];
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);

        exiEvent = exiEventList[7];
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
      }
    }

    /// <summary>
    /// <foo foo='foo at value' /> consists of event sequence:
    /// SD SE(foo) AT(foo) EE ED
    /// Local-name "foo" is literally encoded twice in the stream on purpose.
    /// </summary>
    [Test]
    public virtual void testDecodeDuplicateLocalNames_01() {

      GrammarCache grammarCache = new GrammarCache(null, GrammarOptions.DEFAULT_OPTIONS);

      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;

      decoder.AlignmentType = AlignmentType.byteAligned;
      decoder.GrammarCache = grammarCache;
      Uri url = resolveSystemIdAsURL("/perversion/duplicateLocalNames_01.byteAligned");
      FileStream inputStream = new FileStream(url.LocalPath, FileMode.Open);
      decoder.InputStream = inputStream;
      scanner = decoder.processHeader();

      List<EventDescription> exiEventList = new List<EventDescription>();

      EventDescription exiEvent;
      int n_events = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        exiEventList.Add(exiEvent);
      }
      inputStream.Close();

      Assert.AreEqual(5, n_events);

      EventType eventType;

      exiEvent = exiEventList[0];
      Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SD, eventType.itemType);

      exiEvent = exiEventList[1];
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      Assert.AreEqual("foo", exiEvent.Name);
      Assert.AreEqual("", exiEvent.URI);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);

      exiEvent = exiEventList[2];
      Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
      Assert.AreEqual("foo", exiEvent.Name);
      Assert.AreEqual("", exiEvent.URI);
      Assert.AreEqual("foo at value", exiEvent.Characters.makeString());
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);

      exiEvent = exiEventList[3];
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
      Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

      exiEvent = exiEventList[4];
      Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
    }

    /// <summary>
    /// <bla:foo xmlns:bla="uri:bla" bla:foo="bla:foo at value"/> consists of event sequence:
    /// SD SE(bla:foo) AT(bla:foo) EE ED
    /// URI "uri:bla" and local-name "foo" are both literally encoded twice 
    /// in the stream on purpose.
    /// </summary>
    [Test]
    public virtual void testDecodeDuplicateURIsLocalNames_01() {

      GrammarCache grammarCache = new GrammarCache(null, GrammarOptions.DEFAULT_OPTIONS);

      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;

      decoder.AlignmentType = AlignmentType.byteAligned;
      decoder.GrammarCache = grammarCache;
      Uri url = resolveSystemIdAsURL("/perversion/duplicateURIsLocalNames_01.byteAligned");
      FileStream inputStream = new FileStream(url.LocalPath, FileMode.Open);
      decoder.InputStream = inputStream;
      scanner = decoder.processHeader();

      List<EventDescription> exiEventList = new List<EventDescription>();

      EventDescription exiEvent;
      int n_events = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        exiEventList.Add(exiEvent);
      }
      inputStream.Close();

      Assert.AreEqual(5, n_events);

      EventType eventType;

      exiEvent = exiEventList[0];
      Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SD, eventType.itemType);

      exiEvent = exiEventList[1];
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      Assert.AreEqual("foo", exiEvent.Name);
      Assert.AreEqual("uri:bla", exiEvent.URI);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);

      exiEvent = exiEventList[2];
      Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
      Assert.AreEqual("foo", exiEvent.Name);
      Assert.AreEqual("uri:bla", exiEvent.URI);
      Assert.AreEqual("bla:foo at value", exiEvent.Characters.makeString());
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);

      exiEvent = exiEventList[3];
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
      Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

      exiEvent = exiEventList[4];
      Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
    }

  }

}