using System;
using System.Diagnostics;
using System.Collections.Generic;

using EventCode = Nagasena.Proc.Common.EventCode;
using EventType = Nagasena.Proc.Common.EventType;
using EventTypeList = Nagasena.Proc.Common.EventTypeList;
using GrammarOptions = Nagasena.Proc.Common.GrammarOptions;
using IGrammar = Nagasena.Proc.Common.IGrammar;
using EXISchema = Nagasena.Schema.EXISchema;

namespace Nagasena.Proc.Grammars {

  internal sealed class ElementFragmentGrammar : SchemaInformedGrammar {

    ///////////////////////////////////////////////////////////////////////////
    /// immutables
    ///////////////////////////////////////////////////////////////////////////

    private readonly int[] m_fragmentINodes;
    private readonly EventType[][] m_eventTypes;
    private readonly EventCodeTuple[] m_eventCodes;
    private readonly ArrayEventTypeList[] m_eventTypeLists;

    internal ElementFragmentGrammar(GrammarCache grammarCache) : base(SCHEMA_GRAMMAR_ELEMENT_FRAGMENT, grammarCache) {

      m_fragmentINodes = schema.FragmentINodes;
      int n_fragmentElems = schema.FragmentElemCount;
      int n_fragmentAttrs = m_fragmentINodes.Length - n_fragmentElems;

      m_eventTypes = new EventType[4][];
      m_eventCodes = new EventCodeTuple[4];
      m_eventTypeLists = new ArrayEventTypeList[4];

      int i;
      for (i = 0; i < 4; i++) {
        m_eventTypeLists[i] = new ArrayEventTypeList();
      }

      bool addUndeclaredEA = GrammarOptions.isPermitDeviation(grammarCache.grammarOptions);

      List<EventType> eventTypeList;
      EventCodeTupleSink res;
      List<EventType> invalidAttributes = null;
      EventTypeSchema eventType;

      eventTypeList = new List<EventType>();
      if (addUndeclaredEA) {
        invalidAttributes = new List<EventType>();
      }
      for (i = 0; i < n_fragmentAttrs; i++) {
        int ind = n_fragmentElems + i;
        bool useRelaxedGrammar;
        int inode;
        if (useRelaxedGrammar = ((inode = m_fragmentINodes[ind]) & 0x80000000) != 0) {
          inode = ~inode;
        }
        eventType = createAttributeEventType(inode, useRelaxedGrammar, m_eventTypeLists[0]);
        eventTypeList.Add(eventType);
        if (addUndeclaredEA) {
          invalidAttributes.Add(createEventTypeSchemaAttributeInvalid(eventType, m_eventTypeLists[0]));
        }
      }
      eventTypeList.Add(new EventType(EventCode.EVENT_CODE_DEPTH_ONE, m_eventTypeLists[0], EventType.ITEM_SCHEMA_AT_WC_ANY, (IGrammar)null));
      for (i = 0; i < n_fragmentElems; i++) {
        EXIGrammarUse subsequentGrammar;
        int elem;
        if (((elem = m_fragmentINodes[i]) & 0x80000000) != 0) {
          elem = ~elem;
          subsequentGrammar = null;
        }
        else {
          // use a specific grammar
          subsequentGrammar = grammarCache.exiGrammarUses[schema.getSerialOfElem(elem)];
        }
        int uriId = schema.getUriOfElem(elem);
        int localNameId = schema.getLocalNameOfElem(elem);
        EventTypeElement eventType2 = EventTypeFactory.createStartElement(uriId, localNameId, schema.uris[uriId], schema.localNames[uriId][localNameId], m_eventTypeLists[0], subsequentGrammar, (EXIGrammar)null);
        eventTypeList.Add(eventType2);
      }
      eventTypeList.Add(new EventType(EventCode.EVENT_CODE_DEPTH_ONE, m_eventTypeLists[0], EventType.ITEM_SE_WC, (IGrammar)null));
      eventTypeList.Add(EventTypeFactory.creatEndElement(EventCode.EVENT_CODE_DEPTH_ONE, m_eventTypeLists[0]));
      eventTypeList.Add(new EventType(EventCode.EVENT_CODE_DEPTH_ONE, m_eventTypeLists[0], EventType.ITEM_CH, (IGrammar)null));

      createEventCodeTuple(eventTypeList, grammarCache.grammarOptions, res = new EventCodeTupleSink(), invalidAttributes, m_eventTypeLists[0], true, EXISchema.NIL_GRAM, false, -1, -1);

      m_eventCodes[0] = res.eventCodeTuple;
      m_eventTypes[0] = res.eventTypes;
      m_eventTypeLists[0].Items = res.eventTypes;


      eventTypeList = new List<EventType>();
      for (i = 0; i < n_fragmentElems; i++) {
        EXIGrammarUse ensuingGrammar;
        int elem;
        if (((elem = m_fragmentINodes[i]) & 0x80000000) != 0) {
          elem = ~elem;
          ensuingGrammar = null;
        }
        else {
          // use a specific grammar
          ensuingGrammar = grammarCache.exiGrammarUses[schema.getSerialOfElem(elem)];
        }
        int uriId = schema.getUriOfElem(elem);
        int localNameId = schema.getLocalNameOfElem(elem);
        EventTypeElement eventType2 = EventTypeFactory.createStartElement(uriId, localNameId, schema.uris[uriId], schema.localNames[uriId][localNameId], m_eventTypeLists[1], ensuingGrammar, (EXIGrammar)null);
        eventTypeList.Add(eventType2);
      }
      eventTypeList.Add(new EventType(EventCode.EVENT_CODE_DEPTH_ONE, m_eventTypeLists[1], EventType.ITEM_SE_WC, (IGrammar)null));
      eventTypeList.Add(EventTypeFactory.creatEndElement(EventCode.EVENT_CODE_DEPTH_ONE, m_eventTypeLists[1]));
      eventTypeList.Add(new EventType(EventCode.EVENT_CODE_DEPTH_ONE, m_eventTypeLists[1], EventType.ITEM_CH, (IGrammar)null));

      createEventCodeTuple(eventTypeList, grammarCache.grammarOptions, res = new EventCodeTupleSink(), m_eventTypeLists[1]);

      m_eventCodes[1] = res.eventCodeTuple;
      m_eventTypes[1] = res.eventTypes;
      m_eventTypeLists[1].Items = res.eventTypes;


      eventTypeList = new List<EventType>();
      if (addUndeclaredEA) {
        invalidAttributes.Clear();
      }
      for (i = 0; i < n_fragmentAttrs; i++) {
        int ind = n_fragmentElems + i;
        bool useRelaxedGrammar;
        int inode;
        if (useRelaxedGrammar = ((inode = m_fragmentINodes[ind]) & 0x80000000) != 0) {
          inode = ~inode;
        }
        eventType = createAttributeEventType(inode, useRelaxedGrammar, m_eventTypeLists[2]);
        eventTypeList.Add(eventType);
        if (addUndeclaredEA) {
          invalidAttributes.Add(createEventTypeSchemaAttributeInvalid(eventType, m_eventTypeLists[2]));
        }
      }
      eventTypeList.Add(new EventType(EventCode.EVENT_CODE_DEPTH_ONE, m_eventTypeLists[2], EventType.ITEM_SCHEMA_AT_WC_ANY, (IGrammar)null));
      eventTypeList.Add(EventTypeFactory.creatEndElement(EventCode.EVENT_CODE_DEPTH_ONE, m_eventTypeLists[2]));

      // For strict grammars, use the atZero param value of "false" to avoid getting xsi:type and xsi:nil.
      createEventCodeTuple(eventTypeList, grammarCache.grammarOptions, res = new EventCodeTupleSink(), invalidAttributes, m_eventTypeLists[2], (grammarCache.grammarOptions & GrammarOptions.STRICT_OPTIONS) == 0, EXISchema.NIL_GRAM, false, -1, -1);

      m_eventCodes[2] = res.eventCodeTuple;
      m_eventTypes[2] = res.eventTypes;
      m_eventTypeLists[2].Items = res.eventTypes;


      eventTypeList = new List<EventType>();
      eventTypeList.Add(EventTypeFactory.creatEndElement(EventCode.EVENT_CODE_DEPTH_ONE, m_eventTypeLists[3]));

      createEventCodeTuple(eventTypeList, grammarCache.grammarOptions, res = new EventCodeTupleSink(), m_eventTypeLists[3]);

      m_eventCodes[3] = res.eventCodeTuple;
      m_eventTypes[3] = res.eventTypes;
      m_eventTypeLists[3].Items = res.eventTypes;
    }

    public override void init(GrammarState stateVariables) {
      stateVariables.targetGrammar = this;
      stateVariables.phase = ELEMENT_FRAGMENT_STATE_TAG;
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Implementation of abstract methods declared in CommonState
    ///////////////////////////////////////////////////////////////////////////

    internal override void xsitp(int tp, GrammarState stateVariables) {
      Debug.Assert(EXISchema.NIL_NODE != tp);
      EXIGrammar typeGrammar = m_grammarCache.getTypeGrammar(tp);
      typeGrammar.init(stateVariables);
      stateVariables.contentDatatype = schema.isSimpleType(tp) ? tp : schema.getContentDatatypeOfComplexType(tp);
    }

    internal override void nillify(int eventTypeIndex, GrammarState stateVariables) {
      Debug.Assert(stateVariables.phase == ELEMENT_FRAGMENT_STATE_TAG);
      stateVariables.phase = ELEMENT_FRAGMENT_EMPTY_STATE_TAG;
    }

    internal override EventTypeList getNextEventTypes(GrammarState stateVariables) {
      return m_eventTypeLists[stateVariables.phase - ELEMENT_FRAGMENT_STATE_BASE];
    }

    internal override EventCodeTuple getNextEventCodes(GrammarState stateVariables) {
      return m_eventCodes[stateVariables.phase - ELEMENT_FRAGMENT_STATE_BASE];
    }

    public override void element(EventType eventType, GrammarState stateVariables) {
      Grammar subsequentGrammar;
      Grammar grammar;
      subsequentGrammar = (grammar = ((EventTypeElement)eventType).ensuingGrammar) != null ? (Grammar)grammar : this;

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

    internal override Grammar wildcardElement(int eventTypeIndex, int uriId, int localNameId, GrammarState stateVariables) {
      switch (stateVariables.phase) {
        case ELEMENT_FRAGMENT_STATE_TAG:
          stateVariables.phase = ELEMENT_FRAGMENT_STATE_CONTENT;
          break;
        case ELEMENT_FRAGMENT_EMPTY_STATE_TAG:
          stateVariables.phase = ELEMENT_FRAGMENT_EMPTY_STATE_CONTENT;
          break;
      }
      return base.wildcardElement(eventTypeIndex, uriId, localNameId, stateVariables);
    }

    public override void chars(EventType eventType, GrammarState stateVariables) {
      throw new InvalidOperationException("char() cannot be invoked on an element fragment grammar.");
    }

    public override void undeclaredChars(int eventTypeIndex, GrammarState stateVariables) {
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
          Debug.Assert(false);
          break;
      }
    }

    public override void miscContent(int eventTypeIndex, GrammarState stateVariables) {
      undeclaredChars(eventTypeIndex, stateVariables);
    }

    public override void end(GrammarState stateVariables) {
    }

    /// <summary>
    /// Create EventTypeSchema(s) from a node in EXISchema.
    /// </summary>
    private EventTypeSchema createAttributeEventType(int attr, bool useRelaxedGrammar, EventTypeList eventTypeList) {
      int uriId = schema.getUriOfAttr(attr);
      int localNameId = schema.getLocalNameOfAttr(attr);
      return new EventTypeSchema(useRelaxedGrammar ? EXISchema.NIL_NODE : schema.getTypeOfAttr(attr), schema.uris[uriId], schema.localNames[uriId][localNameId], uriId, localNameId, EventCode.EVENT_CODE_DEPTH_ONE, eventTypeList, EventType.ITEM_SCHEMA_AT, (EXIGrammar)null);
    }

  }

}