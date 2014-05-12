using System;
using System.Collections.Generic;
using System.IO;
using NUnit.Framework;

using AlignmentType = Nagasena.Proc.Common.AlignmentType;
using EventDescription = Nagasena.Proc.Common.EventDescription;
using EventDescription_Fields = Nagasena.Proc.Common.EventDescription_Fields;
using EventType = Nagasena.Proc.Common.EventType;
using EventTypeList = Nagasena.Proc.Common.EventTypeList;
using GrammarOptions = Nagasena.Proc.Common.GrammarOptions;
using GrammarCache = Nagasena.Proc.Grammars.GrammarCache;
using Scanner = Nagasena.Proc.IO.Scanner;
using ChannellingScanner = Nagasena.Proc.IO.Compression.ChannellingScanner;
using ExiUriConst = Nagasena.Proc.Util.ExiUriConst;
using EXISchema = Nagasena.Schema.EXISchema;
using TestBase = Nagasena.Schema.TestBase;
using EXISchemaFactoryTestUtil = Nagasena.Scomp.EXISchemaFactoryTestUtil;

namespace Nagasena.Proc {

  [TestFixture]
  public class DecodeStrictTest : TestBase {

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
    /// Decode EXI-encoded NLM data.
    /// </summary>
    [Test]
    public virtual void testNLM_strict_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/NLM/nlmcatalogrecord_060101.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string[] exiFiles = new string[] { 
        "/NLM/catplussamp2006.bitPacked", 
        "/NLM/catplussamp2006.byteAligned", 
        "/NLM/catplussamp2006.preCompress", 
        "/NLM/catplussamp2006.compress" 
      };

      for (int i = 0; i < Alignments.Length; i++) {
        AlignmentType alignment = Alignments[i];
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        decoder.AlignmentType = alignment;

        Uri url = resolveSystemIdAsURL(exiFiles[i]);
        FileStream inputStream = new FileStream(url.LocalPath, FileMode.Open);

        int n_events;

        decoder.GrammarCache = grammarCache;
        decoder.InputStream = inputStream;
        scanner = decoder.processHeader();

        List<EventDescription> exiEventList = new List<EventDescription>();

        EventDescription exiEvent;
        n_events = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          exiEventList.Add(exiEvent);
        }
        inputStream.Close();
        Assert.AreEqual(35176, n_events);

        // Check the last value in the last value channel
        exiEvent = exiEventList[33009];
        Assert.AreEqual("Interdisciplinary Studies", exiEvent.Characters.makeString());
      }
    }

    /// <summary>
    /// Only a handful of values in a stream.
    /// </summary>
    [Test]
    public virtual void testSequence_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/interop/schemaInformedGrammar/acceptance.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string[] exiFiles = new string[] { 
        "/interop/schemaInformedGrammar/declaredProductions/sequence-01.bitPacked", 
        "/interop/schemaInformedGrammar/declaredProductions/sequence-01.byteAligned", 
        "/interop/schemaInformedGrammar/declaredProductions/sequence-01.preCompress", 
        "/interop/schemaInformedGrammar/declaredProductions/sequence-01.compress" 
      };

      for (int i = 0; i < Alignments.Length; i++) {
        AlignmentType alignment = Alignments[i];
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        decoder.AlignmentType = alignment;

        Uri url = resolveSystemIdAsURL(exiFiles[i]);
        FileStream inputStream = new FileStream(url.LocalPath, FileMode.Open);

        int n_events;

        decoder.GrammarCache = grammarCache;
        decoder.InputStream = inputStream;
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
        Assert.AreEqual(1, eventTypeList.Length);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("A", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AB", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AC", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AC", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AD", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("AE", exiEvent.Name);
        Assert.AreEqual("urn:foo", exiEvent.URI);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Characters.makeString());
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        ++n_events;

        inputStream.Close();
        Assert.AreEqual(19, n_events);
      }
    }

    [Test]
    public virtual void testHeaderOptionsAlignment_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/optionsSchema.xsc", this);

      /// Use DEFAULT_OPTIONS to confuse the decoder. The streams all have been
      /// encoded with STRICT_OPTIONS.
      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      string[] exiFiles = new string[] { 
        "/encoding/headerOptions-01.bitPacked", 
        "/encoding/headerOptions-01.byteAligned", 
        "/encoding/headerOptions-01.preCompress", 
        "/encoding/headerOptions-01.compress" 
      };

      for (int i = 0; i < Alignments.Length; i++) {
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        Uri url = resolveSystemIdAsURL(exiFiles[i]);
        FileStream inputStream = new FileStream(url.LocalPath, FileMode.Open);

        int n_events;

        AlignmentType falseAlignmentType;
        falseAlignmentType = Alignments[i] == AlignmentType.compress ? AlignmentType.bitPacked : AlignmentType.compress;
        decoder.AlignmentType = falseAlignmentType; // trying to confuse decoder.
        decoder.GrammarCache = grammarCache;
        decoder.InputStream = inputStream;
        scanner = decoder.processHeader();
        Assert.AreEqual(Alignments[i], scanner.AlignmentType);

        List<EventDescription> exiEventList = new List<EventDescription>();

        EventDescription exiEvent;
        n_events = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          exiEventList.Add(exiEvent);
        }
        inputStream.Close();

        Assert.AreEqual(6, n_events);

        EventType eventType;

        exiEvent = exiEventList[0];
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);

        exiEvent = exiEventList[1];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("header", exiEvent.Name);
        Assert.AreEqual(ExiUriConst.W3C_2009_EXI_URI, exiEvent.URI);

        exiEvent = exiEventList[2];
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("strict", exiEvent.Name);
        Assert.AreEqual(ExiUriConst.W3C_2009_EXI_URI, exiEvent.URI);

        exiEvent = exiEventList[3];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);

        exiEvent = exiEventList[4];
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);

        exiEvent = exiEventList[5];
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
      }
    }

    [Test]
    public virtual void testEmptyBlock_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/compression/emptyBlock_01.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;

      decoder.AlignmentType = AlignmentType.compress;
      decoder.BlockSize = 1;

      Uri url = resolveSystemIdAsURL("/compression/emptyBlock_01.compress");
      FileStream inputStream = new FileStream(url.LocalPath, FileMode.Open);

      int n_events;

      decoder.GrammarCache = grammarCache;
      decoder.InputStream = inputStream;
      scanner = decoder.processHeader();

      List<EventDescription> exiEventList = new List<EventDescription>();

      EventDescription exiEvent;
      n_events = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        ++n_events;
        exiEventList.Add(exiEvent);
      }
      inputStream.Close();

      Assert.AreEqual(11, n_events);
      Assert.AreEqual(1, ((ChannellingScanner)scanner).BlockCount);

      EventType eventType;
      EventTypeList eventTypeList;

      int pos = 0;

      exiEvent = exiEventList[pos++];
      Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.IsNull(eventTypeList.EE);

      exiEvent = exiEventList[pos++];
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      Assert.AreEqual("root", exiEvent.Name);
      Assert.AreEqual("", exiEvent.URI);

      exiEvent = exiEventList[pos++];
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      Assert.AreEqual("parent", exiEvent.Name);
      Assert.AreEqual("", exiEvent.URI);

      exiEvent = exiEventList[pos++];
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      Assert.AreEqual("child", exiEvent.Name);
      Assert.AreEqual("", exiEvent.URI);

      exiEvent = exiEventList[pos++];
      Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
      Assert.AreEqual("42", exiEvent.Characters.makeString());
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);

      exiEvent = exiEventList[pos++];
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);

      exiEvent = exiEventList[pos++];
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);

      exiEvent = exiEventList[pos++];
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      Assert.AreEqual("adjunct", exiEvent.Name);
      Assert.AreEqual("", exiEvent.URI);

      exiEvent = exiEventList[pos++];
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);

      exiEvent = exiEventList[pos++];
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);

      exiEvent = exiEventList[pos++];
      Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
    }

    /// <summary>
    /// Enumeration of union
    /// </summary>
    [Test]
    public virtual void testEnmueration_04() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/interop/datatypes/enumeration/enumeration.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      string exiFiles = "/interop/datatypes/enumeration/enumeration-valid-04.byteAligned";

      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;

      decoder.AlignmentType = AlignmentType.byteAligned;

      Uri url = resolveSystemIdAsURL(exiFiles);
      FileStream inputStream = new FileStream(url.LocalPath, FileMode.Open);

      int n_events;

      decoder.GrammarCache = grammarCache;
      decoder.InputStream = inputStream;
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
      Assert.AreEqual(1, eventTypeList.Length);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      Assert.AreEqual("root", exiEvent.Name);
      Assert.AreEqual("", exiEvent.URI);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      Assert.AreEqual("union", exiEvent.Name);
      Assert.AreEqual("", exiEvent.URI);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
      Assert.AreEqual("+10", exiEvent.Characters.makeString());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      Assert.AreEqual("union", exiEvent.Name);
      Assert.AreEqual("", exiEvent.URI);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
      Assert.AreEqual("12:32:00", exiEvent.Characters.makeString());
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
      ++n_events;

      inputStream.Close();
      Assert.AreEqual(10, n_events);
    }

  }

}