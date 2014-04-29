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
  public class ValuePartitionCapacityTest : TestBase {

    ///////////////////////////////////////////////////////////////////////////
    // Test cases
    ///////////////////////////////////////////////////////////////////////////

    [Test]
    public virtual void testGlobalPartition() {

      string[] values = new string[] { "val1", "val2", "val3", "val2", "val1" };

      GrammarCache grammarCache = new GrammarCache(EmptySchema.EXISchema, GrammarOptions.DEFAULT_OPTIONS);

      Transmogrifier encoder = new Transmogrifier();
      EXIDecoder decoder = new EXIDecoder();
      Scanner scanner;
      InputSource inputSource;

      encoder.AlignmentType = AlignmentType.bitPacked;
      encoder.ValuePartitionCapacity = 2;
      encoder.OutputOptions = HeaderOptionsOutputType.lessSchemaId;

      encoder.GrammarCache = grammarCache;
      MemoryStream baos = new MemoryStream();
      encoder.OutputStream = baos;

      Uri url = resolveSystemIdAsURL("/interop/datatypes/string/indexed-05.xml");
      FileStream inputStream = new FileStream(url.LocalPath, FileMode.Open);
      inputSource = new InputSource<Stream>(inputStream, url.ToString());

      byte[] bts;
      int n_texts;

      encoder.encode(inputSource);
      inputStream.Close();

      bts = baos.ToArray();

      decoder.GrammarCache = grammarCache;
      decoder.InputStream = new MemoryStream(bts);
      scanner = decoder.processHeader();
      Assert.AreEqual(2, scanner.HeaderOptions.ValuePartitionCapacity);

      List<EventDescription> exiEventList = new List<EventDescription>();

      EventDescription exiEvent;
      string stringValue = null;
      n_texts = 0;
      while ((exiEvent = scanner.nextEvent()) != null) {
        if (exiEvent.EventKind == EventDescription_Fields.EVENT_CH) {
          stringValue = exiEvent.Characters.makeString();
          Assert.AreEqual(values[n_texts], stringValue);
          ++n_texts;
        }
        exiEventList.Add(exiEvent);
      }
    }

  }

}