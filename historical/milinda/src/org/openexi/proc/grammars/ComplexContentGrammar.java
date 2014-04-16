package org.openexi.proc.grammars;

import java.util.ArrayList;

import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventTypeList;
import org.openexi.schema.EXISchema;

final class ComplexContentGrammar extends ElementContentGrammar {

  private static final int POS_0 = 0;
  private static final int POS_1 = 1;
  private static final int POS_2 = 2;
  private static final int POS_SZ = 3;
  
  ///////////////////////////////////////////////////////////////////////////
  /// immutables
  ///////////////////////////////////////////////////////////////////////////

  /**
   * True if the group (i.e. term) of the content type (i.e. particle) is
   * a fixture.
   */  
  private final boolean m_isFixtureGroup;
  private final int m_minOccurs;
  private final int m_maxOccurs;
  private final int m_group;

  /** Number of substance particles that can initiate the group. */
  private final int[] m_n_initials;
  /** The list of substance particles */
  private final int[] m_initials;
  /** list of particle index within the group */
  private final int[] m_indices;
  
  private final int[] m_initialParticles; 
  
  private final EventType[][] m_eventTypes;
  private final EventCodeTuple[] m_eventCodes;
  private final ArrayEventTypeList[] m_eventTypeLists;

  /**
   * The initial cursor is 1 when m_group is "all" group, otherwise 0.
   */
  private final int m_initialCursor;

  /**
   * Each group represents cursor position POS_0, POS_1 and POS_2,
   * respectively.
   */
  private final GroupGrammar[] m_groupStates; // [0...2]

  ///////////////////////////////////////////////////////////////////////////
  /// constructors, initializers
  ///////////////////////////////////////////////////////////////////////////

  ComplexContentGrammar(int tp, GrammarCache cache, boolean checkSanity) {
    super(tp, cache);

    assert m_schema.getNodeType(tp) == EXISchema.COMPLEX_TYPE_NODE;

    // REVISIT: Be prepared for NIL_NODE types and behave as if it is an anyType.
    assert tp != EXISchema.NIL_NODE;
    
    final int contentClass = m_schema.getContentClassOfComplexType(tp);
    assert contentClass == EXISchema.CONTENT_ELEMENT_ONLY || contentClass == EXISchema.CONTENT_MIXED;
    
    m_group = m_schema.getParticleTermOfComplexType(tp);
    m_minOccurs = m_schema.getParticleMinOccursOfComplexType(tp);
    int maxOccurs;
    maxOccurs = m_schema.getParticleMaxOccursOfComplexType(tp);
    if (EXISchema.UNBOUNDED_OCCURS == maxOccurs) {
      maxOccurs = Integer.MAX_VALUE;
    }
    m_maxOccurs = maxOccurs;
    m_isFixtureGroup = m_group != EXISchema.NIL_NODE ? m_schema.isFixtureGroup(m_group) : false;

    m_n_initials = new int[POS_SZ];
    m_indices = new int[POS_SZ];
    m_initials = new int[POS_SZ];

    final int[][] sortedInitials = new int[POS_SZ][];

    final int substanceList = m_schema.getSubstanceListOfComplexType(m_nd);
    final int n_substances = m_schema.getSubstanceCountOfComplexType(m_nd); 

    m_initials[POS_0] = substanceList;
    m_indices[POS_0] = substanceList + n_substances;
    m_n_initials[POS_0] = m_schema.getHeadSubstanceCountOfComplexType(m_nd);
    
    m_initials[POS_1] = substanceList;
    m_indices[POS_1] = substanceList + n_substances;
    m_n_initials[POS_1] = n_substances;
    
    m_initials[POS_2] = substanceList + n_substances - 1;
    m_indices[POS_2] = substanceList + 2*n_substances - 1;
    m_n_initials[POS_2] = 1;
    
    assert m_opaques[m_initials[POS_2]] == EXISchema.NIL_NODE;
    
    int i, len;
    for (i = 0; i < POS_SZ; i++) {
      sortedInitials[i] = new int[m_n_initials[i]];
      sortInitials(sortedInitials[i], m_initials[i], m_n_initials[i]);
    }

    final int compositor = m_schema.getCompositorOfGroup(m_group); 
    m_initialCursor = compositor != EXISchema.GROUP_ALL ? POS_0 : POS_1; 
    
    m_initialParticles = new int[m_n_initials[m_initialCursor]];
    for (i = 0, len = m_initialParticles.length; i < len; i++) {
      m_initialParticles[i] = m_opaques[m_initials[m_initialCursor] + sortedInitials[m_initialCursor][i]];
    }

    final boolean isContentMixed = contentClass == EXISchema.CONTENT_MIXED;
    
    m_eventTypes = new EventType[POS_SZ][];
    m_eventCodes = new EventCodeTuple[POS_SZ];
    m_eventTypeLists = new ArrayEventTypeList[POS_SZ];
    for (i = 0; i < POS_SZ; i++) {
      len = m_n_initials[i];
      m_eventTypeLists[i] = new ArrayEventTypeList();
      ArrayList<AbstractEventType> eventTypeList = new ArrayList<AbstractEventType>();
      EventTypeSchema[] eventTypes;
      int j, serial;
      for (j = 0, serial = 0; j < len; j++, serial += eventTypes.length) {
        eventTypes = createEventType(m_opaques[m_initials[i] + sortedInitials[i][j]], sortedInitials[i][j], serial, this, m_eventTypeLists[i]);
        for (int k = 0; k < eventTypes.length; k++) {
          EventTypeSchema eventType = eventTypes[k];
          eventTypeList.add(eventType);
        }
      }
      if (isContentMixed) {
        eventTypeList.add(new EventTypeSchemaMixedCharacters(serial++, this, m_eventTypeLists[i]));
      }
      len = eventTypeList.size();
      
      EventCodeTupleSink res;
      createEventCodeTuple(eventTypeList, cache.grammarOptions, res = new EventCodeTupleSink(), m_eventTypeLists[i]);

      m_eventCodes[i] = res.eventCodeTuple;
      m_eventTypes[i] = res.eventTypes;
      m_eventTypeLists[i].setItems(res.eventTypes);
    }

    // triplet (n_initials, initials and indices)
    final int[] triplets = new int[3];

    m_groupStates = new GroupGrammar[POS_SZ];
    m_groupStates[POS_0] = null;
    m_groupStates[POS_1] = null;
    m_groupStates[POS_2] = null;
    if (m_group != EXISchema.NIL_NODE) {
      // GROUP_ALL groups never come back to POS_0 or POS_1, they do straight to POS_2 
      if (compositor != EXISchema.GROUP_ALL) {
        // push the subsequent triplet (n_initials, initials and indices) at POS_0
        triplets[0] = m_n_initials[POS_0];
        triplets[1] = m_initials[POS_0];
        triplets[2] = m_indices[POS_0];
        m_groupStates[POS_0] = GroupGrammar.createGroupState(
            m_group, this, cache, this.m_schema, triplets, 0, 1, isContentMixed, m_eventTypes[0], checkSanity);
        // push the subsequent triplet (n_initials, initials and indices) at POS_1
        triplets[0] = m_n_initials[POS_1];
        triplets[1] = m_initials[POS_1];
        triplets[2] = m_indices[POS_1];
        m_groupStates[POS_1] = GroupGrammar.createGroupState(
            m_group, this, cache, this.m_schema, triplets, 0, 1, isContentMixed, m_eventTypes[1], checkSanity);
      }
      // push the subsequent triplet (n_initials, initials and indices) at POS_2
      triplets[0] = m_n_initials[POS_2];
      triplets[1] = m_initials[POS_2];
      triplets[2] = m_indices[POS_2];
      m_groupStates[POS_2] = GroupGrammar.createGroupState(
          m_group, this, cache, this.m_schema, triplets, 0, 1, isContentMixed, m_eventTypes[2], checkSanity);
    }
  }

  @Override
  public final void init(GrammarState stateVariables) {
    super.init(stateVariables);
    stateVariables.occurs = 0;
    stateVariables.cursor = m_initialCursor;
    updateState(stateVariables);
  }
  
  ///////////////////////////////////////////////////////////////////////////
  /// Implementation of abstract methods declared in CommonState
  ///////////////////////////////////////////////////////////////////////////

  @Override
  final EventTypeList getNextEventTypes(GrammarState stateVariables) {
    return m_eventTypeLists[stateVariables.cursor]; 
  }

  @Override
  final EventCodeTuple getNextEventCodes(GrammarState stateVariables) {
    return m_eventCodes[stateVariables.cursor];
  }

  @Override
  final void element(int eventTypeIndex, String uri, String name, GrammarState stateVariables) {

    stateVariables.occurs++;
    if (stateVariables.cursor == POS_0) {
      stateVariables.cursor = stateVariables.occurs < m_minOccurs ? POS_0 :
        (stateVariables.occurs < m_maxOccurs ? POS_1 : POS_2);
    }
    else if (stateVariables.cursor == POS_1 && stateVariables.occurs == m_maxOccurs) {
      stateVariables.cursor = POS_2;
    }
    updateState(stateVariables);
    final GrammarState groupState = stateVariables.documentGrammarState.pushState();
    m_groupStates[stateVariables.cursor].init(groupState);
    ((SchemaInformedGrammar)groupState.targetGrammar).element(eventTypeIndex, uri, name, groupState);
  }

  @Override
  final void schemaAttribute(int eventTypeIndex, String uri, String name, GrammarState stateVariables) {
    throw new UnsupportedOperationException();
  }

  @Override
  final void xsitp(int tp, GrammarState stateVariables) {
    throw new UnsupportedOperationException();
  }
  
  @Override
  final void nillify(GrammarState stateVariables) {
    throw new UnsupportedOperationException();
  }
  
  @Override
  public final void chars(GrammarState stateVariables) {
    // nothing to do.
  }

  @Override
  public final void undeclaredChars(GrammarState stateVariables) {
    // nothing to do.
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Implementation of abstract methods declared in ElementContentState
  ///////////////////////////////////////////////////////////////////////////

  @Override
  final protected void end(GrammarState stateVariables) {
    // REVISIT: remove the "if" block below.
    if (stateVariables.cursor == POS_0 && stateVariables.occurs < m_minOccurs && m_isFixtureGroup) {
      assert stateVariables.parent.targetGrammar.m_grammarType == SCHEMA_GRAMMAR_ELEMENT_TAG;
      assert stateVariables.parent.parent.targetGrammar.m_grammarType == SCHEMA_GRAMMAR_ELEMENT;
    }
    finish(stateVariables);
  }

  @Override
  final int[] getInitials() {
    return m_initialParticles;
  }
  
  @Override
  final String getContentRegime() {
    return "complex";
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Private methods
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Update state.
   */
  private void updateState(GrammarState stateVariables) {
    
    final int initials = m_initials[stateVariables.cursor];
    final int n_initials = m_n_initials[stateVariables.cursor];
    
    stateVariables.phase = m_opaques[initials + n_initials - 1] != EXISchema.NIL_NODE ? ELEMENT_STATE_CONTENT_DEPLETE :
      (stateVariables.phase = n_initials == 1 ? ELEMENT_STATE_CONTENT_COMPLETE : ELEMENT_STATE_CONTENT_ACCEPTED);
  }

}