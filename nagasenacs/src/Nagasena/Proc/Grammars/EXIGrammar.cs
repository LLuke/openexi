using System.Diagnostics;
using System.Collections.Generic;

using EventCode = Nagasena.Proc.Common.EventCode;
using EventType = Nagasena.Proc.Common.EventType;
using EventTypeList = Nagasena.Proc.Common.EventTypeList;
using IGrammar = Nagasena.Proc.Common.IGrammar;
using GrammarOptions = Nagasena.Proc.Common.GrammarOptions;
using EXISchema = Nagasena.Schema.EXISchema;

namespace Nagasena.Proc.Grammars {

  internal sealed class EXIGrammar : SchemaInformedGrammar, IGrammar {

    ///////////////////////////////////////////////////////////////////////////
    /// immutables
    ///////////////////////////////////////////////////////////////////////////

    private EventType[] m_eventTypes;
    private EventCodeTuple m_eventCode;
    private ArrayEventTypeList m_eventTypeList;

    ///////////////////////////////////////////////////////////////////////////
    /// constructors, initializers
    ///////////////////////////////////////////////////////////////////////////

    internal EXIGrammar(GrammarCache grammarCache) : 
      base(Grammar.SCHEMA_GRAMMAR_ELEMENT_AND_TYPE, grammarCache) {
    }

    internal void substantiate(int nd, bool isElem) {
      int gram;
      if (isElem) {
        // nd represents an elem
        Debug.Assert(nd != EXISchema.NIL_NODE);
        int tp = schema.getTypeOfElem(nd);
        gram = schema.getGrammarOfType(tp);
      }
      else {
        gram = nd; // nd represents a gram
      }

      m_eventTypeList = new ArrayEventTypeList();

      List<EventType> eventTypeList = new List<EventType>();

      int n_productions = schema.getProductionCountOfGrammar(gram);

      int gramContent = schema.getContentGrammarOfGrammar(gram);

      List<EventType> invalidAttributes = GrammarOptions.isPermitDeviation(m_grammarCache.grammarOptions) && gramContent != EXISchema.NIL_GRAM ? new List<EventType>() : null;

      for (int i = 0; i < n_productions; i++) {
        int prod = schema.getProductionOfGrammar(gram, i);
        EventType eventType = createEventType(prod, m_eventTypeList);
        eventTypeList.Add(eventType);
        if (invalidAttributes != null && eventType.itemType == EventType.ITEM_SCHEMA_AT) {
          invalidAttributes.Add(createEventTypeSchemaAttributeInvalid((EventTypeSchema)eventType, m_eventTypeList));
        }
      }
      if (schema.hasEndElement(gram)) {
        EventType endElement = EventTypeFactory.creatEndElement(EventCode.EVENT_CODE_DEPTH_ONE, m_eventTypeList);
        int n_eventTypes = eventTypeList.Count;
        int pos = 0;
        if (n_eventTypes != 0) {
          EventType eventType = eventTypeList[n_eventTypes - 1];
          pos = eventType.itemType == EventType.ITEM_SCHEMA_CH_MIXED ? n_eventTypes - 1 : n_eventTypes;
        }
        eventTypeList.Insert(pos, endElement);
      }

      int gramTypeEmpty = schema.getTypeEmptyGrammarOfGrammar(gram);

      EventCodeTupleSink res;
      createEventCodeTuple(eventTypeList, m_grammarCache.grammarOptions, res = new EventCodeTupleSink(), invalidAttributes, m_eventTypeList, schema.hasEmptyGrammar(gram), nd, isElem, gramTypeEmpty, gramContent);

      m_eventCode = res.eventCodeTuple;
      m_eventTypes = res.eventTypes;
      m_eventTypeList.Items = res.eventTypes;
    }

    public override void init(GrammarState stateVariables) {
      stateVariables.targetGrammar = this;
    }

    private EventType createEventType(int prod, EventTypeList eventTypeList) {
      int gram = schema.getGrammarOfProduction(prod);
      EXIGrammar subsequentGrammar = retrieveEXIGrammar(gram);
      int @event;
      switch (@event = schema.getEventOfProduction(prod)) {
        case EXISchema.EVENT_AT_WILDCARD:
          return new EventType(EventCode.EVENT_CODE_DEPTH_ONE, eventTypeList, EventType.ITEM_SCHEMA_AT_WC_ANY, subsequentGrammar);
        case EXISchema.EVENT_SE_WILDCARD:
          return new EventType(EventCode.EVENT_CODE_DEPTH_ONE, eventTypeList, EventType.ITEM_SCHEMA_WC_ANY, subsequentGrammar);
        case EXISchema.EVENT_CH_UNTYPED:
          return new EventType(EventCode.EVENT_CODE_DEPTH_ONE, eventTypeList, EventType.ITEM_SCHEMA_CH_MIXED, subsequentGrammar);
        case EXISchema.EVENT_CH_TYPED:
          return new EventType(EventCode.EVENT_CODE_DEPTH_ONE, eventTypeList, EventType.ITEM_SCHEMA_CH, subsequentGrammar);
        default:
          int uriId, localNameId;
          switch (schema.getEventType(@event)) {
            case EXISchema.EVENT_TYPE_AT:
              int attr = schema.getNodeOfEventType(@event);
              uriId = schema.getUriOfAttr(attr);
              localNameId = schema.getLocalNameOfAttr(attr);
              return new EventTypeSchema(schema.getTypeOfAttr(attr), schema.uris[uriId], schema.localNames[uriId][localNameId], uriId, localNameId, EventCode.EVENT_CODE_DEPTH_ONE, eventTypeList, EventType.ITEM_SCHEMA_AT, subsequentGrammar);
            case EXISchema.EVENT_TYPE_SE:
              int elem = schema.getNodeOfEventType(@event);
              uriId = schema.getUriOfElem(elem);
              localNameId = schema.getLocalNameOfElem(elem);
              EXIGrammarUse ensuingGrammarUse = m_grammarCache.exiGrammarUses[schema.getSerialOfElem(elem)];
              return new EventTypeElement(uriId, schema.uris[uriId], localNameId, schema.localNames[uriId][localNameId], eventTypeList, ensuingGrammarUse, subsequentGrammar);
            case EXISchema.EVENT_TYPE_AT_WILDCARD_NS:
              uriId = schema.getUriOfEventType(@event);
              return new EventType(schema.uris[uriId], (string)null, uriId, -1, EventCode.EVENT_CODE_DEPTH_ONE, eventTypeList, EventType.ITEM_SCHEMA_AT_WC_NS, subsequentGrammar);
            case EXISchema.EVENT_TYPE_SE_WILDCARD_NS:
              uriId = schema.getUriOfEventType(@event);
              return new EventType(schema.uris[uriId], (string)null, uriId, -1, EventCode.EVENT_CODE_DEPTH_ONE, eventTypeList, EventType.ITEM_SCHEMA_WC_NS, subsequentGrammar);
            default:
              Debug.Assert(false);
              return null;
          }
      }
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Implementation of IGrammar (used by StringTable)
    ///////////////////////////////////////////////////////////////////////////

    public void reset() {
      Debug.Assert(false);
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Implementation of abstract methods
    ///////////////////////////////////////////////////////////////////////////

    internal override void attribute(EventType eventType, GrammarState stateVariables) {
      makeTransition(eventType, stateVariables);
    }

    internal override EventTypeList getNextEventTypes(GrammarState stateVariables) {
      return m_eventTypeList;
    }

    internal override EventCodeTuple getNextEventCodes(GrammarState stateVariables) {
      return m_eventCode;
    }

    public override void element(EventType eventType, GrammarState stateVariables) {
      Debug.Assert(eventType.itemType == EventType.ITEM_SE);
      EventTypeElement eventTypeElement = (EventTypeElement)eventType;
      GrammarState kid = stateVariables.apparatus.pushState();
      eventTypeElement.ensuingGrammar.init(kid);
      makeTransition(eventType, stateVariables);
    }

    internal override Grammar wildcardElement(int eventTypeIndex, int uriId, int localNameId, GrammarState stateVariables) {
      base.wildcardElement(eventTypeIndex, uriId, localNameId, stateVariables);
      return makeTransition(eventTypeIndex, stateVariables);
    }

    internal override void xsitp(int tp, GrammarState stateVariables) {
      Debug.Assert(EXISchema.NIL_NODE != tp);
      int gram = schema.getGrammarOfType(tp);
      stateVariables.targetGrammar = m_grammarCache.exiGrammars[schema.getSerialOfGrammar(gram)];
      stateVariables.contentDatatype = schema.isSimpleType(tp) ? tp : schema.getContentDatatypeOfComplexType(tp);
    }

    internal override void nillify(int eventTypeIndex, GrammarState stateVariables) {
      makeTransition(eventTypeIndex, stateVariables);
    }

    public override void chars(EventType eventType, GrammarState stateVariables) {
      makeTransition(eventType, stateVariables);
    }

    public override void undeclaredChars(int eventTypeIndex, GrammarState stateVariables) {
      makeTransition(eventTypeIndex, stateVariables);
    }

    public override void miscContent(int eventTypeIndex, GrammarState stateVariables) {
      makeTransition(eventTypeIndex, stateVariables);
    }

    public override void end(GrammarState stateVariables) {
    }

    private Grammar makeTransition(int eventTypeIndex, GrammarState stateVariables) {
      Grammar subsequentGrammar = (Grammar)m_eventTypes[eventTypeIndex].subsequentGrammar;
      stateVariables.targetGrammar = subsequentGrammar;
      return subsequentGrammar;
    }

    private Grammar makeTransition(EventType eventType, GrammarState stateVariables) {
        Grammar subsequentGrammar = (Grammar)eventType.subsequentGrammar;
        stateVariables.targetGrammar = subsequentGrammar;
        return subsequentGrammar;
    }

  }

}