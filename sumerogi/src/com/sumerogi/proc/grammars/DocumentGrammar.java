package com.sumerogi.proc.grammars;

import com.sumerogi.proc.common.EventCode;
import com.sumerogi.proc.common.EventDescription;
import com.sumerogi.proc.common.EventType;
import com.sumerogi.proc.common.EventTypeList;
import com.sumerogi.proc.common.StringTable;

final class DocumentGrammar extends Grammar {
  
  private static final byte DOCUMENT_STATE_BASE = 0;
  protected static final byte DOCUMENT_STATE_CREATED     = DOCUMENT_STATE_BASE;
  protected static final byte DOCUMENT_STATE_DEPLETE     = DOCUMENT_STATE_CREATED + 1;
  public static final byte DOCUMENT_STATE_COMPLETED      = DOCUMENT_STATE_DEPLETE + 1;
  public static final byte DOCUMENT_STATE_END            = DOCUMENT_STATE_COMPLETED + 1;

  ///////////////////////////////////////////////////////////////////////////
  /// immutables
  ///////////////////////////////////////////////////////////////////////////

  private final EventType[][] m_eventTypes;
  private final EventCodeTuple[] m_eventCodes;
  private final ArrayEventTypeList[] m_eventTypeLists;

  DocumentGrammar(GrammarCache grammarCache) {
    super(SCHEMA_GRAMMAR_DOCUMENT, grammarCache);
    
    m_eventTypes = new EventType[3][];
    m_eventCodes = new EventCodeTuple[3];
    m_eventTypeLists = new ArrayEventTypeList[3];

    int i;
    for (i = 0; i < 3; i++) {
      m_eventTypeLists[i] = new ArrayEventTypeList();
    }
    
    int n_eventTypes, n_items, n_itemsL2, n_itemsL3;
    EventType[] eventTypes;
    ArrayEventCodeTuple tuple, tupleL2, tupleL3;
    EventCode[] eventCodeItems;
    
    eventTypes = new EventType[1];
    eventTypes[0] = EventTypeFactory.createStartDocument(m_eventTypeLists[0]);
    tuple = new ArrayEventCodeTuple(); 
    tuple.setItems(new EventType[] { eventTypes[0] });
    m_eventCodes[0] = tuple;
    m_eventTypes[0] = eventTypes;

    /*
     * SO 0
     * SA 1.0
     * SV 1.1.0
     * NV 1.1.1
     * BV 1.1.2
     * NL 1.1.3
     */
    tupleL2 = tupleL3 = null;
    n_items = 2; // SO and tupleL2
    n_itemsL2 = 2; // SA and tupleL3
    n_itemsL3 = 4; // SV, NV, BV, NL
    n_eventTypes = n_itemsL3 + n_itemsL2 + n_items  - 2;
    eventTypes = new EventType[n_eventTypes];
    eventCodeItems = new EventCode[n_items];
    EventCode[] eventCodeItemsL3;
    int n = 0;
    final EventType so = new EventType(EventCode.EVENT_CODE_DEPTH_ONE, 
        m_eventTypeLists[1], EventType.ITEM_START_OBJECT_ANONYMOUS, EventDescription.EVENT_START_OBJECT);
    eventCodeItems[n] = eventTypes[n] = so;
    ++n;
    EventCode[] eventCodeItemsL2 = new EventCode[n_itemsL2];
    tupleL2 = new ArrayEventCodeTuple();
    eventCodeItems[n] = tupleL2;
    int m = 0;
    final EventType sa = new EventType(EventCode.EVENT_CODE_DEPTH_TWO, 
        m_eventTypeLists[1], EventType.ITEM_START_ARRAY_ANONYMOUS, EventDescription.EVENT_START_ARRAY);
    eventTypes[n++] = sa;
    eventCodeItemsL2[m++] = sa;
    eventCodeItemsL3 = new EventCode[n_itemsL3];
    tupleL3 = new ArrayEventCodeTuple();
    eventCodeItemsL2[m] = tupleL3;
    int k = 0;
    final EventType sv = new EventType(EventCode.EVENT_CODE_DEPTH_THREE, 
        m_eventTypeLists[1], EventType.ITEM_STRING_VALUE_ANONYMOUS);
    eventTypes[n++] = sv;
    eventCodeItemsL3[k++] = sv;
    final EventType nv = new EventType(EventCode.EVENT_CODE_DEPTH_THREE, 
        m_eventTypeLists[1], EventType.ITEM_NUMBER_VALUE_ANONYMOUS);
    eventTypes[n++] = nv;
    eventCodeItemsL3[k++] = nv;
    final EventType bv = new EventType(EventCode.EVENT_CODE_DEPTH_THREE, 
        m_eventTypeLists[1], EventType.ITEM_BOOLEAN_VALUE_ANONYMOUS);
    eventTypes[n++] = bv;
    eventCodeItemsL3[k++] = bv;
    final EventType nl = new EventTypeNullAnonymous(EventCode.EVENT_CODE_DEPTH_THREE, m_eventTypeLists[1]); 
    eventTypes[n++] = nl;
    eventCodeItemsL3[k++] = nl;
    
    tuple = new ArrayEventCodeTuple();
    tuple.setItems(eventCodeItems);
    tupleL2.setItems(eventCodeItemsL2);
    tupleL3.setItems(eventCodeItemsL3);
    m_eventCodes[1] = tuple;
    m_eventTypes[1] = eventTypes;
    
    eventTypes = new EventType[1];
    eventTypes[0] = EventTypeFactory.createEndDocument(m_eventTypeLists[2]);
    tuple = new ArrayEventCodeTuple(); 
    tuple.setItems(new EventType[] { eventTypes[0] });
    m_eventCodes[2] = tuple;
    m_eventTypes[2] = eventTypes;
    
    for (i = 0; i < 3; i++) {
      m_eventTypeLists[i].setItems(m_eventTypes[i]);
    }
  }
  
  @Override
  public void init(GrammarState stateVariables) {
    stateVariables.targetGrammar = this;
    stateVariables.name = StringTable.NAME_DOCUMENT; // i.e. no name 
    stateVariables.phase = DOCUMENT_STATE_CREATED;
  }

  @Override
  EventTypeList getNextEventTypes(GrammarState stateVariables) {
    switch (stateVariables.phase) {
      case DOCUMENT_STATE_CREATED:
        return m_eventTypeLists[0];
      case DOCUMENT_STATE_DEPLETE:
        return m_eventTypeLists[1];
      case DOCUMENT_STATE_COMPLETED:
        return m_eventTypeLists[2];
      case DOCUMENT_STATE_END:
        // REVISIT: return a meaningful object.
        return null;
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
      case DOCUMENT_STATE_DEPLETE:
        return m_eventCodes[1];
      case DOCUMENT_STATE_COMPLETED:
        return m_eventCodes[2];
      default:
        assert stateVariables.phase == DOCUMENT_STATE_END;
        return null;
    }
  }

  @Override
  public final void startDocument(GrammarState stateVariables) {
    assert stateVariables.phase == DOCUMENT_STATE_CREATED;
    stateVariables.phase = DOCUMENT_STATE_DEPLETE;
  }

  @Override
  public final void endDocument(GrammarState stateVariables) {
    if (stateVariables.phase == DOCUMENT_STATE_COMPLETED)
      stateVariables.phase = DOCUMENT_STATE_END;
  }
  
  @Override
  public Grammar startObjectAnonymous(EventType eventType, GrammarState stateVariables) {
    assert stateVariables.phase ==  DOCUMENT_STATE_DEPLETE; 
    stateVariables.phase = DOCUMENT_STATE_COMPLETED;
    return super.startObjectAnonymous(eventType, stateVariables);
  }

  @Override
  public void startObjectNamed(EventType eventType, GrammarState stateVariables) {
    throw new IllegalStateException();
  }

  @Override
  public Grammar startObjectWildcard(int name, GrammarState stateVariables) {
    throw new IllegalStateException();
  }
  
  @Override
  public void endObject(GrammarState stateVariables) {
    throw new IllegalStateException();
  }

  @Override
  public Grammar startArrayAnonymous(final GrammarState stateVariables) {
    assert stateVariables.phase ==  DOCUMENT_STATE_DEPLETE; 
    stateVariables.phase = DOCUMENT_STATE_COMPLETED;
    return super.startArrayAnonymous(stateVariables);
  }

  @Override
  public void startArrayNamed(EventType eventType, GrammarState stateVariables) {
    throw new IllegalStateException();
  }

  @Override
  public Grammar startArrayWildcard(int name, GrammarState stateVariables) {
    throw new IllegalStateException();
  }
  
  @Override
  public void endArray(GrammarState stateVariables) {
    throw new IllegalStateException();
  }

  @Override
  public void anonymousStringValue(EventType eventType, GrammarState stateVariables) {
    assert stateVariables.phase ==  DOCUMENT_STATE_DEPLETE; 
    stateVariables.phase = DOCUMENT_STATE_COMPLETED;
  }
  
  @Override
  public void wildcardStringValue(int eventTypeIndex, int nameId) {
    throw new IllegalStateException();
  }
  
  @Override
  public void anonymousNumberValue(EventType eventType, GrammarState stateVariables) {
    assert stateVariables.phase ==  DOCUMENT_STATE_DEPLETE; 
    stateVariables.phase = DOCUMENT_STATE_COMPLETED;
  }
  
  @Override
  public void wildcardNumberValue(int eventTypeIndex, int nameId) {
    // REVISIT: implement
    throw new IllegalStateException();
  }
  
  @Override
  public void anonymousBooleanValue(EventType eventType, GrammarState stateVariables) {
    assert stateVariables.phase ==  DOCUMENT_STATE_DEPLETE; 
    stateVariables.phase = DOCUMENT_STATE_COMPLETED;
  }
  
  @Override
  public void wildcardBooleanValue(int eventTypeIndex, int nameId) {
    // REVISIT: implement
    throw new IllegalStateException();
  }
  
  @Override
  public void anonymousNullValue(EventType eventType, GrammarState stateVariables) {
    assert stateVariables.phase ==  DOCUMENT_STATE_DEPLETE; 
    stateVariables.phase = DOCUMENT_STATE_COMPLETED;
  }
  
  @Override
  public void wildcardNullValue(int eventTypeIndex, int nameId) {
    // REVISIT: implement
    throw new IllegalStateException();
  }

}
