using System;
using System.IO;
using NUnit.Framework;

using Org.System.Xml.Sax;

using EXIDecoder = Nagasena.Proc.EXIDecoder;
using AlignmentType = Nagasena.Proc.Common.AlignmentType;
using EventType = Nagasena.Proc.Common.EventType;
using EventDescription = Nagasena.Proc.Common.EventDescription;
using EventDescription_Fields = Nagasena.Proc.Common.EventDescription_Fields;
using GrammarOptions = Nagasena.Proc.Common.GrammarOptions;
using XmlUriConst = Nagasena.Proc.Common.XmlUriConst;
using Scanner = Nagasena.Proc.IO.Scanner;
using GrammarCache = Nagasena.Proc.Grammars.GrammarCache;
using EXISchema = Nagasena.Schema.EXISchema;
using TestBase = Nagasena.Schema.TestBase;

using EXISchemaFactoryTestUtil = Nagasena.Scomp.EXISchemaFactoryTestUtil;

namespace Nagasena.Sax {

  [TestFixture]
  public class WhitespacesTest : TestBase {

    private static readonly AlignmentType[] Alignments = new AlignmentType[] { 
      AlignmentType.bitPacked, 
      AlignmentType.byteAligned, 
      AlignmentType.preCompress, 
      AlignmentType.compress 
    };

    ///////////////////////////////////////////////////////////////////////////
    // Test cases
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Whitespaces in element-only contexts are not preserved regardless of 
     * PreserveWhitespaces setting in strict mode.
     */
    [Test]
    public void testElementOnlyStrict_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
          "/whiteSpace.gram", this);
    
      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      String xmlString = 
        "<foo:B xmlns:foo='urn:foo'>\n" +
        "  <C/>\n" +
        "  <D/>\n" +
        "</foo:B>\n";

      Transmogrifier encoder = new Transmogrifier();
      encoder.GrammarCache = grammarCache;
    
      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveWhitespaces in new bool[] { true, false }) {
          encoder.PreserveWhitespaces = preserveWhitespaces;
        
          encoder.AlignmentType = alignment;
          decoder.AlignmentType = alignment;
  
          MemoryStream baos = new MemoryStream();
          encoder.OutputStream = baos;
        
          byte[] bts;

          encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));
        
          bts = baos.ToArray();
        
          decoder.InputStream = new MemoryStream(bts);
          Scanner scanner = decoder.processHeader();

          EventType eventType;
          EventDescription exiEvent;
        
          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreSame(exiEvent, eventType);
          Assert.AreEqual(0, eventType.Index);

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("B", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
        
          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("C", eventType.name);
          Assert.AreEqual("", eventType.uri);
    
          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("D", eventType.name);
          Assert.AreEqual("", eventType.uri);

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreSame(exiEvent, eventType);

          Assert.IsNull(scanner.nextEvent());
        }
      }
    }

    /**
     * Whitespaces in element-only contexts are not preserved regardless of 
     * PreserveWhitespaces setting in strict mode.
     */
    [Test]
    public void testElementOnlyStrict_02() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
          "/whiteSpace.gram", this);
    
      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      String xmlString = 
          "<foo:B xmlns:foo='urn:foo' xmlns:xml='http://www.w3.org/XML/1998/namespace' " + 
          "       xml:space='preserve'>\n" +
          "  <C/>\n" +
          "  <D/>\n" +
          "</foo:B>\n";

      Transmogrifier encoder = new Transmogrifier();
      encoder.GrammarCache = grammarCache;
    
      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveWhitespaces in new bool[] { true, false }) {
          encoder.PreserveWhitespaces = preserveWhitespaces;
        
          encoder.AlignmentType = alignment;
          decoder.AlignmentType = alignment;
  
          MemoryStream baos = new MemoryStream();
          encoder.OutputStream = baos;
        
          byte[] bts;
        
          try {
            encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));
          }
          catch (TransmogrifierException te) {
            Assert.IsTrue(preserveWhitespaces);
            continue;
          }
          Assert.IsFalse(preserveWhitespaces);
        
          bts = baos.ToArray();
        
          decoder.InputStream = new MemoryStream(bts);
          Scanner scanner = decoder.processHeader();

          EventType eventType;
          EventDescription exiEvent;
        
          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreSame(exiEvent, eventType);
          Assert.AreEqual(0, eventType.Index);

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("B", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
        
          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
          Assert.AreEqual(XmlUriConst.W3C_XML_1998_URI, exiEvent.URI);
          Assert.AreEqual("space", exiEvent.Name);
          Assert.AreEqual("preserve", exiEvent.Characters.makeString());
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        
          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("C", eventType.name);
          Assert.AreEqual("", eventType.uri);
    
          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("D", eventType.name);
          Assert.AreEqual("", eventType.uri);

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreSame(exiEvent, eventType);

          Assert.IsNull(scanner.nextEvent());
        }
      }
    }

    /**
     * Whitespaces in element-only contexts are preserved if PreserveWhitespaces is 
     * set to true in default mode.
     */
    [Test]
    public void testElementOnlyDefault_01() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
          "/whiteSpace.gram", this);
    
      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      String xmlString = 
        "<foo:B xmlns:foo='urn:foo'>\n" +
        "  <C/>\n" +
        "  <D/>\n" +
        "</foo:B>\n";

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      foreach (AlignmentType alignment in Alignments) {
        foreach (bool preserveWhitespaces in new bool[] { true, false }) {
          encoder.PreserveWhitespaces = preserveWhitespaces;
        
          encoder.AlignmentType = alignment;
          decoder.AlignmentType = alignment;
  
          encoder.GrammarCache = grammarCache;
          MemoryStream baos = new MemoryStream();
          encoder.OutputStream = baos;
        
          byte[] bts;

          encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));
        
          bts = baos.ToArray();
        
          decoder.GrammarCache = grammarCache;
        
          decoder.InputStream = new MemoryStream(bts);
          Scanner scanner = decoder.processHeader();

          EventType eventType;
          EventDescription exiEvent;
        
          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreSame(exiEvent, eventType);
          Assert.AreEqual(0, eventType.Index);

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("B", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
        
          if (preserveWhitespaces) {
            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual("\n  ", exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        
          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("C", eventType.name);
          Assert.AreEqual("", eventType.uri);
    
          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);

          if (preserveWhitespaces) {
            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual("\n  ", exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }
        
          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("D", eventType.name);
          Assert.AreEqual("", eventType.uri);

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);

          if (preserveWhitespaces) {
            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual("\n", exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
          }

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreSame(exiEvent, eventType);

          Assert.IsNull(scanner.nextEvent());
        }
      }
    }

    /**
     * Whitespaces in element-only contexts are preserved if current xml:space  
     * setting is preserve in default mode.
     */
    [Test]
    public void testElementOnlyDefault_02() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
          "/whiteSpace.gram", this);
    
      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      String xmlString = 
        "<foo:B xmlns:foo='urn:foo' xmlns:xml='http://www.w3.org/XML/1998/namespace' " + 
        "       xml:space='preserve'>\n" +
        "  <C/>\n" +
        "  <D/>\n" +
        "</foo:B>\n";

      Transmogrifier encoder = new Transmogrifier();
      encoder.PreserveWhitespaces = false;
      encoder.GrammarCache = grammarCache;

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;

      foreach (AlignmentType alignment in Alignments) {
        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;
      
        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));
      
        byte[] bts = baos.ToArray();
      
        decoder.InputStream = new MemoryStream(bts);
        Scanner scanner = decoder.processHeader();

        EventType eventType;
        EventDescription exiEvent;
      
        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("B", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
      
      
        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
        Assert.AreEqual(XmlUriConst.W3C_XML_1998_URI, exiEvent.URI);
        Assert.AreEqual("space", exiEvent.Name);
        Assert.AreEqual("preserve", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
      
        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("\n  ", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
      
        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("C", eventType.name);
        Assert.AreEqual("", eventType.uri);
  
        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("\n  ", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);
      
        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("D", eventType.name);
        Assert.AreEqual("", eventType.uri);

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual("\n", exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_CH, eventType.itemType);

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);

        Assert.IsNull(scanner.nextEvent());
      }
    }

  }
}