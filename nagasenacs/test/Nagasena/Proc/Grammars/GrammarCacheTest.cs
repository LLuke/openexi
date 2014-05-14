using System;
using NUnit.Framework;

using GrammarOptions = Nagasena.Proc.Common.GrammarOptions;
using EXISchema = Nagasena.Schema.EXISchema;
using EXISchemaLayout = Nagasena.Schema.EXISchemaLayout;
using EXISchemaFactoryTestUtil = Nagasena.Scomp.EXISchemaFactoryTestUtil;

namespace Nagasena.Proc.Grammars {

  [TestFixture]
  public class GrammarCacheTest {

    [SetUp]
    public void setUp() {
    }

    /// <summary>
    /// Tests EXIGrammar's internalization.
    /// </summary>
    [Test]
    public virtual void testEXIGrammarInternalization() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/verySimple.gram", this);

      int eB, eC, eD, eE, ee, ef, eg, eh, ei, ej;
      eB = eC = eD = eE = ee = ef = eg = eh = ei = ej = EXISchema.NIL_NODE;

      for (int elem = 0; elem < schema.Elems.Length; elem += EXISchemaLayout.SZ_ELEM) {
        string uri = schema.uris[schema.getUriOfElem(elem)];
        if ("urn:foo".Equals(uri)) {
          string name = schema.getNameOfElem(elem);
          if ("B".Equals(name)) {
            eB = elem;
          }
          else if ("C".Equals(name)) {
            eC = elem;
          }
          else if ("D".Equals(name)) {
            eD = elem;
          }
          else if ("E".Equals(name)) {
            eE = elem;
          }
        }
        else if ("urn:goo".Equals(uri)) {
          string name = schema.getNameOfElem(elem);
          if ("e".Equals(name)) {
            ee = elem;
          }
          else if ("f".Equals(name)) {
            ef = elem;
          }
          else if ("g".Equals(name)) {
            eg = elem;
          }
          else if ("h".Equals(name)) {
            eh = elem;
          }
          else if ("i".Equals(name)) {
            ei = elem;
          }
          else if ("j".Equals(name)) {
            ej = elem;
          }
        }
      }

      Assert.IsTrue(eB != EXISchema.NIL_NODE);
      Assert.IsTrue(eC != EXISchema.NIL_NODE);
      Assert.IsTrue(eD != EXISchema.NIL_NODE);
      Assert.IsTrue(eE != EXISchema.NIL_NODE);
      Assert.IsTrue(ee != EXISchema.NIL_NODE);
      Assert.IsTrue(ef != EXISchema.NIL_NODE);
      Assert.IsTrue(eg != EXISchema.NIL_NODE);
      Assert.IsTrue(eh != EXISchema.NIL_NODE);
      Assert.IsTrue(ei != EXISchema.NIL_NODE);
      Assert.IsTrue(ej != EXISchema.NIL_NODE);

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      EXIGrammarUse grammarUse_B = grammarCache.exiGrammarUses[schema.getSerialOfElem(eB)];
      EXIGrammarUse grammarUse_C = grammarCache.exiGrammarUses[schema.getSerialOfElem(eC)];
      EXIGrammarUse grammarUse_D = grammarCache.exiGrammarUses[schema.getSerialOfElem(eD)];
      EXIGrammarUse grammarUse_E = grammarCache.exiGrammarUses[schema.getSerialOfElem(eE)];
      EXIGrammarUse grammarUse_e = grammarCache.exiGrammarUses[schema.getSerialOfElem(ee)];
      EXIGrammarUse grammarUse_f = grammarCache.exiGrammarUses[schema.getSerialOfElem(ef)];
      EXIGrammarUse grammarUse_g = grammarCache.exiGrammarUses[schema.getSerialOfElem(eg)];
      EXIGrammarUse grammarUse_h = grammarCache.exiGrammarUses[schema.getSerialOfElem(eh)];
      EXIGrammarUse grammarUse_i = grammarCache.exiGrammarUses[schema.getSerialOfElem(ei)];
      EXIGrammarUse grammarUse_j = grammarCache.exiGrammarUses[schema.getSerialOfElem(ej)];

      Assert.IsTrue(grammarUse_B.exiGrammar != grammarUse_E.exiGrammar);

      // Elements C and D have properties (nillable, typable) = (false, false)
      Assert.AreEqual(grammarUse_C.exiGrammar, grammarUse_D.exiGrammar);
      // Elements e and f have properties (nillable, typable) = (true, false)
      Assert.AreEqual(grammarUse_e.exiGrammar, grammarUse_f.exiGrammar);
      // Elements g and h have properties (nillable, typable) = (false, true)
      Assert.AreEqual(grammarUse_g.exiGrammar, grammarUse_h.exiGrammar);
      // Elements i and j have properties (nillable, typable) = (true, true)
      Assert.AreEqual(grammarUse_i.exiGrammar, grammarUse_j.exiGrammar);
    }

  }

}