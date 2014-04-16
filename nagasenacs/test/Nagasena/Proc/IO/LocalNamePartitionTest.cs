using System;
using NUnit.Framework;

using GrammarOptions = Nagasena.Proc.Common.GrammarOptions;
using IGrammar = Nagasena.Proc.Common.IGrammar;
using StringTable = Nagasena.Proc.Common.StringTable;
using XmlUriConst = Nagasena.Proc.Common.XmlUriConst;
using GrammarCache = Nagasena.Proc.Grammars.GrammarCache;
using EXISchema = Nagasena.Schema.EXISchema;
using EXISchemaFactoryTestUtil = Nagasena.Scomp.EXISchemaFactoryTestUtil;

namespace Nagasena.Proc.IO {

  [TestFixture]
  public class LocalNamePartitionTest {

    ///////////////////////////////////////////////////////////////////////////
    // Test cases
    ///////////////////////////////////////////////////////////////////////////

    [Test]
    public virtual void testXmlNames() {

      StringTable stringTable;
      StringTable.LocalNamePartition localNamePartition;

      stringTable = Scriber.createStringTable(null);

      int uriId = stringTable.getCompactIdOfURI(XmlUriConst.W3C_XML_1998_URI);
      localNamePartition = stringTable.getLocalNamePartition(uriId);

      Assert.AreEqual(2, localNamePartition.width);

      Assert.AreEqual(4, localNamePartition.n_strings);
      Assert.AreEqual(0, localNamePartition.getCompactId("base"));
      Assert.AreEqual(1, localNamePartition.getCompactId("id"));
      Assert.AreEqual(2, localNamePartition.getCompactId("lang"));
      Assert.AreEqual(3, localNamePartition.getCompactId("space"));

      localNamePartition.addName("a", (IGrammar)null);
      Assert.AreEqual(5, localNamePartition.n_strings);
      Assert.AreEqual(3, localNamePartition.width);
      localNamePartition.addName("b", (IGrammar)null);
      Assert.AreEqual(6, localNamePartition.n_strings);
      Assert.AreEqual(3, localNamePartition.width);
      localNamePartition.addName("c", (IGrammar)null);
      Assert.AreEqual(7, localNamePartition.n_strings);
      Assert.AreEqual(3, localNamePartition.width);
      localNamePartition.addName("d", (IGrammar)null);
      Assert.AreEqual(8, localNamePartition.n_strings);
      Assert.AreEqual(3, localNamePartition.width);
      localNamePartition.addName("e", (IGrammar)null);
      Assert.AreEqual(9, localNamePartition.n_strings);
      Assert.AreEqual(4, localNamePartition.width);

      localNamePartition.reset();

      Assert.AreEqual(2, localNamePartition.width);

      Assert.AreEqual(4, localNamePartition.n_strings);
      Assert.AreEqual(0, localNamePartition.getCompactId("base"));
      Assert.AreEqual(1, localNamePartition.getCompactId("id"));
      Assert.AreEqual(2, localNamePartition.getCompactId("lang"));
      Assert.AreEqual(3, localNamePartition.getCompactId("space"));
    }

    [Test]
    public virtual void testXsiNames() {

      StringTable stringTable;
      StringTable.LocalNamePartition localNamePartition;

      stringTable = Scriber.createStringTable(null);

      int uriId = stringTable.getCompactIdOfURI(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI);
      localNamePartition = stringTable.getLocalNamePartition(uriId);

      Assert.AreEqual(1, localNamePartition.width);

      Assert.AreEqual(2, localNamePartition.n_strings);
      Assert.AreEqual(0, localNamePartition.getCompactId("nil"));
      Assert.AreEqual(1, localNamePartition.getCompactId("type"));

      localNamePartition.addName("a", (IGrammar)null);
      Assert.AreEqual(3, localNamePartition.n_strings);
      Assert.AreEqual(2, localNamePartition.width);
      localNamePartition.addName("b", (IGrammar)null);
      Assert.AreEqual(4, localNamePartition.n_strings);
      Assert.AreEqual(2, localNamePartition.width);
      localNamePartition.addName("c", (IGrammar)null);
      Assert.AreEqual(5, localNamePartition.n_strings);
      Assert.AreEqual(3, localNamePartition.width);

      localNamePartition.reset();

      Assert.AreEqual(1, localNamePartition.width);

      Assert.AreEqual(2, localNamePartition.n_strings);
      Assert.AreEqual(0, localNamePartition.getCompactId("nil"));
      Assert.AreEqual(1, localNamePartition.getCompactId("type"));
    }

    [Test]
    public virtual void testXsdNames() {

      StringTable stringTable;
      StringTable.LocalNamePartition localNamePartition;

      stringTable = Scriber.createStringTable(null);

      int uriId;

      uriId = stringTable.getCompactIdOfURI(XmlUriConst.W3C_2001_XMLSCHEMA_URI);
      Assert.AreEqual(-1, uriId);

      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema();

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      stringTable = Scriber.createStringTable(grammarCache);

      uriId = stringTable.getCompactIdOfURI(XmlUriConst.W3C_2001_XMLSCHEMA_URI);
      localNamePartition = stringTable.getLocalNamePartition(uriId);

      Assert.AreEqual(6, localNamePartition.width);

      Assert.AreEqual(46, localNamePartition.n_strings);
      Assert.AreEqual(0, localNamePartition.getCompactId("ENTITIES"));
      Assert.AreEqual(10, localNamePartition.getCompactId("QName"));
      Assert.AreEqual(20, localNamePartition.getCompactId("double"));
      Assert.AreEqual(30, localNamePartition.getCompactId("integer"));
      Assert.AreEqual(40, localNamePartition.getCompactId("time"));
      Assert.AreEqual(45, localNamePartition.getCompactId("unsignedShort"));

      for (int i = 46; i < 64; i++) {
        int c = 'a' + (64 - i);
        localNamePartition.addName(new string(new char[] { (char)c }), (IGrammar)null);
      }
      Assert.AreEqual(64, localNamePartition.n_strings);
      Assert.AreEqual(6, localNamePartition.width);
      localNamePartition.addName("z", (IGrammar)null);
      Assert.AreEqual(65, localNamePartition.n_strings);
      Assert.AreEqual(7, localNamePartition.width);

      localNamePartition.reset();

      Assert.AreEqual(6, localNamePartition.width);

      Assert.AreEqual(46, localNamePartition.n_strings);
      Assert.AreEqual(0, localNamePartition.getCompactId("ENTITIES"));
      Assert.AreEqual(10, localNamePartition.getCompactId("QName"));
      Assert.AreEqual(20, localNamePartition.getCompactId("double"));
      Assert.AreEqual(30, localNamePartition.getCompactId("integer"));
      Assert.AreEqual(40, localNamePartition.getCompactId("time"));
      Assert.AreEqual(45, localNamePartition.getCompactId("unsignedShort"));
    }

  }

}