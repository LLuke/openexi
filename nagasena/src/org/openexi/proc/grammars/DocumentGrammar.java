package org.openexi.proc.grammars;

import org.openexi.proc.common.EventCode;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventTypeList;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.common.IGrammar;

final class DocumentGrammar extends Grammar {

  ///////////////////////////////////////////////////////////////////////////
  /// immutables
  ///////////////////////////////////////////////////////////////////////////

  private final int[] m_elems;
  private final EventType[][] m_eventTypes;
  private final EventCodeTuple[] m_eventCodes;
  private final ArrayEventTypeList[] m_eventTypeLists;

  ///////////////////////////////////////////////////////////////////////////
  /// containers with variable contents
  ///////////////////////////////////////////////////////////////////////////

  ///////////////////////////////////////////////////////////////////////////
  /// Constructors, initializers
  ///////////////////////////////////////////////////////////////////////////

  DocumentGrammar(GrammarCache grammarCache) {
    super(SCHEMA_GRAMMAR_DOCUMENT, grammarCache);

    final short grammarOptions  =  grammarCache.grammarOptions;
    
    final int n_elems = schema != null ? schema.getGlobalElemCountOfSchema() : 0;
    
    m_eventTypes = new EventType[3][];
    m_eventCodes = new EventCodeTuple[3];
    m_eventTypeLists = new ArrayEventTypeList[3];

    int i;
    for (i = 0; i < 3; i++) {
      m_eventTypeLists[i] = new ArrayEventTypeList();
    }
    
    boolean addTupleL2, addTupleL3;
    int n_eventTypes, n_items, n_itemsL2, n_itemsL3;
    boolean addDTD, addCM, addPI;
    EventType[] eventTypes;
    ArrayEventCodeTuple tuple, tupleL2, tupleL3;
    EventCode[] eventCodeItems, eventCodeItemsL2;
    
    eventTypes = new EventType[1];
    eventTypes[0] = EventTypeFactory.createStartDocument(m_eventTypeLists[0]);
    tuple = new ArrayEventCodeTuple(); 
    tuple.setItems(new EventType[] { eventTypes[0] });
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
    eventTypes = new EventType[n_eventTypes];
    eventCodeItems = new EventCode[n_items];
    eventCodeItemsL2 = null;
    EventCode[] eventCodeItemsL3 = null;
    int n;
    for (n = 0; n < n_elems; n++) {
      m_elems[n] = schema.getGlobalElemOfSchema(n);
      final int elem = m_elems[n];
      final EXIGrammarUse ensuingGrammar = grammarCache.exiGrammarUses[schema.getSerialOfElem(elem)];
      final EventType event;
      final int uriId = schema.getUriOfElem(elem);
      final int localNameId = schema.getLocalNameOfElem(elem);
      event = EventTypeFactory.createStartElement(
          uriId, localNameId, schema.uris[uriId], schema.localNames[uriId][localNameId],
          m_eventTypeLists[1], ensuingGrammar, (EXIGrammar)null);
      eventCodeItems[n] = eventTypes[n] = event;
    }
    eventCodeItems[n] = eventTypes[n] = new EventType(EventCode.EVENT_CODE_DEPTH_ONE, m_eventTypeLists[1], EventType.ITEM_SE_WC, (IGrammar)null);
    ++n;
    if (addTupleL2) {
      eventCodeItemsL2 = new EventCode[n_itemsL2];
      tupleL2 = new ArrayEventCodeTuple();
      eventCodeItems[n] = tupleL2;
      int m = 0;
      if (addDTD) {
        EventType eventTypeDTD = new EventType(EventCode.EVENT_CODE_DEPTH_TWO, m_eventTypeLists[1], EventType.ITEM_DTD, (IGrammar)null);
        eventTypes[n++] = eventTypeDTD;
        eventCodeItemsL2[m++] = eventTypeDTD;
      }
      if (addTupleL3) {
        eventCodeItemsL3 = new EventCode[n_itemsL3];
        tupleL3 = new ArrayEventCodeTuple();
        eventCodeItemsL2[m] = tupleL3;
        int k = 0;
        if (addCM) {
          final EventType comment;
          comment = new EventType(EventCode.EVENT_CODE_DEPTH_THREE, m_eventTypeLists[1], EventType.ITEM_CM, (IGrammar)null);
          eventTypes[n++] = comment;
          eventCodeItemsL3[k++] = comment;
        }
        if (addPI) {
          final EventType pi;
          pi = new EventType(EventCode.EVENT_CODE_DEPTH_THREE, m_eventTypeLists[1], EventType.ITEM_PI, (IGrammar)null);
          eventTypes[n++] = pi;
          eventCodeItemsL3[k++] = pi;
        }
      }
    }
    tuple = new ArrayEventCodeTuple();
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
    eventTypes = new EventType[n_eventTypes];
    eventCodeItems = new EventCode[n_items];
    eventCodeItemsL2 = null;
    n = 0;
    eventCodeItems[n] = eventTypes[n] = EventTypeFactory.createEndDocument(m_eventTypeLists[2]);
    ++n;
    if (addTupleL2) {
      eventCodeItemsL2 = new EventCode[n_itemsL2];
      tupleL2 = new ArrayEventCodeTuple();
      eventCodeItems[n] = tupleL2;
      int m = 0;
      if (addCM) {
        final EventType comment;
        comment = new EventType(EventCode.EVENT_CODE_DEPTH_TWO, m_eventTypeLists[2], EventType.ITEM_CM, (IGrammar)null);
        eventTypes[n++] = comment;
        eventCodeItemsL2[m++] = comment;
      }
      if (addPI) {
        final EventType pi;
        pi = new EventType(EventCode.EVENT_CODE_DEPTH_TWO, m_eventTypeLists[2], EventType.ITEM_PI, (IGrammar)null);
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
//    tuple.setItems(new EventType[] { eventTypes[0] });
    m_eventCodes[2] = tuple;
    m_eventTypes[2] = eventTypes;
    
    for (i = 0; i < 3; i++) {
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
    stateVariables.phase = DOCUMENT_STATE_DEPLETE;
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
        return new EventTypeList(false) {
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
          public final EventTypeSchema getSchemaAttribute(String uri, String name) {
            return null;
          }
          @Override
          public final EventTypeSchema getSchemaAttributeInvalid(String uri, String name) {
            return null;
          }
          @Override
          public final EventType getLearnedAttribute(String uri, String name) {
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
  public void element(EventType eventType, GrammarState stateVariables) { 
    assert stateVariables.phase ==  DOCUMENT_STATE_DEPLETE;
    stateVariables.phase = DOCUMENT_STATE_COMPLETED;
    (((EventTypeElement)eventType).ensuingGrammar).init(stateVariables.apparatus.pushState());
  }

  @Override
  public Grammar wildcardElement(int eventTypeIndex, int uriId, int localNameId, GrammarState stateVariables) {
    assert stateVariables.phase ==  DOCUMENT_STATE_DEPLETE; 
    stateVariables.phase = DOCUMENT_STATE_COMPLETED;
    return super.wildcardElement(eventTypeIndex, uriId, localNameId, stateVariables);
  }

}
