using System;
using System.IO;
using NUnit.Framework;

using Org.System.Xml.Sax;

using EXIDecoder = Nagasena.Proc.EXIDecoder;
using AlignmentType = Nagasena.Proc.Common.AlignmentType;
using EXIOptionsException = Nagasena.Proc.Common.EXIOptionsException;
using EventCode = Nagasena.Proc.Common.EventCode;
using EventType = Nagasena.Proc.Common.EventType;
using EventTypeList = Nagasena.Proc.Common.EventTypeList;
using EventDescription = Nagasena.Proc.Common.EventDescription;
using EventDescription_Fields = Nagasena.Proc.Common.EventDescription_Fields;
using GrammarOptions = Nagasena.Proc.Common.GrammarOptions;
using StringTable = Nagasena.Proc.Common.StringTable;
using XmlUriConst = Nagasena.Proc.Common.XmlUriConst;
using EXIEventNS = Nagasena.Proc.Events.EXIEventNS;
using EXIEventSchemaNil = Nagasena.Proc.Events.EXIEventSchemaNil;
using EXIEventSchemaType = Nagasena.Proc.Events.EXIEventSchemaType;
using GrammarCache = Nagasena.Proc.Grammars.GrammarCache;
using Scanner = Nagasena.Proc.IO.Scanner;
using EXISchema = Nagasena.Schema.EXISchema;
using TestBase = Nagasena.Schema.TestBase;

using Docbook43Schema = Nagasena.Scomp.Docbook43Schema;
using EXISchemaFactoryTestUtil = Nagasena.Scomp.EXISchemaFactoryTestUtil;

namespace Nagasena.Sax {

  [TestFixture]
  [Category("Enable_Compression")]
  public class GrammarSchemaInformedTest : TestBase {

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

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString =
        "<A xmlns='urn:foo'>\n" +
        "  <AB> </AB>\t<AC> </AC><AC/><AD> </AD><AE> </AE>\n" +
        "</A>\n";

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

          byte[] bts;
          int n_events;

          encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

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

          if (preserveWhitespaces) {
            exiEvent = scanner.nextEvent();
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
            ++n_events;
          }

          exiEvent = scanner.nextEvent();
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
          ++n_events;

          exiEvent = scanner.nextEvent();
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

          if (preserveWhitespaces) {
            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual("\t", exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
            Assert.AreEqual(4, eventType.Index);
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
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            ++n_events;
          }

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
          Assert.AreEqual(5, eventTypeList.Length);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("AD", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
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
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          Assert.AreEqual(3, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(8, eventTypeList.Length);
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
          Assert.AreEqual(4, eventTypeList.Length);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
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
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("AE", exiEvent.Name);
          Assert.AreEqual("urn:foo", exiEvent.URI);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("AE", eventType.name);
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
          ++n_events;

          exiEvent = scanner.nextEvent();
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

          if (preserveWhitespaces) {
            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual("\n", exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
            Assert.AreEqual(2, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(3, eventTypeList.Length);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            ++n_events;
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
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreSame(exiEvent, eventType);
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(1, eventTypeList.Length);
          ++n_events;

          Assert.AreEqual(preserveWhitespaces ? 21 : 18, n_events);
        }
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
    /// The 3rd <AC/> is not expected.
    /// <A>
    ///   <AB/><AC/><AC/><AC/><AD/>
    /// </A>
    /// </summary>
    [Test]
    public virtual void testAcceptanceForA_01_with_UndeclaredElement_1() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString =
        "<A xmlns='urn:foo'>" +
          "<AB> </AB><AC> </AC><AC/><AC> </AC><AD> </AD>" +
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

        // events at position 0...9 are the same as testAcceptanceForA_01
        for (n_events = 0; n_events < 10; n_events++) {
          scanner.nextEvent();
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AC", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(4, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AD", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
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
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
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
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(4, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AE", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
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

        Assert.AreEqual(18, n_events);
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
    /// The 1st <AC/> is not expected.
    /// <A>
    ///   <AC/><AB/><AD/>
    /// </A>
    /// </summary>
    [Test]
    public virtual void testAcceptanceForA_01_with_UndeclaredElement_2() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString =
        "<A xmlns='urn:foo'>" +
          "<AC/><AB/><AD/>" +
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
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AC", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        Assert.AreEqual(6, eventType.Index);
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
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(8, eventTypeList.Length);
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
        Assert.AreEqual(4, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(8, eventTypeList.Length);
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
        ++n_events;

        exiEvent = scanner.nextEvent();
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
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(8, eventTypeList.Length);
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
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(4, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AE", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
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

        Assert.AreEqual(10, n_events);
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

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString =
        "<A xmlns='urn:foo'>" +
          "<AB> </AB><AD> </AD>" +
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
        ++n_events;

        exiEvent = scanner.nextEvent();
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
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
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
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(4, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AE", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
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

        Assert.AreEqual(10, n_events);
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

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString =
        "<B xmlns='urn:foo'>" +
          "<AB> </AB><AC> </AC><AC/><AD> </AD>" +
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
        ++n_events;

        exiEvent = scanner.nextEvent();
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
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual(" ", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
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
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(8, eventTypeList.Length);
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
        Assert.AreEqual(4, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual(" ", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
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

        Assert.AreEqual(15, n_events);
      }
    }

    /// <summary>
    /// Schema: 
    /// <xsd:element name="nillable_B" type="foo:B" nillable="true" />
    /// 
    /// Instance:
    /// The element <AB/> is unexpected.
    /// <nillable_B xmlns='urn:foo' xsi:nil='true' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>
    ///   <AB/>
    /// </nillable_B>
    /// </summary>
    [Test]
    public virtual void testAcceptanceForNillableB_with_UndeclaredElement() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString =
        "<nillable_B xmlns='urn:foo' xsi:nil='true' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>" +
          "<AB/>" +
        "</nillable_B>\n";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        byte[] bts;
        MemoryStream baos;

        baos = new MemoryStream();
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
        Assert.AreEqual("nillable_B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
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
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AB", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        Assert.AreEqual(3, eventType.Index);
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
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(8, eventTypeList.Length);
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

        Assert.AreEqual(7, n_events);
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
    /// <C>
    ///   <AB/><AC/>
    /// </C>
    /// </summary>
    [Test]
    public virtual void testAcceptanceForC_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString =
        "<C xmlns='urn:foo'>" +
          "<AB> </AB>\t<AC> </AC>\n" +
        "</C>\n";

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
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("C", eventType.name);
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
          Assert.AreEqual(2, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(9, eventTypeList.Length);
          eventType = eventTypeList.item(0);
          Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
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

          if (preserveWhitespaces) {
            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual("\t", exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
            Assert.AreEqual(4, eventType.Index);
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
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            ++n_events;
          }

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("AC", exiEvent.Name);
          Assert.AreEqual("urn:foo", exiEvent.URI);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("AC", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
          Assert.AreEqual(1, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(5, eventTypeList.Length);
          eventType = eventTypeList.item(0);
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("AB", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
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

          if (preserveWhitespaces) {
            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual("\n", exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
            Assert.AreEqual(4, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(5, eventTypeList.Length);
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
            ++n_events;
          }

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(2, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(5, eventTypeList.Length);
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
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreSame(exiEvent, eventType);
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(1, eventTypeList.Length);
          ++n_events;

          Assert.AreEqual(preserveWhitespaces ? 12 : 10, n_events);
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
    /// <C>
    ///   <AC/><AB/><!-- reverse order -->  
    /// </C>
    /// where C has "all" group that consists of AC and AB.
    /// </summary>
    [Test]
    public virtual void testAcceptanceForC_02() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString =
        "<C xmlns='urn:foo'>" +
          "<AC> </AC><AB> </AB>" +
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
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("C", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AC", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AC", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(3, eventType.Index);
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
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AB", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
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
        ++n_events;

        exiEvent = scanner.nextEvent();
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
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
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
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        ++n_events;

        Assert.AreEqual(10, n_events);
      }
    }

    /// <summary>
    /// Schema:
    /// <xsd:element name="AB" type="xsd:anySimpleType"/>
    /// 
    /// Instance:
    /// The element <AC/> is not expected.
    /// <AB><AC/>abc</AB>
    /// </summary>
    [Test]
    public virtual void testAcceptanceForAB_with_UndeclaredElement_1() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString = "<AB xmlns='urn:foo'><AC/>abc</AB>\n";

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
        Assert.AreEqual("AB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AC", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        Assert.AreEqual(6, eventType.Index);
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
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(8, eventTypeList.Length);
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
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("abc", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
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

        Assert.AreEqual(7, n_events);
      }
    }

    /// <summary>
    /// Schema:
    /// <xsd:element name="AB" type="xsd:anySimpleType"/>
    /// 
    /// Instance:
    /// The element <AC/> is not expected.
    /// <AB><AC/>abc</AB>
    /// </summary>
    [Test]
    public virtual void testAcceptanceForAB_with_UndeclaredElement_2() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString =
        "<AB xmlns='urn:foo'>abc<AC/>\n" +
        "</AB>\n";

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
          Assert.AreEqual("AB", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
          eventTypeList = eventType.EventTypeList;
          Assert.IsNull(eventTypeList.EE);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
          Assert.AreEqual("abc", exiEvent.Characters.makeString());
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
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("AC", exiEvent.Name);
          Assert.AreEqual("urn:foo", exiEvent.URI);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          Assert.AreEqual(1, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(3, eventTypeList.Length);
          eventType = eventTypeList.item(0);
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          Assert.AreEqual(3, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(8, eventTypeList.Length);
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
          ++n_events;

          if (preserveWhitespaces) {
            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual("\n", exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
            Assert.AreEqual(2, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(3, eventTypeList.Length);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            ++n_events;
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
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreSame(exiEvent, eventType);
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(1, eventTypeList.Length);
          ++n_events;

          Assert.AreEqual(preserveWhitespaces ? 8 : 7, n_events);
        }
      }
    }

    /// <summary>
    /// Schema:
    /// <xsd:complexType name="B">
    ///   <xsd:sequence>
    ///     <xsd:element ref="foo:AB"/>
    ///     <xsd:element ref="foo:AC" minOccurs="0" maxOccurs="2"/>
    ///     <xsd:element ref="foo:AD" minOccurs="0"/>
    ///   </xsd:sequence>
    /// </xsd:complexType>
    /// 
    /// <xsd:element name="B" type="foo:B"/>
    /// 
    /// Instance:
    /// <B xsi:nil="true" xmlns="urn:foo">
    /// </B>
    /// 
    /// Use of xsi:nil is permitted in default mode, even though it would
    /// not have been permitted in strict mode. 
    /// </summary>
    [Test]
    public virtual void testLenientXsiNil_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache;

      grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString =
        "<B xsi:nil='true' xmlns='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>" +
        "</B>\n";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier _encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        _encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        _encoder.GrammarCache = grammarCache;
        MemoryStream baos = new MemoryStream();
        _encoder.OutputStream = baos;

        _encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        byte[] bts = baos.ToArray();

        decoder.GrammarCache = grammarCache;
        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

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
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_NL, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
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
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
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
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
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

        Assert.AreEqual(5, n_events);
      }

      Transmogrifier encoder = new Transmogrifier();
      encoder.GrammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      foreach (AlignmentType alignment in Alignments) {

        encoder.AlignmentType = alignment;

        bool caught;

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
    /// <xsd:simpleType name="finalString" final="#all">
    ///   <xsd:restriction base="xsd:string" />
    /// </xsd:simpleType>
    /// 
    /// <xsd:element name="K" type="foo:finalString"/>
    /// 
    /// Instance:
    /// <K xsi:nil="true" xmlns="urn:foo"/>
    /// 
    /// Use of xsi:nil is permitted in default mode, even though it would
    /// not have been permitted in strict mode. 
    /// </summary>
    [Test]
    public virtual void testLenientXsiNil_02() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache;

      grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString =
        "<K xsi:nil='true' xmlns='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'/>";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier _encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        _encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        _encoder.GrammarCache = grammarCache;
        MemoryStream baos = new MemoryStream();
        _encoder.OutputStream = baos;

        _encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        byte[] bts = baos.ToArray();

        decoder.GrammarCache = grammarCache;
        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

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
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("K", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_NL, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(8, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
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
        Assert.AreEqual(0, eventType.Index);
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
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
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

        Assert.AreEqual(5, n_events);
      }

      Transmogrifier encoder = new Transmogrifier();
      encoder.GrammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      foreach (AlignmentType alignment in Alignments) {

        encoder.AlignmentType = alignment;

        encoder.OutputStream = new MemoryStream();

        bool caught = false;
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
    /// <xsd:simpleType name="finalString" final="#all">
    ///   <xsd:restriction base="xsd:string" />
    /// </xsd:simpleType>
    /// 
    /// <xsd:element name="K" type="foo:finalString"/>
    /// 
    /// Instance:
    /// <K xsi:nil="troo" xmlns="urn:foo"/>
    /// 
    /// Use of xsi:nil (even with invalid value) is permitted in default mode. 
    /// </summary>
    [Test]
    public virtual void testLenientXsiNil_03() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache;

      grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString = 
        "<K xsi:nil='troo' xmlns='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'/>";

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        byte[] bts = baos.ToArray();

        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

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
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("K", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        Assert.AreEqual(5, eventType.Index);
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
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(8, eventTypeList.Length);
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

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString =
        "<foo:D xmlns:foo='urn:foo'>" +
          "<A/><A/><B/><C/><D/>\t<B/><E/><F/>" +
        "</foo:D>\n";

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveWhitespaces in new bool[] { true, false }) {
          Scanner scanner;

          encoder.AlignmentType = alignment;
          decoder.AlignmentType = alignment;

          encoder.PreserveWhitespaces = preserveWhitespaces;
          MemoryStream baos = new MemoryStream();
          encoder.OutputStream = baos;

          encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

          byte[] bts = baos.ToArray();

          decoder.InputStream = new MemoryStream(bts);
          scanner = decoder.processHeader();

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
          Assert.IsNull(eventTypeList.EE);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("D", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
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
          Assert.AreEqual(2, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(9, eventTypeList.Length);
          eventType = eventTypeList.item(0);
          Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("B", eventType.name);
          Assert.AreEqual("", eventType.uri);
          eventType = eventTypeList.item(4);
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
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
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(4, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(10, eventTypeList.Length);
          Assert.IsNotNull(eventTypeList.EE);
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
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(9);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
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
          Assert.AreEqual(5, eventTypeList.Length);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("B", eventType.name);
          Assert.AreEqual("", eventType.uri);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(4, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(10, eventTypeList.Length);
          Assert.IsNotNull(eventTypeList.EE);
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
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(9);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
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
          Assert.AreEqual(4, eventTypeList.Length);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(4, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(10, eventTypeList.Length);
          Assert.IsNotNull(eventTypeList.EE);
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
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(9);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
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
          Assert.AreEqual(8, eventTypeList.Length);
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
          eventType = eventTypeList.item(6);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(7);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(4, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(10, eventTypeList.Length);
          Assert.IsNotNull(eventTypeList.EE);
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
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(9);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("D", exiEvent.Name);
          Assert.AreEqual("", exiEvent.URI);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("D", eventType.name);
          Assert.AreEqual("", eventType.uri);
          Assert.AreEqual(1, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(7, eventTypeList.Length);
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
          eventType = eventTypeList.item(5);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(6);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(4, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(10, eventTypeList.Length);
          Assert.IsNotNull(eventTypeList.EE);
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
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(9);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          ++n_events;

          if (preserveWhitespaces) {
            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual("\t", exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
            Assert.AreEqual(5, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(6, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("B", eventType.name);
            Assert.AreEqual("", eventType.uri);
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
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            ++n_events;
          }

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
          Assert.AreEqual(6, eventTypeList.Length);
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
          eventType = eventTypeList.item(4);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(5);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(4, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(10, eventTypeList.Length);
          Assert.IsNotNull(eventTypeList.EE);
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
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(9);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("E", exiEvent.Name);
          Assert.AreEqual("", exiEvent.URI);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("E", eventType.name);
          Assert.AreEqual("", eventType.uri);
          Assert.AreEqual(2, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(7, eventTypeList.Length);
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
          eventType = eventTypeList.item(5);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(6);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(4, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(10, eventTypeList.Length);
          Assert.IsNotNull(eventTypeList.EE);
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
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(9);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("F", exiEvent.Name);
          Assert.AreEqual("", exiEvent.URI);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("F", eventType.name);
          Assert.AreEqual("", eventType.uri);
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(4, eventTypeList.Length);
          Assert.IsNotNull(eventTypeList.EE);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(4, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(10, eventTypeList.Length);
          Assert.IsNotNull(eventTypeList.EE);
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
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(9);
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

          Assert.AreEqual(preserveWhitespaces ? 21 : 20, n_events);
        }
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

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString =
        "<foo:E xmlns:foo='urn:foo'>" +
          "<A/><A/><B/><C/><B/><G/><H/>" +
        "</foo:E>\n";

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        byte[] bts = baos.ToArray();

        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

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
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("E", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
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
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(12, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("D", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("G", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("H", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(10);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(11);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(4, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(10, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
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
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
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
        Assert.AreEqual(5, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        Assert.AreSame(eventType, eventTypeList.EE);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(4);
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
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
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
        Assert.AreEqual(4, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        Assert.AreSame(eventType, eventTypeList.EE);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(3);
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
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
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
        Assert.AreEqual(8, eventTypeList.Length);
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
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(7);
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
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
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
        Assert.AreEqual(7, eventTypeList.Length);
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
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(6);
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
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("G", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("G", eventType.name);
        Assert.AreEqual("", eventType.uri);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(6, eventTypeList.Length);
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
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(5);
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
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("H", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("H", eventType.name);
        Assert.AreEqual("", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(4, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(3);
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
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
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

        Assert.AreEqual(18, n_events);
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

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      string xmlString;

      MemoryStream baos;
      byte[] bts;

      EventDescription exiEvent;
      int n_events;

      EventType eventType;
      EventTypeList eventTypeList;

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        xmlString =
          "<foo:F xmlns:foo='urn:foo' foo:aA='' foo:aB=''>" +
            "<foo:AB>xyz</foo:AB>" +
          "</foo:F>\n";

        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        baos = new MemoryStream();
        encoder.OutputStream = baos;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

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
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("aA", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
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
        Assert.AreEqual(8, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("aB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
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
        Assert.AreEqual(6, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
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
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("xyz", exiEvent.Characters.makeString());
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

        Assert.AreEqual(9, n_events);
      }

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        xmlString = "<foo:F xmlns:foo='urn:foo' foo:aA='' foo:aB=''>" + "</foo:F>\n";

        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        baos = new MemoryStream();
        encoder.OutputStream = baos;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

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
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("aA", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
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
        Assert.AreEqual(8, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("aB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
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
        Assert.AreEqual(6, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AB", eventType.name);
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

        Assert.AreEqual(6, n_events);
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

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      string xmlString;

      MemoryStream baos;
      byte[] bts;

      EventDescription exiEvent;
      int n_events;

      EventType eventType;
      EventTypeList eventTypeList;

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        xmlString =
          "<foo:F xmlns:foo='urn:foo' foo:aA='' foo:aB='' xsi:nil='true' \n" +
          "       xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>" +
          "</foo:F>\n";

        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        baos = new MemoryStream();
        encoder.OutputStream = baos;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

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
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(9, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("aA", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("aA", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
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
        Assert.AreEqual(7, eventTypeList.Length);
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
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("aA", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
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
        Assert.AreEqual(7, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("aB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
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

        Assert.AreEqual(7, n_events);
      }

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        xmlString = "<foo:F xmlns:foo='urn:foo' foo:aA='' foo:aB='' xsi:nil='true' \n" + "       xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>" + "<foo:AB/>" + "</foo:F>\n";

        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        baos = new MemoryStream();
        encoder.OutputStream = baos;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

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
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(9, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("aA", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("aA", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
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
        Assert.AreEqual(7, eventTypeList.Length);
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
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("aA", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
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
        Assert.AreEqual(7, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("aB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AB", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
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
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(8, eventTypeList.Length);
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

        Assert.AreEqual(9, n_events);
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
    public virtual void testAcceptanceForNilledF_with_UndeclaredElement() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString =
        "<foo:F xmlns:foo='urn:foo' foo:aA='' foo:aB='' xsi:nil='true' \n" +
        "       xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>" +
          "<foo:AB/>" +
        "</foo:F>\n";

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        MemoryStream baos;

        baos = new MemoryStream();
        encoder.OutputStream = baos;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        byte[] bts = baos.ToArray();

        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

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
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(9, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("aA", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("aA", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
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
        Assert.AreEqual(7, eventTypeList.Length);
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
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("aA", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
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
        Assert.AreEqual(7, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("aB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AB", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(8, eventTypeList.Length);
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

        Assert.AreEqual(9, n_events);
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

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      string xmlString;

      MemoryStream baos;

      byte[] bts;
      int n_events;

      EventDescription exiEvent;

      EventType eventType;
      EventTypeList eventTypeList;

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        xmlString =
          "<foo:G xmlns:foo='urn:foo' foo:aA='' foo:aB=''>" +
            "<foo:AB>xyz</foo:AB>" +
          "</foo:G>\n";

        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        baos = new MemoryStream();
        encoder.OutputStream = baos;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

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
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("aA", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
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
        Assert.AreEqual(8, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("aB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
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
        Assert.AreEqual(6, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
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
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("xyz", exiEvent.Characters.makeString());
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

        Assert.AreEqual(9, n_events);
      }

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        xmlString = "<foo:G xmlns:foo='urn:foo' foo:aA='' foo:aB=''>" + "</foo:G>\n";

        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        baos = new MemoryStream();
        encoder.OutputStream = baos;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

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
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("aA", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
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
        Assert.AreEqual(8, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("aB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
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
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(6, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AB", eventType.name);
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

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString =
        "<foo:H xmlns:foo='urn:foo' xmlns:goo='urn:goo'>" +
          "<goo:AB> </goo:AB>" +
          "<B/>" +
          "<goo:AB></goo:AB>" +
        "</foo:H>\n";

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        byte[] bts = baos.ToArray();

        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

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
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("H", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AB", exiEvent.Name);
        Assert.AreEqual("urn:goo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:goo", eventType.uri);
        Assert.AreEqual(4, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(10, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:eoo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(6);
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
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(3);
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
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AB", exiEvent.Name);
        Assert.AreEqual("urn:goo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
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
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(8, eventTypeList.Length);
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

        Assert.AreEqual(11, n_events);
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
    /// </summary>
    [Test]
    public virtual void testAcceptanceForH2_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString =
        "<foo:H2 xmlns:foo='urn:foo' xmlns:goo='urn:goo'>" +
          "<goo:AB> </goo:AB>\n" + // <xsd:any namespace="##other" minOccurs="0" />
          "<foo:AB> </foo:AB>\n" + // <xsd:any namespace="##targetNamespace ##local" minOccurs="0" />
        "</foo:H2>\n";

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveWhitespaces in new bool[] { true, false }) {
          Scanner scanner;

          encoder.AlignmentType = alignment;
          decoder.AlignmentType = alignment;

          encoder.PreserveWhitespaces = preserveWhitespaces;
          MemoryStream baos = new MemoryStream();
          encoder.OutputStream = baos;

          byte[] bts;
          int n_events;

          encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

          bts = baos.ToArray();

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
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("H2", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
          eventTypeList = eventType.EventTypeList;
          Assert.IsNull(eventTypeList.EE);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("AB", exiEvent.Name);
          Assert.AreEqual("urn:goo", exiEvent.URI);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
          Assert.AreEqual(4, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(10, eventTypeList.Length);
          eventType = eventTypeList.item(0);
          Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
          Assert.AreEqual("urn:foo", eventType.uri);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
          Assert.AreEqual("", eventType.uri);
          eventType = eventTypeList.item(5);
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          eventType = eventTypeList.item(6);
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

          if (preserveWhitespaces) {
            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual("\n", exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
            Assert.AreEqual(4, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(5, eventTypeList.Length);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
            Assert.AreEqual("urn:foo", eventType.uri);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
            Assert.AreEqual("", eventType.uri);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            ++n_events;
          }

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("AB", exiEvent.Name);
          Assert.AreEqual("urn:foo", exiEvent.URI);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
          Assert.AreEqual("urn:foo", eventType.uri);
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(5, eventTypeList.Length);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
          Assert.AreEqual("", eventType.uri);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
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

          if (preserveWhitespaces) {
            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual("\n", exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
            Assert.AreEqual(2, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(3, eventTypeList.Length);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            ++n_events;
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
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreSame(exiEvent, eventType);
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(1, eventTypeList.Length);
          ++n_events;

          Assert.AreEqual(preserveWhitespaces ? 12 : 10, n_events);
        }
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
    public virtual void testAcceptanceForH2_01_with_UndeclaredElement() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString =
        "<foo:H2 xmlns:foo='urn:foo' xmlns:goo='urn:goo'>" +
          "<goo:AB/>" +
          "<goo:AB/>" + // unexpected
        "</foo:H2>\n";

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        byte[] bts;
        int n_events;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

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
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("H2", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AB", exiEvent.Name);
        Assert.AreEqual("urn:goo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        Assert.AreEqual(4, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(10, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(6);
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
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(8, eventTypeList.Length);
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
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AB", exiEvent.Name);
        Assert.AreEqual("urn:goo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(3, eventType.Index);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(8, eventTypeList.Length);
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
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("", eventType.uri);
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

        Assert.AreEqual(8, n_events);
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
    /// </summary>
    [Test]
    public virtual void testAcceptanceForH2_02() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString =
        "<foo:H2 xmlns:foo='urn:foo' xmlns:goo='urn:goo'>" +
          "<goo:AB> </goo:AB>" +
          "<AB> </AB>" +
        "</foo:H2>\n";

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        byte[] bts;
        int n_events;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

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
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("H2", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AB", exiEvent.Name);
        Assert.AreEqual("urn:goo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        Assert.AreEqual(4, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(10, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(6);
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
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AB", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("", eventType.uri);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
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

        Assert.AreEqual(10, n_events);
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
    /// After an occurrence of "##other", "##local" wildcard can come only once, 
    /// but not twice.
    /// </summary>
    [Test]
    public virtual void testAcceptanceForH2_02_with_UndeclaredElement() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString =
        "<foo:H2 xmlns:foo='urn:foo' xmlns:goo='urn:goo'>" +
          "<goo:AB/>" +
          "<AB/>" +
          "<AB/>" + // unexpected
        "</foo:H2>\n";

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        byte[] bts;
        int n_events;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

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
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("H2", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AB", exiEvent.Name);
        Assert.AreEqual("urn:goo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        Assert.AreEqual(4, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(10, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(6);
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
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(8, eventTypeList.Length);
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
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AB", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("", eventType.uri);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(8, eventTypeList.Length);
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
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AB", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(8, eventTypeList.Length);
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

        Assert.AreEqual(10, n_events);
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
    /// </summary>
    [Test]
    public virtual void testAcceptanceForH2_03() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString =
        "<foo:H2 xmlns:foo='urn:foo' xmlns:goo='urn:goo'>" +
          "<foo:AB> </foo:AB>" +
        "</foo:H2>\n";

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        byte[] bts;
        int n_events;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

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
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("H2", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AB", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(10, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(6);
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

        Assert.AreEqual(7, n_events);
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
    public virtual void testAcceptanceForH2_03_with_UndeclaredElement() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString =
        "<foo:H2 xmlns:foo='urn:foo' xmlns:goo='urn:goo'>" +
          "<foo:AB/>" +
          "<goo:AB/>" + // unexpected
        "</foo:H2>\n";

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        byte[] bts;
        int n_events;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

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
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("H2", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AB", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(10, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(6);
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
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(8, eventTypeList.Length);
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
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AB", exiEvent.Name);
        Assert.AreEqual("urn:goo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(8, eventTypeList.Length);
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

        Assert.AreEqual(8, n_events);
      }
    }

    /// <summary>
    /// Schema:
    /// <xsd:element name="H2">
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

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      string xmlString;

      MemoryStream baos;

      byte[] bts;
      int n_events;

      EventDescription exiEvent;

      EventType eventType;
      EventTypeList eventTypeList;

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        xmlString =
          "<foo:H2 xmlns:foo='urn:foo' xmlns:goo='urn:goo'>" +
            "<goo:AB> </goo:AB>" +
          "</foo:H2>\n";

        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        baos = new MemoryStream();
        encoder.OutputStream = baos;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

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
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("H2", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AB", exiEvent.Name);
        Assert.AreEqual("urn:goo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        Assert.AreEqual(4, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(10, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(6);
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
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("", eventType.uri);
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

        Assert.AreEqual(7, n_events);
      }

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        xmlString = "<foo:H2 xmlns:foo='urn:foo' xmlns:goo='urn:goo'>" + "<goo:AB/>" + "xyz" + "</foo:H2>\n"; // unexpected

        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        baos = new MemoryStream();
        encoder.OutputStream = baos;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

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
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("H2", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AB", exiEvent.Name);
        Assert.AreEqual("urn:goo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        Assert.AreEqual(4, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(10, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(6);
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
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(8, eventTypeList.Length);
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
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("xyz", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        Assert.AreEqual(4, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        ++n_events;

        Assert.AreEqual(7, n_events);
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

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString =
        "<foo:H3 xmlns:foo='urn:foo' xmlns:goo='urn:goo' xmlns:hoo='urn:hoo' xmlns:ioo='urn:ioo'>" +
          "<foo:AB> </foo:AB>" + // <xsd:any namespace="##local ##targetNamespace" minOccurs="0" maxOccurs="2"/>
          "<AB> </AB>" + // same as above
          "<hoo:AC> </hoo:AC>" + // <xsd:element ref="hoo:AC" minOccurs="0"/>
          "<goo:AB> </goo:AB>\n" + // <xsd:any namespace="urn:goo" minOccurs="0" />
          "<hoo:AB> </hoo:AB>" + // <xsd:element ref="hoo:AB" minOccurs="0"/>
          "<ioo:AB> </ioo:AB>" + // <xsd:any namespace="urn:ioo" minOccurs="0" />
        "</foo:H3>\n";

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveWhitespaces in new bool[] { true, false }) {
          Scanner scanner;

          encoder.AlignmentType = alignment;
          decoder.AlignmentType = alignment;

          encoder.PreserveWhitespaces = preserveWhitespaces;
          MemoryStream baos = new MemoryStream();
          encoder.OutputStream = baos;

          byte[] bts;
          int n_events;

          encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

          bts = baos.ToArray();

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
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("H3", exiEvent.Name);
          Assert.AreEqual("urn:foo", exiEvent.URI);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("H3", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
          eventTypeList = eventType.EventTypeList;
          Assert.IsNull(eventTypeList.EE);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("AB", exiEvent.Name);
          Assert.AreEqual("urn:foo", exiEvent.URI);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
          Assert.AreEqual("urn:foo", eventType.uri);
          Assert.AreEqual(5, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(13, eventTypeList.Length);
          eventType = eventTypeList.item(0);
          Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("AC", eventType.name);
          Assert.AreEqual("urn:hoo", eventType.uri);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("AB", eventType.name);
          Assert.AreEqual("urn:hoo", eventType.uri);
          eventType = eventTypeList.item(4);
          Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
          Assert.AreEqual("", eventType.uri);
          eventType = eventTypeList.item(6);
          Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
          Assert.AreEqual("urn:goo", eventType.uri);
          eventType = eventTypeList.item(7);
          Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
          Assert.AreEqual("urn:ioo", eventType.uri);
          eventType = eventTypeList.item(8);
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          eventType = eventTypeList.item(9);
          Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(10);
          Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
          eventType = eventTypeList.item(11);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(12);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
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
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("AB", exiEvent.Name);
          Assert.AreEqual("", exiEvent.URI);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
          Assert.AreEqual("", eventType.uri);
          Assert.AreEqual(2, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(9, eventTypeList.Length);
          eventType = eventTypeList.item(0);
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("AC", eventType.name);
          Assert.AreEqual("urn:hoo", eventType.uri);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("AB", eventType.name);
          Assert.AreEqual("urn:hoo", eventType.uri);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
          Assert.AreEqual("urn:foo", eventType.uri);
          eventType = eventTypeList.item(4);
          Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
          Assert.AreEqual("urn:goo", eventType.uri);
          eventType = eventTypeList.item(5);
          Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
          Assert.AreEqual("urn:ioo", eventType.uri);
          eventType = eventTypeList.item(6);
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          eventType = eventTypeList.item(7);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(8);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
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
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("AC", exiEvent.Name);
          Assert.AreEqual("urn:hoo", exiEvent.URI);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("AC", eventType.name);
          Assert.AreEqual("urn:hoo", eventType.uri);
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(7, eventTypeList.Length);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("AB", eventType.name);
          Assert.AreEqual("urn:hoo", eventType.uri);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
          Assert.AreEqual("urn:goo", eventType.uri);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
          Assert.AreEqual("urn:ioo", eventType.uri);
          eventType = eventTypeList.item(4);
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          eventType = eventTypeList.item(5);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(6);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
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
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("AB", exiEvent.Name);
          Assert.AreEqual("urn:goo", exiEvent.URI);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
          Assert.AreEqual("urn:goo", eventType.uri);
          Assert.AreEqual(1, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(6, eventTypeList.Length);
          eventType = eventTypeList.item(0);
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("AB", eventType.name);
          Assert.AreEqual("urn:hoo", eventType.uri);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
          Assert.AreEqual("urn:ioo", eventType.uri);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(5);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
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

          if (preserveWhitespaces) {
            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual("\n", exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
            Assert.AreEqual(4, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(5, eventTypeList.Length);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("AB", eventType.name);
            Assert.AreEqual("urn:hoo", eventType.uri);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
            Assert.AreEqual("urn:ioo", eventType.uri);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            ++n_events;
          }

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("AB", exiEvent.Name);
          Assert.AreEqual("urn:hoo", exiEvent.URI);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("AB", eventType.name);
          Assert.AreEqual("urn:hoo", eventType.uri);
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(5, eventTypeList.Length);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
          Assert.AreEqual("urn:ioo", eventType.uri);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
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
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("AB", exiEvent.Name);
          Assert.AreEqual("urn:ioo", exiEvent.URI);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
          Assert.AreEqual("urn:ioo", eventType.uri);
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(4, eventTypeList.Length);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
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

          Assert.AreEqual(preserveWhitespaces ? 23 : 22, n_events);
        }
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
    /// </summary>
    [Test]
    public virtual void testAcceptanceForH3_01_with_UndeclaredElement() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString =
        "<foo:H3 xmlns:foo='urn:foo' xmlns:goo='urn:goo' xmlns:hoo='urn:hoo' xmlns:ioo='urn:ioo'>" +
          "<foo:AB/>" + // <xsd:any namespace="##targetNamespace ##local" minOccurs="0" maxOccurs="2"/>
          "<hoo:AD/>" + // unexpected
        "</foo:H3>\n";

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        byte[] bts;
        int n_events;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

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
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("H3", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("H3", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AB", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(5, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(13, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AC", eventType.name);
        Assert.AreEqual("urn:hoo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AB", eventType.name);
        Assert.AreEqual("urn:hoo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:ioo", eventType.uri);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(10);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(11);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(12);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(8, eventTypeList.Length);
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
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AD", exiEvent.Name);
        Assert.AreEqual("urn:hoo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        Assert.AreEqual(7, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(9, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AC", eventType.name);
        Assert.AreEqual("urn:hoo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AB", eventType.name);
        Assert.AreEqual("urn:hoo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:ioo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(8, eventTypeList.Length);
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
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(6, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(9, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AC", eventType.name);
        Assert.AreEqual("urn:hoo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AB", eventType.name);
        Assert.AreEqual("urn:hoo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:ioo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
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

        Assert.AreEqual(8, n_events);
      }
    }

    /// <summary>
    /// OPENGIS schema and instance.
    /// There are nested groups in this example.
    /// </summary>
    [Test]
    public virtual void testOpenGisExample01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/opengis/openGis.xsc", this);

      Assert.AreEqual(9, corpus.uris.Length);
      Assert.AreEqual("", corpus.uris[0]);
      Assert.AreEqual(XmlUriConst.W3C_XML_1998_URI, corpus.uris[1]);
      Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, corpus.uris[2]);
      Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_URI, corpus.uris[3]);
      Assert.AreEqual("http://www.opengis.net/gml", corpus.uris[4]);
      Assert.AreEqual("http://www.opengis.net/ogc", corpus.uris[5]);
      Assert.AreEqual("http://www.opengis.net/wfs", corpus.uris[6]);
      Assert.AreEqual("http://www.ordnancesurvey.co.uk/xml/namespaces/osgb", corpus.uris[7]);
      Assert.AreEqual("urn:myhub", corpus.uris[8]);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      EventDescription exiEvent;

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

          Uri url = resolveSystemIdAsURL("/opengis/openGis.xml");
          FileStream inputStream = new FileStream(url.LocalPath, FileMode.Open);
          InputSource inputSource = new InputSource<Stream>(inputStream, url.ToString());

          encoder.encode(inputSource);
          inputStream.Close();

          byte[] bts = baos.ToArray();

          decoder.GrammarCache = grammarCache;
          decoder.InputStream = new MemoryStream(bts);
          scanner = decoder.processHeader();

          int n_events = 0;
          int n_undeclaredCharacters = 0;
          while ((exiEvent = scanner.nextEvent()) != null) {
            if (exiEvent.EventKind == EventDescription_Fields.EVENT_CH) {
              if (exiEvent.getEventType().itemType == EventType.ITEM_CH) {
                ++n_undeclaredCharacters;
                continue;
              }
            }
            ++n_events;
          }

          Assert.AreEqual(77, n_events);
          Assert.AreEqual(preserveWhitespaces ? 38 : 0, n_undeclaredCharacters);
        }
      }

      // Turn on preserve.prefixes
      grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

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

          Uri url = resolveSystemIdAsURL("/opengis/openGis.xml");
          FileStream inputStream = new FileStream(url.LocalPath, FileMode.Open);
          InputSource inputSource = new InputSource<Stream>(inputStream, url.ToString());

          encoder.encode(inputSource);
          inputStream.Close();

          byte[] bts = baos.ToArray();

          decoder.GrammarCache = grammarCache;
          decoder.InputStream = new MemoryStream(bts);
          scanner = decoder.processHeader();

          StringTable stringTable;
          stringTable = scanner.stringTable;
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            Assert.AreEqual(9, stringTable.n_uris);
          }

          int n_events = 0;
          int n_undeclaredCharacters = 0;
          while ((exiEvent = scanner.nextEvent()) != null) {
            if (exiEvent.EventKind == EventDescription_Fields.EVENT_CH) {
              if (exiEvent.getEventType().itemType == EventType.ITEM_CH) {
                ++n_undeclaredCharacters;
                continue;
              }
            }
            ++n_events;
            // Check to see if the right prefixe is used for each element and attribute. 
            short variety;
            if ((variety = exiEvent.EventKind) == EventDescription_Fields.EVENT_SE) {
              if ("http://www.opengis.net/gml".Equals(exiEvent.URI)) {
                Assert.AreEqual("gml", exiEvent.Prefix);
              }
              else if ("http://www.opengis.net/wfs".Equals(exiEvent.URI)) {
                Assert.AreEqual(null, exiEvent.Prefix);
              }
              else if ("http://www.ordnancesurvey.co.uk/xml/namespaces/osgb".Equals(exiEvent.URI)) {
                Assert.AreEqual("osgb", exiEvent.Prefix);
              }
              else {
                Assert.Fail();
              }
            }
            else if (variety == EventDescription_Fields.EVENT_AT) {
              if ("".Equals(exiEvent.URI)) {
                Assert.AreEqual("", exiEvent.Prefix);
              }
              else {
                Assert.Fail();
              }
            }
          }

          // There are 7 namespace declarations.
          Assert.AreEqual(84, n_events);
          Assert.AreEqual(preserveWhitespaces ? 38 : 0, n_undeclaredCharacters);

          stringTable = scanner.stringTable;
          Assert.AreEqual(11, stringTable.n_uris);
          /*
           * URI  0 "" [empty string]
           * URI  1 "http://www.w3.org/XML/1998/namespace"
           * URI  2 "http://www.w3.org/2001/XMLSchema-instance"
           * URI  3 "http://www.w3.org/2001/XMLSchema"
           * URI  4 "http://www.opengis.net/gml"
           * URI  5 "http://www.opengis.net/ogc"
           * URI  6 "http://www.opengis.net/wfs"
           * URI  7 "http://www.ordnancesurvey.co.uk/xml/namespaces/osgb"
           * URI  8 "urn:myhub"
           * URI  9 "http://www.census.gov/geo/tiger"
           * URI 10 "http://www.w3.org/1999/xlink"
           */
          // Inspect the URI partition
          Assert.AreEqual("", stringTable.getURI(0));
          Assert.AreEqual(XmlUriConst.W3C_XML_1998_URI, stringTable.getURI(1));
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, stringTable.getURI(2));
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_URI, stringTable.getURI(3));
          Assert.AreEqual("http://www.opengis.net/gml", stringTable.getURI(4));
          Assert.AreEqual("http://www.opengis.net/ogc", stringTable.getURI(5));
          Assert.AreEqual("http://www.opengis.net/wfs", stringTable.getURI(6));
          Assert.AreEqual("http://www.ordnancesurvey.co.uk/xml/namespaces/osgb", stringTable.getURI(7));
          Assert.AreEqual("urn:myhub", stringTable.getURI(8));
          Assert.AreEqual("http://www.census.gov/geo/tiger", stringTable.getURI(9));
          Assert.AreEqual("http://www.w3.org/1999/xlink", stringTable.getURI(10));
        }
      }
    }

    /// <summary>
    /// Docbook 4.3 schema and instance.
    /// </summary>
    [Test]
    public virtual void testDocbook43ExampleVerySimple01() {
      EXISchema corpus = Docbook43Schema.EXISchema;

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

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

          encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

          byte[] bts = baos.ToArray();

          decoder.GrammarCache = grammarCache;
          decoder.InputStream = new MemoryStream(bts);
          scanner = decoder.processHeader();

          EventDescription exiEvent;
          int n_events = 0;
          int n_undeclaredCharacters = 0;
          while ((exiEvent = scanner.nextEvent()) != null) {
            if (exiEvent.EventKind == EventDescription_Fields.EVENT_CH) {
              if (exiEvent.getEventType().itemType == EventType.ITEM_CH) {
                ++n_undeclaredCharacters;
                continue;
              }
            }
            ++n_events;
          }

          Assert.AreEqual(22, n_events);
          Assert.AreEqual(preserveWhitespaces ? 11 : 0, n_undeclaredCharacters);
        }
      }
    }

    /// <summary>
    /// FPML 4.0 schema and instance.
    /// xsi:type is used in this example.
    /// </summary>
    [Test]
    public virtual void testFpmlExample01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/fpml-4.0/fpml-main-4-0.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

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

          Uri url = resolveSystemIdAsURL("/fpml-4.0/msg_ex01_request_confirmation.xml");
          FileStream inputStream = new FileStream(url.LocalPath, FileMode.Open);
          InputSource inputSource = new InputSource<Stream>(inputStream, url.ToString());

          encoder.encode(inputSource);
          inputStream.Close();

          byte[] bts = baos.ToArray();

          decoder.GrammarCache = grammarCache;
          decoder.InputStream = new MemoryStream(bts);
          scanner = decoder.processHeader();

          EventDescription exiEvent;

          int n_events = 0;
          int n_undeclaredCharacters = 0;
          while ((exiEvent = scanner.nextEvent()) != null) {
            if (exiEvent.EventKind == EventDescription_Fields.EVENT_CH) {
              if (exiEvent.getEventType().itemType == EventType.ITEM_CH) {
                ++n_undeclaredCharacters;
                continue;
              }
            }
            ++n_events;
          }

          Assert.AreEqual(102, n_events);
          Assert.AreEqual(preserveWhitespaces ? 49 : 0, n_undeclaredCharacters);
        }
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

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString =
        "<foo:ANY xmlns:foo='urn:foo' xmlns:goo='urn:goo'\n" +
        "  foo:aA='a' foo:aB='b' foo:aC='c' >\n" +
        "  TEXT 1 " +
        "  <goo:AB> </goo:AB>\n" +
        "  TEXT 2 " +
        "  <goo:AC> </goo:AC>\n" +
        "  TEXT 3 " +
        "</foo:ANY>\n";

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        byte[] bts = baos.ToArray();

        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

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
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("ANY", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
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
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(10, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
        eventType = eventTypeList.item(6);
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
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(10, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
        eventType = eventTypeList.item(6);
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
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(10, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
        eventType = eventTypeList.item(6);
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
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("\n  TEXT 1   ", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
        Assert.AreEqual(5, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(10, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(6);
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
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AB", exiEvent.Name);
        Assert.AreEqual("urn:goo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        Assert.AreEqual(null, eventType.name);
        Assert.IsNull(eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
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
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("\n  TEXT 2   ", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AC", exiEvent.Name);
        Assert.AreEqual("urn:goo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        Assert.AreEqual(null, eventType.name);
        Assert.IsNull(eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
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
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("\n  TEXT 3 ", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
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

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString =
        "<foo:ANY xmlns:foo='urn:foo' xmlns:goo='urn:goo'>\n" +
        "  <goo:NONE>abc</goo:NONE>\n" +
        "  <goo:NONE>\n" +
        "    <foo:NONE>def</foo:NONE>\n" +
        "  </goo:NONE>\n" +
        "</foo:ANY>"; 

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveWhitespaces in new bool[] { true, false }) {
          Scanner scanner;

          encoder.AlignmentType = alignment;
          decoder.AlignmentType = alignment;

          encoder.PreserveWhitespaces = preserveWhitespaces;
          MemoryStream baos = new MemoryStream();
          encoder.OutputStream = baos;

          encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

          byte[] bts = baos.ToArray();

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
            Assert.AreEqual(1, eventTypeList.Length);
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
            Assert.AreEqual(5, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(10, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            eventType = eventTypeList.item(6);
            Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(7);
            Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
            eventType = eventTypeList.item(8);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(9);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
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
            Assert.AreEqual(5, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
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
            Assert.AreEqual(5, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
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
            Assert.AreEqual(5, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
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
            if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
              Assert.AreEqual(4, eventType.Index);
              eventTypeList = eventType.EventTypeList;
              Assert.AreEqual(5, eventTypeList.Length);
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
            Assert.AreEqual(5, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }

          if ((exiEvent = scanner.nextEvent()) != null) {
            ++n_events;
            Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(1, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(5, eventTypeList.Length);
            Assert.IsNotNull(eventTypeList.EE);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(4);
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

          Assert.AreEqual(preserveWhitespaces ? 17 : 15, n_events);
        }
      }
    }

    /// <summary>
    /// Schema:
    /// <xsd:element name="ANY" type="xsd:anyType"/>
    /// 
    /// Use mixed content characters in xsd:anyType. 
    /// </summary>
    [Test]
    public virtual void testAcceptanceForANY_03() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString = "<foo:ANY xmlns:foo='urn:foo'>abc</foo:ANY>";

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        byte[] bts = baos.ToArray();

        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

        int n_events = 0;
        EventDescription exiEvent;

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
        Assert.AreEqual("ANY", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("abc", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
        Assert.AreEqual(5, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(10, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_ONE, eventType.Depth);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(6);
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
        Assert.AreEqual(5, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);
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

        Assert.AreEqual(5, n_events);
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

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString =
        "<J xmlns='urn:foo'>" +
          "<AB> </AB><AC> </AC><AC> </AC><AB> </AB><AC> </AC>" +
        "</J>\n";

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        byte[] bts;
        int n_events;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

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
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("J", eventType.name);
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
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AC", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AC", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
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
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AC", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AC", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
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
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AB", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AB", eventType.name);
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
        ++n_events;

        exiEvent = scanner.nextEvent();
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
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AC", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AC", eventType.name);
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
        ++n_events;

        exiEvent = scanner.nextEvent();
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
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(4, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AC", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
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

        Assert.AreEqual(19, n_events);
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
    public virtual void testAcceptanceForJ_01_with_UndeclaredElement() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString =
        "<J xmlns='urn:foo'>" +
          "<AB/><AC/><AC/><AC/>" + // The 3rd <AC/> is not expected.
        "</J>\n";

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        byte[] bts;
        int n_events;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

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
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("J", eventType.name);
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
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(8, eventTypeList.Length);
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
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AC", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AC", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(8, eventTypeList.Length);
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
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AC", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AC", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(8, eventTypeList.Length);
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
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AC", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(4, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(8, eventTypeList.Length);
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
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(4, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
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

        Assert.AreEqual(12, n_events);
      }
    }

    /// <summary>
    /// Make use of ITEM_UNDECLARED_EE that belongs to an ElementGrammar.
    /// 
    /// Schema:
    /// <xsd:element name="A">
    ///   <xsd:complexType>
    ///     <xsd:sequence>
    ///       <xsd:sequence>
    ///         <xsd:element ref="foo:AB"/>
    ///         ......
    ///       </xsd:sequence>
    ///       ......
    ///     </xsd:sequence>
    ///   </xsd:complexType>
    /// </xsd:element>
    /// 
    /// Instance:
    /// <A></A>
    /// </summary>
    [Test]
    public virtual void testUndeclaredEEOfElementGrammar() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString =
        "<A xmlns='urn:foo'>" +
        "</A>\n";

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        byte[] bts;
        int n_events;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

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
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        Assert.AreEqual(3, eventType.Index);
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

    /// <summary>
    /// Make use of ITEM_UNDECLARED_EE that belongs to an ElementTagGrammar.
    /// 
    /// Schema:
    /// <xsd:complexType name="B">
    ///   <xsd:sequence>
    ///     <xsd:element ref="foo:AB"/>
    ///     <xsd:element ref="foo:AC" minOccurs="0" maxOccurs="2"/>
    ///     <xsd:element ref="foo:AD" minOccurs="0"/>
    ///   </xsd:sequence>
    /// </xsd:complexType>
    /// 
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
    /// <B xsi:type="restricted_B"></B>
    /// </summary>
    [Test]
    public virtual void testUndeclaredEEOfElementTagGrammar_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString =
        "<B xsi:type='restricted_B' xmlns='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>" +
        "</B>\n";

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        byte[] bts;
        int n_events;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

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
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
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
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        Assert.AreEqual(3, eventType.Index);
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
    /// Make use of ITEM_UNDECLARED_EE that belongs to an ElementTagGrammar, 
    /// together with xsi:nil="true" attribute.
    /// 
    /// Schema:
    /// <xsd:complexType name="F">
    ///   <xsd:sequence>
    ///   ...
    ///   </xsd:sequence>
    ///   <xsd:attribute ref="foo:aA" use="required"/>
    ///   ...
    /// </xsd:complexType>
    /// 
    /// <xsd:element name="F" type="foo:F" nillable="true"/>
    /// 
    /// Instance:
    /// <F xsi:type="F" xsi:nil="true"></F>
    /// </summary>
    [Test]
    public virtual void testUndeclaredEEOfElementTagGrammar_02() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString =
        "<F xsi:type='F' xsi:nil='true' xmlns='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>" +
        "</F>\n";

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        byte[] bts;
        int n_events;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

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
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("F", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
        Assert.AreEqual("type", exiEvent.Name);
        Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
        Assert.AreEqual("F", ((EXIEventSchemaType)exiEvent).TypeName);
        Assert.AreEqual("urn:foo", ((EXIEventSchemaType)exiEvent).TypeURI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        Assert.AreEqual("type", eventType.name);
        Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(9, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("aA", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("aA", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_NL, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(9, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("aA", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("aA", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(7, eventTypeList.Length);
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
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("aA", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("aA", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(6);
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
    }

    /// <summary>
    /// Make use of ITEM_UNDECLARED_EE that belongs to an ComplexContentGrammar.
    /// 
    /// Schema:
    /// <xsd:element name="L">
    ///   <xsd:complexType>
    ///     <xsd:sequence minOccurs="2" maxOccurs="2">
    ///       <xsd:element ref="foo:AB"/>
    ///     </xsd:sequence>
    ///   </xsd:complexType>
    /// </xsd:element>
    /// 
    /// Instance:
    /// <L>
    ///   <AB>xyz</AB>
    /// </L>
    /// </summary>
    [Test]
    public virtual void testUndeclaredEEOfComplexContentGrammar() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString =
        "<L xmlns='urn:foo'>" +
          "<AB>xyz</AB>" +
        "</L>\n";

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        byte[] bts;
        int n_events;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

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
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("L", eventType.name);
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
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("xyz", exiEvent.Characters.makeString());
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
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(4, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
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

        Assert.AreEqual(7, n_events);
      }
    }

    /// <summary>
    /// Make use of ITEM_UNDECLARED_EE that belongs to an SimpleContentGrammar.
    /// 
    /// Schema:
    /// <xsd:element name="AB" type="xsd:anySimpleType"/>
    /// 
    /// Instance:
    /// The element <AC/> is not expected.
    /// <AB><AC/></AB>
    /// </summary>
    [Test]
    public virtual void testUndeclaredEEOfSimpleContentGrammar() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString = "<AB xmlns='urn:foo'><AC/></AB>\n";

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        byte[] bts;
        int n_events;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

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
        Assert.AreEqual("AB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AC", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        Assert.AreEqual(6, eventType.Index);
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
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(8, eventTypeList.Length);
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

        Assert.AreEqual(6, n_events);
      }
    }

    /// <summary>
    /// Schema:
    /// <xsd:element name="AB" type="xsd:anySimpleType"/>
    /// 
    /// Instance:
    /// <AB xmlns="urn:foo" foo:aA="abc">xyz</AB>
    /// </summary>
    [Test]
    public virtual void testUndeclaredAttrWildcardAnyOfElementGrammar() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache;

      grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString =
        "<foo:AB xmlns:foo='urn:foo' foo:aA='abc'>xyz</foo:AB>";

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        byte[] bts = baos.ToArray();

        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

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
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        Assert.AreEqual("aA", exiEvent.Name);
        Assert.AreEqual("abc", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        Assert.AreEqual(4, eventType.Index);
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
        Assert.AreEqual("xyz", exiEvent.Characters.makeString());
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

        Assert.AreEqual(6, n_events);
      }

      encoder.GrammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      foreach (AlignmentType alignment in Alignments) {

        encoder.AlignmentType = alignment;

        encoder.OutputStream = new MemoryStream();

        bool caught = false;
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
    /// <xsd:element name="AB" type="xsd:anySimpleType"/>
    /// 
    /// Instance:
    /// <AB xmlns="urn:foo" xsi:type="xsd:string" foo:aA="abc">xyz</AB>
    /// </summary>
    [Test]
    public virtual void testUndeclaredAttrWildcardAnyOfElementTagGrammar_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache;

      grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString =
        "<foo:AB xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' \n" +
        "  xmlns:xsd='http://www.w3.org/2001/XMLSchema' \n" +
        "  xmlns:foo='urn:foo' xsi:type='xsd:string' foo:aA='abc'>" +
        "xyz</foo:AB>";

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        byte[] bts = baos.ToArray();

        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

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
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AB", eventType.name);
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
        Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        Assert.AreEqual("aA", exiEvent.Name);
        Assert.AreEqual("abc", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        Assert.AreEqual(4, eventType.Index);
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
        Assert.AreEqual("xyz", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        Assert.AreEqual(2, eventType.Index);
        Assert.AreSame(eventTypeList, eventType.EventTypeList);
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

        Assert.AreEqual(7, n_events);
      }
    }

    /// <summary>
    /// Schema:
    /// <xsd:element name="AB" type="xsd:anySimpleType"/>
    /// 
    /// Instance:
    /// <AB xmlns="urn:foo" xsi:type="xsd:string" xsi:nil="true" foo:aA="abc"></AB>
    /// </summary>
    [Test]
    public virtual void testUndeclaredAttrWildcardAnyOfElementTagGrammar_02() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache;

      grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString =
        "<foo:AB xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' \n" +
        "  xmlns:xsd='http://www.w3.org/2001/XMLSchema' \n" +
        "  xmlns:foo='urn:foo' xsi:type='xsd:string' xsi:nil='true' foo:aA='abc'>" +
        "</foo:AB>";

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        byte[] bts = baos.ToArray();

        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

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
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AB", eventType.name);
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
        Assert.AreEqual(EventDescription_Fields.EVENT_NL, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        Assert.IsTrue(((EXIEventSchemaNil)exiEvent).Nilled);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(8, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
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
        Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        Assert.AreEqual("aA", exiEvent.Name);
        Assert.AreEqual("abc", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        Assert.AreEqual(1, eventType.Index);
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
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        // No grammatical transition was caused by the preceding ITEM_SCHEMA_AT_WC_ANY.
        Assert.AreSame(eventTypeList, eventType.EventTypeList);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        ++n_events;

        Assert.AreEqual(7, n_events);
      }
    }

    /// <summary>
    /// Test the uses of ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE and 
    /// ITEM_AT_WC_ANY.
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

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString =
        "<foo:M xmlns:foo='urn:foo' xmlns:goo='urn:goo' " +
        "       goo:b0='' foo:bA='' goo:bA='' foo:bB='' goo:bB='' " +
        "       foo:bC='' goo:bC='' foo:bD='' goo:bD='' >" +
        "</foo:M>\n";

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        MemoryStream baos;

        baos = new MemoryStream();
        encoder.OutputStream = baos;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        byte[] bts = baos.ToArray();

        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

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
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("M", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
        Assert.AreEqual("b0", exiEvent.Name);
        Assert.AreEqual("urn:goo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        Assert.AreEqual(5, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(11, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("bA", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("bB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("bA", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("bB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(10);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
        Assert.AreEqual("bA", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("bA", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(6, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(11, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("bA", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("bB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("bB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(10);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
        Assert.AreEqual("bA", exiEvent.Name);
        Assert.AreEqual("urn:goo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        Assert.AreEqual(4, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(7, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("bB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("bB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
        Assert.AreEqual("bB", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("bB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(7, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("bB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
        Assert.AreEqual("bB", exiEvent.Name);
        Assert.AreEqual("urn:goo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        Assert.AreEqual(6, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(9, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("bC", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("bD", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("bC", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("bD", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
        Assert.AreEqual("bC", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("bC", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(4, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(9, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("bC", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("bD", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("bD", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
        Assert.AreEqual("bC", exiEvent.Name);
        Assert.AreEqual("urn:goo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        Assert.AreEqual(4, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(7, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("bD", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("bD", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
        Assert.AreEqual("bD", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("bD", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(7, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("bD", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
        Assert.AreEqual("bD", exiEvent.Name);
        Assert.AreEqual("urn:goo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
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

        Assert.AreEqual(13, n_events);
      }
    }

    /// <summary>
    /// Test the uses of ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE with
    /// prefix preservation.
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
    public virtual void testUseInvalidAttributes_PreserveNS() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

      const string xmlString =
        "<foo:M xmlns:foo='urn:foo' " +
        "       foo:bA='' foo:bB='' foo:bC='' foo:bD=''>" +
        "</foo:M>\n";

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        MemoryStream baos;

        baos = new MemoryStream();
        encoder.OutputStream = baos;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        byte[] bts = baos.ToArray();

        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

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
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("M", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        Assert.IsNull(exiEvent.Prefix);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("M", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
        Assert.AreEqual("foo", exiEvent.Prefix);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        Assert.IsTrue(((EXIEventNS)exiEvent).LocalElementNs);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
        Assert.IsNull(eventType.name);
        Assert.IsNull(eventType.uri);
        Assert.AreEqual(9, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(12, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("bA", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("bB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("bA", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("bB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(10);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(11);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
        Assert.AreEqual("bA", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        Assert.AreEqual("foo", exiEvent.Prefix);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("bA", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(6, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(12, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("bA", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("bB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("bB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
        Assert.IsNull(eventType.name);
        Assert.IsNull(eventType.uri);
        eventType = eventTypeList.item(10);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(11);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
        Assert.AreEqual("bB", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        Assert.AreEqual("foo", exiEvent.Prefix);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("bB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(7, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("bB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
        Assert.AreEqual("bC", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        Assert.AreEqual("foo", exiEvent.Prefix);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("bC", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(4, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(9, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("bC", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("bD", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("bD", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
        Assert.AreEqual("bD", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        Assert.AreEqual("foo", exiEvent.Prefix);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("bD", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(7, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("bD", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
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

        Assert.AreEqual(9, n_events);
      }
    }

    /// <summary>
    /// Make use of ITEM_SCHEMA_NS that belongs to an ElementGrammar and ElementTagGrammar.
    /// Note that the ITEM_SCHEMA_NS event in ElementTagGrammar cannot be exercised since
    /// it never matches an namespace declaration instance. 
    /// 
    /// Schema:
    /// <xsd:complexType name="F">
    ///   <xsd:sequence>
    ///   ...
    ///   </xsd:sequence>
    ///   <xsd:attribute ref="foo:aA" use="required"/>
    ///   ...
    /// </xsd:complexType>
    /// 
    /// <xsd:element name="F" type="foo:F" nillable="true"/>
    /// </summary>
    [Test]
    public virtual void testNamespaceDeclaration_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

      const string xmlString =
        "<F xsi:type='F' xmlns='urn:foo' xmlns:foo='urn:foo' " +
        "   xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'" +
        "   foo:aA='abc'>" +
        "</F>\n";

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveLexicalValues in new bool[] { true, false }) {
          // REVISIT: create encoder and decoder outside the loop. Why not?
          Transmogrifier encoder = new Transmogrifier();
          EXIDecoder decoder = new EXIDecoder();

          encoder.GrammarCache = grammarCache;
          decoder.GrammarCache = grammarCache;

          encoder.AlignmentType = alignment;
          decoder.AlignmentType = alignment;

          encoder.PreserveLexicalValues = preserveLexicalValues;
          decoder.PreserveLexicalValues = preserveLexicalValues;

          MemoryStream baos = new MemoryStream();
          encoder.OutputStream = baos;

          byte[] bts;
          int n_events;

          encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

          bts = baos.ToArray();

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
          Assert.AreEqual(1, eventTypeList.Length);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("F", exiEvent.Name);
          Assert.AreEqual("urn:foo", exiEvent.URI);
          Assert.IsNull(exiEvent.Prefix);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("F", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
          eventTypeList = eventType.EventTypeList;
          Assert.IsNull(eventTypeList.EE);
          ++n_events;

          for (int i = 2; i < 5; i++) {
            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
            switch (i) {
              case 2:
                Assert.AreEqual("", exiEvent.Prefix);
                Assert.AreEqual("urn:foo", exiEvent.URI);
                Assert.IsTrue(((EXIEventNS)exiEvent).LocalElementNs);
                break;
              case 3:
                Assert.AreEqual("foo", exiEvent.Prefix);
                Assert.AreEqual("urn:foo", exiEvent.URI);
                Assert.IsFalse(((EXIEventNS)exiEvent).LocalElementNs);
                break;
              case 4:
                Assert.AreEqual("xsi", exiEvent.Prefix);
                Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
                Assert.IsFalse(((EXIEventNS)exiEvent).LocalElementNs);
                break;
              default:
                break;
            }
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
            Assert.IsNull(eventType.name);
            Assert.IsNull(eventType.uri);
            Assert.AreEqual(7, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(10, eventTypeList.Length);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
            Assert.AreEqual("aA", eventType.name);
            Assert.AreEqual("urn:foo", eventType.uri);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(5);
            Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
            Assert.AreEqual("aA", eventType.name);
            Assert.AreEqual("urn:foo", eventType.uri);
            eventType = eventTypeList.item(6);
            Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
            eventType = eventTypeList.item(8);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(9);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
            ++n_events;
          }

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
          Assert.AreEqual("type", exiEvent.Name);
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
          Assert.AreEqual("F", ((EXIEventSchemaType)exiEvent).TypeName);
          Assert.AreEqual("urn:foo", ((EXIEventSchemaType)exiEvent).TypeURI);
          Assert.AreEqual("", ((EXIEventSchemaType)exiEvent).TypePrefix);
          Assert.IsTrue(preserveLexicalValues && "F".Equals(exiEvent.Characters.makeString()) || !preserveLexicalValues && exiEvent.Characters == null);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
          Assert.AreEqual("type", eventType.name);
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(10, eventTypeList.Length);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
          Assert.AreEqual("aA", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(4);
          Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(5);
          Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
          Assert.AreEqual("aA", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
          eventType = eventTypeList.item(6);
          Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
          eventType = eventTypeList.item(7);
          Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
          Assert.IsNull(eventType.name);
          Assert.IsNull(eventType.uri);
          eventType = eventTypeList.item(8);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(9);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
          Assert.AreEqual("aA", exiEvent.Name);
          Assert.AreEqual("urn:foo", exiEvent.URI);
          Assert.AreEqual("foo", exiEvent.Prefix);
          Assert.AreEqual("abc", exiEvent.Characters.makeString());
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
          Assert.AreEqual("aA", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
          Assert.AreEqual(2, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(10, eventTypeList.Length);
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
          Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
          Assert.AreEqual("aA", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
          eventType = eventTypeList.item(6);
          Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
          eventType = eventTypeList.item(7);
          Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
          Assert.IsNull(eventType.name);
          Assert.IsNull(eventType.uri);
          eventType = eventTypeList.item(8);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(9);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          Assert.AreEqual(2, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(8, eventTypeList.Length);
          eventType = eventTypeList.item(0);
          Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
          Assert.AreEqual("aB", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("AB", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(4);
          Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
          Assert.AreEqual("aB", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
          eventType = eventTypeList.item(5);
          Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
          eventType = eventTypeList.item(6);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(7);
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

          Assert.AreEqual(9, n_events);
        }
      }
    }

    /// <summary>
    /// Nested elements with namespace declarations.
    /// 
    /// Schema:
    /// <xsd:element name="H">
    ///   <xsd:complexType>
    ///     <xsd:sequence>
    ///       <xsd:element name="A" minOccurs="0"/>
    ///       <xsd:any namespace="urn:eoo urn:goo" />
    ///       .....
    ///     </xsd:sequence>
    ///   </xsd:complexType>
    /// </xsd:element>
    /// </summary>
    [Test]
    public virtual void testNamespaceDeclaration_02() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

      const string xmlString =
        "<H xmlns='urn:foo' xmlns:goo1='urn:goo' xmlns:goo2='urn:goo' " +
        "   goo1:aA='abc' >" +
          "<goo2:AB xmlns='urn:goo' xmlns:goo2='urn:goo' xmlns:foo='urn:foo'/>" +
        "</H>\n";

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        byte[] bts;
        int n_events;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

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
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("H", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        Assert.IsNull(exiEvent.Prefix);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("H", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        for (int i = 2; i < 5; i++) {
          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
          switch (i) {
            case 2:
              Assert.AreEqual("", exiEvent.Prefix);
              Assert.AreEqual("urn:foo", exiEvent.URI);
              Assert.IsTrue(((EXIEventNS)exiEvent).LocalElementNs);
              break;
            case 3:
              Assert.AreEqual("goo1", exiEvent.Prefix);
              Assert.AreEqual("urn:goo", exiEvent.URI);
              Assert.IsFalse(((EXIEventNS)exiEvent).LocalElementNs);
              break;
            case 4:
              Assert.AreEqual("goo2", exiEvent.Prefix);
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
          Assert.AreEqual(8, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(11, eventTypeList.Length);
          eventType = eventTypeList.item(0);
          Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("A", eventType.name);
          Assert.AreEqual("", eventType.uri);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
          Assert.AreEqual("urn:eoo", eventType.uri);
          eventType = eventTypeList.item(4);
          Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
          Assert.AreEqual("urn:goo", eventType.uri);
          eventType = eventTypeList.item(5);
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(6);
          Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(7);
          Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
          eventType = eventTypeList.item(9);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(10);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          ++n_events;
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
        Assert.AreEqual("aA", exiEvent.Name);
        Assert.AreEqual("urn:goo", exiEvent.URI);
        Assert.AreEqual("goo1", exiEvent.Prefix);
        Assert.AreEqual("abc", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        Assert.AreEqual(6, eventType.Index);
        Assert.AreEqual(11, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:eoo", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:goo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
        Assert.IsNull(eventType.name);
        Assert.IsNull(eventType.uri);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(10);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AB", exiEvent.Name);
        Assert.AreEqual("urn:goo", exiEvent.URI);
        Assert.AreEqual("goo2", exiEvent.Prefix);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:goo", eventType.uri);
        Assert.AreEqual(4, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(11, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:eoo", eventType.uri);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
        Assert.IsNull(eventType.name);
        Assert.IsNull(eventType.uri);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(10);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        for (int i = 7; i < 10; i++) {
          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
          switch (i) {
            case 7:
              Assert.AreEqual("", exiEvent.Prefix);
              Assert.AreEqual("urn:goo", exiEvent.URI);
              Assert.IsFalse(((EXIEventNS)exiEvent).LocalElementNs);
              break;
            case 8:
              Assert.AreEqual("goo2", exiEvent.Prefix);
              Assert.AreEqual("urn:goo", exiEvent.URI);
              Assert.IsTrue(((EXIEventNS)exiEvent).LocalElementNs);
              break;
            case 9:
              Assert.AreEqual("foo", exiEvent.Prefix);
              Assert.AreEqual("urn:foo", exiEvent.URI);
              Assert.IsFalse(((EXIEventNS)exiEvent).LocalElementNs);
              break;
            default:
              break;
          }
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
          Assert.IsNull(eventType.name);
          Assert.IsNull(eventType.uri);
          Assert.AreEqual(6, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(9, eventTypeList.Length);
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
          eventType = eventTypeList.item(7);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(8);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          ++n_events;
        }

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(9, eventTypeList.Length);
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
        Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
        Assert.IsNull(eventType.name);
        Assert.IsNull(eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(4, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("", eventType.uri);
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

        Assert.AreEqual(13, n_events);
      }
    }

    /// <summary>
    /// Nested elements with namespace declarations, where two or more
    /// prefixes are assigned to the same URI. 
    /// 
    /// Schema:
    /// <xsd:complexType name="B">
    ///   <xsd:sequence>
    ///     <xsd:element ref="foo:AB"/>
    ///     <xsd:element ref="foo:AC" minOccurs="0" maxOccurs="2"/>
    ///     <xsd:element ref="foo:AD" minOccurs="0"/>
    ///   </xsd:sequence>
    /// </xsd:complexType>
    /// </summary>
    [Test]
    public virtual void testNamespaceDeclaration_03() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

      const string xmlString =
        "<B xmlns='urn:foo' xmlns:goo='urn:goo' xmlns:foo='urn:foo'>" +
          "<AB xmlns:hoo='urn:hoo'/>" +
        "</B>\n";

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        byte[] bts;
        int n_events;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

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
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("B", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        Assert.IsNull(exiEvent.Prefix);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Prefix);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        Assert.IsTrue(((EXIEventNS)exiEvent).LocalElementNs);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
        Assert.IsNull(eventType.name);
        Assert.IsNull(eventType.uri);
        Assert.AreEqual(6, eventType.Index);
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
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
        Assert.AreEqual("goo", exiEvent.Prefix);
        Assert.AreEqual("urn:goo", exiEvent.URI);
        Assert.IsFalse(((EXIEventNS)exiEvent).LocalElementNs);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
        Assert.IsNull(eventType.name);
        Assert.IsNull(eventType.uri);
        Assert.AreEqual(6, eventType.Index);
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
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
        Assert.AreEqual("foo", exiEvent.Prefix);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        Assert.IsFalse(((EXIEventNS)exiEvent).LocalElementNs);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
        Assert.IsNull(eventType.name);
        Assert.IsNull(eventType.uri);
        Assert.AreEqual(6, eventType.Index);
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
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AB", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        Assert.AreEqual("", exiEvent.Prefix);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
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
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
        Assert.IsNull(eventType.name);
        Assert.IsNull(eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
        Assert.AreEqual("hoo", exiEvent.Prefix);
        Assert.AreEqual("urn:hoo", exiEvent.URI);
        Assert.IsFalse(((EXIEventNS)exiEvent).LocalElementNs);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
        Assert.IsNull(eventType.name);
        Assert.IsNull(eventType.uri);
        Assert.AreEqual(6, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(9, eventTypeList.Length);
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
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(9, eventTypeList.Length);
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
        Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
        Assert.IsNull(eventType.name);
        Assert.IsNull(eventType.uri);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
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
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        ++n_events;

        Assert.AreEqual(10, n_events);
      }
    }

    /// <summary>
    /// xsi:nil attribute with a single corresponding namespace declaration.
    /// 
    /// Schema:
    /// <xsd:complexType name="B">
    ///   <xsd:sequence>
    ///     <xsd:element ref="foo:AB"/>
    ///     ...
    ///   </xsd:sequence>
    /// </xsd:complexType>
    /// </summary>
    [Test]
    public virtual void testNamespaceDeclaration_04() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

      const string xmlString =
        "<B xmlns='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " +
        "   xsi:nil='true' >" +
        "</B>\n";

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveLexicalValues in new bool[] { true, false }) {
          Scanner scanner;

          encoder.AlignmentType = alignment;
          decoder.AlignmentType = alignment;

          encoder.PreserveLexicalValues = preserveLexicalValues;
          decoder.PreserveLexicalValues = preserveLexicalValues;

          MemoryStream baos = new MemoryStream();
          encoder.OutputStream = baos;

          byte[] bts;
          int n_events;

          encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

          bts = baos.ToArray();

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
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("B", exiEvent.Name);
          Assert.AreEqual("urn:foo", exiEvent.URI);
          Assert.IsNull(exiEvent.Prefix);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("B", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
          eventTypeList = eventType.EventTypeList;
          Assert.IsNull(eventTypeList.EE);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
          Assert.AreEqual("", exiEvent.Prefix);
          Assert.AreEqual("urn:foo", exiEvent.URI);
          Assert.IsTrue(((EXIEventNS)exiEvent).LocalElementNs);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
          Assert.IsNull(eventType.name);
          Assert.IsNull(eventType.uri);
          Assert.AreEqual(6, eventType.Index);
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
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(4);
          Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(5);
          Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
          eventType = eventTypeList.item(7);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(8);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
          Assert.AreEqual("xsi", exiEvent.Prefix);
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
          Assert.IsFalse(((EXIEventNS)exiEvent).LocalElementNs);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
          Assert.IsNull(eventType.name);
          Assert.IsNull(eventType.uri);
          Assert.AreEqual(6, eventType.Index);
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
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(4);
          Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(5);
          Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
          eventType = eventTypeList.item(7);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(8);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_NL, exiEvent.EventKind);
          Assert.AreEqual("nil", exiEvent.Name);
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
          Assert.AreEqual("xsi", exiEvent.Prefix);
          Assert.IsTrue(((EXIEventSchemaNil)exiEvent).Nilled);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
          Assert.AreEqual(1, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(9, eventTypeList.Length);
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
          Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
          eventType = eventTypeList.item(7);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(8);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(6, eventTypeList.Length);
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
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
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

          Assert.AreEqual(7, n_events);
        }
      }
    }

    /// <summary>
    /// xsi:nil attribute with two corresponding namespace declarations.
    /// 
    /// Schema:
    /// <xsd:complexType name="B">
    ///   <xsd:sequence>
    ///     <xsd:element ref="foo:AB"/>
    ///     ...
    ///   </xsd:sequence>
    /// </xsd:complexType>
    /// </summary>
    [Test]
    public virtual void testNamespaceDeclaration_05() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

      const string xmlString =
        "<B xmlns='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " +
        "   xmlns:xsh='http://www.w3.org/2001/XMLSchema-instance' " +
        "   xsh:nil='true' >" +
        "</B>\n";

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveLexicalValues in new bool[] { true, false }) {
          Scanner scanner;

          encoder.AlignmentType = alignment;
          decoder.AlignmentType = alignment;

          encoder.PreserveLexicalValues = preserveLexicalValues;
          decoder.PreserveLexicalValues = preserveLexicalValues;

          MemoryStream baos = new MemoryStream();
          encoder.OutputStream = baos;

          byte[] bts;
          int n_events;

          encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

          bts = baos.ToArray();

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
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("B", exiEvent.Name);
          Assert.AreEqual("urn:foo", exiEvent.URI);
          Assert.IsNull(exiEvent.Prefix);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("B", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
          eventTypeList = eventType.EventTypeList;
          Assert.IsNull(eventTypeList.EE);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
          Assert.AreEqual("", exiEvent.Prefix);
          Assert.AreEqual("urn:foo", exiEvent.URI);
          Assert.IsTrue(((EXIEventNS)exiEvent).LocalElementNs);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
          Assert.IsNull(eventType.name);
          Assert.IsNull(eventType.uri);
          Assert.AreEqual(6, eventType.Index);
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
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(4);
          Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(5);
          Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
          eventType = eventTypeList.item(7);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(8);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
          Assert.AreEqual("xsi", exiEvent.Prefix);
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
          Assert.IsFalse(((EXIEventNS)exiEvent).LocalElementNs);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
          Assert.IsNull(eventType.name);
          Assert.IsNull(eventType.uri);
          Assert.AreEqual(6, eventType.Index);
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
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(4);
          Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(5);
          Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
          eventType = eventTypeList.item(7);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(8);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
          Assert.AreEqual("xsh", exiEvent.Prefix);
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
          Assert.IsFalse(((EXIEventNS)exiEvent).LocalElementNs);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
          Assert.IsNull(eventType.name);
          Assert.IsNull(eventType.uri);
          Assert.AreEqual(6, eventType.Index);
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
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(4);
          Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(5);
          Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
          eventType = eventTypeList.item(7);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(8);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_NL, exiEvent.EventKind);
          Assert.AreEqual("nil", exiEvent.Name);
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
          Assert.AreEqual("xsh", exiEvent.Prefix);
          Assert.IsTrue(((EXIEventSchemaNil)exiEvent).Nilled);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
          Assert.AreEqual(1, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(9, eventTypeList.Length);
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
          Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
          eventType = eventTypeList.item(7);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(8);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(6, eventTypeList.Length);
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
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
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

          Assert.AreEqual(8, n_events);
        }
      }
    }

    /// <summary>
    /// xsi:type attribute with a single corresponding namespace declaration.
    /// 
    /// Schema: 
    /// <xsd:complexType name="restricted_B">
    ///   <xsd:complexContent>
    ///     <xsd:restriction base="foo:B">
    ///       <xsd:sequence>
    ///         <xsd:element ref="foo:AB"/>
    ///         ...
    ///       </xsd:sequence>
    ///     </xsd:restriction>
    ///   </xsd:complexContent>
    /// </xsd:complexType>
    /// </summary>
    [Test]
    public virtual void testNamespaceDeclaration_06() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

      const string xmlString =
        "<B xmlns='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " +
        "   xsi:type='restricted_B' xsi:nil='true'>" +
        "</B>\n";

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveLexicalValues in new bool[] { true, false }) {
          Scanner scanner;

          encoder.AlignmentType = alignment;
          decoder.AlignmentType = alignment;

          encoder.PreserveLexicalValues = preserveLexicalValues;
          decoder.PreserveLexicalValues = preserveLexicalValues;

          MemoryStream baos = new MemoryStream();
          encoder.OutputStream = baos;

          encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

          byte[] bts = baos.ToArray();

          decoder.InputStream = new MemoryStream(bts);
          scanner = decoder.processHeader();

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
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("B", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
          eventTypeList = eventType.EventTypeList;
          Assert.IsNull(eventTypeList.EE);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
          Assert.AreEqual("", exiEvent.Prefix);
          Assert.AreEqual("urn:foo", exiEvent.URI);
          Assert.IsTrue(((EXIEventNS)exiEvent).LocalElementNs);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
          Assert.IsNull(eventType.name);
          Assert.IsNull(eventType.uri);
          Assert.AreEqual(6, eventType.Index);
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
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(4);
          Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(5);
          Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
          eventType = eventTypeList.item(7);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(8);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
          Assert.AreEqual("xsi", exiEvent.Prefix);
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
          Assert.IsFalse(((EXIEventNS)exiEvent).LocalElementNs);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
          Assert.IsNull(eventType.name);
          Assert.IsNull(eventType.uri);
          Assert.AreEqual(6, eventType.Index);
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
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(4);
          Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(5);
          Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
          eventType = eventTypeList.item(7);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(8);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
          Assert.AreEqual("type", exiEvent.Name);
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
          Assert.AreEqual("xsi", exiEvent.Prefix);
          Assert.AreEqual("restricted_B", ((EXIEventSchemaType)exiEvent).TypeName);
          Assert.AreEqual("urn:foo", ((EXIEventSchemaType)exiEvent).TypeURI);
          Assert.AreEqual("", ((EXIEventSchemaType)exiEvent).TypePrefix);
          Assert.IsTrue(preserveLexicalValues && "restricted_B".Equals(exiEvent.Characters.makeString()) || !preserveLexicalValues && exiEvent.Characters == null);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
          Assert.AreEqual("type", eventType.name);
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
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
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(4);
          Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(5);
          Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
          eventType = eventTypeList.item(6);
          Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
          eventType = eventTypeList.item(7);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(8);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_NL, exiEvent.EventKind);
          Assert.AreEqual("nil", exiEvent.Name);
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
          Assert.AreEqual("xsi", exiEvent.Prefix);
          Assert.IsTrue(((EXIEventSchemaNil)exiEvent).Nilled);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
          Assert.AreEqual("nil", eventType.name);
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
          Assert.AreEqual(1, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(9, eventTypeList.Length);
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
          Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
          eventType = eventTypeList.item(7);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(8);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(6, eventTypeList.Length);
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
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
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

          Assert.AreEqual(8, n_events);
        }
      }
    }

    /// <summary>
    /// xsi:type attribute with a single corresponding namespace declaration.
    /// 
    /// Schema: 
    /// <xsd:complexType name="restricted_B">
    ///   <xsd:complexContent>
    ///     <xsd:restriction base="foo:B">
    ///       <xsd:sequence>
    ///         <xsd:element ref="foo:AB"/>
    ///         ...
    ///       </xsd:sequence>
    ///     </xsd:restriction>
    ///   </xsd:complexContent>
    /// </xsd:complexType>
    /// </summary>
    [Test]
    public virtual void testNamespaceDeclaration_07() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

      const string xmlString =
        "<B xmlns='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " +
        "   xmlns:xsh='http://www.w3.org/2001/XMLSchema-instance' " +
        "   xsh:type='restricted_B' xsi:nil='true'>" +
        "</B>\n";

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveLexicalValues in new bool[] { true, false }) {
          Scanner scanner;

          encoder.AlignmentType = alignment;
          decoder.AlignmentType = alignment;

          encoder.PreserveLexicalValues = preserveLexicalValues;
          decoder.PreserveLexicalValues = preserveLexicalValues;

          MemoryStream baos = new MemoryStream();
          encoder.OutputStream = baos;

          encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

          byte[] bts = baos.ToArray();

          decoder.InputStream = new MemoryStream(bts);
          scanner = decoder.processHeader();

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
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("B", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
          eventTypeList = eventType.EventTypeList;
          Assert.IsNull(eventTypeList.EE);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
          Assert.AreEqual("", exiEvent.Prefix);
          Assert.AreEqual("urn:foo", exiEvent.URI);
          Assert.IsTrue(((EXIEventNS)exiEvent).LocalElementNs);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
          Assert.IsNull(eventType.name);
          Assert.IsNull(eventType.uri);
          Assert.AreEqual(6, eventType.Index);
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
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(4);
          Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(5);
          Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
          eventType = eventTypeList.item(7);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(8);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
          Assert.AreEqual("xsi", exiEvent.Prefix);
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
          Assert.IsFalse(((EXIEventNS)exiEvent).LocalElementNs);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
          Assert.IsNull(eventType.name);
          Assert.IsNull(eventType.uri);
          Assert.AreEqual(6, eventType.Index);
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
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(4);
          Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(5);
          Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
          eventType = eventTypeList.item(7);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(8);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
          Assert.AreEqual("xsh", exiEvent.Prefix);
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
          Assert.IsFalse(((EXIEventNS)exiEvent).LocalElementNs);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
          Assert.IsNull(eventType.name);
          Assert.IsNull(eventType.uri);
          Assert.AreEqual(6, eventType.Index);
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
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(4);
          Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(5);
          Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
          eventType = eventTypeList.item(7);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(8);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
          Assert.AreEqual("type", exiEvent.Name);
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
          Assert.AreEqual("xsh", exiEvent.Prefix);
          Assert.AreEqual("restricted_B", ((EXIEventSchemaType)exiEvent).TypeName);
          Assert.AreEqual("urn:foo", ((EXIEventSchemaType)exiEvent).TypeURI);
          Assert.AreEqual("", ((EXIEventSchemaType)exiEvent).TypePrefix);
          Assert.IsTrue(preserveLexicalValues && "restricted_B".Equals(exiEvent.Characters.makeString()) || !preserveLexicalValues && exiEvent.Characters == null);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
          Assert.AreEqual("type", eventType.name);
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
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
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(4);
          Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(5);
          Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
          eventType = eventTypeList.item(6);
          Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
          eventType = eventTypeList.item(7);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(8);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_NL, exiEvent.EventKind);
          Assert.AreEqual("nil", exiEvent.Name);
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
          Assert.AreEqual("xsi", exiEvent.Prefix);
          Assert.IsTrue(((EXIEventSchemaNil)exiEvent).Nilled);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
          Assert.AreEqual("nil", eventType.name);
          Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
          Assert.AreEqual(1, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(9, eventTypeList.Length);
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
          Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
          eventType = eventTypeList.item(7);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(8);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(6, eventTypeList.Length);
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
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
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

          Assert.AreEqual(9, n_events);
        }
      }
    }

    /// <summary>
    /// Schema:
    /// <xsd:complexType name="B">
    ///   <xsd:sequence>
    ///     <xsd:element ref="foo:AB"/>
    ///     <xsd:element ref="foo:AC" minOccurs="0" maxOccurs="2"/>
    ///     <xsd:element ref="foo:AD" minOccurs="0"/>
    ///   </xsd:sequence>
    /// </xsd:complexType>
    /// 
    /// <xsd:complexType name="extended_B">
    ///   <xsd:complexContent>
    ///     <xsd:extension base="foo:B">
    ///       <xsd:attribute ref="foo:aA" use="required"/>
    ///     </xsd:extension>
    ///   </xsd:complexContent>
    /// </xsd:complexType>
    /// </summary>
    [Test]
    public virtual void testSelfContained_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addSC(GrammarOptions.DEFAULT_OPTIONS));

      const string xmlString =
        "<foo:B xmlns:foo='urn:foo' xsi:type='foo:extended_B' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'" +
        "       foo:aA='xyz'>" +
          "<foo:AB> </foo:AB>" +
        "</foo:B>\n";

      foreach (AlignmentType alignment in Alignments) {
        // REVISIT: create encoder and decoder outside the loop. Why not?
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        try {
          encoder.GrammarCache = grammarCache;
        }
        catch (EXIOptionsException) {
          Assert.IsTrue(alignment == AlignmentType.compress || alignment == AlignmentType.preCompress);
          continue;
        }
        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        byte[] bts = baos.ToArray();

        decoder.GrammarCache = grammarCache;
        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

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
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
        Assert.AreEqual("type", exiEvent.Name);
        Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
        Assert.AreEqual("extended_B", ((EXIEventSchemaType)exiEvent).TypeName);
        Assert.AreEqual("urn:foo", ((EXIEventSchemaType)exiEvent).TypeURI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        Assert.AreEqual("type", eventType.name);
        Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
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
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SC, eventType.itemType);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
        Assert.AreEqual("aA", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        Assert.AreEqual("xyz", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("aA", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(10, eventTypeList.Length);
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
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("aA", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
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
        Assert.AreEqual(6, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
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
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual(" ", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
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
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_SC, eventType.itemType);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
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
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        ++n_events;

        Assert.AreEqual(9, n_events);
      }
    }

    /// <summary>
    /// Schema:
    /// <xsd:complexType name="B">
    ///   <xsd:sequence>
    ///     <xsd:element ref="foo:AB"/>
    ///     <xsd:element ref="foo:AC" minOccurs="0" maxOccurs="2"/>
    ///     <xsd:element ref="foo:AD" minOccurs="0"/>
    ///   </xsd:sequence>
    /// </xsd:complexType>
    /// 
    /// <xsd:complexType name="extended_B">
    ///   <xsd:complexContent>
    ///     <xsd:extension base="foo:B">
    ///       <xsd:attribute ref="foo:aA" use="required"/>
    ///     </xsd:extension>
    ///   </xsd:complexContent>
    /// </xsd:complexType>
    /// 
    /// Same as testSelfContained_01 except that NS is enabled in addition to SC.
    /// </summary>
    [Test]
    public virtual void testSelfContained_02() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      short grammarOptions;
      grammarOptions = GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS);
      grammarOptions = GrammarOptions.addSC(grammarOptions);

      GrammarCache grammarCache = new GrammarCache(corpus, grammarOptions);

      const string xmlString =
        "<foo:B xmlns:foo='urn:foo' xsi:type='foo:extended_B' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'" +
        "       foo:aA='xyz'>" +
          "<foo:AB> </foo:AB>" +
        "</foo:B>\n";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        try {
          encoder.GrammarCache = grammarCache;
        }
        catch (EXIOptionsException) {
          Assert.IsTrue(alignment == AlignmentType.compress || alignment == AlignmentType.preCompress);
          continue;
        }
        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        byte[] bts = baos.ToArray();

        decoder.GrammarCache = grammarCache;
        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

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
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
        Assert.AreEqual("foo", exiEvent.Prefix);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        Assert.IsTrue(((EXIEventNS)exiEvent).LocalElementNs);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
        Assert.IsNull(eventType.name);
        Assert.IsNull(eventType.uri);
        Assert.AreEqual(6, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(10, eventTypeList.Length);
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
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
        Assert.AreEqual("xsi", exiEvent.Prefix);
        Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
        Assert.IsFalse(((EXIEventNS)exiEvent).LocalElementNs);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
        Assert.IsNull(eventType.name);
        Assert.IsNull(eventType.uri);
        Assert.AreEqual(6, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(10, eventTypeList.Length);
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
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
        Assert.AreEqual("type", exiEvent.Name);
        Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
        Assert.AreEqual("extended_B", ((EXIEventSchemaType)exiEvent).TypeName);
        Assert.AreEqual("urn:foo", ((EXIEventSchemaType)exiEvent).TypeURI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        Assert.AreEqual("type", eventType.name);
        Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(10, eventTypeList.Length);
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
        Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
        Assert.AreEqual("aA", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        Assert.AreEqual("xyz", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        Assert.AreEqual("aA", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(11, eventTypeList.Length);
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
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventType.itemType);
        Assert.AreEqual("aA", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_SC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(10);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
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
        Assert.AreEqual(6, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
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
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual(" ", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(10, eventTypeList.Length);
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
        Assert.AreEqual(EventType.ITEM_NS, eventType.itemType);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
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
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        ++n_events;

        Assert.AreEqual(11, n_events);
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
    /// Use of xsi:nil in the presence of SC.
    /// </summary>
    [Test]
    public virtual void testSelfContained_03() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addSC(GrammarOptions.DEFAULT_OPTIONS));

      const string xmlString =
        "<nillable_B xmlns='urn:foo' xsi:nil='true' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>" +
        "</nillable_B>\n";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        try {
          encoder.GrammarCache = grammarCache;
        }
        catch (EXIOptionsException) {
          Assert.IsTrue(alignment == AlignmentType.compress || alignment == AlignmentType.preCompress);
          continue;
        }
        byte[] bts;
        MemoryStream baos;

        baos = new MemoryStream();
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
        Assert.AreEqual("nillable_B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
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
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(9, eventTypeList.Length);
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
        Assert.AreEqual(EventType.ITEM_SC, eventType.itemType);
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(6, eventTypeList.Length);
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
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_THREE, eventType.Depth);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SC, eventType.itemType);
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
    public virtual void testCommentPI_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      short options = GrammarOptions.DEFAULT_OPTIONS;
      options = GrammarOptions.addCM(options);
      options = GrammarOptions.addPI(options);

      GrammarCache grammarCache = new GrammarCache(corpus, options);

      const string xmlString =
        "<C xmlns='urn:foo'><AC/><!-- Good? --><?eg Good! ?></C><?eg Good? ?><!-- Good! -->";

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
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("C", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
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
        ++n_events;

        exiEvent = scanner.nextEvent();
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
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CM, exiEvent.EventKind);
        Assert.AreEqual(" Good? ", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        Assert.AreEqual(5, eventType.Index);
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
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_PI, exiEvent.EventKind);
        Assert.AreEqual("eg", exiEvent.Name);
        Assert.AreEqual("Good! ", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
        Assert.AreEqual(6, eventType.Index);
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
        ++n_events;

        exiEvent = scanner.nextEvent();
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
        ++n_events;

        exiEvent = scanner.nextEvent();
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
        ++n_events;

        exiEvent = scanner.nextEvent();
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
        ++n_events;

        exiEvent = scanner.nextEvent();
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
        ++n_events;

        Assert.AreEqual(10, n_events);
      }
    }

    /// <summary>
    /// Exercise CM and PI in "sequence" group.
    /// 
    /// Schema:
    /// <xsd:complexType name="B">
    ///   <xsd:sequence>
    ///     <xsd:element ref="foo:AB"/>
    ///     <xsd:element ref="foo:AC" minOccurs="0" maxOccurs="2"/>
    ///     <xsd:element ref="foo:AD" minOccurs="0"/>
    ///   </xsd:sequence>
    /// </xsd:complexType>
    /// 
    /// Instance:
    /// <B><AB/><!-- Good? --><?eg Good! ?></B>
    /// </summary>
    [Test]
    public virtual void testCommentPI_02() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      short options = GrammarOptions.DEFAULT_OPTIONS;
      options = GrammarOptions.addCM(options);
      options = GrammarOptions.addPI(options);

      GrammarCache grammarCache = new GrammarCache(corpus, options);

      const string xmlString =
        "<B xmlns='urn:foo'><AB/><!-- Good? --><?eg Good! ?></B>";

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
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
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
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(10, eventTypeList.Length);
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
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
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
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CM, exiEvent.EventKind);
        Assert.AreEqual(" Good? ", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        Assert.AreEqual(5, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(7, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AC", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
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
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_PI, exiEvent.EventKind);
        Assert.AreEqual("eg", exiEvent.Name);
        Assert.AreEqual("Good! ", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
        Assert.AreEqual(6, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(7, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AC", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
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
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(7, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
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
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
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
        ++n_events;

        Assert.AreEqual(8, n_events);
      }
    }

    /// <summary>
    /// Exercise CM and PI in "sequence" group.
    /// 
    /// Schema:
    /// <xsd:element name="D">
    ///   <xsd:complexType>
    ///     <xsd:choice>
    ///       <xsd:element name="E" minOccurs="2" maxOccurs="2"/>
    ///       <xsd:element name="F"/>
    ///     </xsd:choice>
    ///   </xsd:complexType>
    /// </xsd:element>
    /// 
    /// Instance:
    /// <D><E/><!-- Good? --><?eg Good! ?></D>
    /// </summary>
    [Test]
    public virtual void testCommentPI_03() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/choiceGroup.xsc", this);

      short options = GrammarOptions.DEFAULT_OPTIONS;
      options = GrammarOptions.addCM(options);
      options = GrammarOptions.addPI(options);

      GrammarCache grammarCache = new GrammarCache(corpus, options);

      const string xmlString =
        "<foo:D xmlns:foo='urn:foo'><E/><!-- Good? --><?eg Good! ?></foo:D>";

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
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("D", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("E", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("E", eventType.name);
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
        Assert.AreEqual("F", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
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
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(4, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(12, eventTypeList.Length);
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
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        eventType = eventTypeList.item(10);
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        eventType = eventTypeList.item(11);
        Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CM, exiEvent.EventKind);
        Assert.AreEqual(" Good? ", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        Assert.AreEqual(4, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(6, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("E", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_PI, exiEvent.EventKind);
        Assert.AreEqual("eg", exiEvent.Name);
        Assert.AreEqual("Good! ", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
        Assert.AreEqual(5, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(6, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("E", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(6, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("E", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
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
        ++n_events;

        Assert.AreEqual(8, n_events);
      }
    }

    /// <summary>
    /// Exercise CM and PI in ComplexContentGrammar.
    /// 
    /// Schema:
    /// <xsd:element name="D">
    ///   <xsd:complexType>
    ///     <xsd:choice>
    ///       <xsd:element name="E" minOccurs="2" maxOccurs="2"/>
    ///       <xsd:element name="F"/>
    ///     </xsd:choice>
    ///   </xsd:complexType>
    /// </xsd:element>
    /// 
    /// Instance:
    /// <D> <!-- Good? --><F/><?eg Good! ?></D>
    /// </summary>
    [Test]
    public virtual void testCommentPI_04() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/choiceGroup.xsc", this);

      short options = GrammarOptions.DEFAULT_OPTIONS;
      options = GrammarOptions.addCM(options);
      options = GrammarOptions.addPI(options);

      GrammarCache grammarCache = new GrammarCache(corpus, options);

      const string xmlString =
        "<foo:D xmlns:foo='urn:foo'> <!-- Good? --><F/><?eg Good! ?></foo:D>";

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
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("D", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
          eventTypeList = eventType.EventTypeList;
          Assert.IsNull(eventTypeList.EE);
          ++n_events;

          if (preserveWhitespaces) {
            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual(" ", exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
            Assert.AreEqual(8, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(11, eventTypeList.Length);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_SCHEMA_NIL, eventType.itemType);
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
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
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
            ++n_events;
          }

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_CM, exiEvent.EventKind);
          Assert.AreEqual(" Good? ", exiEvent.Characters.makeString());
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
          if (preserveWhitespaces) {
            Assert.AreEqual(5, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(7, eventTypeList.Length);
            eventType = eventTypeList.item(0);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("E", eventType.name);
            Assert.AreEqual("", eventType.uri);
            eventType = eventTypeList.item(1);
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("F", eventType.name);
            Assert.AreEqual("", eventType.uri);
            eventType = eventTypeList.item(2);
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
            eventType = eventTypeList.item(3);
            Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
            eventType = eventTypeList.item(4);
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
            eventType = eventTypeList.item(6);
            Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
          }
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("F", exiEvent.Name);
          Assert.AreEqual("", exiEvent.URI);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("F", eventType.name);
          Assert.AreEqual("", eventType.uri);
          Assert.AreEqual(1, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(7, eventTypeList.Length);
          eventType = eventTypeList.item(0);
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("E", eventType.name);
          Assert.AreEqual("", eventType.uri);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          eventType = eventTypeList.item(5);
          Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
          eventType = eventTypeList.item(6);
          Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(4, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(12, eventTypeList.Length);
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
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(9);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          eventType = eventTypeList.item(10);
          Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
          eventType = eventTypeList.item(11);
          Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_PI, exiEvent.EventKind);
          Assert.AreEqual("eg", exiEvent.Name);
          Assert.AreEqual("Good! ", exiEvent.Characters.makeString());
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
          Assert.AreEqual(4, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(5, eventTypeList.Length);
          eventType = eventTypeList.item(0);
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
          Assert.AreEqual(0, eventType.Index);
          eventTypeList = eventType.EventTypeList;
          Assert.AreEqual(5, eventTypeList.Length);
          eventType = eventTypeList.item(1);
          Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
          eventType = eventTypeList.item(2);
          Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          eventType = eventTypeList.item(3);
          Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
          eventType = eventTypeList.item(4);
          Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
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
          ++n_events;

          Assert.AreEqual(preserveWhitespaces ? 9 : 8, n_events);
        }
      }
    }

    /// <summary>
    /// Exercise CM in ElementGrammar.
    /// 
    /// Schema:
    /// <xsd:complexType name="B">
    ///   <xsd:sequence>
    ///     <xsd:element ref="foo:AB"/>
    ///     <xsd:element ref="foo:AC" minOccurs="0" maxOccurs="2"/>
    ///     <xsd:element ref="foo:AD" minOccurs="0"/>
    ///   </xsd:sequence>
    /// </xsd:complexType>
    /// 
    /// Instance:
    /// <B><!-- Good? --><AB/></B>
    /// </summary>
    [Test]
    public virtual void testCommentPI_05() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      short options = GrammarOptions.DEFAULT_OPTIONS;
      options = GrammarOptions.addCM(options);
      options = GrammarOptions.addPI(options);

      GrammarCache grammarCache = new GrammarCache(corpus, options);

      const string xmlString =
        "<B xmlns='urn:foo'><!-- Good? --><AB/></B>";

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
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CM, exiEvent.EventKind);
        Assert.AreEqual(" Good? ", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        Assert.AreEqual(8, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(10, eventTypeList.Length);
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
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
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
        Assert.AreEqual(6, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
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
        ++n_events;

        Assert.AreEqual(7, n_events);
      }
    }

    /// <summary>
    /// Exercise PI in ElementGrammar.
    /// 
    /// Schema:
    /// <xsd:complexType name="B">
    ///   <xsd:sequence>
    ///     <xsd:element ref="foo:AB"/>
    ///     <xsd:element ref="foo:AC" minOccurs="0" maxOccurs="2"/>
    ///     <xsd:element ref="foo:AD" minOccurs="0"/>
    ///   </xsd:sequence>
    /// </xsd:complexType>
    /// 
    /// Instance:
    /// <B><?eg Good! ?><AB/></B>
    /// </summary>
    [Test]
    public virtual void testCommentPI_06() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      short options = GrammarOptions.DEFAULT_OPTIONS;
      options = GrammarOptions.addCM(options);
      options = GrammarOptions.addPI(options);

      GrammarCache grammarCache = new GrammarCache(corpus, options);

      const string xmlString =
        "<B xmlns='urn:foo'><?eg Good! ?><AB/></B>";

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
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_PI, exiEvent.EventKind);
        Assert.AreEqual("eg", exiEvent.Name);
        Assert.AreEqual("Good! ", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
        Assert.AreEqual(9, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(10, eventTypeList.Length);
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
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
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
        Assert.AreEqual(6, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
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
        ++n_events;

        Assert.AreEqual(7, n_events);
      }
    }

    /// <summary>
    /// Exercise CM in ElementTagGrammar.
    /// 
    /// Schema:
    /// <xsd:complexType name="B">
    ///   <xsd:sequence>
    ///     <xsd:element ref="foo:AB"/>
    ///     <xsd:element ref="foo:AC" minOccurs="0" maxOccurs="2"/>
    ///     <xsd:element ref="foo:AD" minOccurs="0"/>
    ///   </xsd:sequence>
    /// </xsd:complexType>
    /// 
    /// Instance:
    /// <B xsi:type='B'><!-- Good? --><AB/></B>
    /// </summary>
    [Test]
    public virtual void testCommentPI_07() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      short options = GrammarOptions.DEFAULT_OPTIONS;
      options = GrammarOptions.addCM(options);
      options = GrammarOptions.addPI(options);

      GrammarCache grammarCache = new GrammarCache(corpus, options);

      const string xmlString =
        "<foo:B xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:type='foo:B'>" +
        "<!-- Good? --><foo:AB/></foo:B>";

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
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
        Assert.AreEqual("type", exiEvent.Name);
        Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
        Assert.AreEqual("B", ((EXIEventSchemaType)exiEvent).TypeName);
        Assert.AreEqual("urn:foo", ((EXIEventSchemaType)exiEvent).TypeURI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(10, eventTypeList.Length);
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
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CM, exiEvent.EventKind);
        Assert.AreEqual(" Good? ", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        Assert.AreEqual(8, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(10, eventTypeList.Length);
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
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
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
        Assert.AreEqual(6, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
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
        ++n_events;

        Assert.AreEqual(8, n_events);
      }
    }

    /// <summary>
    /// Exercise CM in ElementTagGrammar.
    /// 
    /// Schema:
    /// <xsd:complexType name="B">
    ///   <xsd:sequence>
    ///     <xsd:element ref="foo:AB"/>
    ///     <xsd:element ref="foo:AC" minOccurs="0" maxOccurs="2"/>
    ///     <xsd:element ref="foo:AD" minOccurs="0"/>
    ///   </xsd:sequence>
    /// </xsd:complexType>
    /// 
    /// Instance:
    /// <B xsi:type='B'><?eg Good! ?><AB/></B>
    /// </summary>
    [Test]
    public virtual void testCommentPI_08() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      short options = GrammarOptions.DEFAULT_OPTIONS;
      options = GrammarOptions.addCM(options);
      options = GrammarOptions.addPI(options);

      GrammarCache grammarCache = new GrammarCache(corpus, options);

      const string xmlString =
        "<foo:B xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:type='foo:B'>" +
        "<?eg Good! ?><foo:AB/></foo:B>";

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
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
        Assert.AreEqual("type", exiEvent.Name);
        Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
        Assert.AreEqual("B", ((EXIEventSchemaType)exiEvent).TypeName);
        Assert.AreEqual("urn:foo", ((EXIEventSchemaType)exiEvent).TypeURI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(10, eventTypeList.Length);
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
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_PI, exiEvent.EventKind);
        Assert.AreEqual("eg", exiEvent.Name);
        Assert.AreEqual("Good! ", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
        Assert.AreEqual(9, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(10, eventTypeList.Length);
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
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
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
        Assert.AreEqual(6, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
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
        ++n_events;

        Assert.AreEqual(8, n_events);
      }
    }

    /// <summary>
    /// Exercise CM and PI in ElementTagGrammar.
    /// 
    /// Schema:
    /// <xsd:element name="AB" type="xsd:anySimpleType"/>
    /// 
    /// Instance:
    /// The element <AC/> is not expected.
    /// <AB><AC/><!-- Good? -->abc<?eg Good! ?></AB>
    /// </summary>
    [Test]
    public virtual void testCommentPI_09() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      short options = GrammarOptions.DEFAULT_OPTIONS;
      options = GrammarOptions.addCM(options);
      options = GrammarOptions.addPI(options);

      GrammarCache grammarCache = new GrammarCache(corpus, options);

      const string xmlString = "<AB xmlns='urn:foo'><AC/><!-- Good? -->abc<?eg Good! ?></AB>\n";

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
        Assert.AreEqual("AB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AC", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        Assert.AreEqual(6, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(10, eventTypeList.Length);
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
        eventType = eventTypeList.item(7);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CM, exiEvent.EventKind);
        Assert.AreEqual(" Good? ", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        Assert.AreEqual(4, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(6, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("abc", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(6, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_PI, exiEvent.EventKind);
        Assert.AreEqual("eg", exiEvent.Name);
        Assert.AreEqual("Good! ", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
        Assert.AreEqual(4, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
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
        ++n_events;

        Assert.AreEqual(9, n_events);
      }
    }

    /// <summary>
    /// Exercise CM and PI in EmptyContentGrammar.
    /// 
    /// Schema:
    /// <xsd:element name="AB" type="xsd:anySimpleType"/>
    /// 
    /// Instance:
    /// The element <AC/> is not expected.
    /// <AB xsi:nil='true'><AC/><!-- Good? --><?eg Good! ?></AB>
    /// </summary>
    [Test]
    public virtual void testCommentPI_10() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.xsc", this);

      short options = GrammarOptions.DEFAULT_OPTIONS;
      options = GrammarOptions.addCM(options);
      options = GrammarOptions.addPI(options);

      GrammarCache grammarCache = new GrammarCache(corpus, options);

      const string xmlString =
        "<AB xmlns='urn:foo' xsi:nil='true' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>" +
          "<AC/><!-- Good? --><?eg Good! ?>" +
        "</AB>\n";

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
        Assert.AreEqual("AB", eventType.name);
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
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(10, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventType.itemType);
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
        eventType = eventTypeList.item(8);
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        eventType = eventTypeList.item(9);
        Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AC", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(7, eventTypeList.Length);
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
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        eventType = eventTypeList.item(5);
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        eventType = eventTypeList.item(6);
        Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(EventCode.EVENT_CODE_DEPTH_TWO, eventType.Depth);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CM, exiEvent.EventKind);
        Assert.AreEqual(" Good? ", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_PI, exiEvent.EventKind);
        Assert.AreEqual("eg", exiEvent.Name);
        Assert.AreEqual("Good! ", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
        Assert.AreEqual(4, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_CM, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_PI, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
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
        ++n_events;

        Assert.AreEqual(9, n_events);
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

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      const string xmlString =
        "<A xmlns='urn:foo' xsi:nil='false' " +
        "   xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>" +
          "<B/>" +
        "</A>";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        byte[] bts;
        MemoryStream baos;

        baos = new MemoryStream();
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
        Assert.AreEqual("A", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_NL, exiEvent.EventKind);
        Assert.AreEqual("nil", exiEvent.Name);
        Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
        Assert.IsFalse(((EXIEventSchemaNil)exiEvent).Nilled);
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
        Assert.AreEqual("B", eventType.name);
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
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("B", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
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
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(8, eventTypeList.Length);
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

        Assert.AreEqual(7, n_events);
      }
    }

  }

}