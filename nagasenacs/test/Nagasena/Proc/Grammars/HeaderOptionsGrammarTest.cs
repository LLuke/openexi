using System;
using NUnit.Framework;

using GrammarOptions = Nagasena.Proc.Common.GrammarOptions;
using ExiUriConst = Nagasena.Proc.Util.ExiUriConst;
using EXISchema = Nagasena.Schema.EXISchema;
using EXISchemaLayout = Nagasena.Schema.EXISchemaLayout;
using EXISchemaUtil = Nagasena.Schema.EXISchemaUtil;
using HeaderOptionsSchema = Nagasena.Schema.HeaderOptionsSchema;

namespace Nagasena.Proc.Grammars {

  [TestFixture]
  public class HeaderOptionsGrammarTest {

    ///////////////////////////////////////////////////////////////////////////
    // Test cases
    ///////////////////////////////////////////////////////////////////////////

    /// <summary>
    /// Vet the header options grammar.
    /// </summary>
    [Test]
    public virtual void testHeaderOptionsGrammar() {
      EXISchema schema = HeaderOptionsSchema.EXISchema;
      Assert.IsNotNull(schema);

      Assert.AreEqual(5, schema.uris.Length);
      Assert.AreEqual("", schema.uris[0]);
      Assert.AreEqual("http://www.w3.org/XML/1998/namespace", schema.uris[1]);
      Assert.AreEqual("http://www.w3.org/2001/XMLSchema-instance", schema.uris[2]);
      Assert.AreEqual("http://www.w3.org/2001/XMLSchema", schema.uris[3]);
      Assert.AreEqual("http://www.w3.org/2009/exi", schema.uris[4]);

      Assert.AreEqual(17, EXISchemaUtil.getTypeCountOfSchema("http://www.w3.org/2009/exi", schema));

      int valueMaxLength, valuePartitionCapacity, blockSize;
      valueMaxLength = valuePartitionCapacity = blockSize = EXISchema.NIL_NODE;

      for (int elem = 0; elem < schema.Elems.Length; elem += EXISchemaLayout.SZ_ELEM) {
        int uri = schema.getUriOfElem(elem);
        if (uri == ExiUriConst.W3C_2009_EXI_URI_ID) {
          string name = schema.getNameOfElem(elem);
          if ("valueMaxLength".Equals(name)) {
            valueMaxLength = elem;
          }
          else if ("valuePartitionCapacity".Equals(name)) {
            valuePartitionCapacity = elem;
          }
          else if ("blockSize".Equals(name)) {
            blockSize = elem;
          }
        }
      }

      Assert.IsTrue(valueMaxLength != EXISchema.NIL_NODE);
      Assert.IsTrue(valuePartitionCapacity != EXISchema.NIL_NODE);
      Assert.IsTrue(blockSize != EXISchema.NIL_NODE);

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      EXIGrammarUse grammarUse_valueMaxLength = grammarCache.exiGrammarUses[schema.getSerialOfElem(valueMaxLength)];
      EXIGrammarUse grammarUse_valuePartitionCapacity = grammarCache.exiGrammarUses[schema.getSerialOfElem(valuePartitionCapacity)];
      EXIGrammarUse grammarUse_blockSize = grammarCache.exiGrammarUses[schema.getSerialOfElem(blockSize)];

      // Three grammars are the same.
      Assert.AreEqual(grammarUse_valueMaxLength.exiGrammar, grammarUse_valuePartitionCapacity.exiGrammar);
      Assert.AreEqual(grammarUse_valueMaxLength.exiGrammar, grammarUse_blockSize.exiGrammar);

      Assert.IsTrue(grammarUse_valueMaxLength.contentDatatype != EXISchema.NIL_NODE);
      Assert.IsTrue(grammarUse_valuePartitionCapacity.contentDatatype != EXISchema.NIL_NODE);
      Assert.IsTrue(grammarUse_blockSize.contentDatatype != EXISchema.NIL_NODE);

      // Three contentDatatypes are different.
      Assert.IsTrue(grammarUse_valueMaxLength.contentDatatype != grammarUse_valuePartitionCapacity.contentDatatype);
      Assert.IsTrue(grammarUse_valuePartitionCapacity.contentDatatype != grammarUse_blockSize.contentDatatype);
      Assert.IsTrue(grammarUse_blockSize.contentDatatype != grammarUse_valueMaxLength.contentDatatype);
    }

  }

}