package org.openexi.fujitsu.proc.grammars;

abstract class RootGrammar extends Grammar {

  RootGrammar(byte grammarType, GrammarCache stateCache) {
    super(grammarType, stateCache);
  }
  
  @Override
  public final void init(GrammarState stateVariables) {
    super.init(stateVariables);
    stateVariables.phase = DOCUMENT_STATE_CREATED;
    
    final DocumentGrammarState documentGrammarState;
    documentGrammarState = (DocumentGrammarState)stateVariables;
    documentGrammarState.reset();
  }
  
  @Override
  final void endDocument(GrammarState stateVariables) {
    if (stateVariables.phase == DOCUMENT_STATE_COMPLETED)
      stateVariables.phase = DOCUMENT_STATE_END;
  }
  
  @Override
  void xsitp(int tp, GrammarState stateVariables) {
    // REVISIT: does it ever get called?
    stateVariables.targetGrammar.xsitp(tp, stateVariables);
  }
  
  @Override
  void nillify(GrammarState stateVariables) {
    // REVISIT: does it ever get called?
    stateVariables.targetGrammar.nillify(stateVariables);
  }

  @Override
  public void chars(GrammarState stateVariables) {
    assert stateVariables.phase == DOCUMENT_STATE_DEPLETE || stateVariables.phase == DOCUMENT_STATE_COMPLETED;
  }

  @Override
  final public void undeclaredChars(GrammarState stateVariables) {
    assert stateVariables.phase == DOCUMENT_STATE_DEPLETE || stateVariables.phase == DOCUMENT_STATE_COMPLETED;
  }

  @Override
  public void miscContent(GrammarState stateVariables) {
    assert stateVariables.phase == DOCUMENT_STATE_DEPLETE || stateVariables.phase == DOCUMENT_STATE_COMPLETED;
  }

  @Override
  void done(GrammarState kid, GrammarState stateVariables) {
    stateVariables.phase = DOCUMENT_STATE_COMPLETED;
  }

  /**
   * It is considered to be a well-formedness violation if this method is
   * ever called.
   */
  @Override
  void end(String uri, String name, GrammarState stateVariables) {
    assert false;
  }

}
