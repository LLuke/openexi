package org.openexi.proc.grammars;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.openexi.proc.common.GrammarOptions;
import org.openexi.schema.EXISchema;
import org.openexi.schema.EXISchemaLayout;
import org.openexi.scomp.EXISchemaFactoryErrorMonitor;
import org.openexi.scomp.EXISchemaFactoryTestUtil;

public class GrammarCacheTest extends TestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  private EXISchemaFactoryErrorMonitor m_compilerErrorHandler;

  /**
   * Tests EXIGrammar's internalization.
   */
  public void testEXIGrammarInternalization() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema(
        "/verySimple.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    int eB, eC, eD, eE, ee, ef, eg, eh, ei, ej;
    eB = eC = eD = eE = ee = ef = eg = eh = ei = ej = EXISchema.NIL_NODE;
    
    for (int elem = 0; elem < schema.getElems().length; elem += EXISchemaLayout.SZ_ELEM) {
      final String uri = schema.uris[schema.getUriOfElem(elem)];
      if ("urn:foo".equals(uri)) {
        final String name = schema.getNameOfElem(elem);
        if ("B".equals(name))
          eB = elem;
        else if ("C".equals(name))
          eC = elem;
        else if ("D".equals(name))
          eD = elem;
        else if ("E".equals(name))
          eE = elem;
      }
      else if ("urn:goo".equals(uri)) {
        final String name = schema.getNameOfElem(elem);
        if ("e".equals(name))
          ee = elem;
        else if ("f".equals(name))
          ef = elem;
        else if ("g".equals(name))
          eg = elem;
        else if ("h".equals(name))
          eh = elem;
        else if ("i".equals(name))
          ei = elem;
        else if ("j".equals(name))
          ej = elem;
      }
    }
    
    Assert.assertTrue(eB != EXISchema.NIL_NODE);
    Assert.assertTrue(eC != EXISchema.NIL_NODE);
    Assert.assertTrue(eD != EXISchema.NIL_NODE);
    Assert.assertTrue(eE != EXISchema.NIL_NODE);
    Assert.assertTrue(ee != EXISchema.NIL_NODE);
    Assert.assertTrue(ef != EXISchema.NIL_NODE);
    Assert.assertTrue(eg != EXISchema.NIL_NODE);
    Assert.assertTrue(eh != EXISchema.NIL_NODE);
    Assert.assertTrue(ei != EXISchema.NIL_NODE);
    Assert.assertTrue(ej != EXISchema.NIL_NODE);

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

    Assert.assertTrue(grammarUse_B.exiGrammar != grammarUse_E.exiGrammar);
    
    // Elements C and D have properties (nillable, typable) = (false, false)
    Assert.assertEquals(grammarUse_C.exiGrammar, grammarUse_D.exiGrammar);
    // Elements e and f have properties (nillable, typable) = (true, false)
    Assert.assertEquals(grammarUse_e.exiGrammar, grammarUse_f.exiGrammar);
    // Elements g and h have properties (nillable, typable) = (false, true)
    Assert.assertEquals(grammarUse_g.exiGrammar, grammarUse_h.exiGrammar);
    // Elements i and j have properties (nillable, typable) = (true, true)
    Assert.assertEquals(grammarUse_i.exiGrammar, grammarUse_j.exiGrammar);
  }
  
}
