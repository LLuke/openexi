package com.sumerogi.proc.grammars;

import com.sumerogi.proc.common.AlignmentType;
import com.sumerogi.proc.common.EventType;
import com.sumerogi.proc.common.EventTypeList;
import com.sumerogi.proc.common.StringTable;

public abstract class Apparatus {

  private static final short INIT_GRAMMARS_DEPTH = 32;

  ///////////////////////////////////////////////////////////////////////////
  /// variables
  ///////////////////////////////////////////////////////////////////////////

  public GrammarState currentState;

  protected GrammarState[] m_statesStack;
  protected int m_n_stackedStates;
  
  public StringTable stringTable;
  
  public Apparatus() {
    m_statesStack = new GrammarState[INIT_GRAMMARS_DEPTH];
    currentState = m_statesStack[0] = new GrammarState(this);
    for (int i = 1; i < INIT_GRAMMARS_DEPTH; i++) {
      m_statesStack[i] = new GrammarState(this);
    }
    m_n_stackedStates = 1;
  }

  public void reset() {
    m_n_stackedStates = 1;
    currentState = m_statesStack[0];
    stringTable.reset();
  }
  
  public abstract AlignmentType getAlignmentType();

  public void setStringTable(StringTable stringTable) {
    this.stringTable = stringTable;
  }
  
  ///////////////////////////////////////////////////////////////////////////
  /// APIs specific to SchemaVM
  ///////////////////////////////////////////////////////////////////////////

  public final void startDocument() {
    currentState.targetGrammar.startDocument(currentState);
  }
  
  public final EventTypeList getNextEventTypes() {
    return currentState.targetGrammar.getNextEventTypes(currentState);
  }
  
  public final EventCodeTuple getNextEventCodes() {
    return currentState.targetGrammar.getNextEventCodes(currentState);
  }
  
  public final void startObjectNamed(EventType eventType) {
    currentState.targetGrammar.startObjectNamed(eventType, currentState);
  }
  
  public final void startObjectAnonymous(EventType eventType) {
    currentState.targetGrammar.startObjectAnonymous(eventType, currentState);
  }
  
  public final void startObjectWildcard(int name) {
    currentState.targetGrammar.startObjectWildcard(name, currentState);
  }

  public final void startArrayNamed(EventType eventType) {
    currentState.targetGrammar.startArrayNamed(eventType, currentState);
  }
  
  public final void startArrayAnonymous() {
    currentState.targetGrammar.startArrayAnonymous(currentState);
  }

  public final void startArrayWildcard(int name) {
    currentState.targetGrammar.startArrayWildcard(name, currentState);
  }

  public void anonymousStringValue(EventType eventType) {
    currentState.targetGrammar.anonymousStringValue(eventType, currentState);
  }
  
  public void wildcardStringValue(int eventTypeIndex, int nameId) {
    currentState.targetGrammar.wildcardStringValue(eventTypeIndex, nameId);
  }

  public void anonymousNumberValue(EventType eventType) {
    currentState.targetGrammar.anonymousNumberValue(eventType, currentState);
  }
  
  public void wildcardNumberValue(int eventTypeIndex, int nameId) {
    currentState.targetGrammar.wildcardNumberValue(eventTypeIndex, nameId);
  }

  public void anonymousNullValue(EventType eventType) {
    currentState.targetGrammar.anonymousNullValue(eventType, currentState);
  }

  public void wildcardNullValue(int eventTypeIndex, int nameId) {
    currentState.targetGrammar.wildcardNullValue(eventTypeIndex, nameId);
  }

  public void wildcardNullValue(int eventTypeIndex, int nameId, GrammarState stateVariables) {
    currentState.targetGrammar.wildcardNullValue(eventTypeIndex, nameId);
  }

  public void anonymousBooleanValue(EventType eventType) {
    currentState.targetGrammar.anonymousBooleanValue(eventType, currentState);
  }

  public void wildcardBooleanValue(int eventTypeIndex, int nameId) {
    currentState.targetGrammar.wildcardBooleanValue(eventTypeIndex, nameId);
  }
  
  /**
   * Signals the end of an object.
   */
  public final void endObject() {
    currentState.targetGrammar.endObject(currentState);
    currentState = m_statesStack[--m_n_stackedStates - 1];
  }

  /**
   * Signals the end of an array.
   */
  public final void endArray() {
    currentState.targetGrammar.endArray(currentState);
    currentState = m_statesStack[--m_n_stackedStates - 1];
  }

  public final void endDocument() {
    assert currentState.targetGrammar instanceof DocumentGrammar;
    currentState.targetGrammar.endDocument(currentState);
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Utilities
  ///////////////////////////////////////////////////////////////////////////

  final GrammarState pushState() {
    final int stackLength; 
    if ((stackLength = m_statesStack.length) == m_n_stackedStates) {
      final GrammarState[] _statesStack;
      final int _stackLength = 2 * stackLength;
      _statesStack = new GrammarState[_stackLength];
      System.arraycopy(m_statesStack, 0, _statesStack, 0, stackLength);
      for (int i = stackLength; i < _stackLength; i++) {
        _statesStack[i] = new GrammarState(this);
      }
      m_statesStack = _statesStack;
    }
    return currentState = m_statesStack[m_n_stackedStates++];
  }
  
}
