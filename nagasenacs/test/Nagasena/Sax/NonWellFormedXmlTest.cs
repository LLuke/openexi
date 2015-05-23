using System;
using System.IO;
using System.Xml;
using NUnit.Framework;

using Org.System.Xml.Sax;

using GrammarOptions = Nagasena.Proc.Common.GrammarOptions;
using GrammarCache = Nagasena.Proc.Grammars.GrammarCache;
using TestBase = Nagasena.Schema.TestBase;

namespace Nagasena.Sax {

  [TestFixture]
  public class NonWellFormedXmlTest : TestBase {

    ///////////////////////////////////////////////////////////////////////////
    // Test cases
    ///////////////////////////////////////////////////////////////////////////

    /// <summary>
    /// Not a well-formed XML 
    /// </summary>
    [Test]
    public virtual void testNoEndTag() {

      string xmlString;
      xmlString = "<?xml version='1.0' ?>" +
        "<None xmlns='urn:foo'>\n" +
        "  <A>\n" +
        "</None>";

      Transmogrifier encoder = new Transmogrifier();

      encoder.GrammarCache = new GrammarCache(GrammarOptions.DEFAULT_OPTIONS);
      MemoryStream baos = new MemoryStream();
      encoder.OutputStream = baos;

      try {
        encoder.encode(new InputSource<Stream>(string2Stream(xmlString)));
      }
      catch (XmlException xe) {
        Assert.AreEqual(3, xe.LineNumber);
        return;
      }
      Assert.Fail();
    }

  }

}