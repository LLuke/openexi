package com.sumerogi.proc.grammars;

import com.sumerogi.proc.common.IGrammar;
import com.sumerogi.proc.common.StringTable;

public abstract class BuiltinGrammar extends Grammar implements IGrammar {

  StringTable stringTable;

  protected final ReversedEventTypeList m_eventTypeList;
  protected final ReverseEventCodeTuple m_eventCodes;

  protected boolean dirty;

  protected BuiltinGrammar(GrammarCache grammarCache) {
    super(grammarCache);
    
    m_eventTypeList = new ReversedEventTypeList();
    m_eventCodes = new ReverseEventCodeTuple();
    
    dirty = false;
    
    populateContentGrammar();
    
    m_eventTypeList.checkPoint();
    m_eventCodes.checkPoint();
  }
  
  @Override
  public final void init(GrammarState stateVariables) {
    stateVariables.targetGrammar = this;
  }

  protected abstract void populateContentGrammar();

  ///////////////////////////////////////////////////////////////////////////
  /// Implementation of IGrammar (used by StringTable)
  ///////////////////////////////////////////////////////////////////////////
  
  public final void reset() {
    if (dirty) {
      m_eventTypeList.reset();
      m_eventCodes.reset();
      dirty = false;
    }
  }

}
