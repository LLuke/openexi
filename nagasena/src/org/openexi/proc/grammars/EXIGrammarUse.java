package org.openexi.proc.grammars;

import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventTypeList;
import org.openexi.proc.common.IGrammar;

final class EXIGrammarUse extends Grammar implements IGrammar {
  
  EXIGrammar exiGrammar;
  final int contentDatatype;

  EXIGrammarUse(int contentDatatype, GrammarCache grammarCache) {
    super(SCHEMA_GRAMMAR_ELEMENT_AND_TYPE_USE, grammarCache);
    this.contentDatatype = contentDatatype;
  }

  @Override
  public void init(GrammarState stateVariables) {
    exiGrammar.init(stateVariables);
    stateVariables.contentDatatype = contentDatatype;
  }

  @Override
  public boolean isSchemaInformed() {
    assert false;
    return true;
  }
  
  ///////////////////////////////////////////////////////////////////////////
  /// Implementation of IGrammar (used by StringTable)
  ///////////////////////////////////////////////////////////////////////////

  public void reset() {
    assert false;
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Implementation of abstract methods
  ///////////////////////////////////////////////////////////////////////////

  @Override
  void attribute(EventType eventType, GrammarState stateVariables) {
    assert false;
  }
  
  @Override
  EventTypeList getNextEventTypes(GrammarState stateVariables) {
    assert false;
    return null;
  }

  @Override
  EventCodeTuple getNextEventCodes(GrammarState stateVariables) {
    assert false;
    return null;
  }

  @Override
  public void element(EventType eventType, GrammarState stateVariables) {
    assert false;
  }

  @Override
  Grammar wildcardElement(int eventTypeIndex, int uriId, int localNameId, GrammarState stateVariables) {
    assert false;
    return null;
  }

  @Override
  void xsitp(int tp, GrammarState stateVariables) {
    assert false;
  }

  @Override
  void nillify(int eventTypeIndex, GrammarState stateVariables) {
    assert false;
  }
  
  @Override
  public void chars(EventType eventType, GrammarState stateVariables) {
    assert false;
  }

  @Override
  public void undeclaredChars(int eventTypeIndex, GrammarState stateVariables) {
    assert false;
  }

  @Override
  public void miscContent(int eventTypeIndex, GrammarState stateVariables) {
    assert false;
  }

  @Override
  public void end(GrammarState stateVariables) {
    assert false;
  }
  
}
