package org.openexi.proc.common;

import org.openexi.schema.EXISchema;

public interface IGrammarCache {

  public EXISchema getEXISchema();
  
  public IGrammar getElementGrammarUse(int elem);

}
