package org.openexi.proc.grammars;

import org.openexi.proc.common.EventCode;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventTypeList;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.schema.EXISchema;

final class BuiltinElementGrammar extends BuiltinGrammar {

  private final ReversedEventTypeList m_eventTypeListTag;
  private final ReversedEventTypeList m_eventTypeListContent;
  
  private final ReverseEventCodeTuple m_eventCodesTag;
  private final ReverseEventCodeTuple m_eventCodesContent;
  
  final String uri;
  
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
  BuiltinElementGrammar(final String uri, final GrammarCache grammarCache) {
    super(BUILTIN_GRAMMAR_ELEMENT, grammarCache);
    
    final short grammarOptions = grammarCache.grammarOptions;
    
    m_eventTypeListTag = new ReversedEventTypeList();
    m_eventCodesTag = new ReverseEventCodeTuple();
    populateTagGrammar(grammarOptions);

    m_eventTypeListContent = new ReversedEventTypeList();
    m_eventCodesContent = new ReverseEventCodeTuple();
    populateContentGrammar(grammarOptions);
    
    this.uri = uri;
  }
  
  /**
   * For exclusive use by GrammarCache only.
   * GrammarCache calls this method to instantiate a new BuiltinElementGrammar
   * from a template grammar.
   */
  BuiltinElementGrammar duplicate(final String uri, final EventTypeNonSchema[] eventTypes) {
    return new BuiltinElementGrammar(uri, m_grammarCache, 
        m_eventCodesTag, m_eventCodesContent, eventTypes);
  }

  /**
   * Used only by duplicate() method above.
   */
  private BuiltinElementGrammar(final String uri, GrammarCache grammarCache, 
      ReverseEventCodeTuple sourceEventCodesTag, 
      ReverseEventCodeTuple sourceEventCodesContent, EventTypeNonSchema[] eventTypes) {
    super(BUILTIN_GRAMMAR_ELEMENT, grammarCache);
    
    m_eventTypeListTag = new ReversedEventTypeList();
    m_eventCodesTag = new ReverseEventCodeTuple();
    
    cloneTagGrammar(this, sourceEventCodesTag, m_eventTypeListTag, m_eventCodesTag, eventTypes);

    m_eventTypeListContent = new ReversedEventTypeList();
    m_eventCodesContent = new ReverseEventCodeTuple();

    cloneContentGrammar(this, sourceEventCodesContent, m_eventTypeListContent, m_eventCodesContent, eventTypes);
    
    this.uri = uri;
  }
  
  @Override
  public void init(GrammarState stateVariables) {
    super.init(stateVariables);
    stateVariables.phase = ELEMENT_STATE_IN_TAG;
  }

  private void populateTagGrammar(final short grammarOptions) {

    final ReversedEventTypeList eventList  = m_eventTypeListTag;
    final ReverseEventCodeTuple eventCodes = m_eventCodesTag;

    /*
     * StartTagContent : 
     *   EE                     0.0 
     *   AT (*) StartTagContent 0.1 
     *   NS StartTagContent     0.2 (if addNS) 
     *   SC Fragment            0.3 (if addSC)
     *   SE (*) ElementContent  0.4
     *   CH ElementContent      0.5 
     *   ER ElementContent      0.6 (if addER)
     *   CM ElementContent      0.7.0 (if addCM)
     *   PI ElementContent      0.7.1 (if addPI)
     */

    final boolean addDTD = GrammarOptions.hasDTD(grammarOptions);
    final boolean addCM  = GrammarOptions.hasCM(grammarOptions);
    final boolean addPI  = GrammarOptions.hasPI(grammarOptions);
    final boolean addNS  = GrammarOptions.hasNS(grammarOptions);
    final boolean addSC  = GrammarOptions.hasSC(grammarOptions);
    
    final EventTypeNonSchema undeclaredEE;  
    final EventTypeNonSchema eventTypeNS;
    final EventTypeNonSchema eventTypeSC;
    final EventTypeNonSchema undeclaredWildcardAnyAT;
    final EventTypeNonSchema elementWildcard;
    final EventTypeNonSchema untypedCharacters;
    final EventTypeNonSchema entityReference;
    final EventTypeNonSchema comment;
    final EventTypeNonSchema processingInstruction;
    
    boolean addTupleL3 = false;
    int n_itemsL2 = 0;
    int n_itemsL3 = 0;
    
    if (addPI) {
      processingInstruction = new EventTypeProcessingInstruction(EventCode.EVENT_CODE_DEPTH_THREE, this, eventList);
      eventList.add(processingInstruction);
      ++n_itemsL3;
      addTupleL3 = true;
    }
    else {
      processingInstruction = null;
    }
    if (addCM) {
      comment = new EventTypeComment(EventCode.EVENT_CODE_DEPTH_THREE, this, eventList);
      eventList.add(comment);
      ++n_itemsL3;
      addTupleL3 = true;
    }
    else {
      comment = null;
    }
    if (addDTD) {
      entityReference = new EventTypeEntityReference(this, eventList);
      eventList.add(entityReference);
      ++n_itemsL2;
    }
    else {
      entityReference = null;
    }
    untypedCharacters = new EventTypeCharactersSecond(EventCode.EVENT_CODE_DEPTH_TWO, this, eventList);
    eventList.add(untypedCharacters);
    elementWildcard = new EventTypeElementWildcard(EventCode.EVENT_CODE_DEPTH_TWO, this, eventList);
    eventList.add(elementWildcard);
    n_itemsL2 += 2;
    if (addSC) {
      eventTypeSC = new EventTypeSelfContained(this, eventList);
      eventList.add(eventTypeSC);
      ++n_itemsL2;
    }
    else {
      eventTypeSC = null;
    }
    if (addNS) {
      eventTypeNS = new EventTypeNamespaceDeclaration(this, eventList);
      eventList.add(eventTypeNS);
      ++n_itemsL2;
    }
    else {
      eventTypeNS = null;
    }
    undeclaredWildcardAnyAT = new EventTypeAttributeWildcardAnyUntyped(EventCode.EVENT_CODE_DEPTH_TWO, this, eventList);
    eventList.add(undeclaredWildcardAnyAT);
    undeclaredEE = new EventTypeEndElementSecond(this, eventList); 
    eventList.add(undeclaredEE);
    n_itemsL2 += 2;
    if (addTupleL3) {
      ++n_itemsL2;
    }
    
    EventCode[] eventCodeItemsL2 = null;
    EventCode[] eventCodeItemsL3 = null;
    ArrayEventCodeTuple tupleL2 = null;
    ArrayEventCodeTuple tupleL3 = null;
    eventCodeItemsL2 = new EventCode[n_itemsL2];
    tupleL2 = new ArrayEventCodeTupleReversed(); 
    eventCodes.setInitialSoloTuple(tupleL2);
    if (addTupleL3) {
      eventCodeItemsL3 = new EventCode[n_itemsL3];
      tupleL3 = ArrayEventCodeTuple.createTuple();
      eventCodeItemsL2[n_itemsL2 - 1] = tupleL3;
    }
    int m = 0, k = 0;
    eventCodeItemsL2[m++] = undeclaredEE;
    eventCodeItemsL2[m++] = undeclaredWildcardAnyAT;
    if (addNS)
      eventCodeItemsL2[m++] = eventTypeNS; 
    if (addSC)
      eventCodeItemsL2[m++] = eventTypeSC; 
    eventCodeItemsL2[m++] = elementWildcard;
    eventCodeItemsL2[m++] = untypedCharacters;
    if (addDTD)
      eventCodeItemsL2[m++] = entityReference;
    if (addCM)
      eventCodeItemsL3[k++] = comment;
    if (addPI)
      eventCodeItemsL3[k++] = processingInstruction;
    tupleL2.setItems(eventCodeItemsL2);
    if (addTupleL3)
      tupleL3.setItems(eventCodeItemsL3);
  }

  private void populateContentGrammar(final short grammarOptions) {

    final ReversedEventTypeList eventList  = m_eventTypeListContent;
    final ReverseEventCodeTuple eventCodes = m_eventCodesContent;

    /*
     * ElementContent : 
     *   EE                    0 
     *   SE (*) ElementContent 1.0 
     *   CH ElementContent     1.1 
     *   ER ElementContent     1.2 (if addER)
     *   CM ElementContent     1.3.0 (if addCM)
     *   PI ElementContent     1.3.1 (if addPI)
     */

    final boolean addDTD = GrammarOptions.hasDTD(grammarOptions);
    final boolean addCM  = GrammarOptions.hasCM(grammarOptions);
    final boolean addPI  = GrammarOptions.hasPI(grammarOptions);
    
    final EventTypeNonSchema undeclaredEE;  
    final EventTypeNonSchema elementWildcard;
    final EventTypeNonSchema untypedCharacters;
    final EventTypeNonSchema entityReference;
    final EventTypeNonSchema comment;
    final EventTypeNonSchema processingInstruction;
    
    boolean addTupleL3 = false;
    int n_itemsL2 = 0;
    int n_itemsL3 = 0;
    
    if (addPI) {
      processingInstruction = new EventTypeProcessingInstruction(EventCode.EVENT_CODE_DEPTH_THREE, this, eventList);
      eventList.add(processingInstruction);
      ++n_itemsL3;
      addTupleL3 = true;
    }
    else {
      processingInstruction = null;
    }
    if (addCM) {
      comment = new EventTypeComment(EventCode.EVENT_CODE_DEPTH_THREE, this, eventList);
      eventList.add(comment);
      ++n_itemsL3;
      addTupleL3 = true;
    }
    else {
      comment = null;
    }
    if (addDTD) {
      entityReference = new EventTypeEntityReference(this, eventList);
      eventList.add(entityReference);
      ++n_itemsL2;
    }
    else {
      entityReference = null;
    }
    untypedCharacters = new EventTypeCharactersSecond(EventCode.EVENT_CODE_DEPTH_TWO, this, eventList);
    eventList.add(untypedCharacters);
    elementWildcard = new EventTypeElementWildcard(EventCode.EVENT_CODE_DEPTH_TWO, this, eventList);
    eventList.add(elementWildcard);
    n_itemsL2 += 2;
    if (addTupleL3) {
      ++n_itemsL2;
    }
    undeclaredEE = new EventTypeEndElementReversed(this, eventList); 
    eventList.add(undeclaredEE);
    
    EventCode[] eventCodeItemsL2 = null;
    EventCode[] eventCodeItemsL3 = null;
    ArrayEventCodeTuple tupleL2 = null;
    ArrayEventCodeTuple tupleL3 = null;
    eventCodeItemsL2 = new EventCode[n_itemsL2];
    tupleL2 = new ArrayEventCodeTupleReversed();
    eventCodes.setInitialItems(undeclaredEE, tupleL2);
    if (addTupleL3) {
      eventCodeItemsL3 = new EventCode[n_itemsL3];
      tupleL3 = ArrayEventCodeTuple.createTuple();
      eventCodeItemsL2[n_itemsL2 - 1] = tupleL3;
    }
    int m = 0, k = 0;
    eventCodeItemsL2[m++] = elementWildcard;
    eventCodeItemsL2[m++] = untypedCharacters;
    if (addDTD)
      eventCodeItemsL2[m++] = entityReference;
    if (addCM)
      eventCodeItemsL3[k++] = comment;
    if (addPI)
      eventCodeItemsL3[k++] = processingInstruction;
    tupleL2.setItems(eventCodeItemsL2);
    if (addTupleL3)
      tupleL3.setItems(eventCodeItemsL3);
  }

  private void cloneTagGrammar(
      Grammar ownerGrammar, ReverseEventCodeTuple sourceEventCodes, 
      ReversedEventTypeList eventList, ReverseEventCodeTuple eventCodes, 
      EventTypeNonSchema[] eventTypes) {
    
    System.arraycopy(m_eventTypesInit, 0, eventTypes, 0, EventCode.N_NONSCHEMA_ITEMS);
    
    assert sourceEventCodes.itemsCount == 1 && sourceEventCodes.getItem(0).itemType == EventCode.ITEM_TUPLE;
    
    /*
     * StartTagContent : 
     *   EE                     0.0 
     *   AT (*) StartTagContent 0.1 
     *   NS StartTagContent     0.2 (if addNS) 
     *   SC Fragment            0.3 (if addSC)
     *   SE (*) ElementContent  0.4
     *   CH ElementContent      0.5 
     *   ER ElementContent      0.6 (if addER)
     *   CM ElementContent      0.7.0 (if addCM)
     *   PI ElementContent      0.7.1 (if addPI)
     */

    final EventCodeTuple sourceTupleL2 = (EventCodeTuple)sourceEventCodes.getItem(0);
    final int n_itemsL2 = sourceTupleL2.itemsCount;

    final ArrayEventCodeTuple tupleL2 = new ArrayEventCodeTupleReversed();
    eventCodes.setInitialSoloTuple(tupleL2);
    final EventCode[] eventCodeItemsL2 = new EventCode[n_itemsL2];

    int i;
    for (i = 0; i < n_itemsL2; i++) {
      final EventCode ithSourceItem = sourceTupleL2.getItem(i);
      if (ithSourceItem.itemType != EventCode.ITEM_TUPLE) {
        final EventTypeNonSchema eventType = ((EventTypeNonSchema)ithSourceItem).duplicate(ownerGrammar, eventList);
        eventCodeItemsL2[i] = eventType;
        eventTypes[eventType.itemType] = eventType;
      }
      else {
        assert i == n_itemsL2 - 1;
        final EventCodeTuple sourceTupleL3 = (EventCodeTuple)ithSourceItem;
        final int n_itemsL3 = sourceTupleL3.itemsCount;
        final EventCode[] eventCodeItemsL3 = new EventCode[n_itemsL3];
        for (int j = 0; j < n_itemsL3; j++) {
          EventCode jthSourceItem = sourceTupleL3.getItem(j);
          final EventTypeNonSchema eventType = ((EventTypeNonSchema)jthSourceItem).duplicate(ownerGrammar, eventList);
          eventCodeItemsL3[j] = eventType;
          eventTypes[eventType.itemType] = eventType;
        }
        ArrayEventCodeTuple tupleL3 = ArrayEventCodeTuple.createTuple();
        eventCodeItemsL2[i] = tupleL3;
        tupleL2.setItems(eventCodeItemsL2);
        tupleL3.setItems(eventCodeItemsL3);
        break;
      }
    }
    if (i == n_itemsL2) {
      tupleL2.setItems(eventCodeItemsL2);
    }
    
    for (i = 0; i < EventCode.N_NONSCHEMA_ITEMS; i++) {
      final EventTypeNonSchema ith = eventTypes[i];
      if (ith != null) {
        eventList.add(ith);
      }
    }
  }
  
  private void cloneContentGrammar(
      Grammar ownerGrammar, ReverseEventCodeTuple sourceEventCodes, 
      ReversedEventTypeList eventList, ReverseEventCodeTuple eventCodes, 
      EventTypeNonSchema[] eventTypes) {
    
    System.arraycopy(m_eventTypesInit, 0, eventTypes, 0, EventCode.N_NONSCHEMA_ITEMS);
    
    assert sourceEventCodes.itemsCount == 2 && sourceEventCodes.getItem(1).itemType == EventCode.ITEM_TUPLE;
    
    /*
     * ElementContent : 
     *   EE                    0 
     *   SE (*) ElementContent 1.0 
     *   CH ElementContent     1.1 
     *   ER ElementContent     1.2 (if addER)
     *   CM ElementContent     1.3.0 (if addCM)
     *   PI ElementContent     1.3.1 (if addPI)
     */

    final EventCodeTuple sourceTupleL2 = (EventCodeTuple)sourceEventCodes.getItem(1);
    final int n_itemsL2 = sourceTupleL2.itemsCount;

    final ArrayEventCodeTuple tupleL2 = new ArrayEventCodeTupleReversed();
    final EventTypeNonSchema undeclaredEE;  
    undeclaredEE = new EventTypeEndElementReversed(ownerGrammar, eventList); 
    eventTypes[EventCode.ITEM_EE] = undeclaredEE;
    eventCodes.setInitialItems(undeclaredEE, tupleL2);
    final EventCode[] eventCodeItemsL2 = new EventCode[n_itemsL2];

    int i;
    for (i = 0; i < n_itemsL2; i++) {
      final EventCode ithSourceItem = sourceTupleL2.getItem(i);
      if (ithSourceItem.itemType != EventCode.ITEM_TUPLE) {
        final EventTypeNonSchema eventType = ((EventTypeNonSchema)ithSourceItem).duplicate(ownerGrammar, eventList);
        eventCodeItemsL2[i] = eventType;
        eventTypes[eventType.itemType] = eventType;
      }
      else {
        assert i == n_itemsL2 - 1;
        final EventCodeTuple sourceTupleL3 = (EventCodeTuple)ithSourceItem;
        final int n_itemsL3 = sourceTupleL3.itemsCount;
        final EventCode[] eventCodeItemsL3 = new EventCode[n_itemsL3];
        for (int j = 0; j < n_itemsL3; j++) {
          EventCode jthSourceItem = sourceTupleL3.getItem(j);
          final EventTypeNonSchema eventType = ((EventTypeNonSchema)jthSourceItem).duplicate(ownerGrammar, eventList);
          eventCodeItemsL3[j] = eventType;
          eventTypes[eventType.itemType] = eventType;
        }
        ArrayEventCodeTuple tupleL3 = ArrayEventCodeTuple.createTuple();
        eventCodeItemsL2[i] = tupleL3;
        tupleL2.setItems(eventCodeItemsL2);
        tupleL3.setItems(eventCodeItemsL3);
        break;
      }
    }
    if (i == n_itemsL2) {
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
  void element(int eventTypeIndex, String uri, String name, GrammarState stateVariables) {
    final ReversedEventTypeList eventTypeList;
    if (stateVariables.phase != ELEMENT_STATE_IN_TAG)
      eventTypeList = m_eventTypeListContent;
    else {
      eventTypeList = m_eventTypeListTag;
      stateVariables.phase = ELEMENT_STATE_IN_CONTENT;
    }
    final EventType eventType = eventTypeList.item(eventTypeIndex);
    assert eventType.itemType == EventCode.ITEM_SE;
    final Grammar ensuingGrammar = ((EventTypeElement)eventType).getEnsuingGrammar();
    ensuingGrammar.init(stateVariables.documentGrammarState.pushState());
  }

  @Override
  public void chars(GrammarState stateVariables) {
    throw new IllegalStateException("char() cannot be invoked on a built-in element grammar.");
  }

  @Override
  public void undeclaredChars(GrammarState stateVariables) {
    final ReversedEventTypeList eventTypeList;
    eventTypeList = stateVariables.phase != ELEMENT_STATE_IN_TAG ? m_eventTypeListContent : m_eventTypeListTag;
    if (!eventTypeList.hasDepthOneCH) {
      final ReverseEventCodeTuple eventCodes;
      eventCodes = stateVariables.phase != ELEMENT_STATE_IN_TAG ? m_eventCodesContent : m_eventCodesTag;
      final EventTypeCharacters untypedCharacters;
      untypedCharacters = new EventTypeCharactersReversed(this, eventTypeList); 
      eventTypeList.add(untypedCharacters);
      eventCodes.addItem(untypedCharacters);
      assert eventTypeList.hasDepthOneCH;
    }
    stateVariables.phase = ELEMENT_STATE_IN_CONTENT;
  }

  @Override
  final void end(String uri, String name, GrammarState stateVariables) {
    if (stateVariables.phase == ELEMENT_STATE_IN_TAG && !m_eventTypeListTag.hasDepthOneEE) {
      final EventTypeNonSchema undeclaredEE;  
      undeclaredEE = new EventTypeEndElementReversed(this, m_eventTypeListTag); 
      m_eventTypeListTag.add(undeclaredEE);
      m_eventCodesTag.addItem(undeclaredEE);
      assert m_eventTypeListTag.hasDepthOneEE;
    }
    finish(stateVariables);
  }
  
  @Override
  final void done(GrammarState kid, GrammarState stateVariables) {
    if (kid.targetGrammar.getGrammarType() == Grammar.SCHEMA_GRAMMAR_ELEMENT_TAG) {
      finish(stateVariables);
    }
  }

  @Override
  public EventCodeTuple getNextEventCodes(GrammarState stateVariables) {
    return stateVariables.phase != ELEMENT_STATE_IN_TAG ? m_eventCodesContent : m_eventCodesTag;
  }

  @Override
  public EventTypeList getNextEventTypes(GrammarState stateVariables) {
    return stateVariables.phase != ELEMENT_STATE_IN_TAG ? m_eventTypeListContent : m_eventTypeListTag;
  }

  @Override
  public void miscContent(GrammarState stateVariables) {
    stateVariables.phase = ELEMENT_STATE_IN_CONTENT;
  }

  @Override
  void xsitp(int tp, GrammarState stateVariables) {
    assert stateVariables.phase == ELEMENT_STATE_IN_TAG;
    assert tp != EXISchema.NIL_NODE && (m_schema.getNodeType(tp) & EXISchema.TYPE_MASK) == EXISchema.TYPE_MASK;
    final GrammarState elementTagStateVariables = stateVariables.documentGrammarState.pushState();
    stateVariables.elementTagStateVariables = elementTagStateVariables;
    m_grammarCache.retrieveElementTagGrammar(tp).init(elementTagStateVariables);
    stateVariables.phase = ELEMENT_STATE_DELEGATED;
  }

  @Override
  Grammar undeclaredElement(final String uri, final String name, final GrammarState stateVariables) {
    final Grammar ensuingGrammar = super.undeclaredElement(uri, name, stateVariables);
    final ReversedEventTypeList eventTypeList;
    final ReverseEventCodeTuple eventCodes;
    if (stateVariables.phase != ELEMENT_STATE_IN_TAG) {
      eventTypeList = m_eventTypeListContent;
      eventCodes = m_eventCodesContent;
    }
    else {
      eventTypeList = m_eventTypeListTag;
      eventCodes = m_eventCodesTag;
      stateVariables.phase = ELEMENT_STATE_IN_CONTENT;
    }
    final EventTypeElement eventTypeElement = new EventTypeElementReversed(uri, name, this, eventTypeList, ensuingGrammar); 
    eventTypeList.add(eventTypeElement);
    eventCodes.addItem(eventTypeElement);
    return ensuingGrammar;
  }

  @Override
  void undeclaredAttribute(String uri, String name, GrammarState stateVariables) {
    assert stateVariables.phase == ELEMENT_STATE_IN_TAG;
    final EventTypeAttribute eventTypeAttribute = new EventTypeAttributeReversed(uri, name, this, m_eventTypeListTag); 
    m_eventTypeListTag.add(eventTypeAttribute);
    m_eventCodesTag.addItem(eventTypeAttribute);
  }

}
