package com.sumerogi.proc.grammars;

import com.sumerogi.proc.common.EventCode;
import com.sumerogi.proc.common.EventDescription;
import com.sumerogi.proc.common.EventType;
import com.sumerogi.proc.common.EventTypeList;
import com.sumerogi.proc.common.IGrammar;
import com.sumerogi.proc.common.StringTable;

final class BuiltinObjectGrammar extends BuiltinGrammar implements IGrammar {

  private final ReversedEventTypeList m_eventTypeList;
  private final ReverseEventCodeTuple m_eventCodes;
  
  private boolean dirty;
  StringTable stringTable;

  /**
   */
  BuiltinObjectGrammar(final GrammarCache grammarCache) {
    super(BUILTIN_GRAMMAR_OBJECT, grammarCache);
    
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
     * ObjectContent : 
     *   EO                    0      End Object
     *   SV (*) ObjectContent  1.0    String Value
     *   NV (*) ObjectContent  1.1    Number Value
     *   BV (*) ObjectContent  1.2    Boolean Value
     *   SO (*) ObjectContent  1.3.0  Start Object
     *   SA (*) ObjectContent  1.3.1  Start Array
     *   NL (*) ObjectContent  1.3.2  Null Value
     */
    final EventType undeclaredEO;  
    final EventType objectWildcard;
    final EventType arrayWildcard;
    final EventType stringValueWildcard;
    final EventType numberValueWildcard;
    final EventType booleanValueWildcard;
    final EventType nullValueWildcard;
    
    int n_itemsL3 = 0;

    nullValueWildcard = new EventType(EventCode.EVENT_CODE_DEPTH_THREE, m_eventTypeList, EventType.ITEM_NULL_WILDCARD, EventDescription.EVENT_NULL);
    m_eventTypeList.add(nullValueWildcard);
    arrayWildcard = new EventType(EventCode.EVENT_CODE_DEPTH_THREE, m_eventTypeList, EventType.ITEM_START_ARRAY_WILDCARD); 
    m_eventTypeList.add(arrayWildcard);
    objectWildcard = new EventType(EventCode.EVENT_CODE_DEPTH_THREE, m_eventTypeList, EventType.ITEM_START_OBJECT_WILDCARD); 
    m_eventTypeList.add(objectWildcard);
    n_itemsL3 += 3;

    int n_itemsL2 = 1; // accounting for tupleL3

    booleanValueWildcard = new EventType(EventCode.EVENT_CODE_DEPTH_TWO, m_eventTypeList, EventType.ITEM_BOOLEAN_VALUE_WILDCARD);
    m_eventTypeList.add(booleanValueWildcard);
    numberValueWildcard = new EventType(EventCode.EVENT_CODE_DEPTH_TWO, m_eventTypeList, EventType.ITEM_NUMBER_VALUE_WILDCARD);
    m_eventTypeList.add(numberValueWildcard);
    stringValueWildcard = new EventType(EventCode.EVENT_CODE_DEPTH_TWO, m_eventTypeList, EventType.ITEM_STRING_VALUE_WILDCARD);
    m_eventTypeList.add(stringValueWildcard);
    n_itemsL2 += 3;
    
    undeclaredEO = EventTypeFactory.creatEndObject(m_eventTypeList); 
    m_eventTypeList.add(undeclaredEO);
    
    EventCode[] eventCodeItemsL2 = null;
    EventCode[] eventCodeItemsL3 = null;
    ArrayEventCodeTuple tupleL2 = null;
    ArrayEventCodeTuple tupleL3 = null;
    eventCodeItemsL2 = new EventCode[n_itemsL2];
    tupleL2 = new ArrayEventCodeTuple();
    m_eventCodes.setInitialItems(undeclaredEO, tupleL2);
    eventCodeItemsL3 = new EventCode[n_itemsL3];
    tupleL3 = new ArrayEventCodeTuple();
    int m = 0;
    eventCodeItemsL2[m++] = stringValueWildcard;
    eventCodeItemsL2[m++] = numberValueWildcard;
    eventCodeItemsL2[m++] = booleanValueWildcard;
    eventCodeItemsL2[m++] = tupleL3;
    assert m == n_itemsL2;
    int k = 0;
    eventCodeItemsL3[k++] = objectWildcard;
    eventCodeItemsL3[k++] = arrayWildcard;
    eventCodeItemsL3[k++] = nullValueWildcard;
    
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
    assert eventType.itemType == EventType.ITEM_START_OBJECT_NAMED;
    final Grammar ensuingGrammar = ((EventTypeObject)eventType).objectGrammar;
    final GrammarState kid = stateVariables.apparatus.pushState();
    ensuingGrammar.init(kid);
    kid.name = eventType.getNameId();
    kid.distance = 0;
  }

  @Override
  public void startArrayNamed(EventType eventType, GrammarState stateVariables) {
    assert eventType.itemType == EventType.ITEM_START_ARRAY_NAMED;
    final Grammar ensuingGrammar = ((EventTypeArray)eventType).ensuingGrammar;
    final GrammarState kid = stateVariables.apparatus.pushState();
    ensuingGrammar.init(kid);
    kid.name = eventType.getNameId();
    kid.distance = 0;
  }

  @Override
  public final void endObject(GrammarState stateVariables) {
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
  public Grammar startObjectWildcard(int name, GrammarState stateVariables) {
    final Grammar ensuingGrammar;
    
    final GrammarState kid = stateVariables.apparatus.pushState();
    kid.name = name;
    kid.distance = 0;
    final Grammar objectGrammar;
    if ((objectGrammar = (Grammar)stringTable.localNameEntries[name].objectGrammars[0]) != null) {
      objectGrammar.init(kid);
      ensuingGrammar = objectGrammar;
    }
    else {
      final BuiltinObjectGrammar builtinObjectGrammar;
      builtinObjectGrammar = new BuiltinObjectGrammar(m_grammarCache); 
      builtinObjectGrammar.stringTable = stringTable;
      stringTable.setObjectGrammar(name, 0, builtinObjectGrammar);
      builtinObjectGrammar.init(kid);
      ensuingGrammar = builtinObjectGrammar;
    }
    
    final EventTypeObject eventTypeElement = new EventTypeObject(name, 
        stringTable.localNameEntries[name].localName, 
        m_eventTypeList, ensuingGrammar, (IGrammar)null); 
    m_eventTypeList.add(eventTypeElement);
    m_eventCodes.addItem(eventTypeElement);
    if (!dirty) {
      stringTable.addTouchedBuiltinGrammars(this);
      dirty = true;
    }
    return ensuingGrammar;
  }
  
  @Override
  public Grammar startArrayWildcard(int name, GrammarState stateVariables) {
    final Grammar ensuingGrammar;
    
    final GrammarState kid = stateVariables.apparatus.pushState();
    kid.name = name;
    kid.distance = 0;
    final Grammar arrayGrammar;
    if ((arrayGrammar = (Grammar)stringTable.localNameEntries[name].arrayGrammars[0]) != null) {
      arrayGrammar.init(kid);
      ensuingGrammar = arrayGrammar;
    }
    else {
      final BuiltinArrayGrammar builtinArrayGrammar;
      builtinArrayGrammar = new BuiltinArrayGrammar(m_grammarCache); 
      builtinArrayGrammar.stringTable = stringTable;
      stringTable.setArrayGrammar(name, 0, builtinArrayGrammar);
      builtinArrayGrammar.init(kid);
      ensuingGrammar = builtinArrayGrammar;
    }
    
    final EventTypeArray eventTypeArray = new EventTypeArray(name, 
        stringTable.localNameEntries[name].localName, 
        m_eventTypeList, ensuingGrammar, (IGrammar)null); 
    m_eventTypeList.add(eventTypeArray);
    m_eventCodes.addItem(eventTypeArray);
    if (!dirty) {
      stringTable.addTouchedBuiltinGrammars(this);
      dirty = true;
    }
    return ensuingGrammar;
    
  }
  
  private void wildcardValue(int eventTypeIndex, int nameId, byte itemType, byte eventKind) {
    assert m_eventTypeList.item(eventTypeIndex).getDepth() > EventCode.EVENT_CODE_DEPTH_ONE;
    final String name = stringTable.localNameEntries[nameId].localName;
    addEventType(new EventType(name, nameId, EventCode.EVENT_CODE_DEPTH_ONE, m_eventTypeList, itemType, eventKind));
  }
  
  private void addEventType(EventType eventType) {
    m_eventTypeList.add(eventType);
    m_eventCodes.addItem(eventType);
    if (!dirty) {
      stringTable.addTouchedBuiltinGrammars(this);
      dirty = true;
    }
  }

  @Override
  public void anonymousStringValue(EventType eventType, GrammarState stateVariables) {
    throw new IllegalStateException();
  }
  
  @Override
  public void wildcardStringValue(int eventTypeIndex, int nameId) {
    wildcardValue(eventTypeIndex, nameId, EventType.ITEM_STRING_VALUE_NAMED, EventDescription.EVENT_STRING_VALUE);
  }
  
  @Override
  public void anonymousNumberValue(EventType eventType, GrammarState stateVariables) {
    throw new IllegalStateException();
  }
  
  @Override
  public void wildcardNumberValue(int eventTypeIndex, int nameId) {
    wildcardValue(eventTypeIndex, nameId, EventType.ITEM_NUMBER_VALUE_NAMED, EventDescription.EVENT_NUMBER_VALUE);
  }
  
  @Override
  public void anonymousBooleanValue(EventType eventType, GrammarState stateVariables) {
    throw new IllegalStateException();
  }
  
  @Override
  public void wildcardBooleanValue(int eventTypeIndex, int nameId) {
    wildcardValue(eventTypeIndex, nameId, EventType.ITEM_BOOLEAN_VALUE_NAMED, EventDescription.EVENT_BOOLEAN_VALUE);
  }

  @Override
  public void anonymousNullValue(EventType eventType, GrammarState stateVariables) {
    throw new IllegalStateException();
  }
  
  @Override
  public void wildcardNullValue(int eventTypeIndex, int nameId) {
    addEventType(new EventTypeNull(nameId, stringTable.localNameEntries[nameId].localName, m_eventTypeList));
  }
  
}
