package org.openexi.proc.grammars;

import java.util.ArrayList;

import org.openexi.proc.common.EventCode;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventTypeList;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.common.IGrammar;
import org.openexi.proc.common.XmlUriConst;
import org.openexi.schema.EXISchema;
import org.openexi.schema.EXISchemaConst;

public abstract class SchemaInformedGrammar extends Grammar {

  protected static final byte ELEMENT_FRAGMENT_STATE_BASE = DOCUMENT_STATE_END + 1;
  public static final byte ELEMENT_FRAGMENT_STATE_TAG           = ELEMENT_FRAGMENT_STATE_BASE;
  public static final byte ELEMENT_FRAGMENT_STATE_CONTENT       = ELEMENT_FRAGMENT_STATE_TAG + 1;
  public static final byte ELEMENT_FRAGMENT_EMPTY_STATE_TAG     = ELEMENT_FRAGMENT_STATE_CONTENT + 1;
  public static final byte ELEMENT_FRAGMENT_EMPTY_STATE_CONTENT = ELEMENT_FRAGMENT_EMPTY_STATE_TAG + 1;

  protected SchemaInformedGrammar(byte grammarType, GrammarCache stateCache) {
    super(grammarType, stateCache);
  }

  @Override
  public final boolean isSchemaInformed() {
    return true;
  }

  /**
   * Create an EventType instance of item type EventType.ITEM_SCHEMA_AT_INVALID_VALUE.
   */
  protected final EventType createEventTypeSchemaAttributeInvalid(EventType eventTypeSchemaAttribute, 
    EventTypeList eventTypeList) {
    return new EventType(eventTypeSchemaAttribute.uri, eventTypeSchemaAttribute.name,
        eventTypeSchemaAttribute.getURIId(), eventTypeSchemaAttribute.getNameId(),
        EventCode.EVENT_CODE_DEPTH_THREE, eventTypeList, 
        EventType.ITEM_SCHEMA_AT_INVALID_VALUE, eventTypeSchemaAttribute.subsequentGrammar);
  }
  
  private EventType createEventTypeXsiNil(EventTypeList eventTypeList, IGrammar subsequentGrammar) {
    return new EventType(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, "nil",
        XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI_ID, EXISchemaConst.XSI_LOCALNAME_NIL_ID,
        EventCode.EVENT_CODE_DEPTH_TWO, eventTypeList, EventType.ITEM_SCHEMA_NIL, subsequentGrammar);
  }

  private EventType createEventTypeXsiType(EventTypeList eventTypeList) {
    return new EventType(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, "type",
        XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI_ID, EXISchemaConst.XSI_LOCALNAME_TYPE_ID,
        EventCode.EVENT_CODE_DEPTH_TWO, eventTypeList, EventType.ITEM_SCHEMA_TYPE, (IGrammar)null);
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Methods computing EventCodeTuple and EventTypes
  ///////////////////////////////////////////////////////////////////////////

  protected static final class EventCodeTupleSink {
    EventCodeTuple eventCodeTuple;
    EventType[] eventTypes;
    void clear() {
      eventCodeTuple = null;
      eventTypes = null;
    }
  }

  protected final void createEventCodeTuple(final ArrayList<EventType> schemaEventTypes, 
      final short grammarOptions, final EventCodeTupleSink out, EventTypeList eventTypeList) {
    createEventCodeTuple(schemaEventTypes, grammarOptions, out, 
        (ArrayList<EventType>)null, eventTypeList, false, EXISchema.NIL_GRAM, false,  -1, -1);
  }

  protected final void createEventCodeTuple(final ArrayList<EventType> schemaEventTypes, 
      final short grammarOptions, final EventCodeTupleSink out, 
      final ArrayList<EventType> invalidAttributes, EventTypeList eventTypeList, 
      final boolean atZero, final int nd, boolean isElem, int gramTypeEmpty, int gramContent) {
    final int gram, elem;
    if (isElem) {
      elem = nd; // nd represents an elem when isElem is true
      assert elem != EXISchema.NIL_NODE;
      final int tp = schema.getTypeOfElem(nd);
      gram = schema.getGrammarOfType(tp);
      assert gram != EXISchema.NIL_GRAM;
    }
    else {
      elem = EXISchema.NIL_NODE;
      gram = nd; // nd represents a gram when isElem is false;
    }
    if (gramContent == EXISchema.NIL_GRAM) {
      gramContent = gram;
    }
    final int n_invalidAttributes = invalidAttributes != null ? invalidAttributes.size() : 0;
    
    final boolean addUndeclaredEA = GrammarOptions.isPermitDeviation(grammarOptions);
    assert invalidAttributes == null || addUndeclaredEA; // invalidAttributes is non-null only when addUndeclaredEA is true
    final boolean addDTD = GrammarOptions.hasDTD(grammarOptions);
    final boolean addCM =  GrammarOptions.hasCM(grammarOptions);
    final boolean addPI =  GrammarOptions.hasPI(grammarOptions);
    final boolean addNS, addSC, nillable, typable;
    
    if (atZero) {
      if (grammarType == SCHEMA_GRAMMAR_ELEMENT_FRAGMENT) {
        typable  = nillable = true;
      }
      else {
        assert grammarType == SCHEMA_GRAMMAR_ELEMENT_AND_TYPE;
        if (isElem) {
          final boolean nilTypeRestricted = GrammarOptions.isXsiNilTypeRestricted(grammarOptions);
          typable = !nilTypeRestricted || schema.isTypableType(schema.getTypeOfElem(elem));
          nillable = !nilTypeRestricted || schema.isNillableElement(elem);
        }
        else {
          final boolean nillableTypable = !GrammarOptions.isXsiNilTypeRestricted(grammarOptions);
          typable  = nillableTypable;
          nillable = nillableTypable;
        }
      }
      addNS = GrammarOptions.hasNS(grammarOptions);
      addSC = GrammarOptions.hasSC(grammarOptions);
    }
    else {
      typable = nillable = addNS = addSC = false;
    }
    
    boolean addUndeclaredEE = false;
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
    boolean addTupleL2 = false;
    boolean addTupleL3a = false;
    boolean addTupleL3b = false;
    int n_itemsL2 = 0;
    int n_itemsL3b = 0;
    int i,  len;
    len = schemaEventTypes.size();
    if (addUndeclaredEA) {
      for (i = 0; i < len; i++) {
        final EventType eventType = schemaEventTypes.get(i);
        if (eventType.itemType == EventType.ITEM_EE)
          break;
      }
      if (i == len) {
        addUndeclaredEE = true;
        undeclaredEE = EventTypeFactory.creatEndElement(EventCode.EVENT_CODE_DEPTH_TWO, eventTypeList); 
        ++n_itemsL2;
      }
      elementWildcard = new EventType(EventCode.EVENT_CODE_DEPTH_TWO, eventTypeList, EventType.ITEM_SE_WC, 
          retrieveEXIGrammar(gramContent));
      untypedCharacters = new EventType(EventCode.EVENT_CODE_DEPTH_TWO, eventTypeList, EventType.ITEM_CH, 
          retrieveEXIGrammar(gramContent));
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
      eventTypeSchemaType = createEventTypeXsiType(eventTypeList); 
      ++n_itemsL2;
      addTupleL2 = true;
    }
    if (nillable) {
      eventTypeSchemaNil = createEventTypeXsiNil(eventTypeList, retrieveEXIGrammar(gramTypeEmpty));
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
      entityReference = new EventType(EventCode.EVENT_CODE_DEPTH_TWO, eventTypeList, EventType.ITEM_ER, 
          retrieveEXIGrammar(gramContent));
      ++n_itemsL2;
      addTupleL2 = true;
    }
    if (addCM) {
      comment = new EventType(EventCode.EVENT_CODE_DEPTH_THREE, eventTypeList, EventType.ITEM_CM, 
          retrieveEXIGrammar(gramContent));
      ++n_itemsL3b;
      addTupleL3b = true;
    }
    if (addPI) {
      processingInstruction = new EventType(EventCode.EVENT_CODE_DEPTH_THREE, eventTypeList, EventType.ITEM_PI, 
          retrieveEXIGrammar(gramContent));
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
    final int totalLen = n_itemsL3b != 0 ? 
        len + (n_itemsL2 - 1) + n_itemsL3b + n_invalidAttributes : len + n_itemsL2 + n_invalidAttributes;
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
      final EventType eventType = schemaEventTypes.get(i);
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
      eventCodeItemsL3a =  new EventCode[n_invalidAttributes + 1];
      tupleL3a = new ArrayEventCodeTuple();
      eventCodeItemsL2[m++] = tupleL3a;
      for (i = 0; i < n_invalidAttributes; i++) {
        eventTypes[n++] = invalidAttributes.get(i);
        eventCodeItemsL3a[i] = invalidAttributes.get(i);
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
    assert totalLen == n && (addTupleL3b ? n_itemsL2 - 1 : n_itemsL2) == m && n_itemsL3b == k;
    eventCodes.setItems(eventCodeItems);
    if (addTupleL2) {
      tupleL2.setItems(eventCodeItemsL2);
      if (addTupleL3a) {
        tupleL3a.setItems(eventCodeItemsL3a);
      }
      if (addTupleL3b) {
        tupleL3b.setItems(eventCodeItemsL3b);
      }
    }
    
    out.eventCodeTuple = eventCodes;
    out.eventTypes = eventTypes;
  }

}
