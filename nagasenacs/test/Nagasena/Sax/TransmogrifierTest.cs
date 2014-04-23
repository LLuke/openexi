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
using EmptySchema = Nagasena.Schema.EmptySchema;
using TestBase = Nagasena.Schema.TestBase;

namespace Nagasena.Sax {

  [TestFixture]
  [Category("Enable_Compression")]
  public class TransmogrifierTest : TestBase {

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
    /// SAXParser factory for use with Transmogrifier needs to be aware of namespaces.
    /// </summary>
    //public virtual void testSAXParserFactoryMisConfigured_01() {
    //}

    /// <summary>
    /// Tests accessors to the GrammarCache.
    /// </summary>
    [Test]
    public virtual void testGrammarCacheAccessor_01() {

      GrammarCache grammarCache = new GrammarCache(EmptySchema.EXISchema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier encoder = new Transmogrifier();

      encoder.GrammarCache = grammarCache;
      Assert.AreSame(grammarCache, encoder.GrammarCache);
    }

    /// <summary>
    /// Enable XML parser's "http://xml.org/sax/features/namespace-prefixes" feature.
    /// </summary>
    [Test]
    public virtual void testNamespacePrefixesFeature_01() {

      GrammarCache grammarCache = new GrammarCache((EXISchema)null, GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));

      // REVISIT: repair AElfred
      const string xmlString = "<rpc message-id='id' xmlns='a.b.c'><inner/></rpc>\n";
      //const string xmlString = "<abc:rpc message-id='id' xmlns:abc='a.b.c'><abc:inner/></abc:rpc>\n";

      foreach (AlignmentType alignment in Alignments) {

        Transmogrifier encoder = new Transmogrifier(true); // Turn on "http://xml.org/sax/features/namespace-prefixes"
        encoder.AlignmentType = alignment;
        MemoryStream baos = new MemoryStream();
        encoder.GrammarCache = grammarCache;
        encoder.OutputStream = baos;

        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));

        byte[] bts = baos.ToArray();

        EXIDecoder decoder = new EXIDecoder();

        decoder.AlignmentType = alignment;
        decoder.GrammarCache = grammarCache;
        decoder.InputStream = new MemoryStream(bts);

        Scanner scanner = decoder.processHeader();

        EventDescription exiEvent;
        EventType eventType;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("rpc", exiEvent.Name);
        Assert.AreEqual("a.b.c", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_NS, exiEvent.EventKind);
        Assert.AreEqual("", exiEvent.Prefix);
        Assert.AreEqual("a.b.c", exiEvent.URI);

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
        Assert.AreEqual("message-id", exiEvent.Name);
        Assert.AreEqual("", exiEvent.URI);
        Assert.AreEqual("id", exiEvent.Characters.makeString());

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        Assert.AreEqual("inner", exiEvent.Name);
        Assert.AreEqual("a.b.c", exiEvent.URI);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE_WC, eventType.itemType);

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);

        Assert.IsNull(scanner.nextEvent());
      }
    }

  }

}