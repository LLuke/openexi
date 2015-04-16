package org.openexi.proc.grammars;

import java.util.ArrayList;

import org.openexi.proc.common.EventCode;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventTypeList;
import org.openexi.proc.common.IGrammar;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.schema.EXISchema;

final class EXIGrammar extends SchemaInformedGrammar implements IGrammar {

  ///////////////////////////////////////////////////////////////////////////
  /// immutables
  ///////////////////////////////////////////////////////////////////////////
  
  private EventType[] m_eventTypes;
  private EventCodeTuple m_eventCode;
  private ArrayEventTypeList m_eventTypeList;
  
  ///////////////////////////////////////////////////////////////////////////
  /// constructors, initializers
  ///////////////////////////////////////////////////////////////////////////
  
  EXIGrammar(GrammarCache grammarCache) {
    super(Grammar.SCHEMA_GRAMMAR_ELEMENT_AND_TYPE, grammarCache);
  }

  void substantiate(int nd, boolean isElem) {
    final int gram;
    if (isElem) {
      // nd represents an elem
      assert nd != EXISchema.NIL_NODE;
      final int tp = schema.getTypeOfElem(nd);
      gram = schema.getGrammarOfType(tp);
    }
    else
      gram = nd; // nd represents a gram
    
    m_eventTypeList = new ArrayEventTypeList();

    ArrayList<EventType> eventTypeList = new ArrayList<EventType>();
    
    final int n_productions = schema.getProductionCountOfGrammar(gram);

    final int gramContent = schema.getContentGrammarOfGrammar(gram);

    final ArrayList<EventType> invalidAttributes = 
        GrammarOptions.isPermitDeviation(m_grammarCache.grammarOptions) && gramContent != EXISchema.NIL_GRAM ? new ArrayList<EventType>() : null;
    
    for (int i = 0; i < n_productions; i++) {
      final int prod = schema.getProductionOfGrammar(gram, i);
      final EventType eventType = createEventType(prod, m_eventTypeList);
      eventTypeList.add(eventType);
      if (invalidAttributes != null && eventType.itemType == EventType.ITEM_SCHEMA_AT) {
        invalidAttributes.add(createEventTypeSchemaAttributeInvalid((EventTypeSchema)eventType, m_eventTypeList));
      }
    }
    if (schema.hasEndElement(gram)) {
      final EventType endElement = EventTypeFactory.creatEndElement(EventCode.EVENT_CODE_DEPTH_ONE, m_eventTypeList) ;
      final int n_eventTypes = eventTypeList.size();
      int pos = 0;
      if (n_eventTypes != 0) {
        final EventType eventType = eventTypeList.get(n_eventTypes - 1);
        pos = eventType.itemType == EventType.ITEM_SCHEMA_CH_MIXED ? n_eventTypes - 1 : n_eventTypes;
      }
      eventTypeList.add(pos, endElement);
    }
    
    final int gramTypeEmpty = schema.getTypeEmptyGrammarOfGrammar(gram);
    
    EventCodeTupleSink res;
    createEventCodeTuple(eventTypeList, m_grammarCache.grammarOptions, res = new EventCodeTupleSink(), invalidAttributes, 
        m_eventTypeList, schema.hasEmptyGrammar(gram), nd, isElem, gramTypeEmpty, gramContent);

    m_eventCode = res.eventCodeTuple;
    m_eventTypes = res.eventTypes;
    m_eventTypeList.setItems(res.eventTypes);
  }
  
  @Override
  public void init(GrammarState stateVariables) {
    stateVariables.targetGrammar = this;
  }

  private EventType createEventType(int prod, EventTypeList eventTypeList) {
    final int gram = schema.getGrammarOfProduction(prod);
    final EXIGrammar subsequentGrammar = retrieveEXIGrammar(gram);
    final int event;
    switch (event = schema.getEventOfProduction(prod)) {
      case EXISchema.EVENT_AT_WILDCARD:
        return new EventType(EventCode.EVENT_CODE_DEPTH_ONE, eventTypeList, EventType.ITEM_SCHEMA_AT_WC_ANY, subsequentGrammar); 
      case EXISchema.EVENT_SE_WILDCARD:
        return new EventType(EventCode.EVENT_CODE_DEPTH_ONE, eventTypeList, EventType.ITEM_SCHEMA_WC_ANY, subsequentGrammar);
      case EXISchema.EVENT_CH_UNTYPED:
        return new EventType(EventCode.EVENT_CODE_DEPTH_ONE, eventTypeList, EventType.ITEM_SCHEMA_CH_MIXED, subsequentGrammar);
      case EXISchema.EVENT_CH_TYPED:
        return new EventType(EventCode.EVENT_CODE_DEPTH_ONE, eventTypeList, EventType.ITEM_SCHEMA_CH, subsequentGrammar); 
      default:
        final int uriId, localNameId;
        switch (schema.getEventType(event)) {
          case EXISchema.EVENT_TYPE_AT:
            final int attr = schema.getNodeOfEventType(event);
            uriId = schema.getUriOfAttr(attr);
            localNameId = schema.getLocalNameOfAttr(attr);
            return new EventTypeSchema(schema.getTypeOfAttr(attr), schema.uris[uriId], schema.localNames[uriId][localNameId],
                uriId, localNameId, EventCode.EVENT_CODE_DEPTH_ONE, eventTypeList, EventType.ITEM_SCHEMA_AT, subsequentGrammar);
          case EXISchema.EVENT_TYPE_SE:
            final int elem = schema.getNodeOfEventType(event);
            uriId = schema.getUriOfElem(elem);
            localNameId = schema.getLocalNameOfElem(elem);
            final EXIGrammarUse ensuingGrammarUse = m_grammarCache.exiGrammarUses[schema.getSerialOfElem(elem)];
            return new EventTypeElement(uriId, schema.uris[uriId], localNameId, schema.localNames[uriId][localNameId], 
                eventTypeList, ensuingGrammarUse, subsequentGrammar);
          case EXISchema.EVENT_TYPE_AT_WILDCARD_NS:
            uriId = schema.getUriOfEventType(event);
            return new EventType(schema.uris[uriId], (String)null, uriId, -1, EventCode.EVENT_CODE_DEPTH_ONE, 
                eventTypeList, EventType.ITEM_SCHEMA_AT_WC_NS, subsequentGrammar);
          case EXISchema.EVENT_TYPE_SE_WILDCARD_NS:
            uriId = schema.getUriOfEventType(event);
            return new EventType(schema.uris[uriId], (String)null, uriId, -1, EventCode.EVENT_CODE_DEPTH_ONE, 
                eventTypeList, EventType.ITEM_SCHEMA_WC_NS, subsequentGrammar);
          default:
            assert false;
            return null;
        }
    }
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Implementation of IGrammar (used by StringTable)
  ///////////////////////////////////////////////////////////////////////////

  public void reset() {
    assert false;
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Implementation of abstract methods
  ///////////////////////////////////////////////////////////////////////////

  @Override
  void attribute(EventType eventType, GrammarState stateVariables) {
    makeTransition(eventType, stateVariables);
  }

  @Override
  public EventTypeList getNextEventTypes(GrammarState stateVariables) {
    return m_eventTypeList;
  }

  @Override
  EventCodeTuple getNextEventCodes(GrammarState stateVariables) {
    return m_eventCode;
  }

  @Override
  public void element(EventType eventType, GrammarState stateVariables) {
    assert eventType.itemType == EventType.ITEM_SE; 
    final EventTypeElement eventTypeElement = (EventTypeElement)eventType;
    final GrammarState kid = stateVariables.apparatus.pushState();
    eventTypeElement.ensuingGrammar.init(kid);
    makeTransition(eventType, stateVariables);
  }
  
  @Override
  Grammar wildcardElement(int eventTypeIndex, int uriId, int localNameId, GrammarState stateVariables) {
    super.wildcardElement(eventTypeIndex, uriId, localNameId, stateVariables);
    return makeTransition(eventTypeIndex, stateVariables);
  }

  @Override
  void xsitp(int tp, GrammarState stateVariables) {
    assert EXISchema.NIL_NODE != tp;
    final int gram = schema.getGrammarOfType(tp);
    stateVariables.targetGrammar = m_grammarCache.exiGrammars[schema.getSerialOfGrammar(gram)];;
    stateVariables.contentDatatype = schema.isSimpleType(tp) ? tp : schema.getContentDatatypeOfComplexType(tp);
  }

  @Override
  void nillify(int eventTypeIndex, GrammarState stateVariables) {
    makeTransition(eventTypeIndex, stateVariables);
  }

  @Override
  public void chars(EventType eventType, GrammarState stateVariables) {
    makeTransition(eventType, stateVariables);
  }

  @Override
  public void undeclaredChars(int eventTypeIndex, GrammarState stateVariables) {
    makeTransition(eventTypeIndex, stateVariables);
  }

  @Override
  public void miscContent(int eventTypeIndex, GrammarState stateVariables) {
    makeTransition(eventTypeIndex, stateVariables);
  }

  @Override
  public void end(GrammarState stateVariables) {
  }

  private Grammar makeTransition(int eventTypeIndex, GrammarState stateVariables) {
    final Grammar subsequentGrammar = (Grammar)m_eventTypes[eventTypeIndex].subsequentGrammar; 
    stateVariables.targetGrammar = subsequentGrammar;
    return subsequentGrammar;
  }

  private Grammar makeTransition(EventType eventType, GrammarState stateVariables) {
      final Grammar subsequentGrammar = (Grammar)eventType.subsequentGrammar;
      stateVariables.targetGrammar = subsequentGrammar;
      return subsequentGrammar;
  }

}
