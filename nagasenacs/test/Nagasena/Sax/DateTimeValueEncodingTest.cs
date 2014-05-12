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
using GrammarOptions = Nagasena.Proc.Common.GrammarOptions;
using GrammarCache = Nagasena.Proc.Grammars.GrammarCache;
using Scanner = Nagasena.Proc.IO.Scanner;
using EXISchema = Nagasena.Schema.EXISchema;
using EXISchemaConst = Nagasena.Schema.EXISchemaConst;
using EXISchemaFactoryTestUtil = Nagasena.Scomp.EXISchemaFactoryTestUtil;
using TestBase = Nagasena.Schema.TestBase;

namespace Nagasena.Sax {

  [TestFixture]
  public class DateTimeValueEncodingTest : TestBase {

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
    /// Preserve lexical dateTime values by turning on Preserve.lexicalValues.
    /// </summary>
    [Test]
    public virtual void testDateTimeRCS() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/dateTime.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      String[] xmlStrings;
      String[] originalValues = {
          " \t\r *2003-04-25*T*11:41:30.45+09:00*\n", // '*' will be encoded as an escaped character 
      };
      String[] parsedOriginalValues = {
          " \t\n *2003-04-25*T*11:41:30.45+09:00*\n", 
      };

      int i;
      xmlStrings = new string[originalValues.Length];
      for (i = 0; i < originalValues.Length; i++) {
        xmlStrings[i] = "<foo:DateTime xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:DateTime>\n";
      };

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      encoder.PreserveLexicalValues = true;
      decoder.PreserveLexicalValues = true;

      foreach (AlignmentType alignment in Alignments) {
        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;
        for (i = 0; i < xmlStrings.Length; i++) {
          Scanner scanner;

          MemoryStream baos = new MemoryStream();
          encoder.OutputStream = baos;

          byte[] bts;
          int n_events, n_texts;

          encoder.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));

          bts = baos.ToArray();

          decoder.InputStream = new MemoryStream(bts);
          scanner = decoder.processHeader();

          List<EventDescription> exiEventList = new List<EventDescription>();

          EventDescription exiEvent;
          n_events = 0;
          n_texts = 0;
          while ((exiEvent = scanner.nextEvent()) != null) {
            ++n_events;
            if (exiEvent.EventKind == EventDescription_Fields.EVENT_CH) {
              string stringValue = exiEvent.Characters.makeString();
              Assert.AreEqual(parsedOriginalValues[i], stringValue);
              Assert.AreEqual(EventType.ITEM_SCHEMA_CH, exiEvent.getEventType().itemType);
              ++n_texts;
            }
            exiEventList.Add(exiEvent);
          }
          Assert.AreEqual(1, n_texts);
          Assert.AreEqual(5, n_events);
        }
      }
    }

    /// <summary>
    /// A valid dateTime value matching ITEM_SCHEMA_CH where the associated
    /// datatype is xsd:dateTime.
    /// </summary>
    [Test]
    public virtual void testDateTime() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/dateTime.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      String[] xmlStrings;
      String[] originalValues = {
         " \t\r 2003-04-25T11:41:30.45+09:00\n",
         "2003-04-25T11:41:30.45+14:00", 
         "1997-07-16T19:20:30.45-12:00", 
         "1997-07-16T19:20:30.45Z", 
         "1999-12-31T24:00:00",
         "-0601-07-16T19:20:30.45-05:09",
         "1972-06-30T23:59:60", // valid leap second (1972-06-30)
         "2013-06-30T23:59:60", // invalid
         "2009-04-01T12:34:56.0001234",
         "----------",
         "1997-07-16Z", // not a valid xsd:dateTime value
         "xyz", // an absurd value
      };
      String[] parsedOriginalValues = {
          " \t\n 2003-04-25T11:41:30.45+09:00\n",
          "2003-04-25T11:41:30.45+14:00", 
          "1997-07-16T19:20:30.45-12:00", 
          "1997-07-16T19:20:30.45Z", 
          "1999-12-31T24:00:00",
          "-0601-07-16T19:20:30.45-05:09",
          "1972-06-30T23:59:60",
          "2013-06-30T23:59:60",
          "2009-04-01T12:34:56.0001234",
          "----------",
          "1997-07-16Z",
          "xyz",
      };
      String[] resultValues = {
          "2003-04-25T11:41:30.45+09:00",
          "2003-04-25T11:41:30.45+14:00", 
          "1997-07-16T19:20:30.45-12:00", 
          "1997-07-16T19:20:30.45Z", 
          "1999-12-31T24:00:00",
          "-0601-07-16T19:20:30.45-05:09",
          "1972-06-30T23:59:60",
          "2013-06-30T23:59:60",
          "2009-04-01T12:34:56.0001234",
          "----------",
          "1997-07-16Z",
          "xyz",
      };

      int i;
      xmlStrings = new string[originalValues.Length];
      for (i = 0; i < originalValues.Length; i++) {
        xmlStrings[i] = "<foo:DateTime xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:DateTime>\n";
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
            Assert.AreEqual("DateTime", eventType.name);
            Assert.AreEqual("urn:foo", eventType.uri);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual(values[i], exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
            if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
              int tp = scanner.currentState.contentDatatype;
              Assert.AreEqual(EXISchemaConst.DATETIME_TYPE, corpus.getSerialOfType(tp));
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
    /// datatype is xsd:date.
    /// </summary>
    [Test]
    public virtual void testDate() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/dateTime.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      String[] xmlStrings;
      String[] originalValues = {
          " \t\r 2003-04-25+09:00\n",
          "-0601-07-16-05:00",
          "1997-07-16",
          "1997-07-16Z",
          "2012-02-29", // 02-29 is permitted in leap years
          "----------",
          "1997-07Z", // not a valid xsd:date value
          "xyz", // an absurd value
          "2013-02-29", // 2013 is not a leap year
      };
      String[] parsedOriginalValues = {
          " \t\n 2003-04-25+09:00\n",
          "-0601-07-16-05:00",
          "1997-07-16",
          "1997-07-16Z",
          "2012-02-29",
          "----------",
          "1997-07Z",
          "xyz",
          "2013-02-29",
      };
      String[] resultValues = {
          "2003-04-25+09:00",
          "-0601-07-16-05:00",
          "1997-07-16",
          "1997-07-16Z",
          "2012-02-29",
          "----------",
          "1997-07Z",
          "xyz",
          "2013-02-29",
      };

      int i;
      xmlStrings = new string[originalValues.Length];
      for (i = 0; i < originalValues.Length; i++) {
        xmlStrings[i] = "<foo:Date xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:Date>\n";
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
              encoder.encode(new InputSource<Stream>(string2Stream(originalValue)));
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
            Assert.AreEqual("Date", eventType.name);
            Assert.AreEqual("urn:foo", eventType.uri);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual(values[i], exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
            if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
              int tp = scanner.currentState.contentDatatype;
              Assert.AreEqual(EXISchemaConst.DATE_TYPE, corpus.getSerialOfType(tp));
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
    /// A valid time value matching ITEM_SCHEMA_CH where the associated
    /// datatype is xsd:time.
    /// </summary>
    [Test]
    public virtual void testTime() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/dateTime.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      String[] xmlStrings;
      String[] originalValues = {
          " \t\r 13:20:00+09:00\n", 
          "13:20:30.455-09:45", 
          "13:20:00", 
          "13:20:00Z",
          "24:00:00",
          "----------",
          "1997-07-16Z", // not a valid xsd:time value
          "xyz", // an absurd value
          "24:00:01",
      };
      String[] parsedOriginalValues = {
          " \t\n 13:20:00+09:00\n", 
          "13:20:30.455-09:45", 
          "13:20:00", 
          "13:20:00Z",
          "24:00:00",
          "----------",
          "1997-07-16Z",        
          "xyz",
          "24:00:01",
      };
      String[] resultValues = {
          "13:20:00+09:00", 
          "13:20:30.455-09:45", 
          "13:20:00", 
          "13:20:00Z",
          "24:00:00",
          "----------",
          "1997-07-16Z",        
          "xyz",
          "24:00:01",
      };

      int i;
      xmlStrings = new string[originalValues.Length];
      for (i = 0; i < originalValues.Length; i++) {
        xmlStrings[i] = "<foo:Time xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:Time>\n";
      };

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveLexicalValues in new bool[] { true, false }) {
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
            Assert.AreEqual("Time", eventType.name);
            Assert.AreEqual("urn:foo", eventType.uri);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual(values[i], exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
            if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
              int tp = scanner.currentState.contentDatatype;
              Assert.AreEqual(EXISchemaConst.TIME_TYPE, corpus.getSerialOfType(tp));
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
    /// A valid gregorian day value matching ITEM_SCHEMA_CH where the associated
    /// datatype is xsd:gDay.
    /// </summary>
    [Test]
    public virtual void testGDay() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/dateTime.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      String[] xmlStrings;
      String[] originalValues = {
          " \t\r ---16\n",
          "---16+09:00",
          "---16Z",
          "---31+04:32",
          "----------",
          "1997-07-16Z", // not a valid xsd:gDay value
          "xyz", // an absurd value
          "---4", // #digits != 2
          "---32", // too big
          "---004", // #digits != 2
      };
      String[] parsedOriginalValues = {
          " \t\n ---16\n",
          "---16+09:00",
          "---16Z",
          "---31+04:32",
          "----------",
          "1997-07-16Z",
          "xyz",
          "---4",
          "---32",
          "---004",
      };
      String[] resultValues = {
          "---16",
          "---16+09:00",
          "---16Z",
          "---31+04:32",
          "----------",
          "1997-07-16Z",
          "xyz",
          "---4",
          "---32",
          "---004",
      };



      int i;
      xmlStrings = new string[originalValues.Length];
      for (i = 0; i < originalValues.Length; i++) {
        xmlStrings[i] = "<foo:GDay xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:GDay>\n";
      };

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveLexicalValues in new bool[] { true, false }) {
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
            Assert.AreEqual("GDay", eventType.name);
            Assert.AreEqual("urn:foo", eventType.uri);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual(values[i], exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
            if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
              int tp = scanner.currentState.contentDatatype;
              Assert.AreEqual(EXISchemaConst.G_DAY_TYPE, corpus.getSerialOfType(tp));
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
    /// A valid gregorian month value matching ITEM_SCHEMA_CH where the associated
    /// datatype is xsd:gMonth.
    /// </summary>
    [Test]
    public virtual void testGMonth() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/dateTime.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      String[] xmlStrings;
      String[] originalValues = {
          " \t\r --09\n",
          "--09+09:00",
          "--09Z",
          "----------",
          "1997-07-16Z", // not a valid xsd:gMonth value
          "xyz", // an absurd value
          "--4", // #digits != 2
          "--13", // too big
          "--004", // #digits != 2
      };
      String[] parsedOriginalValues = {
          " \t\n --09\n",
          "--09+09:00",
          "--09Z",
          "----------",
          "1997-07-16Z",
          "xyz",
          "--4",
          "--13",
          "--004", 
      };
      String[] resultValues = {
          "--09",
          "--09+09:00",
          "--09Z",
          "----------",
          "1997-07-16Z",
          "xyz",
          "--4",
          "--13",
          "--004", 
      };

      int i;
      xmlStrings = new string[originalValues.Length];
      for (i = 0; i < originalValues.Length; i++) {
        xmlStrings[i] = "<foo:GMonth xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:GMonth>\n";
      };

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveLexicalValues in new bool[] { true, false }) {
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
            Assert.AreEqual("GMonth", eventType.name);
            Assert.AreEqual("urn:foo", eventType.uri);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual(values[i], exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
            if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
              int tp = scanner.currentState.contentDatatype;
              Assert.AreEqual(EXISchemaConst.G_MONTH_TYPE, corpus.getSerialOfType(tp));
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
    /// datatype is xsd:gMonthDay.
    /// </summary>
    [Test]
    public virtual void testGMonthDay() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/dateTime.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      String[] xmlStrings;
      String[] originalValues = {
          " \t\r --09-16\n",
          "--09-16+09:00",
          "--09-16Z",
          "--02-29",
          "----------",
          "1997-07-16Z", // not a valid xsd:gMonthDay value
          "xyz", // an absurd value
          "--02-30",
          "--04-31",
      };
      String[] parsedOriginalValues = {
          " \t\n --09-16\n",
          "--09-16+09:00",
          "--09-16Z",
          "--02-29",
          "----------",
          "1997-07-16Z",
          "xyz",
          "--02-30",
          "--04-31",
      };
      String[] resultValues = {
          "--09-16",
          "--09-16+09:00",
          "--09-16Z",
          "--02-29",
          "----------",
          "1997-07-16Z",
          "xyz",
          "--02-30",
          "--04-31",
      };

      int i;
      xmlStrings = new string[originalValues.Length];
      for (i = 0; i < originalValues.Length; i++) {
        xmlStrings[i] = "<foo:GMonthDay xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:GMonthDay>\n";
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
            Assert.AreEqual("GMonthDay", eventType.name);
            Assert.AreEqual("urn:foo", eventType.uri);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual(values[i], exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
            if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
              int tp = scanner.currentState.contentDatatype;
              Assert.AreEqual(EXISchemaConst.G_MONTHDAY_TYPE, corpus.getSerialOfType(tp));
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
    /// A valid gregorian calendar year value matching ITEM_SCHEMA_CH where the associated
    /// datatype is xsd:gYear.
    /// </summary>
    [Test]
    public virtual void testGYear() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/dateTime.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      String[] xmlStrings;
      String[] originalValues = {
          " \t\r 1969\n",
          "1969+09:00",
          "1969Z",
          "0001",
          "----------",
          "1997-07-16Z", // not a valid xsd:gYear value
          "xyz", // an absurd value
          "001", // # of digits < 4
      };
      String[] parsedOriginalValues = {
          " \t\n 1969\n",
          "1969+09:00",
          "1969Z",
          "0001",
          "----------",
          "1997-07-16Z",
          "xyz",
          "001",
      };
      String[] resultValues = {
          "1969",
          "1969+09:00",
          "1969Z",
          "0001",
          "----------",
          "1997-07-16Z",
          "xyz",
          "001",
      };

      int i;
      xmlStrings = new string[originalValues.Length];
      for (i = 0; i < originalValues.Length; i++) {
        xmlStrings[i] = "<foo:GYear xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:GYear>\n";
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
            Assert.AreEqual("GYear", eventType.name);
            Assert.AreEqual("urn:foo", eventType.uri);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual(values[i], exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
            if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
              int tp = scanner.currentState.contentDatatype;
              Assert.AreEqual(EXISchemaConst.G_YEAR_TYPE, corpus.getSerialOfType(tp));
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
    /// A valid value representing a specific gregorian month in a specific 
    /// gregorian year value matching ITEM_SCHEMA_CH where the associated datatype 
    /// is xsd:gYearMonth.
    /// </summary>
    [Test]
    public virtual void testGYearMonth() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/dateTime.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      String[] xmlStrings;
      String[] originalValues = {
          " \t\r 1999-05\n",
          "1999-05+09:00",
          "1999-05Z",
          "1997-07-16Z", // not a valid xsd:gYearMonth value
          "xyz", // an absurd value
      };
      String[] parsedOriginalValues = {
          " \t\n 1999-05\n",
          "1999-05+09:00",
          "1999-05Z",
          "1997-07-16Z",
          "xyz",
      };
      String[] resultValues = {
          "1999-05",
          "1999-05+09:00",
          "1999-05Z",
          "1997-07-16Z",
          "xyz",
      };
      const int n_validValues = 3;

      int i;
      xmlStrings = new string[originalValues.Length];
      for (i = 0; i < originalValues.Length; i++) {
        xmlStrings[i] = "<foo:GYearMonth xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:GYearMonth>\n";
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

            try {
              encoder.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));
            }
            catch (TransmogrifierException) {
              Assert.IsTrue(!preserveLexicalValues && n_validValues <= i);
              continue;
            }
            Assert.IsTrue(preserveLexicalValues || i < n_validValues);

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
            Assert.AreEqual("GYearMonth", eventType.name);
            Assert.AreEqual("urn:foo", eventType.uri);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual(values[i], exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
            if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
              int tp = scanner.currentState.contentDatatype;
              Assert.AreEqual(EXISchemaConst.G_YEARMONTH_TYPE, corpus.getSerialOfType(tp));
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

  }

}