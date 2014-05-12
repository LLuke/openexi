using System;
using System.IO;
using System.Collections.Generic;
using NUnit.Framework;

using Org.System.Xml.Sax;

using EXIDecoder = Nagasena.Proc.EXIDecoder;
using AlignmentType = Nagasena.Proc.Common.AlignmentType;
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
  public class CookieTest : TestBase {

    private static readonly AlignmentType[] Alignments = new AlignmentType[] { 
      AlignmentType.bitPacked, 
      AlignmentType.byteAligned, 
      AlignmentType.preCompress, 
      AlignmentType.compress 
    };

    ///////////////////////////////////////////////////////////////////////////
    // Test cases
    ///////////////////////////////////////////////////////////////////////////

    [Test]
    public virtual void testCookie_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema((string)null, this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      const string xmlString = "<A/>";

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder();

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        MemoryStream baos;

        byte[] noCookie, withCookie;

        baos = new MemoryStream();
        encoder.OutputStream = baos;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));
        noCookie = baos.ToArray();

        baos = new MemoryStream();
        encoder.OutputStream = baos;

        encoder.OutputCookie = true;
        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));
        withCookie = baos.ToArray();

        Assert.AreEqual(noCookie.Length, withCookie.Length - 4);

        int n_events;
        List<EventDescription> exiEventList;

        decoder.GrammarCache = grammarCache;

        foreach (byte[] bts in new byte[][] { noCookie, withCookie }) {
          decoder.InputStream = new MemoryStream(bts);
          Scanner scanner = decoder.processHeader();

          exiEventList = new List<EventDescription>();

          EventDescription exiEvent;
          n_events = 0;
          while ((exiEvent = scanner.nextEvent()) != null) {
            ++n_events;
            exiEventList.Add(exiEvent);
          }

          Assert.AreEqual(4, n_events);

          int pos = 0;

          exiEvent = exiEventList[pos++];
          Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);

          exiEvent = exiEventList[pos++];
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          Assert.AreEqual("A", exiEvent.Name);
          Assert.AreEqual("", exiEvent.URI);

          exiEvent = exiEventList[pos++];
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);

          exiEvent = exiEventList[pos++];
          Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        }
      }
    }

  }

}