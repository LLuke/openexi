using System;
using System.Collections.Generic;
using System.IO;
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
using EXIEventSchemaNil = Nagasena.Proc.Events.EXIEventSchemaNil;
using Scanner = Nagasena.Proc.IO.Scanner;
using GrammarCache = Nagasena.Proc.Grammars.GrammarCache;
using EXISchema = Nagasena.Schema.EXISchema;
using TestBase = Nagasena.Schema.TestBase;

using Docbook43Schema = Nagasena.Scomp.Docbook43Schema;
using EXISchemaFactoryTestUtil = Nagasena.Scomp.EXISchemaFactoryTestUtil;

namespace Nagasena.Sax {

  [TestFixture]
  [Category("Enable_Compression")]
  public class GrammarStrictTest : TestBase {

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
    public virtual void testAcceptanceForA_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

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

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

        List<EventDescription> exiEventList = new List<EventDescription>();

        EventDescription exiEvent;
        n_events = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          exiEventList.Add(exiEvent);
        }

        Assert.AreEqual(19, n_events);

        EventType eventType;
        EventTypeList eventTypeList;

        exiEvent = exiEventList[0];
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[1];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[2];
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

        exiEvent = exiEventList[3];
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        Assert.AreEqual(1, eventType.Index); // because of xsi:type
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);

        exiEvent = exiEventList[4];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);

        exiEvent = exiEventList[5];
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

        exiEvent = exiEventList[6];
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);

        exiEvent = exiEventList[7];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);

        exiEvent = exiEventList[8];
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

        exiEvent = exiEventList[9];
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);

        exiEvent = exiEventList[10];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);

        exiEvent = exiEventList[11];
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

        exiEvent = exiEventList[12];
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);

        exiEvent = exiEventList[13];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);

        exiEvent = exiEventList[14];
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

        exiEvent = exiEventList[15];
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);

        exiEvent = exiEventList[16];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);

        exiEvent = exiEventList[17];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);

        exiEvent = exiEventList[18];
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
      }

      xmlString = 
        "<A xmlns='urn:foo'>\n" +
        "  <AB/><AC/><AC/><AC/>\n" + // The 3rd <AC/> is not expected.
        "</A>\n";

      encoder = new Transmogrifier();

      foreach (AlignmentType alignment in Alignments) {
        encoder.AlignmentType = alignment;

        bool caught;

        encoder.GrammarCache = grammarCache;
        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        caught = false;
        try {
          encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));
        }
        catch (TransmogrifierException eee) {
          caught = true;
          Assert.AreEqual(TransmogrifierException.UNEXPECTED_ELEM, eee.Code);
        }
        Assert.IsTrue(caught);
      }
    }

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
    ///   <AB/><AD/>
    /// </A>
    /// </summary>
    [Test]
    public virtual void testAcceptanceForA_02() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string xmlString;
      byte[] bts;
      int n_events;

      xmlString = 
        "<A xmlns='urn:foo'>\n" + 
        "  <AB/><AD/>\n" + 
        "</A>\n";

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

        Assert.AreEqual(10, n_events);

        EventType eventType;
        EventTypeList eventTypeList;

        exiEvent = exiEventList[0];
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[1];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[2];
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

        exiEvent = exiEventList[3];
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);

        exiEvent = exiEventList[4];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);

        exiEvent = exiEventList[5];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AD", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AD", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[6];
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);

        exiEvent = exiEventList[7];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);

        exiEvent = exiEventList[8];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(1, eventType.Index);

        exiEvent = exiEventList[9];
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
      }

      xmlString =
        "<A xmlns='urn:foo'>\n" +
        "  <AB/><AC/><AC/><AE/>\n" + // <AD/> is required.
        "</A>\n";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();

        encoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        bool caught;

        caught = false;
        try {
          encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));
        }
        catch (TransmogrifierException eee) {
          caught = true;
          Assert.AreEqual(TransmogrifierException.UNEXPECTED_ELEM, eee.Code);
        }
        Assert.IsTrue(caught);
      }
    }

    /// <summary>
    /// Schema: 
    /// <xsd:element name="B">
    ///   <xsd:complexType>
    ///     <xsd:sequence>
    ///       <xsd:element ref="foo:AB"/>
    ///       <xsd:element ref="foo:AC" minOccurs="0" maxOccurs="2"/>
    ///       <xsd:element ref="foo:AD" minOccurs="0"/>
    ///     </xsd:sequence>
    ///   </xsd:complexType>
    /// </xsd:element>
    /// 
    /// Instance:
    /// <B>
    ///   <AB/><AC/><AC/><AD/>
    /// </B>
    /// </summary>
    [Test]
    public virtual void testAcceptanceForB() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      const string xmlString =
        "<B xmlns='urn:foo'>\n" +
        "  <AB/><AC/><AC/><AD/>\n" +
        "</B>\n";

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

        Assert.AreEqual(16, n_events);

        EventType eventType;
        EventTypeList eventTypeList;

        exiEvent = exiEventList[0];
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[1];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[2];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AB", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(2, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[3];
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        Assert.AreEqual(1, eventType.Index); // because of xsi:type
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);

        exiEvent = exiEventList[4];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);

        exiEvent = exiEventList[5];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AC", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AC", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual("AD", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.IsNotNull(eventTypeList.EE);

        exiEvent = exiEventList[6];
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);

        exiEvent = exiEventList[7];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);

        exiEvent = exiEventList[8];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AC", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AC", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual("AD", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.IsNotNull(eventTypeList.EE);

        exiEvent = exiEventList[9];
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);

        exiEvent = exiEventList[10];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);

        exiEvent = exiEventList[11];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AD", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AD", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(2, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);

        exiEvent = exiEventList[12];
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);

        exiEvent = exiEventList[13];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);

        exiEvent = exiEventList[14];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);

        exiEvent = exiEventList[15];
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
      }
    }

    /// <summary>
    /// Test getNextSubstanceParticles() method with nested sequences. 
    /// 
    /// <xsd:element name="D">
    ///   <xsd:complexType>
    ///     <xsd:sequence>
    ///       <xsd:sequence>
    ///         <xsd:element name="A" minOccurs="0" maxOccurs="2"/>
    ///         <xsd:sequence maxOccurs="2">
    ///           <xsd:element name="B" />
    ///           <xsd:element name="C" minOccurs="0" />
    ///           <xsd:element name="D" minOccurs="0" />
    ///         </xsd:sequence>
    ///       </xsd:sequence>
    ///       <xsd:element name="E" minOccurs="0"/>
    ///       <xsd:element name="F" minOccurs="0"/>
    ///     </xsd:sequence>
    ///   </xsd:complexType>
    /// </xsd:element>
    /// </summary>
    [Test]
    public virtual void testNextSubstances_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      const string xmlString = 
        "<foo:D xmlns:foo='urn:foo'>\n" +
        "  <A/><A/><B/><C/><D/><B/><E/><F/>\n" +
        "</foo:D>\n";

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

        Assert.AreEqual(20, n_events);

        EventType eventType;
        EventTypeList eventTypeList;

        exiEvent = exiEventList[0];
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[1];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("D", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[2];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("A", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(2, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("", eventType.uri);
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[3];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);

        exiEvent = exiEventList[4];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("A", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(2, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("", eventType.uri);
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[5];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);

        exiEvent = exiEventList[6];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("B", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[7];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);

        exiEvent = exiEventList[8];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("C", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("C", eventType.name);
        Assert.AreEqual("", eventType.uri);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(6, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("D", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("E", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("F", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

        exiEvent = exiEventList[9];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);

        exiEvent = exiEventList[10];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("D", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("D", eventType.name);
        Assert.AreEqual("", eventType.uri);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("E", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("F", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

        exiEvent = exiEventList[11];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);

        exiEvent = exiEventList[12];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("B", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(4, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("E", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("F", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

        exiEvent = exiEventList[13];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);

        exiEvent = exiEventList[14];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("E", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("E", eventType.name);
        Assert.AreEqual("", eventType.uri);
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("C", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("D", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("F", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

        exiEvent = exiEventList[15];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);

        exiEvent = exiEventList[16];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("F", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("F", eventType.name);
        Assert.AreEqual("", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(2, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

        exiEvent = exiEventList[17];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);

        exiEvent = exiEventList[18];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);

        exiEvent = exiEventList[19];
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
      }
    }

    /// <summary>
    /// Test getNextSubstanceParticles() method with nested sequences/choices. 
    /// 
    /// <xsd:element name="E">
    ///   <xsd:complexType>
    ///     <xsd:sequence>
    ///       <xsd:choice>
    ///         <xsd:sequence maxOccurs="2">
    ///           <xsd:element name="A" minOccurs="0" maxOccurs="2" />
    ///           <xsd:element name="B" />
    ///           <xsd:element name="C" minOccurs="0" />
    ///         </xsd:sequence>
    ///         <xsd:sequence minOccurs="0">
    ///           <xsd:element name="D" />
    ///           <xsd:element name="E" />
    ///           <xsd:element name="F" />
    ///         </xsd:sequence>
    ///       </xsd:choice>
    ///       <xsd:element name="G" minOccurs="0" />
    ///       <xsd:element name="H" minOccurs="0" />
    ///     </xsd:sequence>
    ///   </xsd:complexType>
    /// </xsd:element>
    /// </summary>
    [Test]
    public virtual void testNextSubstances_02() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      const string xmlString = 
        "<foo:E xmlns:foo='urn:foo'>\n" +
        "  <A/><A/><B/><C/><B/><G/><H/>\n" +
        "</foo:E>\n";

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

        Assert.AreEqual(18, n_events);

        EventType eventType;
        EventTypeList eventTypeList;

        exiEvent = exiEventList[0];
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[1];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("E", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[2];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("A", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(6, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("D", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("G", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("H", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

        exiEvent = exiEventList[3];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);

        exiEvent = exiEventList[4];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("A", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(2, eventTypeList.Length);
        Assert.IsNull(eventTypeList.EE);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("", eventType.uri);

        exiEvent = exiEventList[5];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);

        exiEvent = exiEventList[6];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("B", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[7];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);

        exiEvent = exiEventList[8];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("C", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("C", eventType.name);
        Assert.AreEqual("", eventType.uri);
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(6, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("G", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("H", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

        exiEvent = exiEventList[9];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);

        exiEvent = exiEventList[10];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("B", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("", eventType.uri);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("G", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("H", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

        exiEvent = exiEventList[11];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);

        exiEvent = exiEventList[12];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("G", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("G", eventType.name);
        Assert.AreEqual("", eventType.uri);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(4, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("C", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("H", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

        exiEvent = exiEventList[13];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);

        exiEvent = exiEventList[14];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("H", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("H", eventType.name);
        Assert.AreEqual("", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(2, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

        exiEvent = exiEventList[15];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);

        exiEvent = exiEventList[16];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);

        exiEvent = exiEventList[17];
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
      }
    }

    /// <summary>
    /// Schema:
    /// <xsd:element name="F" nillable="true">
    ///  <xsd:complexType>
    ///    <xsd:sequence>
    ///      <xsd:element ref="foo:AB"/>
    ///    </xsd:sequence>
    ///    <xsd:attribute ref="foo:aA" use="required"/>
    ///    <xsd:attribute ref="foo:aB" />
    ///  </xsd:complexType>
    /// </xsd:element>
    /// </summary>
    [Test]
    public virtual void testAcceptanceForF() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      MemoryStream baos;

      string xmlString;

      xmlString = 
        "<foo:F xmlns:foo='urn:foo' foo:aA='' foo:aB=''>\n" + 
        "  <foo:AB/>\n" + 
        "</foo:F>\n";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        baos = new MemoryStream();
        encoder.OutputStream = baos;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        byte[] bts = baos.ToArray();

        decoder.GrammarCache = grammarCache;
        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

        EventDescription exiEvent;
        int n_events = 0;

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
        Assert.AreEqual("F", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
        Assert.AreEqual("aA", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("aA", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(2, eventTypeList.Length);
        Assert.IsNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
        Assert.AreEqual("aB", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("aB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(2, eventTypeList.Length);
        Assert.IsNull(eventTypeList.EE);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
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
        Assert.AreEqual(1, eventTypeList.Length);
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
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        Assert.AreEqual(9, n_events);
      }

      xmlString = "<foo:F xmlns:foo='urn:foo' foo:aA='' foo:aB=''>\n" + "</foo:F>\n";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        baos = new MemoryStream();
        encoder.OutputStream = baos;

        try {
          encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));
        }
        catch (TransmogrifierException te) {
          Assert.AreEqual(TransmogrifierException.UNEXPECTED_END_ELEM, te.Code);
          return;
        }
        Assert.Fail();
      }
    }

    /// <summary>
    /// Schema:
    /// <xsd:element name="F" nillable="true">
    ///  <xsd:complexType>
    ///    <xsd:sequence>
    ///      <xsd:element ref="foo:AB"/>
    ///    </xsd:sequence>
    ///    <xsd:attribute ref="foo:aA" use="required"/>
    ///    <xsd:attribute ref="foo:aB" />
    ///  </xsd:complexType>
    /// </xsd:element>
    /// </summary>
    [Test]
    public virtual void testAcceptanceForNilledF() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      MemoryStream baos;

      string xmlString;

      xmlString = 
        "<foo:F xmlns:foo='urn:foo' foo:aA='' foo:aB='' xsi:nil='true' \n" + 
        "       xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'> \n" + 
        "</foo:F>\n";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        baos = new MemoryStream();
        encoder.OutputStream = baos;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        byte[] bts = baos.ToArray();

        decoder.GrammarCache = grammarCache;
        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

        EventDescription exiEvent;
        int n_events = 0;

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
        Assert.AreEqual("F", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_NL, exiEvent.EventKind);
        Assert.AreEqual("nil", exiEvent.Name);
        Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        Assert.AreEqual("nil", eventType.name);
        Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(2, eventTypeList.Length);
        Assert.IsNull(eventTypeList.EE);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("aA", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
        Assert.AreEqual("aA", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("aA", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
        Assert.AreEqual("aB", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("aB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(2, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
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
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        Assert.AreEqual(7, n_events);
      }

      xmlString = 
        "<foo:F xmlns:foo='urn:foo' foo:aA='' foo:aB='' xsi:nil='true' \n" + 
        "       xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'> \n" + 
        "  <foo:AB/>\n" + "</foo:F>\n";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();

        encoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        baos = new MemoryStream();
        encoder.OutputStream = baos;

        try {
          encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));
        }
        catch (TransmogrifierException eee) {
          Assert.AreEqual(TransmogrifierException.UNEXPECTED_ELEM, eee.Code);
          return;
        }
        Assert.Fail();
      }
    }

    /// <summary>
    /// Schema:
    /// <xsd:element name="G" nillable="true">
    ///  <xsd:complexType>
    ///    <xsd:sequence>
    ///      <xsd:element ref="foo:AB" minOccurs="0"/>
    ///    </xsd:sequence>
    ///    <xsd:attribute ref="foo:aA" use="required"/>
    ///    <xsd:attribute ref="foo:aB" />
    ///  </xsd:complexType>
    /// </xsd:element>
    /// </summary>
    [Test]
    public virtual void testAcceptanceForG() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      MemoryStream baos;

      string xmlString;
      byte[] bts;
      int n_events;

      EventDescription exiEvent;

      EventType eventType;
      EventTypeList eventTypeList;

      xmlString = 
        "<foo:G xmlns:foo='urn:foo' foo:aA='' foo:aB=''>\n" +
        "  <foo:AB/>\n" +
        "</foo:G>\n";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        baos = new MemoryStream();
        encoder.OutputStream = baos;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

        decoder.GrammarCache = grammarCache;
        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

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
        Assert.AreEqual("G", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
        Assert.AreEqual("aA", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("aA", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(2, eventTypeList.Length);
        Assert.IsNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
        Assert.AreEqual("aB", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("aB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
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
        Assert.AreEqual(2, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
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
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        Assert.AreEqual(9, n_events);
      }

      xmlString = "<foo:G xmlns:foo='urn:foo' foo:aA='' foo:aB=''>\n" + "</foo:G>\n";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        baos = new MemoryStream();
        encoder.OutputStream = baos;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

        decoder.GrammarCache = grammarCache;
        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

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
        Assert.AreEqual("G", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
        Assert.AreEqual("aA", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("aA", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(2, eventTypeList.Length);
        Assert.IsNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
        Assert.AreEqual("aB", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("aB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(2, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        Assert.AreEqual(6, n_events);
      }
    }

    /// <summary>
    /// Schema:
    /// <xsd:element name="H">
    ///   <xsd:complexType>
    ///     <xsd:sequence>
    ///       <xsd:element name="A" minOccurs="0"/>
    ///       <xsd:any namespace="urn:eoo urn:goo" />
    ///       <xsd:element name="B" />
    ///       <xsd:any namespace="##other" />
    ///     </xsd:sequence>
    ///   </xsd:complexType>
    /// </xsd:element>
    /// </summary>
    [Test]
    public virtual void testAcceptanceForH() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string xmlString;

      xmlString = 
        "<foo:H xmlns:foo='urn:foo' xmlns:goo='urn:goo'>\n" +
        "  <goo:AB/>\n" +
        "  <B/>\n" +
        "  <goo:AB/>\n" +
        "</foo:H>\n";

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

        Assert.AreEqual(12, n_events);

        EventType eventType;
        EventTypeList eventTypeList;

        exiEvent = exiEventList[0];
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[1];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("H", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[2];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AB", exiEvent.Name);
        Assert.AreEqual("urn:goo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:goo", eventType.uri);
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.Length);
        Assert.IsNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:eoo", eventType.uri);

        exiEvent = exiEventList[3];
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        Assert.AreEqual(1, eventType.Index); // because of xsi:type
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);

        exiEvent = exiEventList[4];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);

        exiEvent = exiEventList[5];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("B", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[6];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);

        exiEvent = exiEventList[7];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AB", exiEvent.Name);
        Assert.AreEqual("urn:goo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[8];
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        Assert.AreEqual(1, eventType.Index); // because of xsi:type
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);

        exiEvent = exiEventList[9];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);

        exiEvent = exiEventList[10];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);

        exiEvent = exiEventList[11];
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
      }
    }

    /// <summary>
    /// Schema:
    /// <xsd:element name="H">
    ///   <xsd:complexType>
    ///     <xsd:sequence>
    ///       <xsd:any namespace="##other" minOccurs="0" />
    ///       <xsd:any namespace="##targetNamespace ##local" minOccurs="0" />
    ///     </xsd:sequence>
    ///   </xsd:complexType>
    /// </xsd:element>
    /// 
    /// After the occurrence of "##other" wildcard, "##targetNamespace" wildcard
    /// can come, but not "##other" wildcard again.
    /// </summary>
    [Test]
    public virtual void testAcceptanceForH2_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string xmlString;
      byte[] bts;
      int n_events;

      xmlString =
        "<foo:H2 xmlns:foo='urn:foo' xmlns:goo='urn:goo'>\n" +
        "  <goo:AB/>\n" + // <xsd:any namespace="##other" minOccurs="0" />
        "  <foo:AB/>\n" + // <xsd:any namespace="##targetNamespace ##local" minOccurs="0" />
        "</foo:H2>\n";

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

        Assert.AreEqual(10, n_events);

        EventType eventType;
        EventTypeList eventTypeList;

        exiEvent = exiEventList[0];
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[1];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("H2", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[2];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AB", exiEvent.Name);
        Assert.AreEqual("urn:goo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.EE.Index);

        exiEvent = exiEventList[3];
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);

        exiEvent = exiEventList[4];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);

        exiEvent = exiEventList[5];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AB", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(2, eventTypeList.EE.Index);

        exiEvent = exiEventList[6];
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);

        exiEvent = exiEventList[7];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);

        exiEvent = exiEventList[8];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);

        exiEvent = exiEventList[9];
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
      }

      xmlString =
        "<foo:H2 xmlns:foo='urn:foo' xmlns:goo='urn:goo'>\n" +
        "  <goo:AB/>\n" +
        "  <goo:AB/>\n" + // unexpected
        "</foo:H2>\n";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();

        encoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        bool caught;

        caught = false;
        try {
          encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));
        }
        catch (TransmogrifierException eee) {
          caught = true;
          Assert.AreEqual(TransmogrifierException.UNEXPECTED_ELEM, eee.Code);
        }
        Assert.IsTrue(caught);
      }
    }

    /// <summary>
    /// Schema:
    /// <xsd:element name="H">
    ///   <xsd:complexType>
    ///     <xsd:sequence>
    ///       <xsd:any namespace="##other" minOccurs="0" />
    ///       <xsd:any namespace="##targetNamespace ##local" minOccurs="0" />
    ///     </xsd:sequence>
    ///   </xsd:complexType>
    /// </xsd:element>
    /// 
    /// After the occurrence of "##other" wildcard, "##local" wildcard
    /// can come only once, but not twice.
    /// </summary>
    [Test]
    public virtual void testAcceptanceForH2_02() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string xmlString;
      byte[] bts;
      int n_events;

      xmlString = 
        "<foo:H2 xmlns:foo='urn:foo' xmlns:goo='urn:goo'>\n" + 
        "  <goo:AB/>\n" + 
        "  <AB/>\n" + 
        "</foo:H2>\n";

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

        Assert.AreEqual(10, n_events);

        EventType eventType;
        EventTypeList eventTypeList;

        exiEvent = exiEventList[0];
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[1];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("H2", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[2];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AB", exiEvent.Name);
        Assert.AreEqual("urn:goo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.EE.Index);

        exiEvent = exiEventList[3];
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);

        exiEvent = exiEventList[4];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);

        exiEvent = exiEventList[5];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AB", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("", eventType.uri);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(2, eventTypeList.EE.Index);

        exiEvent = exiEventList[6];
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);

        exiEvent = exiEventList[7];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);

        exiEvent = exiEventList[8];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);

        exiEvent = exiEventList[9];
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
      }

      xmlString =
        "<foo:H2 xmlns:foo='urn:foo' xmlns:goo='urn:goo'>\n" +
        "  <goo:AB/>\n" +
        "  <AB/>\n" +
        "  <AB/>\n" + // unexpected
        "</foo:H2>\n";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();

        encoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        bool caught;

        caught = false;
        try {
          encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));
        }
        catch (TransmogrifierException eee) {
          caught = true;
          Assert.AreEqual(TransmogrifierException.UNEXPECTED_ELEM, eee.Code);
        }
        Assert.IsTrue(caught);
      }
    }

    /// <summary>
    /// Schema:
    /// <xsd:element name="H">
    ///   <xsd:complexType>
    ///     <xsd:sequence>
    ///       <xsd:any namespace="##other" minOccurs="0" />
    ///       <xsd:any namespace="##targetNamespace ##local" minOccurs="0" />
    ///     </xsd:sequence>
    ///   </xsd:complexType>
    /// </xsd:element>
    /// 
    /// "##other" wildcard can be omitted. 
    /// "##other" wildcard cannot occur after "##local" wildcard.
    /// </summary>
    [Test]
    public virtual void testAcceptanceForH2_03() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string xmlString;
      byte[] bts;
      int n_events;

      xmlString = 
        "<foo:H2 xmlns:foo='urn:foo' xmlns:goo='urn:goo'>\n" + 
        "  <foo:AB/>\n" + 
        "</foo:H2>\n";

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

        Assert.AreEqual(7, n_events);

        EventType eventType;
        EventTypeList eventTypeList;

        exiEvent = exiEventList[0];
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[1];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("H2", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[2];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AB", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.EE.Index);

        exiEvent = exiEventList[3];
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        Assert.AreEqual(1, eventType.Index); // because of xsi:type
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);

        exiEvent = exiEventList[4];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);

        exiEvent = exiEventList[5];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);

        exiEvent = exiEventList[6];
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
      }

      xmlString =
        "<foo:H2 xmlns:foo='urn:foo' xmlns:goo='urn:goo'>\n" +
        "  <foo:AB/>\n" +
        "  <goo:AB/>\n" + // unexpected
        "</foo:H2>\n";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();

        encoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        bool caught;

        caught = false;
        try {
          encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));
        }
        catch (TransmogrifierException eee) {
          caught = true;
          Assert.AreEqual(TransmogrifierException.UNEXPECTED_ELEM, eee.Code);
        }
        Assert.IsTrue(caught);
      }
    }

    /// <summary>
    /// Schema:
    /// <xsd:element name="H">
    ///   <xsd:complexType>
    ///     <xsd:sequence>
    ///       <xsd:any namespace="##other" minOccurs="0" />
    ///       <xsd:any namespace="##targetNamespace ##local" minOccurs="0" />
    ///     </xsd:sequence>
    ///   </xsd:complexType>
    /// </xsd:element>
    /// 
    /// The 2nd wildcard ("##targetNamespace ##local") can be omitted. 
    /// characters cannot occur after "##other" wildcard.
    /// </summary>
    [Test]
    public virtual void testAcceptanceForH2_04() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string xmlString;
      byte[] bts;
      int n_events;

      xmlString = 
        "<foo:H2 xmlns:foo='urn:foo' xmlns:goo='urn:goo'>\n" +
        "  <goo:AB/>\n" +
        "</foo:H2>\n";

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

        Assert.AreEqual(7, n_events);

        EventType eventType;
        EventTypeList eventTypeList;

        exiEvent = exiEventList[0];
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[1];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("H2", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[2];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AB", exiEvent.Name);
        Assert.AreEqual("urn:goo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.EE.Index);

        exiEvent = exiEventList[3];
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        Assert.AreEqual(1, eventType.Index); // because of xsi:type
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);

        exiEvent = exiEventList[4];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);

        exiEvent = exiEventList[5];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(2, eventType.Index);

        exiEvent = exiEventList[6];
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
      }

      xmlString =
        "<foo:H2 xmlns:foo='urn:foo' xmlns:goo='urn:goo'>\n" +
        "  <goo:AB/>\n" +
        "  xyz" + // unexpected
        "</foo:H2>\n";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();

        encoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        bool caught;

        caught = false;
        try {
          encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));
        }
        catch (TransmogrifierException eee) {
          caught = true;
          Assert.AreEqual(TransmogrifierException.UNEXPECTED_CHARS, eee.Code);
        }
        Assert.IsTrue(caught);
      }
    }

    /// <summary>
    /// Schema:
    /// <xsd:element name="H3">
    ///   <xsd:complexType>
    ///     <xsd:sequence>
    ///       <xsd:sequence>
    ///         <xsd:any namespace="##local ##targetNamespace" minOccurs="0" maxOccurs="2"/>
    ///         <xsd:element ref="hoo:AC" minOccurs="0"/>
    ///         <xsd:sequence>
    ///           <xsd:any namespace="urn:goo" minOccurs="0" />
    ///           <xsd:element ref="hoo:AB" minOccurs="0"/>
    ///         </xsd:sequence>
    ///       </xsd:sequence>
    ///       <xsd:any namespace="urn:ioo" minOccurs="0" />
    ///     </xsd:sequence>
    ///   </xsd:complexType>
    /// </xsd:element>
    /// 
    /// Use of wildcards in a context with structures.
    /// </summary>
    [Test]
    public virtual void testAcceptanceForH3_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string xmlString;
      byte[] bts;
      int n_events;

      xmlString = 
        "<foo:H3 xmlns:foo='urn:foo' xmlns:goo='urn:goo' xmlns:hoo='urn:hoo' xmlns:ioo='urn:ioo'>\n" +
        "  <foo:AB/>\n" + // <xsd:any namespace="##local ##targetNamespace" minOccurs="0" maxOccurs="2"/>
        "  <AB/>\n" + // same as above
        "  <hoo:AC/>\n" + // <xsd:element ref="hoo:AC" minOccurs="0"/>
        "  <goo:AB/>\n" + // <xsd:any namespace="urn:goo" minOccurs="0" />
        "  <hoo:AB/>\n" + // <xsd:element ref="hoo:AB" minOccurs="0"/>
        "  <ioo:AB/>\n" + // <xsd:any namespace="urn:ioo" minOccurs="0" />
        "</foo:H3>\n";

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

        Assert.AreEqual(22, n_events);

        EventType eventType;
        EventTypeList eventTypeList;

        exiEvent = exiEventList[0];
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[1];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("H3", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("H3", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[2];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AB", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(6, eventTypeList.EE.Index);

        exiEvent = exiEventList[3];
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);

        exiEvent = exiEventList[4];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

        exiEvent = exiEventList[5];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AB", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("", eventType.uri);
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(6, eventTypeList.EE.Index);

        exiEvent = exiEventList[6];
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);

        exiEvent = exiEventList[7];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

        exiEvent = exiEventList[8];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AC", exiEvent.Name);
        Assert.AreEqual("urn:hoo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AC", eventType.name);
        Assert.AreEqual("urn:hoo", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(4, eventTypeList.EE.Index);

        exiEvent = exiEventList[9];
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);

        exiEvent = exiEventList[10];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

        exiEvent = exiEventList[11];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AB", exiEvent.Name);
        Assert.AreEqual("urn:goo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:goo", eventType.uri);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.EE.Index);

        exiEvent = exiEventList[12];
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);

        exiEvent = exiEventList[13];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

        exiEvent = exiEventList[14];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AB", exiEvent.Name);
        Assert.AreEqual("urn:hoo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AB", eventType.name);
        Assert.AreEqual("urn:hoo", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(2, eventTypeList.EE.Index);

        exiEvent = exiEventList[15];
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);

        exiEvent = exiEventList[16];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

        exiEvent = exiEventList[17];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AB", exiEvent.Name);
        Assert.AreEqual("urn:ioo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:ioo", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.EE.Index);

        exiEvent = exiEventList[18];
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);

        exiEvent = exiEventList[19];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

        exiEvent = exiEventList[20];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);

        exiEvent = exiEventList[21];
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
      }

      xmlString = 
        "<foo:H3 xmlns:foo='urn:foo' xmlns:goo='urn:goo' xmlns:hoo='urn:hoo' xmlns:ioo='urn:ioo'>\n" +
        "  <foo:AB/>\n" + // <xsd:any namespace="##targetNamespace ##local" minOccurs="0" maxOccurs="2"/>
        "  <hoo:AD/>\n" + // unexpected
        "</foo:H3>\n";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();

        encoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        bool caught;

        caught = false;
        try {
          encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));
        }
        catch (TransmogrifierException eee) {
          caught = true;
          Assert.AreEqual(TransmogrifierException.UNEXPECTED_ELEM, eee.Code);
        }
        Assert.IsTrue(caught);
      }
    }

    /// <summary>
    /// OPENGIS schema and instance.
    /// There are nested groups in this example.
    /// </summary>
    [Test]
    public virtual void testOpenGisExample01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/opengis/openGis.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        Uri url = resolveSystemIdAsURL("/opengis/openGis.xml");
        FileStream inputStream = new FileStream(url.LocalPath, FileMode.Open);
        InputSource inputSource = new InputSource<Stream>(inputStream, url.ToString());

        encoder.encode(inputSource);

        byte[] bts = baos.ToArray();

        decoder.GrammarCache = grammarCache;
        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

        int n_events = 0;
        while (scanner.nextEvent() != null) {
          ++n_events;
        }

        Assert.AreEqual(77, n_events);
      }
    }

    /// <summary>
    /// Docbook 4.3 schema and instance.
    /// </summary>
    [Test]
    public virtual void testDocbook43ExampleVerySimple01() {
      EXISchema corpus = Docbook43Schema.EXISchema;

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string xmlString;

      xmlString = 
        "<book>\n" +
        "  <part>\n" +
        "    <title>XYZ</title>\n" +
        "    <chapter>\n" +
        "      <title>YZX</title>\n" +
        "      <sect1>\n" +
        "        <title>ZXY</title>\n" +
        "        <para>abcde</para>\n" +
        "      </sect1>\n" +
        "    </chapter>\n" +
        "  </part>\n" +
        "</book>\n";

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

        byte[] bts = baos.ToArray();

        decoder.GrammarCache = grammarCache;
        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

        int n_events = 0;
        while (scanner.nextEvent() != null) {
          ++n_events;
        }

        Assert.AreEqual(22, n_events);
      }
    }

    /// <summary>
    /// FPML 4.0 schema and instance.
    /// xsi:type is used in this example.
    /// </summary>
    [Test]
    [Ignore("ASSERT")]
    public virtual void testFpmlExample01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/fpml-4.0/fpml-main-4-0.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        Uri url = resolveSystemIdAsURL("/fpml-4.0/msg_ex01_request_confirmation.xml");
        FileStream inputStream = new FileStream(url.LocalPath, FileMode.Open);
        InputSource inputSource = new InputSource<Stream>(inputStream, url.ToString());

        encoder.encode(inputSource);

        byte[] bts = baos.ToArray();

        decoder.GrammarCache = grammarCache;
        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

        int n_events = 0;
        while (scanner.nextEvent() != null) {
          ++n_events;
        }

        Assert.AreEqual(102, n_events);
      }
    }

    /// <summary>
    /// Schema:
    /// <xsd:element name="ANY" type="xsd:anyType"/>
    /// 
    /// All attributes and child elements are defined in schema. 
    /// </summary>
    [Test]
    public virtual void testAcceptanceForANY_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string xmlString;

      xmlString = 
        "<foo:ANY xmlns:foo='urn:foo' xmlns:goo='urn:goo'\n" +
        "  foo:aA='a' foo:aB='b' foo:aC='c' >\n" +
        "  TEXT 1 " +
        "  <goo:AB/>\n" +
        "  TEXT 2 " +
        "  <goo:AC/>\n" +
        "  TEXT 3 " +
        "</foo:ANY>\n";

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

        byte[] bts = baos.ToArray();

        decoder.GrammarCache = grammarCache;
        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

        int n_events = 0;
        EventDescription exiEvent;

        EventType eventType;
        EventTypeList eventTypeList;

        exiEvent = scanner.nextEvent();
        ++n_events;
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);

        exiEvent = scanner.nextEvent();
        ++n_events;
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("ANY", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);

        exiEvent = scanner.nextEvent();
        ++n_events;
        Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
        Assert.AreEqual("aA", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        Assert.AreEqual("a", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        Assert.AreEqual(null, eventType.name);
        Assert.IsNull(eventType.uri);
        Assert.IsNull(eventType.name);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);

        exiEvent = scanner.nextEvent();
        ++n_events;
        Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
        Assert.AreEqual("aB", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        Assert.AreEqual("b", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        Assert.AreEqual(null, eventType.name);
        Assert.IsNull(eventType.uri);
        Assert.IsNull(eventType.name);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);

        exiEvent = scanner.nextEvent();
        ++n_events;
        Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
        Assert.AreEqual("aC", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        Assert.AreEqual("c", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        Assert.AreEqual(null, eventType.name);
        Assert.IsNull(eventType.uri);
        Assert.IsNull(eventType.name);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);

        exiEvent = scanner.nextEvent();
        ++n_events;
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("\n  TEXT 1   ", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
        Assert.AreEqual(4, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

        exiEvent = scanner.nextEvent();
        ++n_events;
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AB", exiEvent.Name);
        Assert.AreEqual("urn:goo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        Assert.AreEqual(null, eventType.name);
        Assert.IsNull(eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);

        exiEvent = scanner.nextEvent();
        ++n_events;
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(2, eventTypeList.Length);
        Assert.IsNull(eventTypeList.EE);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);

        exiEvent = scanner.nextEvent();
        ++n_events;
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        Assert.AreEqual(1, eventType.EventTypeList.Length);

        exiEvent = scanner.nextEvent();
        ++n_events;
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("\n  TEXT 2   ", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

        exiEvent = scanner.nextEvent();
        ++n_events;
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AC", exiEvent.Name);
        Assert.AreEqual("urn:goo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        Assert.AreEqual(null, eventType.name);
        Assert.IsNull(eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);

        exiEvent = scanner.nextEvent();
        ++n_events;
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(2, eventTypeList.Length);
        Assert.IsNull(eventTypeList.EE);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);

        exiEvent = scanner.nextEvent();
        ++n_events;
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        Assert.AreEqual(1, eventType.EventTypeList.Length);

        exiEvent = scanner.nextEvent();
        ++n_events;
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("\n  TEXT 3 ", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

        exiEvent = scanner.nextEvent();
        ++n_events;
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);

        exiEvent = scanner.nextEvent();
        ++n_events;
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);

        Assert.AreEqual(16, n_events);
      }
    }

    /// <summary>
    /// Schema:
    /// <xsd:element name="ANY" type="xsd:anyType"/>
    /// 
    /// Use of elements that are not defined in schema.
    /// </summary>
    [Test]
    public virtual void testAcceptanceForANY_02() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string xmlString;

      xmlString = 
        "<foo:ANY xmlns:foo='urn:foo' xmlns:goo='urn:goo'>\n" +
        "  <goo:NONE>abc</goo:NONE>\n" +
        "  <goo:NONE>\n" +
        "    <foo:NONE>def</foo:NONE>\n" +
        "  </goo:NONE>\n" +
        "</foo:ANY>"; 

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveWhitespaces in new bool[] { true, false }) {
          Transmogrifier encoder = new Transmogrifier();
          EXIDecoder decoder = new EXIDecoder();
          Scanner scanner;

          encoder.AlignmentType = alignment;
          decoder.AlignmentType = alignment;

          encoder.GrammarCache = grammarCache;
          encoder.PreserveWhitespaces = preserveWhitespaces;
          MemoryStream baos = new MemoryStream();
          encoder.OutputStream = baos;

          encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

          byte[] bts = baos.ToArray();

          decoder.GrammarCache = grammarCache;
          decoder.InputStream = new MemoryStream(bts);
          scanner = decoder.processHeader();

          int n_events = 0;
          EventDescription exiEvent;

          EventType eventType;
          EventTypeList eventTypeList;

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
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("ANY", eventType.name);
            Assert.AreEqual("urn:foo", eventType.uri);
            eventTypeList = eventType.EventTypeList;
            Assert.IsNull(eventTypeList.EE);
          }

          if ((exiEvent = scanner.nextEvent()) != null) {
            ++n_events;
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual("\n  ", exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
            Assert.AreEqual(4, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(5, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          }

          if ((exiEvent = scanner.nextEvent()) != null) {
            ++n_events;
            Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
            Assert.AreEqual("NONE", exiEvent.Name);
            Assert.AreEqual("urn:goo", exiEvent.URI);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
            Assert.AreEqual(null, eventType.name);
            Assert.IsNull(eventType.uri);
            Assert.AreEqual(0, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(3, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
          }

          if ((exiEvent = scanner.nextEvent()) != null) {
            ++n_events;
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual("abc", exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
              Assert.AreEqual(4, eventType.Index);
              eventTypeList = eventType.EventTypeList;
              Assert.AreEqual(5, eventTypeList.Length);
              Assert.IsNotNull(eventTypeList.EE);
              eventType = eventTypeList.item(0);
              Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
              Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
              eventType = eventTypeList.item(1);
              Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
              Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
              eventType = eventTypeList.item(2);
              Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
              eventType = eventTypeList.item(3);
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
            }
          }

          if ((exiEvent = scanner.nextEvent()) != null) {
            ++n_events;
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual("\n  ", exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
            Assert.AreEqual(2, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(3, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          }

          if ((exiEvent = scanner.nextEvent()) != null) {
            ++n_events;
            Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
            Assert.AreEqual("NONE", exiEvent.Name);
            Assert.AreEqual("urn:goo", exiEvent.URI);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
            Assert.AreEqual(null, eventType.name);
            Assert.IsNull(eventType.uri);
            Assert.AreEqual(0, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(3, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
          }

          if (preserveWhitespaces && (exiEvent = scanner.nextEvent()) != null) {
            ++n_events;
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual("\n    ", exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
            if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
              Assert.AreEqual(0, eventType.Index);
              eventTypeList = eventType.EventTypeList;
              Assert.AreEqual(5, eventTypeList.Length);
              Assert.IsNotNull(eventTypeList.EE);
              eventType = eventTypeList.item(1);
              Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
              Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
              eventType = eventTypeList.item(2);
              Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
              eventType = eventTypeList.item(3);
              Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
              eventType = eventTypeList.item(4);
              Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
              Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            }
          }

          if ((exiEvent = scanner.nextEvent()) != null) {
            ++n_events;
            Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
            Assert.AreEqual("NONE", exiEvent.Name);
            Assert.AreEqual("urn:foo", exiEvent.URI);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            Assert.AreEqual(null, eventType.name);
            Assert.IsNull(eventType.uri);
            if (preserveWhitespaces && (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned)) {
              Assert.AreEqual(2, eventType.Index);
              eventTypeList = eventType.EventTypeList;
              Assert.AreEqual(4, eventTypeList.Length);
              Assert.IsNotNull(eventTypeList.EE);
              eventType = eventTypeList.item(0);
              Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
              Assert.AreEqual("urn:foo", eventType.uri);
              Assert.AreEqual("NONE", eventType.name);
              eventType = eventTypeList.item(1);
              Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
              Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
              eventType = eventTypeList.item(3);
              Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
            }
          }

          if ((exiEvent = scanner.nextEvent()) != null) {
            ++n_events;
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual("def", exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            Assert.AreEqual(4, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(5, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          }

          if ((exiEvent = scanner.nextEvent()) != null) {
            ++n_events;
            Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
            Assert.AreEqual(0, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(3, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }

          if (preserveWhitespaces && (exiEvent = scanner.nextEvent()) != null) {
            ++n_events;
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual("\n  ", exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            Assert.AreEqual(4, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(5, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("urn:foo", eventType.uri);
            Assert.AreEqual("NONE", eventType.name);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          }

          if ((exiEvent = scanner.nextEvent()) != null) {
            ++n_events;
            Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
            if (preserveWhitespaces) {
              Assert.AreEqual(2, eventType.Index);
              eventTypeList = eventType.EventTypeList;
              Assert.AreEqual(5, eventTypeList.Length);
              Assert.IsNotNull(eventTypeList.EE);
              eventType = eventTypeList.item(0);
              Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
              Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
              eventType = eventTypeList.item(1);
              Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
              Assert.AreEqual("urn:foo", eventType.uri);
              Assert.AreEqual("NONE", eventType.name);
              eventType = eventTypeList.item(3);
              Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
              eventType = eventTypeList.item(4);
              Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
              Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            }
          }

          if ((exiEvent = scanner.nextEvent()) != null) {
            ++n_events;
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual("\n", exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
            Assert.AreEqual(2, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(3, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          }

          if ((exiEvent = scanner.nextEvent()) != null) {
            ++n_events;
            Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(1, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(3, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
          }

          if ((exiEvent = scanner.nextEvent()) != null) {
            ++n_events;
            Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreSame(exiEvent, eventType);
            Assert.AreEqual(0, eventType.Index);
          }

          Assert.AreEqual(preserveWhitespaces ? 17 : 15, n_events);
        }
      }
    }

    /// <summary>
    /// Schema:
    /// <xsd:element name="J">
    ///   <xsd:complexType>
    ///     <xsd:sequence maxOccurs="2">
    ///       <xsd:element ref="foo:AB"/>
    ///       <xsd:element ref="foo:AC" minOccurs="0" maxOccurs="2"/>
    ///     </xsd:sequence>
    ///   </xsd:complexType>
    /// </xsd:element>
    /// 
    /// Instance:
    /// <J>
    ///   <AB/><AC/><AC/><AB/><AC/>
    /// </J>
    /// </summary>
    [Test]
    public virtual void testAcceptanceForJ_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string xmlString;
      byte[] bts;
      int n_events;

      xmlString = 
        "<J xmlns='urn:foo'>\n" + 
        "  <AB/><AC/><AC/><AB/><AC/>\n" + 
        "</J>\n";

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

        Assert.AreEqual(19, n_events);

        EventType eventType;
        EventTypeList eventTypeList;

        exiEvent = exiEventList[0];
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[1];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("J", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[2];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AB", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[3];
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        Assert.AreEqual(1, eventType.Index); // because of xsi:type
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);

        exiEvent = exiEventList[4];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);

        exiEvent = exiEventList[5];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AC", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AC", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual("AB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.IsNotNull(eventTypeList.EE);

        exiEvent = exiEventList[6];
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);

        exiEvent = exiEventList[7];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);

        exiEvent = exiEventList[8];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AC", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AC", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual("AB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.IsNotNull(eventTypeList.EE);

        exiEvent = exiEventList[9];
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);

        exiEvent = exiEventList[10];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);

        exiEvent = exiEventList[11];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AB", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(2, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);

        exiEvent = exiEventList[12];
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);

        exiEvent = exiEventList[13];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);

        exiEvent = exiEventList[14];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AC", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AC", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(2, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);

        exiEvent = exiEventList[15];
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);

        exiEvent = exiEventList[16];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);

        exiEvent = exiEventList[17];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(2, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual("AC", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);

        exiEvent = exiEventList[18];
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
      }

      xmlString = 
        "<A xmlns='urn:foo'>\n" +
        "  <AB/><AC/><AC/><AC/>\n" + // The 3rd <AC/> is not expected.
        "</A>\n";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();

        encoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        bool caught;

        caught = false;
        try {
          encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));
        }
        catch (TransmogrifierException eee) {
          caught = true;
          Assert.AreEqual(TransmogrifierException.UNEXPECTED_ELEM, eee.Code);
        }
        Assert.IsTrue(caught);
      }
    }

    /// <summary>
    /// Schema:
    /// None available
    /// 
    /// Instance:
    /// <None>ABC</None>
    /// </summary>
    [Test]
    public virtual void testAcceptanceForNone() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string xmlString;
      byte[] bts;
      int n_events;

      xmlString = "<None xmlns='urn:foo'>ABC</None>\n";

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
        n_events = 0;

        EventType eventType;
        EventTypeList eventTypeList;

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
          int i;
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
          Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
          Assert.AreEqual("ABC", exiEvent.Characters.makeString());
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          Assert.AreEqual(4, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(5, eventTypeList.Length);
          Assert.IsNotNull(eventTypeList.EE);
          eventType = eventTypeList.item(0);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(3, eventTypeList.Length);
          Assert.IsNotNull(eventTypeList.EE);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        }

        if ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreSame(exiEvent, eventType);
          Assert.AreEqual(0, eventType.Index);
        }

        Assert.AreEqual(5, n_events);
      }
    }

    /// <summary>
    /// Schema:
    /// <xsd:element name="Z">
    ///   <xsd:complexType>
    ///     <xsd:sequence>
    ///       <xsd:element ref="foo:Z" minOccurs="0" />
    ///       <xsd:sequence>
    ///         <xsd:element ref="foo:C" minOccurs="0" maxOccurs="unbounded" />
    ///       </xsd:sequence>
    ///     </xsd:sequence>
    ///   </xsd:complexType>
    /// </xsd:element>
    /// 
    /// Instance:
    /// <foo:Z>
    ///   <goo:A />
    ///   <goo:C />
    ///   <aoo:B />
    ///   <aoo:C />
    ///   <foo:C />
    ///   <foo:D />
    /// </foo:Z>
    /// </summary>
    [Test]
    public virtual void testSubstitutionGroup_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/substitutionGroup.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string xmlString;
      byte[] bts;
      int n_events;

      xmlString = 
        "<foo:Z xmlns:aoo='urn:aoo' xmlns:foo='urn:foo' xmlns:goo='urn:goo'>" +
          "<goo:A />" +
          "<goo:C />" +
          "<aoo:B />" +
          "<aoo:C />" +
          "<foo:C />" +
          "<foo:D />" +
        "</foo:Z>";

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

        Assert.AreEqual(16, n_events);

        EventType eventType;
        EventTypeList eventTypeList;

        exiEvent = exiEventList[0];
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[1];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[2];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("A", exiEvent.Name);
        Assert.AreEqual("urn:goo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(8, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Z", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:aoo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("C", eventType.name);
        Assert.AreEqual("urn:aoo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("C", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("C", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("D", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

        exiEvent = exiEventList[3];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(2, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);

        exiEvent = exiEventList[4];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("C", exiEvent.Name);
        Assert.AreEqual("urn:goo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("C", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        Assert.AreEqual(4, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(7, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:aoo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("C", eventType.name);
        Assert.AreEqual("urn:aoo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("C", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("D", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

        exiEvent = exiEventList[5];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);

        exiEvent = exiEventList[6];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("B", exiEvent.Name);
        Assert.AreEqual("urn:aoo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:aoo", eventType.uri);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(7, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("C", eventType.name);
        Assert.AreEqual("urn:aoo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("C", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("C", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("D", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

        exiEvent = exiEventList[7];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(2, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);

        exiEvent = exiEventList[8];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("C", exiEvent.Name);
        Assert.AreEqual("urn:aoo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("C", eventType.name);
        Assert.AreEqual("urn:aoo", eventType.uri);
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(7, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:aoo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("C", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("C", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("D", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

        exiEvent = exiEventList[9];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(2, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);

        exiEvent = exiEventList[10];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("C", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("C", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(7, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:aoo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("C", eventType.name);
        Assert.AreEqual("urn:aoo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("C", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("D", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

        exiEvent = exiEventList[11];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(2, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);

        exiEvent = exiEventList[12];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("D", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("D", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(5, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(7, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:aoo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("C", eventType.name);
        Assert.AreEqual("urn:aoo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("C", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("C", eventType.name);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

        exiEvent = exiEventList[13];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(2, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);

        exiEvent = exiEventList[14];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(6, eventType.Index);

        exiEvent = exiEventList[15];
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
      }
    }

    /// <summary>
    /// Schema: 
    /// <xsd:element name="A" nillable="true">
    ///   <xsd:complexType>
    ///     <xsd:sequence>
    ///       <xsd:element name="B"/>
    ///     </xsd:sequence>
    ///   </xsd:complexType>
    /// </xsd:element>
    /// 
    /// Instance:
    /// <A xsi:nil='false'>
    ///   <B/>
    /// </A>
    /// </summary>
    [Test]
    public virtual void testNilFalse() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/nillable01.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string xmlString;
      byte[] bts;
      MemoryStream baos;

      xmlString = 
        "<A xmlns='urn:foo' xsi:nil='false' \n" +
        "   xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>\n" +
        "  <B/>\n" +
        "</A>\n";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        baos = new MemoryStream();
        encoder.OutputStream = baos;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

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

        Assert.AreEqual(8, n_events);

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
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[2];
        Assert.AreEqual(EventDescription_Fields.EVENT_NL, exiEvent.EventKind);
        Assert.AreEqual("nil", exiEvent.Name);
        Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
        Assert.IsFalse(((EXIEventSchemaNil)exiEvent).Nilled);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        Assert.AreEqual("nil", eventType.name);
        Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(2, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);

        exiEvent = exiEventList[3];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("B", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(2, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        Assert.AreEqual("nil", eventType.name);
        Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);

        exiEvent = exiEventList[4];
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        Assert.AreEqual(1, eventType.Index); // because of xsi:type
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(2, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);

        exiEvent = exiEventList[5];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);

        exiEvent = exiEventList[6];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);

        exiEvent = exiEventList[7];
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
      }
    }

    /// <summary>
    /// Schema:
    /// <xsd:complexType name="datedAddress">
    ///   <xsd:sequence minOccurs="0" maxOccurs="5">
    ///     <xsd:choice maxOccurs="unbounded">
    ///       <xsd:sequence>
    ///         <xsd:element name="Street4a" maxOccurs="2"><xsd:complexType/></xsd:element>
    ///         <xsd:element name="City4a"><xsd:complexType/></xsd:element>
    ///       </xsd:sequence>
    ///       <xsd:sequence maxOccurs="2">
    ///         <xsd:element name="Street4b" maxOccurs="unbounded"><xsd:complexType/></xsd:element>
    ///         <xsd:element name="City4b"><xsd:complexType/></xsd:element>
    ///       </xsd:sequence>
    ///     </xsd:choice>
    ///     <xsd:element name="Zip" maxOccurs="unbounded"><xsd:complexType/></xsd:element>
    ///   </xsd:sequence>
    /// </xsd:complexType>
    /// 
    /// <xsd:element name="DatedAddress" type="foo:datedAddress"/>
    /// 
    /// Instance:
    /// <foo:DatedAddress xmlns:foo="urn:foo">
    ///   <Street4b/><City4b/><Street4b/><City4b/><Zip/>
    /// </foo:DatedAddress>
    /// </summary>
    [Test]
    public virtual void testAmbiguousParticleContext01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/ambiguousParticleContext01.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string xmlString;
      byte[] bts;
      int n_events;

      xmlString = 
        "<foo:DatedAddress xmlns:foo='urn:foo'>\n" +
        "  <Street4b/><City4b/><Street4b/><City4b/><Zip/>\n" +
        "</foo:DatedAddress>\n";

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

        Assert.AreEqual(14, n_events);

        EventType eventType;
        EventTypeList eventTypeList;

        exiEvent = exiEventList[0];
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[1];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("DatedAddress", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[2];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("Street4b", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Street4b", eventType.name);
        Assert.AreEqual("", eventType.uri);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Street4a", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

        exiEvent = exiEventList[3];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);

        exiEvent = exiEventList[4];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("City4b", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("City4b", eventType.name);
        Assert.AreEqual("", eventType.uri);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(2, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Street4b", eventType.name);
        Assert.AreEqual("", eventType.uri);

        exiEvent = exiEventList[5];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);

        exiEvent = exiEventList[6];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("Street4b", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Street4b", eventType.name);
        Assert.AreEqual("", eventType.uri);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Street4a", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Zip", eventType.name);
        Assert.AreEqual("", eventType.uri);

        exiEvent = exiEventList[7];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);

        exiEvent = exiEventList[8];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("City4b", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("City4b", eventType.name);
        Assert.AreEqual("", eventType.uri);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(2, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Street4b", eventType.name);
        Assert.AreEqual("", eventType.uri);

        exiEvent = exiEventList[9];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);

        exiEvent = exiEventList[10];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("Zip", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Zip", eventType.name);
        Assert.AreEqual("", eventType.uri);
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Street4a", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Street4b", eventType.name);
        Assert.AreEqual("", eventType.uri);

        exiEvent = exiEventList[11];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);

        exiEvent = exiEventList[12];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(4, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Street4a", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Street4b", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Zip", eventType.name);
        Assert.AreEqual("", eventType.uri);

        exiEvent = exiEventList[13];
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
      }
    }

    /// <summary>
    /// Schema:
    /// <xsd:element name="Street5"><xsd:complexType/></xsd:element>
    /// 
    /// <xsd:complexType name="datedAddress5">
    ///   <xsd:sequence minOccurs="0" maxOccurs="5">
    ///     <xsd:choice maxOccurs="2">
    ///       <xsd:element ref="foo:Street5" maxOccurs="unbounded"/>
    ///       <xsd:element name="City5a"><xsd:complexType/></xsd:element>
    ///     </xsd:choice>
    ///     <xsd:element name="Zip5" maxOccurs="unbounded"><xsd:complexType/></xsd:element>
    ///   </xsd:sequence>
    /// </xsd:complexType>
    /// 
    /// <xsd:element name="DatedAddress5" type="foo:datedAddress5"/>
    /// 
    /// Instance:
    /// <foo:DatedAddress5 xmlns:foo="urn:foo">
    ///   <foo:Street5/><foo:Street5/><Zip5/>
    /// </foo:DatedAddress5>
    /// </summary>
    [Test]
    public virtual void testAmbiguousParticleContext02() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/ambiguousParticleContext02.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string xmlString;
      byte[] bts;
      int n_events;

      xmlString = 
        "<foo:DatedAddress5 xmlns:foo='urn:foo'>\n" + 
        "  <foo:Street5/><foo:Street5/><Zip5/>\n" + 
        "</foo:DatedAddress5>\n";

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

        Assert.AreEqual(10, n_events);

        EventType eventType;
        EventTypeList eventTypeList;

        exiEvent = exiEventList[0];
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[1];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("DatedAddress5", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[2];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("Street5", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Street5", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("City5a", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

        exiEvent = exiEventList[3];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);

        exiEvent = exiEventList[4];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("Street5", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Street5", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("City5a", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Zip5", eventType.name);
        Assert.AreEqual("", eventType.uri);

        exiEvent = exiEventList[5];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);

        exiEvent = exiEventList[6];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("Zip5", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Zip5", eventType.name);
        Assert.AreEqual("", eventType.uri);
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Street5", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("City5a", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Zip5", eventType.name);
        Assert.AreEqual("", eventType.uri);

        exiEvent = exiEventList[7];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);

        exiEvent = exiEventList[8];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(4, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Street5", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("City5a", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Zip5", eventType.name);
        Assert.AreEqual("", eventType.uri);

        exiEvent = exiEventList[9];
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
      }
    }

    /// <summary>
    /// Schema:
    /// <xsd:element name="E">
    ///   <xsd:complexType>
    ///     <xsd:sequence>
    ///       <xsd:sequence minOccurs="2" maxOccurs="2"><!-- (minOccurs,maxOccurs)=(2,2) -->
    ///         <xsd:element name="A" minOccurs="0" maxOccurs="2" />
    ///         <xsd:sequence maxOccurs="2">
    ///           <xsd:element name="B" />
    ///           <xsd:element name="C" minOccurs="0" />
    ///         </xsd:sequence>
    ///       </xsd:sequence>
    ///       <xsd:element name="D" minOccurs="0" />
    ///     </xsd:sequence>
    ///   </xsd:complexType>
    /// </xsd:element>
    /// 
    /// Instance:
    /// <E>
    ///   <!-- sequence particle of (minOccurs,maxOccurs) = (2,2) -->
    ///   <A/><A/><B/><C/><B/><C/>
    ///   <A/><A/><B/><C/><B/><C/>
    ///   <D/>
    /// </E>
    /// </summary>
    [Test]
    public virtual void testAmbiguousParticleContext03() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/ambiguousParticleContext03.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      const string xmlString = 
        "<E>" + 
        "  <A/><A/><B/><C/><B/><C/>" + 
        "  <A/><A/><B/><C/><B/><C/>" + 
        "  <D/>" + 
        "</E>\n";

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

        byte[] bts = baos.ToArray();

        decoder.GrammarCache = grammarCache;
        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

        EventDescription exiEvent;
        int n_events = 0;

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
        Assert.AreEqual("E", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("A", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(2, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("", eventType.uri);
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
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("A", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(2, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("", eventType.uri);
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
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("B", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
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
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("C", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("C", eventType.name);
        Assert.AreEqual("", eventType.uri);
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("", eventType.uri);
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
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("B", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("", eventType.uri);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(2, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("", eventType.uri);
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
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("C", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("C", eventType.name);
        Assert.AreEqual("", eventType.uri);
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("D", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
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
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("A", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(4, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("D", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
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
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("A", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(2, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("", eventType.uri);
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
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("B", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
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
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("C", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("C", eventType.name);
        Assert.AreEqual("", eventType.uri);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(4, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("D", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
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
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("B", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("D", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
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
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("C", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("C", eventType.name);
        Assert.AreEqual("", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("D", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
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
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("D", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("D", eventType.name);
        Assert.AreEqual("", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(2, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
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
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        ++n_events;

        Assert.AreEqual(30, n_events);
      }
    }

    /// <summary>
    /// Test the attempt to use ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE in
    /// Transmogrifier. 
    /// 
    /// Schema:
    /// <xsd:element name="M">
    ///   <xsd:complexType>
    ///     <xsd:attribute ref="foo:bA" />
    ///     <xsd:attribute ref="foo:bB" use="required" />
    ///     <xsd:attribute ref="foo:bC" />
    ///     <xsd:attribute ref="foo:bD" />
    ///   </xsd:complexType>
    /// </xsd:element>
    /// </summary>
    [Test]
    public virtual void testUseInvalidAttributes() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      const String xmlString = "<foo:M xmlns:foo='urn:foo' foo:bB='true' />\n";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;

        bool caught = false;
        try {
          encoder.OutputStream = new MemoryStream();
          // Transmogrifier will try to fallback to ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE
          encoder.encode(new InputSource<Stream>(string2Stream("<foo:M xmlns:foo='urn:foo' foo:bB='' />\n")));
        }
        catch (TransmogrifierException te) {
          Assert.AreEqual(TransmogrifierException.UNEXPECTED_ATTR, te.Code);
          caught = true;
        }
        Assert.IsTrue(caught);

        MemoryStream baos;
        baos = new MemoryStream();
        encoder.OutputStream = baos;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

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

        Assert.AreEqual(5, n_events);

        EventType eventType;
        EventTypeList eventTypeList;

        exiEvent = exiEventList[0];
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[1];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("M", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[2];
        Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
        Assert.AreEqual("bB", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("bB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(2, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("bA", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);

        exiEvent = exiEventList[3];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("bC", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("bD", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);

        exiEvent = exiEventList[4];
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
      }
    }

  }

}