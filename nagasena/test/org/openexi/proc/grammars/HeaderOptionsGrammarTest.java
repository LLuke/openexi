package org.openexi.proc.grammars;

import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.util.ExiUriConst;
import org.openexi.schema.EXISchema;
import org.openexi.schema.EXISchemaLayout;
import org.openexi.schema.EXISchemaUtil;
import org.openexi.schema.HeaderOptionsSchema;

import junit.framework.Assert;
import junit.framework.TestCase;

public class HeaderOptionsGrammarTest extends TestCase {

  public HeaderOptionsGrammarTest(String name) {
    super(name);
  }

  ///////////////////////////////////////////////////////////////////////////
  // Test cases
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Vet the header options grammar.
   */
  public void testHeaderOptionsGrammar() throws Exception {
    EXISchema schema = HeaderOptionsSchema.getEXISchema();
    Assert.assertNotNull(schema);
    
    Assert.assertEquals(5, schema.uris.length);
    Assert.assertEquals("", schema.uris[0]); 
    Assert.assertEquals("http://www.w3.org/XML/1998/namespace", schema.uris[1]); 
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema-instance", schema.uris[2]); 
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema", schema.uris[3]); 
    Assert.assertEquals("http://www.w3.org/2009/exi", schema.uris[4]); 

    Assert.assertEquals(17, EXISchemaUtil.getTypeCountOfSchema("http://www.w3.org/2009/exi", schema));
    
    int valueMaxLength, valuePartitionCapacity, blockSize;
    valueMaxLength = valuePartitionCapacity = blockSize = EXISchema.NIL_NODE;
    
    for (int elem = 0; elem < schema.getElems().length; elem += EXISchemaLayout.SZ_ELEM) {
      final int uri = schema.getUriOfElem(elem);
      if (uri == ExiUriConst.W3C_2009_EXI_URI_ID) {
        final String name = schema.getNameOfElem(elem);
        if ("valueMaxLength".equals(name))
          valueMaxLength = elem;
        else if ("valuePartitionCapacity".equals(name))
          valuePartitionCapacity = elem;
        else if ("blockSize".equals(name))
          blockSize = elem;
      }
    }
    
    Assert.assertTrue(valueMaxLength != EXISchema.NIL_NODE);
    Assert.assertTrue(valuePartitionCapacity != EXISchema.NIL_NODE);
    Assert.assertTrue(blockSize != EXISchema.NIL_NODE);

    GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

    EXIGrammarUse grammarUse_valueMaxLength = grammarCache.exiGrammarUses[schema.getSerialOfElem(valueMaxLength)];
    EXIGrammarUse grammarUse_valuePartitionCapacity = grammarCache.exiGrammarUses[schema.getSerialOfElem(valuePartitionCapacity)];
    EXIGrammarUse grammarUse_blockSize = grammarCache.exiGrammarUses[schema.getSerialOfElem(blockSize)];
    
    // Three grammars are the same.
    Assert.assertEquals(grammarUse_valueMaxLength.exiGrammar, grammarUse_valuePartitionCapacity.exiGrammar);
    Assert.assertEquals(grammarUse_valueMaxLength.exiGrammar, grammarUse_blockSize.exiGrammar);
    
    Assert.assertTrue(grammarUse_valueMaxLength.contentDatatype != EXISchema.NIL_NODE);
    Assert.assertTrue(grammarUse_valuePartitionCapacity.contentDatatype != EXISchema.NIL_NODE);
    Assert.assertTrue(grammarUse_blockSize.contentDatatype != EXISchema.NIL_NODE);
    
    // Three contentDatatypes are different.
    Assert.assertTrue(grammarUse_valueMaxLength.contentDatatype != grammarUse_valuePartitionCapacity.contentDatatype);
    Assert.assertTrue(grammarUse_valuePartitionCapacity.contentDatatype != grammarUse_blockSize.contentDatatype);
    Assert.assertTrue(grammarUse_blockSize.contentDatatype != grammarUse_valueMaxLength.contentDatatype);
  }

}
