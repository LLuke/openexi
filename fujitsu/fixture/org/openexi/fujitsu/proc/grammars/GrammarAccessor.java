package org.openexi.fujitsu.proc.grammars;

import org.openexi.fujitsu.schema.EXISchema;

public class GrammarAccessor {

  public static final EXISchema getEXISchema(Grammar grammar) {
    return grammar.getEXISchema();
  }
  
  public static final int getNode(SchemaInformedGrammar schemaInformedGrammar) {
    return schemaInformedGrammar.getNode();
  }
  
}
