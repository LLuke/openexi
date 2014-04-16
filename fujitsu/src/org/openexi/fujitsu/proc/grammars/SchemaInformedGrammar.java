package org.openexi.fujitsu.proc.grammars;

import java.util.ArrayList;

import org.openexi.fujitsu.proc.common.EventCode;
import org.openexi.fujitsu.proc.common.EventType;
import org.openexi.fujitsu.proc.common.EventTypeList;
import org.openexi.fujitsu.proc.common.GrammarOptions;
import org.openexi.fujitsu.schema.EXISchema;

public abstract class SchemaInformedGrammar extends Grammar {

  protected static final byte ELEMENT_FRAGMENT_STATE_BASE = DOCUMENT_STATE_END + 1;
  public static final byte ELEMENT_FRAGMENT_STATE_TAG           = ELEMENT_FRAGMENT_STATE_BASE;
  public static final byte ELEMENT_FRAGMENT_STATE_CONTENT       = ELEMENT_FRAGMENT_STATE_TAG + 1;
  public static final byte ELEMENT_FRAGMENT_EMPTY_STATE_TAG     = ELEMENT_FRAGMENT_STATE_CONTENT + 1;
  public static final byte ELEMENT_FRAGMENT_EMPTY_STATE_CONTENT = ELEMENT_FRAGMENT_EMPTY_STATE_TAG + 1;
  public static final byte ELEMENT_FRAGMENT_BOUND               = ELEMENT_FRAGMENT_EMPTY_STATE_CONTENT + 1;

  private static final byte ELEMENT_STATE_BASE = ELEMENT_FRAGMENT_BOUND + 1;
  // SchemaInformedElementGrammar
  public static final byte ELEMENT_STATE_UNBOUND  = ELEMENT_STATE_BASE;
  public static final byte ELEMENT_STATE_BOUND    = ELEMENT_STATE_UNBOUND + 1;
  // ElementTagGrammar
  public static final byte ELEMENT_STATE_TAG     = ELEMENT_STATE_BOUND + 1;
  protected static final byte ELEMENT_STATE_CONTENT = ELEMENT_STATE_TAG + 1;

  private static final byte ELEMENT_STATE_CONTENT_BASE = ELEMENT_STATE_CONTENT + 1;
  // ElementContentGrammar
  protected static final byte ELEMENT_STATE_CONTENT_DEPLETE     = ELEMENT_STATE_CONTENT_BASE;
  protected static final byte ELEMENT_STATE_CONTENT_ACCEPTED    = ELEMENT_STATE_CONTENT_DEPLETE + 1;
  public static final byte ELEMENT_STATE_CONTENT_COMPLETE    = ELEMENT_STATE_CONTENT_ACCEPTED + 1;


  protected final int[] m_nodes;
  protected final String[] m_names;

  /**
  * schema node (if STATE_DOCUMENT), element node (if STATE_ELEMENT),
  * group node (if STATE_GROUP)
  */
  final int   m_nd;
  
  protected SchemaInformedGrammar(int nd, byte grammarType, GrammarCache stateCache) {
    super(grammarType, stateCache);
    m_nd = nd;
    if (m_schema != null) {
      m_nodes  = m_schema.getNodes();
      m_names  = m_schema.getNames();
    }
    else {
      m_nodes  = null;
      m_names  = null;
    }
  }

  @Override
  public final boolean isSchemaInformed() {
    return true;
  }

  final int getNode() {
    return m_nd;
  }
  
  abstract void schemaAttribute(int eventTypeIndex, String uri, String name, GrammarState stateVariables);

  protected final boolean schemaElement(final EventTypeSchema schemaEventType, String uri, String name, GrammarState stateVariables) {
    EventTypeList eventTypeList = getNextEventTypes(stateVariables);
    for (int i = 0; i < eventTypeList.getLength(); i++) {
      EventType ith = eventTypeList.item(i);
      if (ith.isSchemaInformed() && schemaEventType.getSchemaSubstance() == ((EventTypeSchema)ith).getSchemaSubstance()) {
        element(i, uri, name, stateVariables);
        return true;
      }
    }
    return false;
  }

  /**
   * Sort particles in EXI schema order.
   * @param initials the location where initials start in m_nodes
   * @param the number of initials
   */
  protected void sortInitials(int[] sortedInitials, int initials, int n_initials) {
    assert sortedInitials.length == n_initials; // caller is responsible for allocation
    assert initials != EXISchema.NIL_NODE;
    int i;
    for (i = 0; i < n_initials; i++) {
      sortedInitials[i] = i;
    }
    for (i = 0; i < n_initials - 1; i++) {
      for (int j = i + 1; j < n_initials; j++) {
        final int particle_j;
        if ((particle_j = m_nodes[initials + sortedInitials[j]]) == EXISchema.NIL_NODE) {
          assert j == n_initials - 1;
          break;
        }
        final int particle_i, term_i, sn_i;
        particle_i = m_nodes[initials + sortedInitials[i]];
        assert particle_i != EXISchema.NIL_NODE;
        term_i = m_schema.getTermOfParticle(particle_i);
        sn_i = m_schema.getSerialInTypeOfParticle(particle_i);

        final int term_j, sn_j;
        term_j = m_schema.getTermOfParticle(particle_j);
        sn_j = m_schema.getSerialInTypeOfParticle(particle_j);
        
        if (!isAscending(particle_i, term_i, sn_i, particle_j, term_j, sn_j)) {
          final int _saved;
          _saved = sortedInitials[i];
          sortedInitials[i] =  sortedInitials[j];
          sortedInitials[j] = _saved;
        }
      }
    }
  }
  
  /**
   * Determines if (particle1, term1, sn1) <= (particle2, term2, sn2) in EXI schema order.
   */
  protected boolean isAscending(int particle1, int term1, int sn1, int particle2, int term2, int sn2) {
    assert (sn1 < 0 || particle1 != EXISchema.NIL_NODE && term1 != EXISchema.NIL_NODE) &&
      (sn2 == Integer.MAX_VALUE || particle2 != EXISchema.NIL_NODE && term2 != EXISchema.NIL_NODE);
    if (sn1 < 0) {
      assert particle1 == EXISchema.NIL_NODE && term1 == EXISchema.NIL_NODE && sn2 >= 0;
      return true;
    }
    if (sn2 == Integer.MAX_VALUE) {
      assert particle2 == EXISchema.NIL_NODE && term2 == EXISchema.NIL_NODE && sn1 < sn2;
      return true;
    }
    final int termType1, termType2;
    termType1 = m_nodes[term1];
    termType2 = m_nodes[term2];
    
    if (termType1 == EXISchema.ELEMENT_NODE && termType2 == EXISchema.WILDCARD_NODE) {
      return true;
    }
    else if (termType1 == EXISchema.WILDCARD_NODE && termType2 == EXISchema.ELEMENT_NODE) {
      return false;
    }
    
    if (termType1 == EXISchema.ELEMENT_NODE) {
      assert termType2 == EXISchema.ELEMENT_NODE;
      if (sn1 <= sn2) {
        assert sn1 < sn2 || particle1 == particle2 && term1 != term2;
        return true;
      }
    }
    else {
      assert termType1 == EXISchema.WILDCARD_NODE && termType2 == EXISchema.WILDCARD_NODE;
      int constraint1, constraint2;
      constraint1 = m_schema.getConstraintTypeOfWildcard(term1);
      constraint2 = m_schema.getConstraintTypeOfWildcard(term2);
      if (constraint1 != EXISchema.WC_TYPE_NAMESPACES)
        constraint1 = EXISchema.WC_TYPE_ANY;
      if (constraint2 != EXISchema.WC_TYPE_NAMESPACES)
        constraint2 = EXISchema.WC_TYPE_ANY;
      assert constraint1 != constraint2 || constraint1 == EXISchema.WC_TYPE_NAMESPACES;
      if (constraint1 == EXISchema.WC_TYPE_NAMESPACES && constraint2 == EXISchema.WC_TYPE_ANY)
        return true;
      else if (constraint1 == EXISchema.WC_TYPE_ANY && constraint2 == EXISchema.WC_TYPE_NAMESPACES)
        return false;
      assert constraint1 == EXISchema.WC_TYPE_NAMESPACES && constraint2 == EXISchema.WC_TYPE_NAMESPACES;
      if (sn1 <= sn2)
        return true;
    }
    return false;
  }
  
  ///////////////////////////////////////////////////////////////////////////
  /// Methods computing EventCodeTuple and EventTypes
  ///////////////////////////////////////////////////////////////////////////

  protected static final class EventCodeTupleSink {
    EventCodeTuple eventCodeTuple;
    AbstractEventType[] eventTypes;
    void clear() {
      eventCodeTuple = null;
      eventTypes = null;
    }
  }

  protected final void createEventCodeTuple(final ArrayList<AbstractEventType> schemaEventTypes, 
      final short grammarOptions, final EventCodeTupleSink out, EventTypeList eventTypeList) {
    createEventCodeTuple(schemaEventTypes, grammarOptions, out, 
        (ArrayList<EventTypeSchemaAttributeInvalid>)null, eventTypeList, false);
  }

  protected final void createEventCodeTuple(final ArrayList<AbstractEventType> schemaEventTypes, 
      final short grammarOptions, final EventCodeTupleSink out, 
      final ArrayList<EventTypeSchemaAttributeInvalid> invalidAttributes, EventTypeList eventTypeList, final boolean atZero) {
    
    final int n_invalidAttributes = invalidAttributes != null ? invalidAttributes.size() : 0;
    
    final boolean addUndeclaredEA = GrammarOptions.hasUndeclaredEA(grammarOptions);
    assert invalidAttributes == null || addUndeclaredEA; // invalidAttributes is non-null only when addUndeclaredEA is true
    final boolean addDTD = GrammarOptions.hasDTD(grammarOptions);
    final boolean addCM =  GrammarOptions.hasCM(grammarOptions);
    final boolean addPI =  GrammarOptions.hasPI(grammarOptions);
    final boolean addNS, addSC, nillable, typable;
    
    if (atZero) {
      if (m_grammarType == SCHEMA_GRAMMAR_ELEMENT) {
        if (m_nd != EXISchema.NIL_NODE) {
          final int tp;
          final boolean nilTypeRestricted = GrammarOptions.isXsiNilTypeRestricted(grammarOptions);
          tp = m_schema.getTypeOfElem(m_nd);
          typable = !nilTypeRestricted || m_schema.hasSubType(tp) || 
            m_schema.getNodeType(tp) == EXISchema.SIMPLE_TYPE_NODE && m_schema.getVarietyOfSimpleType(tp) == EXISchema.UNION_SIMPLE_TYPE;
          nillable = !nilTypeRestricted || m_schema.isNillableElement(m_nd);
        }
        else {
          typable = true;
          nillable = true;
        }
      }
      else if (m_grammarType == SCHEMA_GRAMMAR_ELEMENT_TAG) {
        final boolean nillableTypable = !GrammarOptions.isXsiNilTypeRestricted(grammarOptions);
        typable  = nillableTypable;
        nillable = nillableTypable;
      }
      else {
        assert m_grammarType == SCHEMA_GRAMMAR_ELEMENT_FRAGMENT;
        typable  = nillable = true;
      }
      addNS = GrammarOptions.hasNS(grammarOptions);
      addSC = GrammarOptions.hasSC(grammarOptions);
    }
    else {
      typable = nillable = addNS = addSC = false;
    }
    
    boolean addUndeclaredEE = false;
    EventTypeEndElementSecond undeclaredEE = null;  
    EventTypeSchemaType eventTypeSchemaType = null;
    EventTypeSchemaNil eventTypeSchemaNil = null;
    EventTypeNamespaceDeclaration eventTypeNS = null;
    EventTypeSelfContained eventTypeSC = null;
    EventTypeSchemaAttributeWildcardAny undeclaredWildcardAnyAT = null;
    EventTypeAttributeWildcardAnyUntyped undeclaredWildcardAnyInvalidAT = null;
    EventTypeElementWildcard elementWildcard = null;
    EventTypeCharacters untypedCharacters = null;
    EventTypeEntityReference entityReference = null;
    EventTypeComment comment = null;
    EventTypeProcessingInstruction processingInstruction = null;
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
        if (eventType.itemType == EventCode.ITEM_SCHEMA_EE)
          break;
      }
      if (i == len) {
        addUndeclaredEE = true;
        undeclaredEE = new EventTypeEndElementSecond(this, eventTypeList); 
        ++n_itemsL2;
      }
      elementWildcard = new EventTypeElementWildcard(EventCode.EVENT_CODE_DEPTH_TWO, this, eventTypeList);
      untypedCharacters = new EventTypeCharactersSecond(EventCode.EVENT_CODE_DEPTH_TWO, this, eventTypeList);
      n_itemsL2 += 2;
      if (invalidAttributes != null) {
        undeclaredWildcardAnyAT = EventTypeSchemaAttributeWildcardAny.createLevelTwo(this, eventTypeList);
        undeclaredWildcardAnyInvalidAT = new EventTypeAttributeWildcardAnyUntyped(EventCode.EVENT_CODE_DEPTH_THREE, this, eventTypeList); 
        n_itemsL2 += 2;
        addTupleL3a = true;
      }
      addTupleL2 = true;
    }
    if (typable) {
      eventTypeSchemaType = new EventTypeSchemaType(this, eventTypeList);
      ++n_itemsL2;
      addTupleL2 = true;
    }
    if (nillable) {
      eventTypeSchemaNil = new EventTypeSchemaNil(this, eventTypeList);
      ++n_itemsL2;
      addTupleL2 = true;
    }
    if (addNS) {
      eventTypeNS = new EventTypeNamespaceDeclaration(this, eventTypeList);
      ++n_itemsL2;
      addTupleL2 = true;
    }
    if (addSC) {
      eventTypeSC = new EventTypeSelfContained(this, eventTypeList);
      ++n_itemsL2;
      addTupleL2 = true;
    }
    if (addDTD) {
      entityReference = new EventTypeEntityReference(this, eventTypeList);
      ++n_itemsL2;
      addTupleL2 = true;
    }
    if (addCM) {
      comment = new EventTypeComment(EventCode.EVENT_CODE_DEPTH_THREE, this, eventTypeList);
      ++n_itemsL3b;
      addTupleL3b = true;
    }
    if (addPI) {
      processingInstruction = new EventTypeProcessingInstruction(EventCode.EVENT_CODE_DEPTH_THREE, this, eventTypeList);
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
        len + (n_itemsL2 - 1) + n_itemsL3b + n_invalidAttributes: len + n_itemsL2 + n_invalidAttributes;
    eventCodes = ArrayEventCodeTuple.createTuple(); 
    AbstractEventType[] eventTypes = new AbstractEventType[totalLen];
    eventCodeItems = new EventCode[addTupleL2 ? len + 1 : len];
    int n = 0;
    if (typable) {
      eventTypes[n++] = eventTypeSchemaType;
    }
    if (nillable) {
      eventTypes[n++] = eventTypeSchemaNil;
    }
    for (i = 0; i < len; i++) {
      final AbstractEventType eventType = schemaEventTypes.get(i);
      eventTypes[n + i] = eventType;
      eventCodeItems[i] = eventType;
    }
    n += len;
    if (addTupleL2) {
      eventCodeItemsL2 = new EventCode[n_itemsL2];
      tupleL2 = ArrayEventCodeTuple.createTuple();
      eventCodeItems[len] = tupleL2;
      if (addTupleL3b) {
        eventCodeItemsL3b = new EventCode[n_itemsL3b];
        tupleL3b = ArrayEventCodeTuple.createTuple();
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
      tupleL3a = ArrayEventCodeTuple.createTuple();
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
