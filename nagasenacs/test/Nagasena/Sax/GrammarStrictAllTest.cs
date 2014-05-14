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
using EventTypeList = Nagasena.Proc.Common.EventTypeList;
using GrammarOptions = Nagasena.Proc.Common.GrammarOptions;
using GrammarCache = Nagasena.Proc.Grammars.GrammarCache;
using Scanner = Nagasena.Proc.IO.Scanner;
using EXISchema = Nagasena.Schema.EXISchema;
using TestBase = Nagasena.Schema.TestBase;
using EXISchemaFactoryTestUtil = Nagasena.Scomp.EXISchemaFactoryTestUtil;

namespace Nagasena.Sax {

  [TestFixture]
  public class GrammarStrictAllTest : TestBase {

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
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.gram", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      const string xmlString =
        "<C xmlns='urn:foo'>\n" +
        "  <AB/><AC/>\n" +
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
        Assert.AreEqual("C", eventType.name);
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
        Assert.AreEqual(3, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AC", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

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
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);

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
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

        exiEvent = exiEventList[6];
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        Assert.AreEqual(1, eventType.Index); // because of xsi:type
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);

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
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AC", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);

        exiEvent = exiEventList[9];
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
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/testStates/acceptance.gram", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      const string xmlString =
        "<C xmlns='urn:foo'>\n" +
        "  <AC/><AB/>\n" +
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
        Assert.AreEqual(1, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

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
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);

        exiEvent = exiEventList[5];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AB", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AC", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

        exiEvent = exiEventList[6];
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        Assert.AreEqual(1, eventType.Index); // because of xsi:type
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        Assert.AreEqual(EventType.ITEM_SCHEMA_TYPE, eventTypeList.item(0).itemType);

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
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(3, eventTypeList.Length);
        Assert.IsNotNull(eventTypeList.EE);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AB", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("AC", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);

        exiEvent = exiEventList[9];
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
    /// <xsd:complexType name="C">
    ///   <xsd:all>
    ///     <xsd:element ref="foo:AB" minOccurs="0" />
    ///     <xsd:element ref="foo:AC" />
    ///   </xsd:all>
    /// </xsd:complexType>
    /// 
    /// <xsd:element name="C" type="foo:C"/>
    /// 
    /// <C>
    ///   <AC/><AB/><AC/>  
    /// </C>
    /// </summary>
    [Test]
    public virtual void testDecodeAll_03() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/interop/schemaInformedGrammar/acceptance.gram", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;

      decoder.AlignmentType = AlignmentType.bitPacked;

      // <C xmlns='urn:foo'><AC/><AB/><AC/></C>
      Uri url = resolveSystemIdAsURL("/interop/schemaInformedGrammar/declaredProductions/all-03.bitPacked");
      FileStream inputStream = new FileStream(url.LocalPath, FileMode.Open);

      decoder.GrammarCache = grammarCache;
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

      Assert.AreEqual(13, n_events);

      EventType eventType;

      exiEvent = exiEventList[0];
      Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);

      exiEvent = exiEventList[1];
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      Assert.AreEqual("C", eventType.name);
      Assert.AreEqual("urn:foo", eventType.uri);

      exiEvent = exiEventList[2];
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      Assert.AreEqual("AC", exiEvent.Name);
      Assert.AreEqual("urn:foo", exiEvent.URI);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      Assert.AreEqual("AC", eventType.name);
      Assert.AreEqual("urn:foo", eventType.uri);

      exiEvent = exiEventList[3];
      Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
      Assert.AreEqual("", exiEvent.Characters.makeString());
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);

      exiEvent = exiEventList[4];
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

      exiEvent = exiEventList[5];
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      Assert.AreEqual("AB", exiEvent.Name);
      Assert.AreEqual("urn:foo", exiEvent.URI);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      Assert.AreEqual("AB", eventType.name);
      Assert.AreEqual("urn:foo", eventType.uri);

      exiEvent = exiEventList[6];
      Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
      Assert.AreEqual("", exiEvent.Characters.makeString());
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);

      exiEvent = exiEventList[7];
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

      exiEvent = exiEventList[8];
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      Assert.AreEqual("AC", exiEvent.Name);
      Assert.AreEqual("urn:foo", exiEvent.URI);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      Assert.AreEqual("AC", eventType.name);
      Assert.AreEqual("urn:foo", eventType.uri);

      exiEvent = exiEventList[9];
      Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
      Assert.AreEqual("", exiEvent.Characters.makeString());
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);

      exiEvent = exiEventList[10];
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

      exiEvent = exiEventList[11];
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

      exiEvent = exiEventList[12];
      Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
    }

  }

}