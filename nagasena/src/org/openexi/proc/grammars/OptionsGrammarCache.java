package org.openexi.proc.grammars;

import org.openexi.proc.common.GrammarOptions;
import org.openexi.schema.EXISchema;
import org.openexi.schema.HeaderOptionsSchema;

public final class OptionsGrammarCache {
  
  private static final GrammarCache m_grammarCache;
  static {
    EXISchema schema = HeaderOptionsSchema.getEXISchema();
    m_grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
  }
  
  private OptionsGrammarCache() {
  }
  
  public static GrammarCache getGrammarCache() {
    return m_grammarCache;
  }

}
