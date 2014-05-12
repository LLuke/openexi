using System;
using System.IO;
using System.Collections.Generic;
using NUnit.Framework;

using Org.System.Xml.Sax;

using EXIDecoder = Nagasena.Proc.EXIDecoder;
using AlignmentType = Nagasena.Proc.Common.AlignmentType;
using EventType = Nagasena.Proc.Common.EventType;
using EventDescription = Nagasena.Proc.Common.EventDescription;
using EventDescription_Fields = Nagasena.Proc.Common.EventDescription_Fields;
using GrammarOptions = Nagasena.Proc.Common.GrammarOptions;
using GrammarCache = Nagasena.Proc.Grammars.GrammarCache;
using Scanner = Nagasena.Proc.IO.Scanner;
using EXISchema = Nagasena.Schema.EXISchema;
using TestBase = Nagasena.Schema.TestBase;
using EXISchemaFactoryTestUtil = Nagasena.Scomp.EXISchemaFactoryTestUtil;

namespace Nagasena.Sax {

  [TestFixture]
  public class RCSTest : TestBase {

    private static readonly AlignmentType[] Alignments = new AlignmentType[] { 
      AlignmentType.bitPacked, 
      AlignmentType.byteAligned, 
      AlignmentType.preCompress, 
      AlignmentType.compress 
    };

    ///////////////////////////////////////////////////////////////////////////
    // Test cases
    ///////////////////////////////////////////////////////////////////////////

    ///  
    [Test]
    public virtual void testNoChars_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/patterns.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      encoder.ValueMaxLength = 3;

      string xmlString;

      xmlString = "<NoChars xmlns='urn:foo'>XYZ</NoChars>\n";

      foreach (AlignmentType alignment in Alignments) {

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        byte[] bts;
        int n_events, n_texts;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

        Scanner scanner;

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
            Assert.AreEqual("XYZ", stringValue);
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH, exiEvent.getEventType().itemType);
            ++n_texts;
          }
          exiEventList.Add(exiEvent);
        }
        Assert.AreEqual(1, n_texts);
        Assert.AreEqual(5, n_events);
      }
    }

    ///  
    [Test]
    public virtual void testOneChar_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/patterns.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      encoder.ValueMaxLength = 3;

      string xmlString;

      xmlString = "<OneChar xmlns='urn:foo'>XYZ</OneChar>\n";

      foreach (AlignmentType alignment in Alignments) {

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        byte[] bts;
        int n_events, n_texts;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

        Scanner scanner;

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
            Assert.AreEqual("XYZ", stringValue);
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

}