package org.openexi.fujitsu.proc.grammars;

import org.openexi.fujitsu.proc.common.EventCode;
import org.openexi.fujitsu.proc.common.EventType;
import org.openexi.fujitsu.proc.common.EventTypeList;
import org.openexi.fujitsu.proc.common.GrammarOptions;
import org.openexi.fujitsu.schema.EXISchema;

class DocumentGrammar extends RootGrammar {

  ///////////////////////////////////////////////////////////////////////////
  /// immutables
  ///////////////////////////////////////////////////////////////////////////

  private final int[] m_elems;
  private final AbstractEventType[][] m_eventTypes;
  private final EventCodeTuple[] m_eventCodes;
  private final ArrayEventTypeList[] m_eventTypeLists;

  ///////////////////////////////////////////////////////////////////////////
  /// containers with variable contents
  ///////////////////////////////////////////////////////////////////////////

  ///////////////////////////////////////////////////////////////////////////
  /// Constructors, initializers
  ///////////////////////////////////////////////////////////////////////////

  DocumentGrammar(GrammarCache stateCache) {
    super(SCHEMA_GRAMMAR_DOCUMENT, stateCache);

    final short grammarOptions  =  stateCache.grammarOptions;
    
    final int n_elems = m_schema != null ? m_schema.getElemCountOfSchema() : 0;
    
    m_eventTypes = new AbstractEventType[3][];
    m_eventCodes = new EventCodeTuple[3];
    m_eventTypeLists = new ArrayEventTypeList[3];

    int i;
    for (i = 0; i < 3; i++) {
      m_eventTypeLists[i] = new ArrayEventTypeList();
    }
    
    boolean addTupleL2, addTupleL3;
    int n_eventTypes, n_items, n_itemsL2, n_itemsL3;
    boolean addDTD, addCM, addPI;
    AbstractEventType[] eventTypes;
    ArrayEventCodeTuple tuple, tupleL2, tupleL3;
    EventCode[] eventCodeItems, eventCodeItemsL2;
    
    eventTypes = new AbstractEventType[1];
    eventTypes[0] = new EventTypeStartDocument(this, m_eventTypeLists[0]);
    tuple = ArrayEventCodeTuple.createTuple(); 
    tuple.setItems(new AbstractEventType[] { eventTypes[0] });
    m_eventCodes[0] = tuple;
    m_eventTypes[0] = eventTypes;

    addTupleL2 = addTupleL3 = false;
    tupleL2 = tupleL3 = null;
    n_items = n_elems + 1;
    n_itemsL2 = n_itemsL3 = 0;
    if (addDTD = GrammarOptions.hasDTD(grammarOptions))
      ++n_itemsL2;
    if (addCM =  GrammarOptions.hasCM(grammarOptions))
      ++n_itemsL3;
    if (addPI =  GrammarOptions.hasPI(grammarOptions))
      ++n_itemsL3;
    if (n_itemsL3 != 0) {
      addTupleL3 = true;
      ++n_itemsL2;
    }
    if (n_itemsL2 != 0) {
      addTupleL2 = true;
      ++n_items;
    }
    m_elems = new int[n_elems];
    n_eventTypes = addTupleL3 ? n_itemsL3 + n_itemsL2 + n_items  - 2 :
      addTupleL2 ? n_itemsL2 + n_items - 1 : n_items;
    eventTypes = new AbstractEventType[n_eventTypes];
    eventCodeItems = new EventCode[n_items];
    eventCodeItemsL2 = null;
    EventCode[] eventCodeItemsL3 = null;
    int n;
    for (n = 0; n < n_elems; n++) {
      m_elems[n] = m_schema.getElemOfSchema(n);
      AbstractEventType[] events;
      events = createEventType(m_elems[n], n, n, this, m_eventTypeLists[1]);
      assert events.length == 1;
      eventCodeItems[n] = eventTypes[n] = events[0];
    }
    eventCodeItems[n] = eventTypes[n] = new EventTypeElementWildcard(EventCode.EVENT_CODE_DEPTH_ONE, this, m_eventTypeLists[1]);
    ++n;
    if (addTupleL2) {
      eventCodeItemsL2 = new EventCode[n_itemsL2];
      tupleL2 = ArrayEventCodeTuple.createTuple();
      eventCodeItems[n] = tupleL2;
      int m = 0;
      if (addDTD) {
        EventTypeDTD eventTypeDTD = new EventTypeDTD(this, m_eventTypeLists[1]); 
        eventTypes[n++] = eventTypeDTD;
        eventCodeItemsL2[m++] = eventTypeDTD;
      }
      if (addTupleL3) {
        eventCodeItemsL3 = new EventCode[n_itemsL3];
        tupleL3 = ArrayEventCodeTuple.createTuple();
        eventCodeItemsL2[m] = tupleL3;
        int k = 0;
        if (addCM) {
          final EventTypeComment comment;
          comment = new EventTypeComment(EventCode.EVENT_CODE_DEPTH_THREE, this, m_eventTypeLists[1]); 
          eventTypes[n++] = comment;
          eventCodeItemsL3[k++] = comment;
        }
        if (addPI) {
          final EventTypeProcessingInstruction pi;
          pi = new EventTypeProcessingInstruction(EventCode.EVENT_CODE_DEPTH_THREE, this, m_eventTypeLists[1]); 
          eventTypes[n++] = pi;
          eventCodeItemsL3[k++] = pi;
        }
      }
    }
    tuple = ArrayEventCodeTuple.createTuple();
    if (eventTypes.length > 0) {
      tuple.setItems(eventCodeItems);
      if (addTupleL2) {
        tupleL2.setItems(eventCodeItemsL2);
        if (addTupleL3) {
          tupleL3.setItems(eventCodeItemsL3);
        }
      }
    }
    m_eventCodes[1] = tuple;
    m_eventTypes[1] = eventTypes;
    
    addTupleL2 = false;
    tupleL2 = null;
    n_items = 1;
    n_itemsL2 = 0;
    if (addCM =  GrammarOptions.hasCM(grammarOptions))
      ++n_itemsL2;
    if (addPI =  GrammarOptions.hasPI(grammarOptions))
      ++n_itemsL2;
    if (n_itemsL2 != 0) {
      addTupleL2 = true;
      ++n_items;
    }
    n_eventTypes = addTupleL2 ? n_itemsL2 + n_items - 1 : n_items;
    eventTypes = new AbstractEventType[n_eventTypes];
    eventCodeItems = new EventCode[n_items];
    eventCodeItemsL2 = null;
    n = 0;
    eventCodeItems[n] = eventTypes[n] = new EventTypeEndDocument(this, m_eventTypeLists[2]);
    ++n;
    if (addTupleL2) {
      eventCodeItemsL2 = new EventCode[n_itemsL2];
      tupleL2 = ArrayEventCodeTuple.createTuple();
      eventCodeItems[n] = tupleL2;
      int m = 0;
      if (addCM) {
        final EventTypeComment comment;
        comment = new EventTypeComment(EventCode.EVENT_CODE_DEPTH_TWO, this, m_eventTypeLists[2]); 
        eventTypes[n++] = comment;
        eventCodeItemsL2[m++] = comment;
      }
      if (addPI) {
        final EventTypeProcessingInstruction pi;
        pi = new EventTypeProcessingInstruction(EventCode.EVENT_CODE_DEPTH_TWO, this, m_eventTypeLists[2]); 
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
//    tuple.setItems(new EventType[] { eventTypes[0] });
    m_eventCodes[2] = tuple;
    m_eventTypes[2] = eventTypes;
    
    for (i = 0; i < 3; i++) {
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
    stateVariables.phase = DOCUMENT_STATE_DEPLETE;
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
      case DOCUMENT_STATE_DEPLETE:
        return m_eventTypeLists[1];
      case DOCUMENT_STATE_COMPLETED:
        return m_eventTypeLists[2];
      case DOCUMENT_STATE_END:
        return new AbstractEventTypeList() {
          @Override
          public final int getLength() {
            return 0;
          }
          @Override
          public final EventType item(int i) {
            assert false;
            return null;
          }
          @Override
          public final EventType getSD() {
            return null;
          }
          @Override
          public final EventType getEE() {
            return null;
          }
          @Override
          public final EventTypeSchemaAttribute getSchemaAttribute(String uri, String name) {
            return null;
          }
          @Override
          public final EventTypeSchemaAttributeInvalid getSchemaAttributeInvalid(String uri, String name) {
            return null;
          }
          @Override
          public final EventTypeAttribute getAttribute(String uri, String name) {
            return null;
          }
          @Override
          public final EventType getSchemaAttributeWildcardAny() {
            return null;
          }
          @Override
          public final EventType getAttributeWildcardAnyUntyped() {
            return null;
          }
          @Override
          public final EventType getSchemaAttributeWildcardNS(String uri) {
            return null;
          }
          @Override
          public final EventType getSchemaCharacters() {
            return (EventType)null; 
          }
          @Override
          public final EventType getCharacters() {
            return (EventType)null; 
          }
          @Override
          public final EventType getNamespaceDeclaration() {
            return (EventType)null; 
          }
          @Override
          public final boolean isMutable() {
            return false;
          }
        };
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
  void element(int eventTypeIndex, String uri, String name, GrammarState stateVariables) { 
    if (stateVariables.phase ==  DOCUMENT_STATE_DEPLETE) {
      int elem = EXISchema.NIL_NODE;
      
      assert EventCode.ITEM_SCHEMA_SE == m_eventTypes[1][eventTypeIndex].itemType; 
      
      EventTypeSchema elementEventType = (EventTypeSchema)m_eventTypes[1][eventTypeIndex];
      elem = m_elems[elementEventType.index];
      
      m_grammarCache.retrieveElementGrammar(elem).init(stateVariables.documentGrammarState.pushState());
    }
    else {
      // REVISIT: report an error.
    }
  }

  @Override
  Grammar undeclaredElement(String uri, String name, GrammarState stateVariables) {
    if (stateVariables.phase ==  DOCUMENT_STATE_DEPLETE) {
      return super.undeclaredElement(uri, name, stateVariables);
    }
    else {
      // REVISIT: report an error.
      return null;
    }
  }

}
