package org.openexi.fujitsu.proc.grammars;

import java.util.ArrayList;

import org.openexi.fujitsu.proc.common.EventCode;
import org.openexi.fujitsu.proc.common.EventType;
import org.openexi.fujitsu.proc.common.EventTypeList;
import org.openexi.fujitsu.proc.common.GrammarOptions;
import org.openexi.fujitsu.schema.EXISchema;
import org.openexi.fujitsu.schema.EXISchemaConst;

final class SchemaInformedElementGrammar extends SchemaInformedGrammar {

  private final ElementTagGrammar m_naturalTagGrammar;
  
  private final EventType[] m_eventTypes;
  private final ArrayEventTypeList m_eventTypeList;
  
  private final EventCodeTuple m_eventCodes;
  
  private static final ArrayList<EventTypeSchemaAttributeInvalid> NO_INVALID_ATTRIBUTES =
    new ArrayList<EventTypeSchemaAttributeInvalid>();
  
  ///////////////////////////////////////////////////////////////////////////
  /// constructors, initializers
  ///////////////////////////////////////////////////////////////////////////

  SchemaInformedElementGrammar(int elem, GrammarCache cache) {
    super(elem, SCHEMA_GRAMMAR_ELEMENT, cache);
    final int tp;
    if (m_nd != EXISchema.NIL_NODE)
      tp = m_schema.getTypeOfElem(m_nd);
    else {
      tp = m_schema.getBuiltinTypeOfSchema(EXISchemaConst.ANY_TYPE);
      assert "anyType".equals(m_schema.getNameOfType(tp));
    }

    m_naturalTagGrammar = m_grammarCache.retrieveElementTagGrammar(tp);
    
    final boolean addUndeclaredEA;
    addUndeclaredEA = GrammarOptions.hasUndeclaredEA(cache.grammarOptions);

    m_eventTypeList = new ArrayEventTypeList();

    final EventType[] eventTypes = m_naturalTagGrammar.getInitialEventTypes();
    // Extract EventTypeSchema out of eventTypes into subsequentEventTypes
    final ArrayList<AbstractEventType> subsequentEventTypes = new ArrayList<AbstractEventType>();
    ArrayList<EventTypeSchemaAttributeInvalid> invalidAttributes = null;
    for (int i = 0; i < eventTypes.length; i++) {
      EventType eventType = eventTypes[i];
      if (eventType.isSchemaInformed()) {
        EventTypeSchema eventTypeSchema = (EventTypeSchema)eventType;
        if (!eventTypeSchema.isAugmented()) {
          final short itemType = eventType.itemType;
          assert eventType.itemType != EventCode.ITEM_SCHEMA_NIL && itemType != EventCode.ITEM_SCHEMA_TYPE; 
          subsequentEventTypes.add(eventTypeSchema.duplicate(m_eventTypeList));
          if (addUndeclaredEA && itemType == EventCode.ITEM_SCHEMA_AT) {
            if (invalidAttributes == null) {
              invalidAttributes = new ArrayList<EventTypeSchemaAttributeInvalid>();
            }
            invalidAttributes.add(new EventTypeSchemaAttributeInvalid((EventTypeSchemaAttribute)eventType, this, m_eventTypeList));
          }
        }
      }
    }
    if (addUndeclaredEA && invalidAttributes == null) {
      assert NO_INVALID_ATTRIBUTES.size() == 0;
      invalidAttributes = NO_INVALID_ATTRIBUTES;
    }

    EventCodeTupleSink res;
    createEventCodeTuple(subsequentEventTypes, cache.grammarOptions, res = new EventCodeTupleSink(), invalidAttributes, m_eventTypeList, true);

    m_eventCodes = res.eventCodeTuple;
    m_eventTypes = res.eventTypes;
    m_eventTypeList.setItems(res.eventTypes);
  }

  @Override
  public final void init(GrammarState stateVariables) {
    super.init(stateVariables);
    stateVariables.phase = ELEMENT_STATE_UNBOUND;
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Implementation of abstract methods declared in CommonState
  ///////////////////////////////////////////////////////////////////////////

  @Override
  final void xsitp(int tp, GrammarState stateVariables) {
    assert stateVariables.phase == ELEMENT_STATE_UNBOUND;
    assert (m_schema.getNodeType(tp) & EXISchema.TYPE_MASK) == EXISchema.TYPE_MASK;
    final GrammarState kid = bind(stateVariables);
    m_grammarCache.retrieveElementTagGrammar(tp).init(kid);
  }
  
  @Override
  final void nillify(GrammarState stateVariables) {
    assert stateVariables.phase == ELEMENT_STATE_UNBOUND;
    final GrammarState kid = bind(stateVariables);
    m_naturalTagGrammar.init(kid);
    kid.targetGrammar.nillify(kid);
  }
  
  @Override
  final EventTypeList getNextEventTypes(GrammarState stateVariables) {
    final GrammarState elementTagState = stateVariables.elementTagStateVariables;
    if (stateVariables.phase != ELEMENT_STATE_BOUND)
      return m_eventTypeList;
    else
      return elementTagState.targetGrammar.getNextEventTypes(elementTagState);
  }
  
  @Override
  final EventCodeTuple getNextEventCodes(GrammarState stateVariables) {
    final GrammarState elementTagState = stateVariables.elementTagStateVariables;
    if (stateVariables.phase != ELEMENT_STATE_BOUND)
      return m_eventCodes;
    else
      return elementTagState.targetGrammar.getNextEventCodes(elementTagState);
  }

  @Override
  final void element(int eventTypeIndex, String uri, String name, GrammarState stateVariables) {
    assert stateVariables.phase == ELEMENT_STATE_UNBOUND;
    GrammarState elementTagState = bind(stateVariables); 
    m_naturalTagGrammar.init(elementTagState);
    ((SchemaInformedGrammar)elementTagState.targetGrammar).element(
        ((EventTypeSchema)m_eventTypes[eventTypeIndex]).serial, uri, name, elementTagState);
  }
  
  @Override
  final Grammar undeclaredElement(String uri, String name, GrammarState stateVariables) {
    assert stateVariables.phase == ELEMENT_STATE_UNBOUND;
    GrammarState elementTagState = bind(stateVariables);
    m_naturalTagGrammar.init(elementTagState);
    return elementTagState.targetGrammar.undeclaredElement(uri, name, elementTagState); // delegate
  }

  @Override
  final void schemaAttribute(int eventTypeIndex, String uri, String name, GrammarState stateVariables) {
    if (stateVariables.phase == ELEMENT_STATE_UNBOUND) {
      GrammarState elementTagState = bind(stateVariables);
      m_naturalTagGrammar.init(elementTagState);
      ((SchemaInformedGrammar)elementTagState.targetGrammar).schemaAttribute(
          ((EventTypeSchema)m_eventTypes[eventTypeIndex]).serial, uri, name, elementTagState);
      return;
    }
    assert false;
  }
  
  @Override
  public final void chars(GrammarState stateVariables) {
    if (stateVariables.phase == ELEMENT_STATE_UNBOUND) {
      GrammarState elementTagState = bind(stateVariables);
      m_naturalTagGrammar.init(elementTagState);
      elementTagState.targetGrammar.chars(elementTagState);
      return;
    }
    assert false;
  }

  @Override
  public final void undeclaredChars(GrammarState stateVariables) {
    if (stateVariables.phase == ELEMENT_STATE_UNBOUND) {
      GrammarState elementTagState = bind(stateVariables);
      m_naturalTagGrammar.init(elementTagState);
      elementTagState.targetGrammar.undeclaredChars(elementTagState);
      return;
    }
    assert false;
  }

  @Override
  public final void miscContent(GrammarState stateVariables) {
    if (stateVariables.phase == ELEMENT_STATE_UNBOUND) {
      GrammarState elementTagState = bind(stateVariables);
      m_naturalTagGrammar.init(elementTagState);
      elementTagState.targetGrammar.miscContent(elementTagState);
      return;
    }
    assert false;
  }

  @Override
  final void done(GrammarState kid, GrammarState stateVariables) {
    assert kid.targetGrammar.m_grammarType == SCHEMA_GRAMMAR_ELEMENT_TAG;
    finish(stateVariables);
  }

  @Override
  final void end(String uri, String name, GrammarState stateVariables) {
    if (stateVariables.phase == ELEMENT_STATE_UNBOUND) {
      GrammarState elementTagState = bind(stateVariables);
      m_naturalTagGrammar.init(elementTagState);
      elementTagState.targetGrammar.end(uri, name, elementTagState);
      return;
    }
    finish(stateVariables);
  }

  private GrammarState bind(GrammarState stateVariables) {
    final GrammarState elementTagStateVariables = stateVariables.documentGrammarState.pushState();
    stateVariables.elementTagStateVariables = elementTagStateVariables;
    stateVariables.phase = ELEMENT_STATE_BOUND;
    return elementTagStateVariables;
  }
  
}