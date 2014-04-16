using System;
using System.IO;
using NUnit.Framework;

using AlignmentType = Nagasena.Proc.Common.AlignmentType;
using EventDescription = Nagasena.Proc.Common.EventDescription;
using EventDescription_Fields = Nagasena.Proc.Common.EventDescription_Fields;
using EventType = Nagasena.Proc.Common.EventType;
using EventTypeList = Nagasena.Proc.Common.EventTypeList;
using GrammarOptions = Nagasena.Proc.Common.GrammarOptions;
using QName = Nagasena.Proc.Common.QName;
using StringTable = Nagasena.Proc.Common.StringTable;
using EXIEventNS = Nagasena.Proc.Events.EXIEventNS;
using EXIEventSchemaType = Nagasena.Proc.Events.EXIEventSchemaType;
using Grammar = Nagasena.Proc.Grammars.Grammar;
using GrammarCache = Nagasena.Proc.Grammars.GrammarCache;
using Scanner = Nagasena.Proc.IO.Scanner;
using Scribble = Nagasena.Proc.IO.Scribble;
using Scriber = Nagasena.Proc.IO.Scriber;
using ScriberFactory = Nagasena.Proc.IO.ScriberFactory;
using ValueScriber = Nagasena.Proc.IO.ValueScriber;
using EXISchema = Nagasena.Schema.EXISchema;
using EXISchemaFactoryTestUtil = Nagasena.Scomp.EXISchemaFactoryTestUtil;

namespace Nagasena.Proc {

  [TestFixture]
  [Category("Enable_Compression")]
  public class XsiTypeTest {

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
    /// Typed representation of xsi:type. 
    /// </summary>
    [Test]
    public virtual void testXsiType_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/boolean.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      Scribble scribble = new Scribble();

      QName trueTypeQName = new QName("  foo:trueType   ", "urn:foo");
      Assert.AreEqual("trueType", trueTypeQName.localName);
      Assert.AreEqual("foo", trueTypeQName.prefix);
      Assert.AreEqual("  foo:trueType   ", trueTypeQName.qName);

      int trueType = corpus.getTypeOfSchema("urn:foo", trueTypeQName.localName);
      Assert.IsTrue(EXISchema.NIL_NODE != trueType);

      foreach (AlignmentType alignment in Alignments) {
        Scriber scriber = ScriberFactory.createScriber(alignment);
        scriber.setSchema(grammarCache.EXISchema, (QName[])null, 0);
        scriber.PreserveNS = GrammarOptions.hasNS(grammarCache.grammarOptions);
        StringTable stringTable = Scriber.createStringTable(new GrammarCache(grammarCache.EXISchema));
        scriber.StringTable = stringTable;

        ValueScriber booleanValueScriber = scriber.getValueScriber(trueType);

        MemoryStream baos = new MemoryStream();
        scriber.OutputStream = baos;

        Scriber.writeHeaderPreamble(baos, false, false);

        Grammar documentGrammar = grammarCache.retrieveRootGrammar(false, scriber.eventTypesWorkSpace);
        documentGrammar.init(scriber.currentState);

        EventTypeList eventTypes;
        EventType eventType;
        QName qname = new QName();

        int fooId = stringTable.getCompactIdOfURI("urn:foo");

        eventTypes = scriber.NextEventTypes;

        eventType = eventTypes.item(0);
        Assert.AreEqual(EventType.ITEM_SD, eventType.itemType);
        Assert.AreEqual(1, eventTypes.Length);
        scriber.writeEventType(eventType);
        scriber.startDocument();
        eventTypes = scriber.NextEventTypes;
        Assert.AreEqual(5, eventTypes.Length);

        eventType = eventTypes.item(0);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        scriber.writeEventType(eventType);
        scriber.writeQName(qname.setValue("urn:foo", "A", null), eventType);
        int localNameId_A = qname.localNameId;
        scriber.startElement(eventType);
        eventTypes = scriber.NextEventTypes;
        Assert.AreEqual(2, eventTypes.Length);

        eventType = eventTypes.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        scriber.writeEventType(eventType);
        scriber.writeXsiTypeValue(trueTypeQName);
        scriber.xsitp(trueType);
        eventTypes = scriber.NextEventTypes;
        Assert.AreEqual(1, eventTypes.Length);

        eventType = eventTypes.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        scriber.writeEventType(eventType);
        Assert.IsTrue(booleanValueScriber.process("true", trueType, corpus, scribble, scriber));
        booleanValueScriber.scribe("true", scribble, localNameId_A, fooId, trueType, scriber);
        scriber.characters(eventType);
        eventTypes = scriber.NextEventTypes;
        Assert.AreEqual(1, eventTypes.Length);

        eventType = eventTypes.item(0);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        scriber.writeEventType(eventType);
        scriber.endElement();
        eventTypes = scriber.NextEventTypes;
        Assert.AreEqual(1, eventTypes.Length);

        eventType = eventTypes.item(0);
        Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
        scriber.writeEventType(eventType);
        scriber.endDocument();
        eventTypes = scriber.NextEventTypes;
        Assert.AreEqual(0, eventTypes.Length);

        scriber.finish();


        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        decoder.AlignmentType = alignment;
        decoder.Fragment = false;

        decoder.GrammarCache = grammarCache;
        decoder.InputStream = new MemoryStream(baos.ToArray());
        scanner = decoder.processHeader();

        EventDescription exiEvent;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
        Assert.AreEqual("trueType", ((EXIEventSchemaType)exiEvent).TypeName);
        Assert.AreEqual("urn:foo", ((EXIEventSchemaType)exiEvent).TypeURI);

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("true", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          int tp = scanner.currentState.contentDatatype;
          Assert.AreEqual(trueType, tp);
        }
        Assert.AreEqual(0, eventType.Index);

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);

        Assert.IsNull(scanner.nextEvent());
      }
    }

    /// <summary>
    /// Literal representation of xsi:type with lexical preservation option on. 
    /// </summary>
    [Test]
    public virtual void testXsiTypePreserveLexicalValue_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/boolean.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

      Scribble scribble = new Scribble();

      QName trueTypeQName = new QName("  foo:trueType   ", "urn:foo");
      Assert.AreEqual("trueType", trueTypeQName.localName);
      Assert.AreEqual("foo", trueTypeQName.prefix);
      Assert.AreEqual("  foo:trueType   ", trueTypeQName.qName);

      int trueType = corpus.getTypeOfSchema("urn:foo", trueTypeQName.localName);
      Assert.IsTrue(EXISchema.NIL_NODE != trueType);

      foreach (AlignmentType alignment in Alignments) {
        Scriber scriber = ScriberFactory.createScriber(alignment);
        scriber.setSchema(grammarCache.EXISchema, (QName[])null, 0);
        scriber.PreserveNS = GrammarOptions.hasNS(grammarCache.grammarOptions);
        StringTable stringTable = Scriber.createStringTable(new GrammarCache(grammarCache.EXISchema));
        scriber.StringTable = stringTable;
        scriber.PreserveLexicalValues = true;

        ValueScriber booleanValueScriber = scriber.getValueScriber(trueType);

        MemoryStream baos = new MemoryStream();
        scriber.OutputStream = baos;

        Scriber.writeHeaderPreamble(baos, false, false);

        Grammar documentGrammar = grammarCache.retrieveRootGrammar(false, scriber.eventTypesWorkSpace);
        documentGrammar.init(scriber.currentState);

        EventTypeList eventTypes;
        EventType eventType;
        QName qname = new QName();

        int fooId = stringTable.getCompactIdOfURI("urn:foo");

        eventTypes = scriber.NextEventTypes;

        eventType = eventTypes.item(0);
        Assert.AreEqual(EventType.ITEM_SD, eventType.itemType);
        Assert.AreEqual(1, eventTypes.Length);
        scriber.writeEventType(eventType);
        scriber.startDocument();
        eventTypes = scriber.NextEventTypes;
        Assert.AreEqual(5, eventTypes.Length);

        eventType = eventTypes.item(0);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        scriber.writeEventType(eventType);
        scriber.writeQName(qname.setValue("urn:foo", "A", ""), eventType);
        int localNameId_A = qname.localNameId;
        scriber.startElement(eventType);
        eventTypes = scriber.NextEventTypes;

        eventType = eventTypes.NamespaceDeclaration;
        scriber.writeEventType(eventType);
        scriber.writeNS("urn:foo", "foo", false);

        eventType = eventTypes.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        scriber.writeEventType(eventType);
        scriber.writeXsiTypeValue(trueTypeQName);
        scriber.xsitp(trueType);
        eventTypes = scriber.NextEventTypes;
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypes.item(0).itemType);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventTypes.item(1).itemType);

        eventType = eventTypes.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        scriber.writeEventType(eventType);
        Assert.IsTrue(booleanValueScriber.process("true", trueType, corpus, scribble, scriber));
        booleanValueScriber.scribe("true", scribble, localNameId_A, fooId, trueType, scriber);
        scriber.characters(eventType);
        eventTypes = scriber.NextEventTypes;

        eventType = eventTypes.item(0);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        scriber.writeEventType(eventType);
        scriber.endElement();
        eventTypes = scriber.NextEventTypes;
        Assert.AreEqual(1, eventTypes.Length);

        eventType = eventTypes.item(0);
        Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
        scriber.writeEventType(eventType);
        scriber.endDocument();
        eventTypes = scriber.NextEventTypes;
        Assert.AreEqual(0, eventTypes.Length);

        scriber.finish();


        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        decoder.AlignmentType = alignment;
        decoder.Fragment = false;
        decoder.PreserveLexicalValues = true;

        decoder.GrammarCache = grammarCache;
        decoder.InputStream = new MemoryStream(baos.ToArray());
        scanner = decoder.processHeader();

        EventDescription exiEvent;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
        Assert.AreEqual("foo", ((EXIEventNS)exiEvent).Prefix);
        Assert.AreEqual("urn:foo", ((EXIEventNS)exiEvent).URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
        Assert.AreEqual("trueType", ((EXIEventSchemaType)exiEvent).TypeName);
        Assert.AreEqual("foo", ((EXIEventSchemaType)exiEvent).TypePrefix);
        Assert.AreEqual("urn:foo", ((EXIEventSchemaType)exiEvent).TypeURI);
        Assert.AreEqual("  foo:trueType   ", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("true", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          int tp = scanner.currentState.contentDatatype;
          Assert.AreEqual(trueType, tp);
        }
        Assert.AreEqual(2, eventType.Index);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.EventTypeList.item(0).itemType);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.EventTypeList.item(1).itemType);

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);

        Assert.IsNull(scanner.nextEvent());
      }
    }

  }

}