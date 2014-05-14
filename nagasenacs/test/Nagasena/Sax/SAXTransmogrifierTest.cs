using System;
using System.IO;
using System.Text;
using System.Collections.Generic;
using NUnit.Framework;

using Org.System.Xml.Sax;
using LocatorImpl = Org.System.Xml.Sax.Helpers.LocatorImpl;
using AttributesImpl = Org.System.Xml.Sax.Helpers.AttributesImpl;
using SaxDriver = AElfred.SaxDriver;

using EXIDecoder = Nagasena.Proc.EXIDecoder;
using AlignmentType = Nagasena.Proc.Common.AlignmentType;
using BinaryDataSource = Nagasena.Proc.Common.BinaryDataSource;
using BinaryDataUtil = Nagasena.Proc.Common.BinaryDataUtil;
using EventDescription = Nagasena.Proc.Common.EventDescription;
using EventDescription_Fields = Nagasena.Proc.Common.EventDescription_Fields;
using EventCode = Nagasena.Proc.Common.EventCode;
using EventType = Nagasena.Proc.Common.EventType;
using EventTypeList = Nagasena.Proc.Common.EventTypeList;
using GrammarOptions = Nagasena.Proc.Common.GrammarOptions;
using EXIEventNS = Nagasena.Proc.Events.EXIEventNS;
using GrammarCache = Nagasena.Proc.Grammars.GrammarCache;
using BinaryDataSink = Nagasena.Proc.IO.BinaryDataSink;
using Scanner = Nagasena.Proc.IO.Scanner;
using ScriberRuntimeException = Nagasena.Proc.IO.ScriberRuntimeException;
using Base64 = Nagasena.Schema.Base64;
using EXISchema = Nagasena.Schema.EXISchema;
using TestBase = Nagasena.Schema.TestBase;
using EXISchemaFactoryTestUtil = Nagasena.Scomp.EXISchemaFactoryTestUtil;
using EXISchemaFactoryTestUtilContext = Nagasena.Scomp.EXISchemaFactoryTestUtilContext;

namespace Nagasena.Sax {

  [TestFixture]
  public class SAXTransmogrifierTest : TestBase {

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
    /// <xsd:element name="A">
    ///   <xsd:complexType>
    ///     <xsd:sequence>
    ///       <xsd:sequence>
    ///         <xsd:element ref="foo:AB"/>
    ///         <xsd:element ref="foo:AC" minOccurs="0" maxOccurs="2"/>
    ///       </xsd:sequence>
    ///       <xsd:element ref="foo:AD"/>
    ///       <xsd:element ref="foo:AE" minOccurs="0"/>
    ///     </xsd:sequence>
    ///   </xsd:complexType>
    /// </xsd:element>
    /// 
    /// Instance:
    /// <A>
    ///   <AB/><AC/><AC/><AD/><AE/>
    /// </A>
    /// </summary>
    [Test]
    public virtual void testStrict_01() {
      EXISchema corpus;
      try {
        corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.gram", this);
      }
      finally {
        //System.out.println(m_stringBuilder.toString());
      }
      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string xmlString;
      byte[] bts;
      int n_events;

      xmlString =
        "<A xmlns='urn:foo'>\n" +
        "  <AB/><AC/><AC/><AD/><AE/>\n" +
        "</A>\n";

      Transmogrifier encoder = new Transmogrifier();
      encoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        SAXTransmogrifier saxTransmogrifier;

        SaxDriver xmlReader = new SaxDriver();
        xmlReader.SetFeature(Constants.NamespacesFeature, true);
        saxTransmogrifier = encoder.SAXTransmogrifier;
        xmlReader.ContentHandler = saxTransmogrifier;

        Assert.AreSame(grammarCache, saxTransmogrifier.GrammarCache);

        xmlReader.Parse(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

        decoder.GrammarCache = grammarCache;
        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

        EventDescription exiEvent;
        n_events = 0;

        EventType eventType;
        EventTypeList eventTypeList;

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
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AB", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        Assert.AreEqual(1, eventType.Index); // because of xsi:type
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AC", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AC", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AC", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AC", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AD", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AD", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AE", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AE", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.EE.Index);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        ++n_events;

        Assert.AreEqual(19, n_events);
      }

      xmlString =
        "<A xmlns='urn:foo'>\n" +
        "  <AB/><AC/><AC/><AC/>\n" + // The 3rd <AC/> is not expected.
        "</A>\n";

      foreach (AlignmentType alignment in Alignments) {

        encoder.AlignmentType = alignment;

        bool caught;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        SaxDriver xmlReader = new SaxDriver();
        xmlReader.SetFeature(Constants.NamespacesFeature, true);
        xmlReader.ContentHandler = encoder.SAXTransmogrifier;

        caught = false;
        try {
          xmlReader.Parse(new InputSource<Stream>(string2Stream(xmlString)));
        }
        catch (SaxException se) {
          caught = true;
          TransmogrifierException te = (TransmogrifierException)se.InnerException;
          Assert.AreEqual(TransmogrifierException.UNEXPECTED_ELEM, te.Code);
        }
        Assert.IsTrue(caught);
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
    public virtual void testLexicalHandler_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.gram", this);

      short options = GrammarOptions.DEFAULT_OPTIONS;
      options = GrammarOptions.addCM(options);
      options = GrammarOptions.addPI(options);

      GrammarCache grammarCache = new GrammarCache(corpus, options);

      const string xmlString = 
        "<C xmlns='urn:foo'><AC/><!-- Good? --><?eg Good! ?></C><?eg Good? ?><!-- Good! -->";

      Transmogrifier encoder = new Transmogrifier();
      encoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        SaxDriver xmlReader = new SaxDriver();
        xmlReader.SetFeature(Constants.NamespacesFeature, true);
        xmlReader.ContentHandler = encoder.SAXTransmogrifier;
        xmlReader.LexicalHandler = encoder.SAXTransmogrifier;

        xmlReader.Parse(new InputSource<Stream>(string2Stream(xmlString)));

        byte[] bts = baos.ToArray();

        decoder.GrammarCache = grammarCache;
        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

        List<EventDescription> exiEventList = new List<EventDescription>();

        EventDescription exiEvent;
        int n_events = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          exiEventList.Add(exiEvent);
        }

        Assert.AreEqual(10, n_events);

        EventType eventType;
        EventTypeList eventTypeList;

        exiEvent = exiEventList[0];
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);

        exiEvent = exiEventList[1];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("C", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[2];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AC", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AC", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(11, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        eventType = eventTypeList.item(10);
        Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);

        exiEvent = exiEventList[3];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(10, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
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
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);

        exiEvent = exiEventList[4];
        Assert.AreEqual(EventDescription_Fields.EVENT_CM, exiEvent.EventKind);
        Assert.AreEqual(" Good? ", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        Assert.AreEqual(5, eventType.Index);
        // make sure this ITEM_CM belongs to AllGroupGrammar
        // This assertion is not relevant in Nagasena
        //Assert.AreEqual(EXISchema.GROUP_ALL, ((GroupGrammar)EventTypeAccessor.getGrammar(eventType)).getCompositor()); 
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(7, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AC", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);

        exiEvent = exiEventList[5];
        Assert.AreEqual(EventDescription_Fields.EVENT_PI, exiEvent.EventKind);
        Assert.AreEqual("eg", exiEvent.Name);
        Assert.AreEqual("Good! ", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
        Assert.AreEqual(6, eventType.Index);
        // make sure this ITEM_CM belongs to AllGroupGrammar
        // This assertion is not relevant in Nagasena
        //Assert.AreEqual(EXISchema.GROUP_ALL, ((GroupGrammar)EventTypeAccessor.getGrammar(eventType)).getCompositor()); 
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(7, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AC", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);

        exiEvent = exiEventList[6];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(7, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AC", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);

        exiEvent = exiEventList[7];
        Assert.AreEqual(EventDescription_Fields.EVENT_PI, exiEvent.EventKind);
        Assert.AreEqual("eg", exiEvent.Name);
        Assert.AreEqual("Good? ", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);

        exiEvent = exiEventList[8];
        Assert.AreEqual(EventDescription_Fields.EVENT_CM, exiEvent.EventKind);
        Assert.AreEqual(" Good! ", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);

        exiEvent = exiEventList[9];
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
      }
    }

    /// <summary>
    /// Tests BinaryDataHandler methods.
    /// </summary>
    [Test]
    public virtual void testBinaryData_01a() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/base64Binary.gram", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      const string textValue = "QUJDREVGR0hJSg==";

      byte[] octets = new byte[64];
      int n_octets = Base64.decode(textValue, octets);

      Transmogrifier encoder = new Transmogrifier();
      encoder.GrammarCache = grammarCache;

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveLexicalValues in new bool[] { true, false }) {
          foreach (bool binaryDataEnabled in new bool[] { true, false }) {
            decoder.EnableBinaryData = binaryDataEnabled;
            Scanner scanner;

            encoder.AlignmentType = alignment;
            decoder.AlignmentType = alignment;

            encoder.PreserveLexicalValues = preserveLexicalValues;
            decoder.PreserveLexicalValues = preserveLexicalValues;

            MemoryStream baos = new MemoryStream();
            encoder.OutputStream = baos;

            SAXTransmogrifier saxTransmogrifier = encoder.SAXTransmogrifier;
            BinaryDataSink binaryDataSink;

            saxTransmogrifier.SetDocumentLocator(new LocatorImpl());
            saxTransmogrifier.StartDocument();
            saxTransmogrifier.StartPrefixMapping("foo", "urn:foo");
            saxTransmogrifier.StartElement("urn:foo", "Base64Binary", "foo:Base64Binary", new AttributesImpl());
            try {
              binaryDataSink = saxTransmogrifier.startBinaryData(n_octets);
            }
            catch (SaxException se) {
              TransmogrifierException te = (TransmogrifierException)se.InnerException;
              Assert.AreEqual(TransmogrifierException.UNEXPECTED_BINARY_VALUE, te.Code);
              Assert.IsTrue(preserveLexicalValues);
              continue;
            }
            saxTransmogrifier.binaryData(octets, 0, n_octets, binaryDataSink);
            saxTransmogrifier.endBinaryData(binaryDataSink);
            Assert.IsFalse(preserveLexicalValues);
            saxTransmogrifier.EndElement("urn:foo", "Base64Binary", "foo:Base64Binary");
            saxTransmogrifier.EndPrefixMapping("foo");
            saxTransmogrifier.EndDocument();

            byte[] bts = baos.ToArray();
            decoder.InputStream = new MemoryStream(bts);

            scanner = decoder.processHeader();
            EventDescription @event;
            int n_events = 0;
            byte[] decodedBytes = null;
            string decodedText = null;
            while ((@event = scanner.nextEvent()) != null) {
              sbyte eventKind = @event.EventKind;
              if (EventDescription_Fields.EVENT_BLOB == eventKind) {
                Assert.IsTrue(binaryDataEnabled);
                decodedBytes = BinaryDataUtil.makeBytes(@event.BinaryDataSource);
              }
              else if (EventDescription_Fields.EVENT_CH == eventKind) {
                Assert.IsTrue(!binaryDataEnabled || preserveLexicalValues);
                decodedText = @event.Characters.makeString();
              }
              ++n_events;
            }
            Assert.AreEqual(5, n_events);
            if (binaryDataEnabled && !preserveLexicalValues) {
              Assert.AreEqual(n_octets, decodedBytes.Length);
              for (int i = 0; i < n_octets; i++) {
                Assert.AreEqual(octets[i], decodedBytes[i]);
              }
            }
            else {
              Assert.AreEqual(textValue, decodedText);
            }
          }
        }
      }
    }

    /// <summary>
    /// Call BinaryDataHandler's API sequence startBinaryData, binaryData and endBinaryData *twice*. 
    /// </summary>
    [Test]
    public virtual void testBinaryData_01b() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/base64Binary.gram", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      byte[] octets1, octets2;

      const string textValue1 = "QUJDREVGR0hJSg==";
      int n_octets1 = Base64.decode(textValue1, octets1 = new byte[64]);

      const string textValue2 = "aG9nZXBpeW9mb29iYXI=";
      int n_octets2 = Base64.decode(textValue2, octets2 = new byte[64]);

      Transmogrifier encoder = new Transmogrifier();
      encoder.GrammarCache = grammarCache;

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveLexicalValues in new bool[] { true, false }) {
          foreach (bool binaryDataEnabled in new bool[] { true, false }) {
            decoder.EnableBinaryData = binaryDataEnabled;
            Scanner scanner;

            encoder.AlignmentType = alignment;
            decoder.AlignmentType = alignment;

            encoder.PreserveLexicalValues = preserveLexicalValues;
            decoder.PreserveLexicalValues = preserveLexicalValues;

            MemoryStream baos = new MemoryStream();
            encoder.OutputStream = baos;

            SAXTransmogrifier saxTransmogrifier = encoder.SAXTransmogrifier;
            BinaryDataSink binaryDataSink;

            saxTransmogrifier.SetDocumentLocator(new LocatorImpl());
            saxTransmogrifier.StartDocument();
            saxTransmogrifier.StartPrefixMapping("foo", "urn:foo");
            saxTransmogrifier.StartElement("urn:foo", "A", "foo:A", new AttributesImpl());
            saxTransmogrifier.StartElement("urn:foo", "Base64Binary", "foo:Base64Binary", new AttributesImpl());
            try {
              binaryDataSink = saxTransmogrifier.startBinaryData(n_octets1);
            }
            catch (SaxException se) {
              TransmogrifierException te = (TransmogrifierException)se.InnerException;
              Assert.AreEqual(TransmogrifierException.UNEXPECTED_BINARY_VALUE, te.Code);
              Assert.IsTrue(preserveLexicalValues);
              continue;
            }
            saxTransmogrifier.binaryData(octets1, 0, n_octets1, binaryDataSink);
            saxTransmogrifier.endBinaryData(binaryDataSink);
            Assert.IsFalse(preserveLexicalValues);
            saxTransmogrifier.EndElement("urn:foo", "Base64Binary", "foo:Base64Binary");
            saxTransmogrifier.StartElement("urn:foo", "Base64Binary", "foo:Base64Binary", new AttributesImpl());
            binaryDataSink = saxTransmogrifier.startBinaryData(n_octets2);
            saxTransmogrifier.binaryData(octets2, 0, n_octets2, binaryDataSink);
            saxTransmogrifier.endBinaryData(binaryDataSink);
            saxTransmogrifier.EndElement("urn:foo", "Base64Binary", "foo:Base64Binary");
            saxTransmogrifier.EndElement("urn:foo", "A", "foo:A");
            saxTransmogrifier.EndPrefixMapping("foo");
            saxTransmogrifier.EndDocument();

            byte[] bts = baos.ToArray();
            decoder.InputStream = new MemoryStream(bts);

            scanner = decoder.processHeader();
            EventDescription @event;
            int n_events = 0;
            byte[] decodedBytes1, decodedBytes2;
            decodedBytes1 = decodedBytes2 = null;
            string decodedText1, decodedText2;
            decodedText1 = decodedText2 = null;
            while ((@event = scanner.nextEvent()) != null) {
              sbyte eventKind = @event.EventKind;
              if (EventDescription_Fields.EVENT_BLOB == eventKind) {
                Assert.IsTrue(binaryDataEnabled);
                if (decodedBytes1 == null) {
                  decodedBytes1 = BinaryDataUtil.makeBytes(@event.BinaryDataSource);
                }
                else {
                  decodedBytes2 = BinaryDataUtil.makeBytes(@event.BinaryDataSource);
                }
              }
              else if (EventDescription_Fields.EVENT_CH == eventKind) {
                Assert.IsTrue(!binaryDataEnabled || preserveLexicalValues);
                if (decodedText1 == null) {
                  decodedText1 = @event.Characters.makeString();
                }
                else {
                  decodedText2 = @event.Characters.makeString();
                }
              }
              ++n_events;
            }
            Assert.AreEqual(10, n_events);
            if (binaryDataEnabled && !preserveLexicalValues) {
              Assert.AreEqual(n_octets1, decodedBytes1.Length);
              for (int i = 0; i < n_octets1; i++) {
                Assert.AreEqual(octets1[i], decodedBytes1[i]);
              }
              Assert.AreEqual(n_octets2, decodedBytes2.Length);
              for (int i = 0; i < n_octets2; i++) {
                Assert.AreEqual(octets2[i], decodedBytes2[i]);
              }
            }
            else {
              Assert.AreEqual(textValue1, decodedText1);
              Assert.AreEqual(textValue2, decodedText2);
            }
          }
        }
      }
    }

    /// <summary>
    /// Decode binary values in chunks. 
    /// </summary>
    [Test]
    public virtual void testBinaryData_01c() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/base64Binary.gram", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      const string textValue =
        "RHIuIFN1ZSBDbGFyayBpcyBjdXJyZW50bHkgYSBSZWdlbnRzIFByb2Zlc3NvciBvZiBDaGVtaXN0\n" +
        "cnkgYXQgV2FzaGluZ3RvbiBTdGF0ZSBVbml2ZXJzaXR5IGluIFB1bGxtYW4sIFdBLCB3aGVyZSBz\n" +
        "aGUgaGFzIHRhdWdodCBhbmQgY29uZHVjdGVkIHJlc2VhcmNoIGluIGFjdGluaWRlIGVudmlyb25t\n" +
        "ZW50YWwgY2hlbWlzdHJ5IGFuZCByYWRpb2FuYWx5dGljYWwgY2hlbWlzdHJ5IHNpbmNlIDE5OTYu\n" +
        "ICBGcm9tIDE5OTIgdG8gMTk5NiwgRHIuIENsYXJrIHdhcyBhIFJlc2VhcmNoIEVjb2xvZ2lzdCBh\n" +
        "dCB0aGUgVW5pdmVyc2l0eSBvZiBHZW9yZ2lh4oCZcyBTYXZhbm5haCBSaXZlciBFY29sb2d5IExh\n" +
        "Ym9yYXRvcnkuICBQcmlvciB0byBoZXIgcG9zaXRpb24gYXQgdGhlIFVuaXZlcnNpdHkgb2YgR2Vv\n" +
        "cmdpYSwgc2hlIHdhcyBhIFNlbmlvciBTY2llbnRpc3QgYXQgdGhlIFdlc3Rpbmdob3VzZSBTYXZh\n" +
        "bm5haCBSaXZlciBDb21wYW554oCZcyBTYXZhbm5haCBSaXZlciBUZWNobm9sb2d5IENlbnRlci4g\n" +
        "IERyLiBDbGFyayBoYXMgc2VydmVkIG9uIHZhcmlvdXMgYm9hcmRzIGFuZCBhZHZpc29yeSBjb21t\n" +
        "aXR0ZWVzLCBpbmNsdWRpbmcgdGhlIE5hdGlvbmFsIEFjYWRlbWllcyBOdWNsZWFyIGFuZCBSYWRp\n" +
        "YXRpb24gU3R1ZGllcyBCb2FyZCBhbmQgdGhlIERlcGFydG1lbnQgb2YgRW5lcmd54oCZcyBCYXNp\n" +
        "YyBFbmVyZ3kgU2NpZW5jZXMgQWR2aXNvcnkgQ29tbWl0dGVlLiAgRHIuIENsYXJrIGhvbGRzIGEg\n" +
        "UGguRC4gYW5kIE0uUy4gaW4gSW5vcmdhbmljL1JhZGlvY2hlbWlzdHJ5IGZyb20gRmxvcmlkYSBT\n" +
        "dGF0ZSBVbml2ZXJzaXR5IGFuZCBhIEIuUy4gaW4gQ2hlbWlzdHJ5IGZyb20gTGFuZGVyIENvbGxl\n" +
        "Z2UuDQo=";

      byte[] octets = new byte[1024];
      int n_octets = Base64.decode(textValue, octets);

      Transmogrifier encoder = new Transmogrifier();
      encoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        EXIDecoder decoder = new EXIDecoder();
        decoder.GrammarCache = grammarCache;

        decoder.EnableBinaryData = true;
        decoder.InitialBinaryDataBufferSize = 64;
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        SAXTransmogrifier saxTransmogrifier = encoder.SAXTransmogrifier;
        BinaryDataSink binaryDataSink;

        saxTransmogrifier.SetDocumentLocator(new LocatorImpl());
        saxTransmogrifier.StartDocument();
        saxTransmogrifier.StartPrefixMapping("foo", "urn:foo");
        saxTransmogrifier.StartElement("urn:foo", "Base64Binary", "foo:Base64Binary", new AttributesImpl());
        binaryDataSink = saxTransmogrifier.startBinaryData(n_octets);
        saxTransmogrifier.binaryData(octets, 0, n_octets, binaryDataSink);
        saxTransmogrifier.endBinaryData(binaryDataSink);
        saxTransmogrifier.EndElement("urn:foo", "Base64Binary", "foo:Base64Binary");
        saxTransmogrifier.EndPrefixMapping("foo");
        saxTransmogrifier.EndDocument();

        byte[] bts = baos.ToArray();
        decoder.InputStream = new MemoryStream(bts);

        MemoryStream octetStream = new MemoryStream();

        scanner = decoder.processHeader();
        try {
          scanner.BinaryChunkSize = 100;
        }
        catch (System.NotSupportedException) {
          Assert.IsTrue(alignment == AlignmentType.compress || alignment == AlignmentType.preCompress);
        }
        EventDescription @event;
        int n_events = 0;
        byte[] decodedBytes = null;
        while ((@event = scanner.nextEvent()) != null) {
          sbyte eventKind = @event.EventKind;
          if (EventDescription_Fields.EVENT_BLOB == eventKind) {
            int n_chunks = 0;
            BinaryDataSource binaryData = @event.BinaryDataSource;
            octetStream.Write(binaryData.ByteArray, binaryData.StartIndex, binaryData.Length);
            ++n_chunks;
            do {
              if (!binaryData.hasNext()) {
                break;
              }
              binaryData.next();
              octetStream.Write(binaryData.ByteArray, binaryData.StartIndex, binaryData.Length);
              ++n_chunks;
            }
            while (true);
            if (alignment == AlignmentType.compress || alignment == AlignmentType.preCompress) {
              Assert.AreEqual(1, n_chunks);
              Assert.IsTrue(n_octets < binaryData.ByteArray.Length && 2 * n_octets > binaryData.ByteArray.Length);
            }
            else {
              Assert.AreEqual(9, n_chunks);
              Assert.AreEqual(128, binaryData.ByteArray.Length);
            }
            decodedBytes = octetStream.ToArray();
          }
          else if (EventDescription_Fields.EVENT_CH == eventKind) {
            Assert.Fail();
          }
          ++n_events;
        }
        Assert.AreEqual(5, n_events);
        Assert.AreEqual(n_octets, decodedBytes.Length);
        for (int i = 0; i < n_octets; i++) {
          Assert.AreEqual(octets[i], decodedBytes[i]);
        }
      }
    }

    /// <summary>
    /// Call binaryData method successively in a row.
    /// </summary>
    [Test]
    public virtual void testBinaryData_01d() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/base64Binary.gram", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      const string textValue =
        "RHIuIFN1ZSBDbGFyayBpcyBjdXJyZW50bHkgYSBSZWdlbnRzIFByb2Zlc3NvciBvZiBDaGVtaXN0\n" +
        "cnkgYXQgV2FzaGluZ3RvbiBTdGF0ZSBVbml2ZXJzaXR5IGluIFB1bGxtYW4sIFdBLCB3aGVyZSBz\n" +
        "aGUgaGFzIHRhdWdodCBhbmQgY29uZHVjdGVkIHJlc2VhcmNoIGluIGFjdGluaWRlIGVudmlyb25t\n" +
        "ZW50YWwgY2hlbWlzdHJ5IGFuZCByYWRpb2FuYWx5dGljYWwgY2hlbWlzdHJ5IHNpbmNlIDE5OTYu\n" +
        "ICBGcm9tIDE5OTIgdG8gMTk5NiwgRHIuIENsYXJrIHdhcyBhIFJlc2VhcmNoIEVjb2xvZ2lzdCBh\n" +
        "dCB0aGUgVW5pdmVyc2l0eSBvZiBHZW9yZ2lh4oCZcyBTYXZhbm5haCBSaXZlciBFY29sb2d5IExh\n" +
        "Ym9yYXRvcnkuICBQcmlvciB0byBoZXIgcG9zaXRpb24gYXQgdGhlIFVuaXZlcnNpdHkgb2YgR2Vv\n" +
        "cmdpYSwgc2hlIHdhcyBhIFNlbmlvciBTY2llbnRpc3QgYXQgdGhlIFdlc3Rpbmdob3VzZSBTYXZh\n" +
        "bm5haCBSaXZlciBDb21wYW554oCZcyBTYXZhbm5haCBSaXZlciBUZWNobm9sb2d5IENlbnRlci4g\n" +
        "IERyLiBDbGFyayBoYXMgc2VydmVkIG9uIHZhcmlvdXMgYm9hcmRzIGFuZCBhZHZpc29yeSBjb21t\n" +
        "aXR0ZWVzLCBpbmNsdWRpbmcgdGhlIE5hdGlvbmFsIEFjYWRlbWllcyBOdWNsZWFyIGFuZCBSYWRp\n" +
        "YXRpb24gU3R1ZGllcyBCb2FyZCBhbmQgdGhlIERlcGFydG1lbnQgb2YgRW5lcmd54oCZcyBCYXNp\n" +
        "YyBFbmVyZ3kgU2NpZW5jZXMgQWR2aXNvcnkgQ29tbWl0dGVlLiAgRHIuIENsYXJrIGhvbGRzIGEg\n" +
        "UGguRC4gYW5kIE0uUy4gaW4gSW5vcmdhbmljL1JhZGlvY2hlbWlzdHJ5IGZyb20gRmxvcmlkYSBT\n" +
        "dGF0ZSBVbml2ZXJzaXR5IGFuZCBhIEIuUy4gaW4gQ2hlbWlzdHJ5IGZyb20gTGFuZGVyIENvbGxl\n" +
        "Z2UuDQo=";

      byte[] octets = new byte[1024];
      int n_octets = Base64.decode(textValue, octets);

      Transmogrifier encoder = new Transmogrifier();
      encoder.GrammarCache = grammarCache;

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveLexicalValues in new bool[] { true, false }) {
          foreach (bool binaryDataEnabled in new bool[] { true, false }) {
            decoder.EnableBinaryData = binaryDataEnabled;
            Scanner scanner;

            encoder.AlignmentType = alignment;
            decoder.AlignmentType = alignment;

            encoder.PreserveLexicalValues = preserveLexicalValues;
            decoder.PreserveLexicalValues = preserveLexicalValues;

            MemoryStream baos = new MemoryStream();
            encoder.OutputStream = baos;

            SAXTransmogrifier saxTransmogrifier = encoder.SAXTransmogrifier;
            BinaryDataSink binaryDataSink;

            saxTransmogrifier.SetDocumentLocator(new LocatorImpl());
            saxTransmogrifier.StartDocument();
            saxTransmogrifier.StartPrefixMapping("foo", "urn:foo");
            saxTransmogrifier.StartElement("urn:foo", "Base64Binary", "foo:Base64Binary", new AttributesImpl());
            try {
              binaryDataSink = saxTransmogrifier.startBinaryData(n_octets);
              saxTransmogrifier.binaryData(octets, 0, n_octets - 400, binaryDataSink);
              saxTransmogrifier.binaryData(octets, n_octets - 400, 400, binaryDataSink);
              saxTransmogrifier.endBinaryData(binaryDataSink);
            }
            catch (SaxException se) {
              TransmogrifierException te = (TransmogrifierException)se.InnerException;
              Assert.AreEqual(TransmogrifierException.UNEXPECTED_BINARY_VALUE, te.Code);
              Assert.IsTrue(preserveLexicalValues);
              continue;
            }
            Assert.IsFalse(preserveLexicalValues);
            saxTransmogrifier.EndElement("urn:foo", "Base64Binary", "foo:Base64Binary");
            saxTransmogrifier.EndPrefixMapping("foo");
            saxTransmogrifier.EndDocument();

            byte[] bts = baos.ToArray();
            decoder.InputStream = new MemoryStream(bts);

            scanner = decoder.processHeader();
            EventDescription @event;
            int n_events = 0;
            byte[] decodedBytes = null;
            string decodedText = null;
            while ((@event = scanner.nextEvent()) != null) {
              sbyte eventKind = @event.EventKind;
              if (EventDescription_Fields.EVENT_BLOB == eventKind) {
                Assert.IsTrue(binaryDataEnabled);
                decodedBytes = BinaryDataUtil.makeBytes(@event.BinaryDataSource);
              }
              else if (EventDescription_Fields.EVENT_CH == eventKind) {
                Assert.IsTrue(!binaryDataEnabled || preserveLexicalValues);
                decodedText = @event.Characters.makeString();
              }
              ++n_events;
            }
            Assert.AreEqual(5, n_events);
            if (binaryDataEnabled && !preserveLexicalValues) {
              Assert.AreEqual(n_octets, decodedBytes.Length);
              for (int i = 0; i < n_octets; i++) {
                Assert.AreEqual(octets[i], decodedBytes[i]);
              }
            }
            else {
              Assert.AreEqual(textValue, decodedText);
            }
          }
        }
      }
    }

    /// <summary>
    /// Test SAXTransmogrifier's characters(byte[] binaryValue, int offset, int length) method
    /// where the method is invoked in a wrong context.
    /// </summary>
    [Test]
    public virtual void testBinaryData_02() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/integer.gram", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      const string textValue = "QUJDREVGR0hJSg==";

      byte[] octets = new byte[64];
      int n_octets = Base64.decode(textValue, octets);

      Transmogrifier encoder = new Transmogrifier();
      encoder.GrammarCache = grammarCache;

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveLexicalValues in new bool[] { true, false }) {
          encoder.AlignmentType = alignment;
          decoder.AlignmentType = alignment;

          encoder.PreserveLexicalValues = preserveLexicalValues;
          decoder.PreserveLexicalValues = preserveLexicalValues;

          MemoryStream baos = new MemoryStream();
          encoder.OutputStream = baos;

          SAXTransmogrifier saxTransmogrifier = encoder.SAXTransmogrifier;

          saxTransmogrifier.SetDocumentLocator(new LocatorImpl());
          saxTransmogrifier.StartDocument();
          saxTransmogrifier.StartPrefixMapping("foo", "urn:foo");
          saxTransmogrifier.StartElement("urn:foo", "Integer", "foo:Integer", new AttributesImpl());
          try {
            saxTransmogrifier.startBinaryData(n_octets);
          }
          catch (SaxException se) {
            TransmogrifierException te = (TransmogrifierException)se.InnerException;
            Assert.AreEqual(TransmogrifierException.UNEXPECTED_BINARY_VALUE, te.Code);
            continue;
          }
          Assert.Fail();
        }
      }
    }

    /// <summary>
    /// Duplicate NS event xmlns:foo="urn:foo" in the same element.
    /// </summary>
    [Test]
    public virtual void testDuplicateNS_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/verySimpleDefault.gram", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

      byte[] bts;
      int n_events;

      Transmogrifier encoder = new Transmogrifier();
      encoder.GrammarCache = grammarCache;

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        SAXTransmogrifier saxTransmogrifier = encoder.SAXTransmogrifier;

        saxTransmogrifier.SetDocumentLocator(new LocatorImpl());
        saxTransmogrifier.StartDocument();
        saxTransmogrifier.StartPrefixMapping("foo", "urn:foo");
        saxTransmogrifier.StartPrefixMapping("foo", "urn:foo");
        saxTransmogrifier.StartElement("", "B", "B", new AttributesImpl());
        saxTransmogrifier.Characters("xyz".ToCharArray(), 0, 3);
        saxTransmogrifier.EndElement("", "B", "B");
        saxTransmogrifier.EndPrefixMapping("foo");
        saxTransmogrifier.EndDocument();

        bts = baos.ToArray();

        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

        EventDescription exiEvent;
        n_events = 0;

        EventType eventType;
        EventTypeList eventTypeList;

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
        Assert.AreEqual("", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
        Assert.AreEqual("foo", ((EXIEventNS)exiEvent).Prefix);
        Assert.AreEqual("urn:foo", ((EXIEventNS)exiEvent).URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("xyz", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNotNull(eventTypeList.EE);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
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
    /// Duplicate NS event xmlns:foo="" in the same element.
    /// </summary>
    [Test]
    public virtual void testDuplicateNS_02() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/verySimpleDefault.gram", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

      byte[] bts;
      int n_events;

      Transmogrifier encoder = new Transmogrifier();
      encoder.GrammarCache = grammarCache;

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        SAXTransmogrifier saxTransmogrifier = encoder.SAXTransmogrifier;

        saxTransmogrifier.SetDocumentLocator(new LocatorImpl());
        saxTransmogrifier.StartDocument();
        saxTransmogrifier.StartPrefixMapping("foo", "");
        saxTransmogrifier.StartPrefixMapping("foo", "");
        saxTransmogrifier.StartElement("", "B", "B", new AttributesImpl());
        saxTransmogrifier.Characters("xyz".ToCharArray(), 0, 3);
        saxTransmogrifier.EndElement("", "B", "B");
        saxTransmogrifier.EndPrefixMapping("foo");
        saxTransmogrifier.EndDocument();

        bts = baos.ToArray();

        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

        EventDescription exiEvent;
        n_events = 0;

        EventType eventType;
        EventTypeList eventTypeList;

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
        Assert.AreEqual("", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
        Assert.AreEqual("foo", ((EXIEventNS)exiEvent).Prefix);
        Assert.AreEqual("", ((EXIEventNS)exiEvent).URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("xyz", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNotNull(eventTypeList.EE);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
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
    /// Duplicate NS event xmlns="urn:foo" in the same element.
    /// </summary>
    [Test]
    public virtual void testDuplicateNS_03() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/verySimpleDefault.gram", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

      byte[] bts;
      int n_events;

      Transmogrifier encoder = new Transmogrifier();
      encoder.GrammarCache = grammarCache;

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        SAXTransmogrifier saxTransmogrifier = encoder.SAXTransmogrifier;

        saxTransmogrifier.SetDocumentLocator(new LocatorImpl());
        saxTransmogrifier.StartDocument();
        saxTransmogrifier.StartPrefixMapping("", "urn:foo");
        saxTransmogrifier.StartPrefixMapping("", "urn:foo");
        saxTransmogrifier.StartElement("", "B", "B", new AttributesImpl());
        saxTransmogrifier.Characters("xyz".ToCharArray(), 0, 3);
        saxTransmogrifier.EndElement("", "B", "B");
        saxTransmogrifier.EndPrefixMapping("foo");
        saxTransmogrifier.EndDocument();

        bts = baos.ToArray();

        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

        EventDescription exiEvent;
        n_events = 0;

        EventType eventType;
        EventTypeList eventTypeList;

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
        Assert.AreEqual("", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
        Assert.AreEqual("", ((EXIEventNS)exiEvent).Prefix);
        Assert.AreEqual("urn:foo", ((EXIEventNS)exiEvent).URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("xyz", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNotNull(eventTypeList.EE);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
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
    /// Duplicate NS event xmlns="" in the same element.
    /// </summary>
    [Test]
    public virtual void testDuplicateNS_04() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/verySimpleDefault.gram", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

      byte[] bts;
      int n_events;

      Transmogrifier encoder = new Transmogrifier();
      encoder.GrammarCache = grammarCache;

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        SAXTransmogrifier saxTransmogrifier = encoder.SAXTransmogrifier;

        saxTransmogrifier.SetDocumentLocator(new LocatorImpl());
        saxTransmogrifier.StartDocument();
        saxTransmogrifier.StartPrefixMapping("", "");
        saxTransmogrifier.StartPrefixMapping("", "");
        saxTransmogrifier.StartElement("", "B", "B", new AttributesImpl());
        saxTransmogrifier.Characters("xyz".ToCharArray(), 0, 3);
        saxTransmogrifier.EndElement("", "B", "B");
        saxTransmogrifier.EndPrefixMapping("foo");
        saxTransmogrifier.EndDocument();

        bts = baos.ToArray();

        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

        EventDescription exiEvent;
        n_events = 0;

        EventType eventType;
        EventTypeList eventTypeList;

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
        Assert.AreEqual("", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
        Assert.AreEqual("", ((EXIEventNS)exiEvent).Prefix);
        Assert.AreEqual("", ((EXIEventNS)exiEvent).URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("xyz", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNotNull(eventTypeList.EE);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
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
    /// Tests BinaryDataHandler's startBinaryData method with manifested total size 
    /// greater than Integer.MAX_VALUE.
    /// </summary>
    [Test]
    public virtual void testBinaryDataLong_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/base64Binary.gram", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier encoder = new Transmogrifier();
      encoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        encoder.AlignmentType = alignment;

        encoder.OutputStream = new MemoryStream();

        SAXTransmogrifier saxTransmogrifier = encoder.SAXTransmogrifier;

        saxTransmogrifier.SetDocumentLocator(new LocatorImpl());
        saxTransmogrifier.StartDocument();
        saxTransmogrifier.StartPrefixMapping("foo", "urn:foo");
        saxTransmogrifier.StartElement("urn:foo", "Base64Binary", "foo:Base64Binary", new AttributesImpl());
        try {
          saxTransmogrifier.startBinaryData(((long)int.MaxValue) * 10); // big data
        }
        catch (SaxException se) {
          Assert.IsTrue(alignment == AlignmentType.preCompress || alignment == AlignmentType.compress);
          TransmogrifierException te = (TransmogrifierException)se.InnerException;
          Assert.AreEqual(TransmogrifierException.SCRIBER_ERROR, te.Code);
          ScriberRuntimeException se2 = (ScriberRuntimeException)te.Exception;
          Assert.AreEqual(ScriberRuntimeException.BINARY_DATA_SIZE_TOO_LARGE, se2.Code);
          continue;
        }
        Assert.IsTrue(alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned);
      }
    }

    /// <summary>
    /// Test decoding big binary size (> Integer.MAX_VALUE). (Only the size part) 
    /// </summary>
    [Test]
    public virtual void testBinaryDataLong_02() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/base64Binary.gram", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      const string textValue =
        "RHIuIFN1ZSBDbGFyayBpcyBjdXJyZW50bHkgYSBSZWdlbnRzIFByb2Zlc3NvciBvZiBDaGVtaXN0\n" +
        "cnkgYXQgV2FzaGluZ3RvbiBTdGF0ZSBVbml2ZXJzaXR5IGluIFB1bGxtYW4sIFdBLCB3aGVyZSBz\n" +
        "aGUgaGFzIHRhdWdodCBhbmQgY29uZHVjdGVkIHJlc2VhcmNoIGluIGFjdGluaWRlIGVudmlyb25t\n" +
        "ZW50YWwgY2hlbWlzdHJ5IGFuZCByYWRpb2FuYWx5dGljYWwgY2hlbWlzdHJ5IHNpbmNlIDE5OTYu\n" +
        "ICBGcm9tIDE5OTIgdG8gMTk5NiwgRHIuIENsYXJrIHdhcyBhIFJlc2VhcmNoIEVjb2xvZ2lzdCBh\n" +
        "dCB0aGUgVW5pdmVyc2l0eSBvZiBHZW9yZ2lh4oCZcyBTYXZhbm5haCBSaXZlciBFY29sb2d5IExh\n" +
        "Ym9yYXRvcnkuICBQcmlvciB0byBoZXIgcG9zaXRpb24gYXQgdGhlIFVuaXZlcnNpdHkgb2YgR2Vv\n" +
        "cmdpYSwgc2hlIHdhcyBhIFNlbmlvciBTY2llbnRpc3QgYXQgdGhlIFdlc3Rpbmdob3VzZSBTYXZh\n" +
        "bm5haCBSaXZlciBDb21wYW554oCZcyBTYXZhbm5haCBSaXZlciBUZWNobm9sb2d5IENlbnRlci4g\n" +
        "IERyLiBDbGFyayBoYXMgc2VydmVkIG9uIHZhcmlvdXMgYm9hcmRzIGFuZCBhZHZpc29yeSBjb21t\n" +
        "aXR0ZWVzLCBpbmNsdWRpbmcgdGhlIE5hdGlvbmFsIEFjYWRlbWllcyBOdWNsZWFyIGFuZCBSYWRp\n" +
        "YXRpb24gU3R1ZGllcyBCb2FyZCBhbmQgdGhlIERlcGFydG1lbnQgb2YgRW5lcmd54oCZcyBCYXNp\n" +
        "YyBFbmVyZ3kgU2NpZW5jZXMgQWR2aXNvcnkgQ29tbWl0dGVlLiAgRHIuIENsYXJrIGhvbGRzIGEg\n" +
        "UGguRC4gYW5kIE0uUy4gaW4gSW5vcmdhbmljL1JhZGlvY2hlbWlzdHJ5IGZyb20gRmxvcmlkYSBT\n" +
        "dGF0ZSBVbml2ZXJzaXR5IGFuZCBhIEIuUy4gaW4gQ2hlbWlzdHJ5IGZyb20gTGFuZGVyIENvbGxl\n" +
        "Z2UuDQo=";

      byte[] octets = new byte[1024];
      int n_octets = Base64.decode(textValue, octets);

      Transmogrifier encoder = new Transmogrifier();
      encoder.GrammarCache = grammarCache;

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;

      decoder.EnableBinaryData = true;
      decoder.InitialBinaryDataBufferSize = 64;
      Scanner scanner;

      MemoryStream baos = new MemoryStream();
      encoder.OutputStream = baos;

      SAXTransmogrifier saxTransmogrifier = encoder.SAXTransmogrifier;
      BinaryDataSink binaryDataSink;

      saxTransmogrifier.SetDocumentLocator(new LocatorImpl());
      saxTransmogrifier.StartDocument();
      saxTransmogrifier.StartPrefixMapping("foo", "urn:foo");
      saxTransmogrifier.StartElement("urn:foo", "Base64Binary", "foo:Base64Binary", new AttributesImpl());
      binaryDataSink = saxTransmogrifier.startBinaryData(((long)int.MaxValue) * 10); // big data
      saxTransmogrifier.binaryData(octets, 0, n_octets, binaryDataSink);
      // Stop encoding here prematurely. We can't output gigantic data.

      byte[] bts = baos.ToArray();
      decoder.InputStream = new MemoryStream(bts);

      MemoryStream octetStream = new MemoryStream();

      const int chunkSize = 100;
      scanner = decoder.processHeader();
      scanner.BinaryChunkSize = chunkSize;

      EventDescription @event;
      while ((@event = scanner.nextEvent()) != null) {
        sbyte eventKind = @event.EventKind;
        if (EventDescription_Fields.EVENT_BLOB == eventKind) {
          BinaryDataSource binaryData = @event.BinaryDataSource;
          Assert.AreEqual(((long)int.MaxValue) * 10 - chunkSize, binaryData.RemainingBytesCount);
          octetStream.Write(binaryData.ByteArray, binaryData.StartIndex, binaryData.Length);
          byte[] decodedBytes = octetStream.ToArray();
          for (int i = 0; i < chunkSize; i++) {
            Assert.AreEqual(octets[i], decodedBytes[i]);
          }
          return;
        }
        else if (EventDescription_Fields.EVENT_CH == eventKind) {
          Assert.Fail();
        }
      }
      Assert.Fail();
    }

    /// <summary>
    /// Tests BinaryDataHandler's startBinaryData method with manifested total size 
    /// greater than Integer.MAX_VALUE.
    /// </summary>
    [Test]
    public virtual void testBinaryDataSizeMismatch_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/base64Binary.gram", this);
      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier encoder = new Transmogrifier();
      encoder.GrammarCache = grammarCache;

      byte[] bts = new byte[100];

      foreach (AlignmentType alignment in Alignments) {
        encoder.AlignmentType = alignment;

        encoder.OutputStream = new MemoryStream();

        SAXTransmogrifier saxTransmogrifier = encoder.SAXTransmogrifier;

        BinaryDataSink binaryDataSink;

        saxTransmogrifier.SetDocumentLocator(new LocatorImpl());
        saxTransmogrifier.StartDocument();
        saxTransmogrifier.StartPrefixMapping("foo", "urn:foo");
        saxTransmogrifier.StartElement("urn:foo", "Base64Binary", "foo:Base64Binary", new AttributesImpl());

        binaryDataSink = saxTransmogrifier.startBinaryData(100);
        saxTransmogrifier.binaryData(bts, 0, 30, binaryDataSink);
        saxTransmogrifier.binaryData(bts, 0, 30, binaryDataSink);
        try {
          saxTransmogrifier.endBinaryData(binaryDataSink);
        }
        catch (SaxException se) {
          TransmogrifierException te = (TransmogrifierException)se.InnerException;
          Assert.AreEqual(TransmogrifierException.SCRIBER_ERROR, te.Code);
          ScriberRuntimeException se2 = (ScriberRuntimeException)te.Exception;
          Assert.AreEqual(ScriberRuntimeException.BINARY_DATA_SIZE_MISMATCH, se2.Code);
          continue;
        }
        Assert.Fail();
      }
    }

  }

}