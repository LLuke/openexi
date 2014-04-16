package org.openexi.proc.grammars;

import org.openexi.proc.common.GrammarOptions;
import org.openexi.schema.EXISchema;
import org.openexi.schema.HeaderOptionsSchema;

public final class OptionsGrammarCache {
  
  private static final GrammarCache m_grammarCache;
  static {
    EXISchema schema = HeaderOptionsSchema.getEXISchema();
    m_grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS); 
    
    int pos, nodesLen;
    for (pos = EXISchema.THE_SCHEMA, nodesLen = schema.getNodes().length; pos < nodesLen;) {
      final int node = pos;
      pos += EXISchema._getNodeSize(node, schema.getNodes());
      int nodeType = schema.getNodeType(node);
      if (nodeType == EXISchema.SIMPLE_TYPE_NODE || nodeType == EXISchema.COMPLEX_TYPE_NODE) {
        m_grammarCache.retrieveElementTagGrammar(node);
        m_grammarCache.retrieveElementContentGrammar(node);
      }
      else if (nodeType == EXISchema.ELEMENT_NODE) {
        m_grammarCache.retrieveElementGrammar(node);
      }
    }
  }
  
  private OptionsGrammarCache() {
  }
  
  public static GrammarCache getGrammarCache() {
    return m_grammarCache;
  }

}
