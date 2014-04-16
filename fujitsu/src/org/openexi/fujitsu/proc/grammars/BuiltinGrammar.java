package org.openexi.fujitsu.proc.grammars;

public abstract class BuiltinGrammar extends Grammar {

  public static final byte ELEMENT_STATE_IN_TAG     = 0;
  public static final byte ELEMENT_STATE_IN_CONTENT = 1;
  static final byte ELEMENT_STATE_DELEGATED  = 2;
  
  protected BuiltinGrammar(byte grammarType, GrammarCache grammarCache) {
    super(grammarType, grammarCache);
  }

  @Override
  public final boolean isSchemaInformed() {
    return false;
  }

  @Override
  final void nillify(GrammarState stateVariables) {
    assert false;
  }

}
