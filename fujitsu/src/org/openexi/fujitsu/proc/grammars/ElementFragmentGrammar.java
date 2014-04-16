package org.openexi.fujitsu.proc.grammars;

import java.util.ArrayList;

import org.openexi.fujitsu.proc.common.EventCode;
import org.openexi.fujitsu.proc.common.EventTypeList;
import org.openexi.fujitsu.proc.common.GrammarOptions;
import org.openexi.fujitsu.schema.EXISchema;

final class ElementFragmentGrammar extends SchemaInformedGrammar {

  ///////////////////////////////////////////////////////////////////////////
  /// immutables
  ///////////////////////////////////////////////////////////////////////////
  
  private final int[] m_fragmentINodes;
  private final AbstractEventType[][] m_eventTypes;
  private final EventCodeTuple[] m_eventCodes;
  private final ArrayEventTypeList[] m_eventTypeLists;
  
  ElementFragmentGrammar(GrammarCache stateCache) {
    super(EXISchema.NIL_NODE, SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, stateCache);

    m_fragmentINodes = m_schema.getFragmentINodes();
    final int n_fragmentElems = m_schema.getFragmentElemCount();
    final int n_fragmentAttrs = m_fragmentINodes.length - n_fragmentElems;
    
    m_eventTypes = new AbstractEventType[4][];
    m_eventCodes = new EventCodeTuple[4];
    m_eventTypeLists = new ArrayEventTypeList[4];

    int i;
    for (i = 0; i < 4; i++) {
      m_eventTypeLists[i] = new ArrayEventTypeList();
    }
    
    final boolean addUndeclaredEA = GrammarOptions.hasUndeclaredEA(stateCache.grammarOptions);
    
    ArrayList<AbstractEventType> eventTypeList;
    EventTypeSchema[] eventTypes;
    int serial;
    EventCodeTupleSink res;
    ArrayList<EventTypeSchemaAttributeInvalid> invalidAttributes = null;
    EventTypeSchema eventType;

    eventTypeList = new ArrayList<AbstractEventType>();
    if (addUndeclaredEA)
      invalidAttributes = new ArrayList<EventTypeSchemaAttributeInvalid>();
    for (i = 0, serial = 0; i < n_fragmentAttrs; i++, serial++) {
      final int ind = n_fragmentElems + i;
      final int inode = m_fragmentINodes[ind];
      eventTypes = createEventType(inode, ind, serial, this, m_eventTypeLists[0]);
      assert eventTypes.length == 1;
      eventType = eventTypes[0];
      eventTypeList.add(eventType);
      if (addUndeclaredEA)
        invalidAttributes.add(new EventTypeSchemaAttributeInvalid((EventTypeSchemaAttribute)eventType, this, m_eventTypeLists[0]));
    }
    eventTypeList.add(EventTypeSchemaAttributeWildcardAny.createLevelOne(this, m_eventTypeLists[0]));
    ++serial;
    for (i = 0; i < n_fragmentElems; i++, serial++) {
      final int inode = m_fragmentINodes[i];
      eventTypes = createEventType(inode, i, serial, this, m_eventTypeLists[0]);
      assert eventTypes.length == 1;
      eventTypeList.add(eventTypes[0]);
    }
    eventTypeList.add(new EventTypeElementWildcard(EventCode.EVENT_CODE_DEPTH_ONE, this, m_eventTypeLists[0]));
    ++serial;
    eventTypeList.add(new EventTypeSchemaEndElement(serial, this, m_eventTypeLists[0]));
    ++serial;
    eventTypeList.add(new EventTypeCharactersSecond(EventCode.EVENT_CODE_DEPTH_ONE, this, m_eventTypeLists[0]));

    createEventCodeTuple(eventTypeList, stateCache.grammarOptions, res = new EventCodeTupleSink(), invalidAttributes, m_eventTypeLists[0], true);

    m_eventCodes[0] = res.eventCodeTuple;
    m_eventTypes[0] = res.eventTypes;
    m_eventTypeLists[0].setItems(res.eventTypes);

    
    eventTypeList = new ArrayList<AbstractEventType>();
    for (i = 0, serial = 0; i < n_fragmentElems; i++, serial++) {
      final int inode = m_fragmentINodes[i];
      eventTypes = createEventType(inode, i, serial, this, m_eventTypeLists[1]);
      assert eventTypes.length == 1;
      eventTypeList.add(eventTypes[0]);
    }
    eventTypeList.add(new EventTypeElementWildcard(EventCode.EVENT_CODE_DEPTH_ONE, this, m_eventTypeLists[1]));
    ++serial;
    eventTypeList.add(new EventTypeSchemaEndElement(serial, this, m_eventTypeLists[1]));
    ++serial;
    eventTypeList.add(new EventTypeCharactersSecond(EventCode.EVENT_CODE_DEPTH_ONE, this, m_eventTypeLists[1]));

    createEventCodeTuple(eventTypeList, stateCache.grammarOptions, res = new EventCodeTupleSink(), m_eventTypeLists[1]);

    m_eventCodes[1] = res.eventCodeTuple;
    m_eventTypes[1] = res.eventTypes;
    m_eventTypeLists[1].setItems(res.eventTypes);

    
    eventTypeList = new ArrayList<AbstractEventType>();
    if (addUndeclaredEA)
      invalidAttributes.clear();
    for (i = 0, serial = 0; i < n_fragmentAttrs; i++, serial++) {
      final int ind = n_fragmentElems + i;
      final int inode = m_fragmentINodes[ind];
      eventTypes = createEventType(inode, ind, serial, this, m_eventTypeLists[2]);
      assert eventTypes.length == 1;
      eventType = eventTypes[0];
      eventTypeList.add(eventType);
      if (addUndeclaredEA)
        invalidAttributes.add(new EventTypeSchemaAttributeInvalid((EventTypeSchemaAttribute)eventType, this, m_eventTypeLists[2]));
    }
    eventTypeList.add(EventTypeSchemaAttributeWildcardAny.createLevelOne(this, m_eventTypeLists[2]));
    ++serial;
    eventTypeList.add(new EventTypeSchemaEndElement(serial, this, m_eventTypeLists[2]));

    // For strict grammars, use the atZero param value of "false" to avoid getting xsi:type and xsi:nil.
    createEventCodeTuple(eventTypeList, stateCache.grammarOptions, res = new EventCodeTupleSink(), invalidAttributes, m_eventTypeLists[2],
        (stateCache.grammarOptions & GrammarOptions.STRICT_OPTIONS) == 0);

    m_eventCodes[2] = res.eventCodeTuple;
    m_eventTypes[2] = res.eventTypes;
    m_eventTypeLists[2].setItems(res.eventTypes);

    
    eventTypeList = new ArrayList<AbstractEventType>();
    eventTypeList.add(new EventTypeSchemaEndElement(0, this, m_eventTypeLists[3]));

    createEventCodeTuple(eventTypeList, stateCache.grammarOptions, res = new EventCodeTupleSink(), m_eventTypeLists[3]);

    m_eventCodes[3] = res.eventCodeTuple;
    m_eventTypes[3] = res.eventTypes;
    m_eventTypeLists[3].setItems(res.eventTypes);
  }
  
  @Override
  public void init(GrammarState stateVariables) {
    super.init(stateVariables);
    stateVariables.phase = ELEMENT_FRAGMENT_STATE_TAG;
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Implementation of abstract methods declared in CommonState
  ///////////////////////////////////////////////////////////////////////////

  @Override
  void xsitp(int tp, GrammarState stateVariables) {
    assert stateVariables.phase == ELEMENT_FRAGMENT_STATE_TAG;
    assert (m_schema.getNodeType(tp) & EXISchema.TYPE_MASK) == EXISchema.TYPE_MASK;
    final GrammarState elementTagStateVariables = stateVariables.documentGrammarState.pushState(); 
    stateVariables.elementTagStateVariables = elementTagStateVariables;
    stateVariables.phase = ELEMENT_FRAGMENT_BOUND;
    m_grammarCache.retrieveElementTagGrammar(tp).init(elementTagStateVariables);
  }
  
  @Override
  void nillify(GrammarState stateVariables) {
    assert stateVariables.phase == ELEMENT_FRAGMENT_STATE_TAG;
    stateVariables.phase = ELEMENT_FRAGMENT_EMPTY_STATE_TAG;
  }
  
  @Override
  EventTypeList getNextEventTypes(GrammarState stateVariables) {
    assert stateVariables.phase < ELEMENT_FRAGMENT_BOUND;
    return m_eventTypeLists[stateVariables.phase - ELEMENT_FRAGMENT_STATE_BASE];
  }
  
  @Override
  EventCodeTuple getNextEventCodes(GrammarState stateVariables) {
    assert stateVariables.phase < ELEMENT_FRAGMENT_BOUND;
    return m_eventCodes[stateVariables.phase - ELEMENT_FRAGMENT_STATE_BASE];
  }

  @Override
  void element(int eventTypeIndex, String uri, String name, GrammarState stateVariables) {
    assert stateVariables.phase < ELEMENT_FRAGMENT_BOUND;

    GrammarState kid = null;
    int elemFragment = EXISchema.NIL_NODE;
    
    assert EventCode.ITEM_SCHEMA_SE == m_eventTypes[stateVariables.phase - ELEMENT_FRAGMENT_STATE_BASE][eventTypeIndex].itemType; 
    
    EventTypeSchema elementEventType = (EventTypeSchema)m_eventTypes[stateVariables.phase - ELEMENT_FRAGMENT_STATE_BASE][eventTypeIndex];
    elemFragment = m_fragmentINodes[elementEventType.index];
    
    kid = stateVariables.documentGrammarState.pushState();
    m_grammarCache.retrieveElementFragmentGrammar(elemFragment).init(kid);

    switch (stateVariables.phase) {
      case ELEMENT_FRAGMENT_STATE_TAG:
        stateVariables.phase = ELEMENT_FRAGMENT_STATE_CONTENT;
        break;
      case ELEMENT_FRAGMENT_EMPTY_STATE_TAG:
        stateVariables.phase = ELEMENT_FRAGMENT_EMPTY_STATE_CONTENT;
        break;
      default:
        break;
    }
  }
  
  @Override
  Grammar undeclaredElement(final String uri, final String name, final GrammarState stateVariables) {
    switch (stateVariables.phase) {
      case ELEMENT_FRAGMENT_STATE_TAG:
        stateVariables.phase = ELEMENT_FRAGMENT_STATE_CONTENT;
        break;
      case ELEMENT_FRAGMENT_EMPTY_STATE_TAG:
        stateVariables.phase = ELEMENT_FRAGMENT_EMPTY_STATE_CONTENT;
        break;
    }    
    return super.undeclaredElement(uri, name, stateVariables);
  }
  
  @Override
  void schemaAttribute(int eventTypeIndex, String uri, String name, GrammarState stateVariables) {
    assert stateVariables.phase == ELEMENT_FRAGMENT_STATE_TAG || stateVariables.phase == ELEMENT_FRAGMENT_EMPTY_STATE_TAG;
  }
  
  @Override
  public void chars(GrammarState stateVariables) {
    throw new IllegalStateException("char() cannot be invoked on an element fragment grammar.");
  }

  @Override
  final public void undeclaredChars(GrammarState stateVariables) {
    switch (stateVariables.phase) {
      case ELEMENT_FRAGMENT_STATE_TAG:
        stateVariables.phase = ELEMENT_FRAGMENT_STATE_CONTENT;
        break;
      case ELEMENT_FRAGMENT_EMPTY_STATE_TAG:
        stateVariables.phase = ELEMENT_FRAGMENT_EMPTY_STATE_CONTENT;
        break;
      case ELEMENT_FRAGMENT_STATE_CONTENT:
      case ELEMENT_FRAGMENT_EMPTY_STATE_CONTENT:
        break;
      default:
        assert false;
        break;
    }
  }
  
  @Override
  public void miscContent(GrammarState stateVariables) {
    undeclaredChars(stateVariables);
  }

  @Override
  void done(GrammarState kid, GrammarState stateVariables) {
    if (stateVariables.phase == ELEMENT_FRAGMENT_BOUND) {
      finish(stateVariables);
    }
  }

  @Override
  void end(String uri, String name, GrammarState stateVariables) {
    finish(stateVariables);
  }

}
