using System;
using System.IO;
using NUnit.Framework;

using Org.System.Xml.Sax;

using EXIDecoder = Nagasena.Proc.EXIDecoder;
using EXISchemaResolver = Nagasena.Proc.EXISchemaResolver;
using HeaderOptionsOutputType = Nagasena.Proc.HeaderOptionsOutputType;
using AlignmentType = Nagasena.Proc.Common.AlignmentType;
using EXIOptions = Nagasena.Proc.Common.EXIOptions;
using EventCode = Nagasena.Proc.Common.EventCode;
using EventType = Nagasena.Proc.Common.EventType;
using EventTypeList = Nagasena.Proc.Common.EventTypeList;
using EventDescription = Nagasena.Proc.Common.EventDescription;
using EventDescription_Fields = Nagasena.Proc.Common.EventDescription_Fields;
using GrammarOptions = Nagasena.Proc.Common.GrammarOptions;
using SchemaId = Nagasena.Proc.Common.SchemaId;
using XmlUriConst = Nagasena.Proc.Common.XmlUriConst;
using EXIEventNS = Nagasena.Proc.Events.EXIEventNS;
using EXIEventSchemaNil = Nagasena.Proc.Events.EXIEventSchemaNil;
using GrammarCache = Nagasena.Proc.Grammars.GrammarCache;
using OptionsGrammarCache = Nagasena.Proc.Grammars.OptionsGrammarCache;
using Scanner = Nagasena.Proc.IO.Scanner;
using ExiUriConst = Nagasena.Proc.Util.ExiUriConst;
using EXISchema = Nagasena.Schema.EXISchema;
using EmptySchema = Nagasena.Schema.EmptySchema;
using TestBase = Nagasena.Schema.TestBase;
using EXISchemaFactoryTestUtil = Nagasena.Scomp.EXISchemaFactoryTestUtil;

namespace Nagasena.Sax {

  [TestFixture]
  [Category("Enable_Compression")]
  public class OptionsDocumentTest : TestBase {

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
    /// Schema:
    /// 
    /// Instance:
    /// </summary>
    [Test]
    public virtual void testSchemaId_01() {
      GrammarCache grammarCache = OptionsGrammarCache.GrammarCache;

      Transmogrifier encoder = new Transmogrifier();
      encoder.GrammarCache = grammarCache;
      MemoryStream baos = new MemoryStream();
      encoder.OutputStream = baos;

      string xmlString;
      byte[] bts;
      EXIDecoder decoder;
      Scanner scanner;
      int n_events;

      xmlString = 
        "<header xmlns='http://www.w3.org/2009/exi' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>\n" +
        "  <common>\n" +
        "    <schemaId xsi:nil='true'/>\n" +
        "  </common>\n" +
        "</header>\n";

      encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

      bts = baos.ToArray();

      decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;
      decoder.InputStream = new MemoryStream(bts);
      scanner = decoder.processHeader();
      Assert.IsNull(scanner.HeaderOptions);

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
      Assert.AreEqual("header", exiEvent.Name);
      Assert.AreEqual(ExiUriConst.W3C_2009_EXI_URI, exiEvent.URI);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      Assert.AreEqual("header", eventType.name);
      Assert.AreEqual(ExiUriConst.W3C_2009_EXI_URI, eventType.uri);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.AreEqual(2, eventTypeList.Length);
      eventType = eventTypeList.item(1);
      Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      Assert.AreEqual("common", exiEvent.Name);
      Assert.AreEqual(ExiUriConst.W3C_2009_EXI_URI, exiEvent.URI);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      Assert.AreEqual("common", eventType.name);
      Assert.AreEqual(ExiUriConst.W3C_2009_EXI_URI, eventType.uri);
      Assert.AreEqual(1, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.AreEqual(4, eventTypeList.Length);
      eventType = eventTypeList.item(0);
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      Assert.AreEqual("lesscommon", eventType.name);
      Assert.AreEqual(ExiUriConst.W3C_2009_EXI_URI, eventType.uri);
      eventType = eventTypeList.item(2);
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      Assert.AreEqual("strict", eventType.name);
      Assert.AreEqual(ExiUriConst.W3C_2009_EXI_URI, eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      Assert.AreEqual("schemaId", exiEvent.Name);
      Assert.AreEqual(ExiUriConst.W3C_2009_EXI_URI, exiEvent.URI);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      Assert.AreEqual("schemaId", eventType.name);
      Assert.AreEqual(ExiUriConst.W3C_2009_EXI_URI, eventType.uri);
      Assert.AreEqual(2, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.AreEqual(4, eventTypeList.Length);
      eventType = eventTypeList.item(0);
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      Assert.AreEqual("compression", eventType.name);
      Assert.AreEqual(ExiUriConst.W3C_2009_EXI_URI, eventType.uri);
      eventType = eventTypeList.item(1);
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      Assert.AreEqual("fragment", eventType.name);
      Assert.AreEqual(ExiUriConst.W3C_2009_EXI_URI, eventType.uri);
      eventType = eventTypeList.item(3);
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_NL, exiEvent.EventKind);
      Assert.AreEqual("nil", exiEvent.Name);
      Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
      Assert.IsTrue(((EXIEventSchemaNil)exiEvent).Nilled);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
      Assert.AreEqual("nil", eventType.name);
      Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.AreEqual(2, eventTypeList.Length);
      eventType = eventTypeList.item(1);
      Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      eventTypeList = eventType.EventTypeList;
      Assert.AreEqual(1, eventTypeList.Length);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.AreEqual(1, eventTypeList.Length);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
      Assert.AreEqual(1, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.AreEqual(2, eventTypeList.Length);
      eventType = eventTypeList.item(0);
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      Assert.AreEqual("strict", eventType.name);
      Assert.AreEqual(ExiUriConst.W3C_2009_EXI_URI, eventType.uri);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      ++n_events;

      Assert.AreEqual(9, n_events);
    }

    [Test]
    public virtual void testAlignmentOption_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/optionsSchema.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        encoder.GrammarCache = grammarCache;
        encoder.AlignmentType = alignment;
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
        // DO NOT SET AlignmentType for decoder.
        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

        EventDescription exiEvent;
        EventType eventType;
        n_events = 0;

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

    [Test]
    public virtual void testStrictOption_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/optionsSchema.xsc", this);

      GrammarCache encodeGrammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
      GrammarCache decodeGrammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        encoder.GrammarCache = encodeGrammarCache;
        encoder.AlignmentType = alignment;
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
        decoder.GrammarCache = decodeGrammarCache;
        // DO NOT SET AlignmentType for decoder.
        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

        EventDescription exiEvent;
        EventType eventType;
        n_events = 0;

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

    [Test]
    public virtual void testPreserveCommentsOption_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/optionsSchema.xsc", this);

      GrammarCache encodeGrammarCache = new GrammarCache(corpus, GrammarOptions.addCM(GrammarOptions.DEFAULT_OPTIONS));
      GrammarCache decodeGrammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        encoder.GrammarCache = encodeGrammarCache;
        encoder.AlignmentType = alignment;
        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;
        encoder.OutputOptions = HeaderOptionsOutputType.lessSchemaId;

        string xmlString;
        byte[] bts;
        EXIDecoder decoder;
        Scanner scanner;
        int n_events;

        xmlString = "<header xmlns='http://www.w3.org/2009/exi'><!-- A --><strict/><!-- B --></header>\n";

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

        decoder = new EXIDecoder();
        decoder.GrammarCache = decodeGrammarCache;
        // DO NOT SET AlignmentType for decoder.
        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

        EventDescription exiEvent;
        EventType eventType;
        n_events = 0;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("header", exiEvent.Name);
        Assert.AreEqual(ExiUriConst.W3C_2009_EXI_URI, exiEvent.URI);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CM, exiEvent.EventKind);
        Assert.AreEqual(" A ", exiEvent.Characters.makeString());
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
        Assert.AreEqual(EventDescription_Fields.EVENT_CM, exiEvent.EventKind);
        Assert.AreEqual(" B ", exiEvent.Characters.makeString());
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

        Assert.AreEqual(8, n_events);
      }
    }

    [Test]
    public virtual void testPreserveProcessingInstructionOption_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/optionsSchema.xsc", this);

      GrammarCache encodeGrammarCache = new GrammarCache(corpus, GrammarOptions.addPI(GrammarOptions.DEFAULT_OPTIONS));
      GrammarCache decodeGrammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        encoder.GrammarCache = encodeGrammarCache;
        encoder.AlignmentType = alignment;
        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;
        encoder.OutputOptions = HeaderOptionsOutputType.lessSchemaId;

        string xmlString;
        byte[] bts;
        EXIDecoder decoder;
        Scanner scanner;
        int n_events;

        xmlString = "<header xmlns='http://www.w3.org/2009/exi'><?eg Good! ?><strict/><?eg Good? ?></header>\n";

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

        decoder = new EXIDecoder();
        decoder.GrammarCache = decodeGrammarCache;
        // DO NOT SET AlignmentType for decoder.
        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

        EventDescription exiEvent;
        EventType eventType;

        n_events = 0;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("header", exiEvent.Name);
        Assert.AreEqual(ExiUriConst.W3C_2009_EXI_URI, exiEvent.URI);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_PI, exiEvent.EventKind);
        Assert.AreEqual("Good! ", exiEvent.Characters.makeString());
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
        Assert.AreEqual(EventDescription_Fields.EVENT_PI, exiEvent.EventKind);
        Assert.AreEqual("Good? ", exiEvent.Characters.makeString());
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

        Assert.AreEqual(8, n_events);
      }
    }

    [Test]
    public virtual void testPreserveDtdOption_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/optionsSchema.xsc", this);

      GrammarCache encodeGrammarCache = new GrammarCache(corpus, GrammarOptions.addDTD(GrammarOptions.DEFAULT_OPTIONS));
      GrammarCache decodeGrammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        encoder.ResolveExternalGeneralEntities = false;
        encoder.GrammarCache = encodeGrammarCache;
        encoder.AlignmentType = alignment;
        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;
        encoder.OutputOptions = HeaderOptionsOutputType.lessSchemaId;

        string xmlString;
        byte[] bts;
        EXIDecoder decoder;
        Scanner scanner;
        int n_events;

        xmlString = "<!DOCTYPE header [ <!ENTITY ent SYSTEM 'er-entity.xml'> ]>" +
          "<header xmlns='http://www.w3.org/2009/exi'>&ent;<strict/></header>\n";

        InputSource inputSource = new InputSource<Stream>(string2Stream(xmlString));
        inputSource.SystemId = resolveSystemIdAsURL("/").ToString();
        encoder.encode(inputSource);

        bts = baos.ToArray();

        decoder = new EXIDecoder();
        decoder.GrammarCache = decodeGrammarCache;
        // DO NOT SET AlignmentType for decoder.
        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

        EventDescription exiEvent;
        EventType eventType;
        n_events = 0;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_DTD, exiEvent.EventKind);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("header", exiEvent.Name);
        Assert.AreEqual(ExiUriConst.W3C_2009_EXI_URI, exiEvent.URI);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ER, exiEvent.EventKind);
        Assert.AreEqual("ent", exiEvent.Name);
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

        Assert.AreEqual(8, n_events);
      }
    }

    [Test]
    public virtual void testPreservePrefixesOption_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/optionsSchema.xsc", this);

      GrammarCache encodeGrammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));
      GrammarCache decodeGrammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        encoder.GrammarCache = encodeGrammarCache;
        encoder.AlignmentType = alignment;
        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;
        encoder.OutputOptions = HeaderOptionsOutputType.lessSchemaId;

        string xmlString;
        byte[] bts;
        EXIDecoder decoder;
        Scanner scanner;
        int n_events;

        xmlString = "<exi:header xmlns:exi='http://www.w3.org/2009/exi'><exi:strict/></exi:header>\n";

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

        decoder = new EXIDecoder();
        decoder.GrammarCache = decodeGrammarCache;
        // DO NOT SET AlignmentType for decoder.
        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

        EventDescription exiEvent;
        EventType eventType;
        n_events = 0;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("header", exiEvent.Name);
        Assert.AreEqual(ExiUriConst.W3C_2009_EXI_URI, exiEvent.URI);
        Assert.IsNull(exiEvent.Prefix);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
        Assert.AreEqual("exi", exiEvent.Prefix);
        Assert.AreEqual("http://www.w3.org/2009/exi", exiEvent.URI);
        Assert.IsTrue(((EXIEventNS)exiEvent).LocalElementNs);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("strict", exiEvent.Name);
        Assert.AreEqual(ExiUriConst.W3C_2009_EXI_URI, exiEvent.URI);
        Assert.AreEqual("exi", exiEvent.Prefix);
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

        Assert.AreEqual(7, n_events);
      }
    }

    [Test]
    public virtual void testSchemaIdOption_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/optionsSchema.xsc", this);

      GrammarCache encodeGrammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
      GrammarCache decodeGrammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        encoder.setGrammarCache(encodeGrammarCache, new SchemaId("aiueo"));
        encoder.AlignmentType = alignment;
        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;
        encoder.OutputOptions = HeaderOptionsOutputType.all;

        string xmlString;
        byte[] bts;
        EXIDecoder decoder;
        Scanner scanner;
        int n_events;
        EXIOptions headerOptions;

        xmlString = "<header xmlns='http://www.w3.org/2009/exi'><strict/></header>\n";

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

        decoder = new EXIDecoder();
        decoder.GrammarCache = decodeGrammarCache;
        // DO NOT SET AlignmentType for decoder.
        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();
        headerOptions = scanner.HeaderOptions;
        Assert.AreEqual("aiueo", headerOptions.SchemaId.Value);

        EventDescription exiEvent;
        EventType eventType;
        n_events = 0;

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

    /// <summary>
    /// Use HeaderOptionsOutputType of value lessSchemaId to suppress 
    /// schemaId in header options.
    /// </summary>
    [Test]
    public virtual void testSchemaIdOption_02() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/optionsSchema.xsc", this);

      GrammarCache encodeGrammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);
      GrammarCache decodeGrammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        encoder.setGrammarCache(encodeGrammarCache, new SchemaId("aiueo"));
        encoder.AlignmentType = alignment;
        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;
        encoder.OutputOptions = HeaderOptionsOutputType.lessSchemaId;

        string xmlString;
        byte[] bts;
        EXIDecoder decoder;
        Scanner scanner;
        int n_events;
        EXIOptions headerOptions;

        xmlString = "<header xmlns='http://www.w3.org/2009/exi'><strict/></header>\n";

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

        decoder = new EXIDecoder();
        decoder.GrammarCache = decodeGrammarCache;
        // DO NOT SET AlignmentType for decoder.
        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();
        headerOptions = scanner.HeaderOptions;
        Assert.IsNull(headerOptions.SchemaId);

        EventDescription exiEvent;
        EventType eventType;
        n_events = 0;

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

    /// <summary>
    /// Let the decoder fetch a grammar cache via EXISchemaResolver.
    /// </summary>
    [Test]
    public virtual void testSchemaIdOption_03() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/optionsSchema.xsc", this);

      GrammarCache encodeGrammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        encoder.setGrammarCache(encodeGrammarCache, new SchemaId("aiueo"));
        encoder.AlignmentType = alignment;
        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;
        encoder.OutputOptions = HeaderOptionsOutputType.all;

        string xmlString;
        byte[] bts;
        EXIDecoder decoder;
        Scanner scanner;
        int n_events;
        EXIOptions headerOptions;

        xmlString = "<header xmlns='http://www.w3.org/2009/exi'><strict/></header>\n";

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

        decoder = new EXIDecoder();
        decoder.EXISchemaResolver = new EXISchemaResolverAnonymousInnerClassHelper(this, corpus);
        // DO NOT SET AlignmentType for decoder.
        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();
        headerOptions = scanner.HeaderOptions;
        Assert.AreEqual("aiueo", headerOptions.SchemaId.Value);

        EventDescription exiEvent;
        EventType eventType;
        n_events = 0;

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

    private class EXISchemaResolverAnonymousInnerClassHelper : EXISchemaResolver {
      private readonly OptionsDocumentTest outerInstance;

      private EXISchema corpus;

      public EXISchemaResolverAnonymousInnerClassHelper(OptionsDocumentTest outerInstance, EXISchema corpus) {
        this.outerInstance = outerInstance;
        this.corpus = corpus;
      }

      public virtual GrammarCache resolveSchema(string schemaId, short grammarOptions) {
        if ("aiueo".Equals(schemaId)) {
          return new GrammarCache(corpus, grammarOptions);
        }
        Assert.Fail();
        return null;
      }
    }

    [Test]
    public virtual void testSchemaIdOptionNil_01() {

      GrammarCache grammarCache = new GrammarCache((EXISchema)null, GrammarOptions.addCM(GrammarOptions.DEFAULT_OPTIONS));

      string xmlString;
      byte[] bts;
      int n_events;

      xmlString = "<None xmlns='urn:foo'><!-- abc --><!-- def --></None>\n";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();

        encoder.OutputOptions = HeaderOptionsOutputType.all;
        encoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

        EXIDecoder decoder = new EXIDecoder();
        decoder.InputStream = new MemoryStream(bts);
        Scanner scanner = decoder.processHeader();

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
        Assert.AreEqual("None", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CM, exiEvent.EventKind);
        Assert.AreEqual(" abc ", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CM, exiEvent.EventKind);
        Assert.AreEqual(" def ", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        ++n_events;

        Assert.AreEqual(6, n_events);
      }
    }

    [Test]
    public virtual void testSelfContainedOption_01() {
      Transmogrifier encoder = new Transmogrifier();
      encoder.GrammarCache = OptionsGrammarCache.GrammarCache;
      MemoryStream baos = new MemoryStream();
      encoder.OutputStream = baos;
      encoder.OutputOptions = HeaderOptionsOutputType.none;

      string xmlString;
      EXIDecoder decoder;
      Scanner scanner;

      xmlString = "<header xmlns='http://www.w3.org/2009/exi'><lesscommon><uncommon><selfContained/></uncommon></lesscommon></header>\n";

      encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

      byte[] bts = baos.ToArray();
      /// <summary>
      /// 0011 0000 where the third bit (Presence Bit) is on.
      /// This effectively makes the encoded body appear as an header options. 
      /// </summary>
      bts[0] = 0xB0;

      decoder = new EXIDecoder();
      decoder.GrammarCache = new GrammarCache(EmptySchema.EXISchema, GrammarOptions.STRICT_OPTIONS);
      decoder.InputStream = new MemoryStream(bts);
      scanner = decoder.processHeader();

      EXIOptions options = scanner.HeaderOptions;
      Assert.IsTrue(options.InfuseSC);
    }

  }

}