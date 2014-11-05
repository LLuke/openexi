package com.sumerogi.proc.grammars;

import com.sumerogi.proc.common.EventCode;
import com.sumerogi.proc.common.EventDescription;
import com.sumerogi.proc.common.EventType;
import com.sumerogi.proc.common.EventTypeList;
import com.sumerogi.proc.common.IGrammar;
import com.sumerogi.proc.common.StringTable;

final class BuiltinArrayGrammar extends BuiltinGrammar implements IGrammar {

  private final ReversedEventTypeList m_eventTypeList;
  private final ReverseEventCodeTuple m_eventCodes;
  
  private boolean dirty;
  StringTable stringTable;

  /**
   */
  BuiltinArrayGrammar(final GrammarCache grammarCache) {
    super(BUILTIN_GRAMMAR_ARRAY, grammarCache);
    
    m_eventTypeList = new ReversedEventTypeList();
    m_eventCodes = new ReverseEventCodeTuple();
    populateContentGrammar();
    
    m_eventTypeList.checkPoint();
    m_eventCodes.checkPoint();
  }
  
  @Override
  public void init(GrammarState stateVariables) {
    stateVariables.targetGrammar = this;
  }

  private void populateContentGrammar() {
    /*
     * ArrayContent : 
     *   EA               0      End Array
     *   SO ArrayContent  1.0    Start Object
     *   SA ArrayContent  1.1    Start Array
     *   SV ArrayContent  1.2.0  String Value
     *   NV ArrayContent  1.2.1  Number Value
     *   BV ArrayContent  1.2.2  Boolean Value
     *   NL ArrayContent  1.2.3  Null Value
     */
    final EventType undeclaredEA;
    final EventType objectAnonymous;
    final EventType arrayAnonymous;
    final EventType stringValueAnonymous;
    final EventType numberValueAnonymous;
    final EventType booleanValueAnonymous;
    final EventType nullValueAnonymous;
    
    int n_itemsL3 = 0;

    nullValueAnonymous = new EventTypeNullAnonymous(EventCode.EVENT_CODE_DEPTH_THREE, m_eventTypeList); 
    m_eventTypeList.add(nullValueAnonymous);
    booleanValueAnonymous = new EventType(EventCode.EVENT_CODE_DEPTH_THREE, m_eventTypeList, EventType.ITEM_BOOLEAN_VALUE_ANONYMOUS);
    m_eventTypeList.add(booleanValueAnonymous);
    numberValueAnonymous = new EventType(EventCode.EVENT_CODE_DEPTH_THREE, m_eventTypeList, EventType.ITEM_NUMBER_VALUE_ANONYMOUS);
    m_eventTypeList.add(numberValueAnonymous);
    stringValueAnonymous = new EventType(EventCode.EVENT_CODE_DEPTH_THREE, m_eventTypeList, EventType.ITEM_STRING_VALUE_ANONYMOUS);
    m_eventTypeList.add(stringValueAnonymous);
    n_itemsL3 += 4;

    int n_itemsL2 = 1; // accounting for tupleL3

    arrayAnonymous = new EventType(EventCode.EVENT_CODE_DEPTH_TWO, m_eventTypeList, EventType.ITEM_START_ARRAY_ANONYMOUS, EventDescription.EVENT_START_ARRAY); 
    m_eventTypeList.add(arrayAnonymous);
    objectAnonymous = new EventType(EventCode.EVENT_CODE_DEPTH_TWO, m_eventTypeList, EventType.ITEM_START_OBJECT_ANONYMOUS, EventDescription.EVENT_START_OBJECT); 
    m_eventTypeList.add(objectAnonymous);
    n_itemsL2 += 2;

    undeclaredEA = EventTypeFactory.creatEndArray(m_eventTypeList); 
    m_eventTypeList.add(undeclaredEA);
    
    EventCode[] eventCodeItemsL2 = null;
    EventCode[] eventCodeItemsL3 = null;
    ArrayEventCodeTuple tupleL2 = null;
    ArrayEventCodeTuple tupleL3 = null;
    eventCodeItemsL2 = new EventCode[n_itemsL2];
    tupleL2 = new ArrayEventCodeTuple();
    m_eventCodes.setInitialItems(undeclaredEA, tupleL2);
    eventCodeItemsL3 = new EventCode[n_itemsL3];
    tupleL3 = new ArrayEventCodeTuple();
    int m = 0;
    eventCodeItemsL2[m++] = objectAnonymous;
    eventCodeItemsL2[m++] = arrayAnonymous;
    eventCodeItemsL2[m++] = tupleL3;
    assert m == n_itemsL2;
    int k = 0;
    eventCodeItemsL3[k++] = stringValueAnonymous;
    eventCodeItemsL3[k++] = numberValueAnonymous;
    eventCodeItemsL3[k++] = booleanValueAnonymous;
    eventCodeItemsL3[k++] = nullValueAnonymous;
    
    tupleL2.setItems(eventCodeItemsL2);
    tupleL3.setItems(eventCodeItemsL3);
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Implementation of IGrammar (used by StringTable)
  ///////////////////////////////////////////////////////////////////////////

  public void reset() {
    if (dirty) {
      m_eventTypeList.reset();
      m_eventCodes.reset();
      dirty = false;
    }
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Method implementations for event processing
  ///////////////////////////////////////////////////////////////////////////

  @Override
  public void startObjectNamed(EventType eventType, GrammarState stateVariables) {
    throw new IllegalStateException();
//    assert eventType.itemType == EventType.ITEM_START_OBJECT_NAMED;
//    final Grammar ensuingGrammar = ((EventTypeObject)eventType).ensuingGrammar;
//    final GrammarState kid = stateVariables.apparatus.pushState();
//    ensuingGrammar.init(kid);
//    kid.name = eventType.getNameId();
//    kid.distance = 0;
  }

  @Override
  public final void endObject(GrammarState stateVariables) {
  }
  
  @Override
  public void startArrayNamed(EventType eventType, GrammarState stateVariables) {
    throw new IllegalStateException();
  }

  @Override
  public final void endArray(GrammarState stateVariables) {
  }
  
  @Override
  public EventCodeTuple getNextEventCodes(GrammarState stateVariables) {
    return m_eventCodes;
  }

  @Override
  public EventTypeList getNextEventTypes(GrammarState stateVariables) {
    return m_eventTypeList;
  }

  @Override
  public final Grammar startObjectAnonymous(EventType eventType, GrammarState stateVariables) {
    final int depth = eventType.getDepth();
    assert depth == EventCode.EVENT_CODE_DEPTH_TWO || depth == EventCode.EVENT_CODE_DEPTH_ONE; 
    final Grammar objectGrammar;
    if (depth == EventCode.EVENT_CODE_DEPTH_ONE) {
      objectGrammar = ((EventTypeObject)eventType).objectGrammar;
      final GrammarState kid = stateVariables.apparatus.pushState();
      objectGrammar.init(kid);
      kid.name = stateVariables.name;
      kid.distance = stateVariables.distance + 1;
    }
    else {
      objectGrammar = super.startObjectAnonymous(eventType, stateVariables);
      final EventTypeObject eventTypeObject = new EventTypeObject( 
          m_eventTypeList, objectGrammar, (IGrammar)null); 
      m_eventTypeList.add(eventTypeObject);
      m_eventCodes.addItem(eventTypeObject);
      if (!dirty) {
        stringTable.addTouchedBuiltinGrammars(this);
        dirty = true;
      }
    }
    return objectGrammar;
  }
  
  @Override
  public Grammar startObjectWildcard(int nameId, GrammarState stateVariables) {
    throw new IllegalStateException();
  }
  
  @Override
  public Grammar startArrayWildcard(int nameId, GrammarState stateVariables) {
    throw new IllegalStateException();
  }
  
  private void anonymousValue(EventType eventType, byte itemType, byte eventKind) {
    final int depth = eventType.getDepth();
    assert depth == EventCode.EVENT_CODE_DEPTH_THREE || depth == EventCode.EVENT_CODE_DEPTH_ONE; 
    if (depth == EventCode.EVENT_CODE_DEPTH_THREE) {
      final EventType eventTypeStringValue = itemType == EventType.ITEM_NULL_ANONYMOUS ?
          new EventTypeNullAnonymous(EventCode.EVENT_CODE_DEPTH_ONE, m_eventTypeList) :
          new EventType(EventCode.EVENT_CODE_DEPTH_ONE, m_eventTypeList, itemType, eventKind);
      m_eventTypeList.add(eventTypeStringValue);
      m_eventCodes.addItem(eventTypeStringValue);
      if (!dirty) {
        stringTable.addTouchedBuiltinGrammars(this);
        dirty = true;
      }
    }
  }

  @Override
  public void anonymousStringValue(EventType eventType, GrammarState stateVariables) {
    anonymousValue(eventType, EventType.ITEM_STRING_VALUE_ANONYMOUS, EventDescription.EVENT_STRING_VALUE);
  }

  @Override
  public void anonymousNumberValue(EventType eventType, GrammarState stateVariables) {
    anonymousValue(eventType, EventType.ITEM_NUMBER_VALUE_ANONYMOUS, EventDescription.EVENT_NUMBER_VALUE);
  }

  @Override
  public void anonymousNullValue(EventType eventType, GrammarState stateVariables) {
    anonymousValue(eventType, EventType.ITEM_NULL_ANONYMOUS, EventDescription.EVENT_NULL);
  }

  @Override
  public void anonymousBooleanValue(EventType eventType, GrammarState stateVariables) {
    anonymousValue(eventType, EventType.ITEM_BOOLEAN_VALUE_ANONYMOUS, EventDescription.EVENT_BOOLEAN_VALUE);
  }

  @Override
  public void wildcardStringValue(int eventTypeIndex, int nameId) {
    throw new IllegalStateException();
  }
  
  @Override
  public void wildcardNumberValue(int eventTypeIndex, int nameId) {
//    wildcardValue(eventTypeIndex, nameId, EventType.ITEM_NV_NAMED, EventDescription.EVENT_NV);
  }
  
  @Override
  public void wildcardBooleanValue(int eventTypeIndex, int nameId) {
//    wildcardValue(eventTypeIndex, nameId, EventType.ITEM_BV_NAMED, EventDescription.EVENT_BV);
  }

  @Override
  public void wildcardNullValue(int eventTypeIndex, int nameId) {
//    wildcardValue(eventTypeIndex, nameId, EventType.ITEM_NL_NAMED, EventDescription.EVENT_NL);
  }

  
}
