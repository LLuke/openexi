package org.openexi.fujitsu.proc.grammars;

import org.openexi.fujitsu.proc.common.EventCode;
import org.openexi.fujitsu.proc.common.EventTypeList;
import org.openexi.fujitsu.proc.common.GrammarOptions;
import org.openexi.fujitsu.schema.EXISchema;

class FragmentGrammar extends RootGrammar {
  
  ///////////////////////////////////////////////////////////////////////////
  /// immutables
  ///////////////////////////////////////////////////////////////////////////

  private final int[] m_fragmentElems;
  private final AbstractEventType[][] m_eventTypes;
  private final EventCodeTuple[] m_eventCodes;
  private final ArrayEventTypeList[] m_eventTypeLists;
  
  ///////////////////////////////////////////////////////////////////////////
  /// containers with variable contents
  ///////////////////////////////////////////////////////////////////////////

  ///////////////////////////////////////////////////////////////////////////
  /// Constructors, initializers
  ///////////////////////////////////////////////////////////////////////////

  FragmentGrammar(GrammarCache stateCache) {
    super(SCHEMA_GRAMMAR_FRAGMENT, stateCache);

    final short grammarOptions  =  stateCache.grammarOptions;
    
    final int n_fragmentElems = m_schema != null ? m_schema.getFragmentElemCount() : 0;
    
    m_eventTypes = new AbstractEventType[2][];
    m_eventCodes = new EventCodeTuple[2];
    m_eventTypeLists = new ArrayEventTypeList[2];

    int i;
    for (i = 0; i < 2; i++) {
      m_eventTypeLists[i] = new ArrayEventTypeList();
    }
    
    boolean addTupleL2;
    int n_eventTypes, n_items, n_itemsL2;
    boolean addCM, addPI;
    AbstractEventType[] eventTypes;
    ArrayEventCodeTuple tuple, tupleL2;
    EventCode[] eventCodeItems, eventCodeItemsL2;
    
    eventTypes = new AbstractEventType[1];
    eventTypes[0] = new EventTypeStartDocument(this, m_eventTypeLists[0]);
    tuple = ArrayEventCodeTuple.createTuple(); 
    tuple.setItems(new AbstractEventType[] { eventTypes[0] });
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
    m_fragmentElems = m_schema != null ? m_schema.getFragmentINodes() : null;
    n_eventTypes = addTupleL2 ? n_itemsL2 + n_items - 1 : n_items;
    eventTypes = new AbstractEventType[n_eventTypes];
    eventCodeItems = new EventCode[n_items];
    eventCodeItemsL2 = null;
    int n;
    for (n = 0; n < n_fragmentElems; n++) {
      AbstractEventType[] events;
      final int fragmentElem = m_fragmentElems[n];
      events = createEventType(fragmentElem, n, n, this, m_eventTypeLists[1]);
      assert events.length == 1;
      eventCodeItems[n] = eventTypes[n] = events[0];
    }
    eventCodeItems[n] = eventTypes[n] = new EventTypeElementWildcard(
        EventCode.EVENT_CODE_DEPTH_ONE, this, m_eventTypeLists[1]);
    ++n;
    eventCodeItems[n] = eventTypes[n] = new EventTypeEndDocument(this, m_eventTypeLists[1]);
    ++n;
    if (addTupleL2) {
      eventCodeItemsL2 = new EventCode[n_itemsL2];
      tupleL2 = ArrayEventCodeTuple.createTuple();
      eventCodeItems[n] = tupleL2;
      int m = 0;
      if (addCM) {
        EventTypeComment comment = new EventTypeComment(
            EventCode.EVENT_CODE_DEPTH_TWO, this, m_eventTypeLists[1]); 
        eventTypes[n++] = comment;
        eventCodeItemsL2[m++] = comment;
      }
      if (addPI) {
        EventTypeProcessingInstruction pi = new EventTypeProcessingInstruction(
            EventCode.EVENT_CODE_DEPTH_TWO, this, m_eventTypeLists[1]); 
        eventTypes[n++] = pi;
        eventCodeItemsL2[m++] = pi;
      }
    }
    tuple = ArrayEventCodeTuple.createTuple();
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
  public final boolean isSchemaInformed() {
    return true;
  }

  @Override
  final void startDocument(GrammarState stateVariables) {
    assert stateVariables.phase == DOCUMENT_STATE_CREATED;
    stateVariables.phase = DOCUMENT_STATE_COMPLETED;
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Accessors
  ///////////////////////////////////////////////////////////////////////////

  @Override
  final EXISchema getEXISchema() {
    return m_schema;
  }

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
        return AbstractEventTypeList.EMPTY;
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
  void element(int eventTypeIndex, String uri, String name, GrammarState stateVariables) { 
    if (stateVariables.phase ==  DOCUMENT_STATE_COMPLETED) {
      GrammarState kid = null;
      int elemFragment = EXISchema.NIL_NODE;
      
      assert EventCode.ITEM_SCHEMA_SE == m_eventTypes[1][eventTypeIndex].itemType; 
      
      EventTypeSchema elementEventType = (EventTypeSchema)m_eventTypes[1][eventTypeIndex];
      elemFragment = m_fragmentElems[elementEventType.index];
      
      kid = stateVariables.documentGrammarState.pushState();
      m_grammarCache.retrieveElementFragmentGrammar(elemFragment).init(kid);
    }
    else {
      // REVISIT: report an error.
    }
  }

  @Override
  Grammar undeclaredElement(String uri, String name, GrammarState stateVariables) {
    return stateVariables.phase != DOCUMENT_STATE_COMPLETED ? null :
      super.undeclaredElement(uri, name, stateVariables);
  }

}
