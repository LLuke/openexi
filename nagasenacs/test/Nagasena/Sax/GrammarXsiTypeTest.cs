using System;
using System.IO;
using System.Collections.Generic;
using NUnit.Framework;

using Org.System.Xml.Sax;

using EXIDecoder = Nagasena.Proc.EXIDecoder;
using AlignmentType = Nagasena.Proc.Common.AlignmentType;
using EventDescription = Nagasena.Proc.Common.EventDescription;
using EventDescription_Fields = Nagasena.Proc.Common.EventDescription_Fields;
using EventCode = Nagasena.Proc.Common.EventCode;
using EventType = Nagasena.Proc.Common.EventType;
using EventTypeList = Nagasena.Proc.Common.EventTypeList;
using GrammarOptions = Nagasena.Proc.Common.GrammarOptions;
using XmlUriConst = Nagasena.Proc.Common.XmlUriConst;
using EXIEventSchemaNil = Nagasena.Proc.Events.EXIEventSchemaNil;
using EXIEventSchemaType = Nagasena.Proc.Events.EXIEventSchemaType;
using GrammarCache = Nagasena.Proc.Grammars.GrammarCache;
using Scanner = Nagasena.Proc.IO.Scanner;
using EXISchema = Nagasena.Schema.EXISchema;
using EXISchemaConst = Nagasena.Schema.EXISchemaConst;
using TestBase = Nagasena.Schema.TestBase;
using EXISchemaFactoryTestUtil = Nagasena.Scomp.EXISchemaFactoryTestUtil;

namespace Nagasena.Sax {

  [TestFixture]
  [Category("Enable_Compression")]
  public class GrammarXsiTypeTest : TestBase {

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
    /// <xsd:complexType name="restricted_B">
    ///   <xsd:complexContent>
    ///     <xsd:restriction base="foo:B">
    ///       <xsd:sequence>
    ///         <xsd:element ref="foo:AB"/>
    ///         <xsd:element ref="foo:AC" minOccurs="0"/>
    ///         <xsd:element ref="foo:AD" minOccurs="0"/>
    ///       </xsd:sequence>
    ///     </xsd:restriction>
    ///   </xsd:complexContent>
    /// </xsd:complexType>
    /// 
    /// Instance:
    /// <B xsi:type='restricted_B'>
    ///   <AB/><AC/><AD/>
    /// </B>
    /// </summary>
    [Test]
    public virtual void testXsiTypeStrict() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string xmlString;

      xmlString =
        "<B xmlns='urn:foo' xsi:type='restricted_B' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>\n" +
        "  <AB/><AC/><AD/>\n" +
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
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[2];
        Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
        Assert.AreEqual("type", exiEvent.Name);
        Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
        Assert.AreEqual("restricted_B", ((EXIEventSchemaType)exiEvent).TypeName);
        Assert.AreEqual("urn:foo", ((EXIEventSchemaType)exiEvent).TypeURI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        Assert.AreEqual("type", eventType.name);
        Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);

        exiEvent = exiEventList[3];
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

        exiEvent = exiEventList[4];
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        Assert.AreEqual(1, eventType.Index); // because of xsi:type
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);

        exiEvent = exiEventList[5];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);

        exiEvent = exiEventList[6];
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

        exiEvent = exiEventList[7];
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);

        exiEvent = exiEventList[8];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);

        exiEvent = exiEventList[9];
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

        exiEvent = exiEventList[10];
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);

        exiEvent = exiEventList[11];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);

        exiEvent = exiEventList[12];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);

        exiEvent = exiEventList[13];
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
      }

      xmlString =
        "<B xmlns='urn:foo' xsi:type='restricted_B' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>\n" +
        "  <AB/><AC/><AC/><AD/>\n" +
        "</B>\n";

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
    /// <xsd:complexType name="restricted_B">
    ///   <xsd:complexContent>
    ///     <xsd:restriction base="foo:B">
    ///       <xsd:sequence>
    ///         <xsd:element ref="foo:AB"/>
    ///         <xsd:element ref="foo:AC" minOccurs="0"/>
    ///         <xsd:element ref="foo:AD" minOccurs="0"/>
    ///       </xsd:sequence>
    ///     </xsd:restriction>
    ///   </xsd:complexContent>
    /// </xsd:complexType>
    /// 
    /// Instance:
    /// <B xsi:type='restricted_B'>
    ///   <AB/><AC/><AD/>
    /// </B>
    /// </summary>
    [Test]
    public virtual void testXsiTypeDefault() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString =
        "<B xmlns='urn:foo' xsi:type='restricted_B' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>\n" +
        "  <AB> </AB><AC> </AC><AD> </AD>" +
        "</B>\n";

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

          List<EventDescription> exiEventList = new List<EventDescription>();

          EventDescription exiEvent;
          int n_events = 0;
          while ((exiEvent = scanner.nextEvent()) != null) {
            ++n_events;
            exiEventList.Add(exiEvent);
          }

          Assert.AreEqual(preserveWhitespaces ? 15 : 14, n_events);

          EventType eventType;
          EventTypeList eventTypeList;
          int n = 0;

          exiEvent = exiEventList[n++];
          Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreSame(exiEvent, eventType);
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(1, eventTypeList.Length);

          exiEvent = exiEventList[n++];
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("B", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
          eventTypeList = eventType.EventTypeList;
          Assert.IsNull(eventTypeList.EE);

          exiEvent = exiEventList[n++];
          Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
          Assert.AreEqual("type", exiEvent.Name);
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
          Assert.AreEqual("restricted_B", ((EXIEventSchemaType)exiEvent).TypeName);
          Assert.AreEqual("urn:foo", ((EXIEventSchemaType)exiEvent).TypeURI);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
          Assert.AreEqual("type", eventType.name);
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(8, eventTypeList.Length);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("AB", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
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

          if (preserveWhitespaces) {
            exiEvent = exiEventList[n++];
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual("\n  ", exiEvent.Characters.makeString());
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
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("AB", eventType.name);
            Assert.AreEqual("urn:foo", eventType.uri);
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
          }

          exiEvent = exiEventList[n++];
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("AB", exiEvent.Name);
          Assert.AreEqual("urn:foo", exiEvent.URI);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("AB", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
          if (preserveWhitespaces) {
            Assert.AreEqual(0, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(4, eventTypeList.Length);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }

          exiEvent = exiEventList[n++];
          Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
          Assert.AreEqual(" ", exiEvent.Characters.makeString());
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

          exiEvent = exiEventList[n++];
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

          exiEvent = exiEventList[n++];
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("AC", exiEvent.Name);
          Assert.AreEqual("urn:foo", exiEvent.URI);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("AC", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(5, eventTypeList.Length);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("AD", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);

          exiEvent = exiEventList[n++];
          Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
          Assert.AreEqual(" ", exiEvent.Characters.makeString());
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

          exiEvent = exiEventList[n++];
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

          exiEvent = exiEventList[n++];
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("AD", exiEvent.Name);
          Assert.AreEqual("urn:foo", exiEvent.URI);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("AD", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(4, eventTypeList.Length);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);

          exiEvent = exiEventList[n++];
          Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
          Assert.AreEqual(" ", exiEvent.Characters.makeString());
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

          exiEvent = exiEventList[n++];
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

          exiEvent = exiEventList[n++];
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

          exiEvent = exiEventList[n++];
          Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreSame(exiEvent, eventType);
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(1, eventTypeList.Length);
        }
      }
    }

    /// <summary>
    /// Schema: 
    /// <xsd:complexType name="restricted_B">
    ///   <xsd:complexContent>
    ///     <xsd:restriction base="foo:B">
    ///       <xsd:sequence>
    ///         <xsd:element ref="foo:AB"/>
    ///         <xsd:element ref="foo:AC" minOccurs="0"/>
    ///         <xsd:element ref="foo:AD" minOccurs="0"/>
    ///       </xsd:sequence>
    ///     </xsd:restriction>
    ///   </xsd:complexContent>
    /// </xsd:complexType>
    /// 
    /// <xsd:element name="nillable_B" type="foo:B" nillable="true" />
    /// 
    /// Instance:
    /// <nillable_B xmlns='urn:foo' xsi:type='restricted_B' xsi:nil='true' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'/>
    /// 
    /// xsi:type and xsi:nil='true' cannot co-occur in strict schema mode.
    /// </summary>
    [Test]
    public virtual void testXsiTypeNillableStrict() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string xmlString;
      byte[] bts;
      MemoryStream baos;
      bool caught;
      List<EventDescription> exiEventList;
      EventDescription exiEvent;
      int n_events;
      EventType eventType;
      EventTypeList eventTypeList;

      xmlString =
        "<nillable_B xmlns='urn:foo' xsi:type='restricted_B' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>\n" +
        "  <AB/>\n" +
        "</nillable_B>\n";

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

        exiEventList = new List<EventDescription>();

        n_events = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          exiEventList.Add(exiEvent);
        }

        Assert.AreEqual(8, n_events);

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
        Assert.AreEqual("nillable_B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[2];
        Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
        Assert.AreEqual("type", exiEvent.Name);
        Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
        Assert.AreEqual("restricted_B", ((EXIEventSchemaType)exiEvent).TypeName);
        Assert.AreEqual("urn:foo", ((EXIEventSchemaType)exiEvent).TypeURI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        Assert.AreEqual("type", eventType.name);
        Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);

        exiEvent = exiEventList[3];
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
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AC", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AD", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);

        exiEvent = exiEventList[7];
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
      }

      xmlString =
        "<nillable_B xmlns='urn:foo' xsi:nil='true' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>\n" +
        "  <AB/>\n" +
        "</nillable_B>\n";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();

        encoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        baos = new MemoryStream();
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

      xmlString =
        "<nillable_B xmlns='urn:foo' xsi:nil='true' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>\n" +
        "</nillable_B>\n";

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
        exiEventList = new List<EventDescription>();
        while ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          exiEventList.Add(exiEvent);
        }

        Assert.AreEqual(5, n_events);

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
        Assert.AreEqual("nillable_B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[2];
        Assert.AreEqual(EventDescription_Fields.EVENT_NL, exiEvent.EventKind);
        Assert.AreEqual("nil", exiEvent.Name);
        Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
        Assert.IsTrue(((EXIEventSchemaNil)exiEvent).Nilled);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        Assert.AreEqual("nil", eventType.name);
        Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);

        exiEvent = exiEventList[3];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);

        exiEvent = exiEventList[4];
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
      }

      // xsi:type and xsi:nil cannot co-occur in strict schema mode.
      xmlString =
        "<nillable_B xmlns='urn:foo' xsi:type='restricted_B' xsi:nil='true' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>\n" +
        "</nillable_B>\n";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();

        encoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        baos = new MemoryStream();
        encoder.OutputStream = baos;

        caught = false;
        try {
          encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));
        }
        catch (TransmogrifierException eee) {
          caught = true;
          Assert.AreEqual(TransmogrifierException.UNEXPECTED_ATTR, eee.Code);
        }
        Assert.IsTrue(caught);
      }
    }

    /// <summary>
    /// Schema: 
    /// <xsd:complexType name="restricted_B">
    ///   <xsd:complexContent>
    ///     <xsd:restriction base="foo:B">
    ///       <xsd:sequence>
    ///         <xsd:element ref="foo:AB"/>
    ///         <xsd:element ref="foo:AC" minOccurs="0"/>
    ///         <xsd:element ref="foo:AD" minOccurs="0"/>
    ///       </xsd:sequence>
    ///     </xsd:restriction>
    ///   </xsd:complexContent>
    /// </xsd:complexType>
    /// 
    /// <xsd:element name="nillable_B" type="foo:B" nillable="true" />
    /// 
    /// Instance:
    /// <nillable_B xmlns='urn:foo' xsi:type='restricted_B' xsi:nil='true' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'/>
    /// 
    /// xsi:type and xsi:nil='true' can occur together in default (i.e. non-strict) schema mode.
    /// </summary>
    [Test]
    public virtual void testXsiTypeNillableDefault() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      string xmlString;
      byte[] bts;
      MemoryStream baos;

      EventDescription exiEvent;
      int n_events = 0;

      List<EventDescription> exiEventList;

      EventType eventType;
      EventTypeList eventTypeList;

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveWhitespaces in new bool[] { true, false }) {
          xmlString =
            "<nillable_B xmlns='urn:foo' xsi:type='restricted_B' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>\n" +
            "  <AB> </AB>" +
            "</nillable_B>\n";

          Transmogrifier encoder = new Transmogrifier();
          EXIDecoder decoder = new EXIDecoder();
          Scanner scanner;

          encoder.AlignmentType = alignment;
          decoder.AlignmentType = alignment;

          encoder.GrammarCache = grammarCache;
          encoder.PreserveWhitespaces = preserveWhitespaces;

          baos = new MemoryStream();
          encoder.OutputStream = baos;

          encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

          bts = baos.ToArray();

          decoder.GrammarCache = grammarCache;
          decoder.InputStream = new MemoryStream(bts);
          scanner = decoder.processHeader();

          exiEventList = new List<EventDescription>();

          n_events = 0;
          while ((exiEvent = scanner.nextEvent()) != null) {
            ++n_events;
            exiEventList.Add(exiEvent);
          }

          Assert.AreEqual(preserveWhitespaces ? 9 : 8, n_events);

          int n = 0;

          exiEvent = exiEventList[n++];
          Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreSame(exiEvent, eventType);
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(1, eventTypeList.Length);

          exiEvent = exiEventList[n++];
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("nillable_B", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
          eventTypeList = eventType.EventTypeList;
          Assert.IsNull(eventTypeList.EE);

          exiEvent = exiEventList[n++];
          Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
          Assert.AreEqual("type", exiEvent.Name);
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
          Assert.AreEqual("restricted_B", ((EXIEventSchemaType)exiEvent).TypeName);
          Assert.AreEqual("urn:foo", ((EXIEventSchemaType)exiEvent).TypeURI);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
          Assert.AreEqual("type", eventType.name);
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(8, eventTypeList.Length);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("AB", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
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

          if (preserveWhitespaces) {
            exiEvent = exiEventList[n++];
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual("\n  ", exiEvent.Characters.makeString());
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
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("AB", eventType.name);
            Assert.AreEqual("urn:foo", eventType.uri);
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
          }

          exiEvent = exiEventList[n++];
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("AB", exiEvent.Name);
          Assert.AreEqual("urn:foo", exiEvent.URI);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("AB", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
          if (preserveWhitespaces) {
            Assert.AreEqual(0, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(4, eventTypeList.Length);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }

          exiEvent = exiEventList[n++];
          Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
          Assert.AreEqual(" ", exiEvent.Characters.makeString());
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

          exiEvent = exiEventList[n++];
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

          exiEvent = exiEventList[n++];
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(2, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(5, eventTypeList.Length);
          eventType = eventTypeList.item(0);
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("AC", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("AD", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);

          exiEvent = exiEventList[n++];
          Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreSame(exiEvent, eventType);
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(1, eventTypeList.Length);
        }
      }

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveWhitespaces in new bool[] { true, false }) {
          xmlString = "<nillable_B xmlns='urn:foo' xsi:nil='true' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>\n" + "</nillable_B>\n";

          Transmogrifier encoder = new Transmogrifier();
          EXIDecoder decoder = new EXIDecoder();
          Scanner scanner;

          encoder.AlignmentType = alignment;
          decoder.AlignmentType = alignment;

          encoder.GrammarCache = grammarCache;
          encoder.PreserveWhitespaces = preserveWhitespaces;

          baos = new MemoryStream();
          encoder.OutputStream = baos;

          encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

          bts = baos.ToArray();

          decoder.GrammarCache = grammarCache;
          decoder.InputStream = new MemoryStream(bts);
          scanner = decoder.processHeader();

          n_events = 0;
          exiEventList = new List<EventDescription>();
          while ((exiEvent = scanner.nextEvent()) != null) {
            ++n_events;
            exiEventList.Add(exiEvent);
          }

          Assert.AreEqual(preserveWhitespaces ? 6 : 5, n_events);

          int n = 0;

          exiEvent = exiEventList[n++];
          Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreSame(exiEvent, eventType);
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(1, eventTypeList.Length);

          exiEvent = exiEventList[n++];
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("nillable_B", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
          eventTypeList = eventType.EventTypeList;
          Assert.IsNull(eventTypeList.EE);

          exiEvent = exiEventList[n++];
          Assert.AreEqual(EventDescription_Fields.EVENT_NL, exiEvent.EventKind);
          Assert.AreEqual("nil", exiEvent.Name);
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
          Assert.IsTrue(((EXIEventSchemaNil)exiEvent).Nilled);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
          Assert.AreEqual("nil", eventType.name);
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
          Assert.AreEqual(1, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(8, eventTypeList.Length);
          eventType = eventTypeList.item(0);
          Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("AB", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
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

          if (preserveWhitespaces) {
            exiEvent = exiEventList[n++];
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual("\n", exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
            Assert.AreEqual(4, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(5, eventTypeList.Length);
            /// <summary>
            /// ITEM_SCHEMA_TYPE no longer participates in TypeEmpty grammars.
            /// eventType = eventTypeList.item(0);
            /// Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
            /// </summary>
            /// <summary>
            /// ITEM_SCHEMA_NIL no longer participates in TypeEmpty grammars.
            /// eventType = eventTypeList.item(1);
            /// Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
            /// </summary>
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          }

          exiEvent = exiEventList[n++];
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          if (preserveWhitespaces) {
            Assert.AreEqual(0, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(3, eventTypeList.Length);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }

          exiEvent = exiEventList[n++];
          Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreSame(exiEvent, eventType);
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(1, eventTypeList.Length);
        }
      }

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveWhitespaces in new bool[] { true, false }) {
          xmlString = "<nillable_B xmlns='urn:foo' xsi:type='restricted_B' xsi:nil='true' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>\n" + "</nillable_B>\n";

          Transmogrifier encoder = new Transmogrifier();
          EXIDecoder decoder = new EXIDecoder();
          Scanner scanner;

          encoder.AlignmentType = alignment;
          decoder.AlignmentType = alignment;

          encoder.GrammarCache = grammarCache;
          encoder.PreserveWhitespaces = preserveWhitespaces;

          baos = new MemoryStream();
          encoder.OutputStream = baos;

          // xsi:type and xsi:nil cannot co-occur in strict schema mode, but are permitted to
          // occur together otherwise.
          encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

          bts = baos.ToArray();

          decoder.GrammarCache = grammarCache;
          decoder.InputStream = new MemoryStream(bts);
          scanner = decoder.processHeader();

          n_events = 0;
          exiEventList = new List<EventDescription>();
          while ((exiEvent = scanner.nextEvent()) != null) {
            ++n_events;
            exiEventList.Add(exiEvent);
          }

          Assert.AreEqual(preserveWhitespaces ? 7 : 6, n_events);

          int n = 0;

          exiEvent = exiEventList[n++];
          Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreSame(exiEvent, eventType);
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(1, eventTypeList.Length);

          exiEvent = exiEventList[n++];
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("nillable_B", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
          eventTypeList = eventType.EventTypeList;
          Assert.IsNull(eventTypeList.EE);

          exiEvent = exiEventList[n++];
          Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
          Assert.AreEqual("type", exiEvent.Name);
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
          Assert.AreEqual("restricted_B", ((EXIEventSchemaType)exiEvent).TypeName);
          Assert.AreEqual("urn:foo", ((EXIEventSchemaType)exiEvent).TypeURI);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
          Assert.AreEqual("type", eventType.name);
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(8, eventTypeList.Length);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("AB", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
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

          exiEvent = exiEventList[n++];
          Assert.AreEqual(EventDescription_Fields.EVENT_NL, exiEvent.EventKind);
          Assert.AreEqual("nil", exiEvent.Name);
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
          Assert.IsTrue(((EXIEventSchemaNil)exiEvent).Nilled);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
          Assert.AreEqual("nil", eventType.name);
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
          Assert.AreEqual(1, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(8, eventTypeList.Length);
          eventType = eventTypeList.item(0);
          Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("AB", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
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

          if (preserveWhitespaces) {
            exiEvent = exiEventList[n++];
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
            Assert.AreEqual(4, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(5, eventTypeList.Length);
            /// <summary>
            /// ITEM_SCHEMA_TYPE no longer participates in TypeEmpty grammars.
            /// eventType = eventTypeList.item(0);
            /// Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
            /// </summary>
            /// <summary>
            /// ITEM_SCHEMA_NIL no longer participates in TypeEmpty grammars.
            /// eventType = eventTypeList.item(1);
            /// Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
            /// </summary>
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          }

          exiEvent = exiEventList[n++];
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          if (preserveWhitespaces) {
            Assert.AreEqual(0, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(3, eventTypeList.Length);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);

            exiEvent = exiEventList[n++];
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

    /// <summary>
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
    /// <C xsi:type="foo:B" xmlns="urn:foo">
    ///   <AB/><AD/>
    /// </C>
    /// 
    /// where
    /// 
    /// <xsd:complexType name="B">
    ///   <xsd:sequence>
    ///     <xsd:element ref="foo:AB"/>
    ///     <xsd:element ref="foo:AC" minOccurs="0" maxOccurs="2"/>
    ///     <xsd:element ref="foo:AD" minOccurs="0"/>
    ///   </xsd:sequence>
    /// </xsd:complexType>
    /// 
    /// Use of xsi:type is permitted in default mode, even though it would
    /// not have been permitted in strict mode. 
    /// </summary>
    [Test]
    public virtual void testLenientXsiType_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache;

      grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString =
        "<C xsi:type='B' xmlns='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>" +
          "<AB> </AB><AD> </AD>" +
        "</C>\n";
        
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

        Assert.AreEqual(11, n_events);

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
        Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(9, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AC", eventType.name);
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

        exiEvent = exiEventList[3];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AB", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
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

        exiEvent = exiEventList[4];
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual(" ", exiEvent.Characters.makeString());
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

        exiEvent = exiEventList[5];
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

        exiEvent = exiEventList[6];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AD", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AD", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AC", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);

        exiEvent = exiEventList[7];
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual(" ", exiEvent.Characters.makeString());
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

        exiEvent = exiEventList[8];
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

        exiEvent = exiEventList[9];
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

        exiEvent = exiEventList[10];
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
      }

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();

        encoder.AlignmentType = alignment;

        bool caught;

        grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

        encoder.GrammarCache = grammarCache;
        encoder.OutputStream = new MemoryStream();

        caught = false;
        try {
          encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));
        }
        catch (TransmogrifierException eee) {
          caught = true;
          Assert.AreEqual(TransmogrifierException.UNEXPECTED_ATTR, eee.Code);
        }
        Assert.IsTrue(caught);
      }
    }

    /// <summary>
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
    /// <C xsi:type="finalString" xmlns="urn:foo">xyz</C>
    /// 
    /// where
    /// 
    /// <xsd:simpleType name="finalString" final="#all">
    ///   <xsd:restriction base="xsd:string" />
    /// </xsd:simpleType>
    /// 
    /// Use of xsi:type is permitted in default mode, even though it would
    /// not have been permitted in strict mode. 
    /// </summary>
    [Test]
    public virtual void testLenientXsiType_02() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache;

      grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString =
        "<C xsi:type='finalString' xmlns='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>" +
          "xyz" +
        "</C>\n";

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
        Assert.AreEqual(1, eventTypeList.Length);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("C", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(9, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AC", eventType.name);
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
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("xyz", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          int tp = scanner.currentState.contentDatatype;
          int builtinType = corpus.getBaseTypeOfSimpleType(tp);
          Assert.AreEqual(EXISchemaConst.STRING_TYPE, corpus.getSerialOfType(builtinType));
        }
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

        Assert.AreEqual(6, n_events);
      }

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();

        encoder.AlignmentType = alignment;

        bool caught;

        grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

        encoder.GrammarCache = grammarCache;
        encoder.OutputStream = new MemoryStream();

        caught = false;
        try {
          encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));
        }
        catch (TransmogrifierException eee) {
          caught = true;
          Assert.AreEqual(TransmogrifierException.UNEXPECTED_ATTR, eee.Code);
        }
        Assert.IsTrue(caught);
      }
    }

    /// <summary>
    /// Schema:
    /// <xsd:element name="K">
    ///   <xsd:simpleType>
    ///     <xsd:restriction base="xsd:string" />
    ///   </xsd:simpleType>
    /// </xsd:element>
    /// 
    /// Instance:
    /// <K xsi:type="foo:C" xmlns="urn:foo">
    ///   <AB/><AD/>
    /// </K>
    /// 
    /// where
    /// 
    /// <xsd:complexType name="C">
    ///   <xsd:all>
    ///     <xsd:element ref="foo:AB" minOccurs="0" />
    ///     <xsd:element ref="foo:AC" minOccurs="0" />
    ///   </xsd:all>
    /// </xsd:complexType>
    /// 
    /// Use of xsi:type is permitted in default mode, even though it would
    /// not have been permitted in strict mode. 
    /// </summary>
    [Test]
    public virtual void testLenientXsiType_03() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache;

      grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString =
        "<K xsi:type='C' xmlns='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>\n" +
        "</K>\n";

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

          List<EventDescription> exiEventList = new List<EventDescription>();

          EventDescription exiEvent;
          int n_events = 0;
          while ((exiEvent = scanner.nextEvent()) != null) {
            ++n_events;
            exiEventList.Add(exiEvent);
          }

          Assert.AreEqual(preserveWhitespaces ? 6 : 5, n_events);

          EventType eventType;
          EventTypeList eventTypeList;
          int n = 0;

          exiEvent = exiEventList[n++];
          Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreSame(exiEvent, eventType);
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(1, eventTypeList.Length);

          exiEvent = exiEventList[n++];
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("K", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
          eventTypeList = eventType.EventTypeList;
          Assert.IsNull(eventTypeList.EE);

          exiEvent = exiEventList[n++];
          Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(8, eventTypeList.Length);
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

          if (preserveWhitespaces) {
            exiEvent = exiEventList[n++];
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual("\n", exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
            Assert.AreEqual(8, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(9, eventTypeList.Length);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("AB", eventType.name);
            Assert.AreEqual("urn:foo", eventType.uri);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("AC", eventType.name);
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
          }

          exiEvent = exiEventList[n++];
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          if (preserveWhitespaces) {
            Assert.AreEqual(2, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(5, eventTypeList.Length);
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
          }

          exiEvent = exiEventList[n++];
          Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreSame(exiEvent, eventType);
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(1, eventTypeList.Length);
        }
      }

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();

        encoder.AlignmentType = alignment;

        bool caught;

        grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

        encoder.GrammarCache = grammarCache;
        encoder.OutputStream = new MemoryStream();

        caught = false;
        try {
          encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));
        }
        catch (TransmogrifierException eee) {
          caught = true;
          Assert.AreEqual(TransmogrifierException.UNEXPECTED_ATTR, eee.Code);
        }
        Assert.IsTrue(caught);
      }
    }

    /// <summary>
    /// Schema: 
    /// <xsd:simpleType name="unionedEnum">
    ///   <xsd:restriction>
    ///     <xsd:simpleType>
    ///       <xsd:union memberTypes="xsd:int xsd:NMTOKEN"/>
    ///     </xsd:simpleType>
    ///     <xsd:enumeration value="100"/>
    ///     <xsd:enumeration value="Tokyo"/>
    ///     <xsd:enumeration value="101"/>
    ///   </xsd:restriction>
    /// </xsd:simpleType>
    /// 
    /// <xsd:element name="unionedEnum" type="foo:unionedEnum" />
    /// 
    /// Instance:
    /// <foo:unionedEnum xmlns:foo='urn:foo' xmlns:xsd='http://www.w3.org/2001/XMLSchema' 
    ///   xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' 
    ///   xsi:type='xsd:int'>12345</foo:unionedEnum>
    /// </summary>
    [Test]
    public virtual void testXsiTypeOnElementOfUnion() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/interop/schemaInformedGrammar/undeclaredProductions/union.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      byte[] bts;
      MemoryStream baos;

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        baos = new MemoryStream();
        encoder.OutputStream = baos;

        Uri url = resolveSystemIdAsURL("/interop/schemaInformedGrammar/undeclaredProductions/xsiTypeStrict-03.xml");
        FileStream inputStream = new FileStream(url.LocalPath, FileMode.Open);
        InputSource inputSource = new InputSource<Stream>(inputStream, url.ToString());

        encoder.encode(inputSource);
        inputStream.Close();

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

        Assert.AreEqual(6, n_events);

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
        Assert.AreEqual("unionedEnum", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);

        exiEvent = exiEventList[2];
        Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
        Assert.AreEqual("type", exiEvent.Name);
        Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
        Assert.AreEqual("int", ((EXIEventSchemaType)exiEvent).TypeName);
        Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_URI, ((EXIEventSchemaType)exiEvent).TypeURI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        Assert.AreEqual("type", eventType.name);
        Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);

        exiEvent = exiEventList[3];
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("12345", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        Assert.AreEqual(0, eventType.Index); // because of xsi:type
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);

        exiEvent = exiEventList[4];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);

        exiEvent = exiEventList[5];
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
      }
    }

    /// <summary>
    /// EXI interoperability test case "qname.invalid-02" 
    /// </summary>
    [Test]
    public virtual void testInvalidQName_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/exi/qname-invalid.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString =
        "<root QName='xsi:type' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:xsd='http://www.w3.org/2001/XMLSchema'>\n" +
        "  <QName xmlns:test='http://example.org/test'>test:qname</QName>\n" +
        "  <QName xsi:type='xsd:QName'>test:qname</QName>\n" +
        "  <QName xmlns:test='http://example.org/test' xsi:type='test:QName'>test:qname</QName>\n" +
        "</root>";

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveWhitespaces in new bool[] { true, false }) {
          Transmogrifier encoder = new Transmogrifier();
          EXIDecoder decoder = new EXIDecoder();
          Scanner scanner;

          encoder.AlignmentType = alignment;
          decoder.AlignmentType = alignment;

          encoder.GrammarCache = grammarCache;
          encoder.PreserveWhitespaces = preserveWhitespaces;
          byte[] bts;
          MemoryStream baos;

          baos = new MemoryStream();
          encoder.OutputStream = baos;

          encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

          bts = baos.ToArray();

          EXISchemaFactoryTestUtil.serializeBytes(bts, "/exi/qname-invalid.xsc", "qname-invalid-00.xml.exi", this);

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
          Assert.AreEqual(1, eventTypeList.Length);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("root", eventType.name);
          Assert.AreEqual("", eventType.uri);
          eventTypeList = eventType.EventTypeList;
          Assert.IsNull(eventTypeList.EE);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
          Assert.AreEqual("QName", exiEvent.Name);
          Assert.AreEqual("", exiEvent.URI);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
          Assert.AreEqual("QName", eventType.name);
          Assert.AreEqual("", eventType.uri);
          Assert.AreEqual(2, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(11, eventTypeList.Length);
          eventType = eventTypeList.item(0);
          Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("QName", eventType.name);
          Assert.AreEqual("", eventType.uri);
          eventType = eventTypeList.item(4);
          Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
          eventType = eventTypeList.item(5);
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          eventType = eventTypeList.item(6);
          Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(7);
          Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
          Assert.AreEqual("QName", eventType.name);
          Assert.AreEqual("", eventType.uri);
          eventType = eventTypeList.item(8);
          Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
          eventType = eventTypeList.item(9);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(10);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          ++n_events;

          if (preserveWhitespaces) {
            exiEvent = scanner.nextEvent(); // whitespaces
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            ++n_events;
          }
          exiEvent = scanner.nextEvent(); // <QName>
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
          Assert.AreEqual("test:qname", exiEvent.Characters.makeString());
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
          ++n_events;

          exiEvent = scanner.nextEvent(); // </QName>
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          ++n_events;
          if (preserveWhitespaces) {
            exiEvent = scanner.nextEvent(); // whitespaces
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            ++n_events;
          }
          exiEvent = scanner.nextEvent(); // <QName>
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
          Assert.AreEqual("type", exiEvent.Name);
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
          Assert.AreEqual("QName", ((EXIEventSchemaType)exiEvent).TypeName);
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_URI, ((EXIEventSchemaType)exiEvent).TypeURI);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
          Assert.AreEqual("type", eventType.name);
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(8, eventTypeList.Length);
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
          Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
          Assert.AreEqual("test:qname", exiEvent.Characters.makeString());
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
          ++n_events;

          exiEvent = scanner.nextEvent(); // </QName>
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          ++n_events;
          if (preserveWhitespaces) {
            exiEvent = scanner.nextEvent(); // whitespaces
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            ++n_events;
          }
          exiEvent = scanner.nextEvent(); // <QName>
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
          Assert.AreEqual("type", exiEvent.Name);
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
          Assert.AreEqual("QName", ((EXIEventSchemaType)exiEvent).TypeName);
          Assert.AreEqual("http://example.org/test", ((EXIEventSchemaType)exiEvent).TypeURI);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
          Assert.AreEqual("type", eventType.name);
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(8, eventTypeList.Length);
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
          Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
          Assert.AreEqual("test:qname", exiEvent.Characters.makeString());
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
          ++n_events;

          exiEvent = scanner.nextEvent(); // </QName>
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          ++n_events;
          if (preserveWhitespaces) {
            exiEvent = scanner.nextEvent(); // whitespaces
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            ++n_events;
          }

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(2, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(5, eventTypeList.Length);
          eventType = eventTypeList.item(0);
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("QName", eventType.name);
          Assert.AreEqual("", eventType.uri);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(4);
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

          Assert.AreEqual(preserveWhitespaces ? 20 : 16, n_events);
        }
      }
    }

  }

}