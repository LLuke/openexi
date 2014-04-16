package org.openexi.fujitsu.proc.grammars;

import org.openexi.fujitsu.proc.common.EventCode;
import org.openexi.fujitsu.proc.common.EventType;
import org.openexi.fujitsu.proc.common.EventTypeList;
import org.openexi.fujitsu.proc.common.GrammarOptions;

final class BuiltinFragmentGrammar extends BuiltinGrammar {
  
  private final AbstractEventTypeList[] m_eventTypeLists;
  private final EventCodeTuple[] m_eventCodes;
  
  private static final EventTypeNonSchema[] m_eventTypesInit;
  static {
    m_eventTypesInit = new EventTypeNonSchema[EventCode.N_NONSCHEMA_ITEMS];
    for (int i = 0; i < EventCode.N_NONSCHEMA_ITEMS; i++) {
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
    
    m_eventTypeLists = new AbstractEventTypeList[2];
    m_eventCodes = new EventCodeTuple[2];

    final ArrayEventTypeList eventTypeList;
    final ArrayEventCodeTuple tuple;
    
    m_eventTypeLists[0] = eventTypeList = new ArrayEventTypeList();
    m_eventCodes[0] = tuple = ArrayEventCodeTuple.createTuple();
    
    AbstractEventType[] eventTypes = new AbstractEventType[] { new EventTypeStartDocument(this, eventTypeList) };
    eventTypeList.setItems(eventTypes);
    tuple.setItems(new AbstractEventType[] { eventTypes[0] });

    populateContentGrammar(grammarOptions);
  }
  
  @Override
  public final void init(GrammarState stateVariables) {
    super.init(stateVariables);
    stateVariables.phase = DOCUMENT_STATE_CREATED;
    
    final DocumentGrammarState documentGrammarState;
    documentGrammarState = (DocumentGrammarState)stateVariables;
    documentGrammarState.reset();
  }
  
  /**
   * For exclusive use by GrammarCache only.
   * GrammarCache calls this method to instantiate a new BuiltinElementGrammar
   * from a template grammar.
   */
  BuiltinFragmentGrammar duplicate(final EventTypeNonSchema[] eventTypes) {
    return new BuiltinFragmentGrammar(m_grammarCache, m_eventTypeLists, m_eventCodes, eventTypes);
  }

  /**
   * Used only by duplicate() method above.
   */
  private BuiltinFragmentGrammar(GrammarCache grammarCache, AbstractEventTypeList[] sourceEventTypeList, 
      EventCodeTuple[] sourceEventCodes, EventTypeNonSchema[] eventTypes) {
    super(BUILTIN_GRAMMAR_FRAGMENT, grammarCache);

    m_eventTypeLists = new AbstractEventTypeList[2];
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

    final EventTypeNonSchema elementWildcard;
    final EventTypeNonSchema endDocument;
    final EventTypeNonSchema comment;
    final EventTypeNonSchema processingInstruction;
    
    boolean addTupleL2 = false;
    int n_itemsL2 = 0;
    
    if (addPI) {
      processingInstruction = new EventTypeProcessingInstruction(EventCode.EVENT_CODE_DEPTH_TWO, this, eventList);
      eventList.add(processingInstruction);
      ++n_itemsL2;
      addTupleL2 = true;
    }
    else {
      processingInstruction = null;
    }
    if (addCM) {
      comment = new EventTypeComment(EventCode.EVENT_CODE_DEPTH_TWO, this, eventList);
      eventList.add(comment);
      ++n_itemsL2;
      addTupleL2 = true;
    }
    else {
      comment = null;
    }
    endDocument = new EventTypeEndDocumentReversed(this, eventList); 
    eventList.add(endDocument);
    elementWildcard = new EventTypeElementWildcardReversed(this, eventList); 
    eventList.add(elementWildcard);
    
    EventCode[] eventCodeItemsL2 = null;
    ArrayEventCodeTuple tupleL2 = null;
    if (addTupleL2) {
      eventCodeItemsL2 = new EventCode[n_itemsL2];
      tupleL2 = new ArrayEventCodeTupleReversed();
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
      EventTypeNonSchema[] eventTypes) {
    
    System.arraycopy(m_eventTypesInit, 0, eventTypes, 0, EventCode.N_NONSCHEMA_ITEMS);

    assert sourceEventCodes.itemsCount == 2 || sourceEventCodes.itemsCount == 3 && 
      sourceEventCodes.getItem(2).itemType == EventCode.ITEM_TUPLE;
    
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
      tupleL2 = new ArrayEventCodeTupleReversed();
    }
    else {
      sourceTupleL2 = null;
      n_itemsL2 = 0;
      eventCodeItemsL2 = null;
      tupleL2 = null;
    }

    final EventTypeNonSchema elementWildcard;
    final EventTypeNonSchema endDocument;
    endDocument = new EventTypeEndDocumentReversed(this, eventList);
    eventTypes[EventCode.ITEM_ED] = endDocument;
    elementWildcard = new EventTypeElementWildcardReversed(this, eventList);
    eventTypes[EventCode.ITEM_SE_WC] = elementWildcard;
    eventCodes.setInitialItems(elementWildcard, endDocument, tupleL2);

    int i;
    if (addTupleL2) {
      for (i = 0; i < n_itemsL2; i++) {
        final EventCode ithSourceItem = sourceTupleL2.getItem(i);
        final EventTypeNonSchema eventType = ((EventTypeNonSchema)ithSourceItem).duplicate(ownerGrammar, eventList);
        eventCodeItemsL2[i] = eventType;
        eventTypes[eventType.itemType] = eventType;
      }
      tupleL2.setItems(eventCodeItemsL2);
    }
    
    for (i = 0; i < EventCode.N_NONSCHEMA_ITEMS; i++) {
      final EventTypeNonSchema ith = eventTypes[i];
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
      phase == DOCUMENT_STATE_CREATED ? m_eventTypeLists[0] : AbstractEventTypeList.EMPTY;
  }

  @Override
  final void startDocument(GrammarState stateVariables) {
    assert stateVariables.phase == DOCUMENT_STATE_CREATED;
    stateVariables.phase = DOCUMENT_STATE_COMPLETED;
  }

  @Override
  final void endDocument(GrammarState stateVariables) {
    assert stateVariables.phase == DOCUMENT_STATE_COMPLETED;
    stateVariables.phase = DOCUMENT_STATE_END;
  }

  @Override
  void element(int eventTypeIndex, String uri, String name, GrammarState stateVariables) {
    assert stateVariables.phase == DOCUMENT_STATE_COMPLETED;
    final EventType eventType = m_eventTypeLists[1].item(eventTypeIndex);
    assert eventType.itemType == EventCode.ITEM_SE;
    final Grammar ensuingGrammar = ((EventTypeElement)eventType).getEnsuingGrammar();
    ensuingGrammar.init(stateVariables.documentGrammarState.pushState());
  }

  @Override
  public void chars(GrammarState stateVariables) {
    assert false;
  }

  @Override
  public void undeclaredChars(GrammarState stateVariables) {
    assert false;
  }

  @Override
  final void end(String uri, String name, GrammarState stateVariables) {
    assert false;
  }
  
  @Override
  final void done(GrammarState kid, GrammarState stateVariables) {
    assert stateVariables.phase == DOCUMENT_STATE_COMPLETED;
  }

  /**
   * Signals CM, PI or ER event. 
   */
  @Override
  public void miscContent(GrammarState stateVariables) {
    assert stateVariables.phase == DOCUMENT_STATE_COMPLETED;
  }

  @Override
  void xsitp(int tp, GrammarState stateVariables) {
    assert false;
  }

  @Override
  Grammar undeclaredElement(final String uri, final String name, final GrammarState stateVariables) {
    assert stateVariables.phase == DOCUMENT_STATE_COMPLETED;
    final Grammar ensuingGrammar = super.undeclaredElement(uri, name, stateVariables);
    final EventTypeElement eventTypeElement = new EventTypeElementReversed(uri, name, this, m_eventTypeLists[1], ensuingGrammar); 
    ((ReversedEventTypeList)m_eventTypeLists[1]).add(eventTypeElement);
    ((ReverseEventCodeTuple)m_eventCodes[1]).addItem(eventTypeElement);
    return ensuingGrammar;
  }

  @Override
  void undeclaredAttribute(String uri, String name, GrammarState stateVariables) {
    assert false;
  }

}
