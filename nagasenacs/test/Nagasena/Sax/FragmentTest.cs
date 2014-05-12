using System;
using System.IO;
using System.Collections.Generic;
using NUnit.Framework;

using Org.System.Xml.Sax;

using EXIDecoder = Nagasena.Proc.EXIDecoder;
using HeaderOptionsOutputType = Nagasena.Proc.HeaderOptionsOutputType;
using AlignmentType = Nagasena.Proc.Common.AlignmentType;
using EventDescription = Nagasena.Proc.Common.EventDescription;
using EventDescription_Fields = Nagasena.Proc.Common.EventDescription_Fields;
using EXIOptions = Nagasena.Proc.Common.EXIOptions;
using EventCode = Nagasena.Proc.Common.EventCode;
using EventType = Nagasena.Proc.Common.EventType;
using EventTypeList = Nagasena.Proc.Common.EventTypeList;
using GrammarOptions = Nagasena.Proc.Common.GrammarOptions;
using QName = Nagasena.Proc.Common.QName;
using StringTable = Nagasena.Proc.Common.StringTable;
using XmlUriConst = Nagasena.Proc.Common.XmlUriConst;
using EXIEventSchemaNil = Nagasena.Proc.Events.EXIEventSchemaNil;
using EXIEventSchemaType = Nagasena.Proc.Events.EXIEventSchemaType;
using Apparatus = Nagasena.Proc.Grammars.Apparatus;
using BuiltinGrammar = Nagasena.Proc.Grammars.BuiltinGrammar;
using EventTypeSchema = Nagasena.Proc.Grammars.EventTypeSchema;
using Grammar = Nagasena.Proc.Grammars.Grammar;
using GrammarCache = Nagasena.Proc.Grammars.GrammarCache;
using GrammarState = Nagasena.Proc.Grammars.GrammarState;
using SchemaInformedGrammar = Nagasena.Proc.Grammars.SchemaInformedGrammar;
using Scanner = Nagasena.Proc.IO.Scanner;
using Scribble = Nagasena.Proc.IO.Scribble;
using Scriber = Nagasena.Proc.IO.Scriber;
using ScriberFactory = Nagasena.Proc.IO.ScriberFactory;
using ValueScriber = Nagasena.Proc.IO.ValueScriber;
using EXISchema = Nagasena.Schema.EXISchema;
using EXISchemaConst = Nagasena.Schema.EXISchemaConst;
using EXISchemaUtil = Nagasena.Schema.EXISchemaUtil;
using TestBase = Nagasena.Schema.TestBase;
using EXISchemaFactoryTestUtil = Nagasena.Scomp.EXISchemaFactoryTestUtil;
using Event = Org.W3C.Exi.Ttf.Event;
using SAXRecorder = Org.W3C.Exi.Ttf.Sax.SAXRecorder;

namespace Nagasena.Sax {

  [TestFixture]
  public class FragmentTest : TestBase {

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
    /// The element "A" leads to use a specific element grammar.
    /// </summary>
    [Test]
    public virtual void testSchemaInformedFragment_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/fragment_01.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string xmlString;
      byte[] bts;

      xmlString = "<A xmlns='urn:goo'>true</A>";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        encoder.AlignmentType = alignment;
        encoder.Fragment = true;
        encoder.OutputOptions = HeaderOptionsOutputType.lessSchemaId;

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
        GrammarState grammarState;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(9, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A__", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
       /// <summary>
       /// This assertion is not relevant in Nagasena
       /// if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
       ///   grammarState = scanner.getGrammarState();
       ///   Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_ELEMENT, grammarState.targetGrammar.grammarType);
       ///   Assert.AreEqual(SchemaInformedGrammar.ELEMENT_STATE_UNBOUND, grammarState.phase);
       /// }
       /// </summary>

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("true", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          int tp = scanner.currentState.contentDatatype;
          Assert.AreEqual(EXISchemaConst.BOOLEAN_TYPE, corpus.getSerialOfType(tp));
        }
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
       /// <summary>
       /// This assertion is not relevant in Nagasena
       /// if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
       ///   grammarState = scanner.getGrammarState();
       ///   Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_ELEMENT_CONTENT, grammarState.targetGrammar.grammarType);
       ///   Assert.AreEqual(SchemaInformedGrammar.ELEMENT_STATE_CONTENT_COMPLETE, grammarState.phase);
       /// }
       /// </summary>

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(8, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(9, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A__", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(SchemaInformedGrammar.DOCUMENT_STATE_END, grammarState.phase);
        }

        Assert.IsNull(scanner.nextEvent());
      }
    }

    /// <summary>
    /// Multiple root elements.
    /// </summary>
    [Test]
    public virtual void testSchemaInformedFragment_02() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/fragment_01.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      Scribble scribble = new Scribble();

      int booleanType = corpus.getBuiltinTypeOfSchema(EXISchemaConst.BOOLEAN_TYPE);

      foreach (AlignmentType alignment in Alignments) {
        Scriber scriber = ScriberFactory.createScriber(alignment);
        scriber.setSchema(grammarCache.EXISchema, (QName[])null, 0);
        scriber.PreserveNS = GrammarOptions.hasNS(grammarCache.grammarOptions);
        StringTable stringTable = Scriber.createStringTable(grammarCache);
        scriber.StringTable = stringTable;
        scriber.ValueMaxLength = EXIOptions.VALUE_MAX_LENGTH_UNBOUNDED;

        ValueScriber booleanValueScriber = scriber.getValueScriber(booleanType);

        MemoryStream baos = new MemoryStream();
        scriber.OutputStream = baos;

        Scriber.writeHeaderPreamble(baos, false, false);

        Grammar documentGrammar = grammarCache.retrieveRootGrammar(true, scriber.eventTypesWorkSpace);
        documentGrammar.init(scriber.currentState);

        EventTypeList eventTypes;
        EventType eventType;
        QName qname = new QName();

        eventTypes = scriber.NextEventTypes;

        eventType = eventTypes.item(0);
        Assert.AreEqual(EventType.ITEM_SD, eventType.itemType);
        Assert.AreEqual(1, eventTypes.Length);
        scriber.writeEventType(eventType);
        scriber.startDocument();
        eventTypes = scriber.NextEventTypes;
        Assert.AreEqual(9, eventTypes.Length);

        eventType = eventTypes.item(0);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        scriber.writeEventType(eventType);
        scriber.writeQName(qname.setValue("urn:goo", "A", null), eventType);
        scriber.startElement(eventType);
        eventTypes = scriber.NextEventTypes;
        Assert.AreEqual(1, eventTypes.Length);

        eventType = eventTypes.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        scriber.writeEventType(eventType);
        booleanValueScriber.process("true", booleanType, corpus, scribble, scriber);
        booleanValueScriber.scribe("true", scribble, qname.localNameId, qname.uriId, booleanType, scriber);
        scriber.characters(eventType);
        eventTypes = scriber.NextEventTypes;
        Assert.AreEqual(1, eventTypes.Length);

        eventType = eventTypes.item(0);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        scriber.writeEventType(eventType);
        scriber.endElement();
        eventTypes = scriber.NextEventTypes;
        Assert.AreEqual(9, eventTypes.Length);

        eventType = eventTypes.item(0);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        scriber.writeEventType(eventType);
        scriber.writeQName(qname.setValue("urn:goo", "A", null), eventType);
        scriber.startElement(eventType);
        eventTypes = scriber.NextEventTypes;
        Assert.AreEqual(1, eventTypes.Length);

        eventType = eventTypes.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        scriber.writeEventType(eventType);
        booleanValueScriber.process("false", booleanType, corpus, scribble, scriber);
        booleanValueScriber.scribe("false", scribble, qname.localNameId, qname.uriId, booleanType, scriber);
        scriber.characters(eventType);
        eventTypes = scriber.NextEventTypes;
        Assert.AreEqual(1, eventTypes.Length);

        eventType = eventTypes.item(0);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        scriber.writeEventType(eventType);
        scriber.endElement();
        eventTypes = scriber.NextEventTypes;
        Assert.AreEqual(9, eventTypes.Length);

        eventType = eventTypes.item(8);
        Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
        scriber.writeEventType(eventType);
        scriber.endDocument();
        eventTypes = scriber.NextEventTypes;
        Assert.AreEqual(0, eventTypes.Length);

        scriber.finish();


        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        decoder.AlignmentType = alignment;
        decoder.Fragment = true;

        decoder.GrammarCache = grammarCache;
        decoder.InputStream = new MemoryStream(baos.ToArray());
        scanner = decoder.processHeader();

        EventDescription exiEvent;
        EventTypeList eventTypeList;
        GrammarState grammarState;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(9, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A__", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
       /// <summary>
       /// This assertion is not relevant in Nagasena
       /// if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
       ///   grammarState = scanner.getGrammarState();
       ///   Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_ELEMENT, grammarState.targetGrammar.grammarType);
       ///   Assert.AreEqual(SchemaInformedGrammar.ELEMENT_STATE_UNBOUND, grammarState.phase);
       /// }
       /// </summary>

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("true", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          int tp = scanner.currentState.contentDatatype;
          Assert.AreEqual(EXISchemaConst.BOOLEAN_TYPE, corpus.getSerialOfType(tp));
        }
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
       /// <summary>
       /// This assertion is not relevant in Nagasena
       /// if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
       ///   grammarState = scanner.getGrammarState();
       ///   Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_ELEMENT_CONTENT, grammarState.targetGrammar.grammarType);
       ///   Assert.AreEqual(SchemaInformedGrammar.ELEMENT_STATE_CONTENT_COMPLETE, grammarState.phase);
       /// }
       /// </summary>

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(9, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A__", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
       /// <summary>
       /// This assertion is not relevant in Nagasena
       /// if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
       ///   grammarState = scanner.getGrammarState();
       ///   Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_ELEMENT, grammarState.targetGrammar.grammarType);
       ///   Assert.AreEqual(SchemaInformedGrammar.ELEMENT_STATE_UNBOUND, grammarState.phase);
       /// }
       /// </summary>

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("false", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          int tp = scanner.currentState.contentDatatype;
          Assert.AreEqual(EXISchemaConst.BOOLEAN_TYPE, corpus.getSerialOfType(tp));
        }
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
       /// <summary>
       /// This assertion is not relevant in Nagasena
       /// if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
       ///   grammarState = scanner.getGrammarState();
       ///   Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_ELEMENT_CONTENT, grammarState.targetGrammar.grammarType);
       ///   Assert.AreEqual(SchemaInformedGrammar.ELEMENT_STATE_CONTENT_COMPLETE, grammarState.phase);
       /// }
       /// </summary>

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(8, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(9, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A__", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(SchemaInformedGrammar.DOCUMENT_STATE_END, grammarState.phase);
        }

        Assert.IsNull(scanner.nextEvent());
      }
    }

    /// <summary>
    /// Encode multiple root elements, then parse with EXIReader.
    /// </summary>
    [Test]
    public virtual void testSchemaInformedFragment_03() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/fragment_01.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      Scribble scribble = new Scribble();

      int booleanType = corpus.getBuiltinTypeOfSchema(EXISchemaConst.BOOLEAN_TYPE);

      foreach (AlignmentType alignment in Alignments) {
        Scriber scriber = ScriberFactory.createScriber(alignment);
        scriber.setSchema(grammarCache.EXISchema, (QName[])null, 0);
        scriber.PreserveNS = GrammarOptions.hasNS(grammarCache.grammarOptions);
        StringTable stringTable = Scriber.createStringTable(grammarCache);
        scriber.StringTable = stringTable;
        scriber.ValueMaxLength = EXIOptions.VALUE_MAX_LENGTH_UNBOUNDED;

        ValueScriber booleanValueScriber = scriber.getValueScriber(booleanType);

        MemoryStream baos = new MemoryStream();
        scriber.OutputStream = baos;

        Scriber.writeHeaderPreamble(baos, false, false);

        Grammar documentGrammar = grammarCache.retrieveRootGrammar(true, scriber.eventTypesWorkSpace);
        documentGrammar.init(scriber.currentState);

        EventTypeList eventTypes;
        EventType eventType;
        QName qname = new QName();

        eventTypes = scriber.NextEventTypes;

        eventType = eventTypes.item(0);
        Assert.AreEqual(EventType.ITEM_SD, eventType.itemType);
        Assert.AreEqual(1, eventTypes.Length);
        scriber.writeEventType(eventType);
        scriber.startDocument();
        eventTypes = scriber.NextEventTypes;
        Assert.AreEqual(9, eventTypes.Length);

        eventType = eventTypes.item(0);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        scriber.writeEventType(eventType);
        scriber.writeQName(qname.setValue("urn:goo", "A", null), eventType);
        scriber.startElement(eventType);
        eventTypes = scriber.NextEventTypes;
        Assert.AreEqual(1, eventTypes.Length);

        eventType = eventTypes.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        scriber.writeEventType(eventType);
        booleanValueScriber.process("true", booleanType, corpus, scribble, scriber);
        booleanValueScriber.scribe("true", scribble, qname.localNameId, qname.uriId, booleanType, scriber);
        scriber.characters(eventType);
        eventTypes = scriber.NextEventTypes;
        Assert.AreEqual(1, eventTypes.Length);

        eventType = eventTypes.item(0);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        scriber.writeEventType(eventType);
        scriber.endElement();
        eventTypes = scriber.NextEventTypes;
        Assert.AreEqual(9, eventTypes.Length);

        eventType = eventTypes.item(0);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        scriber.writeEventType(eventType);
        scriber.writeQName(qname.setValue("urn:goo", "A", null), eventType);
        scriber.startElement(eventType);
        eventTypes = scriber.NextEventTypes;
        Assert.AreEqual(1, eventTypes.Length);

        eventType = eventTypes.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        scriber.writeEventType(eventType);
        booleanValueScriber.process("false", booleanType, corpus, scribble, scriber);
        booleanValueScriber.scribe("false", scribble, qname.localNameId, qname.uriId, booleanType, scriber);
        scriber.characters(eventType);
        eventTypes = scriber.NextEventTypes;
        Assert.AreEqual(1, eventTypes.Length);

        eventType = eventTypes.item(0);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        scriber.writeEventType(eventType);
        scriber.endElement();
        eventTypes = scriber.NextEventTypes;
        Assert.AreEqual(9, eventTypes.Length);

        eventType = eventTypes.item(8);
        Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
        scriber.writeEventType(eventType);
        scriber.endDocument();
        eventTypes = scriber.NextEventTypes;
        Assert.AreEqual(0, eventTypes.Length);

        scriber.finish();


        EXIReader decoder = new EXIReader();

        decoder.AlignmentType = alignment;
        decoder.Fragment = true;

        decoder.GrammarCache = grammarCache;

        List<Event> exiEventList = new List<Event>();
        SAXRecorder saxRecorder = new SAXRecorder(exiEventList, true);
        decoder.ContentHandler = saxRecorder;
        decoder.LexicalHandler = saxRecorder;

        decoder.Parse(new InputSource<Stream>(new MemoryStream(baos.ToArray())));

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
        Assert.AreEqual("urn:foo", saxEvent.@namespace);
        Assert.AreEqual("s0", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
        Assert.AreEqual("urn:goo", saxEvent.@namespace);
        Assert.AreEqual("s1", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.START_ELEMENT, saxEvent.type);
        Assert.AreEqual("urn:goo", saxEvent.@namespace);
        Assert.AreEqual("A", saxEvent.localName);
        Assert.AreEqual("s1:A", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.CHARACTERS, saxEvent.type);
        Assert.AreEqual("true", new string(saxEvent.charValue));

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.END_ELEMENT, saxEvent.type);
        Assert.AreEqual("urn:goo", saxEvent.@namespace);
        Assert.AreEqual("A", saxEvent.localName);
        Assert.AreEqual("s1:A", saxEvent.name);

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
        Assert.AreEqual("s0", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
        Assert.AreEqual("urn:goo", saxEvent.@namespace);
        Assert.AreEqual("s1", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.START_ELEMENT, saxEvent.type);
        Assert.AreEqual("urn:goo", saxEvent.@namespace);
        Assert.AreEqual("A", saxEvent.localName);
        Assert.AreEqual("s1:A", saxEvent.name);

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.CHARACTERS, saxEvent.type);
        Assert.AreEqual("false", new string(saxEvent.charValue));

        saxEvent = exiEventList[n++];
        Assert.AreEqual(Event.END_ELEMENT, saxEvent.type);
        Assert.AreEqual("urn:goo", saxEvent.@namespace);
        Assert.AreEqual("A", saxEvent.localName);
        Assert.AreEqual("s1:A", saxEvent.name);

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

        Assert.AreEqual(exiEventList.Count, n);
      }
    }

    /// <summary>
    /// Test BuiltinFragmentGrammar with a single root element.
    /// </summary>
    [Test]
    public virtual void testBuiltinFragment_01() {

      GrammarCache grammarCache = new GrammarCache((EXISchema)null, GrammarOptions.STRICT_OPTIONS);

      string xmlString;
      byte[] bts;

      xmlString = "<A xmlns='urn:goo'>true</A>";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        encoder.AlignmentType = alignment;
        encoder.Fragment = true;

        decoder.AlignmentType = alignment;
        decoder.Fragment = true;

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
        GrammarState grammarState;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.BUILTIN_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.BUILTIN_GRAMMAR_ELEMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(BuiltinGrammar.ELEMENT_STATE_IN_TAG, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("true", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        Assert.AreEqual(4, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.BUILTIN_GRAMMAR_ELEMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(BuiltinGrammar.ELEMENT_STATE_IN_CONTENT, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.Length);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.BUILTIN_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.BUILTIN_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(SchemaInformedGrammar.DOCUMENT_STATE_END, grammarState.phase);
        }

        Assert.IsNull(scanner.nextEvent());
      }
    }

    /// <summary>
    /// Multiple root elements.
    /// </summary>
    [Test]
    public virtual void testBuiltinFragment_02() {
      GrammarCache grammarCache = new GrammarCache((EXISchema)null, GrammarOptions.STRICT_OPTIONS);

      Scribble scribble = new Scribble();

      QName qname = new QName();

      foreach (AlignmentType alignment in Alignments) {
        Scriber scriber = ScriberFactory.createScriber(alignment);
        scriber.setSchema(grammarCache.EXISchema, (QName[])null, 0);
        scriber.PreserveNS = GrammarOptions.hasNS(grammarCache.grammarOptions);
        StringTable stringTable = Scriber.createStringTable(grammarCache);
        scriber.StringTable = stringTable;
        scriber.ValueMaxLength = EXIOptions.VALUE_MAX_LENGTH_UNBOUNDED;

        ValueScriber stringValueScriber = scriber.getValueScriberByID(Apparatus.CODEC_STRING);

        MemoryStream baos = new MemoryStream();
        scriber.OutputStream = baos;

        Scriber.writeHeaderPreamble(baos, false, false);

        Grammar documentGrammar;
        documentGrammar = grammarCache.retrieveRootGrammar(true, scriber.eventTypesWorkSpace);
        documentGrammar.init(scriber.currentState);

        EventTypeList eventTypes;
        EventType eventType;

        eventTypes = scriber.NextEventTypes;

        eventType = eventTypes.item(0);
        Assert.AreEqual(EventType.ITEM_SD, eventType.itemType);
        Assert.AreEqual(1, eventTypes.Length);
        scriber.writeEventType(eventType);
        scriber.startDocument();
        eventTypes = scriber.NextEventTypes;
        Assert.AreEqual(2, eventTypes.Length); // SE(*), ED

        eventType = eventTypes.item(0);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        scriber.writeEventType(eventType);
        qname.setValue("urn:goo", "A", null);
        scriber.writeQName(qname, eventType);
        int gooId = stringTable.getCompactIdOfURI("urn:goo");
        scriber.startWildcardElement(eventType.Index, gooId, qname.localNameId);
        eventTypes = scriber.NextEventTypes;
        Assert.AreEqual(4, eventTypes.Length); // EE, AT(*), SE(*), CH

        eventType = eventTypes.item(3);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        scriber.writeEventType(eventType);
        stringValueScriber.scribe("true", scribble, qname.localNameId, qname.uriId, EXISchema.NIL_NODE, scriber);
        scriber.undeclaredCharacters(eventType.Index);
        eventTypes = scriber.NextEventTypes;
        Assert.AreEqual(3, eventTypes.Length); // EE, SE(*), CH

        eventType = eventTypes.item(0);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        scriber.writeEventType(eventType);
        scriber.endElement();
        eventTypes = scriber.NextEventTypes;
        Assert.AreEqual(3, eventTypes.Length); // SE(goo:A), SE(*), ED

        eventType = eventTypes.item(0);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        scriber.writeEventType(eventType);
        scriber.writeQName(qname.setValue("urn:goo", "A", null), eventType);
        scriber.startElement(eventType);
        eventTypes = scriber.NextEventTypes;
        Assert.AreEqual(5, eventTypes.Length);

        eventType = eventTypes.item(0);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        scriber.writeEventType(eventType);
        stringValueScriber.scribe("false", scribble, qname.localNameId, qname.uriId, EXISchema.NIL_NODE, scriber);
        scriber.undeclaredCharacters(eventType.Index);
        eventTypes = scriber.NextEventTypes;
        Assert.AreEqual(3, eventTypes.Length); // EE, SE(*), CH

        eventType = eventTypes.item(0);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        scriber.writeEventType(eventType);
        scriber.endElement();
        eventTypes = scriber.NextEventTypes;
        Assert.AreEqual(3, eventTypes.Length); // SE(goo:A), SE(*), ED

        eventType = eventTypes.item(2);
        Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
        scriber.writeEventType(eventType);
        scriber.endDocument();
        eventTypes = scriber.NextEventTypes;
        Assert.AreEqual(0, eventTypes.Length);

        scriber.finish();


        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        decoder.AlignmentType = alignment;
        decoder.Fragment = true;

        decoder.GrammarCache = grammarCache;
        decoder.InputStream = new MemoryStream(baos.ToArray());
        scanner = decoder.processHeader();

        EventDescription exiEvent;
        EventTypeList eventTypeList;
        GrammarState grammarState;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.BUILTIN_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("A", exiEvent.Name);
        Assert.AreEqual("urn:goo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.BUILTIN_GRAMMAR_ELEMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(BuiltinGrammar.ELEMENT_STATE_IN_TAG, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("true", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        Assert.AreEqual(4, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length); // CH, EE, AT(*), SE(*), CH
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.BUILTIN_GRAMMAR_ELEMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(BuiltinGrammar.ELEMENT_STATE_IN_CONTENT, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.Length); // EE, SE(*), CH
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.BUILTIN_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("A", exiEvent.Name);
        Assert.AreEqual("urn:goo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.Length); // SE(goo:A), SE(*), ED
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.BUILTIN_GRAMMAR_ELEMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(BuiltinGrammar.ELEMENT_STATE_IN_TAG, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("false", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length); // CH, EE, AT(*), SE(*), CH
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.BUILTIN_GRAMMAR_ELEMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(BuiltinGrammar.ELEMENT_STATE_IN_CONTENT, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.Length); // EE, SE(*), CH
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.BUILTIN_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.Length); // SE(goo:A), SE(*), EE
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.BUILTIN_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(Grammar.DOCUMENT_STATE_END, grammarState.phase);
        }

        Assert.IsNull(scanner.nextEvent());
      }
    }

    /// <summary>
    /// Test Element Fragment Grammar in strict schema mode.
    /// Invoke chars() at ELEMENT_FRAGMENT_STATE_TAG to transition to ELEMENT_FRAGMENT_STATE_CONTENT.
    /// </summary>
    [Test]
    public virtual void testElementFragmentStrict_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/fragment_01.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string xmlString;
      byte[] bts;

      xmlString = "<Z xmlns='urn:foo'>xyz</Z>";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        encoder.AlignmentType = alignment;
        encoder.Fragment = true;
        decoder.AlignmentType = alignment;
        decoder.Fragment = true;

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
        GrammarState grammarState;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(5, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(9, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A__", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_TAG, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("xyz", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        Assert.AreEqual(15, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(16, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("a", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("b", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("c", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A__", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(10);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(11);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(12);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(13);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(14);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_CONTENT, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(8, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(10, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A__", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(8, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(9, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A__", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(SchemaInformedGrammar.DOCUMENT_STATE_END, grammarState.phase);
        }

        Assert.IsNull(scanner.nextEvent());
      }
    }

    /// <summary>
    /// Test Element Fragment Grammar in default schema mode.
    /// Invoke chars() at ELEMENT_FRAGMENT_STATE_TAG to transition to ELEMENT_FRAGMENT_STATE_CONTENT.
    /// </summary>
    [Test]
    public virtual void testElementFragmentDefault_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/fragment_01.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      string xmlString;
      byte[] bts;

      xmlString = "<Z xmlns='urn:foo'>xyz</Z>";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        encoder.AlignmentType = alignment;
        encoder.Fragment = true;
        decoder.AlignmentType = alignment;
        decoder.Fragment = true;

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
        GrammarState grammarState;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(5, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(9, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A__", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_TAG, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("xyz", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        Assert.AreEqual(15, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(23, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("a", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("b", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("c", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A__", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(10);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(11);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(12);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(13);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(14);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(16);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(17);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("a", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(18);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("b", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(19);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("c", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(20);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        eventType = eventTypeList.item(21);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(22);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_CONTENT, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(8, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(12, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A__", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        eventType = eventTypeList.item(10);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(11);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(8, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(9, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A__", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(SchemaInformedGrammar.DOCUMENT_STATE_END, grammarState.phase);
        }

        Assert.IsNull(scanner.nextEvent());
      }
    }

    /// <summary>
    /// Test Element Fragment Grammar in default schema mode.
    /// Invoke chars() at ELEMENT_FRAGMENT_STATE_CONTENT which should incur no state transition.
    /// </summary>
    [Test]
    public virtual void testElementFragmentDefault_02() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/fragment_01.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addCM(GrammarOptions.DEFAULT_OPTIONS));

      string xmlString;
      byte[] bts;

      xmlString = "<Z xmlns='urn:foo'><!-- Hello! -->xyz</Z>";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        encoder.AlignmentType = alignment;
        encoder.Fragment = true;
        decoder.AlignmentType = alignment;
        decoder.Fragment = true;

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
        GrammarState grammarState;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(5, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(10, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A__", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_TAG, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CM, exiEvent.EventKind);
        Assert.AreEqual(" Hello! ", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        Assert.AreEqual(23, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(24, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("a", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("b", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("c", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A__", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(10);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(11);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(12);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(13);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(14);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(15);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        eventType = eventTypeList.item(16);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(17);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("a", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(18);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("b", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(19);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("c", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(20);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        eventType = eventTypeList.item(21);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(22);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_CONTENT, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("xyz", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        Assert.AreEqual(9, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(13, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A__", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(10);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(11);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        eventType = eventTypeList.item(12);
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_CONTENT, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(8, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(13, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A__", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        eventType = eventTypeList.item(10);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(11);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        eventType = eventTypeList.item(12);
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(8, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(10, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A__", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(SchemaInformedGrammar.DOCUMENT_STATE_END, grammarState.phase);
        }

        Assert.IsNull(scanner.nextEvent());
      }
    }

    /// <summary>
    /// Test Element Fragment Grammar in strict schema mode.
    /// Invoking schemaAttribute() at ELEMENT_FRAGMENT_STATE_TAG involves no state transition.
    /// </summary>
    [Test]
    public virtual void testElementFragmentStrict_02() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/fragment_01.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string xmlString;
      byte[] bts;

      xmlString = "<Z xmlns='urn:foo' xmlns:goo='urn:goo' goo:c='true'>xyz</Z>";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        encoder.AlignmentType = alignment;
        encoder.Fragment = true;
        decoder.AlignmentType = alignment;
        decoder.Fragment = true;

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
        GrammarState grammarState;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(5, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(9, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A__", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_TAG, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
        Assert.AreEqual("true", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("c", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        int tp = ((EventTypeSchema)eventType).nd;
        Assert.AreEqual(EXISchemaConst.BOOLEAN_TYPE, corpus.getSerialOfType(tp));
        Assert.AreEqual(4, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(16, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("a", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("b", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A__", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(10);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(11);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(12);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(13);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(14);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(15);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_TAG, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("xyz", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        Assert.AreEqual(15, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(16, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("a", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("b", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("c", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A__", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(10);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(11);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(12);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(13);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(14);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_CONTENT, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(8, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(10, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A__", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(8, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(9, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A__", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(SchemaInformedGrammar.DOCUMENT_STATE_END, grammarState.phase);
        }

        Assert.IsNull(scanner.nextEvent());
      }
    }

    /// <summary>
    /// Test Element Fragment Grammar in strict schema mode.
    /// Invoke element() at ELEMENT_FRAGMENT_STATE_TAG to transition to ELEMENT_FRAGMENT_STATE_CONTENT
    /// where the nested element itself is an element fragment.
    /// </summary>
    [Test]
    public virtual void testElementFragmentStrict_03() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/fragment_01.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string xmlString;
      byte[] bts;

      xmlString = "<Z xmlns='urn:foo'><Z>xyz</Z></Z>";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        encoder.AlignmentType = alignment;
        encoder.Fragment = true;
        decoder.AlignmentType = alignment;
        decoder.Fragment = true;

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
        GrammarState grammarState;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(5, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(9, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A__", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_TAG, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(11, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(16, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("a", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("b", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("c", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A__", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(10);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(12);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(13);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(14);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(15);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_TAG, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("xyz", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        Assert.AreEqual(15, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(16, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("a", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("b", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("c", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A__", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(10);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(11);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(12);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(13);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(14);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_CONTENT, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(8, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(10, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A__", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_CONTENT, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(8, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(10, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A__", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(8, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(9, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A__", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(SchemaInformedGrammar.DOCUMENT_STATE_END, grammarState.phase);
        }

        Assert.IsNull(scanner.nextEvent());
      }
    }

    /// <summary>
    /// Test Element Fragment Grammar in strict schema mode.
    /// Invoke nillify() at ELEMENT_FRAGMENT_STATE_TAG to transition to ELEMENT_FRAGMENT_EMPTY_STATE_TAG.
    /// </summary>
    [Test]
    public virtual void testElementFragmentStrict_04() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/fragment_01.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string xmlString;
      byte[] bts;

      xmlString = "<Z xmlns='urn:foo' xmlns:goo='urn:goo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:nil='true' goo:a='_3.1415926_'/>";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        encoder.AlignmentType = alignment;
        encoder.Fragment = true;
        decoder.AlignmentType = alignment;
        decoder.Fragment = true;

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
        GrammarState grammarState;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(5, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(9, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A__", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_TAG, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_NL, exiEvent.EventKind);
        Assert.IsTrue(((EXIEventSchemaNil)exiEvent).Nilled);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(16, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("a", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("b", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("c", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A__", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(10);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(11);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(12);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(13);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(14);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(15);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(SchemaInformedGrammar.ELEMENT_FRAGMENT_EMPTY_STATE_TAG, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
        Assert.AreEqual("_3.1415926_", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("a", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        Assert.AreEqual(EXISchema.NIL_NODE, ((EventTypeSchema)eventType).nd);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("b", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("c", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(SchemaInformedGrammar.ELEMENT_FRAGMENT_EMPTY_STATE_TAG, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(4, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("a", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("b", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("c", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(8, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(9, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A__", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(SchemaInformedGrammar.DOCUMENT_STATE_END, grammarState.phase);
        }

        Assert.IsNull(scanner.nextEvent());
      }
    }

    /// <summary>
    /// Test Element Fragment Grammar in default schema mode.
    /// Invoke nillify() at ELEMENT_FRAGMENT_STATE_TAG to transition to ELEMENT_FRAGMENT_EMPTY_STATE_TAG.
    /// </summary>
    [Test]
    public virtual void testElementFragmentDefault_04() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/fragment_01.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      string xmlString;
      byte[] bts;

      xmlString = "<Z xmlns='urn:foo' xmlns:goo='urn:goo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:nil='true' goo:a='_3.1415926_'>xyz</Z>";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        encoder.AlignmentType = alignment;
        encoder.Fragment = true;
        decoder.AlignmentType = alignment;
        decoder.Fragment = true;

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
        GrammarState grammarState;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(5, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(9, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A__", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_TAG, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_NL, exiEvent.EventKind);
        Assert.IsTrue(((EXIEventSchemaNil)exiEvent).Nilled);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(23, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("a", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("b", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("c", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A__", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(10);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(11);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(12);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(13);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(14);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(15);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        eventType = eventTypeList.item(16);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(17);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("a", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(18);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("b", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(19);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("c", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(20);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        eventType = eventTypeList.item(21);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(22);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(SchemaInformedGrammar.ELEMENT_FRAGMENT_EMPTY_STATE_TAG, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
        Assert.AreEqual("_3.1415926_", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("a", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        Assert.AreEqual(EXISchema.NIL_NODE, ((EventTypeSchema)eventType).nd);
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(14, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("b", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("c", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("a", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("b", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(10);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("c", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(11);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        eventType = eventTypeList.item(12);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(13);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(SchemaInformedGrammar.ELEMENT_FRAGMENT_EMPTY_STATE_TAG, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("xyz", exiEvent.Characters.makeString());
        Assert.AreEqual(13, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(14, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("a", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("b", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("c", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("a", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("b", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(10);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("c", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(11);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        eventType = eventTypeList.item(12);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(SchemaInformedGrammar.ELEMENT_FRAGMENT_EMPTY_STATE_CONTENT, grammarState.phase);
        }

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
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(8, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(9, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A__", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(SchemaInformedGrammar.DOCUMENT_STATE_END, grammarState.phase);
        }

        Assert.IsNull(scanner.nextEvent());
      }
    }

    /// <summary>
    /// Test Element Fragment Grammar in strict schema mode.
    /// Invoke xsitp() at ELEMENT_FRAGMENT_STATE_TAG to transition to ELEMENT_STATE_TAG.
    /// </summary>
    [Test]
    public virtual void testElementFragmentStrict_05() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/fragment_01.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string xmlString;
      byte[] bts;

      xmlString = "<foo:Z xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:type='foo:ATYPE'><foo:Z>12345</foo:Z></foo:Z>";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        encoder.AlignmentType = alignment;
        encoder.Fragment = true;
        decoder.AlignmentType = alignment;
        decoder.Fragment = true;

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
        GrammarState grammarState;
        int nd;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(5, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(9, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A__", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_TAG, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
        nd = ((EXIEventSchemaType)exiEvent).Tp;
        Assert.AreEqual("ATYPE", corpus.getNameOfType(nd));
        Assert.AreEqual("urn:foo", EXISchemaUtil.getTargetNamespaceNameOfType(nd, corpus));
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(16, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("a", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("b", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("c", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A__", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(10);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(11);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(12);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(13);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(14);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(15);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
       /// <summary>
       /// This assertion is not relevant in Nagasena
       /// if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
       ///   grammarState = scanner.getGrammarState();
       ///   Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_ELEMENT_TAG, grammarState.targetGrammar.grammarType);
       ///   Assert.AreEqual(SchemaInformedGrammar.ELEMENT_STATE_TAG, grammarState.phase);
       /// }
       /// </summary>

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(4, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("b", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
       /// <summary>
       /// This assertion is not relevant in Nagasena
       /// if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
       ///   grammarState = scanner.getGrammarState();
       ///   Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_ELEMENT, grammarState.targetGrammar.grammarType);
       ///   Assert.AreEqual(SchemaInformedGrammar.ELEMENT_STATE_UNBOUND, grammarState.phase);
       /// }
       /// </summary>

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("12345", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          nd = scanner.currentState.contentDatatype;
          Assert.AreEqual(EXISchemaConst.INT_TYPE, corpus.getSerialOfType(nd));
        }
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(2, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
       /// <summary>
       /// This assertion is not relevant in Nagasena
       /// if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
       ///   grammarState = scanner.getGrammarState();
       ///   Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_ELEMENT_CONTENT, grammarState.targetGrammar.grammarType);
       ///   Assert.AreEqual(SchemaInformedGrammar.ELEMENT_STATE_CONTENT_COMPLETE, grammarState.phase);
       /// }
       /// </summary>

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
       /// <summary>
       /// This assertion is not relevant in Nagasena
       /// if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
       ///   grammarState = scanner.getGrammarState();
       ///   Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_ELEMENT_CONTENT, grammarState.targetGrammar.grammarType);
       ///   Assert.AreEqual(SchemaInformedGrammar.ELEMENT_STATE_CONTENT_COMPLETE, grammarState.phase);
       /// }
       /// </summary>

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(8, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(9, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A__", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(SchemaInformedGrammar.DOCUMENT_STATE_END, grammarState.phase);
        }

        Assert.IsNull(scanner.nextEvent());
      }
    }

    /// <summary>
    /// Test Element Fragment Grammar in strict schema mode.
    /// Invoke element() at ELEMENT_FRAGMENT_STATE_TAG to transition to ELEMENT_FRAGMENT_STATE_CONTENT
    /// where the nested element is *not* an element fragment.
    /// </summary>
    [Test]
    public virtual void testElementFragmentStrict_06() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/fragment_01.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string xmlString;
      byte[] bts;

      xmlString = "<foo:Z xmlns:foo='urn:foo' xmlns:goo='urn:goo'><goo:A>true</goo:A></foo:Z>";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        encoder.AlignmentType = alignment;
        encoder.Fragment = true;
        decoder.AlignmentType = alignment;
        decoder.Fragment = true;

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
        GrammarState grammarState;
        int nd;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(5, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(9, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A__", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_TAG, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        Assert.AreEqual(6, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(16, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("a", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("b", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("c", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A__", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(10);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(11);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(12);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(13);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(14);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(15);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
       /// <summary>
       /// This assertion is not relevant in Nagasena
       /// if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
       ///   grammarState = scanner.getGrammarState();
       ///   Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_ELEMENT, grammarState.targetGrammar.grammarType);
       ///   Assert.AreEqual(SchemaInformedGrammar.ELEMENT_STATE_UNBOUND, grammarState.phase);
       /// }
       /// </summary>

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("true", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          nd = scanner.currentState.contentDatatype;
          Assert.AreEqual(EXISchemaConst.BOOLEAN_TYPE, corpus.getSerialOfType(nd));
        }
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
       /// <summary>
       /// This assertion is not relevant in Nagasena
       /// if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
       ///   grammarState = scanner.getGrammarState();
       ///   Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_ELEMENT_CONTENT, grammarState.targetGrammar.grammarType);
       ///   Assert.AreEqual(SchemaInformedGrammar.ELEMENT_STATE_CONTENT_COMPLETE, grammarState.phase);
       /// }
       /// </summary>

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(SchemaInformedGrammar.ELEMENT_FRAGMENT_STATE_CONTENT, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(8, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(10, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A__", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(Grammar.DOCUMENT_STATE_COMPLETED, grammarState.phase);
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(8, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(9, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A_", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A__", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          grammarState = scanner.GrammarState;
          Assert.AreEqual(Grammar.SCHEMA_GRAMMAR_FRAGMENT, grammarState.targetGrammar.grammarType);
          Assert.AreEqual(SchemaInformedGrammar.DOCUMENT_STATE_END, grammarState.phase);
        }

        Assert.IsNull(scanner.nextEvent());
      }
    }

    /// <summary>
    /// There are 3 declarations of the element name "foo:A" all of which
    /// use the same type. The same is true for the element name "foo:B".
    /// 
    /// <foo:A xmlns:foo="urn:foo">
    ///   <foo:A>
    ///     <foo:A />
    ///     <foo:B />
    ///   </foo:A>
    ///   <foo:B>
    ///     <foo:B />
    ///     <foo:A />
    ///   </foo:B>
    /// </foo:A>
    /// <foo:B xmlns:foo="urn:foo">
    ///   <foo:B>
    ///     <foo:B />
    ///     <foo:A />
    ///   </foo:B>
    ///   <foo:A>
    ///     <foo:A />
    ///     <foo:B />
    ///   </foo:A>
    /// </foo:B>
    /// </summary>
    [Test]
    public virtual void testDecodeFragment_03() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/interop/schemaInformedGrammar/declaredProductions/fragment-b.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addPI(GrammarOptions.DEFAULT_OPTIONS));

      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;

      decoder.AlignmentType = AlignmentType.byteAligned;

      Uri url = resolveSystemIdAsURL("/interop/schemaInformedGrammar/declaredProductions/fragment-03.byteAligned");
      FileStream inputStream = new FileStream(url.LocalPath, FileMode.Open);

      decoder.GrammarCache = grammarCache;
      decoder.Fragment = true;
      decoder.InputStream = inputStream;
      scanner = decoder.processHeader();

      EventDescription exiEvent;
      EventType eventType;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      Assert.AreEqual("A", eventType.name);
      Assert.AreEqual("urn:foo", eventType.uri);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      Assert.AreEqual("A", eventType.name);
      Assert.AreEqual("urn:foo", eventType.uri);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      Assert.AreEqual("A", eventType.name);
      Assert.AreEqual("urn:foo", eventType.uri);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      Assert.AreEqual("B", eventType.name);
      Assert.AreEqual("urn:foo", eventType.uri);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      Assert.AreEqual("B", eventType.name);
      Assert.AreEqual("urn:foo", eventType.uri);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      Assert.AreEqual("B", eventType.name);
      Assert.AreEqual("urn:foo", eventType.uri);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      Assert.AreEqual("A", eventType.name);
      Assert.AreEqual("urn:foo", eventType.uri);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      Assert.AreEqual("B", eventType.name);
      Assert.AreEqual("urn:foo", eventType.uri);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      Assert.AreEqual("B", eventType.name);
      Assert.AreEqual("urn:foo", eventType.uri);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      Assert.AreEqual("B", eventType.name);
      Assert.AreEqual("urn:foo", eventType.uri);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      Assert.AreEqual("A", eventType.name);
      Assert.AreEqual("urn:foo", eventType.uri);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      Assert.AreEqual("A", eventType.name);
      Assert.AreEqual("urn:foo", eventType.uri);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      Assert.AreEqual("A", eventType.name);
      Assert.AreEqual("urn:foo", eventType.uri);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      Assert.AreEqual("B", eventType.name);
      Assert.AreEqual("urn:foo", eventType.uri);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);

      Assert.IsNull(scanner.nextEvent());

      inputStream.Close();
    }

    /// <summary>
    /// There are 5 declarations of the element name "foo:A" the types of
    /// which vary. The same is true for the element name "foo:B".
    /// 
    /// <foo:A xmlns:foo="urn:foo">
    ///   <foo:A>
    ///     <foo:A />
    ///     <foo:B />
    ///   </foo:A>
    ///   <foo:B>
    ///     <foo:B />
    ///     <foo:A />
    ///   </foo:B>
    /// </foo:A>
    /// <foo:B xmlns:foo="urn:foo">
    ///   <foo:B>
    ///     <foo:B />
    ///     <foo:A />
    ///   </foo:B>
    ///   <foo:A>
    ///     <foo:A />
    ///     <foo:B />
    ///   </foo:A>
    /// </foo:B>
    /// </summary>
    [Test]
    public virtual void testDecodeFragment_04() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/interop/schemaInformedGrammar/declaredProductions/fragment-c.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addPI(GrammarOptions.DEFAULT_OPTIONS));

      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;

      decoder.AlignmentType = AlignmentType.byteAligned;

      Uri url = resolveSystemIdAsURL("/interop/schemaInformedGrammar/declaredProductions/fragment-04.byteAligned");
      FileStream inputStream = new FileStream(url.LocalPath, FileMode.Open);

      decoder.GrammarCache = grammarCache;
      decoder.Fragment = true;
      decoder.InputStream = inputStream;
      scanner = decoder.processHeader();

      EventDescription exiEvent;
      EventType eventType;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      Assert.AreEqual("A", eventType.name);
      Assert.AreEqual("urn:foo", eventType.uri);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      Assert.AreEqual("A", eventType.name);
      Assert.AreEqual("urn:foo", eventType.uri);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      Assert.AreEqual("A", eventType.name);
      Assert.AreEqual("urn:foo", eventType.uri);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      Assert.AreEqual("B", eventType.name);
      Assert.AreEqual("urn:foo", eventType.uri);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      Assert.AreEqual("B", eventType.name);
      Assert.AreEqual("urn:foo", eventType.uri);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      Assert.AreEqual("B", eventType.name);
      Assert.AreEqual("urn:foo", eventType.uri);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      Assert.AreEqual("A", eventType.name);
      Assert.AreEqual("urn:foo", eventType.uri);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      Assert.AreEqual("B", eventType.name);
      Assert.AreEqual("urn:foo", eventType.uri);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      Assert.AreEqual("B", eventType.name);
      Assert.AreEqual("urn:foo", eventType.uri);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      Assert.AreEqual("B", eventType.name);
      Assert.AreEqual("urn:foo", eventType.uri);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      Assert.AreEqual("A", eventType.name);
      Assert.AreEqual("urn:foo", eventType.uri);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      Assert.AreEqual("A", eventType.name);
      Assert.AreEqual("urn:foo", eventType.uri);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      Assert.AreEqual("A", eventType.name);
      Assert.AreEqual("urn:foo", eventType.uri);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      Assert.AreEqual("B", eventType.name);
      Assert.AreEqual("urn:foo", eventType.uri);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);

      Assert.IsNull(scanner.nextEvent());

      inputStream.Close();
    }

    /// <summary>
    /// <foo:C xmlns:foo="urn:foo" ><None/></foo:C>
    /// 
    /// Start-tag of the element "None" should cause the grammar of C to move to
    /// the a state where no attributes are allowed.
    /// </summary>
    [Test]
    public virtual void testDecodeElementFragment_02() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/interop/schemaInformedGrammar/declaredProductions/elementFragment.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;

      decoder.AlignmentType = AlignmentType.byteAligned;

      Uri url = resolveSystemIdAsURL("/interop/schemaInformedGrammar/declaredProductions/elementFragment-02b.byteAligned");
      FileStream inputStream = new FileStream(url.LocalPath, FileMode.Open);

      decoder.GrammarCache = grammarCache;
      decoder.Fragment = true;
      decoder.InputStream = inputStream;
      scanner = decoder.processHeader();

      EventDescription exiEvent;
      EventType eventType;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      Assert.AreEqual("C", eventType.name);
      Assert.AreEqual("urn:foo", eventType.uri);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      Assert.AreEqual("None", exiEvent.Name);
      Assert.AreEqual("", exiEvent.URI);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);

      Assert.IsNull(scanner.nextEvent());

      inputStream.Close();
    }

    /// <summary>
    /// <foo:C xmlns:foo="urn:foo" xmlns:goo="urn:goo" xmlns:aoo="urn:aoo" 
    ///   xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' 
    ///   none="Welcome to the world of element fragment grammar!"
    ///   xsi:nil="true"
    ///   aoo:b="2010-04-28"
    ///   foo:d="12:34:58" />
    /// 
    /// The content of the root element (i.e. <foo:C>) is evaluated using element
    /// fragment grammar.  
    /// </summary>
    [Test]
    public virtual void testDecodeElementFragment_03() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/interop/schemaInformedGrammar/declaredProductions/elementFragment.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;

      decoder.AlignmentType = AlignmentType.bitPacked;

      Uri url = resolveSystemIdAsURL("/interop/schemaInformedGrammar/declaredProductions/elementFragment-03.bitPacked");
      FileStream inputStream = new FileStream(url.LocalPath, FileMode.Open);

      decoder.GrammarCache = grammarCache;
      decoder.Fragment = true;
      decoder.InputStream = inputStream;
      scanner = decoder.processHeader();

      EventDescription exiEvent;
      EventType eventType;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      Assert.AreEqual("C", eventType.name);
      Assert.AreEqual("urn:foo", eventType.uri);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_NL, exiEvent.EventKind);
      Assert.IsTrue(((EXIEventSchemaNil)exiEvent).Nilled);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
      Assert.AreEqual("2010-04-28", exiEvent.Characters.makeString());
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.AreEqual("b", eventType.name);
      Assert.AreEqual("urn:aoo", eventType.uri);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
      Assert.AreEqual("12:34:58", exiEvent.Characters.makeString());
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.AreEqual("d", eventType.name);
      Assert.AreEqual("urn:foo", eventType.uri);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
      Assert.AreEqual("", exiEvent.URI);
      Assert.AreEqual("none", exiEvent.Name);
      Assert.AreEqual("Welcome to the world of element fragment grammar!", exiEvent.Characters.makeString());
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);

      Assert.IsNull(scanner.nextEvent());

      inputStream.Close();
    }

    /// <summary>
    /// <foo:C xmlns:foo="urn:foo" xmlns:goo="urn:goo" xmlns:aoo="urn:aoo" 
    ///   xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' 
    ///   none="Welcome to the world of element fragment grammar!"
    ///   xsi:type="foo:tC" foo:c="2010-08-18">
    ///   <foo:C />
    ///   <foo:D />
    /// </foo:C>
    /// 
    /// The content of the root element (i.e. <foo:C>) initially is being evaluated 
    /// using element fragment grammar, however, the bulk of it is processed by
    /// a specific type grammar corresponding to "foo:tC" upon xsi:type="foo:tC". 
    /// </summary>
    [Test]
    public virtual void testDecodeElementFragment_04() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/interop/schemaInformedGrammar/declaredProductions/elementFragment.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;

      decoder.AlignmentType = AlignmentType.byteAligned;

      Uri url = resolveSystemIdAsURL("/interop/schemaInformedGrammar/declaredProductions/elementFragment-04.byteAligned");
      FileStream inputStream = new FileStream(url.LocalPath, FileMode.Open);

      decoder.GrammarCache = grammarCache;
      decoder.Fragment = true;
      decoder.InputStream = inputStream;
      scanner = decoder.processHeader();

      EventDescription exiEvent;
      EventType eventType;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      Assert.AreEqual("C", eventType.name);
      Assert.AreEqual("urn:foo", eventType.uri);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
      int nd = ((EXIEventSchemaType)exiEvent).Tp;
      Assert.AreEqual("tC", corpus.getNameOfType(nd));
      Assert.AreEqual("urn:foo", EXISchemaUtil.getTargetNamespaceNameOfType(nd, corpus));
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
      Assert.AreEqual("2010-08-18", exiEvent.Characters.makeString());
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      Assert.AreEqual("c", eventType.name);
      Assert.AreEqual("urn:foo", eventType.uri);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
      Assert.AreEqual("", exiEvent.URI);
      Assert.AreEqual("none", exiEvent.Name);
      Assert.AreEqual("Welcome to the world of element fragment grammar!", exiEvent.Characters.makeString());
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      Assert.AreEqual("C", eventType.name);
      Assert.AreEqual("urn:foo", eventType.uri);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      Assert.AreEqual("D", eventType.name);
      Assert.AreEqual("urn:foo", eventType.uri);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);

      Assert.IsNull(scanner.nextEvent());

      inputStream.Close();
    }

  }

}