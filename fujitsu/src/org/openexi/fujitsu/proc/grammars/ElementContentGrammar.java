package org.openexi.fujitsu.proc.grammars;

import org.openexi.fujitsu.schema.EXISchema;

abstract class ElementContentGrammar extends ContentGrammar {

  ///////////////////////////////////////////////////////////////////////////
  /// constructors, initializers
  ///////////////////////////////////////////////////////////////////////////

  protected ElementContentGrammar(int tp, GrammarCache cache) {
    super(tp, SCHEMA_GRAMMAR_ELEMENT_CONTENT, cache);
    assert tp != EXISchema.NIL_NODE;
  }

}