using System;
using System.IO;
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
using TestBase = Nagasena.Schema.TestBase;
using EXISchemaFactoryTestUtil = Nagasena.Scomp.EXISchemaFactoryTestUtil;

namespace Nagasena.Sax {

  [TestFixture]
  [Category("Enable_Compression")]
  public class HexBinaryValueEncodingTest : TestBase {

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
    /// A valid hexBinary value matching ITEM_SCHEMA_CH where the associated
    /// datatype is xsd:hexBinary.
    /// </summary>
    [Test]
    public virtual void testValidHexBinary() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/hexBinary.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string[] xmlStrings;
      String[] originalValues = {
          " \t\r 0\tF b7\n",
          " \t\r\n ",
          " 0123456789ABCDEFabcdef0123456789ABCDEFabcdef0123456789ABCDEFabcdef0123456789\n" + 
          "ABCDEFabcdef0123456789ABCDEFabcdef0123456789ABCDEFabcdef0123456789ABCDEFabcd\n" + 
          "ef0123456789ABCDEFabcdef0123456789ABCDEFabcdef0123456789ABCDEFabcdef01234567\n" + 
          "89ABCDEFabcdef0123456789ABCDEFabcdef0123456789ABCDEFabcdef0123456789ABCDEFab\n" + 
          "cdef0123456789ABCDEFabcdef0123456789ABCDEFabcdef0123456789ABCDEFabcdef012345\n" + 
          "6789ABCDEFabcdef0123456789ABCDEFabcdef0123456789ABCDEFabcdef0123456789ABCDEF\n" + 
          "abcdef\t\r\n"
      };
      String[] parsedOriginalValues = {
          " \t\n 0\tF b7\n", 
          " \t\n ",
          " 0123456789ABCDEFabcdef0123456789ABCDEFabcdef0123456789ABCDEFabcdef0123456789\n" + 
          "ABCDEFabcdef0123456789ABCDEFabcdef0123456789ABCDEFabcdef0123456789ABCDEFabcd\n" + 
          "ef0123456789ABCDEFabcdef0123456789ABCDEFabcdef0123456789ABCDEFabcdef01234567\n" + 
          "89ABCDEFabcdef0123456789ABCDEFabcdef0123456789ABCDEFabcdef0123456789ABCDEFab\n" + 
          "cdef0123456789ABCDEFabcdef0123456789ABCDEFabcdef0123456789ABCDEFabcdef012345\n" + 
          "6789ABCDEFabcdef0123456789ABCDEFabcdef0123456789ABCDEFabcdef0123456789ABCDEF\n" + 
          "abcdef\t\n"
      };
      String[] resultValues = {
          "0FB7",
          "",
          "0123456789ABCDEFABCDEF0123456789ABCDEFABCDEF0123456789ABCDEFABCDEF0123456789" + 
          "ABCDEFABCDEF0123456789ABCDEFABCDEF0123456789ABCDEFABCDEF0123456789ABCDEFABCD" + 
          "EF0123456789ABCDEFABCDEF0123456789ABCDEFABCDEF0123456789ABCDEFABCDEF01234567" + 
          "89ABCDEFABCDEF0123456789ABCDEFABCDEF0123456789ABCDEFABCDEF0123456789ABCDEFAB" + 
          "CDEF0123456789ABCDEFABCDEF0123456789ABCDEFABCDEF0123456789ABCDEFABCDEF012345" + 
          "6789ABCDEFABCDEF0123456789ABCDEFABCDEF0123456789ABCDEFABCDEF0123456789ABCDEF" + 
          "ABCDEF"
      };

      int i;
      xmlStrings = new string[originalValues.Length];
      for (i = 0; i < originalValues.Length; i++) {
        xmlStrings[i] = "<foo:HexBinary xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:HexBinary>\n";
      };

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveLexicalValues in new bool[] { true, false }) {
          string[] values = preserveLexicalValues ? parsedOriginalValues : resultValues;
          for (i = 0; i < xmlStrings.Length; i++) {
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
            Assert.AreEqual("HexBinary", eventType.name);
            Assert.AreEqual("urn:foo", eventType.uri);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual(values[i], exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
            if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
              int tp = scanner.currentState.contentDatatype;
              Assert.AreEqual(EXISchemaConst.HEXBINARY_TYPE, corpus.getSerialOfType(tp));
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
    /// Preserve lexical hexBinary values by turning on Preserve.lexicalValues.
    /// </summary>
    [Test]
    public virtual void testValidHexBinaryRCS() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/hexBinary.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string[] xmlStrings;
      String[] originalValues = {
          " \t\r 0\tF b@7\n", // '@' will be encoded as an escaped character
      };
      String[] parsedOriginalValues = {
          " \t\n 0\tF b@7\n", 
      };

      int i;
      xmlStrings = new string[originalValues.Length];
      for (i = 0; i < originalValues.Length; i++) {
        xmlStrings[i] = "<foo:HexBinary xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:HexBinary>\n";
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
          int n_events;

          encoder.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));

          bts = baos.ToArray();

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
          Assert.AreEqual("HexBinary", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
          Assert.AreEqual(parsedOriginalValues[i], exiEvent.Characters.makeString());
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            int tp = scanner.currentState.contentDatatype;
            Assert.AreEqual(EXISchemaConst.HEXBINARY_TYPE, corpus.getSerialOfType(tp));
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