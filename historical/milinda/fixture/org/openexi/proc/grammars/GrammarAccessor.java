package org.openexi.proc.grammars;

import org.openexi.schema.EXISchema;

public class GrammarAccessor {

  public static final EXISchema getEXISchema(Grammar grammar) {
    return grammar.getEXISchema();
  }
  
  public static final int getNode(SchemaInformedGrammar schemaInformedGrammar) {
    return schemaInformedGrammar.getNode();
  }
  
}
