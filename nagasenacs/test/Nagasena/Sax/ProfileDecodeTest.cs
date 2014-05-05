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
using XmlUriConst = Nagasena.Proc.Common.XmlUriConst;
using EXIEventSchemaType = Nagasena.Proc.Events.EXIEventSchemaType;
using GrammarCache = Nagasena.Proc.Grammars.GrammarCache;
using Scanner = Nagasena.Proc.IO.Scanner;
using EmptySchema = Nagasena.Schema.EmptySchema;
using TestBase = Nagasena.Schema.TestBase;
using Event = Org.W3C.Exi.Ttf.Event;
using SAXRecorder = Org.W3C.Exi.Ttf.Sax.SAXRecorder;

namespace Nagasena.Sax {

  [TestFixture]
  public class ProfileDecodeTest : TestBase {

    ///////////////////////////////////////////////////////////////////////////
    // Test cases
    ///////////////////////////////////////////////////////////////////////////

    /// <summary>
    /// <A><A/></A> 
    /// </summary>
    [Test]
    public virtual void testNestedA_01() {

      GrammarCache grammarCache = new GrammarCache(EmptySchema.EXISchema, GrammarOptions.STRICT_OPTIONS);

      String[] exiFiles = { 
        "/profile/nestedA_01.bitPacked",
        "/profile/nestedA_01.byteAligned",
        "/profile/nestedA_01_profile.bitPacked",
        "/profile/nestedA_01_profile.byteAligned"
      };

      AlignmentType[] alignments =  { 
        AlignmentType.bitPacked,
        AlignmentType.byteAligned,
        AlignmentType.bitPacked,
        AlignmentType.byteAligned
      };

      bool[] profiled = {
        false,
        false,
        true,
        true
      };

      for (int i = 0; i < exiFiles.Length; i++) {

        EXIReader decoder = new EXIReader();
        decoder.AlignmentType = alignments[i];
        decoder.GrammarCache = grammarCache;

        Uri url = resolveSystemIdAsURL(exiFiles[i]);
        FileStream inputStream = new FileStream(url.LocalPath, FileMode.Open);
        try {
          InputSource inputSource = new InputSource<Stream>(inputStream, url.ToString());
          List<Event> exiEventList = new List<Event>();
          SAXRecorder saxRecorder = new SAXRecorder(exiEventList, true);
          decoder.ContentHandler = saxRecorder;
          decoder.LexicalHandler = saxRecorder;

          decoder.Parse(inputSource);

          Assert.AreEqual(profiled[i] ? 14 : 10, exiEventList.Count);

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

          if (profiled[i]) {
            saxEvent = exiEventList[n++];
            Assert.AreEqual(Event.NAMESPACE, saxEvent.type);
            Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_URI, saxEvent.@namespace);
            Assert.AreEqual("p0", saxEvent.name);
          }

          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.START_ELEMENT, saxEvent.type);
          Assert.AreEqual("", saxEvent.@namespace);
          Assert.AreEqual("A", saxEvent.localName);

          if (profiled[i]) {
            saxEvent = exiEventList[n++];
            Assert.AreEqual(Event.ATTRIBUTE, saxEvent.type);
            Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, saxEvent.@namespace);
            Assert.AreEqual("type", saxEvent.localName);
            Assert.AreEqual("p0:anyType", saxEvent.stringValue);
          }

          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.START_ELEMENT, saxEvent.type);
          Assert.AreEqual("", saxEvent.@namespace);
          Assert.AreEqual("A", saxEvent.name);

          if (profiled[i]) {
            saxEvent = exiEventList[n++];
            Assert.AreEqual(Event.ATTRIBUTE, saxEvent.type);
            Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, saxEvent.@namespace);
            Assert.AreEqual("type", saxEvent.localName);
            Assert.AreEqual("p0:anyType", saxEvent.stringValue);
          }

          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.END_ELEMENT, saxEvent.type);
          Assert.AreEqual("", saxEvent.@namespace);
          Assert.AreEqual("A", saxEvent.name);

          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.END_ELEMENT, saxEvent.type);
          Assert.AreEqual("", saxEvent.@namespace);
          Assert.AreEqual("A", saxEvent.name);

          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
          Assert.AreEqual("xml", saxEvent.name);

          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
          Assert.AreEqual("xsi", saxEvent.name);

          saxEvent = exiEventList[n++];
          Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
          Assert.AreEqual("xsd", saxEvent.name);

          if (profiled[i]) {
            saxEvent = exiEventList[n++];
            Assert.AreEqual(Event.END_NAMESPACE, saxEvent.type);
            Assert.AreEqual("p0", saxEvent.name);
          }

          Assert.AreEqual(exiEventList.Count, n);
        }
        finally {
          inputStream.Close();
        }
      }
    }

    /// <summary>
    /// <A><B/><B/><B/></A>
    /// See testcase xsitype-02 in config/testCases-profile/xsiType.xml
    /// </summary>
    [Test]
    public virtual void testXsiType_02() {

      GrammarCache grammarCache = new GrammarCache(EmptySchema.EXISchema, GrammarOptions.STRICT_OPTIONS);

      String[] exiFiles = { 
        "/profile/xsiType-02_profile.bitPacked",
        "/profile/xsiType-02_profile.byteAligned"
      };

      AlignmentType[] alignments =  { 
        AlignmentType.bitPacked,
        AlignmentType.byteAligned
      };
    
      for (int i = 0; i < exiFiles.Length; i++) {
        AlignmentType alignment = alignments[i];
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        decoder.AlignmentType = alignment;

        Uri url = resolveSystemIdAsURL(exiFiles[i]);
        FileStream inputStream = new FileStream(url.LocalPath, FileMode.Open);

        decoder.GrammarCache = grammarCache;
        decoder.InputStream = inputStream;
        scanner = decoder.processHeader();

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

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("A", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("B", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        Assert.AreEqual(3, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
        Assert.AreEqual("anyType", ((EXIEventSchemaType)exiEvent).TypeName);
        Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_URI, ((EXIEventSchemaType)exiEvent).TypeURI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_AT, eventType.itemType);
        Assert.AreEqual("type", eventType.name);
        Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(4, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("B", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(4, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
        Assert.AreEqual("anyType", ((EXIEventSchemaType)exiEvent).TypeName);
        Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_URI, ((EXIEventSchemaType)exiEvent).TypeURI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_AT_WC_ANY_UNTYPED, eventType.itemType);
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(5, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_AT, eventType.itemType);
        Assert.AreEqual("type", eventType.name);
        Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, eventType.uri);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(4);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        Assert.AreEqual(2, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(4, eventTypeList.Length);
        eventType = eventTypeList.item(0);
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_SCHEMA_WC_ANY, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH_MIXED, eventType.itemType);

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("B", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(0, eventType.Index);
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("", eventType.uri);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(4, eventTypeList.Length);
        eventType = eventTypeList.item(1);
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        eventType = eventTypeList.item(2);
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);
        eventType = eventTypeList.item(3);
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
        Assert.AreEqual("anyType", ((EXIEventSchemaType)exiEvent).TypeName);
        Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_URI, ((EXIEventSchemaType)exiEvent).TypeURI);

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);

        Assert.IsNull(scanner.nextEvent());

        inputStream.Close();
      }
    }

  }

}