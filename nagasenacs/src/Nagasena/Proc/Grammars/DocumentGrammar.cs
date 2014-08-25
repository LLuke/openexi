using System;
using System.Diagnostics;

using EventCode = Nagasena.Proc.Common.EventCode;
using EventType = Nagasena.Proc.Common.EventType;
using EventTypeList = Nagasena.Proc.Common.EventTypeList;
using GrammarOptions = Nagasena.Proc.Common.GrammarOptions;
using IGrammar = Nagasena.Proc.Common.IGrammar;

namespace Nagasena.Proc.Grammars {

  internal sealed class DocumentGrammar : Grammar {

    ///////////////////////////////////////////////////////////////////////////
    /// immutables
    ///////////////////////////////////////////////////////////////////////////

    private readonly int[] m_elems;
    private readonly EventType[][] m_eventTypes;
    private readonly EventCodeTuple[] m_eventCodes;
    private readonly ArrayEventTypeList[] m_eventTypeLists;

    ///////////////////////////////////////////////////////////////////////////
    /// containers with variable contents
    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    /// Constructors, initializers
    ///////////////////////////////////////////////////////////////////////////

    internal DocumentGrammar(GrammarCache grammarCache) : base(SCHEMA_GRAMMAR_DOCUMENT, grammarCache) {

      short grammarOptions = grammarCache.grammarOptions;

      int n_elems = schema != null ? schema.GlobalElemCountOfSchema : 0;

      m_eventTypes = new EventType[3][];
      m_eventCodes = new EventCodeTuple[3];
      m_eventTypeLists = new ArrayEventTypeList[3];

      int i;
      for (i = 0; i < 3; i++) {
        m_eventTypeLists[i] = new ArrayEventTypeList();
      }

      bool addTupleL2, addTupleL3;
      int n_eventTypes, n_items, n_itemsL2, n_itemsL3;
      bool addDTD, addCM, addPI;
      EventType[] eventTypes;
      ArrayEventCodeTuple tuple, tupleL2, tupleL3;
      EventCode[] eventCodeItems, eventCodeItemsL2;

      eventTypes = new EventType[1];
      eventTypes[0] = EventTypeFactory.createStartDocument(m_eventTypeLists[0]);
      tuple = new ArrayEventCodeTuple();
      tuple.Items = new EventType[] { eventTypes[0] };
      m_eventCodes[0] = tuple;
      m_eventTypes[0] = eventTypes;

      addTupleL2 = addTupleL3 = false;
      tupleL2 = tupleL3 = null;
      n_items = n_elems + 1;
      n_itemsL2 = n_itemsL3 = 0;
      if (addDTD = GrammarOptions.hasDTD(grammarOptions)) {
        ++n_itemsL2;
      }
      if (addCM = GrammarOptions.hasCM(grammarOptions)) {
        ++n_itemsL3;
      }
      if (addPI = GrammarOptions.hasPI(grammarOptions)) {
        ++n_itemsL3;
      }
      if (n_itemsL3 != 0) {
        addTupleL3 = true;
        ++n_itemsL2;
      }
      if (n_itemsL2 != 0) {
        addTupleL2 = true;
        ++n_items;
      }
      m_elems = new int[n_elems];
      n_eventTypes = addTupleL3 ? n_itemsL3 + n_itemsL2 + n_items - 2 : addTupleL2 ? n_itemsL2 + n_items - 1 : n_items;
      eventTypes = new EventType[n_eventTypes];
      eventCodeItems = new EventCode[n_items];
      eventCodeItemsL2 = null;
      EventCode[] eventCodeItemsL3 = null;
      int n;
      for (n = 0; n < n_elems; n++) {
        m_elems[n] = schema.getGlobalElemOfSchema(n);
        int elem = m_elems[n];
        EXIGrammarUse ensuingGrammar = grammarCache.exiGrammarUses[schema.getSerialOfElem(elem)];
        EventType @event;
        int uriId = schema.getUriOfElem(elem);
        int localNameId = schema.getLocalNameOfElem(elem);
        @event = EventTypeFactory.createStartElement(uriId, localNameId, schema.uris[uriId], schema.localNames[uriId][localNameId], 
          m_eventTypeLists[1], ensuingGrammar);
        eventCodeItems[n] = eventTypes[n] = @event;
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
            EventType comment;
            comment = new EventType(EventCode.EVENT_CODE_DEPTH_THREE, m_eventTypeLists[1], EventType.ITEM_CM, (IGrammar)null);
            eventTypes[n++] = comment;
            eventCodeItemsL3[k++] = comment;
          }
          if (addPI) {
            EventType pi;
            pi = new EventType(EventCode.EVENT_CODE_DEPTH_THREE, m_eventTypeLists[1], EventType.ITEM_PI, (IGrammar)null);
            eventTypes[n++] = pi;
            eventCodeItemsL3[k++] = pi;
          }
        }
      }
      tuple = new ArrayEventCodeTuple();
      if (eventTypes.Length > 0) {
        tuple.Items = eventCodeItems;
        if (addTupleL2) {
          tupleL2.Items = eventCodeItemsL2;
          if (addTupleL3) {
            tupleL3.Items = eventCodeItemsL3;
          }
        }
      }
      m_eventCodes[1] = tuple;
      m_eventTypes[1] = eventTypes;

      addTupleL2 = false;
      tupleL2 = null;
      n_items = 1;
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
          EventType comment;
          comment = new EventType(EventCode.EVENT_CODE_DEPTH_TWO, m_eventTypeLists[2], EventType.ITEM_CM, (IGrammar)null);
          eventTypes[n++] = comment;
          eventCodeItemsL2[m++] = comment;
        }
        if (addPI) {
          EventType pi;
          pi = new EventType(EventCode.EVENT_CODE_DEPTH_TWO, m_eventTypeLists[2], EventType.ITEM_PI, (IGrammar)null);
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
  //    tuple.setItems(new EventType[] { eventTypes[0] });
      m_eventCodes[2] = tuple;
      m_eventTypes[2] = eventTypes;

      for (i = 0; i < 3; i++) {
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
      stateVariables.phase = DOCUMENT_STATE_DEPLETE;
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
    /// Implementation of abstract methods declared in CommonState
    ///////////////////////////////////////////////////////////////////////////

    internal override EventTypeList getNextEventTypes(GrammarState stateVariables) {
      switch (stateVariables.phase) {
        case DOCUMENT_STATE_CREATED:
          return m_eventTypeLists[0];
        case DOCUMENT_STATE_DEPLETE:
          return m_eventTypeLists[1];
        case DOCUMENT_STATE_COMPLETED:
          return m_eventTypeLists[2];
        case DOCUMENT_STATE_END:
          return new EventTypeListAnonymousInnerClassHelper(this);
        default:
          Debug.Assert(false);
          break;
      }
      return null;
    }

    private class EventTypeListAnonymousInnerClassHelper : EventTypeList {
      private readonly DocumentGrammar outerInstance;

      public EventTypeListAnonymousInnerClassHelper(DocumentGrammar outerInstance) : base(false) {
        this.outerInstance = outerInstance;
      }

      public override int Length {
        get {
          return 0;
        }
      }
      public override EventType item(int i) {
        Debug.Assert(false);
        return null;
      }
      public override EventType SD {
        get {
          return null;
        }
      }
      public override EventType EE {
        get {
          return null;
        }
      }
      public override EventType getSchemaAttribute(string uri, string name) {
        return null;
      }
      public override EventType getSchemaAttributeInvalid(string uri, string name) {
        return null;
      }
      public override EventType getLearnedAttribute(string uri, string name) {
        return null;
      }
      public override EventType SchemaAttributeWildcardAny {
        get {
          return null;
        }
      }
      public override EventType AttributeWildcardAnyUntyped {
        get {
          return null;
        }
      }
      public override EventType getSchemaAttributeWildcardNS(string uri) {
        return null;
      }
      public override EventType SchemaCharacters {
        get {
          return (EventType)null;
        }
      }
      public override EventType Characters {
        get {
          return (EventType)null;
        }
      }
      public override EventType NamespaceDeclaration {
        get {
          return (EventType)null;
        }
      }
    }

    internal override EventCodeTuple getNextEventCodes(GrammarState stateVariables) {
      switch (stateVariables.phase) {
        case DOCUMENT_STATE_CREATED:
          return m_eventCodes[0];
        case DOCUMENT_STATE_DEPLETE:
          return m_eventCodes[1];
        case DOCUMENT_STATE_COMPLETED:
          return m_eventCodes[2];
        default:
          Debug.Assert(stateVariables.phase == DOCUMENT_STATE_END);
          return null;
      }
    }

    public override void element(EventType eventType, GrammarState stateVariables) {
      Debug.Assert(stateVariables.phase == DOCUMENT_STATE_DEPLETE);
      stateVariables.phase = DOCUMENT_STATE_COMPLETED;
      (((EventTypeElement)eventType).ensuingGrammar).init(stateVariables.apparatus.pushState());
    }

    internal override Grammar wildcardElement(int eventTypeIndex, int uriId, int localNameId, GrammarState stateVariables) {
      Debug.Assert(stateVariables.phase == DOCUMENT_STATE_DEPLETE);
      stateVariables.phase = DOCUMENT_STATE_COMPLETED;
      return base.wildcardElement(eventTypeIndex, uriId, localNameId, stateVariables);
    }

  }

}