using System;
using System.IO;
using System.Xml;

using NUnit.Framework;

using Org.System.Xml.Sax;

using EXIDecoder = Nagasena.Proc.EXIDecoder;
using AlignmentType = Nagasena.Proc.Common.AlignmentType;
using EventDescription = Nagasena.Proc.Common.EventDescription;
using EventDescription_Fields = Nagasena.Proc.Common.EventDescription_Fields;
using EventType = Nagasena.Proc.Common.EventType;
using GrammarOptions = Nagasena.Proc.Common.GrammarOptions;
using XmlUriConst = Nagasena.Proc.Common.XmlUriConst;
using EXIEventNS = Nagasena.Proc.Events.EXIEventNS;
using EXIEventSchemaType = Nagasena.Proc.Events.EXIEventSchemaType;
using GrammarCache = Nagasena.Proc.Grammars.GrammarCache;
using Scanner = Nagasena.Proc.IO.Scanner;
using EXISchema = Nagasena.Schema.EXISchema;
using EXISchemaConst = Nagasena.Schema.EXISchemaConst;
using EXISchemaUtil = Nagasena.Schema.EXISchemaUtil;
using TestBase = Nagasena.Schema.TestBase;
using EXISchemaFactoryTestUtil = Nagasena.Scomp.EXISchemaFactoryTestUtil;

namespace Nagasena.Sax {

  [TestFixture]
  [Category("Enable_Compression")]
  public class EnumerationTest : TestBase {

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
    /// A valid dateTime value matching ITEM_SCHEMA_CH where the associated
    /// datatype is an enumeration of xsd:dateTime.
    /// </summary>
    [Test]
    public virtual void testDateTime() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string[] xmlStrings;
      String[] originalValues = {
          " \t\r 2003-03-19T12:20:00-06:00\n",
          "2003-03-20T14:20:00-04:00",
          "2003-03-21T18:20:00Z",
          "2013-06-04T05:00:00Z",
          "2013-06-03T24:00:00-06:00",
          "2012-06-30T23:59:60Z",
          "----------",
          "xyz",
      };
      String[] parsedOriginalValues = {
          " \t\n 2003-03-19T12:20:00-06:00\n",
          "2003-03-20T14:20:00-04:00",
          "2003-03-21T18:20:00Z",
          "2013-06-04T05:00:00Z",
          "2013-06-03T24:00:00-06:00",
          "2012-06-30T23:59:60Z",
          "----------",
          "xyz",
      };
      String[] resultValues = {
          "2003-03-19T13:20:00-05:00",
          "2003-03-20T13:20:00-05:00",
          "2003-03-21T13:20:00-05:00",
          "2013-06-03T24:00:00-05:00",
          "2013-06-04T06:00:00Z",
          "2012-07-01T00:00:00Z",
          "----------",
          "xyz",
      };

      int i;
      xmlStrings = new string[originalValues.Length];
      for (i = 0; i < originalValues.Length; i++) {
        xmlStrings[i] = "<foo:DateTimeDerived xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:DateTimeDerived>\n";
      };

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveLexicalValues in new bool[] { false, true }) {
          string[] values = preserveLexicalValues ? parsedOriginalValues : resultValues;
          bool isValidValue = true;
          for (i = 0; i < xmlStrings.Length; i++) {
            string originalValue = xmlStrings[i];
            if (originalValue.Contains("----------")) {
              isValidValue = false;
              continue;
            }

            Transmogrifier encoder = new Transmogrifier();
            EXIDecoder decoder = new EXIDecoder();
            Scanner scanner;

            encoder.AlignmentType = alignment;
            decoder.AlignmentType = alignment;

            encoder.PreserveLexicalValues = preserveLexicalValues;
            decoder.PreserveLexicalValues = preserveLexicalValues;

            encoder.GrammarCache = grammarCache;
            MemoryStream baos = new MemoryStream();
            encoder.OutputStream = baos;

            byte[] bts;
            int n_events;

            try {
              encoder.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));
            }
            catch (TransmogrifierException) {
              Assert.IsTrue(!preserveLexicalValues && !isValidValue);
              continue;
            }
            Assert.IsTrue(preserveLexicalValues || isValidValue);

            bts = baos.ToArray();

            decoder.GrammarCache = grammarCache;
            decoder.InputStream = new MemoryStream(bts);
            scanner = decoder.processHeader();

            EventDescription exiEvent;
            n_events = 0;

            EventType eventType;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SD, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("DateTimeDerived", eventType.name);
            Assert.AreEqual("urn:foo", eventType.uri);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual(values[i], exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
            if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
              int tp = scanner.currentState.contentDatatype;
              int builtinType = corpus.getBaseTypeOfSimpleType(tp);
              Assert.AreEqual(EXISchemaConst.DATETIME_TYPE, corpus.getSerialOfType(builtinType));
            }
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
            ++n_events;

            Assert.AreEqual(5, n_events);
          }
        }
      }
    }

    /// <summary>
    /// A valid date value matching ITEM_SCHEMA_CH where the associated
    /// datatype is an enumeration of xsd:date.
    /// </summary>
    [Test]
    public virtual void testDate() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

      string[] xmlStrings;
      String[] originalValues = {
          " \t\r 2003-03-19-05:00\n",
          "2003-03-21-05:00",
          "2003-03-23-05:00"
      };
      String[] parsedOriginalValues = {
          " \t\n 2003-03-19-05:00\n",
          "2003-03-21-05:00",
          "2003-03-23-05:00"
      };
      String[] resultValues = {
          "2003-03-19-05:00",
          "2003-03-21-05:00",
          "2003-03-23-05:00"
      };

      int i;
      xmlStrings = new string[originalValues.Length];
      for (i = 0; i < originalValues.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " + "xsi:type='foo:dateDerived' >" + originalValues[i] + "</foo:A>\n";
      };

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveLexicalValues in new bool[] { true, false }) {
          string[] values = preserveLexicalValues ? parsedOriginalValues : resultValues;
          for (i = 0; i < xmlStrings.Length; i++) {
            Transmogrifier encoder = new Transmogrifier();
            EXIDecoder decoder = new EXIDecoder();
            Scanner scanner;

            encoder.AlignmentType = alignment;
            decoder.AlignmentType = alignment;

            encoder.PreserveLexicalValues = preserveLexicalValues;
            decoder.PreserveLexicalValues = preserveLexicalValues;

            encoder.GrammarCache = grammarCache;
            MemoryStream baos = new MemoryStream();
            encoder.OutputStream = baos;

            byte[] bts;
            int n_events;

            encoder.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));

            bts = baos.ToArray();

            decoder.GrammarCache = grammarCache;
            decoder.InputStream = new MemoryStream(bts);
            scanner = decoder.processHeader();

            EventDescription exiEvent;
            n_events = 0;

            EventType eventType;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SD, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("A", eventType.name);
            Assert.AreEqual("urn:foo", eventType.uri);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
            Assert.AreEqual("foo", exiEvent.Prefix);
            Assert.AreEqual("urn:foo", exiEvent.URI);
            Assert.IsTrue(((EXIEventNS)exiEvent).LocalElementNs);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
            Assert.AreEqual("xsi", exiEvent.Prefix);
            Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
            Assert.IsFalse(((EXIEventNS)exiEvent).LocalElementNs);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
            Assert.AreEqual("dateDerived", ((EXIEventSchemaType)exiEvent).TypeName);
            Assert.AreEqual("urn:foo", ((EXIEventSchemaType)exiEvent).TypeURI);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual(values[i], exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
            if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
              int tp = scanner.currentState.contentDatatype;
              int builtinType = corpus.getBaseTypeOfSimpleType(tp);
              Assert.AreEqual(EXISchemaConst.DATE_TYPE, corpus.getSerialOfType(builtinType));
            }
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
            ++n_events;

            Assert.AreEqual(8, n_events);
          }
        }
      }
    }

    /// <summary>
    /// A valid time value matching ITEM_SCHEMA_CH where the associated
    /// datatype is an enumeration of xsd:time.
    /// </summary>
    [Test]
    public virtual void testTime() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

      string[] xmlStrings;
      String[] originalValues = {
          " \t\r 12:20:00-06:00\n",
          "14:22:00-04:00",
          "18:24:00Z"
      };
      String[] parsedOriginalValues = {
          " \t\n 12:20:00-06:00\n",
          "14:22:00-04:00",
          "18:24:00Z"
      };
      String[] resultValues = {
          "13:20:00-05:00",
          "13:22:00-05:00",
          "13:24:00-05:00"
      };

      int i;
      xmlStrings = new string[originalValues.Length];
      for (i = 0; i < originalValues.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " + "xsi:type='foo:timeDerived' >" + originalValues[i] + "</foo:A>\n";
      };

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveLexicalValues in new bool[] { true, false }) {
          string[] values = preserveLexicalValues ? parsedOriginalValues : resultValues;
          for (i = 0; i < xmlStrings.Length; i++) {
            Transmogrifier encoder = new Transmogrifier();
            EXIDecoder decoder = new EXIDecoder();
            Scanner scanner;

            encoder.AlignmentType = alignment;
            decoder.AlignmentType = alignment;

            encoder.PreserveLexicalValues = preserveLexicalValues;
            decoder.PreserveLexicalValues = preserveLexicalValues;

            encoder.GrammarCache = grammarCache;
            MemoryStream baos = new MemoryStream();
            encoder.OutputStream = baos;

            byte[] bts;
            int n_events;

            encoder.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));

            bts = baos.ToArray();

            decoder.GrammarCache = grammarCache;
            decoder.InputStream = new MemoryStream(bts);
            scanner = decoder.processHeader();

            EventDescription exiEvent;
            n_events = 0;

            EventType eventType;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SD, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("A", eventType.name);
            Assert.AreEqual("urn:foo", eventType.uri);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
            Assert.AreEqual("foo", exiEvent.Prefix);
            Assert.AreEqual("urn:foo", exiEvent.URI);
            Assert.IsTrue(((EXIEventNS)exiEvent).LocalElementNs);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
            Assert.AreEqual("xsi", exiEvent.Prefix);
            Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
            Assert.IsFalse(((EXIEventNS)exiEvent).LocalElementNs);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
            Assert.AreEqual("timeDerived", ((EXIEventSchemaType)exiEvent).TypeName);
            Assert.AreEqual("urn:foo", ((EXIEventSchemaType)exiEvent).TypeURI);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual(values[i], exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
            if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
              int tp = scanner.currentState.contentDatatype;
              int builtinType = corpus.getBaseTypeOfSimpleType(tp);
              Assert.AreEqual(EXISchemaConst.TIME_TYPE, corpus.getSerialOfType(builtinType));
            }
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
            ++n_events;

            Assert.AreEqual(8, n_events);
          }
        }
      }
    }

    /// <summary>
    /// A valid gregorian month value matching ITEM_SCHEMA_CH where the associated
    /// datatype is an enumeration of xsd:gYearMonth.
    /// </summary>
    [Test]
    public virtual void testGYearMonth() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

      string[] xmlStrings;
      String[] originalValues = {
          " \t\r 2003-04-05:00\n",
          "2003-06-05:00",
          "2003-08-05:00"
      };
      String[] parsedOriginalValues = {
          " \t\n 2003-04-05:00\n",
          "2003-06-05:00",
          "2003-08-05:00"
      };
      String[] resultValues = {
          "2003-04-05:00",
          "2003-06-05:00",
          "2003-08-05:00"
      };

      int i;
      xmlStrings = new string[originalValues.Length];
      for (i = 0; i < originalValues.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " + "xsi:type='foo:gYearMonthDerived' >" + originalValues[i] + "</foo:A>\n";
      };

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveLexicalValues in new bool[] { true, false }) {
          string[] values = preserveLexicalValues ? parsedOriginalValues : resultValues;
          for (i = 0; i < xmlStrings.Length; i++) {
            Transmogrifier encoder = new Transmogrifier();
            EXIDecoder decoder = new EXIDecoder();
            Scanner scanner;

            encoder.AlignmentType = alignment;
            decoder.AlignmentType = alignment;

            encoder.PreserveLexicalValues = preserveLexicalValues;
            decoder.PreserveLexicalValues = preserveLexicalValues;

            encoder.GrammarCache = grammarCache;
            MemoryStream baos = new MemoryStream();
            encoder.OutputStream = baos;

            byte[] bts;
            int n_events;

            encoder.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));

            bts = baos.ToArray();

            decoder.GrammarCache = grammarCache;
            decoder.InputStream = new MemoryStream(bts);
            scanner = decoder.processHeader();

            EventDescription exiEvent;
            n_events = 0;

            EventType eventType;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SD, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("A", eventType.name);
            Assert.AreEqual("urn:foo", eventType.uri);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
            Assert.AreEqual("foo", exiEvent.Prefix);
            Assert.AreEqual("urn:foo", exiEvent.URI);
            Assert.IsTrue(((EXIEventNS)exiEvent).LocalElementNs);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
            Assert.AreEqual("xsi", exiEvent.Prefix);
            Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
            Assert.IsFalse(((EXIEventNS)exiEvent).LocalElementNs);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
            Assert.AreEqual("gYearMonthDerived", ((EXIEventSchemaType)exiEvent).TypeName);
            Assert.AreEqual("urn:foo", ((EXIEventSchemaType)exiEvent).TypeURI);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual(values[i], exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
            if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
              int tp = scanner.currentState.contentDatatype;
              int builtinType = corpus.getBaseTypeOfSimpleType(tp);
              Assert.AreEqual(EXISchemaConst.G_YEARMONTH_TYPE, corpus.getSerialOfType(builtinType));
            }
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
            ++n_events;

            Assert.AreEqual(8, n_events);
          }
        }
      }
    }

    /// <summary>
    /// A valid gregorian year value matching ITEM_SCHEMA_CH where the associated
    /// datatype is an enumeration of xsd:gYear.
    /// </summary>
    [Test]
    public virtual void testGYear() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

      string[] xmlStrings;
      String[] originalValues = {
          " \t\r 1969+09:00\n",
          "1971+09:00",
          "1973+09:00",
          "0001",
          "0012",
          "0123",
          "12345",
      };
      String[] parsedOriginalValues = {
          " \t\n 1969+09:00\n",
          "1971+09:00",
          "1973+09:00",
          "0001",
          "0012",
          "0123",
          "12345",
      };
      String[] resultValues = {
          "1969+09:00",
          "1971+09:00",
          "1973+09:00",
          "0001",
          "0012",
          "0123",
          "12345",
      };

      int i;
      xmlStrings = new string[originalValues.Length];
      for (i = 0; i < originalValues.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " + "xsi:type='foo:gYearDerived' >" + originalValues[i] + "</foo:A>\n";
      };

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveLexicalValues in new bool[] { true, false }) {
          string[] values = preserveLexicalValues ? parsedOriginalValues : resultValues;
          for (i = 0; i < xmlStrings.Length; i++) {
            Transmogrifier encoder = new Transmogrifier();
            EXIDecoder decoder = new EXIDecoder();
            Scanner scanner;

            encoder.AlignmentType = alignment;
            decoder.AlignmentType = alignment;

            encoder.PreserveLexicalValues = preserveLexicalValues;
            decoder.PreserveLexicalValues = preserveLexicalValues;

            encoder.GrammarCache = grammarCache;
            MemoryStream baos = new MemoryStream();
            encoder.OutputStream = baos;

            byte[] bts;
            int n_events;

            encoder.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));

            bts = baos.ToArray();

            decoder.GrammarCache = grammarCache;
            decoder.InputStream = new MemoryStream(bts);
            scanner = decoder.processHeader();

            EventDescription exiEvent;
            n_events = 0;

            EventType eventType;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SD, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("A", eventType.name);
            Assert.AreEqual("urn:foo", eventType.uri);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
            Assert.AreEqual("foo", exiEvent.Prefix);
            Assert.AreEqual("urn:foo", exiEvent.URI);
            Assert.IsTrue(((EXIEventNS)exiEvent).LocalElementNs);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
            Assert.AreEqual("xsi", exiEvent.Prefix);
            Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
            Assert.IsFalse(((EXIEventNS)exiEvent).LocalElementNs);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
            Assert.AreEqual("gYearDerived", ((EXIEventSchemaType)exiEvent).TypeName);
            Assert.AreEqual("urn:foo", ((EXIEventSchemaType)exiEvent).TypeURI);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual(values[i], exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
            if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
              int tp = scanner.currentState.contentDatatype;
              int builtinType = corpus.getBaseTypeOfSimpleType(tp);
              Assert.AreEqual(EXISchemaConst.G_YEAR_TYPE, corpus.getSerialOfType(builtinType));
            }
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
            ++n_events;

            Assert.AreEqual(8, n_events);
          }
        }
      }
    }

    /// <summary>
    /// A valid gregorian month value matching ITEM_SCHEMA_CH where the associated
    /// datatype is an enumeration of xsd:gMonth.
    /// </summary>
    [Test]
    public virtual void testGMonth() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

      string[] xmlStrings;
      String[] originalValues = {
          " \t\r --07+09:00\n",
          "--09+09:00",
          "--11+09:00"
      };
      String[] parsedOriginalValues = {
          " \t\n --07+09:00\n",
          "--09+09:00",
          "--11+09:00"
      };
      String[] resultValues = {
          "--07+09:00",
          "--09+09:00",
          "--11+09:00"
      };

      int i;
      xmlStrings = new string[originalValues.Length];
      for (i = 0; i < originalValues.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " + "xsi:type='foo:gMonthDerived' >" + originalValues[i] + "</foo:A>\n";
      };

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveLexicalValues in new bool[] { true, false }) {
          string[] values = preserveLexicalValues ? parsedOriginalValues : resultValues;
          for (i = 0; i < xmlStrings.Length; i++) {
            Transmogrifier encoder = new Transmogrifier();
            EXIDecoder decoder = new EXIDecoder();
            Scanner scanner;

            encoder.AlignmentType = alignment;
            decoder.AlignmentType = alignment;

            encoder.PreserveLexicalValues = preserveLexicalValues;
            decoder.PreserveLexicalValues = preserveLexicalValues;

            encoder.GrammarCache = grammarCache;
            MemoryStream baos = new MemoryStream();
            encoder.OutputStream = baos;

            byte[] bts;
            int n_events;

            encoder.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));

            bts = baos.ToArray();

            decoder.GrammarCache = grammarCache;
            decoder.InputStream = new MemoryStream(bts);
            scanner = decoder.processHeader();

            EventDescription exiEvent;
            n_events = 0;

            EventType eventType;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SD, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("A", eventType.name);
            Assert.AreEqual("urn:foo", eventType.uri);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
            Assert.AreEqual("foo", exiEvent.Prefix);
            Assert.AreEqual("urn:foo", exiEvent.URI);
            Assert.IsTrue(((EXIEventNS)exiEvent).LocalElementNs);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
            Assert.AreEqual("xsi", exiEvent.Prefix);
            Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
            Assert.IsFalse(((EXIEventNS)exiEvent).LocalElementNs);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
            Assert.AreEqual("gMonthDerived", ((EXIEventSchemaType)exiEvent).TypeName);
            Assert.AreEqual("urn:foo", ((EXIEventSchemaType)exiEvent).TypeURI);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual(values[i], exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
            if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
              int tp = scanner.currentState.contentDatatype;
              int builtinType = corpus.getBaseTypeOfSimpleType(tp);
              Assert.AreEqual(EXISchemaConst.G_MONTH_TYPE, corpus.getSerialOfType(builtinType));
            }
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
            ++n_events;

            Assert.AreEqual(8, n_events);
          }
        }
      }
    }

    /// <summary>
    /// A valid gregorian date value matching ITEM_SCHEMA_CH where the associated
    /// datatype is an enumeration of xsd:gMonthDay.
    /// </summary>
    [Test]
    public virtual void testGMonthDay() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string[] xmlStrings;
      String[] originalValues = {
          " \t\r --09-16+09:00\n",
          "--09-18+09:00",
          "--09-20+09:00",
          "--02-29+14:00",
          "--04-01+14:00",
          "--03-01+14:00",
      };
      String[] parsedOriginalValues = {
          " \t\n --09-16+09:00\n",
          "--09-18+09:00",
          "--09-20+09:00",
          "--02-29+14:00",
          "--04-01+14:00",
          "--03-01+14:00",
      };
      String[] resultValues = {
          "--09-16+09:00",
          "--09-18+09:00",
          "--09-20+09:00",
          "--02-28-10:00",
          "--03-31-10:00",
          "--02-29-10:00",
      };

      int i;
      xmlStrings = new string[originalValues.Length];
      for (i = 0; i < originalValues.Length; i++) {
        xmlStrings[i] = "<foo:GMonthDayDerived xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:GMonthDayDerived>\n";
      };

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveLexicalValues in new bool[] { false, true }) {
          string[] values = preserveLexicalValues ? parsedOriginalValues : resultValues;
          for (i = 0; i < xmlStrings.Length; i++) {
            Transmogrifier encoder = new Transmogrifier();
            EXIDecoder decoder = new EXIDecoder();
            Scanner scanner;

            encoder.AlignmentType = alignment;
            decoder.AlignmentType = alignment;

            encoder.PreserveLexicalValues = preserveLexicalValues;
            decoder.PreserveLexicalValues = preserveLexicalValues;

            encoder.GrammarCache = grammarCache;
            MemoryStream baos = new MemoryStream();
            encoder.OutputStream = baos;

            byte[] bts;
            int n_events;

            encoder.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));

            bts = baos.ToArray();

            decoder.GrammarCache = grammarCache;
            decoder.InputStream = new MemoryStream(bts);
            scanner = decoder.processHeader();

            EventDescription exiEvent;
            n_events = 0;

            EventType eventType;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SD, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("GMonthDayDerived", eventType.name);
            Assert.AreEqual("urn:foo", eventType.uri);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual(values[i], exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
            if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
              int tp = scanner.currentState.contentDatatype;
              int builtinType = corpus.getBaseTypeOfSimpleType(tp);
              Assert.AreEqual(EXISchemaConst.G_MONTHDAY_TYPE, corpus.getSerialOfType(builtinType));
            }
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
            ++n_events;

            Assert.AreEqual(5, n_events);
          }
        }
      }
    }

    /// <summary>
    /// A valid gregorian date value matching ITEM_SCHEMA_CH where the associated
    /// datatype is an enumeration of xsd:gDay.
    /// </summary>
    [Test]
    public virtual void testGDay() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string[] xmlStrings;
      String[] originalValues = {
          " \t\r ---16+09:00\n",
          "---18+09:00",
          "---20+09:00"
      };
      String[] parsedOriginalValues = {
          " \t\n ---16+09:00\n",
          "---18+09:00",
          "---20+09:00"
      };
      String[] resultValues = {
          "---16+09:00",
          "---18+09:00",
          "---20+09:00"
      };

      int i;
      xmlStrings = new string[originalValues.Length];
      for (i = 0; i < originalValues.Length; i++) {
        xmlStrings[i] = "<foo:GDayDerived xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:GDayDerived>\n";
      };

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveLexicalValues in new bool[] { true, false }) {
          string[] values = preserveLexicalValues ? parsedOriginalValues : resultValues;
          for (i = 0; i < xmlStrings.Length; i++) {
            Transmogrifier encoder = new Transmogrifier();
            EXIDecoder decoder = new EXIDecoder();
            Scanner scanner;

            encoder.AlignmentType = alignment;
            decoder.AlignmentType = alignment;

            encoder.PreserveLexicalValues = preserveLexicalValues;
            decoder.PreserveLexicalValues = preserveLexicalValues;

            encoder.GrammarCache = grammarCache;
            MemoryStream baos = new MemoryStream();
            encoder.OutputStream = baos;

            byte[] bts;
            int n_events;

            encoder.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));

            bts = baos.ToArray();

            decoder.GrammarCache = grammarCache;
            decoder.InputStream = new MemoryStream(bts);
            scanner = decoder.processHeader();

            EventDescription exiEvent;
            n_events = 0;

            EventType eventType;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SD, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("GDayDerived", eventType.name);
            Assert.AreEqual("urn:foo", eventType.uri);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual(values[i], exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
            if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
              int tp = scanner.currentState.contentDatatype;
              int builtinType = corpus.getBaseTypeOfSimpleType(tp);
              Assert.AreEqual(EXISchemaConst.G_DAY_TYPE, corpus.getSerialOfType(builtinType));
            }
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
            ++n_events;

            Assert.AreEqual(5, n_events);
          }
        }
      }
    }

    /// <summary>
    /// A valid duration value matching ITEM_SCHEMA_CH where the associated
    /// datatype is an enumeration of xsd:duration.
    /// </summary>
    [Test]
    public virtual void testDuration() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

      string[] xmlStrings;
      String[] originalValues = {
          " \t\r P14M3DT10H30M\n",
          "P1Y2M4DT9H90M",
          "P1Y2M5DT8H150M"
      };
      String[] parsedOriginalValues = {
          " \t\n P14M3DT10H30M\n",
          "P1Y2M4DT9H90M",
          "P1Y2M5DT8H150M"
      };
      String[] resultValues = {
          "P1Y2M3DT10H30M",
          "P1Y2M4DT10H30M",
          "P1Y2M5DT10H30M"
      };

      int i;
      xmlStrings = new string[originalValues.Length];
      for (i = 0; i < originalValues.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " + "xsi:type='foo:durationDerived' >" + originalValues[i] + "</foo:A>\n";
      };

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveLexicalValues in new bool[] { true, false }) {
          string[] values = preserveLexicalValues ? parsedOriginalValues : resultValues;
          for (i = 0; i < xmlStrings.Length; i++) {
            Transmogrifier encoder = new Transmogrifier();
            EXIDecoder decoder = new EXIDecoder();
            Scanner scanner;

            encoder.AlignmentType = alignment;
            decoder.AlignmentType = alignment;

            encoder.PreserveLexicalValues = preserveLexicalValues;
            decoder.PreserveLexicalValues = preserveLexicalValues;

            encoder.GrammarCache = grammarCache;
            MemoryStream baos = new MemoryStream();
            encoder.OutputStream = baos;

            byte[] bts;
            int n_events;

            encoder.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));

            bts = baos.ToArray();

            decoder.GrammarCache = grammarCache;
            decoder.InputStream = new MemoryStream(bts);
            scanner = decoder.processHeader();

            EventDescription exiEvent;
            n_events = 0;

            EventType eventType;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SD, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("A", eventType.name);
            Assert.AreEqual("urn:foo", eventType.uri);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
            Assert.AreEqual("foo", exiEvent.Prefix);
            Assert.AreEqual("urn:foo", exiEvent.URI);
            Assert.IsTrue(((EXIEventNS)exiEvent).LocalElementNs);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
            Assert.AreEqual("xsi", exiEvent.Prefix);
            Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
            Assert.IsFalse(((EXIEventNS)exiEvent).LocalElementNs);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
            Assert.AreEqual("durationDerived", ((EXIEventSchemaType)exiEvent).TypeName);
            Assert.AreEqual("urn:foo", ((EXIEventSchemaType)exiEvent).TypeURI);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual(XmlConvert.ToTimeSpan(values[i]), XmlConvert.ToTimeSpan(exiEvent.Characters.makeString()));
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
            if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
              int tp = scanner.currentState.contentDatatype;
              int builtinType = corpus.getBaseTypeOfSimpleType(tp);
              Assert.AreEqual(EXISchemaConst.DURATION_TYPE, corpus.getSerialOfType(builtinType));
            }
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
            ++n_events;

            Assert.AreEqual(8, n_events);
          }
        }
      }
    }

    /// <summary>
    /// A valid decimal value matching ITEM_SCHEMA_CH where the associated
    /// datatype is an enumeration of xsd:decimal.
    /// </summary>
    [Test]
    public virtual void testDecimal() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

      string[] xmlStrings;
      String[] originalValues = {
          " \t\r 100.1234567\n",
          "101.2345678",
          "102.3456789",
          "-0",
          "103.abcdefg"
      };
      String[] parsedOriginalValues = {
          " \t\n 100.1234567\n",
          "101.2345678",
          "102.3456789",
          "-0",
          "103.abcdefg"
      };
      String[] resultValues = {
          "100.1234567",
          "101.2345678",
          "102.3456789",
          "0",
          "103.abcdefg"
      };
      const int n_validDecimals = 4;

      int i;
      xmlStrings = new string[originalValues.Length];
      for (i = 0; i < originalValues.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " + "xsi:type='foo:decimalDerived' >" + originalValues[i] + "</foo:A>\n";
      };

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveLexicalValues in new bool[] { true, false }) {
          string[] values = preserveLexicalValues ? parsedOriginalValues : resultValues;
          for (i = 0; i < xmlStrings.Length; i++) {
            Transmogrifier encoder = new Transmogrifier();
            EXIDecoder decoder = new EXIDecoder();
            Scanner scanner;

            encoder.AlignmentType = alignment;
            decoder.AlignmentType = alignment;

            encoder.PreserveLexicalValues = preserveLexicalValues;
            decoder.PreserveLexicalValues = preserveLexicalValues;

            encoder.GrammarCache = grammarCache;
            MemoryStream baos = new MemoryStream();
            encoder.OutputStream = baos;

            byte[] bts;
            int n_events;

            encoder.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));

            bts = baos.ToArray();

            decoder.GrammarCache = grammarCache;
            decoder.InputStream = new MemoryStream(bts);
            scanner = decoder.processHeader();

            EventDescription exiEvent;
            n_events = 0;

            EventType eventType;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SD, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("A", eventType.name);
            Assert.AreEqual("urn:foo", eventType.uri);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
            Assert.AreEqual("foo", exiEvent.Prefix);
            Assert.AreEqual("urn:foo", exiEvent.URI);
            Assert.IsTrue(((EXIEventNS)exiEvent).LocalElementNs);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
            Assert.AreEqual("xsi", exiEvent.Prefix);
            Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
            Assert.IsFalse(((EXIEventNS)exiEvent).LocalElementNs);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
            Assert.AreEqual("decimalDerived", ((EXIEventSchemaType)exiEvent).TypeName);
            Assert.AreEqual("urn:foo", ((EXIEventSchemaType)exiEvent).TypeURI);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            string stringValue = exiEvent.Characters.makeString();
            Assert.AreEqual(values[i], stringValue);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(i < n_validDecimals || preserveLexicalValues ? EventType.ITEM_SCHEMA_CH : EventType.ITEM_CH, eventType.itemType);
            if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
              int tp = scanner.currentState.contentDatatype;
              int builtinType = corpus.getBaseTypeOfSimpleType(tp);
              Assert.AreEqual(EXISchemaConst.DECIMAL_TYPE, corpus.getSerialOfType(builtinType));
            }
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
            ++n_events;

            Assert.AreEqual(8, n_events);
          }
        }
      }
    }

    /// <summary>
    /// A valid integer value matching ITEM_SCHEMA_CH where the associated
    /// datatype is an enumeration of xsd:integer.
    /// </summary>
    [Test]
    public virtual void testInteger() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string[] xmlStrings;
      String[] originalValues = {
          " \t\r 9223372036854775807\n",
          "-9223372036854775808",
          "98765432109876543210",
          "987654321098765432",
          "-987654321098765432",
          "+115",
          "----------",
          "ABCDE", // not even a decimal
          "12345.67" // is a decimal, but not an integer
      };
      String[] parsedOriginalValues = {
          " \t\n 9223372036854775807\n",
          "-9223372036854775808",
          "98765432109876543210",
          "987654321098765432",
          "-987654321098765432",
          "+115",
          "----------",
          "ABCDE",
          "12345.67"
      };
      String[] resultValues = {
          "9223372036854775807",
          "-9223372036854775808",
          "98765432109876543210",
          "987654321098765432",
          "-987654321098765432",
          "115",
          "----------",
          "ABCDE",
          "12345.67"
      };

      int i;
      xmlStrings = new string[originalValues.Length];
      for (i = 0; i < originalValues.Length; i++) {
        xmlStrings[i] = "<foo:IntegerDerived xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:IntegerDerived>\n";
      };

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveLexicalValues in new bool[] { true, false }) {
          string[] values = preserveLexicalValues ? parsedOriginalValues : resultValues;
          bool isValidValue = true;
          for (i = 0; i < xmlStrings.Length; i++) {
            if (xmlStrings[i].Contains("----------")) {
              isValidValue = false;
              continue;
            }

            Transmogrifier encoder = new Transmogrifier();
            EXIDecoder decoder = new EXIDecoder();
            Scanner scanner;

            encoder.AlignmentType = alignment;
            decoder.AlignmentType = alignment;

            encoder.PreserveLexicalValues = preserveLexicalValues;
            decoder.PreserveLexicalValues = preserveLexicalValues;

            encoder.GrammarCache = grammarCache;
            MemoryStream baos = new MemoryStream();
            encoder.OutputStream = baos;

            byte[] bts;
            int n_events;

            try {
              encoder.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));
            }
            catch (TransmogrifierException) {
              Assert.IsTrue(!preserveLexicalValues && !isValidValue);
              continue;
            }
            Assert.IsTrue(preserveLexicalValues || isValidValue);

            bts = baos.ToArray();

            decoder.GrammarCache = grammarCache;
            decoder.InputStream = new MemoryStream(bts);
            scanner = decoder.processHeader();

            EventDescription exiEvent;
            n_events = 0;

            EventType eventType;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SD, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("IntegerDerived", eventType.name);
            Assert.AreEqual("urn:foo", eventType.uri);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual(values[i], exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
            if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
              int tp = scanner.currentState.contentDatatype;
              int builtinType = corpus.getBaseTypeOfSimpleType(tp);
              Assert.AreEqual(EXISchemaConst.INTEGER_TYPE, corpus.getSerialOfType(builtinType));
            }
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
            ++n_events;

            Assert.AreEqual(5, n_events);
          }
        }
      }
    }

    /// <summary>
    /// A valid long value matching ITEM_SCHEMA_CH where the associated
    /// datatype is an enumeration of xsd:long.
    /// </summary>
    [Test]
    public virtual void testLong() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

      string[] xmlStrings;
      String[] originalValues = {
          " \t\r 112\n",
          "113",
          "114"
      };
      String[] parsedOriginalValues = {
          " \t\n 112\n",
          "113",
          "114"
      };
      String[] resultValues = {
          "112",
          "113",
          "114"
      };

      int i;
      xmlStrings = new string[originalValues.Length];
      for (i = 0; i < originalValues.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " + "xsi:type='foo:longDerived' >" + originalValues[i] + "</foo:A>\n";
      };

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveLexicalValues in new bool[] { true, false }) {
          string[] values = preserveLexicalValues ? parsedOriginalValues : resultValues;
          for (i = 0; i < xmlStrings.Length; i++) {
            Transmogrifier encoder = new Transmogrifier();
            EXIDecoder decoder = new EXIDecoder();
            Scanner scanner;

            encoder.AlignmentType = alignment;
            decoder.AlignmentType = alignment;

            encoder.PreserveLexicalValues = preserveLexicalValues;
            decoder.PreserveLexicalValues = preserveLexicalValues;

            encoder.GrammarCache = grammarCache;
            MemoryStream baos = new MemoryStream();
            encoder.OutputStream = baos;

            byte[] bts;
            int n_events;

            encoder.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));

            bts = baos.ToArray();

            decoder.GrammarCache = grammarCache;
            decoder.InputStream = new MemoryStream(bts);
            scanner = decoder.processHeader();

            EventDescription exiEvent;
            n_events = 0;

            EventType eventType;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SD, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("A", eventType.name);
            Assert.AreEqual("urn:foo", eventType.uri);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
            Assert.AreEqual("foo", exiEvent.Prefix);
            Assert.AreEqual("urn:foo", exiEvent.URI);
            Assert.IsTrue(((EXIEventNS)exiEvent).LocalElementNs);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
            Assert.AreEqual("xsi", exiEvent.Prefix);
            Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
            Assert.IsFalse(((EXIEventNS)exiEvent).LocalElementNs);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
            Assert.AreEqual("longDerived", ((EXIEventSchemaType)exiEvent).TypeName);
            Assert.AreEqual("urn:foo", ((EXIEventSchemaType)exiEvent).TypeURI);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual(values[i], exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
            if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
              int tp = scanner.currentState.contentDatatype;
              int builtinType = corpus.getBaseTypeOfSimpleType(tp);
              Assert.AreEqual(EXISchemaConst.LONG_TYPE, corpus.getSerialOfType(builtinType));
            }
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
            ++n_events;

            Assert.AreEqual(8, n_events);
          }
        }
      }
    }

    /// <summary>
    /// A valid int value matching ITEM_SCHEMA_CH where the associated
    /// datatype is an enumeration of xsd:int.
    /// </summary>
    [Test]
    public virtual void testInt() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

      string[] xmlStrings;
      String[] originalValues = {
          " \t\r 109\n",
          "110",
          "111"
      };
      String[] parsedOriginalValues = {
          " \t\n 109\n",
          "110",
          "111"
      };
      String[] resultValues = {
          "109",
          "110",
          "111"
      };

      int i;
      xmlStrings = new string[originalValues.Length];
      for (i = 0; i < originalValues.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " + "xsi:type='foo:intDerived' >" + originalValues[i] + "</foo:A>\n";
      };

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveLexicalValues in new bool[] { true, false }) {
          string[] values = preserveLexicalValues ? parsedOriginalValues : resultValues;
          for (i = 0; i < xmlStrings.Length; i++) {
            Transmogrifier encoder = new Transmogrifier();
            EXIDecoder decoder = new EXIDecoder();
            Scanner scanner;

            encoder.AlignmentType = alignment;
            decoder.AlignmentType = alignment;

            encoder.PreserveLexicalValues = preserveLexicalValues;
            decoder.PreserveLexicalValues = preserveLexicalValues;

            encoder.GrammarCache = grammarCache;
            MemoryStream baos = new MemoryStream();
            encoder.OutputStream = baos;

            byte[] bts;
            int n_events;

            encoder.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));

            bts = baos.ToArray();

            decoder.GrammarCache = grammarCache;
            decoder.InputStream = new MemoryStream(bts);
            scanner = decoder.processHeader();

            EventDescription exiEvent;
            n_events = 0;

            EventType eventType;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SD, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("A", eventType.name);
            Assert.AreEqual("urn:foo", eventType.uri);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
            Assert.AreEqual("foo", exiEvent.Prefix);
            Assert.AreEqual("urn:foo", exiEvent.URI);
            Assert.IsTrue(((EXIEventNS)exiEvent).LocalElementNs);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
            Assert.AreEqual("xsi", exiEvent.Prefix);
            Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
            Assert.IsFalse(((EXIEventNS)exiEvent).LocalElementNs);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
            Assert.AreEqual("intDerived", ((EXIEventSchemaType)exiEvent).TypeName);
            Assert.AreEqual("urn:foo", ((EXIEventSchemaType)exiEvent).TypeURI);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual(values[i], exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
            if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
              int tp = scanner.currentState.contentDatatype;
              int builtinType = corpus.getBaseTypeOfSimpleType(tp);
              Assert.AreEqual(EXISchemaConst.INT_TYPE, corpus.getSerialOfType(builtinType));
            }
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
            ++n_events;

            Assert.AreEqual(8, n_events);
          }
        }
      }
    }

    /// <summary>
    /// A valid byte value matching ITEM_SCHEMA_CH where the associated
    /// datatype is an enumeration of xsd:byte.
    /// </summary>
    [Test]
    public virtual void testByte() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

      string[] xmlStrings;
      String[] originalValues = {
          " \t\r 126\n",
          "127",
          "-128"
      };
      String[] parsedOriginalValues = {
          " \t\n 126\n",
          "127",
          "-128"
      };
      String[] resultValues = {
          "126",
          "127",
          "-128"
      };

      int i;
      xmlStrings = new string[originalValues.Length];
      for (i = 0; i < originalValues.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " + "xsi:type='foo:byteDerived' >" + originalValues[i] + "</foo:A>\n";
      };

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveLexicalValues in new bool[] { true, false }) {
          string[] values = preserveLexicalValues ? parsedOriginalValues : resultValues;
          for (i = 0; i < xmlStrings.Length; i++) {
            Transmogrifier encoder = new Transmogrifier();
            EXIDecoder decoder = new EXIDecoder();
            Scanner scanner;

            encoder.AlignmentType = alignment;
            decoder.AlignmentType = alignment;

            encoder.PreserveLexicalValues = preserveLexicalValues;
            decoder.PreserveLexicalValues = preserveLexicalValues;

            encoder.GrammarCache = grammarCache;
            MemoryStream baos = new MemoryStream();
            encoder.OutputStream = baos;

            byte[] bts;
            int n_events;

            encoder.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));

            bts = baos.ToArray();

            decoder.GrammarCache = grammarCache;
            decoder.InputStream = new MemoryStream(bts);
            scanner = decoder.processHeader();

            EventDescription exiEvent;
            n_events = 0;

            EventType eventType;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SD, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("A", eventType.name);
            Assert.AreEqual("urn:foo", eventType.uri);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
            Assert.AreEqual("foo", exiEvent.Prefix);
            Assert.AreEqual("urn:foo", exiEvent.URI);
            Assert.IsTrue(((EXIEventNS)exiEvent).LocalElementNs);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
            Assert.AreEqual("xsi", exiEvent.Prefix);
            Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
            Assert.IsFalse(((EXIEventNS)exiEvent).LocalElementNs);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
            Assert.AreEqual("byteDerived", ((EXIEventSchemaType)exiEvent).TypeName);
            Assert.AreEqual("urn:foo", ((EXIEventSchemaType)exiEvent).TypeURI);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual(values[i], exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
            if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
              int tp = scanner.currentState.contentDatatype;
              int builtinType = corpus.getBaseTypeOfSimpleType(tp);
              Assert.AreEqual(EXISchemaConst.BYTE_TYPE, corpus.getSerialOfType(builtinType));
            }
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
            ++n_events;

            Assert.AreEqual(8, n_events);
          }
        }
      }
    }

    /// <summary>
    /// A valid float value matching ITEM_SCHEMA_CH where the associated
    /// datatype is an enumeration of xsd:float.
    /// </summary>
    [Test]
    public virtual void testFloat() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

      string[] xmlStrings;
      String[] originalValues = {
          " \t\r 1.0301E2\n",
          "10501E-2",
          "107.01"
      };
      String[] parsedOriginalValues = {
          " \t\n 1.0301E2\n",
          "10501E-2",
          "107.01"
      };
      String[] resultValues = {
          "10301E-2",
          "10501E-2",
          "10701E-2"
      };

      int i;
      xmlStrings = new string[originalValues.Length];
      for (i = 0; i < originalValues.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " + "xsi:type='foo:floatDerived' >" + originalValues[i] + "</foo:A>\n";
      };

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveLexicalValues in new bool[] { true, false }) {
          string[] values = preserveLexicalValues ? parsedOriginalValues : resultValues;
          for (i = 0; i < xmlStrings.Length; i++) {
            Transmogrifier encoder = new Transmogrifier();
            EXIDecoder decoder = new EXIDecoder();
            Scanner scanner;

            encoder.AlignmentType = alignment;
            decoder.AlignmentType = alignment;

            encoder.PreserveLexicalValues = preserveLexicalValues;
            decoder.PreserveLexicalValues = preserveLexicalValues;

            encoder.GrammarCache = grammarCache;
            MemoryStream baos = new MemoryStream();
            encoder.OutputStream = baos;

            byte[] bts;
            int n_events;

            encoder.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));

            bts = baos.ToArray();

            decoder.GrammarCache = grammarCache;
            decoder.InputStream = new MemoryStream(bts);
            scanner = decoder.processHeader();

            EventDescription exiEvent;
            n_events = 0;

            EventType eventType;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SD, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("A", eventType.name);
            Assert.AreEqual("urn:foo", eventType.uri);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
            Assert.AreEqual("foo", exiEvent.Prefix);
            Assert.AreEqual("urn:foo", exiEvent.URI);
            Assert.IsTrue(((EXIEventNS)exiEvent).LocalElementNs);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
            Assert.AreEqual("xsi", exiEvent.Prefix);
            Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
            Assert.IsFalse(((EXIEventNS)exiEvent).LocalElementNs);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
            Assert.AreEqual("floatDerived", ((EXIEventSchemaType)exiEvent).TypeName);
            Assert.AreEqual("urn:foo", ((EXIEventSchemaType)exiEvent).TypeURI);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual(values[i], exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
            if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
              int tp = scanner.currentState.contentDatatype;
              int builtinType = corpus.getBaseTypeOfSimpleType(tp);
              Assert.AreEqual(EXISchemaConst.FLOAT_TYPE, corpus.getSerialOfType(builtinType));
            }
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
            ++n_events;

            Assert.AreEqual(8, n_events);
          }
        }
      }
    }

    /// <summary>
    /// A valid double value matching ITEM_SCHEMA_CH where the associated
    /// datatype is an enumeration of xsd:double.
    /// </summary>
    [Test]
    public virtual void testDouble() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string[] xmlStrings;
      String[] originalValues = {
        " \t\r -1E4\n",
        "1267.43233E12",
        "12.78e-2",
        "12",
        "0",
        "-0",
        "INF",
        "-INF",
        "NaN"
      };
      String[] parsedOriginalValues = {
        " \t\n -1E4\n",
        "1267.43233E12",
        "12.78e-2",
        "12",
        "0",
        "-0",
        "INF",
        "-INF",
        "NaN"
      };
      double[] resultValues = {
        Convert.ToDouble("-1E4"),
        Convert.ToDouble("1267.43233E12"),
        Convert.ToDouble("12.78e-2"),
        Convert.ToDouble("12"),
        Convert.ToDouble("0"),
        Convert.ToDouble("-0"),
        Double.PositiveInfinity,
        Double.NegativeInfinity,
        Double.NaN
      };

      xmlStrings = new string[originalValues.Length];
      for (int i = 0; i < originalValues.Length; i++) {
        xmlStrings[i] = "<foo:DoubleDerived xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:DoubleDerived>\n";
      };

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveLexicalValues in new bool[] { true, false }) {
          for (int i = 0; i < xmlStrings.Length; i++) {
            Transmogrifier encoder = new Transmogrifier();
            EXIDecoder decoder = new EXIDecoder();
            Scanner scanner;

            encoder.AlignmentType = alignment;
            decoder.AlignmentType = alignment;

            encoder.PreserveLexicalValues = preserveLexicalValues;
            decoder.PreserveLexicalValues = preserveLexicalValues;

            encoder.GrammarCache = grammarCache;
            MemoryStream baos = new MemoryStream();
            encoder.OutputStream = baos;

            byte[] bts;
            int n_events;

            encoder.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));

            bts = baos.ToArray();

            decoder.GrammarCache = grammarCache;
            decoder.InputStream = new MemoryStream(bts);
            scanner = decoder.processHeader();

            EventDescription exiEvent;
            n_events = 0;

            EventType eventType;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SD, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("DoubleDerived", eventType.name);
            Assert.AreEqual("urn:foo", eventType.uri);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            if (preserveLexicalValues) {
              Assert.AreEqual(parsedOriginalValues[i], exiEvent.Characters.makeString());
            }
            else {
              double expectedValue = resultValues[i];
              if (Double.IsNaN(expectedValue)) {
                Assert.AreEqual("NaN", exiEvent.Characters.makeString());
              }
              else if (double.IsInfinity(expectedValue)) {
                Assert.AreEqual(expectedValue > 0 ? "INF" : "-INF", exiEvent.Characters.makeString());
              }
              else {
                Assert.AreEqual(expectedValue, Convert.ToDouble(exiEvent.Characters.makeString()), 0.0);
              }
            }
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
            if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
              int tp = scanner.currentState.contentDatatype;
              int builtinType = corpus.getBaseTypeOfSimpleType(tp);
              Assert.AreEqual(EXISchemaConst.DOUBLE_TYPE, corpus.getSerialOfType(builtinType));
            }
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
            ++n_events;

            Assert.AreEqual(5, n_events);
          }
        }
      }
    }

    /// <summary>
    /// A valid QName value matching ITEM_SCHEMA_CH where the associated
    /// datatype is an enumeration of xsd:QName.
    /// </summary>
    [Test]
    public virtual void testQName() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

      string[] xmlStrings;
      String[] originalValues = {
        " \t\r foo:A\n",
        "goo:A",
      };
      String[] parsedOriginalValues = {
        " \t\n foo:A\n",
        "goo:A",
      };
      String[] resultValues = {
        " \t\n foo:A\n",
        "goo:A",
      };

      int i;
      xmlStrings = new string[originalValues.Length];
      for (i = 0; i < originalValues.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " + "xsi:type='foo:qNameDerived' >" + originalValues[i] + "</foo:A>\n";
      };

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveLexicalValues in new bool[] { true, false }) {
          string[] values = preserveLexicalValues ? parsedOriginalValues : resultValues;
          for (i = 0; i < xmlStrings.Length; i++) {
            Transmogrifier encoder = new Transmogrifier();
            EXIDecoder decoder = new EXIDecoder();
            Scanner scanner;

            encoder.AlignmentType = alignment;
            decoder.AlignmentType = alignment;

            encoder.PreserveLexicalValues = preserveLexicalValues;
            decoder.PreserveLexicalValues = preserveLexicalValues;

            encoder.GrammarCache = grammarCache;
            MemoryStream baos = new MemoryStream();
            encoder.OutputStream = baos;

            byte[] bts;
            int n_events;

            encoder.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));

            bts = baos.ToArray();

            decoder.GrammarCache = grammarCache;
            decoder.InputStream = new MemoryStream(bts);
            scanner = decoder.processHeader();

            EventDescription exiEvent;
            n_events = 0;

            EventType eventType;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SD, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("A", eventType.name);
            Assert.AreEqual("urn:foo", eventType.uri);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
            Assert.AreEqual("foo", exiEvent.Prefix);
            Assert.AreEqual("urn:foo", exiEvent.URI);
            Assert.IsTrue(((EXIEventNS)exiEvent).LocalElementNs);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
            Assert.AreEqual("xsi", exiEvent.Prefix);
            Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
            Assert.IsFalse(((EXIEventNS)exiEvent).LocalElementNs);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
            Assert.AreEqual("qNameDerived", ((EXIEventSchemaType)exiEvent).TypeName);
            Assert.AreEqual("urn:foo", ((EXIEventSchemaType)exiEvent).TypeURI);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual(values[i], exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
            if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
              int tp = scanner.currentState.contentDatatype;
              int builtinType = corpus.getBaseTypeOfSimpleType(tp);
              Assert.AreEqual(EXISchemaConst.QNAME_TYPE, corpus.getSerialOfType(builtinType));
            }
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
            ++n_events;

            Assert.AreEqual(8, n_events);
          }
        }
      }
    }

    /// <summary>
    /// A valid Notation value matching ITEM_SCHEMA_CH where the associated
    /// datatype is an enumeration of xsd:Notation.
    /// </summary>
    [Test]
    public virtual void testNotation() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

      string[] xmlStrings;
      String[] originalValues = {
          " \t\r foo:cat\n",
          "foo:dog",
          "foo:pig"
      };
      String[] parsedOriginalValues = {
          " \t\n foo:cat\n",
          "foo:dog",
          "foo:pig"
      };
      String[] resultValues = {
          " \t\n foo:cat\n",
          "foo:dog",
          "foo:pig"
      };

      int i;
      xmlStrings = new string[originalValues.Length];
      for (i = 0; i < originalValues.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " + "xsi:type='foo:notationDerived' >" + originalValues[i] + "</foo:A>\n";
      };

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveLexicalValues in new bool[] { true, false }) {
          string[] values = preserveLexicalValues ? parsedOriginalValues : resultValues;
          for (i = 0; i < xmlStrings.Length; i++) {
            Transmogrifier encoder = new Transmogrifier();
            EXIDecoder decoder = new EXIDecoder();
            Scanner scanner;

            encoder.AlignmentType = alignment;
            decoder.AlignmentType = alignment;

            encoder.PreserveLexicalValues = preserveLexicalValues;
            decoder.PreserveLexicalValues = preserveLexicalValues;

            encoder.GrammarCache = grammarCache;
            MemoryStream baos = new MemoryStream();
            encoder.OutputStream = baos;

            byte[] bts;
            int n_events;

            encoder.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));

            bts = baos.ToArray();

            decoder.GrammarCache = grammarCache;
            decoder.InputStream = new MemoryStream(bts);
            scanner = decoder.processHeader();

            EventDescription exiEvent;
            n_events = 0;

            EventType eventType;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SD, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("A", eventType.name);
            Assert.AreEqual("urn:foo", eventType.uri);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
            Assert.AreEqual("foo", exiEvent.Prefix);
            Assert.AreEqual("urn:foo", exiEvent.URI);
            Assert.IsTrue(((EXIEventNS)exiEvent).LocalElementNs);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
            Assert.AreEqual("xsi", exiEvent.Prefix);
            Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
            Assert.IsFalse(((EXIEventNS)exiEvent).LocalElementNs);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
            Assert.AreEqual("notationDerived", ((EXIEventSchemaType)exiEvent).TypeName);
            Assert.AreEqual("urn:foo", ((EXIEventSchemaType)exiEvent).TypeURI);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual(values[i], exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
            if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
              int tp = scanner.currentState.contentDatatype;
              int builtinType = corpus.getBaseTypeOfSimpleType(tp);
              Assert.AreEqual(EXISchemaConst.NOTATION_TYPE, corpus.getSerialOfType(builtinType));
            }
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
            ++n_events;

            Assert.AreEqual(8, n_events);
          }
        }
      }
    }

    /// <summary>
    /// A valid union-typed value matching ITEM_SCHEMA_CH where the associated
    /// datatype is an enumeration of unioned-type.
    /// </summary>
    [Test]
    public virtual void testUnion() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

      string[] xmlStrings;
      String[] originalValues = {
          " \t\r 100\n",
          "Tokyo",
          "101"
      };
      String[] parsedOriginalValues = {
          " \t\n 100\n",
          "Tokyo",
          "101"
      };
      String[] resultValues = {
          " \t\n 100\n",
          "Tokyo",
          "101"
      };

      int i;
      xmlStrings = new string[originalValues.Length];
      for (i = 0; i < originalValues.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " + "xsi:type='foo:unionedEnum' >" + originalValues[i] + "</foo:A>\n";
      };

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveLexicalValues in new bool[] { true, false }) {
          string[] values = preserveLexicalValues ? parsedOriginalValues : resultValues;
          for (i = 0; i < xmlStrings.Length; i++) {
            Transmogrifier encoder = new Transmogrifier();
            EXIDecoder decoder = new EXIDecoder();
            Scanner scanner;

            encoder.AlignmentType = alignment;
            decoder.AlignmentType = alignment;

            encoder.PreserveLexicalValues = preserveLexicalValues;
            decoder.PreserveLexicalValues = preserveLexicalValues;

            encoder.GrammarCache = grammarCache;
            MemoryStream baos = new MemoryStream();
            encoder.OutputStream = baos;

            byte[] bts;
            int n_events;

            encoder.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));

            bts = baos.ToArray();

            decoder.GrammarCache = grammarCache;
            decoder.InputStream = new MemoryStream(bts);
            scanner = decoder.processHeader();

            EventDescription exiEvent;
            n_events = 0;

            EventType eventType;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SD, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("A", eventType.name);
            Assert.AreEqual("urn:foo", eventType.uri);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
            Assert.AreEqual("foo", exiEvent.Prefix);
            Assert.AreEqual("urn:foo", exiEvent.URI);
            Assert.IsTrue(((EXIEventNS)exiEvent).LocalElementNs);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
            Assert.AreEqual("xsi", exiEvent.Prefix);
            Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
            Assert.IsFalse(((EXIEventNS)exiEvent).LocalElementNs);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
            Assert.AreEqual("unionedEnum", ((EXIEventSchemaType)exiEvent).TypeName);
            Assert.AreEqual("urn:foo", ((EXIEventSchemaType)exiEvent).TypeURI);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual(values[i], exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
            if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
              int tp = scanner.currentState.contentDatatype;
              Assert.AreEqual(EXISchema.UNION_SIMPLE_TYPE, corpus.getVarietyOfSimpleType(tp));
            }
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
            ++n_events;

            Assert.AreEqual(8, n_events);
          }
        }
      }
    }

    /// <summary>
    /// A valid base64Binary value matching ITEM_SCHEMA_CH where the associated
    /// datatype is an enumeration of xsd:base64Binary.
    /// </summary>
    [Test]
    public virtual void testBase64Binary() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

      string[] xmlStrings;
      String[] originalValues = {
          " \t\r YWFhYWE=\n",
          "Y2NjY2M=",
          "ZWVlZWU="
      };
      String[] parsedOriginalValues = {
          " \t\n YWFhYWE=\n",
          "Y2NjY2M=",
          "ZWVlZWU="
      };
      String[] resultValues = {
          "YWFhYWE=",
          "Y2NjY2M=",
          "ZWVlZWU="
      };

      int i;
      xmlStrings = new string[originalValues.Length];
      for (i = 0; i < originalValues.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " + "xsi:type='foo:base64BinaryDerived' >" + originalValues[i] + "</foo:A>\n";
      };

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveLexicalValues in new bool[] { true, false }) {
          string[] values = preserveLexicalValues ? parsedOriginalValues : resultValues;
          for (i = 0; i < xmlStrings.Length; i++) {
            Transmogrifier encoder = new Transmogrifier();
            EXIDecoder decoder = new EXIDecoder();
            Scanner scanner;

            encoder.AlignmentType = alignment;
            decoder.AlignmentType = alignment;

            encoder.PreserveLexicalValues = preserveLexicalValues;
            decoder.PreserveLexicalValues = preserveLexicalValues;

            encoder.GrammarCache = grammarCache;
            MemoryStream baos = new MemoryStream();
            encoder.OutputStream = baos;

            byte[] bts;
            int n_events;

            encoder.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));

            bts = baos.ToArray();

            decoder.GrammarCache = grammarCache;
            decoder.InputStream = new MemoryStream(bts);
            scanner = decoder.processHeader();

            EventDescription exiEvent;
            n_events = 0;

            EventType eventType;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SD, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("A", eventType.name);
            Assert.AreEqual("urn:foo", eventType.uri);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
            Assert.AreEqual("foo", exiEvent.Prefix);
            Assert.AreEqual("urn:foo", exiEvent.URI);
            Assert.IsTrue(((EXIEventNS)exiEvent).LocalElementNs);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
            Assert.AreEqual("xsi", exiEvent.Prefix);
            Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
            Assert.IsFalse(((EXIEventNS)exiEvent).LocalElementNs);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
            Assert.AreEqual("base64BinaryDerived", ((EXIEventSchemaType)exiEvent).TypeName);
            Assert.AreEqual("urn:foo", ((EXIEventSchemaType)exiEvent).TypeURI);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual(values[i], exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
            if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
              int tp = scanner.currentState.contentDatatype;
              int builtinType = corpus.getBaseTypeOfSimpleType(tp);
              Assert.AreEqual(EXISchemaConst.BASE64BINARY_TYPE, corpus.getSerialOfType(builtinType));
            }
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
            ++n_events;

            Assert.AreEqual(8, n_events);
          }
        }
      }
    }

    /// <summary>
    /// A valid hexBinary value matching ITEM_SCHEMA_CH where the associated
    /// datatype is an enumeration of xsd:hexBinary.
    /// </summary>
    [Test]
    public virtual void testHexBinary() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

      string[] xmlStrings;
      String[] originalValues = {
          " \t\r 6161616161\n",
          "6363636363",
          "6565656565"
      };
      String[] parsedOriginalValues = {
          " \t\n 6161616161\n",
          "6363636363",
          "6565656565"
      };
      String[] resultValues = {
          "6161616161",
          "6363636363",
          "6565656565"
      };

      int i;
      xmlStrings = new string[originalValues.Length];
      for (i = 0; i < originalValues.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " + "xsi:type='foo:hexBinaryDerived' >" + originalValues[i] + "</foo:A>\n";
      };

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveLexicalValues in new bool[] { true, false }) {
          string[] values = preserveLexicalValues ? parsedOriginalValues : resultValues;
          for (i = 0; i < xmlStrings.Length; i++) {
            Transmogrifier encoder = new Transmogrifier();
            EXIDecoder decoder = new EXIDecoder();
            Scanner scanner;

            encoder.AlignmentType = alignment;
            decoder.AlignmentType = alignment;

            encoder.PreserveLexicalValues = preserveLexicalValues;
            decoder.PreserveLexicalValues = preserveLexicalValues;

            encoder.GrammarCache = grammarCache;
            MemoryStream baos = new MemoryStream();
            encoder.OutputStream = baos;

            byte[] bts;
            int n_events;

            encoder.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));

            bts = baos.ToArray();

            decoder.GrammarCache = grammarCache;
            decoder.InputStream = new MemoryStream(bts);
            scanner = decoder.processHeader();

            EventDescription exiEvent;
            n_events = 0;

            EventType eventType;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SD, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("A", eventType.name);
            Assert.AreEqual("urn:foo", eventType.uri);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
            Assert.AreEqual("foo", exiEvent.Prefix);
            Assert.AreEqual("urn:foo", exiEvent.URI);
            Assert.IsTrue(((EXIEventNS)exiEvent).LocalElementNs);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
            Assert.AreEqual("xsi", exiEvent.Prefix);
            Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
            Assert.IsFalse(((EXIEventNS)exiEvent).LocalElementNs);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
            Assert.AreEqual("hexBinaryDerived", ((EXIEventSchemaType)exiEvent).TypeName);
            Assert.AreEqual("urn:foo", ((EXIEventSchemaType)exiEvent).TypeURI);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual(values[i], exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
            if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
              int tp = scanner.currentState.contentDatatype;
              int builtinType = corpus.getBaseTypeOfSimpleType(tp);
              Assert.AreEqual(EXISchemaConst.HEXBINARY_TYPE, corpus.getSerialOfType(builtinType));
            }
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
            ++n_events;

            Assert.AreEqual(8, n_events);
          }
        }
      }
    }

    /// <summary>
    /// A valid anyURI value matching ITEM_SCHEMA_CH where the associated
    /// datatype is an enumeration of xsd:anyURI.
    /// </summary>
    [Test]
    public virtual void testAnyURI() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

      string[] xmlStrings;
      String[] originalValues = {
          " \t\r urn:foo\n",
          "urn:goo",
          "urn:hoo"
      };
      String[] parsedOriginalValues = {
          " \t\n urn:foo\n",
          "urn:goo",
          "urn:hoo"
      };
      String[] resultValues = {
          "urn:foo",
          "urn:goo",
          "urn:hoo"
      };

      int i;
      xmlStrings = new string[originalValues.Length];
      for (i = 0; i < originalValues.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " + "xsi:type='foo:anyURIDerived' >" + originalValues[i] + "</foo:A>\n";
      };

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveLexicalValues in new bool[] { true, false }) {
          string[] values = preserveLexicalValues ? parsedOriginalValues : resultValues;
          for (i = 0; i < xmlStrings.Length; i++) {
            Transmogrifier encoder = new Transmogrifier();
            EXIDecoder decoder = new EXIDecoder();
            Scanner scanner;

            encoder.AlignmentType = alignment;
            decoder.AlignmentType = alignment;

            encoder.PreserveLexicalValues = preserveLexicalValues;
            decoder.PreserveLexicalValues = preserveLexicalValues;

            encoder.GrammarCache = grammarCache;
            MemoryStream baos = new MemoryStream();
            encoder.OutputStream = baos;

            byte[] bts;
            int n_events;

            encoder.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));

            bts = baos.ToArray();

            decoder.GrammarCache = grammarCache;
            decoder.InputStream = new MemoryStream(bts);
            scanner = decoder.processHeader();

            EventDescription exiEvent;
            n_events = 0;

            EventType eventType;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SD, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("A", eventType.name);
            Assert.AreEqual("urn:foo", eventType.uri);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
            Assert.AreEqual("foo", exiEvent.Prefix);
            Assert.AreEqual("urn:foo", exiEvent.URI);
            Assert.IsTrue(((EXIEventNS)exiEvent).LocalElementNs);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
            Assert.AreEqual("xsi", exiEvent.Prefix);
            Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
            Assert.IsFalse(((EXIEventNS)exiEvent).LocalElementNs);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
            Assert.AreEqual("anyURIDerived", ((EXIEventSchemaType)exiEvent).TypeName);
            Assert.AreEqual("urn:foo", ((EXIEventSchemaType)exiEvent).TypeURI);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual(values[i], exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
            if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
              int tp = scanner.currentState.contentDatatype;
              int builtinType = corpus.getBaseTypeOfSimpleType(tp);
              Assert.AreEqual(EXISchemaConst.ANYURI_TYPE, corpus.getSerialOfType(builtinType));
            }
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
            ++n_events;

            Assert.AreEqual(8, n_events);
          }
        }
      }
    }

    /// <summary>
    /// A valid list value matching ITEM_SCHEMA_CH where the associated
    /// datatype is an enumeration of a list of xsd:ID.
    /// </summary>
    [Test]
    public virtual void testlistOfIDs() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

      string[] xmlStrings;
      String[] originalValues = {
          " \t\r AB BC CD\n",
          "EF FG GH",
          "IJ JK KL"
      };
      String[] parsedOriginalValues = {
          " \t\n AB BC CD\n",
          "EF FG GH",
          "IJ JK KL"
      };
      String[] resultValues = {
          "AB BC CD",
          "EF FG GH",
          "IJ JK KL"
      };

      int i;
      xmlStrings = new string[originalValues.Length];
      for (i = 0; i < originalValues.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " + "xsi:type='foo:listOfIDsEnum' >" + originalValues[i] + "</foo:A>\n";
      };

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveLexicalValues in new bool[] { true, false }) {
          string[] values = preserveLexicalValues ? parsedOriginalValues : resultValues;
          for (i = 0; i < xmlStrings.Length; i++) {
            Transmogrifier encoder = new Transmogrifier();
            EXIDecoder decoder = new EXIDecoder();
            Scanner scanner;

            encoder.AlignmentType = alignment;
            decoder.AlignmentType = alignment;

            encoder.PreserveLexicalValues = preserveLexicalValues;
            decoder.PreserveLexicalValues = preserveLexicalValues;

            encoder.GrammarCache = grammarCache;
            MemoryStream baos = new MemoryStream();
            encoder.OutputStream = baos;

            byte[] bts;
            int n_events;

            encoder.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));

            bts = baos.ToArray();

            decoder.GrammarCache = grammarCache;
            decoder.InputStream = new MemoryStream(bts);
            scanner = decoder.processHeader();

            EventDescription exiEvent;
            n_events = 0;

            EventType eventType;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SD, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("A", eventType.name);
            Assert.AreEqual("urn:foo", eventType.uri);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
            Assert.AreEqual("foo", exiEvent.Prefix);
            Assert.AreEqual("urn:foo", exiEvent.URI);
            Assert.IsTrue(((EXIEventNS)exiEvent).LocalElementNs);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
            Assert.AreEqual("xsi", exiEvent.Prefix);
            Assert.AreEqual(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, exiEvent.URI);
            Assert.IsFalse(((EXIEventNS)exiEvent).LocalElementNs);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_TP, exiEvent.EventKind);
            Assert.AreEqual("listOfIDsEnum", ((EXIEventSchemaType)exiEvent).TypeName);
            Assert.AreEqual("urn:foo", ((EXIEventSchemaType)exiEvent).TypeURI);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual(values[i], exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
            if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
              int tp = scanner.currentState.contentDatatype;
              Assert.AreEqual("urn:foo", EXISchemaUtil.getTargetNamespaceNameOfType(tp, corpus));
              Assert.AreEqual("listOfIDsEnum", corpus.getNameOfType(tp));
            }
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_ED, eventType.itemType);
            ++n_events;

            Assert.AreEqual(8, n_events);
          }
        }
      }
    }

  }

}