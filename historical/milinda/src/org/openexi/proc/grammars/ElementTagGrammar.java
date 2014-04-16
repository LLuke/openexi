package org.openexi.proc.grammars;

import java.util.ArrayList;

import org.openexi.proc.common.EventCode;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventTypeList;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.schema.EXISchema;
import org.openexi.schema.EXISchemaLayout;

final class ElementTagGrammar extends SchemaInformedGrammar {

  ///////////////////////////////////////////////////////////////////////////
  /// immutables
  ///////////////////////////////////////////////////////////////////////////

  private final int m_n_attruses;
  private final int m_attrwc; // attribute wildcard

  private final int[][] m_initials; // expected attribute uses
  private final int[][] m_indices; // subsequent position
  private final boolean[] m_expectContent; 
  
  private final int[][] m_fullInitials; // expected attribute uses and particles
  
  private final AbstractEventType[][] m_eventTypes;
  private final AbstractEventType[][] m_fullEventTypes;
  private final int[] m_firstContent; // index of the first content event type
  private final ArrayEventTypeList[] m_eventTypeLists;
  private final ArrayEventTypeList[] m_fullEventTypeLists;
  
  private final EventCodeTuple[] m_eventCodes;
  private final EventCodeTuple[] m_fullEventCodes;

  private final EmptyContentGrammar m_emptyContentState;
  private final ContentGrammar m_elementContentState;

  private static final ArrayList<EventTypeSchemaAttributeInvalid> NO_INVALID_ATTRIBUTES =
    new ArrayList<EventTypeSchemaAttributeInvalid>();

  ///////////////////////////////////////////////////////////////////////////
  /// constructors, initializers
  ///////////////////////////////////////////////////////////////////////////

  ElementTagGrammar(int tp, GrammarCache cache) {
    super(tp, SCHEMA_GRAMMAR_ELEMENT_TAG, cache);

    int i, j;
    if (m_schema.getNodeType(tp) == EXISchema.COMPLEX_TYPE_NODE) {
      m_attrwc = m_nodes[tp + EXISchemaLayout.SZ_TYPE + EXISchemaLayout.COMPLEX_TYPE_ATTRIBUTE_WC]; 
      m_n_attruses = m_nodes[tp + EXISchemaLayout.SZ_TYPE + EXISchemaLayout.COMPLEX_TYPE_N_ATTRIBUTE_USES];
      m_initials = new int[m_n_attruses + 1][];
      m_indices = new int[m_n_attruses + 1][];
      m_expectContent = new boolean[m_n_attruses + 1];
      for (i = 0; i < m_n_attruses; i++) {
        boolean expectContent = false;
        final int n_initials = m_schema.getNextAttrUsesCountOfComplexType(tp, i);
        final int attruses = m_schema.getNextAttrUsesOfComplexType(tp, i);
        if (m_attrwc != EXISchema.NIL_NODE) {
          m_initials[i] = new int[n_initials + 1];
          m_indices[i] = new int[n_initials + 1];
        }
        else {
          m_initials[i] = new int[n_initials];
          m_indices[i] = new int[n_initials];
        }
        for (j = 0; j < n_initials; j++) {
          int attruse = m_schema.getOpaques()[attruses + j];
          m_initials[i][j] = attruse;
          m_indices[i][j] = m_schema.getOpaques()[attruses + n_initials + j];
          if (attruse == EXISchema.NIL_NODE) {
            assert j == n_initials - 1;
            expectContent = true;
          }
        }
        if (m_attrwc != EXISchema.NIL_NODE) {
          int ind = n_initials;
          if (expectContent) {
            assert m_initials[i][n_initials - 1] == EXISchema.NIL_NODE;
            m_initials[i][n_initials] = EXISchema.NIL_NODE;
            m_indices[i][n_initials] = m_indices[i][n_initials - 1];
            ind = n_initials - 1;
          }
          m_initials[i][ind] = m_attrwc;
          m_indices[i][ind] = m_n_attruses;
        }
        m_expectContent[i] = expectContent;
      }
      if (m_attrwc != EXISchema.NIL_NODE) {
        m_initials[m_n_attruses] = new int[2];
        m_indices[m_n_attruses] = new int[2];
        m_initials[m_n_attruses][0] = m_attrwc;
        m_indices[m_n_attruses][0] = m_n_attruses;
        m_initials[m_n_attruses][1] = EXISchema.NIL_NODE;
        m_indices[m_n_attruses][1] = m_n_attruses;
      }
      else {
        m_initials[m_n_attruses] = new int[1];
        m_indices[m_n_attruses] = new int[1];
        m_initials[m_n_attruses][0] = EXISchema.NIL_NODE;
        m_indices[m_n_attruses][0] = m_n_attruses;
      }
      m_expectContent[m_n_attruses] = true;
    }
    else {
      m_attrwc = EXISchema.NIL_NODE;
      m_n_attruses = 0;
      m_initials = new int[1][];
      m_indices = new int[1][];
      m_expectContent = new boolean[1];
      m_initials[0] = new int[1];
      m_indices[0] = new int[1];
      m_initials[0][0] = EXISchema.NIL_NODE;
      m_indices[0][0] = 0;
      m_expectContent[0] = true;
    }
    
    m_emptyContentState = cache.getEmptyContentGrammar(); 
    m_elementContentState = m_grammarCache.retrieveElementContentGrammar(m_nd);

    m_fullInitials = new int[m_n_attruses + 1][];
    final int[] subsequentInitials = m_elementContentState.getInitials();
    for (i = 0; i < m_n_attruses + 1; i++) {
      int[] attruses = m_initials[i];
      if (!m_expectContent[i]) {
        m_fullInitials[i] = attruses;
      }
      else {
        assert attruses[attruses.length - 1] == EXISchema.NIL_NODE;
        final int[] fullInitials = new int[attruses.length - 1 + subsequentInitials.length];
        int pos = 0;
        for (j = 0; j < attruses.length - 1; j++) {
          fullInitials[pos++] = attruses[j];
        }
        for (j = 0; j < subsequentInitials.length; j++) {
          fullInitials[pos++] = subsequentInitials[j];
        }
        assert pos == fullInitials.length;
        m_fullInitials[i] = fullInitials;
      }
    }

    final boolean isContentMixed = m_schema.getNodeType(tp) == EXISchema.COMPLEX_TYPE_NODE ?
        m_schema.getContentClassOfComplexType(tp) == EXISchema.CONTENT_MIXED : false;   
    
    final boolean _nillableTypable = !GrammarOptions.isXsiNilTypeRestricted(cache.grammarOptions);
    
    m_eventTypes = new AbstractEventType[m_n_attruses + 1][];
    m_fullEventTypes = new AbstractEventType[m_n_attruses + 1][];
    m_eventCodes = new EventCodeTuple[m_n_attruses + 1];
    m_fullEventCodes = new EventCodeTuple[m_n_attruses + 1];
    m_firstContent = new int[m_n_attruses + 1];
    m_eventTypeLists = new ArrayEventTypeList[m_n_attruses + 1];
    m_fullEventTypeLists = new ArrayEventTypeList[m_n_attruses + 1];
    for (i = 0; i < m_n_attruses + 1; i++) {
      final boolean nillableTypable = i != 0 ?  false : _nillableTypable;
      final int _serial = nillableTypable ? 2 : 0; 
      
      final ArrayList<AbstractEventType> eventTypeList = new ArrayList<AbstractEventType>();
      
      ArrayList<EventTypeSchemaAttributeInvalid> invalidAttributes;
      EventTypeSchema[] eventTypeSchemas;
      
      int serial;
      
      final boolean addUndeclaredEA = GrammarOptions.hasUndeclaredEA(cache.grammarOptions);
      
      m_fullEventTypeLists[i] = new ArrayEventTypeList();
      final int[] fullInitials = m_fullInitials[i];
      int contentIndex = -1;
      for (j = 0, serial = _serial, invalidAttributes = null; j < fullInitials.length; 
           j++, serial += eventTypeSchemas.length) {
        eventTypeSchemas = createEventType(fullInitials[j], j, serial, this, m_fullEventTypeLists[i]);
        for (int k = 0; k < eventTypeSchemas.length; k++) {
          EventTypeSchema eventType;
          eventType = eventTypeSchemas[k];
          if (contentIndex == -1 && eventType.isContent()) {
            contentIndex = eventTypeList.size();
            if (nillableTypable)
              contentIndex += 2;
          }
          eventTypeList.add(eventType);
          final short itemType = eventType.itemType;
          if (addUndeclaredEA && itemType == EventCode.ITEM_SCHEMA_AT) {
            if (invalidAttributes == null) {
              invalidAttributes = new ArrayList<EventTypeSchemaAttributeInvalid>();
            }
            invalidAttributes.add(new EventTypeSchemaAttributeInvalid((EventTypeSchemaAttribute)eventType, this, m_fullEventTypeLists[i]));
          }
        }
      }
      if (addUndeclaredEA && invalidAttributes == null) {
        assert NO_INVALID_ATTRIBUTES.size() == 0;
        invalidAttributes = NO_INVALID_ATTRIBUTES;
      }
      if (isContentMixed && contentIndex != -1) {
        eventTypeList.add(new EventTypeSchemaMixedCharacters(serial++, this, m_fullEventTypeLists[i]));
      }
      
      m_firstContent[i] = contentIndex;
      
      EventCodeTupleSink res;
      createEventCodeTuple(eventTypeList, cache.grammarOptions, res = new EventCodeTupleSink(), invalidAttributes, m_fullEventTypeLists[i], i == 0);
      
      m_fullEventTypes[i] = res.eventTypes;
      m_fullEventCodes[i] = res.eventCodeTuple;

  
      eventTypeList.clear();
      invalidAttributes = null;

      m_eventTypeLists[i] = new ArrayEventTypeList();
      int[] initials = m_initials[i];
      for (j = 0, serial = _serial; j < initials.length; j++, serial += eventTypeSchemas.length) {
        eventTypeSchemas = createEventType(initials[j], j, serial, this, m_eventTypeLists[i]);
        for (int k = 0; k < eventTypeSchemas.length; k++) {
          EventTypeSchema eventType;
          eventType = eventTypeSchemas[k];
          eventTypeList.add(eventType);
          final short itemType = eventType.itemType;
          if (addUndeclaredEA && itemType == EventCode.ITEM_SCHEMA_AT) {
            if (invalidAttributes == null) {
              invalidAttributes = new ArrayList<EventTypeSchemaAttributeInvalid>();
            }
            invalidAttributes.add(new EventTypeSchemaAttributeInvalid((EventTypeSchemaAttribute)eventType, this, m_eventTypeLists[i]));
          }
        }
      }
      if (addUndeclaredEA && invalidAttributes == null) {
        assert NO_INVALID_ATTRIBUTES.size() == 0;
        invalidAttributes = NO_INVALID_ATTRIBUTES;
      }

      res.clear();
      createEventCodeTuple(eventTypeList, cache.grammarOptions, res, invalidAttributes, m_eventTypeLists[i], i == 0);

      m_eventTypes[i] = res.eventTypes;
      m_eventCodes[i] = res.eventCodeTuple;
      
      m_eventTypeLists[i].setItems(m_eventTypes[i]);
      m_fullEventTypeLists[i].setItems(m_fullEventTypes[i]);
    }
  }
  
  @Override
  public final void init(GrammarState stateVariables) {
    super.init(stateVariables);
    stateVariables.phase = ELEMENT_STATE_TAG;
    stateVariables.cursor = 0;
    stateVariables.contentStateVariables = null;
    stateVariables.nilled = false;
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Implementation of abstract methods declared in CommonState
  ///////////////////////////////////////////////////////////////////////////

  @Override
  final EventTypeList getNextEventTypes(GrammarState stateVariables) {
    if (!stateVariables.nilled)
      return m_fullEventTypeLists[stateVariables.cursor];
    else
      return m_eventTypeLists[stateVariables.cursor];
  }
  
  @Override
  final EventCodeTuple getNextEventCodes(GrammarState stateVariables) {
    if (!stateVariables.nilled)
      return m_fullEventCodes[stateVariables.cursor];
    else
      return m_eventCodes[stateVariables.cursor];
  }
  
  @Override
  final void element(int eventTypeIndex, String uri, String name, GrammarState stateVariables) {
    assert stateVariables.phase == ELEMENT_STATE_TAG;
    startContent(stateVariables);
    final GrammarState contentStateVariables = stateVariables.contentStateVariables;
    ((SchemaInformedGrammar)contentStateVariables.targetGrammar).element(
        eventTypeIndex - m_firstContent[stateVariables.cursor], uri, name, contentStateVariables); // delegate
  }
  
  @Override
  final Grammar undeclaredElement(String uri, String name, GrammarState stateVariables) {
    assert stateVariables.phase == ELEMENT_STATE_TAG;
    startContent(stateVariables);
    final GrammarState contentStateVariables = stateVariables.contentStateVariables;
    return contentStateVariables.targetGrammar.undeclaredElement(uri, name, contentStateVariables); // delegate
  }

  @Override
  final void schemaAttribute(int eventTypeIndex, String uri, String name, GrammarState stateVariables) {
    
    EventTypeSchema schemaEventType = 
      (EventTypeSchema)m_eventTypes[stateVariables.cursor][eventTypeIndex];
    
    assert schemaEventType.itemType == EventCode.ITEM_SCHEMA_AT;
    assert stateVariables.phase == ELEMENT_STATE_TAG;

    assert m_n_attruses > 0;
    
    final int[] indices = m_indices[stateVariables.cursor];
    stateVariables.cursor = indices[schemaEventType.index];
  }
  
  @Override
  final void xsitp(int tp, GrammarState stateVariables) {
    throw new UnsupportedOperationException();
  }

  @Override
  final void nillify(GrammarState stateVariables) {
    assert stateVariables.phase == ELEMENT_STATE_TAG;
    stateVariables.nilled = true;
  }
  
  private void startContent(GrammarState stateVariables) {
    assert stateVariables.phase == ELEMENT_STATE_TAG;
    stateVariables.phase = ELEMENT_STATE_CONTENT;
    final GrammarState contentStateVariables = stateVariables.documentGrammarState.pushState();
    stateVariables.contentStateVariables = contentStateVariables;
    final ContentGrammar contentGrammar = stateVariables.nilled ? m_emptyContentState : m_elementContentState;
    contentGrammar.init(contentStateVariables);
  }
  
  @Override
  final public void chars(GrammarState stateVariables) {
    assert stateVariables.phase == ELEMENT_STATE_TAG;
    startContent(stateVariables);
    final GrammarState contentStateVariables = stateVariables.contentStateVariables;
    contentStateVariables.targetGrammar.chars(contentStateVariables); // delegate
  }

  @Override
  final public void undeclaredChars(GrammarState stateVariables) {
    assert stateVariables.phase == ELEMENT_STATE_TAG;
    startContent(stateVariables);
    final GrammarState contentStateVariables = stateVariables.contentStateVariables;
    contentStateVariables.targetGrammar.undeclaredChars(contentStateVariables); // delegate
  }

  @Override
  final public void miscContent(GrammarState stateVariables) {
    assert stateVariables.phase == ELEMENT_STATE_TAG;
    startContent(stateVariables);
    final GrammarState contentStateVariables = stateVariables.contentStateVariables;
    contentStateVariables.targetGrammar.miscContent(contentStateVariables); // delegate
  }

  @Override
  final void done(GrammarState kid, GrammarState stateVariables) {
    assert kid.targetGrammar.m_grammarType == SCHEMA_GRAMMAR_NIL_CONTENT || kid.targetGrammar.m_grammarType == SCHEMA_GRAMMAR_ELEMENT_CONTENT;
    finish(stateVariables);
  }

  @Override
  final void end(String uri, String name, GrammarState stateVariables) {
    assert stateVariables.phase == ELEMENT_STATE_TAG;
    startContent(stateVariables);
    final GrammarState contentStateVariables = stateVariables.contentStateVariables;
    contentStateVariables.targetGrammar.end(uri, name, contentStateVariables); // delegate
  }

  final EventType[] getInitialEventTypes() {
    return m_fullEventTypes[0];
  }
  
}