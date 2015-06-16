using System;
using System.IO;
using NUnit.Framework;

using Org.System.Xml.Sax;

using HeaderOptionsOutputType = Nagasena.Proc.HeaderOptionsOutputType;
using AlignmentType = Nagasena.Proc.Common.AlignmentType;
using GrammarOptions = Nagasena.Proc.Common.GrammarOptions;
using GrammarCache = Nagasena.Proc.Grammars.GrammarCache;
using TestBase = Nagasena.Schema.TestBase;

namespace Nagasena.Sax {

  [TestFixture]
  public class OpenDaylightTest : TestBase {

    ///////////////////////////////////////////////////////////////////////////
    // Test cases
    ///////////////////////////////////////////////////////////////////////////

    /// <summary>
    /// OpenDaylight use case revealed a problem in reusing Transmogrifier.
    /// </summary>
    [Test]
    public virtual void testReuseTransmogrifier() {

      String xml1 ="<rpc xmlns=\"urn:foo\">" +
          "<edit-config>" +
          "<target></target>" +
          "<config>" +
          "<modules xmlns=\"urn:goo\"></modules>" +
          "</config>" +
          "</edit-config>" +
          "</rpc>";

      String xml2 = "<rpc xmlns=\"urn:foo\">" +
          "<edit-config>" +
          "<target></target>" +
          "<default-operation></default-operation>" +
          "<config>" +
          "<modules xmlns=\"urn:goo\"></modules>" +
          "</config>" +
          "</edit-config>" +
          "</rpc>";
    
      GrammarCache grammarCache = new GrammarCache(GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));
    
      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.AlignmentType = AlignmentType.bitPacked;
      transmogrifier.GrammarCache = grammarCache;
      transmogrifier.OutputOptions = HeaderOptionsOutputType.none;
    
      XMLifier reader = new XMLifier();
      reader.AlignmentType = AlignmentType.bitPacked;
      reader.GrammarCache = grammarCache;

      MemoryStream outputStream;
      MemoryStream inputStream;

      outputStream = new MemoryStream();
      transmogrifier.OutputStream = outputStream;
      transmogrifier.encode(new InputSource<Stream>(string2Stream(xml1)));

      inputStream = new MemoryStream(outputStream.ToArray());
      outputStream = new MemoryStream();
      reader.Convert(inputStream, outputStream);
      Assert.AreEqual(xml1, bytesToString(outputStream.ToArray()));

      outputStream = new MemoryStream();
      transmogrifier.OutputStream = outputStream;
      transmogrifier.encode(new InputSource<Stream>(string2Stream(xml2)));

      inputStream = new MemoryStream(outputStream.ToArray());
      outputStream = new MemoryStream();
      reader.Convert(inputStream, outputStream);
      Assert.AreEqual(xml2, bytesToString(outputStream.ToArray()));
    }

  }

}