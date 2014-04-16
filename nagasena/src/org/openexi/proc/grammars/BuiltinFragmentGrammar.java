package org.openexi.proc.grammars;

import org.openexi.proc.common.EventCode;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventTypeList;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.common.IGrammar;
import org.openexi.proc.common.StringTable;

final class BuiltinFragmentGrammar extends BuiltinGrammar {
  
  private final EventTypeList[] m_eventTypeLists;
  private final EventCodeTuple[] m_eventCodes;
  
  private static final EventType[] m_eventTypesInit;
  static {
    m_eventTypesInit = new EventType[N_NONSCHEMA_ITEMS];
    for (int i = 0; i < N_NONSCHEMA_ITEMS; i++) {
      m_eventTypesInit[i] = null;
    }
  }

  /**
   * For exclusive use by GrammarCache only.
   * GrammarCache calls this method to instantiate a template grammar.
   */
  BuiltinFragmentGrammar(final GrammarCache grammarCache) {
    super(BUILTIN_GRAMMAR_FRAGMENT, grammarCache);
    
    final short grammarOptions = grammarCache.grammarOptions;
    
    m_eventTypeLists = new EventTypeList[2];
    m_eventCodes = new EventCodeTuple[2];

    final ArrayEventTypeList eventTypeList;
    final ArrayEventCodeTuple tuple;
    
    m_eventTypeLists[0] = eventTypeList = new ArrayEventTypeList();
    m_eventCodes[0] = tuple = new ArrayEventCodeTuple();
    
    EventType[] eventTypes = new EventType[] { EventTypeFactory.createStartDocument(eventTypeList) };
    eventTypeList.setItems(eventTypes);
    tuple.setItems(new EventType[] { eventTypes[0] });

    populateContentGrammar(grammarOptions);
  }
  
  @Override
  public final void init(GrammarState stateVariables) {
    stateVariables.targetGrammar = this;
    stateVariables.phase = DOCUMENT_STATE_CREATED;
  }
  
  /**
   * For exclusive use by GrammarCache only.
   * GrammarCache calls this method to instantiate a new BuiltinElementGrammar
   * from a template grammar.
   */
  BuiltinFragmentGrammar duplicate(final EventType[] eventTypes) {
    return new BuiltinFragmentGrammar(m_grammarCache, m_eventTypeLists, m_eventCodes, eventTypes);
  }

  /**
   * Used only by duplicate() method above.
   */
  private BuiltinFragmentGrammar(GrammarCache grammarCache, EventTypeList[] sourceEventTypeList, 
      EventCodeTuple[] sourceEventCodes, EventType[] eventTypes) {
    super(BUILTIN_GRAMMAR_FRAGMENT, grammarCache);

    m_eventTypeLists = new EventTypeList[2];
    m_eventCodes = new EventCodeTuple[2];

    m_eventTypeLists[0] = sourceEventTypeList[0];
    m_eventCodes[0] = sourceEventCodes[0];
    
    final ReversedEventTypeList reversedEventTypeList = new ReversedEventTypeList();
    final ReverseEventCodeTuple reverseEventCodeTuple = new ReverseEventCodeTuple();
    
    m_eventTypeLists[1] = reversedEventTypeList;
    m_eventCodes[1] = reverseEventCodeTuple;

    cloneContentGrammar(this, (ReverseEventCodeTuple)sourceEventCodes[1], 
        reversedEventTypeList, reverseEventCodeTuple, eventTypes);
  }
  
  private void populateContentGrammar(final short grammarOptions) {

    final ReversedEventTypeList eventList  = new ReversedEventTypeList();
    final ReverseEventCodeTuple eventCodes = new ReverseEventCodeTuple();

    m_eventTypeLists[1] = eventList;
    m_eventCodes[1] = eventCodes;

    /*
     * FragmentContent :
     *   SE (*) FragmentContent 0
     *   ED                     1
     *   CM FragmentContent     2.0 (if addCM)
     *   PI FragmentContent     2.1 (if addPI)
     */

    final boolean addCM  = GrammarOptions.hasCM(grammarOptions);
    final boolean addPI  = GrammarOptions.hasPI(grammarOptions);

    final EventType elementWildcard;
    final EventType endDocument;
    final EventType comment;
    final EventType processingInstruction;
    
    boolean addTupleL2 = false;
    int n_itemsL2 = 0;
    
    if (addPI) {
      processingInstruction = new EventType(EventCode.EVENT_CODE_DEPTH_TWO, eventList, EventType.ITEM_PI, (IGrammar)null);
      eventList.add(processingInstruction);
      ++n_itemsL2;
      addTupleL2 = true;
    }
    else {
      processingInstruction = null;
    }
    if (addCM) {
      comment = new EventType(EventCode.EVENT_CODE_DEPTH_TWO, eventList, EventType.ITEM_CM, (IGrammar)null);
      eventList.add(comment);
      ++n_itemsL2;
      addTupleL2 = true;
    }
    else {
      comment = null;
    }
    endDocument = EventTypeFactory.createEndDocument(eventList); 
    eventList.add(endDocument);
    elementWildcard = new EventType(EventCode.EVENT_CODE_DEPTH_ONE, eventList, EventType.ITEM_SE_WC, (IGrammar)null);
    eventList.add(elementWildcard);
    
    EventCode[] eventCodeItemsL2 = null;
    ArrayEventCodeTuple tupleL2 = null;
    if (addTupleL2) {
      eventCodeItemsL2 = new EventCode[n_itemsL2];
      tupleL2 = new ArrayEventCodeTuple();
    }
    eventCodes.setInitialItems(elementWildcard, endDocument, tupleL2);
    if (addTupleL2) {
      int m = 0;
      if (addCM)
        eventCodeItemsL2[m++] = comment;
      if (addPI)
        eventCodeItemsL2[m++] = processingInstruction;
      tupleL2.setItems(eventCodeItemsL2);
    }
  }
  
  private void cloneContentGrammar(
      Grammar ownerGrammar, ReverseEventCodeTuple sourceEventCodes, 
      ReversedEventTypeList eventList, ReverseEventCodeTuple eventCodes, 
      EventType[] eventTypes) {
    
    System.arraycopy(m_eventTypesInit, 0, eventTypes, 0, N_NONSCHEMA_ITEMS);

    assert sourceEventCodes.itemsCount == 2 || sourceEventCodes.itemsCount == 3 && 
      sourceEventCodes.getItem(2).itemType == EventType.ITEM_TUPLE;
    
    /*
     * FragmentContent :
     *   SE (*) FragmentContent 0
     *   ED                     1
     *   CM FragmentContent     2.0 (if addCM)
     *   PI FragmentContent     2.1 (if addPI)
     */

    final boolean addTupleL2 = sourceEventCodes.itemsCount == 3;

    final EventCodeTuple sourceTupleL2;
    final int n_itemsL2;
    final EventCode[] eventCodeItemsL2;
    final ArrayEventCodeTuple tupleL2;
    
    if (addTupleL2) {
      sourceTupleL2 = (EventCodeTuple)sourceEventCodes.getItem(2);
      n_itemsL2 = sourceTupleL2.itemsCount;
      eventCodeItemsL2 = new EventCode[n_itemsL2];
      tupleL2 = new ArrayEventCodeTuple();
    }
    else {
      sourceTupleL2 = null;
      n_itemsL2 = 0;
      eventCodeItemsL2 = null;
      tupleL2 = null;
    }

    final EventType elementWildcard;
    final EventType endDocument;
    endDocument = EventTypeFactory.createEndDocument(eventList);
    eventTypes[EventType.ITEM_ED] = endDocument;
    elementWildcard = new EventType(EventCode.EVENT_CODE_DEPTH_ONE, eventList, EventType.ITEM_SE_WC, (IGrammar)null);
    eventTypes[EventType.ITEM_SE_WC] = elementWildcard;
    eventCodes.setInitialItems(elementWildcard, endDocument, tupleL2);

    int i;
    if (addTupleL2) {
      for (i = 0; i < n_itemsL2; i++) {
        final EventCode ithSourceItem = sourceTupleL2.getItem(i);
        final EventType eventType = duplicate(((EventType)ithSourceItem), eventList);
        eventCodeItemsL2[i] = eventType;
        eventTypes[eventType.itemType] = eventType;
      }
      tupleL2.setItems(eventCodeItemsL2);
    }
    
    for (i = 0; i < N_NONSCHEMA_ITEMS; i++) {
      final EventType ith = eventTypes[i];
      if (ith != null) {
        eventList.add(ith);
      }
    }
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Method implementations for event processing
  ///////////////////////////////////////////////////////////////////////////

  @Override
  public EventCodeTuple getNextEventCodes(GrammarState stateVariables) {
    final byte phase;
    return (phase = stateVariables.phase) == DOCUMENT_STATE_COMPLETED ? m_eventCodes[1] :
      phase == DOCUMENT_STATE_CREATED ? m_eventCodes[0] : null;
  }

  @Override
  public EventTypeList getNextEventTypes(GrammarState stateVariables) {
    final byte phase;
    return (phase = stateVariables.phase) == DOCUMENT_STATE_COMPLETED ? m_eventTypeLists[1] :
      phase == DOCUMENT_STATE_CREATED ? m_eventTypeLists[0] : EventTypeList.EMPTY;
  }

  @Override
  public final void startDocument(GrammarState stateVariables) {
    assert stateVariables.phase == DOCUMENT_STATE_CREATED;
    stateVariables.phase = DOCUMENT_STATE_COMPLETED;
  }

  @Override
  public final void endDocument(GrammarState stateVariables) {
    assert stateVariables.phase == DOCUMENT_STATE_COMPLETED;
    stateVariables.phase = DOCUMENT_STATE_END;
  }

  @Override
  public void element(EventType eventType, GrammarState stateVariables) {
    assert stateVariables.phase == DOCUMENT_STATE_COMPLETED && eventType.itemType == EventType.ITEM_SE;
    final Grammar ensuingGrammar = ((EventTypeElement)eventType).ensuingGrammar;
    ensuingGrammar.init(stateVariables.apparatus.pushState());
  }

  @Override
  public void undeclaredChars(int eventTypeIndex, GrammarState stateVariables) {
    assert false;
  }

  @Override
  public final void end(GrammarState stateVariables) {
    assert false;
  }
  
  /**
   * Signals CM, PI or ER event. 
   */
  @Override
  public void miscContent(int eventTypeIndex, GrammarState stateVariables) {
    assert stateVariables.phase == DOCUMENT_STATE_COMPLETED;
  }

  @Override
  void xsitp(int tp, GrammarState stateVariables) {
    assert false;
  }

  @Override
  public Grammar wildcardElement(int eventTypeIndex, int uriId, int localNameId, final GrammarState stateVariables) {
    assert stateVariables.phase == DOCUMENT_STATE_COMPLETED;
    final Grammar ensuingGrammar = super.wildcardElement(eventTypeIndex, uriId, localNameId, stateVariables);
    final StringTable uriPartition = stateVariables.apparatus.stringTable;
    final String uri = uriPartition.getURI(uriId);
    final String name = uriPartition.getLocalNamePartition(uriId).localNameEntries[localNameId].localName;
    final EventTypeElement eventTypeElement = new EventTypeElement(uriId, uri, localNameId, name, m_eventTypeLists[1], ensuingGrammar, (IGrammar)null); 
    ((ReversedEventTypeList)m_eventTypeLists[1]).add(eventTypeElement);
    ((ReverseEventCodeTuple)m_eventCodes[1]).addItem(eventTypeElement);
    return ensuingGrammar;
  }

}
