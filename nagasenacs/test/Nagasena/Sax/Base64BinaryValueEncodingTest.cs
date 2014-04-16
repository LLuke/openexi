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
  public class Base64BinaryValueEncodingTest : TestBase {

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
    /// A valid base64Binary value matching ITEM_SCHEMA_CH where the associated
    /// datatype is xsd:base64Binary.
    /// </summary>
    [Test]
    public virtual void testValidBase64Binary() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/base64Binary.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      String[] xmlStrings;
      String[] originalValues = {
        " \t\r QUJDREVGR0hJSg==\n", 
        " \t\r\n ",
        " RHIuIFN1ZSBDbGFyayBpcyBjdXJyZW50bHkgYSBSZWdlbnRzIFByb2Zlc3NvciBvZiBDaGVtaXN0\n" +
        "cnkgYXQgV2FzaGluZ3RvbiBTdGF0ZSBVbml2ZXJzaXR5IGluIFB1bGxtYW4sIFdBLCB3aGVyZSBz\n" +
        "aGUgaGFzIHRhdWdodCBhbmQgY29uZHVjdGVkIHJlc2VhcmNoIGluIGFjdGluaWRlIGVudmlyb25t\n" +
        "ZW50YWwgY2hlbWlzdHJ5IGFuZCByYWRpb2FuYWx5dGljYWwgY2hlbWlzdHJ5IHNpbmNlIDE5OTYu\n" +
        "ICBGcm9tIDE5OTIgdG8gMTk5NiwgRHIuIENsYXJrIHdhcyBhIFJlc2VhcmNoIEVjb2xvZ2lzdCBh\n" +
        "dCB0aGUgVW5pdmVyc2l0eSBvZiBHZW9yZ2lh4oCZcyBTYXZhbm5haCBSaXZlciBFY29sb2d5IExh\n" +
        "Ym9yYXRvcnkuICBQcmlvciB0byBoZXIgcG9zaXRpb24gYXQgdGhlIFVuaXZlcnNpdHkgb2YgR2Vv\n" +
        "cmdpYSwgc2hlIHdhcyBhIFNlbmlvciBTY2llbnRpc3QgYXQgdGhlIFdlc3Rpbmdob3VzZSBTYXZh\n" +
        "bm5haCBSaXZlciBDb21wYW554oCZcyBTYXZhbm5haCBSaXZlciBUZWNobm9sb2d5IENlbnRlci4g\n" +
        "IERyLiBDbGFyayBoYXMgc2VydmVkIG9uIHZhcmlvdXMgYm9hcmRzIGFuZCBhZHZpc29yeSBjb21t\n" +
        "aXR0ZWVzLCBpbmNsdWRpbmcgdGhlIE5hdGlvbmFsIEFjYWRlbWllcyBOdWNsZWFyIGFuZCBSYWRp\n" +
        "YXRpb24gU3R1ZGllcyBCb2FyZCBhbmQgdGhlIERlcGFydG1lbnQgb2YgRW5lcmd54oCZcyBCYXNp\n" +
        "YyBFbmVyZ3kgU2NpZW5jZXMgQWR2aXNvcnkgQ29tbWl0dGVlLiAgRHIuIENsYXJrIGhvbGRzIGEg\n" +
        "UGguRC4gYW5kIE0uUy4gaW4gSW5vcmdhbmljL1JhZGlvY2hlbWlzdHJ5IGZyb20gRmxvcmlkYSBT\n" +
        "dGF0ZSBVbml2ZXJzaXR5IGFuZCBhIEIuUy4gaW4gQ2hlbWlzdHJ5IGZyb20gTGFuZGVyIENvbGxl\n" +
        "Z2UuDQo=\t\r\n"
      };
      String[] parsedOriginalValues = {
        " \t\n QUJDREVGR0hJSg==\n", 
        " \t\n ",
        " RHIuIFN1ZSBDbGFyayBpcyBjdXJyZW50bHkgYSBSZWdlbnRzIFByb2Zlc3NvciBvZiBDaGVtaXN0\n" +
        "cnkgYXQgV2FzaGluZ3RvbiBTdGF0ZSBVbml2ZXJzaXR5IGluIFB1bGxtYW4sIFdBLCB3aGVyZSBz\n" +
        "aGUgaGFzIHRhdWdodCBhbmQgY29uZHVjdGVkIHJlc2VhcmNoIGluIGFjdGluaWRlIGVudmlyb25t\n" +
        "ZW50YWwgY2hlbWlzdHJ5IGFuZCByYWRpb2FuYWx5dGljYWwgY2hlbWlzdHJ5IHNpbmNlIDE5OTYu\n" +
        "ICBGcm9tIDE5OTIgdG8gMTk5NiwgRHIuIENsYXJrIHdhcyBhIFJlc2VhcmNoIEVjb2xvZ2lzdCBh\n" +
        "dCB0aGUgVW5pdmVyc2l0eSBvZiBHZW9yZ2lh4oCZcyBTYXZhbm5haCBSaXZlciBFY29sb2d5IExh\n" +
        "Ym9yYXRvcnkuICBQcmlvciB0byBoZXIgcG9zaXRpb24gYXQgdGhlIFVuaXZlcnNpdHkgb2YgR2Vv\n" +
        "cmdpYSwgc2hlIHdhcyBhIFNlbmlvciBTY2llbnRpc3QgYXQgdGhlIFdlc3Rpbmdob3VzZSBTYXZh\n" +
        "bm5haCBSaXZlciBDb21wYW554oCZcyBTYXZhbm5haCBSaXZlciBUZWNobm9sb2d5IENlbnRlci4g\n" +
        "IERyLiBDbGFyayBoYXMgc2VydmVkIG9uIHZhcmlvdXMgYm9hcmRzIGFuZCBhZHZpc29yeSBjb21t\n" +
        "aXR0ZWVzLCBpbmNsdWRpbmcgdGhlIE5hdGlvbmFsIEFjYWRlbWllcyBOdWNsZWFyIGFuZCBSYWRp\n" +
        "YXRpb24gU3R1ZGllcyBCb2FyZCBhbmQgdGhlIERlcGFydG1lbnQgb2YgRW5lcmd54oCZcyBCYXNp\n" +
        "YyBFbmVyZ3kgU2NpZW5jZXMgQWR2aXNvcnkgQ29tbWl0dGVlLiAgRHIuIENsYXJrIGhvbGRzIGEg\n" +
        "UGguRC4gYW5kIE0uUy4gaW4gSW5vcmdhbmljL1JhZGlvY2hlbWlzdHJ5IGZyb20gRmxvcmlkYSBT\n" +
        "dGF0ZSBVbml2ZXJzaXR5IGFuZCBhIEIuUy4gaW4gQ2hlbWlzdHJ5IGZyb20gTGFuZGVyIENvbGxl\n" +
        "Z2UuDQo=\t\n"
      };
      String[] resultValues = {
        "QUJDREVGR0hJSg==",
        "",
        "RHIuIFN1ZSBDbGFyayBpcyBjdXJyZW50bHkgYSBSZWdlbnRzIFByb2Zlc3NvciBvZiBDaGVtaXN0\n" +
        "cnkgYXQgV2FzaGluZ3RvbiBTdGF0ZSBVbml2ZXJzaXR5IGluIFB1bGxtYW4sIFdBLCB3aGVyZSBz\n" +
        "aGUgaGFzIHRhdWdodCBhbmQgY29uZHVjdGVkIHJlc2VhcmNoIGluIGFjdGluaWRlIGVudmlyb25t\n" +
        "ZW50YWwgY2hlbWlzdHJ5IGFuZCByYWRpb2FuYWx5dGljYWwgY2hlbWlzdHJ5IHNpbmNlIDE5OTYu\n" +
        "ICBGcm9tIDE5OTIgdG8gMTk5NiwgRHIuIENsYXJrIHdhcyBhIFJlc2VhcmNoIEVjb2xvZ2lzdCBh\n" +
        "dCB0aGUgVW5pdmVyc2l0eSBvZiBHZW9yZ2lh4oCZcyBTYXZhbm5haCBSaXZlciBFY29sb2d5IExh\n" +
        "Ym9yYXRvcnkuICBQcmlvciB0byBoZXIgcG9zaXRpb24gYXQgdGhlIFVuaXZlcnNpdHkgb2YgR2Vv\n" +
        "cmdpYSwgc2hlIHdhcyBhIFNlbmlvciBTY2llbnRpc3QgYXQgdGhlIFdlc3Rpbmdob3VzZSBTYXZh\n" +
        "bm5haCBSaXZlciBDb21wYW554oCZcyBTYXZhbm5haCBSaXZlciBUZWNobm9sb2d5IENlbnRlci4g\n" +
        "IERyLiBDbGFyayBoYXMgc2VydmVkIG9uIHZhcmlvdXMgYm9hcmRzIGFuZCBhZHZpc29yeSBjb21t\n" +
        "aXR0ZWVzLCBpbmNsdWRpbmcgdGhlIE5hdGlvbmFsIEFjYWRlbWllcyBOdWNsZWFyIGFuZCBSYWRp\n" +
        "YXRpb24gU3R1ZGllcyBCb2FyZCBhbmQgdGhlIERlcGFydG1lbnQgb2YgRW5lcmd54oCZcyBCYXNp\n" +
        "YyBFbmVyZ3kgU2NpZW5jZXMgQWR2aXNvcnkgQ29tbWl0dGVlLiAgRHIuIENsYXJrIGhvbGRzIGEg\n" +
        "UGguRC4gYW5kIE0uUy4gaW4gSW5vcmdhbmljL1JhZGlvY2hlbWlzdHJ5IGZyb20gRmxvcmlkYSBT\n" +
        "dGF0ZSBVbml2ZXJzaXR5IGFuZCBhIEIuUy4gaW4gQ2hlbWlzdHJ5IGZyb20gTGFuZGVyIENvbGxl\n" +
        "Z2UuDQo="
      };

      int i;
      xmlStrings = new string[originalValues.Length];
      for (i = 0; i < originalValues.Length; i++) {
        xmlStrings[i] = "<foo:Base64Binary xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:Base64Binary>\n";
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
            EventType eventType;

            n_events = 0;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SD, eventType.itemType);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
            Assert.AreEqual("Base64Binary", eventType.name);
            Assert.AreEqual("urn:foo", eventType.uri);
            ++n_events;

            exiEvent = scanner.nextEvent();
            Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
            Assert.AreEqual(values[i], exiEvent.Characters.makeString());
            eventType = exiEvent.getEventType();
            Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
            if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
              int tp = scanner.currentState.contentDatatype;
              Assert.AreEqual(EXISchemaConst.BASE64BINARY_TYPE, corpus.getSerialOfType(tp));
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
    /// Preserve lexical base64Binary values by turning on Preserve.lexicalValues.
    /// </summary>
    [Test]
    public virtual void testValidBase64BinaryRCS() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/base64Binary.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      String[] xmlStrings;
      String[] originalValues = {
          " \t\r QUJDREVGR0hJSg@==\n", // '@' will be encoded as an escaped character 
      };
      String[] parsedOriginalValues = {
          " \t\n QUJDREVGR0hJSg@==\n", 
      };

      int i;
      xmlStrings = new string[originalValues.Length];
      for (i = 0; i < originalValues.Length; i++) {
        xmlStrings[i] = "<foo:Base64Binary xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:Base64Binary>\n";
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
          EventType eventType;

          n_events = 0;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SD, eventType.itemType);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
          Assert.AreEqual("Base64Binary", eventType.name);
          Assert.AreEqual("urn:foo", eventType.uri);
          ++n_events;

          exiEvent = scanner.nextEvent();
          Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
          Assert.AreEqual(parsedOriginalValues[i], exiEvent.Characters.makeString());
          eventType = exiEvent.getEventType();
          Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
          if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
            int tp = scanner.currentState.contentDatatype;
            Assert.AreEqual(EXISchemaConst.BASE64BINARY_TYPE, corpus.getSerialOfType(tp));
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

    /// <summary>
    /// Process 1000BinaryStore
    /// </summary>
    [Test]
    public virtual void testDecode1000BinaryStore() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/DataStore/DataStore.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      String[] base64Values100 = {
        "R0lGODdhWALCov////T09M7Ozqampn19fVZWVi0tLQUFBSxYAsJAA/8Iutz+MMpJq7046827/2Ao\n",
        "/9j/4BBKRklGAQEBASwBLP/hGlZFeGlmTU0qF5ZOSUtPTiBDT1JQT1JBVElPTk5J",
        "R0lGODlhHh73KSkpOTk5QkJCSkpKUlJSWlpaY2Nja2trc3Nze3t7hISEjIyMlJSUnJycpaWlra2t\n" + "tbW1vb29xsbGzs7O1tbW3t7e5+fn7+/v//////////8=",
        "/9j/4BBKRklGAQEBAf/bQwYEBQYFBAYGBQYHBwYIChAKCgkJChQODwwQFxQYGBc=",
        "R0lGODlhHh73M2aZzP8zMzNmM5kzzDP/M2YzZmZmmWbM",
        "/9j/4BBKRklGAQEBAf/bQwYEBQYFBAYGBQYHBwYIChAKCgkJChQODwwQFxQYGBcUFhYaHSUfGhsj\n" + "HBYWICwgIyYnKSopGR8tMC0oMCUoKSj/20M=",
        "R0lGODdhWAK+ov////j4+NTU1JOTk0tLSx8fHwkJCSxYAr5AA/8IMkzjrEEmahy23SpC",
        "R0lGODdh4QIpAncJIf4aU29mdHdhcmU6IE1pY3Jvc29mdCBPZmZpY2Us4QIpAof//////8z//5n/\n",
        "R0lGODdhWAK+ov////v7++fn58DAwI6Ojl5eXjExMQMDAyxYAr5AA/8Iutz+MMpJq7046827/2Ao\n" + "jmRpnmiqPsKxvg==",
        "R0lGODdh4QIpAncJIf4aU29mdHdhcmU6IE1pY3Jvc29mdCBPZmZpY2Us4QIpAob//////8z//5n/\nzP//zMw=",
      };

      AlignmentType alignment = AlignmentType.bitPacked;

      Transmogrifier encoder = new Transmogrifier();
      encoder.GrammarCache = grammarCache;
      encoder.AlignmentType = alignment;

      MemoryStream baos = new MemoryStream();
      encoder.OutputStream = baos;

      Uri url = resolveSystemIdAsURL("/DataStore/instance/1000BinaryStore.xml");
      FileStream inputStream = new FileStream(url.LocalPath, FileMode.Open);
      encoder.encode(new InputSource<Stream>(inputStream));

      byte[] bts = baos.ToArray();

      Scanner scanner;

      int n_texts;

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;
      decoder.AlignmentType = alignment;
      decoder.InputStream = new MemoryStream(bts);
      scanner = decoder.processHeader();

      EventDescription exiEvent;
      n_texts = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        if (exiEvent.EventKind == EventDescription_Fields.EVENT_CH) {
          if (++n_texts % 100 == 0) {
            string expected = base64Values100[(n_texts / 100) - 1];
            string val = exiEvent.Characters.makeString();
            Assert.AreEqual(expected, val);
          }
        }
      }
      Assert.AreEqual(1000, n_texts);
    }

    /// <summary>
    /// Testing character buffer expansion code. Base64 text size of 95K. 
    /// This is much larger than the initial character buffer size of 4K. 
    /// </summary>
    [Test]
    public virtual void testBase64BinaryText_95K() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/base64Binary.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();

      foreach (AlignmentType alignment in Alignments) {
        Uri url = resolveSystemIdAsURL("/base64Binary_95K.xml");
        FileStream inputStream = new FileStream(url.LocalPath, FileMode.Open);
        InputSource<Stream> inputSource = new InputSource<Stream>(inputStream, url.ToString());

        Scanner scanner;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        byte[] bts;
        int n_events;

        encoder.encode(inputSource);

        bts = baos.ToArray();

        decoder.GrammarCache = grammarCache;
        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

        EventDescription exiEvent;
        EventType eventType;

        n_events = 0;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SD, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        Assert.AreEqual("Base64Binary", eventType.name);
        Assert.AreEqual("urn:foo", eventType.uri);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        string base64Text = exiEvent.Characters.makeString();
        Assert.IsTrue(base64Text.StartsWith("/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAAYEBQYFBAYGBQYHBwYIChAKCgkJChQODwwQFxQYGBcU"));
        Assert.IsTrue(base64Text.EndsWith("cdtrkLfpl+ojHqNoMgxQ97NUOAv4x/0tQVnw1EJqWd5tcy8/4GoLLP1Eo5GkdmCUVjivsf/Z"));
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        if (alignment == AlignmentType.bitPacked || alignment == AlignmentType.byteAligned) {
          int tp = scanner.currentState.contentDatatype;
          Assert.AreEqual(EXISchemaConst.BASE64BINARY_TYPE, corpus.getSerialOfType(tp));
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
        inputStream.Close();
      }
    }

  }

}