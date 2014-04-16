using System;
using NUnit.Framework;

namespace Nagasena.Proc.IO {

  using GrammarOptions = Nagasena.Proc.Common.GrammarOptions;
  using StringTable = Nagasena.Proc.Common.StringTable;
  using GrammarCache = Nagasena.Proc.Grammars.GrammarCache;
  using EXISchema = Nagasena.Schema.EXISchema;
  using EXISchemaFactoryTestUtil = Nagasena.Scomp.EXISchemaFactoryTestUtil;

  [TestFixture]
  public class URIPartitionTest {

    ///////////////////////////////////////////////////////////////////////////
    // Test cases
    ///////////////////////////////////////////////////////////////////////////

    [Test]
    public virtual void testInitialState() {
      StringTable stringTable;

      stringTable = Scriber.createStringTable(null);

      Assert.AreEqual(2, stringTable.uriWidth);
      Assert.AreEqual(2, stringTable.uriForwardedWidth);

      Assert.AreEqual(3, stringTable.n_uris);
      Assert.AreEqual(0, stringTable.getCompactIdOfURI(""));
      Assert.AreEqual(1, stringTable.getCompactIdOfURI("http://www.w3.org/XML/1998/namespace"));
      Assert.AreEqual(2, stringTable.getCompactIdOfURI("http://www.w3.org/2001/XMLSchema-instance"));
      Assert.AreEqual(-1, stringTable.getCompactIdOfURI("http://www.w3.org/2001/XMLSchema"));


      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema();

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      stringTable = Scriber.createStringTable(grammarCache);

      Assert.AreEqual(2, stringTable.uriWidth);
      Assert.AreEqual(3, stringTable.uriForwardedWidth);

      Assert.AreEqual(4, stringTable.n_uris);
      Assert.AreEqual(0, stringTable.getCompactIdOfURI(""));
      Assert.AreEqual(1, stringTable.getCompactIdOfURI("http://www.w3.org/XML/1998/namespace"));
      Assert.AreEqual(2, stringTable.getCompactIdOfURI("http://www.w3.org/2001/XMLSchema-instance"));
      Assert.AreEqual(3, stringTable.getCompactIdOfURI("http://www.w3.org/2001/XMLSchema"));
    }

    [Test]
    public virtual void testInit() {
      StringTable stringTable;

      EXISchema corpus;
      GrammarCache grammarCache;

      corpus = EXISchemaFactoryTestUtil.getEXISchema();

      grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      stringTable = Scriber.createStringTable(grammarCache);

      Assert.AreEqual(2, stringTable.uriWidth);
      Assert.AreEqual(3, stringTable.uriForwardedWidth);

      Assert.AreEqual(4, stringTable.n_uris);
      Assert.AreEqual(0, stringTable.getCompactIdOfURI(""));
      Assert.AreEqual(1, stringTable.getCompactIdOfURI("http://www.w3.org/XML/1998/namespace"));
      Assert.AreEqual(2, stringTable.getCompactIdOfURI("http://www.w3.org/2001/XMLSchema-instance"));
      Assert.AreEqual(3, stringTable.getCompactIdOfURI("http://www.w3.org/2001/XMLSchema"));

      stringTable.reset();

      Assert.AreEqual(2, stringTable.uriWidth);
      Assert.AreEqual(3, stringTable.uriForwardedWidth);

      Assert.AreEqual(4, stringTable.n_uris);
      Assert.AreEqual(0, stringTable.getCompactIdOfURI(""));
      Assert.AreEqual(1, stringTable.getCompactIdOfURI("http://www.w3.org/XML/1998/namespace"));
      Assert.AreEqual(2, stringTable.getCompactIdOfURI("http://www.w3.org/2001/XMLSchema-instance"));
      Assert.AreEqual(3, stringTable.getCompactIdOfURI("http://www.w3.org/2001/XMLSchema"));

      corpus = EXISchemaFactoryTestUtil.getEXISchema("/exi/LocationSightings/LocationSightings.xsc", this);

      grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      stringTable = Scriber.createStringTable(grammarCache);

      Assert.AreEqual(3, stringTable.uriWidth);
      Assert.AreEqual(3, stringTable.uriForwardedWidth);

      Assert.AreEqual(5, stringTable.n_uris);
      Assert.AreEqual(0, stringTable.getCompactIdOfURI(""));
      Assert.AreEqual(1, stringTable.getCompactIdOfURI("http://www.w3.org/XML/1998/namespace"));
      Assert.AreEqual(2, stringTable.getCompactIdOfURI("http://www.w3.org/2001/XMLSchema-instance"));
      Assert.AreEqual(3, stringTable.getCompactIdOfURI("http://www.w3.org/2001/XMLSchema"));
      Assert.AreEqual(4, stringTable.getCompactIdOfURI("http://berjon.com/ns/dahut-sighting"));

      stringTable.reset();

      Assert.AreEqual(3, stringTable.uriWidth);
      Assert.AreEqual(3, stringTable.uriForwardedWidth);

      Assert.AreEqual(5, stringTable.n_uris);
      Assert.AreEqual(0, stringTable.getCompactIdOfURI(""));
      Assert.AreEqual(1, stringTable.getCompactIdOfURI("http://www.w3.org/XML/1998/namespace"));
      Assert.AreEqual(2, stringTable.getCompactIdOfURI("http://www.w3.org/2001/XMLSchema-instance"));
      Assert.AreEqual(3, stringTable.getCompactIdOfURI("http://www.w3.org/2001/XMLSchema"));
      Assert.AreEqual(4, stringTable.getCompactIdOfURI("http://berjon.com/ns/dahut-sighting"));
    }

    [Test]
    public virtual void testInternString() {
      StringTable stringTable;

      stringTable = Scriber.createStringTable(null);

      Assert.AreEqual(2, stringTable.uriWidth);
      Assert.AreEqual(2, stringTable.uriForwardedWidth);
      Assert.AreEqual(3, stringTable.n_uris);

      Assert.AreEqual(3, stringTable.internURI("03"));
      Assert.AreEqual(2, stringTable.uriWidth);
      Assert.AreEqual(3, stringTable.uriForwardedWidth);
      Assert.AreEqual(4, stringTable.n_uris);
      Assert.AreEqual(4, stringTable.internURI("04"));
      Assert.AreEqual(3, stringTable.uriWidth);
      Assert.AreEqual(3, stringTable.uriForwardedWidth);
      Assert.AreEqual(5, stringTable.n_uris);
      Assert.AreEqual(5, stringTable.internURI("05"));
      Assert.AreEqual(3, stringTable.uriWidth);
      Assert.AreEqual(3, stringTable.uriForwardedWidth);
      Assert.AreEqual(6, stringTable.n_uris);
      Assert.AreEqual(6, stringTable.internURI("06"));
      Assert.AreEqual(3, stringTable.uriWidth);
      Assert.AreEqual(3, stringTable.uriForwardedWidth);
      Assert.AreEqual(7, stringTable.n_uris);
      Assert.AreEqual(3, stringTable.internURI("03"));
      Assert.AreEqual(7, stringTable.internURI("07"));
      Assert.AreEqual(3, stringTable.uriWidth);
      Assert.AreEqual(4, stringTable.uriForwardedWidth);
      Assert.AreEqual(8, stringTable.n_uris);
      Assert.AreEqual(8, stringTable.internURI("08"));
      Assert.AreEqual(4, stringTable.uriWidth);
      Assert.AreEqual(4, stringTable.uriForwardedWidth);
      Assert.AreEqual(9, stringTable.n_uris);


      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema();

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      stringTable = Scriber.createStringTable(grammarCache);

      Assert.AreEqual(2, stringTable.uriWidth);
      Assert.AreEqual(3, stringTable.uriForwardedWidth);
      Assert.AreEqual(4, stringTable.n_uris);

      Assert.AreEqual(4, stringTable.internURI("04"));
      Assert.AreEqual(3, stringTable.uriWidth);
      Assert.AreEqual(3, stringTable.uriForwardedWidth);
      Assert.AreEqual(5, stringTable.n_uris);
      Assert.AreEqual(5, stringTable.internURI("05"));
      Assert.AreEqual(3, stringTable.uriWidth);
      Assert.AreEqual(3, stringTable.uriForwardedWidth);
      Assert.AreEqual(6, stringTable.n_uris);
      Assert.AreEqual(6, stringTable.internURI("06"));
      Assert.AreEqual(3, stringTable.uriWidth);
      Assert.AreEqual(3, stringTable.uriForwardedWidth);
      Assert.AreEqual(7, stringTable.n_uris);
      Assert.AreEqual(4, stringTable.internURI("04"));
      Assert.AreEqual(7, stringTable.internURI("07"));
      Assert.AreEqual(3, stringTable.uriWidth);
      Assert.AreEqual(4, stringTable.uriForwardedWidth);
      Assert.AreEqual(8, stringTable.n_uris);
      Assert.AreEqual(8, stringTable.internURI("08"));
      Assert.AreEqual(4, stringTable.uriWidth);
      Assert.AreEqual(4, stringTable.uriForwardedWidth);
      Assert.AreEqual(9, stringTable.n_uris);
    }

  }

}