package org.openexi.proc.grammars;

import java.util.ArrayList;

import org.openexi.proc.common.EventCode;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventTypeList;
import org.openexi.schema.EXISchema;
import org.openexi.schema.EXISchemaLayout;

final class AllGroupGrammar extends GroupGrammar {

  ///////////////////////////////////////////////////////////////////////////
  /// immutables
  ///////////////////////////////////////////////////////////////////////////

  private final int m_indices;
  
  private final EventType[] m_eventTypes;
  private final EventCodeTuple m_eventCode;
  private final ArrayEventTypeList m_eventTypeList;

  ///////////////////////////////////////////////////////////////////////////
  /// constructors, initializers
  ///////////////////////////////////////////////////////////////////////////

  AllGroupGrammar(int group, Grammar parent, GrammarCache cache) { 
    super(group, parent, cache);

    int i;
    for (i = 0; i < m_n_particles; i++) {
      final int particle = m_particles[i];
      if (m_nodes[particle + EXISchemaLayout.PARTICLE_MAXOCCURS] == 0) {
        m_particles[i] = EXISchema.NIL_NODE;
        continue;
      }
      // only elements with maxOccurs="1" can "occur" in "all" group. 
      assert m_schema.getTermTypeOfParticle(particle) != EXISchema.TERM_TYPE_GROUP;
      assert m_schema.getTermTypeOfParticle(particle) != EXISchema.TERM_TYPE_WILDCARD;
    }

    int n_initials = m_schema.getHeadSubstanceCountOfGroup(m_nd);
    final int initials = m_schema.getHeadSubstanceListOfGroup(m_nd);
    m_indices = initials + n_initials;
    if (m_opaques[initials + n_initials - 1] == EXISchema.NIL_NODE)
      --n_initials;
    
    assert m_n_particles == n_initials;

    m_eventTypeList = new ArrayEventTypeList();

    ArrayList<AbstractEventType> eventTypeList = new ArrayList<AbstractEventType>();
    EventTypeSchema[] eventTypes;
    int serial;
    for (i = 0, serial = 0; i < n_initials; i++) {
      final int particle;
      if ((particle = m_opaques[initials + i]) != EXISchema.NIL_NODE) {
        eventTypes = createEventType(particle, m_opaques[m_indices + i], serial, this, m_eventTypeList);
        for (int k = 0; k < eventTypes.length; k++) {
          eventTypeList.add(eventTypes[k]);
          ++serial;
        }
      }
    }
    eventTypes = createEventType(EXISchema.NIL_NODE, m_n_particles, serial, this, m_eventTypeList);
    assert eventTypes.length == 1;
    eventTypeList.add(eventTypes[0]);
    
    EventCodeTupleSink res;
    createEventCodeTuple(eventTypeList, cache.grammarOptions, res = new EventCodeTupleSink(), m_eventTypeList);

    m_eventCode = res.eventCodeTuple;
    m_eventTypes = res.eventTypes;
    m_eventTypeList.setItems(res.eventTypes);
  }

  /**
   * Prepare the state for use.
   */
  @Override
  public final void init(GrammarState stateVariables) {
    super.init(stateVariables);
    stateVariables.phase = GROUP_STATE_ACCEPTED;
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Implementation of abstract methods declared in CommonState
  ///////////////////////////////////////////////////////////////////////////

  @Override
  final EventTypeList getNextEventTypes(GrammarState stateVariables) {
    return m_eventTypeList;
  }

  @Override
  EventCodeTuple getNextEventCodes(GrammarState stateVariables) {
    return m_eventCode;
  }
  
  @Override
  void element(int eventTypeIndex, String uri, String name, GrammarState stateVariables) {

    EventTypeSchema schemaEventType = (EventTypeSchema)m_eventTypes[eventTypeIndex];

    assert schemaEventType.itemType == EventCode.ITEM_SCHEMA_SE;
    assert schemaEventType.getGrammar() == this;

    final int matchedPos = schemaEventType.index;
    final int ind = m_opaques[m_indices + matchedPos];
    
    final int termOfParticle = m_nodes[m_particles[ind] + EXISchemaLayout.PARTICLE_TERM];
    assert m_nodes[termOfParticle] == EXISchema.ELEMENT_NODE;
    // Use the one really matched for elements
    final int term = schemaEventType.getSchemaSubstance();
    
    m_grammarCache.retrieveElementGrammar(term).init(stateVariables.documentGrammarState.pushState());
  }

  @Override
  void schemaAttribute(int eventTypeIndex, String uri, String name, GrammarState stateVariables) {
    throw new UnsupportedOperationException();
  }

  @Override
  void xsitp(int tp, GrammarState stateVariables) {
    throw new UnsupportedOperationException();
  }
  
  @Override
  void nillify(GrammarState stateVariables) {
    throw new UnsupportedOperationException();
  }
  
}