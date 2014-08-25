using System;
using System.Diagnostics;

using EventCode = Nagasena.Proc.Common.EventCode;
using EventType = Nagasena.Proc.Common.EventType;
using EventTypeList = Nagasena.Proc.Common.EventTypeList;
using GrammarOptions = Nagasena.Proc.Common.GrammarOptions;
using IGrammar = Nagasena.Proc.Common.IGrammar;

namespace Nagasena.Proc.Grammars {

  internal sealed class FragmentGrammar : Grammar {

    ///////////////////////////////////////////////////////////////////////////
    /// immutables
    ///////////////////////////////////////////////////////////////////////////

    private readonly int[] m_fragmentElems;
    private readonly EventType[][] m_eventTypes;
    private readonly EventCodeTuple[] m_eventCodes;
    private readonly ArrayEventTypeList[] m_eventTypeLists;

    ///////////////////////////////////////////////////////////////////////////
    /// containers with variable contents
    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    /// Constructors, initializers
    ///////////////////////////////////////////////////////////////////////////

    internal FragmentGrammar(GrammarCache grammarCache) : base(SCHEMA_GRAMMAR_FRAGMENT, grammarCache) {

      short grammarOptions = grammarCache.grammarOptions;

      int n_fragmentElems = schema != null ? schema.FragmentElemCount : 0;

      m_eventTypes = new EventType[2][];
      m_eventCodes = new EventCodeTuple[2];
      m_eventTypeLists = new ArrayEventTypeList[2];

      int i;
      for (i = 0; i < 2; i++) {
        m_eventTypeLists[i] = new ArrayEventTypeList();
      }

      bool addTupleL2;
      int n_eventTypes, n_items, n_itemsL2;
      bool addCM, addPI;
      EventType[] eventTypes;
      ArrayEventCodeTuple tuple, tupleL2;
      EventCode[] eventCodeItems, eventCodeItemsL2;

      eventTypes = new EventType[1];
      eventTypes[0] = EventTypeFactory.createStartDocument(m_eventTypeLists[0]);
      tuple = new ArrayEventCodeTuple();
      tuple.Items = new EventType[] { eventTypes[0] };
      m_eventCodes[0] = tuple;
      m_eventTypes[0] = eventTypes;

      addTupleL2 = false;
      tupleL2 = null;
      n_items = n_fragmentElems + 2; // account for SE(*) and ED
      n_itemsL2 = 0;
      if (addCM = GrammarOptions.hasCM(grammarOptions)) {
        ++n_itemsL2;
      }
      if (addPI = GrammarOptions.hasPI(grammarOptions)) {
        ++n_itemsL2;
      }
      if (n_itemsL2 != 0) {
        addTupleL2 = true;
        ++n_items;
      }
      m_fragmentElems = schema != null ? schema.FragmentINodes : null;
      n_eventTypes = addTupleL2 ? n_itemsL2 + n_items - 1 : n_items;
      eventTypes = new EventType[n_eventTypes];
      eventCodeItems = new EventCode[n_items];
      eventCodeItemsL2 = null;
      int n;
      for (n = 0; n < n_fragmentElems; n++) {
        bool isSpecific = true;
        int fragmentElem;
        if (((fragmentElem = m_fragmentElems[n]) & 0x80000000) != 0) {
          isSpecific = false;
          fragmentElem = ~fragmentElem;
        }
        EXIGrammarUse ensuingGrammar = null;
        if (isSpecific) {
          ensuingGrammar = grammarCache.exiGrammarUses[schema.getSerialOfElem(fragmentElem)];
        }
        int uriId = schema.getUriOfElem(fragmentElem);
        int localNameId = schema.getLocalNameOfElem(fragmentElem);
        EventType @event = EventTypeFactory.createStartElement(uriId, localNameId, schema.uris[uriId], schema.localNames[uriId][localNameId], 
          m_eventTypeLists[1], ensuingGrammar);
        eventCodeItems[n] = eventTypes[n] = @event;
      }
      eventCodeItems[n] = eventTypes[n] = new EventType(EventCode.EVENT_CODE_DEPTH_ONE, m_eventTypeLists[1], EventType.ITEM_SE_WC, (IGrammar)null);
      ++n;
      eventCodeItems[n] = eventTypes[n] = EventTypeFactory.createEndDocument(m_eventTypeLists[1]);
      ++n;
      if (addTupleL2) {
        eventCodeItemsL2 = new EventCode[n_itemsL2];
        tupleL2 = new ArrayEventCodeTuple();
        eventCodeItems[n] = tupleL2;
        int m = 0;
        if (addCM) {
          EventType comment = new EventType(EventCode.EVENT_CODE_DEPTH_TWO, m_eventTypeLists[1], EventType.ITEM_CM, (IGrammar)null);
          eventTypes[n++] = comment;
          eventCodeItemsL2[m++] = comment;
        }
        if (addPI) {
          EventType pi = new EventType(EventCode.EVENT_CODE_DEPTH_TWO, m_eventTypeLists[1], EventType.ITEM_PI, (IGrammar)null);
          eventTypes[n++] = pi;
          eventCodeItemsL2[m++] = pi;
        }
      }
      tuple = new ArrayEventCodeTuple();
      if (eventTypes.Length > 0) {
        tuple.Items = eventCodeItems;
        if (addTupleL2) {
          tupleL2.Items = eventCodeItemsL2;
        }
      }
      m_eventCodes[1] = tuple;
      m_eventTypes[1] = eventTypes;

      for (i = 0; i < 2; i++) {
        m_eventTypeLists[i].Items = m_eventTypes[i];
      }
    }

    public override void init(GrammarState stateVariables) {
      stateVariables.targetGrammar = this;
      stateVariables.phase = DOCUMENT_STATE_CREATED;
    }

    public override bool SchemaInformed {
      get {
        return true;
      }
    }

    public override void startDocument(GrammarState stateVariables) {
      Debug.Assert(stateVariables.phase == DOCUMENT_STATE_CREATED);
      stateVariables.phase = DOCUMENT_STATE_COMPLETED;
    }

    public override void endDocument(GrammarState stateVariables) {
      if (stateVariables.phase == DOCUMENT_STATE_COMPLETED) {
        stateVariables.phase = DOCUMENT_STATE_END;
      }
    }

    internal override void xsitp(int tp, GrammarState stateVariables) {
      throw new InvalidOperationException();
    }

    internal override void nillify(int eventTypeIndex, GrammarState stateVariables) {
      throw new InvalidOperationException();
    }

    public override void chars(EventType eventType, GrammarState stateVariables) {
      throw new InvalidOperationException();
    }

    public override void undeclaredChars(int eventTypeIndex, GrammarState stateVariables) {
      throw new InvalidOperationException();
    }

    public override void miscContent(int eventTypeIndex, GrammarState stateVariables) {
      Debug.Assert(stateVariables.phase == DOCUMENT_STATE_DEPLETE || stateVariables.phase == DOCUMENT_STATE_COMPLETED);
    }

    /// <summary>
    /// It is considered to be a well-formedness violation if this method is
    /// ever called.
    /// </summary>
    public override void end(GrammarState stateVariables) {
      throw new InvalidOperationException();
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Accessors
    ///////////////////////////////////////////////////////////////////////////

    internal GrammarCache StateCache {
      get {
        return m_grammarCache;
      }
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Implementation of abstract methods declared in CommonState
    ///////////////////////////////////////////////////////////////////////////

    internal override EventTypeList getNextEventTypes(GrammarState stateVariables) {
      switch (stateVariables.phase) {
        case DOCUMENT_STATE_CREATED:
          return m_eventTypeLists[0];
        case DOCUMENT_STATE_COMPLETED:
          return m_eventTypeLists[1];
        case DOCUMENT_STATE_END:
          return EventTypeList.EMPTY;
        default:
          Debug.Assert(false);
          break;
      }
      return null;
    }

    internal override EventCodeTuple getNextEventCodes(GrammarState stateVariables) {
      switch (stateVariables.phase) {
        case DOCUMENT_STATE_CREATED:
          return m_eventCodes[0];
        case DOCUMENT_STATE_COMPLETED:
          return m_eventCodes[1];
        default:
          Debug.Assert(stateVariables.phase == DOCUMENT_STATE_END);
          return null;
      }
    }

    public override void element(EventType eventType, GrammarState stateVariables) {
      Debug.Assert(stateVariables.phase == DOCUMENT_STATE_COMPLETED);
      Grammar grammar;
      grammar = (grammar = ((EventTypeElement)eventType).ensuingGrammar) != null ? grammar : m_grammarCache.elementFragmentGrammar;
      grammar.init(stateVariables.apparatus.pushState());
    }

    internal override Grammar wildcardElement(int eventTypeIndex, int uriId, int localNameId, GrammarState stateVariables) {
      return stateVariables.phase != DOCUMENT_STATE_COMPLETED ? null : base.wildcardElement(eventTypeIndex, uriId, localNameId, stateVariables);
    }

  }

}