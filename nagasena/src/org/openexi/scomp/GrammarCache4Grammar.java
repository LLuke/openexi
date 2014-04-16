package org.openexi.scomp;

import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.schema.GrammarSchema;

final public class GrammarCache4Grammar {
  
  public static final GrammarCache m_grammarCache;
  static {
    m_grammarCache = new GrammarCache(GrammarSchema.getEXISchema(), GrammarOptions.STRICT_OPTIONS); 
  }
  
  public static final GrammarCache getGrammarCache() {
    return m_grammarCache;
  }
  
  private GrammarCache4Grammar() {
  }

}
