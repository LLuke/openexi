using System;
using System.Diagnostics;

using EventCode = Nagasena.Proc.Common.EventCode;
using EventType = Nagasena.Proc.Common.EventType;
using EventTypeList = Nagasena.Proc.Common.EventTypeList;
using GrammarOptions = Nagasena.Proc.Common.GrammarOptions;
using IGrammar = Nagasena.Proc.Common.IGrammar;
using StringTable = Nagasena.Proc.Common.StringTable;

namespace Nagasena.Proc.Grammars {

  internal sealed class BuiltinFragmentGrammar : BuiltinGrammar {

    private readonly EventTypeList[] m_eventTypeLists;
    private readonly EventCodeTuple[] m_eventCodes;

    private static readonly EventType[] m_eventTypesInit;
    static BuiltinFragmentGrammar() {
      m_eventTypesInit = new EventType[N_NONSCHEMA_ITEMS];
      for (int i = 0; i < N_NONSCHEMA_ITEMS; i++) {
        m_eventTypesInit[i] = null;
      }
    }

    /// <summary>
    /// For exclusive use by GrammarCache only.
    /// GrammarCache calls this method to instantiate a template grammar.
    /// </summary>
    internal BuiltinFragmentGrammar(GrammarCache grammarCache) : base(BUILTIN_GRAMMAR_FRAGMENT, grammarCache) {

      short grammarOptions = grammarCache.grammarOptions;

      m_eventTypeLists = new EventTypeList[2];
      m_eventCodes = new EventCodeTuple[2];

      ArrayEventTypeList eventTypeList;
      ArrayEventCodeTuple tuple;

      m_eventTypeLists[0] = eventTypeList = new ArrayEventTypeList();
      m_eventCodes[0] = tuple = new ArrayEventCodeTuple();

      EventType[] eventTypes = new EventType[] { EventTypeFactory.createStartDocument(eventTypeList) };
      eventTypeList.Items = eventTypes;
      tuple.Items = new EventType[] { eventTypes[0] };

      populateContentGrammar(grammarOptions);
    }

    public override void init(GrammarState stateVariables) {
      stateVariables.targetGrammar = this;
      stateVariables.phase = DOCUMENT_STATE_CREATED;
    }

    /// <summary>
    /// For exclusive use by GrammarCache only.
    /// GrammarCache calls this method to instantiate a new BuiltinElementGrammar
    /// from a template grammar.
    /// </summary>
    internal BuiltinFragmentGrammar duplicate(EventType[] eventTypes) {
      return new BuiltinFragmentGrammar(m_grammarCache, m_eventTypeLists, m_eventCodes, eventTypes);
    }

    /// <summary>
    /// Used only by duplicate() method above.
    /// </summary>
    private BuiltinFragmentGrammar(GrammarCache grammarCache, EventTypeList[] sourceEventTypeList, EventCodeTuple[] sourceEventCodes, EventType[] eventTypes) : base(BUILTIN_GRAMMAR_FRAGMENT, grammarCache) {

      m_eventTypeLists = new EventTypeList[2];
      m_eventCodes = new EventCodeTuple[2];

      m_eventTypeLists[0] = sourceEventTypeList[0];
      m_eventCodes[0] = sourceEventCodes[0];

      ReversedEventTypeList reversedEventTypeList = new ReversedEventTypeList();
      ReverseEventCodeTuple reverseEventCodeTuple = new ReverseEventCodeTuple();

      m_eventTypeLists[1] = reversedEventTypeList;
      m_eventCodes[1] = reverseEventCodeTuple;

      cloneContentGrammar(this, (ReverseEventCodeTuple)sourceEventCodes[1], reversedEventTypeList, reverseEventCodeTuple, eventTypes);
    }

    private void populateContentGrammar(short grammarOptions) {

      ReversedEventTypeList eventList = new ReversedEventTypeList();
      ReverseEventCodeTuple eventCodes = new ReverseEventCodeTuple();

      m_eventTypeLists[1] = eventList;
      m_eventCodes[1] = eventCodes;

      /*
       * FragmentContent :
       *   SE (*) FragmentContent 0
       *   ED                     1
       *   CM FragmentContent     2.0 (if addCM)
       *   PI FragmentContent     2.1 (if addPI)
       */

      bool addCM = GrammarOptions.hasCM(grammarOptions);
      bool addPI = GrammarOptions.hasPI(grammarOptions);

      EventType elementWildcard;
      EventType endDocument;
      EventType comment;
      EventType processingInstruction;

      bool addTupleL2 = false;
      int n_itemsL2 = 0;

      if (addPI) {
        processingInstruction = new EventType(EventCode.EVENT_CODE_DEPTH_TWO, eventList, EventType.ITEM_PI, (IGrammar)null);
        eventList.add(processingInstruction);
        ++n_itemsL2;
        addTupleL2 = true;
      }
      else {
        processingInstruction = null;
      }
      if (addCM) {
        comment = new EventType(EventCode.EVENT_CODE_DEPTH_TWO, eventList, EventType.ITEM_CM, (IGrammar)null);
        eventList.add(comment);
        ++n_itemsL2;
        addTupleL2 = true;
      }
      else {
        comment = null;
      }
      endDocument = EventTypeFactory.createEndDocument(eventList);
      eventList.add(endDocument);
      elementWildcard = new EventType(EventCode.EVENT_CODE_DEPTH_ONE, eventList, EventType.ITEM_SE_WC, (IGrammar)null);
      eventList.add(elementWildcard);

      EventCode[] eventCodeItemsL2 = null;
      ArrayEventCodeTuple tupleL2 = null;
      if (addTupleL2) {
        eventCodeItemsL2 = new EventCode[n_itemsL2];
        tupleL2 = new ArrayEventCodeTuple();
      }
      eventCodes.setInitialItems(elementWildcard, endDocument, tupleL2);
      if (addTupleL2) {
        int m = 0;
        if (addCM) {
          eventCodeItemsL2[m++] = comment;
        }
        if (addPI) {
          eventCodeItemsL2[m++] = processingInstruction;
        }
        tupleL2.Items = eventCodeItemsL2;
      }
    }

    private void cloneContentGrammar(Grammar ownerGrammar, ReverseEventCodeTuple sourceEventCodes, ReversedEventTypeList eventList, ReverseEventCodeTuple eventCodes, EventType[] eventTypes) {

      Array.Copy(m_eventTypesInit, 0, eventTypes, 0, N_NONSCHEMA_ITEMS);

      Debug.Assert(sourceEventCodes.itemsCount == 2 || sourceEventCodes.itemsCount == 3 && sourceEventCodes.getItem(2).itemType == EventType.ITEM_TUPLE);

      /*
       * FragmentContent :
       *   SE (*) FragmentContent 0
       *   ED                     1
       *   CM FragmentContent     2.0 (if addCM)
       *   PI FragmentContent     2.1 (if addPI)
       */

      bool addTupleL2 = sourceEventCodes.itemsCount == 3;

      EventCodeTuple sourceTupleL2;
      int n_itemsL2;
      EventCode[] eventCodeItemsL2;
      ArrayEventCodeTuple tupleL2;

      if (addTupleL2) {
        sourceTupleL2 = (EventCodeTuple)sourceEventCodes.getItem(2);
        n_itemsL2 = sourceTupleL2.itemsCount;
        eventCodeItemsL2 = new EventCode[n_itemsL2];
        tupleL2 = new ArrayEventCodeTuple();
      }
      else {
        sourceTupleL2 = null;
        n_itemsL2 = 0;
        eventCodeItemsL2 = null;
        tupleL2 = null;
      }

      EventType elementWildcard;
      EventType endDocument;
      endDocument = EventTypeFactory.createEndDocument(eventList);
      eventTypes[EventType.ITEM_ED] = endDocument;
      elementWildcard = new EventType(EventCode.EVENT_CODE_DEPTH_ONE, eventList, EventType.ITEM_SE_WC, (IGrammar)null);
      eventTypes[EventType.ITEM_SE_WC] = elementWildcard;
      eventCodes.setInitialItems(elementWildcard, endDocument, tupleL2);

      int i;
      if (addTupleL2) {
        for (i = 0; i < n_itemsL2; i++) {
          EventCode ithSourceItem = sourceTupleL2.getItem(i);
          EventType eventType = duplicate(((EventType)ithSourceItem), eventList);
          eventCodeItemsL2[i] = eventType;
          eventTypes[eventType.itemType] = eventType;
        }
        tupleL2.Items = eventCodeItemsL2;
      }

      for (i = 0; i < N_NONSCHEMA_ITEMS; i++) {
        EventType ith = eventTypes[i];
        if (ith != null) {
          eventList.add(ith);
        }
      }
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Method implementations for event processing
    ///////////////////////////////////////////////////////////////////////////

    internal override EventCodeTuple getNextEventCodes(GrammarState stateVariables) {
      sbyte phase;
      return (phase = stateVariables.phase) == DOCUMENT_STATE_COMPLETED ? m_eventCodes[1] : phase == DOCUMENT_STATE_CREATED ? m_eventCodes[0] : null;
    }

    internal override EventTypeList getNextEventTypes(GrammarState stateVariables) {
      sbyte phase;
      return (phase = stateVariables.phase) == DOCUMENT_STATE_COMPLETED ? m_eventTypeLists[1] : phase == DOCUMENT_STATE_CREATED ? m_eventTypeLists[0] : EventTypeList.EMPTY;
    }

    public override void startDocument(GrammarState stateVariables) {
      Debug.Assert(stateVariables.phase == DOCUMENT_STATE_CREATED);
      stateVariables.phase = DOCUMENT_STATE_COMPLETED;
    }

    public override void endDocument(GrammarState stateVariables) {
      Debug.Assert(stateVariables.phase == DOCUMENT_STATE_COMPLETED);
      stateVariables.phase = DOCUMENT_STATE_END;
    }

    public override void element(EventType eventType, GrammarState stateVariables) {
      Debug.Assert(stateVariables.phase == DOCUMENT_STATE_COMPLETED && eventType.itemType == EventType.ITEM_SE);
      Grammar ensuingGrammar = ((EventTypeElement)eventType).ensuingGrammar;
      ensuingGrammar.init(stateVariables.apparatus.pushState());
    }

    public override void undeclaredChars(int eventTypeIndex, GrammarState stateVariables) {
      Debug.Assert(false);
    }

    public override void end(GrammarState stateVariables) {
      Debug.Assert(false);
    }

    /// <summary>
    /// Signals CM, PI or ER event. 
    /// </summary>
    public override void miscContent(int eventTypeIndex, GrammarState stateVariables) {
      Debug.Assert(stateVariables.phase == DOCUMENT_STATE_COMPLETED);
    }

    internal override void xsitp(int tp, GrammarState stateVariables) {
      Debug.Assert(false);
    }

    internal override Grammar wildcardElement(int eventTypeIndex, int uriId, int localNameId, GrammarState stateVariables) {
      Debug.Assert(stateVariables.phase == DOCUMENT_STATE_COMPLETED);
      Grammar ensuingGrammar = base.wildcardElement(eventTypeIndex, uriId, localNameId, stateVariables);
      StringTable uriPartition = stateVariables.apparatus.stringTable;
      string uri = uriPartition.getURI(uriId);
      string name = uriPartition.getLocalNamePartition(uriId).localNameEntries[localNameId].localName;
      EventTypeElement eventTypeElement = new EventTypeElement(uriId, uri, localNameId, name, m_eventTypeLists[1], ensuingGrammar, (IGrammar)null);
      ((ReversedEventTypeList)m_eventTypeLists[1]).add(eventTypeElement);
      ((ReverseEventCodeTuple)m_eventCodes[1]).addItem(eventTypeElement);
      return ensuingGrammar;
    }

  }

}