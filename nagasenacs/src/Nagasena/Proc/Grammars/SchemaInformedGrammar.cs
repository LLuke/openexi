using System.Diagnostics;
using System.Collections.Generic;

using EventCode = Nagasena.Proc.Common.EventCode;
using EventType = Nagasena.Proc.Common.EventType;
using EventTypeList = Nagasena.Proc.Common.EventTypeList;
using GrammarOptions = Nagasena.Proc.Common.GrammarOptions;
using IGrammar = Nagasena.Proc.Common.IGrammar;
using XmlUriConst = Nagasena.Proc.Common.XmlUriConst;
using EXISchema = Nagasena.Schema.EXISchema;
using EXISchemaConst = Nagasena.Schema.EXISchemaConst;

namespace Nagasena.Proc.Grammars {

  public abstract class SchemaInformedGrammar : Grammar {

    protected internal const sbyte ELEMENT_FRAGMENT_STATE_BASE = DOCUMENT_STATE_END + 1;
    public const sbyte ELEMENT_FRAGMENT_STATE_TAG = ELEMENT_FRAGMENT_STATE_BASE;
    public const sbyte ELEMENT_FRAGMENT_STATE_CONTENT = ELEMENT_FRAGMENT_STATE_TAG + 1;
    public const sbyte ELEMENT_FRAGMENT_EMPTY_STATE_TAG = ELEMENT_FRAGMENT_STATE_CONTENT + 1;
    public const sbyte ELEMENT_FRAGMENT_EMPTY_STATE_CONTENT = ELEMENT_FRAGMENT_EMPTY_STATE_TAG + 1;

    protected internal SchemaInformedGrammar(sbyte grammarType, GrammarCache stateCache) : base(grammarType, stateCache) {
    }

    public override sealed bool SchemaInformed {
      get {
        return true;
      }
    }

    /// <summary>
    /// Create an EventType instance of item type EventType.ITEM_SCHEMA_AT_INVALID_VALUE.
    /// </summary>
    protected internal EventType createEventTypeSchemaAttributeInvalid(EventType eventTypeSchemaAttribute, EventTypeList eventTypeList) {
      return new EventType(eventTypeSchemaAttribute.uri, eventTypeSchemaAttribute.name, eventTypeSchemaAttribute.URIId, eventTypeSchemaAttribute.NameId, EventCode.EVENT_CODE_DEPTH_THREE, eventTypeList, EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventTypeSchemaAttribute.subsequentGrammar);
    }

    private EventType createEventTypeXsiNil(EventTypeList eventTypeList, IGrammar subsequentGrammar) {
      return new EventType(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, "nil", XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI_ID, EXISchemaConst.XSI_LOCALNAME_NIL_ID, EventCode.EVENT_CODE_DEPTH_TWO, eventTypeList, EventType.ITEM_SCHEMA_NIL, subsequentGrammar);
    }

    private EventType createEventTypeXsiType(EventTypeList eventTypeList) {
      return new EventType(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, "type", XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI_ID, EXISchemaConst.XSI_LOCALNAME_TYPE_ID, EventCode.EVENT_CODE_DEPTH_TWO, eventTypeList, EventType.ITEM_SCHEMA_TYPE, (IGrammar)null);
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Methods computing EventCodeTuple and EventTypes
    ///////////////////////////////////////////////////////////////////////////

    protected internal sealed class EventCodeTupleSink {
      internal EventCodeTuple eventCodeTuple;
      internal EventType[] eventTypes;
      internal void clear() {
        eventCodeTuple = null;
        eventTypes = null;
      }
    }

    protected internal void createEventCodeTuple(List<EventType> schemaEventTypes, short grammarOptions, EventCodeTupleSink @out, EventTypeList eventTypeList) {
      createEventCodeTuple(schemaEventTypes, grammarOptions, @out, (List<EventType>)null, eventTypeList, false, EXISchema.NIL_GRAM, false, -1, -1);
    }

    protected internal void createEventCodeTuple(List<EventType> schemaEventTypes, short grammarOptions, EventCodeTupleSink @out, List<EventType> invalidAttributes, EventTypeList eventTypeList, bool atZero, int nd, bool isElem, int gramTypeEmpty, int gramContent) {
      int gram, elem;
      bool isEmptyGrammar;
      if (isElem) {
        elem = nd; // nd represents an elem when isElem is true
        Debug.Assert(elem != EXISchema.NIL_NODE);
        int tp = schema.getTypeOfElem(nd);
        gram = schema.getGrammarOfType(tp);
        Debug.Assert(gram != EXISchema.NIL_GRAM);
        isEmptyGrammar = false;
      }
      else {
        elem = EXISchema.NIL_NODE;
        gram = nd; // nd represents a gram when isElem is false;
        bool isTypeGrammar = gram != EXISchema.NIL_GRAM;
        if (isTypeGrammar) {
          isEmptyGrammar = gram == gramTypeEmpty;
        }
        else {
          isEmptyGrammar = false;
        }
      }
      if (gramContent == EXISchema.NIL_GRAM) {
        gramContent = gram;
      }
      int n_invalidAttributes = invalidAttributes != null ? invalidAttributes.Count : 0;

      bool addUndeclaredEA = GrammarOptions.isPermitDeviation(grammarOptions);
      Debug.Assert(invalidAttributes == null || addUndeclaredEA); // invalidAttributes is non-null only when addUndeclaredEA is true
      bool addDTD = GrammarOptions.hasDTD(grammarOptions);
      bool addCM = GrammarOptions.hasCM(grammarOptions);
      bool addPI = GrammarOptions.hasPI(grammarOptions);
      bool addNS, addSC, nillable, typable;

      if (atZero) {
        if (grammarType == SCHEMA_GRAMMAR_ELEMENT_FRAGMENT) {
          typable = nillable = true;
        }
        else {
          Debug.Assert(grammarType == SCHEMA_GRAMMAR_ELEMENT_AND_TYPE);
          if (isElem) {
            bool nilTypeRestricted = GrammarOptions.isXsiNilTypeRestricted(grammarOptions);
            typable = !nilTypeRestricted || schema.isTypableType(schema.getTypeOfElem(elem));
            nillable = !nilTypeRestricted || schema.isNillableElement(elem);
          }
          else {
            bool nillableTypable = !GrammarOptions.isXsiNilTypeRestricted(grammarOptions);
            typable = nillableTypable;
            nillable = nillableTypable;
          }
        }
        addNS = GrammarOptions.hasNS(grammarOptions);
        addSC = GrammarOptions.hasSC(grammarOptions);
      }
      else {
        typable = nillable = addNS = addSC = false;
      }

      bool addUndeclaredEE = false;
      EventType undeclaredEE = null;
      EventType eventTypeSchemaType = null;
      EventType eventTypeSchemaNil = null;
      EventType eventTypeNS = null;
      EventType eventTypeSC = null;
      EventType undeclaredWildcardAnyAT = null;
      EventType undeclaredWildcardAnyInvalidAT = null;
      EventType elementWildcard = null;
      EventType untypedCharacters = null;
      EventType entityReference = null;
      EventType comment = null;
      EventType processingInstruction = null;
      bool addTupleL2 = false;
      bool addTupleL3a = false;
      bool addTupleL3b = false;
      int n_itemsL2 = 0;
      int n_itemsL2Null = 0; // To count how many items in tupleL2 are null.
      int n_itemsL3b = 0;
      int i, len;
      len = schemaEventTypes.Count;
      if (addUndeclaredEA) {
        for (i = 0; i < len; i++) {
          EventType eventType = schemaEventTypes[i];
          if (eventType.itemType == EventType.ITEM_EE) {
            break;
          }
        }
        if (i == len) {
          addUndeclaredEE = true;
          undeclaredEE = EventTypeFactory.creatEndElement(EventCode.EVENT_CODE_DEPTH_TWO, eventTypeList);
          ++n_itemsL2;
        }
        elementWildcard = new EventType(EventCode.EVENT_CODE_DEPTH_TWO, eventTypeList, EventType.ITEM_SE_WC, retrieveEXIGrammar(gramContent));
        untypedCharacters = new EventType(EventCode.EVENT_CODE_DEPTH_TWO, eventTypeList, EventType.ITEM_CH, retrieveEXIGrammar(gramContent));
        n_itemsL2 += 2;
        if (invalidAttributes != null) {
          undeclaredWildcardAnyAT = new EventType(EventCode.EVENT_CODE_DEPTH_TWO, eventTypeList, EventType.ITEM_SCHEMA_AT_WC_ANY, (IGrammar)null);
          undeclaredWildcardAnyInvalidAT = new EventType(EventCode.EVENT_CODE_DEPTH_THREE, eventTypeList, EventType.ITEM_AT_WC_ANY_UNTYPED, (IGrammar)null);
          n_itemsL2 += 2;
          addTupleL3a = true;
        }
        addTupleL2 = true;
      }
      if (typable) {
        if (!isEmptyGrammar) {
          eventTypeSchemaType = createEventTypeXsiType(eventTypeList);
        }
        else {
          ++n_itemsL2Null;
        }
        ++n_itemsL2;
        addTupleL2 = true;
      }
      if (nillable) {
        if (!isEmptyGrammar) {
          eventTypeSchemaNil = createEventTypeXsiNil(eventTypeList, retrieveEXIGrammar(gramTypeEmpty));
        }
        else {
          ++n_itemsL2Null;
        }
        ++n_itemsL2;
        addTupleL2 = true;
      }
      if (addNS) {
        eventTypeNS = new EventType(EventCode.EVENT_CODE_DEPTH_TWO, eventTypeList, EventType.ITEM_NS, (IGrammar)null);
        ++n_itemsL2;
        addTupleL2 = true;
      }
      if (addSC) {
        eventTypeSC = new EventType(EventCode.EVENT_CODE_DEPTH_TWO, eventTypeList, EventType.ITEM_SC, (IGrammar)null);
        ++n_itemsL2;
        addTupleL2 = true;
      }
      if (addDTD) {
        entityReference = new EventType(EventCode.EVENT_CODE_DEPTH_TWO, eventTypeList, EventType.ITEM_ER, retrieveEXIGrammar(gramContent));
        ++n_itemsL2;
        addTupleL2 = true;
      }
      if (addCM) {
        comment = new EventType(EventCode.EVENT_CODE_DEPTH_THREE, eventTypeList, EventType.ITEM_CM, retrieveEXIGrammar(gramContent));
        ++n_itemsL3b;
        addTupleL3b = true;
      }
      if (addPI) {
        processingInstruction = new EventType(EventCode.EVENT_CODE_DEPTH_THREE, eventTypeList, EventType.ITEM_PI, retrieveEXIGrammar(gramContent));
        ++n_itemsL3b;
        addTupleL3b = true;
      }
      if (addTupleL3b) {
        ++n_itemsL2;
        addTupleL2 = true;
      }
      ArrayEventCodeTuple eventCodes;
      EventCode[] eventCodeItems;
      EventCode[] eventCodeItemsL2 = null;
      EventCode[] eventCodeItemsL3a = null;
      EventCode[] eventCodeItemsL3b = null;
      ArrayEventCodeTuple tupleL2 = null;
      ArrayEventCodeTuple tupleL3a = null;
      ArrayEventCodeTuple tupleL3b = null;
      int totalLen = n_itemsL3b != 0 ? len + ((n_itemsL2 - n_itemsL2Null) - 1) + n_itemsL3b + n_invalidAttributes: len + (n_itemsL2 - n_itemsL2Null) + n_invalidAttributes;
      eventCodes = new ArrayEventCodeTuple();
      EventType[] eventTypes = new EventType[totalLen];
      eventCodeItems = new EventCode[addTupleL2 ? len + 1 : len];
      int n = 0;
      if (eventTypeSchemaType != null) {
        eventTypes[n++] = eventTypeSchemaType;
      }
      if (eventTypeSchemaNil != null) {
        eventTypes[n++] = eventTypeSchemaNil;
      }
      for (i = 0; i < len; i++) {
        EventType eventType = schemaEventTypes[i];
        eventTypes[n + i] = eventType;
        eventCodeItems[i] = eventType;
      }
      n += len;
      if (addTupleL2) {
        eventCodeItemsL2 = new EventCode[n_itemsL2];
        tupleL2 = new ArrayEventCodeTuple();
        eventCodeItems[len] = tupleL2;
        if (addTupleL3b) {
          eventCodeItemsL3b = new EventCode[n_itemsL3b];
          tupleL3b = new ArrayEventCodeTuple();
          eventCodeItemsL2[n_itemsL2 - 1] = tupleL3b;
        }
      }
      int m = 0, k = 0;
      if (addUndeclaredEE) {
        eventTypes[n++] = undeclaredEE;
        eventCodeItemsL2[m++] = undeclaredEE;
      }
      if (typable) {
        eventCodeItemsL2[m++] = eventTypeSchemaType;
      }
      if (nillable) {
        eventCodeItemsL2[m++] = eventTypeSchemaNil;
      }
      if (addTupleL3a) {
        eventTypes[n++] = undeclaredWildcardAnyAT;
        eventCodeItemsL2[m++] = undeclaredWildcardAnyAT;
        eventCodeItemsL3a = new EventCode[n_invalidAttributes + 1];
        tupleL3a = new ArrayEventCodeTuple();
        eventCodeItemsL2[m++] = tupleL3a;
        for (i = 0; i < n_invalidAttributes; i++) {
          eventTypes[n++] = invalidAttributes[i];
          eventCodeItemsL3a[i] = invalidAttributes[i];
        }
        eventTypes[n++] = undeclaredWildcardAnyInvalidAT;
        eventCodeItemsL3a[n_invalidAttributes] = undeclaredWildcardAnyInvalidAT;
      }
      if (addNS) {
        eventTypes[n++] = eventTypeNS;
        eventCodeItemsL2[m++] = eventTypeNS;
      }
      if (addSC) {
        eventTypes[n++] = eventTypeSC;
        eventCodeItemsL2[m++] = eventTypeSC;
      }
      if (addUndeclaredEA) {
        eventTypes[n++] = elementWildcard;
        eventCodeItemsL2[m++] = elementWildcard;
        eventTypes[n++] = untypedCharacters;
        eventCodeItemsL2[m++] = untypedCharacters;
      }
      if (addDTD) {
        eventTypes[n++] = entityReference;
        eventCodeItemsL2[m++] = entityReference;
      }
      if (addCM) {
        eventTypes[n++] = comment;
        eventCodeItemsL3b[k++] = comment;
      }
      if (addPI) {
        eventTypes[n++] = processingInstruction;
        eventCodeItemsL3b[k++] = processingInstruction;
      }
      Debug.Assert(totalLen == n && (addTupleL3b ? n_itemsL2 - 1: n_itemsL2) == m && n_itemsL3b == k);
      eventCodes.Items = eventCodeItems;
      if (addTupleL2) {
        tupleL2.Items = eventCodeItemsL2;
        if (addTupleL3a) {
          tupleL3a.Items = eventCodeItemsL3a;
        }
        if (addTupleL3b) {
          tupleL3b.Items = eventCodeItemsL3b;
        }
      }

      @out.eventCodeTuple = eventCodes;
      @out.eventTypes = eventTypes;
    }

  }

}