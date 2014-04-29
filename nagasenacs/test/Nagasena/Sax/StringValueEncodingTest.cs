using System;
using System.IO;
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
using EXISchemaConst = Nagasena.Schema.EXISchemaConst;
using TestBase = Nagasena.Schema.TestBase;
using EXISchemaFactoryTestUtil = Nagasena.Scomp.EXISchemaFactoryTestUtil;

namespace Nagasena.Sax {

  [TestFixture]
  [Category("Enable_Compression")]
  public class StringValueEncodingTest : TestBase {

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
    /// Encode then decoder non-BMP characters.
    /// </summary>
    [Test]
    public virtual void testNonBMPCharacters_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/verySimpleDefault.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      string[] xmlStrings;
      String[] values = {
          "\uD840\uDC0B\uD844\uDE3D", // characters in SIP (U+2000B) and (U+2123D)
          "a \uD840\uDC0B\uD844\uDE3D b",
      };
      String[] resultValues = {
          "\uD840\uDC0B\uD844\uDE3D",
          "a \uD840\uDC0B\uD844\uDE3D b",
      };

      int i;
      xmlStrings = new string[values.Length];
      for (i = 0; i < values.Length; i++) {
        xmlStrings[i] = "<B>" + values[i] + "</B>\n";
      };

      Transmogrifier encoder = new Transmogrifier();
      encoder.GrammarCache = grammarCache;

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveLexicalValues in new bool[] { true, false }) {
          for (i = 0; i < xmlStrings.Length; i++) {
            Scanner scanner;

            encoder.AlignmentType = alignment;
            decoder.AlignmentType = alignment;

            encoder.PreserveLexicalValues = preserveLexicalValues;
            decoder.PreserveLexicalValues = preserveLexicalValues;

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
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual(resultValues[i], exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
            if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
              Assert.AreEqual(EXISchemaConst.STRING_TYPE, corpus.getSerialOfType(scanner.currentState.contentDatatype));
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
            Assert.AreSame(exiEvent, eventType);
            Assert.AreEqual(0, eventType.Index);
            eventTypeList = eventType.EventTypeList;
            Assert.AreEqual(1, eventTypeList.Length);
            ++n_events;

            Assert.AreEqual(5, n_events);
          }
        }
      }
    }

  }

}