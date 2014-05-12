using System;
using System.IO;
using System.Collections.Generic;
using NUnit.Framework;

using Org.System.Xml.Sax;

using EXIDecoder = Nagasena.Proc.EXIDecoder;
using HeaderOptionsOutputType = Nagasena.Proc.HeaderOptionsOutputType;
using AlignmentType = Nagasena.Proc.Common.AlignmentType;
using EventDescription = Nagasena.Proc.Common.EventDescription;
using EventDescription_Fields = Nagasena.Proc.Common.EventDescription_Fields;
using GrammarOptions = Nagasena.Proc.Common.GrammarOptions;
using GrammarCache = Nagasena.Proc.Grammars.GrammarCache;
using Scanner = Nagasena.Proc.IO.Scanner;
using EmptySchema = Nagasena.Schema.EmptySchema;
using TestBase = Nagasena.Schema.TestBase;

namespace Nagasena.Sax {

  [TestFixture]
  public class ValueMaxLengthTest : TestBase {

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
    /// The stream can be decoded without a proper valueMaxLength value being set. 
    /// </summary>
    [Test]
    public virtual void testNoAddition_01() {

      GrammarCache grammarCache = new GrammarCache(EmptySchema.EXISchema, GrammarOptions.DEFAULT_OPTIONS);

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      encoder.ValueMaxLength = 3;

      string xmlString;

      xmlString = "<A xmlns='urn:foo'>" + "<AB>abcd</AB><AC>abcd</AC><AB>abcd</AB><AC>abcd</AC>" + "</A>\n";

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
        string stringValue = null;
        n_events = 0;
        n_texts = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          if (exiEvent.EventKind == EventDescription_Fields.EVENT_CH) {
            stringValue = exiEvent.Characters.makeString();
            Assert.AreEqual("abcd", stringValue);
            ++n_texts;
          }
          exiEventList.Add(exiEvent);
        }
        Assert.AreEqual(4, n_texts);
        Assert.AreEqual(16, n_events);
      }
    }

    /// <summary>
    /// The stream *cannot* be decoded without a proper valueMaxLength value being set. 
    /// </summary>
    [Test]
    public virtual void testNoAddition_02() {

      GrammarCache grammarCache = new GrammarCache(EmptySchema.EXISchema, GrammarOptions.DEFAULT_OPTIONS);

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      encoder.GrammarCache = grammarCache;
      decoder.GrammarCache = grammarCache;

      encoder.ValueMaxLength = 3;
      encoder.OutputOptions = HeaderOptionsOutputType.lessSchemaId;

      foreach (AlignmentType alignment in Alignments) {

        encoder.AlignmentType = alignment;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        string[] values = new string[] { "abcd", "abc", "abc" };

        string xmlString = "<A xmlns='urn:foo'>" + "<AB>abcd</AB><AB>abc</AB><AC>abc</AC>" + "</A>\n";

        byte[] bts;
        int n_events, n_texts;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        bts = baos.ToArray();

        Scanner scanner;

        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();
        Assert.AreEqual(3, scanner.HeaderOptions.ValueMaxLength);
        Assert.AreEqual(alignment, scanner.HeaderOptions.AlignmentType);

        List<EventDescription> exiEventList = new List<EventDescription>();

        EventDescription exiEvent;
        string stringValue = null;
        n_events = 0;
        n_texts = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          if (exiEvent.EventKind == EventDescription_Fields.EVENT_CH) {
            stringValue = exiEvent.Characters.makeString();
            Assert.AreEqual(values[n_texts++], stringValue);
          }
          exiEventList.Add(exiEvent);
        }
        Assert.AreEqual(3, n_texts);
        Assert.AreEqual(13, n_events);
      }
    }

  }

}