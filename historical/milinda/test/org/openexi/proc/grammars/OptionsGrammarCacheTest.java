package org.openexi.proc.grammars;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.openexi.schema.EXISchema;

public class OptionsGrammarCacheTest extends TestCase {

  public OptionsGrammarCacheTest(String name) {
    super(name);
  }

  ///////////////////////////////////////////////////////////////////////////
  // Test cases
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Make sure all entries are readily cached.
   */
  public void testCachedEntries() throws Exception {
    GrammarCache grammarCache = OptionsGrammarCache.getGrammarCache();
    Assert.assertNotNull(grammarCache);
    EXISchema schema = grammarCache.getEXISchema();
    final int n_elems = schema.getTotalElemCount();
    final int n_types = schema.getTotalTypeCount();
    int i;
    for (i = 0; i < n_elems; i++) {
      Assert.assertNotNull(grammarCache.getCachedGrammar(Grammar.SCHEMA_GRAMMAR_ELEMENT, i));
    }
    for (i = 0; i < n_types; i++) {
      final ElementTagGrammar tagGrammar;
      tagGrammar = (ElementTagGrammar)grammarCache.getCachedGrammar(Grammar.SCHEMA_GRAMMAR_ELEMENT_TAG, i);
      Assert.assertNotNull(tagGrammar);
      final ContentGrammar contentGrammar;
      contentGrammar = (ContentGrammar)grammarCache.getCachedGrammar(Grammar.SCHEMA_GRAMMAR_ELEMENT_CONTENT, i);
      Assert.assertNotNull(contentGrammar);
    }
  }
  
}
