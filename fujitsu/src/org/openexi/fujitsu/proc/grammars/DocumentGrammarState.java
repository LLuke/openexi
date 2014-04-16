package org.openexi.fujitsu.proc.grammars;

import org.openexi.fujitsu.proc.common.EventCode;
import org.openexi.fujitsu.proc.common.EventTypeList;

public final class DocumentGrammarState extends GrammarState {

  private static final short INIT_GRAMMARS_DEPTH = 32;
  
  ///////////////////////////////////////////////////////////////////////////
  /// variables
  ///////////////////////////////////////////////////////////////////////////

  public GrammarState currentState;

  private GrammarState[] m_statesStack;
  private int m_n_stackedStates;
  
  /**
   * work space used for duplicating BuiltinElementGrammar and BuiltinFragmentGrammar.
   */
  public final EventTypeNonSchema[] eventTypesWorkSpace;  
  
  final BuiltInGrammarCache builtinGrammarCache;
  
  ///////////////////////////////////////////////////////////////////////////
  /// Constructors, initializers
  ///////////////////////////////////////////////////////////////////////////
  
  public DocumentGrammarState() {
    super((GrammarState)null, null);
    m_statesStack = new GrammarState[INIT_GRAMMARS_DEPTH];
    m_statesStack[0] = this;
    GrammarState prev;
    int i;
    for (i = 1, prev = this; i < INIT_GRAMMARS_DEPTH; i++) {
      prev = m_statesStack[i] = new GrammarState(prev, this);
    }
    m_n_stackedStates = 0;
    eventTypesWorkSpace = new EventTypeNonSchema[EventCode.N_NONSCHEMA_ITEMS];
    builtinGrammarCache = new BuiltInGrammarCache();
  }

  ///////////////////////////////////////////////////////////////////////////
  /// APIs specific to SchemaVM
  ///////////////////////////////////////////////////////////////////////////

  public final void startDocument() {
    currentState.targetGrammar.startDocument(this);
  }
  
  public final EventTypeList getNextEventTypes() {
    return currentState.targetGrammar.getNextEventTypes(currentState);
  }
  
  public final EventCodeTuple getNextEventCodes() {
    return currentState != null ? currentState.targetGrammar.getNextEventCodes(currentState) : null;
  }
  
  public final void startElement(int eventTypeIndex, String uri, String name) {
    currentState.targetGrammar.element(eventTypeIndex, uri, name, currentState);
  }
  
  public final void startUndeclaredElement(String uri, String name) {
    currentState.targetGrammar.undeclaredElement(uri, name, currentState);
  }
  
  public final void xsitp(int tp) {
    currentState.targetGrammar.xsitp(tp, currentState);
  }

  public final void nillify() {
    currentState.targetGrammar.nillify(currentState);
  }

  public final void attribute(int eventTypeIndex, String uri, String name) {
    ((SchemaInformedGrammar)currentState.targetGrammar).schemaAttribute(eventTypeIndex, uri, name, currentState);
  }
  
  public final void undeclaredAttribute(String uri, String name) {
    currentState.targetGrammar.undeclaredAttribute(uri, name, currentState);
  }
  
  public final void characters() {
    currentState.targetGrammar.chars(currentState);
  }

  public final void undeclaredCharacters() {
    currentState.targetGrammar.undeclaredChars(currentState);
  }

  public final void miscContent() {
    currentState.targetGrammar.miscContent(currentState);
  }

  /**
   * Signals the end of an element.
   * @param uri  interned, nullable String representing uri of the element
   * @param name interned String representing name of the element
   */
  public final void endElement(String uri, String name) {
    currentState.targetGrammar.end(uri, name, currentState);
  }

  public final void endDocument() {
    assert currentState.targetGrammar.m_grammarType == Grammar.SCHEMA_GRAMMAR_DOCUMENT || 
    currentState.targetGrammar.m_grammarType == Grammar.SCHEMA_GRAMMAR_FRAGMENT ||
    currentState.targetGrammar.m_grammarType == Grammar.BUILTIN_GRAMMAR_FRAGMENT;
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
      
      GrammarState prev;
      int i;
      for (i = stackLength, prev = _statesStack[stackLength - 1]; i < _stackLength; i++) {
        prev = _statesStack[i] = new GrammarState(prev, this);
      }
      m_statesStack = _statesStack;
    }
    return currentState = m_statesStack[m_n_stackedStates++];
  }
  
  final void popState() {
    currentState = m_n_stackedStates != 0 ? m_statesStack[--m_n_stackedStates - 1] : null;
  }

  final void reset() {
    m_n_stackedStates = 1;
    currentState = m_statesStack[0];
    builtinGrammarCache.clear();
  }

}
