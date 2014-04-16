package org.openexi.proc.grammars;

import java.util.ArrayList;

import org.openexi.proc.common.EventCode;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventTypeList;
import org.openexi.schema.EXISchema;
import org.openexi.schema.EXISchemaLayout;
import org.openexi.schema.IntBuffer;

final class SequenceChoiceGrammar extends GroupGrammar {

  ///////////////////////////////////////////////////////////////////////////
  /// immutables
  ///////////////////////////////////////////////////////////////////////////

  // minOccurs, maxOccurs
  private final int[] m_minOccurs;
  private final int[] m_maxOccurs;

  private final int[][] m_initials;
  private final int[][] m_indices;
  private final int[][] m_depths;
  
  private final byte[] m_groupAcceptance;
  
  private final boolean[] m_isFixtureTerm;

  private final EventType[][] m_eventTypes;
  private final EventCodeTuple[] m_eventCodes;
  private final ArrayEventTypeList[] m_eventTypeLists;
  
  private final int m_n_triplets;
  /** Stack of triplets (n_initials, initials and indices) */
  private final int[] m_triplets;

  /**
   * Each particle is assigned three groups.
   * 
   * In case of sequence group:
   * Given i-th particle, the relevant groups in the array are
   * 3*i, 3*i+1 and 3*i+2, which represent cursor positions
   * 2*(i+1)-1, 2*(i+1) and 2*(i+1)+1, respectively.
   * 
   * In case of choice group:
   * Given i-th particle, the relevant groups in the array are
   * 3*i, 3*i+1 and 3*i+2, which represent cursor positions
   * 2*i, 2*i+1 and 2*m_n_particles, respectively.
   * 
   * The array is sparse in that, entries are zeros if the
   * relevant particle is not a group.
   */
  private final GroupGrammar[] m_groupStates; // [ 0 ... 3*m_n_particles )

  private static final int IS_NOT_DONE = 0; // it is not done yet, that is for sure
  private static final int IS_ACCEPTED = 1; // it may be done, but can be followed by more
  private static final int IS_DONE     = 2; // no more is expected. definitely done.

  ///////////////////////////////////////////////////////////////////////////
  /// constructors, initializers
  ///////////////////////////////////////////////////////////////////////////

  SequenceChoiceGrammar(int group, Grammar parent, GrammarCache cache,
      final int[] triplets, final int tripletStart, final int n_triplets,
      boolean mixed, EventType[] subsequentEventTypes, boolean checkSanity) {
    
    super(group, parent, cache);

    m_minOccurs = new int[m_n_particles];
    m_maxOccurs = new int[m_n_particles];
    m_isFixtureTerm = new boolean[m_n_particles];

    m_initials = new int[2 * (m_n_particles + 1)][];
    m_indices = new int[2 * (m_n_particles + 1)][];
    m_depths = new int[2 * (m_n_particles + 1)][];

    m_eventTypes = new EventType[2 * (m_n_particles + 1)][];
    m_eventCodes = new EventCodeTuple[2 * (m_n_particles + 1)];
    m_eventTypeLists = new ArrayEventTypeList[2 * (m_n_particles + 1)];

    final int[] group_n_initials;
    final int[] group_initials;
    final int[] group_indices;

    group_n_initials = new int[2 * (m_n_particles + 1)];
    group_initials   = new int[2 * (m_n_particles + 1)];
    group_indices    = new int[2 * (m_n_particles + 1)];
    m_groupAcceptance = new byte[2 * (m_n_particles + 1)];

    final int[][] group_sortedInitials; // index of initials, not initials themselves
    group_sortedInitials = new int[2 * (m_n_particles + 1)][];

    m_n_triplets = n_triplets - tripletStart;
    m_triplets = new int[3 * m_n_triplets];
    System.arraycopy(triplets, 3 * tripletStart, m_triplets, 0, 3 * m_n_triplets);
    int[] _triplets = new int[3 * (m_n_triplets + 1)]; // make a room for one extra entry
    System.arraycopy(triplets, 3 * tripletStart, _triplets, 0, 3 * m_n_triplets);
    
    int i;

    IntBuffer intBuf;
    intBuf = new IntBuffer();
    int triplet;
    for (i = 1, triplet = m_n_triplets - 1; triplet >= 0; triplet--, i++) {
      final int tripletBase = 3 * triplet;
      final int length = m_triplets[tripletBase + 0];
      final int offset = m_triplets[tripletBase + 1];
      final int indices = m_triplets[tripletBase + 2];
      for (int j = 0; j < length; j++) {
        final int particle = m_opaques[offset + j];
        if (particle != EXISchema.NIL_NODE || triplet == 0) {
          final int index = m_opaques[indices + j];
          intBuf.append(particle).append(index).append(i); // particle, index, depth
        }
      }
    }
    final int[] subsequentPid = intBuf.toIntArray(); // list of subsequent (particle, index, depth)
    
    
    m_groupStates = new GroupGrammar[3 * m_n_particles];

    for (i = 0; i <= m_n_particles; i++) {
      if (i < m_n_particles) {
        int particle = m_particles[i];
        m_minOccurs[i] = m_nodes[particle + EXISchemaLayout.PARTICLE_MINOCCURS]; 
        final int maxOccurs = m_nodes[particle + EXISchemaLayout.PARTICLE_MAXOCCURS]; 
        m_maxOccurs[i] = maxOccurs != EXISchema.UNBOUNDED_OCCURS ?
            maxOccurs : Integer.MAX_VALUE;
        boolean isFixtureTerm = true;
        if (m_schema.getTermTypeOfParticle(particle) == EXISchema.TERM_TYPE_GROUP) {
          final int term = m_schema.getTermOfParticle(particle);
          assert m_schema.getCompositorOfGroup(term) != EXISchema.GROUP_ALL; 
          isFixtureTerm = m_schema.isFixtureGroup(term);
        }
        m_isFixtureTerm[i] = isFixtureTerm;
      }

      int n_initials, initials, n_backward_initials;
      n_initials = m_schema.getHeadSubstanceCountOfGroup(m_nd, i);
      initials = m_schema.getHeadSubstanceListOfGroup(m_nd, i);
      n_backward_initials = m_schema.getBackwardHeadSubstanceCountOfGroup(m_nd, i);
      if (m_compositor == EXISchema.GROUP_CHOICE) {
        if (i == m_n_particles) {
          group_n_initials[2 * i]     = n_initials;
          group_initials[2 * i]       = initials;
          group_indices[2 * i]        = initials + n_initials;
      
          final int n_initials2, initials2;
          n_initials2 = m_schema.getHeadSubstanceCountOfGroup(m_nd);
          initials2 = m_schema.getHeadSubstanceListOfGroup(m_nd);

          // uniform mapping table
          group_n_initials[2 * i + 1] = n_initials2;
          group_initials[2 * i + 1]   = initials2;
          group_indices[2 * i + 1]    = initials2 + n_initials2;
        }
        else {
          group_n_initials[2 * i]     = n_backward_initials;
          group_n_initials[2 * i + 1] = n_initials;
          group_initials[2 * i]       = initials;
          group_initials[2 * i + 1]   = initials;
          group_indices[2 * i]        = initials + n_initials;
          group_indices[2 * i + 1]    = initials + n_initials;
        }
      }
      else {
        group_n_initials[2 * i]     = n_initials;
        group_n_initials[2 * i + 1] = n_initials - n_backward_initials;
        group_initials[2 * i]       = initials;
        group_initials[2 * i + 1]   = initials + n_backward_initials;
        group_indices[2 * i]        = initials + n_initials;
        group_indices[2 * i + 1]    = initials + n_initials + n_backward_initials;
      }

      group_sortedInitials[2 * i] = new int[group_n_initials[2 * i]];
      sortInitials(group_sortedInitials[2 * i], group_initials[2 * i], group_n_initials[2 * i]);
      group_sortedInitials[2 * i + 1] = new int[group_n_initials[2 * i + 1]];
      sortInitials(group_sortedInitials[2 * i + 1], group_initials[2 * i + 1], group_n_initials[2 * i + 1]);
      
      byte acceptance;
      if (m_opaques[group_initials[2 * i] + (group_n_initials[2 * i] - 1)] == EXISchema.NIL_NODE) {
        if (group_n_initials[2 * i] == 1)
          acceptance = GROUP_STATE_DONE;
        else
          acceptance = GROUP_STATE_ACCEPTED;
      }
      else {
        acceptance = GROUP_STATE_NOT_DONE;
      }
      m_groupAcceptance[2 * i] = acceptance;
      
      if (m_opaques[group_initials[2 * i + 1] + (group_n_initials[2 * i + 1] - 1)] == EXISchema.NIL_NODE) {
        if (group_n_initials[2 * i + 1] == 1)
          acceptance = GROUP_STATE_DONE;
        else
          acceptance = GROUP_STATE_ACCEPTED;
      }
      else {
        acceptance = GROUP_STATE_NOT_DONE;
      }
      m_groupAcceptance[2 * i + 1] = acceptance;

      
      int len, pos, j;
      len = m_groupAcceptance[2 * i] == GROUP_STATE_NOT_DONE ?
        group_n_initials[2 * i] : group_n_initials[2 * i] + subsequentPid.length / 3 - 1; 
          
      m_initials[2 * i] = new int[len];
      m_indices[2 * i]  = new int[len];
      m_depths[2 * i]   = new int[len];
      
      for (pos = 0, j = 0, len = group_n_initials[2 * i]; j < len; j++) {
        final int particle = m_opaques[group_initials[2 * i] + j];
        assert particle != EXISchema.NIL_NODE || j == len - 1;
        if (particle != EXISchema.NIL_NODE) {
          m_initials[2 * i][pos] = particle;
          m_indices[2 * i][pos] = m_opaques[group_indices[2 * i] + j];
          m_depths[2 * i][pos] = 0;         
          ++pos;
        }
      }
      if (m_groupAcceptance[2 * i] != GROUP_STATE_NOT_DONE) {
        for (j = 0, len = subsequentPid.length / 3; j < len; j++) {
          m_initials[2 * i][pos] = subsequentPid[3 * j]; 
          m_indices[2 * i][pos] = subsequentPid[3 * j + 1];
          m_depths[2 * i][pos] = subsequentPid[3 * j + 2];         
          ++pos;
        }
      }
      assert pos == m_initials[2 * i].length;

      len = m_groupAcceptance[2 * i + 1] == GROUP_STATE_NOT_DONE ?
          group_n_initials[2 * i + 1] : group_n_initials[2 * i + 1] + subsequentPid.length / 3 - 1; 
            
      m_initials[2 * i + 1] = new int[len];
      m_indices[2 * i + 1]  = new int[len];
      m_depths[2 * i + 1]   = new int[len];
      
      for (pos = 0, j = 0, len = group_n_initials[2 * i + 1]; j < len; j++) {
        final int particle = m_opaques[group_initials[2 * i + 1] + j];
        assert particle != EXISchema.NIL_NODE || j == len - 1;
        if (particle != EXISchema.NIL_NODE) {
          m_initials[2 * i + 1][pos] = particle;
          m_indices[2 * i + 1][pos] = m_opaques[group_indices[2 * i + 1] + j];
          m_depths[2 * i + 1][pos] = 0;         
          ++pos;
        }
      }
      if (m_groupAcceptance[2 * i + 1] != GROUP_STATE_NOT_DONE) {
        for (j = 0, len = subsequentPid.length / 3; j < len; j++) {
          m_initials[2 * i + 1][pos] = subsequentPid[3 * j]; 
          m_indices[2 * i + 1][pos] = subsequentPid[3 * j + 1];
          m_depths[2 * i + 1][pos] = subsequentPid[3 * j + 2];         
          ++pos;
        }
      }
      assert pos == m_initials[2 * i + 1].length;
    }

    for (i = 0; i < m_eventTypes.length; i++) {
      EventTypeSchema[] eventTypeSchemas;          
      final ArrayList<AbstractEventType> eventTypeList = new ArrayList<AbstractEventType>();
      
      final int groupAcceptance = m_groupAcceptance[i];
      
      m_eventTypeLists[i] = new ArrayEventTypeList();

      if (groupAcceptance != GROUP_STATE_NOT_DONE) {
        int prev_sn, prev_particle, prev_term;
        prev_sn = -1;
        prev_particle = prev_term = EXISchema.NIL_NODE;
        for (int j = 0; j < subsequentEventTypes.length; j++) {
          final EventType eventType = subsequentEventTypes[j];
          if (eventType.isSchemaInformed()) {
            final EventTypeSchema eventTypeSchema = (EventTypeSchema)eventType;
            if (!eventTypeSchema.isAugmented()) {
              if (eventTypeSchema.itemType == EventCode.ITEM_SCHEMA_CH_MIXED) {
                assert mixed;
                continue;
              }
              else {
                eventTypeList.add(eventTypeSchema.duplicate(m_eventTypeLists[i]));
                int sn, particle, term;
                if (eventTypeSchema.itemType == EventCode.ITEM_SCHEMA_EE) {
                  particle = term = EXISchema.NIL_NODE;
                  sn = Integer.MAX_VALUE;
                }
                else {
                  particle = ((EventTypeSchemaParticle)eventTypeSchema).particle;
                  term = eventTypeSchema.getSchemaSubstance();
                  sn = m_schema.getSerialInTypeOfParticle(particle);
                }
                assert isAscending(prev_particle, prev_term, prev_sn, particle, term, sn);
                prev_sn = sn;
                prev_particle = particle;
                prev_term = term;
              }
            }
          }
        }
      }
      
      int len = groupAcceptance == GROUP_STATE_NOT_DONE ?
          group_n_initials[i] : group_n_initials[i] - 1;
      
      int j, serial;
      for (j = 0, serial = 0; j < len; j++) {
        final int substanceParticle1 = m_opaques[group_initials[i] + group_sortedInitials[i][j]];
        if (groupAcceptance != GROUP_STATE_NOT_DONE) {
          assert groupAcceptance == GROUP_STATE_ACCEPTED;
          final int term1 = m_schema.getTermOfParticle(substanceParticle1);
          final int sn1 = m_schema.getSerialInTypeOfParticle(substanceParticle1);
          while (serial < eventTypeList.size()) {
            final EventTypeSchema eventType = (EventTypeSchema)eventTypeList.get(serial);
            final int sn2, substanceParticle2, term2;
            if (eventType.itemType == EventCode.ITEM_SCHEMA_EE) {
              sn2 = Integer.MAX_VALUE;
              substanceParticle2 = term2 = EXISchema.NIL_NODE;
              assert sn1 != sn2;
            }
            else {
              substanceParticle2 = ((EventTypeSchemaParticle)eventType).particle;
              // serial number of a particle within the scope of the complex type
              sn2 = m_schema.getSerialInTypeOfParticle(substanceParticle2); 
              term2 = eventType.getSchemaSubstance();
              if (sn1 == sn2) {
                int particle1 = m_particles[m_opaques[group_indices[i] + group_sortedInitials[i][j]]];
                assert m_nodes[particle1] == EXISchema.PARTICLE_NODE;
                SchemaInformedGrammar grammar = (SchemaInformedGrammar)eventType.getGrammar();
                assert grammar instanceof SequenceChoiceGrammar || grammar instanceof ComplexContentGrammar;
                final int particle2;
                if (grammar instanceof SequenceChoiceGrammar) {
                  SequenceChoiceGrammar sequenceChoice = (SequenceChoiceGrammar)grammar;
                  int k;
                  for (k = 0; k < subsequentPid.length / 3; k++) {
                    if (subsequentPid[3 * k] == substanceParticle2)
                      break;
                  }
                  assert k < subsequentPid.length / 3;
                  assert m_depth - sequenceChoice.m_depth == subsequentPid[3 * k + 2]; 
                  int index = subsequentPid[3 * k + 1];
                  particle2 = sequenceChoice.m_particles[index];
                }
                else {
                  assert grammar instanceof ComplexContentGrammar;
                  particle2 = m_schema.getContentTypeOfComplexType(grammar.m_nd);
                }
                assert particle1 != particle2;
                if (m_schema.getMaxOccursOfParticle(particle2) > 1 || m_schema.getMinOccursOfParticle(particle2) > 1) {
                  int maxOccurs1 = m_schema.getMaxOccursOfParticle(particle1);
                  if (maxOccurs1 > 1 || maxOccurs1 == EXISchema.UNBOUNDED_OCCURS) {
                    if (checkSanity) {
                      final int substanceParticleTerm1 = m_schema.getTermOfParticle(substanceParticle1);
                      final boolean isElementTerm = m_schema.getNodeType(substanceParticleTerm1) == EXISchema.ELEMENT_NODE;
                      GrammarRuntimeException gre;
                      if (isElementTerm) {
                        gre = new GrammarRuntimeException(GrammarRuntimeException.AMBIGUOUS_CONTEXT_OF_ELEMENT_PARTICLE,
                            new String[] { m_schema.getNameOfElem(substanceParticleTerm1) });
                      }
                      else {
                        gre = new GrammarRuntimeException(GrammarRuntimeException.AMBIGUOUS_CONTEXT_OF_WILDCARD_PARTICLE,
                            new String[] {});
                      }
                      gre.setNode(substanceParticle1);
                      throw gre;
                    }
                  }
                }
                if (m_schema.getMaxOccursOfParticle(particle1) > 1 || m_schema.getMinOccursOfParticle(particle1) > 1) {
                  int maxOccurs2 = m_schema.getMaxOccursOfParticle(particle2);
                  if (maxOccurs2 > 1 || maxOccurs2 == EXISchema.UNBOUNDED_OCCURS) {
                    if (checkSanity) {
                      final int substanceParticleTerm2 = m_schema.getTermOfParticle(substanceParticle2);
                      final boolean isElementTerm = m_schema.getNodeType(substanceParticleTerm2) == EXISchema.ELEMENT_NODE;
                      GrammarRuntimeException gre;
                      if (isElementTerm) {
                        gre = new GrammarRuntimeException(GrammarRuntimeException.AMBIGUOUS_CONTEXT_OF_ELEMENT_PARTICLE,
                            new String[] { m_schema.getNameOfElem(substanceParticleTerm2) });
                      }
                      else {
                        gre = new GrammarRuntimeException(GrammarRuntimeException.AMBIGUOUS_CONTEXT_OF_WILDCARD_PARTICLE,
                            new String[] {});
                      }
                      gre.setNode(substanceParticle2);
                      throw gre;
                    }
                  }
                }
                eventTypeList.remove(serial);
                continue;
              }
              
            }
            if (!isAscending(substanceParticle1, term1, sn1, substanceParticle2, term2, sn2)) {
              ++serial;
            }
            else {
              assert isAscending(substanceParticle1, term1, sn1, substanceParticle2, term2, sn2);
              break;
            }
          }
          eventTypeSchemas = createEventType(m_opaques[group_initials[i] + group_sortedInitials[i][j]], 
              group_sortedInitials[i][j], serial, this, m_eventTypeLists[i]);
          
          for (int k = 0; k < eventTypeSchemas.length; k++) {
            final EventTypeSchema kth = eventTypeSchemas[k];
            eventTypeList.add(serial++, kth);
          }
        }
        else {
          eventTypeSchemas = createEventType(m_opaques[group_initials[i] + group_sortedInitials[i][j]], 
              group_sortedInitials[i][j], serial, this, m_eventTypeLists[i]);
          for (int k = 0; k < eventTypeSchemas.length; k++) {
            eventTypeList.add(eventTypeSchemas[k]);
            ++serial;
          }
        }
      }
      if (mixed) {
        eventTypeList.add(new EventTypeSchemaMixedCharacters(serial++, this, m_eventTypeLists[i]));
      }

      EventCodeTupleSink res;
      createEventCodeTuple(eventTypeList, cache.grammarOptions, res = new EventCodeTupleSink(), m_eventTypeLists[i]);
      
      m_eventCodes[i] = res.eventCodeTuple;
      m_eventTypes[i] = res.eventTypes;
      m_eventTypeLists[i].setItems(res.eventTypes); 
    }
    
    // populate m_groupStates array
    for (i = 0; i < m_n_particles; i++) {
      // Groups relevant to i-th particle are 3*i, 3*i+1 and 3*i+2
      final int groupBase = 3 * i;
      m_groupStates[groupBase + 0] = null;
      m_groupStates[groupBase + 1] = null;
      m_groupStates[groupBase + 2] = null;
      // entries are zeros if the relevant particle is not a group.
      final int term = m_nodes[m_particles[i] + EXISchemaLayout.PARTICLE_TERM];
      if (term != EXISchema.NIL_NODE && m_nodes[term] == EXISchema.GROUP_NODE) {
        int _tripletStart, _n_triplets;
        final int tripletBase = 3 * m_n_triplets;
        if (m_compositor == EXISchema.GROUP_SEQUENCE) {
          // Given i-th particle, each group represents cursor position
          // 2*(i+1)-1, 2*(i+1) and 2*(i+1)+1, respectively
          for (int j = 0, cursor = 2*(i+1)-1; j < 3; j++, cursor++) {
            final int state;
            if ((state = m_groupAcceptance[cursor]) != GROUP_STATE_DONE) {
              assert state == GROUP_STATE_NOT_DONE || state == GROUP_STATE_ACCEPTED;
              // push the subsequent triplet (n_initials, initials and indices)
              _triplets[tripletBase + 0] = group_n_initials[cursor];
              _triplets[tripletBase + 1] = group_initials[cursor];
              _triplets[tripletBase + 2] = group_indices[cursor];
              _n_triplets = m_n_triplets + 1;
              if (state == GROUP_STATE_NOT_DONE) {
                // inherited triplets are not visible if not done
                _tripletStart = m_n_triplets;
              }
              else
                _tripletStart = 0;
            }
            else { // GROUP_STATE_DONE
              _triplets[tripletBase + 0] = group_n_initials[cursor];
              _triplets[tripletBase + 1] = group_initials[cursor];
              _triplets[tripletBase + 2] = group_indices[cursor];
              _n_triplets = m_n_triplets + 1;
              _tripletStart = 0;
            }
            m_groupStates[groupBase + j] = GroupGrammar.createGroupState(
                term, this, cache, m_schema, _triplets, _tripletStart, _n_triplets, 
                mixed, m_eventTypes[cursor], checkSanity);
          }
        }
        else {
          assert m_compositor == EXISchema.GROUP_CHOICE;
          int j, cursor;
          // Given i-th particle, each group represents cursor position
          // 2*i, 2*i+1 and 2*m_n_particles, respectively
          for (j = 0, cursor = 2*i; j < 2; j++, cursor++) {
            final int state;
            if ((state = m_groupAcceptance[cursor]) != GROUP_STATE_DONE) {
              assert state == GROUP_STATE_NOT_DONE || state == GROUP_STATE_ACCEPTED;
              // push the subsequent triplet (n_initials, initials and indices)
              _triplets[tripletBase + 0] = group_n_initials[cursor];
              _triplets[tripletBase + 1] = group_initials[cursor];
              _triplets[tripletBase + 2] = group_indices[cursor];
              _n_triplets = m_n_triplets + 1;
              if (state == GROUP_STATE_NOT_DONE) {
                // inherited triplets are not visible if not done
                _tripletStart = m_n_triplets;
              }
              else
                _tripletStart = 0;
            }
            else { // GROUP_STATE_DONE
              _triplets[tripletBase + 0] = group_n_initials[cursor];
              _triplets[tripletBase + 1] = group_initials[cursor];
              _triplets[tripletBase + 2] = group_indices[cursor];
              _n_triplets = m_n_triplets + 1;
              _tripletStart = 0;
            }
            m_groupStates[groupBase + j] = GroupGrammar.createGroupState(
                term, this, cache, m_schema, _triplets, _tripletStart, _n_triplets, 
                mixed, m_eventTypes[cursor], checkSanity);
          }
          _triplets[tripletBase + 0] = group_n_initials[2 * m_n_particles];
          _triplets[tripletBase + 1] = group_initials[2 * m_n_particles];
          _triplets[tripletBase + 2] = group_indices[2 * m_n_particles];
          m_groupStates[groupBase + 2] = GroupGrammar.createGroupState(
              term, this, cache, m_schema, _triplets, 0, m_n_triplets + 1, 
              mixed, m_eventTypes[2 * m_n_particles], checkSanity);
        }
      }
    }

    final int _n_triplets = m_n_triplets + 1;
    for (i = 0; i <= m_n_particles; i++) {
      int acceptance;
      _triplets[3 * m_n_triplets + 0] = group_n_initials[2 * i];
      _triplets[3 * m_n_triplets + 1] = group_initials[2 * i];
      acceptance = IS_DONE;
      for (int j = _n_triplets - 1; j >= 0 && acceptance != IS_NOT_DONE; j--) {
        final int n_initials = _triplets[3 * j + 0];
        final int initials = _triplets[3 * j + 1];
        if (m_opaques[initials + n_initials - 1] == EXISchema.NIL_NODE) {
          if (n_initials != 1)
            acceptance = IS_ACCEPTED;
        }
        else
          acceptance = IS_NOT_DONE;
      }
      
      _triplets[3 * m_n_triplets + 0] = group_n_initials[2 * i + 1];
      _triplets[3 * m_n_triplets + 1] = group_initials[2 * i + 1];
      acceptance = IS_DONE;
      for (int j = _n_triplets - 1; j >= 0 && acceptance != IS_NOT_DONE; j--) {
        final int n_initials = _triplets[3 * j + 0];
        final int initials = _triplets[3 * j + 1];
        if (m_opaques[initials + n_initials - 1] == EXISchema.NIL_NODE) {
          if (n_initials != 1)
            acceptance = IS_ACCEPTED;
        }
        else
          acceptance = IS_NOT_DONE;
      }
    }
    
  }
  
  @Override
  public final void init(GrammarState stateVariables) {
    super.init(stateVariables);
    stateVariables.occurs = 0;
    // It starts with uniform mapping table for choice, otherwise 0
    stateVariables.cursor = m_compositor == EXISchema.GROUP_CHOICE ? 2 * m_n_particles + 1 : 0;
    stateVariables.phase = m_groupAcceptance[stateVariables.cursor];
  }
  
  ///////////////////////////////////////////////////////////////////////////
  /// Implementation of abstract methods declared in CommonState
  ///////////////////////////////////////////////////////////////////////////

  @Override
  EventTypeList getNextEventTypes(GrammarState stateVariables) {
    return m_eventTypeLists[stateVariables.cursor]; 
  }

  @Override
  EventCodeTuple getNextEventCodes(GrammarState stateVariables) {
    return m_eventCodes[stateVariables.cursor];
  }

  @Override
  void element(int eventTypeIndex, final String uri, final String name, final GrammarState stateVariables) {
    
    final EventTypeSchema schemaEventType = 
      (EventTypeSchema)m_eventTypes[stateVariables.cursor][eventTypeIndex];

    assert schemaEventType.itemType == EventCode.ITEM_SCHEMA_SE || 
      schemaEventType.itemType == EventCode.ITEM_SCHEMA_WC_ANY ||
      schemaEventType.itemType == EventCode.ITEM_SCHEMA_WC_NS;
    assert stateVariables.phase != GROUP_STATE_DONE;

    int i;
    
    final int[] initials = m_initials[stateVariables.cursor];
    
    if (schemaEventType.getGrammar() == this) {
      final int matchedPos = schemaEventType.index;
      final int ind = m_indices[stateVariables.cursor][matchedPos];
      int substance = m_schema.getTermOfParticle(initials[matchedPos]);
      
      if (m_compositor == EXISchema.GROUP_SEQUENCE) {
        /**
         * 0 (0 1 2), 1 (3 4), 2 (5 6), 3 (7 8), etc.
         * Eg: the index of interest at the cursor 3 or 4 is 1.
         */
        final int prevInd = stateVariables.cursor != 0 ? (stateVariables.cursor + 1) / 2 - 1 : 0;
        if (ind != prevInd)
          stateVariables.occurs = 0;
      }
      final int termOfParticle = m_nodes[m_particles[ind] + EXISchemaLayout.PARTICLE_TERM];
      // Use the one really matched for elements
      final int term = m_nodes[termOfParticle] == EXISchema.ELEMENT_NODE ? 
          schemaEventType.getSchemaSubstance() : termOfParticle;
      stateVariables.occurs++;
      if (m_compositor == EXISchema.GROUP_SEQUENCE) {
        // An odd cursor always move forward whereas an even cursor may move one step backward
        assert stateVariables.cursor % 2 == 1 && (stateVariables.cursor - 1) / 2 <= ind || 
          stateVariables.cursor % 2 == 0 && stateVariables.cursor / 2 - 1 <= ind;
        stateVariables.cursor = 2 * (ind + 1); 
        // Move the cursor backward if the particle we just went through
        // still falls short of minOccurs.
        if (stateVariables.occurs < m_minOccurs[ind]) {
          --stateVariables.cursor; // may need to be predicated on m_isFixtureTerm[prevInd]?
        }
        else if (stateVariables.occurs >= m_maxOccurs[ind]) {
          ++stateVariables.cursor;
          stateVariables.occurs = 0;
        }
      }
      else if (m_compositor == EXISchema.GROUP_CHOICE) {
        stateVariables.cursor = 2 * ind + 1;
        if (stateVariables.occurs < m_minOccurs[ind] && m_isFixtureTerm[ind])
          --stateVariables.cursor;
        else if (stateVariables.occurs >= m_maxOccurs[ind])
          stateVariables.cursor = 2 * m_n_particles;
      }
      /**
       * Update state up front here. it used to be done in done().
       * updating state early was necessary to implement getAcceptance()
       */
      stateVariables.phase = m_groupAcceptance[stateVariables.cursor];
      final GrammarState kid;
      switch (m_nodes[termOfParticle]) {
        case EXISchema.ELEMENT_NODE:
          kid = stateVariables.documentGrammarState.pushState();
          m_grammarCache.retrieveElementGrammar(term).init(kid);
          return;
        case EXISchema.WILDCARD_NODE:
          substance = getElementBroadly(uri, name);
          if (substance != EXISchema.NIL_NODE) {
            kid = stateVariables.documentGrammarState.pushState();
            m_grammarCache.retrieveElementGrammar(substance).init(kid);
          }
          else {
            kid = stateVariables.documentGrammarState.pushState();
            final DocumentGrammarState documentGrammarState = stateVariables.documentGrammarState;
            documentGrammarState.builtinGrammarCache.retrieveElementGrammar(
                uri, name, m_grammarCache, documentGrammarState.eventTypesWorkSpace).init(kid);
          }
          break;
        default: // case EXISchema.GROUP_NODE:
          assert m_nodes[term] == EXISchema.GROUP_NODE;
          if (m_compositor == EXISchema.GROUP_SEQUENCE) {
            kid = stateVariables.documentGrammarState.pushState();
            m_groupStates[3 * ind + (stateVariables.cursor - (2 * ind + 1))].init(kid);
          }
          else {
            assert m_compositor == EXISchema.GROUP_CHOICE;
            kid = stateVariables.documentGrammarState.pushState();
            if (stateVariables.cursor == 2 * m_n_particles)
              m_groupStates[3 * ind + 2].init(kid);
            else
              m_groupStates[3 * ind + (stateVariables.cursor - (2 * ind))].init(kid);
          }
          boolean assertResult;
          assertResult = ((SchemaInformedGrammar)kid.targetGrammar).schemaElement(schemaEventType, uri, name, kid);
          assert assertResult;
          return;
      }
    }
    else { // depth > 0, delegate to one of the ancestors
      final DocumentGrammarState documentGrammarState = stateVariables.documentGrammarState;
      GrammarState ancestorStateVariables = null;
      Grammar grammar = this;
      int depth;
      for (depth = 0; schemaEventType.getGrammar() != grammar; depth++) {
        grammar = ((GroupGrammar)grammar).m_parent;
      }
      for (i = 0; i < depth; i++) {
        ancestorStateVariables = documentGrammarState.currentState;
        documentGrammarState.popState();
      }
      GrammarState peekedState = documentGrammarState.currentState;
      peekedState.targetGrammar.done(ancestorStateVariables, peekedState);
      
      ((SchemaInformedGrammar)peekedState.targetGrammar).element(schemaEventType.serial, uri, name, peekedState);
    }
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