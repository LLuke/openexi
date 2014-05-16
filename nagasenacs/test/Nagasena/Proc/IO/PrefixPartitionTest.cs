using System;
using NUnit.Framework;

using StringTable = Nagasena.Proc.Common.StringTable;
using XmlUriConst = Nagasena.Proc.Common.XmlUriConst;
using GrammarCache = Nagasena.Proc.Grammars.GrammarCache;
using EmptySchema = Nagasena.Schema.EmptySchema;

namespace Nagasena.Proc.IO {

  [TestFixture]
  public class PrefixPartitionTest : Nagasena.LocaleLauncher {

    ///////////////////////////////////////////////////////////////////////////
    // Test cases
    ///////////////////////////////////////////////////////////////////////////

    [Test]
    public virtual void testDefaultPrefixes() {

      StringTable stringTable;
      StringTable.PrefixPartition prefixPartition;

      stringTable = Scriber.createStringTable(null);

      int uriId = stringTable.getCompactIdOfURI("");
      prefixPartition = stringTable.getPrefixPartition(uriId);

      Assert.AreEqual(0, prefixPartition.width);
      Assert.AreEqual(1, prefixPartition.forwardedWidth);

      Assert.AreEqual(1, prefixPartition.n_strings);
      Assert.AreEqual(0, prefixPartition.getCompactId(""));

      prefixPartition.addPrefix("a");
      Assert.AreEqual(2, prefixPartition.n_strings);
      Assert.AreEqual(1, prefixPartition.width);
      Assert.AreEqual(2, prefixPartition.forwardedWidth);
      prefixPartition.addPrefix("b");
      Assert.AreEqual(3, prefixPartition.n_strings);
      Assert.AreEqual(2, prefixPartition.width);
      Assert.AreEqual(2, prefixPartition.forwardedWidth);
      prefixPartition.addPrefix("c");
      Assert.AreEqual(4, prefixPartition.n_strings);
      Assert.AreEqual(2, prefixPartition.width);
      Assert.AreEqual(3, prefixPartition.forwardedWidth);
      prefixPartition.addPrefix("d");
      Assert.AreEqual(5, prefixPartition.n_strings);
      Assert.AreEqual(3, prefixPartition.width);
      Assert.AreEqual(3, prefixPartition.forwardedWidth);
      prefixPartition.addPrefix("e");
      Assert.AreEqual(6, prefixPartition.n_strings);
      Assert.AreEqual(3, prefixPartition.width);
      Assert.AreEqual(3, prefixPartition.forwardedWidth);
      prefixPartition.addPrefix("f");
      Assert.AreEqual(7, prefixPartition.n_strings);
      Assert.AreEqual(3, prefixPartition.width);
      Assert.AreEqual(3, prefixPartition.forwardedWidth);
      prefixPartition.addPrefix("g");
      Assert.AreEqual(8, prefixPartition.n_strings);
      Assert.AreEqual(3, prefixPartition.width);
      Assert.AreEqual(4, prefixPartition.forwardedWidth);

      prefixPartition.reset();

      Assert.AreEqual(0, prefixPartition.width);
      Assert.AreEqual(1, prefixPartition.forwardedWidth);

      Assert.AreEqual(1, prefixPartition.n_strings);
      Assert.AreEqual(0, prefixPartition.getCompactId(""));
    }

    [Test]
    public virtual void testXmlPrefixes() {

      StringTable stringTable;
      StringTable.PrefixPartition prefixPartition;

      stringTable = Scriber.createStringTable(null);

      int uriId = stringTable.getCompactIdOfURI(XmlUriConst.W3C_XML_1998_URI);
      prefixPartition = stringTable.getPrefixPartition(uriId);

      Assert.AreEqual(0, prefixPartition.width);
      Assert.AreEqual(1, prefixPartition.forwardedWidth);

      Assert.AreEqual(1, prefixPartition.n_strings);
      Assert.AreEqual(0, prefixPartition.getCompactId("xml"));

      prefixPartition.addPrefix("a");
      Assert.AreEqual(2, prefixPartition.n_strings);
      Assert.AreEqual(1, prefixPartition.width);
      Assert.AreEqual(2, prefixPartition.forwardedWidth);
      prefixPartition.addPrefix("b");
      Assert.AreEqual(3, prefixPartition.n_strings);
      Assert.AreEqual(2, prefixPartition.width);
      Assert.AreEqual(2, prefixPartition.forwardedWidth);
      prefixPartition.addPrefix("c");
      Assert.AreEqual(4, prefixPartition.n_strings);
      Assert.AreEqual(2, prefixPartition.width);
      Assert.AreEqual(3, prefixPartition.forwardedWidth);
      prefixPartition.addPrefix("d");
      Assert.AreEqual(5, prefixPartition.n_strings);
      Assert.AreEqual(3, prefixPartition.width);
      Assert.AreEqual(3, prefixPartition.forwardedWidth);
      prefixPartition.addPrefix("e");
      Assert.AreEqual(6, prefixPartition.n_strings);
      Assert.AreEqual(3, prefixPartition.width);
      Assert.AreEqual(3, prefixPartition.forwardedWidth);
      prefixPartition.addPrefix("f");
      Assert.AreEqual(7, prefixPartition.n_strings);
      Assert.AreEqual(3, prefixPartition.width);
      Assert.AreEqual(3, prefixPartition.forwardedWidth);
      prefixPartition.addPrefix("g");
      Assert.AreEqual(8, prefixPartition.n_strings);
      Assert.AreEqual(3, prefixPartition.width);
      Assert.AreEqual(4, prefixPartition.forwardedWidth);

      prefixPartition.reset();

      Assert.AreEqual(0, prefixPartition.width);
      Assert.AreEqual(1, prefixPartition.forwardedWidth);

      Assert.AreEqual(1, prefixPartition.n_strings);
      Assert.AreEqual(0, prefixPartition.getCompactId("xml"));
    }

    [Test]
    public virtual void testXsiPrefixes() {

      StringTable stringTable;
      StringTable.PrefixPartition prefixPartition;

      stringTable = Scriber.createStringTable(null);

      int uriId = stringTable.getCompactIdOfURI(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI);
      prefixPartition = stringTable.getPrefixPartition(uriId);

      Assert.AreEqual(0, prefixPartition.width);
      Assert.AreEqual(1, prefixPartition.forwardedWidth);

      Assert.AreEqual(1, prefixPartition.n_strings);
      Assert.AreEqual(0, prefixPartition.getCompactId("xsi"));

      prefixPartition.addPrefix("a");
      Assert.AreEqual(2, prefixPartition.n_strings);
      Assert.AreEqual(1, prefixPartition.width);
      Assert.AreEqual(2, prefixPartition.forwardedWidth);
      prefixPartition.addPrefix("b");
      Assert.AreEqual(3, prefixPartition.n_strings);
      Assert.AreEqual(2, prefixPartition.width);
      Assert.AreEqual(2, prefixPartition.forwardedWidth);
      prefixPartition.addPrefix("c");
      Assert.AreEqual(4, prefixPartition.n_strings);
      Assert.AreEqual(2, prefixPartition.width);
      Assert.AreEqual(3, prefixPartition.forwardedWidth);
      prefixPartition.addPrefix("d");
      Assert.AreEqual(5, prefixPartition.n_strings);
      Assert.AreEqual(3, prefixPartition.width);
      Assert.AreEqual(3, prefixPartition.forwardedWidth);
      prefixPartition.addPrefix("e");
      Assert.AreEqual(6, prefixPartition.n_strings);
      Assert.AreEqual(3, prefixPartition.width);
      Assert.AreEqual(3, prefixPartition.forwardedWidth);
      prefixPartition.addPrefix("f");
      Assert.AreEqual(7, prefixPartition.n_strings);
      Assert.AreEqual(3, prefixPartition.width);
      Assert.AreEqual(3, prefixPartition.forwardedWidth);
      prefixPartition.addPrefix("g");
      Assert.AreEqual(8, prefixPartition.n_strings);
      Assert.AreEqual(3, prefixPartition.width);
      Assert.AreEqual(4, prefixPartition.forwardedWidth);

      prefixPartition.reset();

      Assert.AreEqual(0, prefixPartition.width);
      Assert.AreEqual(1, prefixPartition.forwardedWidth);

      Assert.AreEqual(1, prefixPartition.n_strings);
      Assert.AreEqual(0, prefixPartition.getCompactId("xsi"));
    }

    [Test]
    public virtual void testXsdPrefixes() {

      StringTable stringTable;
      StringTable.PrefixPartition prefixPartition;

      stringTable = Scriber.createStringTable(new GrammarCache(EmptySchema.EXISchema));

      int uriId = stringTable.getCompactIdOfURI(XmlUriConst.W3C_2001_XMLSCHEMA_URI);
      prefixPartition = stringTable.getPrefixPartition(uriId);

      // There are no prefixes initially associated with the XML Schema namespace
      Assert.AreEqual(0, prefixPartition.width);
      Assert.AreEqual(0, prefixPartition.forwardedWidth);
      Assert.AreEqual(0, prefixPartition.n_strings);

      prefixPartition.addPrefix("a");
      Assert.AreEqual(1, prefixPartition.n_strings);
      Assert.AreEqual(0, prefixPartition.width);
      Assert.AreEqual(1, prefixPartition.forwardedWidth);
      prefixPartition.addPrefix("b");
      Assert.AreEqual(2, prefixPartition.n_strings);
      Assert.AreEqual(1, prefixPartition.width);
      Assert.AreEqual(2, prefixPartition.forwardedWidth);
      prefixPartition.addPrefix("c");
      Assert.AreEqual(3, prefixPartition.n_strings);
      Assert.AreEqual(2, prefixPartition.width);
      Assert.AreEqual(2, prefixPartition.forwardedWidth);
      prefixPartition.addPrefix("d");
      Assert.AreEqual(4, prefixPartition.n_strings);
      Assert.AreEqual(2, prefixPartition.width);
      Assert.AreEqual(3, prefixPartition.forwardedWidth);
      prefixPartition.addPrefix("e");
      Assert.AreEqual(5, prefixPartition.n_strings);
      Assert.AreEqual(3, prefixPartition.width);
      Assert.AreEqual(3, prefixPartition.forwardedWidth);
      prefixPartition.addPrefix("f");
      Assert.AreEqual(6, prefixPartition.n_strings);
      Assert.AreEqual(3, prefixPartition.width);
      Assert.AreEqual(3, prefixPartition.forwardedWidth);
      prefixPartition.addPrefix("g");
      Assert.AreEqual(7, prefixPartition.n_strings);
      Assert.AreEqual(3, prefixPartition.width);
      Assert.AreEqual(3, prefixPartition.forwardedWidth);
      prefixPartition.addPrefix("h");
      Assert.AreEqual(8, prefixPartition.n_strings);
      Assert.AreEqual(3, prefixPartition.width);
      Assert.AreEqual(4, prefixPartition.forwardedWidth);

      prefixPartition.reset();

      Assert.AreEqual(0, prefixPartition.width);
      Assert.AreEqual(0, prefixPartition.forwardedWidth);
      Assert.AreEqual(0, prefixPartition.n_strings);
    }

  }

}