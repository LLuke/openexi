package org.openexi.proc.grammars;

import java.util.ArrayList;

import org.openexi.proc.common.EventCode;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventTypeList;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.common.IGrammar;
import org.openexi.schema.EXISchema;

final class ElementFragmentGrammar extends SchemaInformedGrammar {

  ///////////////////////////////////////////////////////////////////////////
  /// immutables
  ///////////////////////////////////////////////////////////////////////////
  
  private final int[] m_fragmentINodes;
  private final EventType[][] m_eventTypes;
  private final EventCodeTuple[] m_eventCodes;
  private final ArrayEventTypeList[] m_eventTypeLists;
  
  ElementFragmentGrammar(GrammarCache grammarCache) {
    super(SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarCache);

    m_fragmentINodes = schema.getFragmentINodes();
    final int n_fragmentElems = schema.getFragmentElemCount();
    final int n_fragmentAttrs = m_fragmentINodes.length - n_fragmentElems;
    
    m_eventTypes = new EventType[4][];
    m_eventCodes = new EventCodeTuple[4];
    m_eventTypeLists = new ArrayEventTypeList[4];

    int i;
    for (i = 0; i < 4; i++) {
      m_eventTypeLists[i] = new ArrayEventTypeList();
    }
    
    final boolean addUndeclaredEA = GrammarOptions.isPermitDeviation(grammarCache.grammarOptions);
    
    ArrayList<EventType> eventTypeList;
    EventCodeTupleSink res;
    ArrayList<EventType> invalidAttributes = null;
    EventTypeSchema eventType;

    eventTypeList = new ArrayList<EventType>();
    if (addUndeclaredEA)
      invalidAttributes = new ArrayList<EventType>();
    for (i = 0; i < n_fragmentAttrs; i++) {
      final int ind = n_fragmentElems + i;
      final boolean useRelaxedGrammar;
      int inode;
      if (useRelaxedGrammar = ((inode = m_fragmentINodes[ind]) & 0x80000000) != 0)
        inode = ~inode;
      eventType = createAttributeEventType(inode, useRelaxedGrammar, m_eventTypeLists[0]);
      eventTypeList.add(eventType);
      if (addUndeclaredEA)
        invalidAttributes.add(createEventTypeSchemaAttributeInvalid(eventType, m_eventTypeLists[0]));
    }
    eventTypeList.add(new EventType(EventCode.EVENT_CODE_DEPTH_ONE, m_eventTypeLists[0], EventType.ITEM_SCHEMA_AT_WC_ANY, (IGrammar)null));
    for (i = 0; i < n_fragmentElems; i++) {
      final EXIGrammarUse subsequentGrammar;
      int elem;
      if (((elem = m_fragmentINodes[i]) & 0x80000000) != 0) {
        elem = ~elem;
        subsequentGrammar = null;
      }
      else {
        // use a specific grammar
        subsequentGrammar = grammarCache.exiGrammarUses[schema.getSerialOfElem(elem)];
      }
      final int uriId = schema.getUriOfElem(elem);
      final int localNameId = schema.getLocalNameOfElem(elem); 
      EventTypeElement eventType2 = EventTypeFactory.createStartElement(
          uriId, localNameId, schema.uris[uriId], schema.localNames[uriId][localNameId],
          m_eventTypeLists[0], subsequentGrammar);
      eventTypeList.add(eventType2);
    }
    eventTypeList.add(new EventType(EventCode.EVENT_CODE_DEPTH_ONE, m_eventTypeLists[0], EventType.ITEM_SE_WC, (IGrammar)null));
    eventTypeList.add(EventTypeFactory.creatEndElement(EventCode.EVENT_CODE_DEPTH_ONE, m_eventTypeLists[0]));
    eventTypeList.add(new EventType(EventCode.EVENT_CODE_DEPTH_ONE, m_eventTypeLists[0], EventType.ITEM_CH, (IGrammar)null));
    
    createEventCodeTuple(eventTypeList, grammarCache.grammarOptions, res = new EventCodeTupleSink(), invalidAttributes, m_eventTypeLists[0], true, EXISchema.NIL_GRAM, false, -1, -1);

    m_eventCodes[0] = res.eventCodeTuple;
    m_eventTypes[0] = res.eventTypes;
    m_eventTypeLists[0].setItems(res.eventTypes);

    
    eventTypeList = new ArrayList<EventType>();
    for (i = 0; i < n_fragmentElems; i++) {
      final EXIGrammarUse ensuingGrammar;
      int elem;
      if (((elem = m_fragmentINodes[i]) & 0x80000000) != 0) {
        elem = ~elem;
        ensuingGrammar = null;
      }
      else {
        // use a specific grammar
        ensuingGrammar = grammarCache.exiGrammarUses[schema.getSerialOfElem(elem)];
      }
      final int uriId = schema.getUriOfElem(elem);
      final int localNameId = schema.getLocalNameOfElem(elem);
      EventTypeElement eventType2 = EventTypeFactory.createStartElement(
          uriId, localNameId, schema.uris[uriId], schema.localNames[uriId][localNameId],
          m_eventTypeLists[1], ensuingGrammar);
      eventTypeList.add(eventType2);
    }
    eventTypeList.add(new EventType(EventCode.EVENT_CODE_DEPTH_ONE, m_eventTypeLists[1], EventType.ITEM_SE_WC, (IGrammar)null));
    eventTypeList.add(EventTypeFactory.creatEndElement(EventCode.EVENT_CODE_DEPTH_ONE, m_eventTypeLists[1]));
    eventTypeList.add(new EventType(EventCode.EVENT_CODE_DEPTH_ONE, m_eventTypeLists[1], EventType.ITEM_CH, (IGrammar)null));
    
    createEventCodeTuple(eventTypeList, grammarCache.grammarOptions, res = new EventCodeTupleSink(), m_eventTypeLists[1]);

    m_eventCodes[1] = res.eventCodeTuple;
    m_eventTypes[1] = res.eventTypes;
    m_eventTypeLists[1].setItems(res.eventTypes);

    
    eventTypeList = new ArrayList<EventType>();
    if (addUndeclaredEA)
      invalidAttributes.clear();
    for (i = 0; i < n_fragmentAttrs; i++) {
      final int ind = n_fragmentElems + i;
      final boolean useRelaxedGrammar;
      int inode;
      if (useRelaxedGrammar = ((inode = m_fragmentINodes[ind]) & 0x80000000) != 0)
        inode = ~inode;
      eventType = createAttributeEventType(inode, useRelaxedGrammar, m_eventTypeLists[2]);
      eventTypeList.add(eventType);
      if (addUndeclaredEA)
        invalidAttributes.add(createEventTypeSchemaAttributeInvalid(eventType, m_eventTypeLists[2]));
    }
    eventTypeList.add(new EventType(EventCode.EVENT_CODE_DEPTH_ONE, m_eventTypeLists[2], EventType.ITEM_SCHEMA_AT_WC_ANY, (IGrammar)null));
    eventTypeList.add(EventTypeFactory.creatEndElement(EventCode.EVENT_CODE_DEPTH_ONE, m_eventTypeLists[2]));

    // For strict grammars, use the atZero param value of "false" to avoid getting xsi:type and xsi:nil.
    createEventCodeTuple(eventTypeList, grammarCache.grammarOptions, res = new EventCodeTupleSink(), invalidAttributes, m_eventTypeLists[2],
        (grammarCache.grammarOptions & GrammarOptions.STRICT_OPTIONS) == 0, EXISchema.NIL_GRAM, false, -1, -1);

    m_eventCodes[2] = res.eventCodeTuple;
    m_eventTypes[2] = res.eventTypes;
    m_eventTypeLists[2].setItems(res.eventTypes);

    
    eventTypeList = new ArrayList<EventType>();
    eventTypeList.add(EventTypeFactory.creatEndElement(EventCode.EVENT_CODE_DEPTH_ONE, m_eventTypeLists[3]));

    createEventCodeTuple(eventTypeList, grammarCache.grammarOptions, res = new EventCodeTupleSink(), m_eventTypeLists[3]);

    m_eventCodes[3] = res.eventCodeTuple;
    m_eventTypes[3] = res.eventTypes;
    m_eventTypeLists[3].setItems(res.eventTypes);
  }
  
  @Override
  public void init(GrammarState stateVariables) {
    stateVariables.targetGrammar = this;
    stateVariables.phase = ELEMENT_FRAGMENT_STATE_TAG;
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Implementation of abstract methods declared in CommonState
  ///////////////////////////////////////////////////////////////////////////

  @Override
  void xsitp(int tp, GrammarState stateVariables) {
    assert EXISchema.NIL_NODE != tp;
    final EXIGrammar typeGrammar = m_grammarCache.getTypeGrammar(tp);
    typeGrammar.init(stateVariables);
    stateVariables.contentDatatype = schema.isSimpleType(tp) ? tp : schema.getContentDatatypeOfComplexType(tp);
  }
  
  @Override
  void nillify(int eventTypeIndex, GrammarState stateVariables) {
    assert stateVariables.phase == ELEMENT_FRAGMENT_STATE_TAG;
    stateVariables.phase = ELEMENT_FRAGMENT_EMPTY_STATE_TAG;
  }
  
  @Override
  public EventTypeList getNextEventTypes(GrammarState stateVariables) {
    return m_eventTypeLists[stateVariables.phase - ELEMENT_FRAGMENT_STATE_BASE];
  }
  
  @Override
  EventCodeTuple getNextEventCodes(GrammarState stateVariables) {
    return m_eventCodes[stateVariables.phase - ELEMENT_FRAGMENT_STATE_BASE];
  }

  @Override
  public void element(EventType eventType, GrammarState stateVariables) {
    final Grammar subsequentGrammar;
    final Grammar grammar;
    subsequentGrammar = (grammar = ((EventTypeElement)eventType).ensuingGrammar) != null ?
        (Grammar)grammar : this; 
    
    subsequentGrammar.init(stateVariables.apparatus.pushState());

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
  public Grammar wildcardElement(int eventTypeIndex, int uriId, int localNameId, GrammarState stateVariables) {
    switch (stateVariables.phase) {
      case ELEMENT_FRAGMENT_STATE_TAG:
        stateVariables.phase = ELEMENT_FRAGMENT_STATE_CONTENT;
        break;
      case ELEMENT_FRAGMENT_EMPTY_STATE_TAG:
        stateVariables.phase = ELEMENT_FRAGMENT_EMPTY_STATE_CONTENT;
        break;
    }    
    return super.wildcardElement(eventTypeIndex, uriId, localNameId, stateVariables);
  }
  
  @Override
  public void chars(EventType eventType, GrammarState stateVariables) {
    throw new IllegalStateException("char() cannot be invoked on an element fragment grammar.");
  }

  @Override
  final public void undeclaredChars(int eventTypeIndex, GrammarState stateVariables) {
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
  public void miscContent(int eventTypeIndex, GrammarState stateVariables) {
    undeclaredChars(eventTypeIndex, stateVariables);
  }

  @Override
  public void end(GrammarState stateVariables) {
  }

  /**
   * Create EventTypeSchema(s) from a node in EXISchema.
   */
  private EventTypeSchema createAttributeEventType(final int attr, boolean useRelaxedGrammar,  
    EventTypeList eventTypeList) {
    final int uriId = schema.getUriOfAttr(attr);
    final int localNameId = schema.getLocalNameOfAttr(attr);
    return new EventTypeSchema(
        useRelaxedGrammar ? EXISchema.NIL_NODE : schema.getTypeOfAttr(attr),
        schema.uris[uriId], schema.localNames[uriId][localNameId], uriId, localNameId, 
        EventCode.EVENT_CODE_DEPTH_ONE, eventTypeList, EventType.ITEM_SCHEMA_AT, (EXIGrammar)null);
  }

}
