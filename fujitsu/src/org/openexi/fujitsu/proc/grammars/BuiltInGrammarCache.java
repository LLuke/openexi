package org.openexi.fujitsu.proc.grammars;

import java.util.HashMap;

final class BuiltInGrammarCache {

  // Local-name to BuiltinElementGrammar[]
  private final HashMap<String,BuiltinElementGrammar[]> m_elementGrammarMap;
  
  public BuiltInGrammarCache() {
    m_elementGrammarMap = new HashMap<String,BuiltinElementGrammar[]>(16);
  }
  
  public void clear() {
    m_elementGrammarMap.clear();
  }

  public BuiltinElementGrammar retrieveElementGrammar(final String uri, final String name, 
      final GrammarCache grammarCache, final EventTypeNonSchema[] eventTypes) {
    final BuiltinElementGrammar[] candidates;
    if ((candidates = m_elementGrammarMap.get(name)) != null) {
      final int len = candidates.length;
      for (int i = 0; i < len; i++) {
        final BuiltinElementGrammar grammar = candidates[i];
        if (uri.equals(grammar.uri))
          return grammar;
      }
      final BuiltinElementGrammar[] _candidates;
      _candidates = new BuiltinElementGrammar[candidates.length + 1];
      System.arraycopy(candidates, 0, _candidates, 0, candidates.length);
      final BuiltinElementGrammar grammar;
      grammar = grammarCache.createBuiltinElementGrammar(uri, eventTypes);
      _candidates[candidates.length] = grammar;
      m_elementGrammarMap.put(name, _candidates);
      return grammar;
    }
    else {
      final BuiltinElementGrammar[] _candidates;
      _candidates = new BuiltinElementGrammar[1];
      final BuiltinElementGrammar grammar;
      grammar = grammarCache.createBuiltinElementGrammar(uri, eventTypes);
      _candidates[0] = grammar;
      m_elementGrammarMap.put(name, _candidates);
      return grammar;
    }
  }
  
}
