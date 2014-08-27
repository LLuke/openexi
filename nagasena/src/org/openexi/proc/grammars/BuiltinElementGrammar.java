package org.openexi.proc.grammars;

import org.openexi.proc.common.EventCode;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventTypeList;
import org.openexi.proc.common.IGrammar;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.common.StringTable;
import org.openexi.proc.common.XmlUriConst;
import org.openexi.schema.EXISchema;
import org.openexi.schema.EXISchemaConst;

final class BuiltinElementGrammar extends BuiltinGrammar implements IGrammar {

  private final ReversedEventTypeList m_eventTypeListTag;
  private final ReversedEventTypeList m_eventTypeListContent;
  
  private final ReverseEventCodeTuple m_eventCodesTag;
  private final ReverseEventCodeTuple m_eventCodesContent;
  
  private boolean dirty;
  StringTable.LocalNamePartition localNamePartition;

  private boolean m_xsiTypeAvailable; 

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
  BuiltinElementGrammar(final String uri, final GrammarCache grammarCache) {
    super(BUILTIN_GRAMMAR_ELEMENT, grammarCache);
    
    final short grammarOptions = grammarCache.grammarOptions;
    
    m_eventTypeListTag = new ReversedEventTypeList();
    m_eventCodesTag = new ReverseEventCodeTuple();
    populateTagGrammar(grammarOptions);

    m_eventTypeListContent = new ReversedEventTypeList();
    m_eventCodesContent = new ReverseEventCodeTuple();
    populateContentGrammar(grammarOptions);
  }
  
  /**
   * For exclusive use by GrammarCache only.
   * GrammarCache calls this method to instantiate a new BuiltinElementGrammar
   * from a template grammar.
   */
  BuiltinElementGrammar duplicate(final String uri, final EventType[] eventTypes) {
    return new BuiltinElementGrammar(uri, m_grammarCache, 
        m_eventCodesTag, m_eventCodesContent, eventTypes);
  }

  /**
   * Used only by duplicate() method above.
   */
  private BuiltinElementGrammar(final String uri, GrammarCache grammarCache, 
      ReverseEventCodeTuple sourceEventCodesTag, 
      ReverseEventCodeTuple sourceEventCodesContent, EventType[] eventTypes) {
    super(BUILTIN_GRAMMAR_ELEMENT, grammarCache);
    
    m_eventTypeListTag = new ReversedEventTypeList();
    m_eventCodesTag = new ReverseEventCodeTuple();
    
    cloneTagGrammar(this, sourceEventCodesTag, m_eventTypeListTag, m_eventCodesTag, eventTypes);
    m_eventTypeListTag.checkPoint();
    m_eventCodesTag.checkPoint();

    m_eventTypeListContent = new ReversedEventTypeList();
    m_eventCodesContent = new ReverseEventCodeTuple();

    cloneContentGrammar(this, sourceEventCodesContent, m_eventTypeListContent, m_eventCodesContent, eventTypes);
    m_eventTypeListContent.checkPoint();
    m_eventCodesContent.checkPoint();
    
    dirty = false;
    m_xsiTypeAvailable = false;
  }

  @Override
  public void init(GrammarState stateVariables) {
    stateVariables.targetGrammar = this;
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
    
    final EventType undeclaredEE;  
    final EventType eventTypeNS;
    final EventType eventTypeSC;
    final EventType undeclaredWildcardAnyAT;
    final EventType elementWildcard;
    final EventType untypedCharacters;
    final EventType entityReference;
    final EventType comment;
    final EventType processingInstruction;
    
    boolean addTupleL3 = false;
    int n_itemsL2 = 0;
    int n_itemsL3 = 0;
    
    if (addPI) {
      processingInstruction = new EventType(EventCode.EVENT_CODE_DEPTH_THREE, eventList, EventType.ITEM_PI, (IGrammar)null); 
      eventList.add(processingInstruction);
      ++n_itemsL3;
      addTupleL3 = true;
    }
    else {
      processingInstruction = null;
    }
    if (addCM) {
      comment = new EventType(EventCode.EVENT_CODE_DEPTH_THREE, eventList, EventType.ITEM_CM, (IGrammar)null);
      eventList.add(comment);
      ++n_itemsL3;
      addTupleL3 = true;
    }
    else {
      comment = null;
    }
    if (addDTD) {
      entityReference = new EventType(EventCode.EVENT_CODE_DEPTH_TWO, eventList, EventType.ITEM_ER, (IGrammar)null);
      eventList.add(entityReference);
      ++n_itemsL2;
    }
    else {
      entityReference = null;
    }
    untypedCharacters = new EventType(EventCode.EVENT_CODE_DEPTH_TWO, eventList, EventType.ITEM_CH, (IGrammar)null);
    eventList.add(untypedCharacters);
    elementWildcard = new EventType(EventCode.EVENT_CODE_DEPTH_TWO, eventList, EventType.ITEM_SE_WC, (IGrammar)null);
    eventList.add(elementWildcard);
    n_itemsL2 += 2;
    if (addSC) {
      eventTypeSC = new EventType(EventCode.EVENT_CODE_DEPTH_TWO, eventList, EventType.ITEM_SC, (IGrammar)null);
      eventList.add(eventTypeSC);
      ++n_itemsL2;
    }
    else {
      eventTypeSC = null;
    }
    if (addNS) {
      eventTypeNS = new EventType(EventCode.EVENT_CODE_DEPTH_TWO, eventList, EventType.ITEM_NS, (IGrammar)null);
      eventList.add(eventTypeNS);
      ++n_itemsL2;
    }
    else {
      eventTypeNS = null;
    }
    undeclaredWildcardAnyAT = new EventType(EventCode.EVENT_CODE_DEPTH_TWO, eventList, EventType.ITEM_AT_WC_ANY_UNTYPED, (IGrammar)null);
    eventList.add(undeclaredWildcardAnyAT);
    undeclaredEE = EventTypeFactory.creatEndElement(EventCode.EVENT_CODE_DEPTH_TWO, eventList); 
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
    tupleL2 = new ArrayEventCodeTuple(); 
    eventCodes.setInitialSoloTuple(tupleL2);
    if (addTupleL3) {
      eventCodeItemsL3 = new EventCode[n_itemsL3];
      tupleL3 = new ArrayEventCodeTuple();
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
    
    final EventType undeclaredEE;  
    final EventType elementWildcard;
    final EventType untypedCharacters;
    final EventType entityReference;
    final EventType comment;
    final EventType processingInstruction;
    
    boolean addTupleL3 = false;
    int n_itemsL2 = 0;
    int n_itemsL3 = 0;
    
    if (addPI) {
      processingInstruction = new EventType(EventCode.EVENT_CODE_DEPTH_THREE, eventList, EventType.ITEM_PI, (IGrammar)null);
      eventList.add(processingInstruction);
      ++n_itemsL3;
      addTupleL3 = true;
    }
    else {
      processingInstruction = null;
    }
    if (addCM) {
      comment = new EventType(EventCode.EVENT_CODE_DEPTH_THREE, eventList, EventType.ITEM_CM, (IGrammar)null);
      eventList.add(comment);
      ++n_itemsL3;
      addTupleL3 = true;
    }
    else {
      comment = null;
    }
    if (addDTD) {
      entityReference = new EventType(EventCode.EVENT_CODE_DEPTH_TWO, eventList, EventType.ITEM_ER, (IGrammar)null);
      eventList.add(entityReference);
      ++n_itemsL2;
    }
    else {
      entityReference = null;
    }
    untypedCharacters = new EventType(EventCode.EVENT_CODE_DEPTH_TWO, eventList, EventType.ITEM_CH, (IGrammar)null);
    eventList.add(untypedCharacters);
    elementWildcard = new EventType(EventCode.EVENT_CODE_DEPTH_TWO, eventList, EventType.ITEM_SE_WC, (IGrammar)null); 
    eventList.add(elementWildcard);
    n_itemsL2 += 2;
    if (addTupleL3) {
      ++n_itemsL2;
    }
    undeclaredEE = EventTypeFactory.creatEndElement(EventCode.EVENT_CODE_DEPTH_ONE, eventList); 
    eventList.add(undeclaredEE);
    
    EventCode[] eventCodeItemsL2 = null;
    EventCode[] eventCodeItemsL3 = null;
    ArrayEventCodeTuple tupleL2 = null;
    ArrayEventCodeTuple tupleL3 = null;
    eventCodeItemsL2 = new EventCode[n_itemsL2];
    tupleL2 = new ArrayEventCodeTuple();
    eventCodes.setInitialItems(undeclaredEE, tupleL2);
    if (addTupleL3) {
      eventCodeItemsL3 = new EventCode[n_itemsL3];
      tupleL3 = new ArrayEventCodeTuple();
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
      EventType[] eventTypes) {
    
    System.arraycopy(m_eventTypesInit, 0, eventTypes, 0, N_NONSCHEMA_ITEMS);
    
    assert sourceEventCodes.itemsCount == 1 && sourceEventCodes.getItem(0).itemType == EventType.ITEM_TUPLE;
    
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

    final ArrayEventCodeTuple tupleL2 = new ArrayEventCodeTuple();
    eventCodes.setInitialSoloTuple(tupleL2);
    final EventCode[] eventCodeItemsL2 = new EventCode[n_itemsL2];

    int i;
    for (i = 0; i < n_itemsL2; i++) {
      final EventCode ithSourceItem = sourceTupleL2.getItem(i);
      if (ithSourceItem.itemType != EventType.ITEM_TUPLE) {
        final EventType eventType = duplicate(((EventType)ithSourceItem), eventList);
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
          final EventType eventType = duplicate(((EventType)jthSourceItem), eventList);
          eventCodeItemsL3[j] = eventType;
          eventTypes[eventType.itemType] = eventType;
        }
        ArrayEventCodeTuple tupleL3 = new ArrayEventCodeTuple();
        eventCodeItemsL2[i] = tupleL3;
        tupleL2.setItems(eventCodeItemsL2);
        tupleL3.setItems(eventCodeItemsL3);
        break;
      }
    }
    if (i == n_itemsL2) {
      tupleL2.setItems(eventCodeItemsL2);
    }
    
    for (i = 0; i < N_NONSCHEMA_ITEMS; i++) {
      final EventType ith = eventTypes[i];
      if (ith != null) {
        eventList.add(ith);
      }
    }
  }
  
  private void cloneContentGrammar(
      Grammar ownerGrammar, ReverseEventCodeTuple sourceEventCodes, 
      ReversedEventTypeList eventList, ReverseEventCodeTuple eventCodes, 
      EventType[] eventTypes) {
    
    System.arraycopy(m_eventTypesInit, 0, eventTypes, 0, N_NONSCHEMA_ITEMS);
    
    assert sourceEventCodes.itemsCount == 2 && sourceEventCodes.getItem(1).itemType == EventType.ITEM_TUPLE;
    
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

    final ArrayEventCodeTuple tupleL2 = new ArrayEventCodeTuple();
    final EventType undeclaredEE;  
    undeclaredEE = EventTypeFactory.creatEndElement(EventCode.EVENT_CODE_DEPTH_ONE, eventList); 
    eventTypes[EventType.ITEM_EE] = undeclaredEE;
    eventCodes.setInitialItems(undeclaredEE, tupleL2);
    final EventCode[] eventCodeItemsL2 = new EventCode[n_itemsL2];

    int i;
    for (i = 0; i < n_itemsL2; i++) {
      final EventCode ithSourceItem = sourceTupleL2.getItem(i);
      if (ithSourceItem.itemType != EventType.ITEM_TUPLE) {
        final EventType eventType = duplicate(((EventType)ithSourceItem), eventList);
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
          final EventType eventType = duplicate(((EventType)jthSourceItem), eventList);
          eventCodeItemsL3[j] = eventType;
          eventTypes[eventType.itemType] = eventType;
        }
        ArrayEventCodeTuple tupleL3 = new ArrayEventCodeTuple();
        eventCodeItemsL2[i] = tupleL3;
        tupleL2.setItems(eventCodeItemsL2);
        tupleL3.setItems(eventCodeItemsL3);
        break;
      }
    }
    if (i == n_itemsL2) {
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
  /// Implementation of IGrammar (used by StringTable)
  ///////////////////////////////////////////////////////////////////////////

  public void reset() {
    if (dirty) {
      m_eventTypeListTag.reset();
      m_eventCodesTag.reset();
      m_eventTypeListContent.reset();
      m_eventCodesContent.reset();
      dirty = m_xsiTypeAvailable = false;
    }
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Method implementations for event processing
  ///////////////////////////////////////////////////////////////////////////

  @Override
  public void element(EventType eventType, GrammarState stateVariables) {
    stateVariables.phase = ELEMENT_STATE_IN_CONTENT;
    assert eventType.itemType == EventType.ITEM_SE;
    final Grammar ensuingGrammar = ((EventTypeElement)eventType).ensuingGrammar;
    ensuingGrammar.init(stateVariables.apparatus.pushState());
  }

  @Override
  public void undeclaredChars(int eventTypeIndex, GrammarState stateVariables) {
    final ReversedEventTypeList eventTypeList;
    eventTypeList = stateVariables.phase != ELEMENT_STATE_IN_TAG ? m_eventTypeListContent : m_eventTypeListTag;
    if (!eventTypeList.hasDepthOneCH) {
      final ReverseEventCodeTuple eventCodes;
      eventCodes = stateVariables.phase != ELEMENT_STATE_IN_TAG ? m_eventCodesContent : m_eventCodesTag;
      final EventType untypedCharacters;
      untypedCharacters = new EventType(EventCode.EVENT_CODE_DEPTH_ONE, eventTypeList, EventType.ITEM_CH, (IGrammar)null);
      eventTypeList.add(untypedCharacters);
      eventCodes.addItem(untypedCharacters);
      if (!dirty) {
        localNamePartition.addTouchedBuiltinElementGrammars(this);
        dirty = true;
      }
      assert eventTypeList.hasDepthOneCH;
    }
    stateVariables.phase = ELEMENT_STATE_IN_CONTENT;
  }

  @Override
  public final void end(GrammarState stateVariables) {
    if (stateVariables.phase == ELEMENT_STATE_IN_TAG && !m_eventTypeListTag.hasDepthOneEE) {
      final EventType undeclaredEE;  
      undeclaredEE = EventTypeFactory.creatEndElement(EventCode.EVENT_CODE_DEPTH_ONE, m_eventTypeListTag); 
      m_eventTypeListTag.add(undeclaredEE);
      m_eventCodesTag.addItem(undeclaredEE);
      if (!dirty) {
        localNamePartition.addTouchedBuiltinElementGrammars(this);
        dirty = true;
      }
      assert m_eventTypeListTag.hasDepthOneEE;
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
  public void miscContent(int eventTypeIndex, GrammarState stateVariables) {
    stateVariables.phase = ELEMENT_STATE_IN_CONTENT;
  }

  @Override
  void xsitp(int tp, GrammarState stateVariables) {
    assert tp != EXISchema.NIL_NODE && stateVariables.phase == ELEMENT_STATE_IN_TAG;
    final EXIGrammar typeGrammar = m_grammarCache.getTypeGrammar(tp);
    typeGrammar.init(stateVariables);
    stateVariables.contentDatatype = schema.isSimpleType(tp) ? tp : schema.getContentDatatypeOfComplexType(tp);
  }

  @Override
  public Grammar wildcardElement(int eventTypeIndex, int uriId, int localNameId, GrammarState stateVariables) {
    final Grammar ensuingGrammar = super.wildcardElement(eventTypeIndex, uriId, localNameId, stateVariables);
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
    final StringTable uriPartition = stateVariables.apparatus.stringTable;
    final String uri = uriPartition.getURI(uriId);
    final String name = uriPartition.getLocalNamePartition(uriId).localNameEntries[localNameId].localName;
    final EventTypeElement eventTypeElement = new EventTypeElement(uriId, uri, localNameId, name, 
        eventTypeList, ensuingGrammar, (IGrammar)null); 
    eventTypeList.add(eventTypeElement);
    eventCodes.addItem(eventTypeElement);
    if (!dirty) {
      localNamePartition.addTouchedBuiltinElementGrammars(this);
      dirty = true;
    }
    return ensuingGrammar;
  }

  @Override
  public void wildcardAttribute(int eventTypeIndex, int uriId, int localNameId, GrammarState stateVariables) {
    assert stateVariables.phase == ELEMENT_STATE_IN_TAG;
    final boolean isXsiType = uriId == XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI_ID && localNameId == EXISchemaConst.XSI_LOCALNAME_TYPE_ID;
    if (!isXsiType || !m_xsiTypeAvailable) {
      final StringTable uriPartition = stateVariables.apparatus.stringTable;
      final String uri = uriPartition.getURI(uriId);
      final String name = uriPartition.getLocalNamePartition(uriId).localNameEntries[localNameId].localName;
      final EventType eventTypeAttribute = new EventType(uri, name, uriId, localNameId, EventCode.EVENT_CODE_DEPTH_ONE, m_eventTypeListTag, EventType.ITEM_AT, (IGrammar)null);
      m_eventTypeListTag.add(eventTypeAttribute);
      m_eventCodesTag.addItem(eventTypeAttribute);
      if (!dirty) {
        localNamePartition.addTouchedBuiltinElementGrammars(this);
        dirty = true;
      }
      if (isXsiType)
        m_xsiTypeAvailable = true;
    }
  }

}
