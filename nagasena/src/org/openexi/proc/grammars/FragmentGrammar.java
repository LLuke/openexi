package org.openexi.proc.grammars;

import org.openexi.proc.common.EventCode;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventTypeList;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.common.IGrammar;

final class FragmentGrammar extends Grammar {
  
  ///////////////////////////////////////////////////////////////////////////
  /// immutables
  ///////////////////////////////////////////////////////////////////////////

  private final int[] m_fragmentElems;
  private final EventType[][] m_eventTypes;
  private final EventCodeTuple[] m_eventCodes;
  private final ArrayEventTypeList[] m_eventTypeLists;
  
  ///////////////////////////////////////////////////////////////////////////
  /// containers with variable contents
  ///////////////////////////////////////////////////////////////////////////

  ///////////////////////////////////////////////////////////////////////////
  /// Constructors, initializers
  ///////////////////////////////////////////////////////////////////////////

  FragmentGrammar(GrammarCache grammarCache) {
    super(SCHEMA_GRAMMAR_FRAGMENT, grammarCache);

    final short grammarOptions  =  grammarCache.grammarOptions;
    
    final int n_fragmentElems = schema != null ? schema.getFragmentElemCount() : 0;
    
    m_eventTypes = new EventType[2][];
    m_eventCodes = new EventCodeTuple[2];
    m_eventTypeLists = new ArrayEventTypeList[2];

    int i;
    for (i = 0; i < 2; i++) {
      m_eventTypeLists[i] = new ArrayEventTypeList();
    }
    
    boolean addTupleL2;
    int n_eventTypes, n_items, n_itemsL2;
    boolean addCM, addPI;
    EventType[] eventTypes;
    ArrayEventCodeTuple tuple, tupleL2;
    EventCode[] eventCodeItems, eventCodeItemsL2;
    
    eventTypes = new EventType[1];
    eventTypes[0] = EventTypeFactory.createStartDocument(m_eventTypeLists[0]);
    tuple = new ArrayEventCodeTuple(); 
    tuple.setItems(new EventType[] { eventTypes[0] });
    m_eventCodes[0] = tuple;
    m_eventTypes[0] = eventTypes;

    addTupleL2 = false;
    tupleL2 = null;
    n_items = n_fragmentElems + 2; // account for SE(*) and ED
    n_itemsL2 = 0;
    if (addCM = GrammarOptions.hasCM(grammarOptions))
      ++n_itemsL2;
    if (addPI = GrammarOptions.hasPI(grammarOptions))
      ++n_itemsL2;
    if (n_itemsL2 != 0) {
      addTupleL2 = true;
      ++n_items;
    }
    m_fragmentElems = schema != null ? schema.getFragmentINodes() : null;
    n_eventTypes = addTupleL2 ? n_itemsL2 + n_items - 1 : n_items;
    eventTypes = new EventType[n_eventTypes];
    eventCodeItems = new EventCode[n_items];
    eventCodeItemsL2 = null;
    int n;
    for (n = 0; n < n_fragmentElems; n++) {
      boolean isSpecific = true;
      int fragmentElem;;
      if (((fragmentElem = m_fragmentElems[n]) & 0x80000000) != 0) {
        isSpecific = false;
        fragmentElem = ~fragmentElem;
      }
      EXIGrammarUse ensuingGrammar = null;
      if (isSpecific) {
        ensuingGrammar = grammarCache.exiGrammarUses[schema.getSerialOfElem(fragmentElem)];
      }
      final int uriId = schema.getUriOfElem(fragmentElem); 
      final int localNameId = schema.getLocalNameOfElem(fragmentElem); 
      final EventType event = EventTypeFactory.createStartElement(
          uriId, localNameId, schema.uris[uriId], schema.localNames[uriId][localNameId],
          m_eventTypeLists[1], ensuingGrammar, (EXIGrammar)null);
      eventCodeItems[n] = eventTypes[n] = event;
    }
    eventCodeItems[n] = eventTypes[n] = new EventType(EventCode.EVENT_CODE_DEPTH_ONE, m_eventTypeLists[1], EventType.ITEM_SE_WC, (IGrammar)null);
    ++n;
    eventCodeItems[n] = eventTypes[n] = EventTypeFactory.createEndDocument(m_eventTypeLists[1]);
    ++n;
    if (addTupleL2) {
      eventCodeItemsL2 = new EventCode[n_itemsL2];
      tupleL2 = new ArrayEventCodeTuple();
      eventCodeItems[n] = tupleL2;
      int m = 0;
      if (addCM) {
        EventType comment = new EventType(EventCode.EVENT_CODE_DEPTH_TWO, m_eventTypeLists[1], EventType.ITEM_CM, (IGrammar)null);
        eventTypes[n++] = comment;
        eventCodeItemsL2[m++] = comment;
      }
      if (addPI) {
        EventType pi = new EventType(EventCode.EVENT_CODE_DEPTH_TWO, m_eventTypeLists[1], EventType.ITEM_PI, (IGrammar)null);
        eventTypes[n++] = pi;
        eventCodeItemsL2[m++] = pi;
      }
    }
    tuple = new ArrayEventCodeTuple();
    if (eventTypes.length > 0) {
      tuple.setItems(eventCodeItems);
      if (addTupleL2)
        tupleL2.setItems(eventCodeItemsL2);
    }
    m_eventCodes[1] = tuple;
    m_eventTypes[1] = eventTypes;
    
    for (i = 0; i < 2; i++) {
      m_eventTypeLists[i].setItems(m_eventTypes[i]);
    }
  }

  @Override
  public final void init(GrammarState stateVariables) {
    stateVariables.targetGrammar = this;
    stateVariables.phase = DOCUMENT_STATE_CREATED;
  }

  @Override
  public final boolean isSchemaInformed() {
    return true;
  }

  @Override
  public final void startDocument(GrammarState stateVariables) {
    assert stateVariables.phase == DOCUMENT_STATE_CREATED;
    stateVariables.phase = DOCUMENT_STATE_COMPLETED;
  }

  @Override
  public final void endDocument(GrammarState stateVariables) {
    if (stateVariables.phase == DOCUMENT_STATE_COMPLETED)
      stateVariables.phase = DOCUMENT_STATE_END;
  }
  
  @Override
  void xsitp(int tp, GrammarState stateVariables) {
    throw new IllegalStateException();
  }
  
  @Override
  final void nillify(int eventTypeIndex, GrammarState stateVariables) {
    throw new IllegalStateException();
  }

  @Override
  public final void chars(EventType eventType, GrammarState stateVariables) {
    throw new IllegalStateException();
  }

  @Override
  final public void undeclaredChars(int eventTypeIndex, GrammarState stateVariables) {
    throw new IllegalStateException();
  }

  @Override
  public void miscContent(int eventTypeIndex, GrammarState stateVariables) {
    assert stateVariables.phase == DOCUMENT_STATE_DEPLETE || stateVariables.phase == DOCUMENT_STATE_COMPLETED;
  }

  /**
   * It is considered to be a well-formedness violation if this method is
   * ever called.
   */
  @Override
  public void end(GrammarState stateVariables) {
    throw new IllegalStateException();
  }
  
  ///////////////////////////////////////////////////////////////////////////
  /// Accessors
  ///////////////////////////////////////////////////////////////////////////

  final GrammarCache getStateCache() {
    return m_grammarCache;
  }
  
  ///////////////////////////////////////////////////////////////////////////
  /// Implementation of abstract methods declared in CommonState
  ///////////////////////////////////////////////////////////////////////////

  @Override
  final EventTypeList getNextEventTypes(GrammarState stateVariables) {
    switch (stateVariables.phase) {
      case DOCUMENT_STATE_CREATED:
        return m_eventTypeLists[0];
      case DOCUMENT_STATE_COMPLETED:
        return m_eventTypeLists[1];
      case DOCUMENT_STATE_END:
        return EventTypeList.EMPTY;
      default:
        assert false;
        break;
    }
    return null;
  }
  
  @Override
  EventCodeTuple getNextEventCodes(GrammarState stateVariables) {
    switch (stateVariables.phase) {
      case DOCUMENT_STATE_CREATED:
        return m_eventCodes[0];
      case DOCUMENT_STATE_COMPLETED:
        return m_eventCodes[1];
      default:
        assert stateVariables.phase == DOCUMENT_STATE_END;
        return null;
    }
  }
  
  @Override
  public void element(EventType eventType, GrammarState stateVariables) { 
    assert stateVariables.phase ==  DOCUMENT_STATE_COMPLETED;
    Grammar grammar;
    grammar = (grammar = ((EventTypeElement)eventType).ensuingGrammar) != null ? grammar : m_grammarCache.elementFragmentGrammar;
    grammar.init(stateVariables.apparatus.pushState());
  }

  @Override
  public Grammar wildcardElement(int eventTypeIndex, int uriId, int localNameId, GrammarState stateVariables) {
    return stateVariables.phase != DOCUMENT_STATE_COMPLETED ? null :
      super.wildcardElement(eventTypeIndex, uriId, localNameId, stateVariables);
  }

}
