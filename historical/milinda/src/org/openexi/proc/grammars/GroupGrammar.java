package org.openexi.proc.grammars;

import org.openexi.proc.common.EventType;
import org.openexi.schema.EXISchema;

public abstract class GroupGrammar extends SchemaInformedGrammar {

  protected static final byte GROUP_STATE_CREATED     = 0;
  protected static final byte GROUP_STATE_NOT_DONE    = 1;
  protected static final byte GROUP_STATE_ACCEPTED    = 2;
  protected static final byte GROUP_STATE_DONE        = 3;

  ///////////////////////////////////////////////////////////////////////////
  /// immutables
  ///////////////////////////////////////////////////////////////////////////

  protected final int m_n_particles;
  // particles in the group
  protected final int[] m_particles;

  protected final int m_compositor;
  
  protected final int m_depth; // first-level group has depth value of 0.
  protected final Grammar m_parent;

  ///////////////////////////////////////////////////////////////////////////
  /// constructors, initializers
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Create a group state.
   * @param group group node
   * @param triplets array of triplets
   * @param tripletStart the first triplet in scope for the group state
   * @param n_triplets the total number of triplets contained in the triplet array
   * @return a group state
   */
  final static GroupGrammar createGroupState(int group, Grammar parent, GrammarCache cache, 
      EXISchema corpus, int[] triplets, int tripletStart, int n_triplets, 
      boolean mixed, EventType[] subsequentEventTypes, boolean checkSanity) {
    final int compositor = corpus.getCompositorOfGroup(group);
    if (compositor == EXISchema.GROUP_ALL) {
      /**
       * AllGroupState is always the immediate child of ComplexContentState,
       * and the ComplexContentState is complete at the time AllGroupState
       * is spawned. All in all, triplets are not very useful in AllGroupState.
       */
      assert tripletStart == 0 && n_triplets == 1;
      assert triplets[0] == 1 && corpus.getOpaques()[triplets[1]] == EXISchema.NIL_NODE;
      return new AllGroupGrammar(group, parent, cache);
    }
    else
      return new SequenceChoiceGrammar(group, parent, cache, triplets, tripletStart, n_triplets, 
          mixed, subsequentEventTypes, checkSanity);
  }
  
  protected GroupGrammar(int group, Grammar parent, GrammarCache cache) { 
    super(group, SCHEMA_GRAMMAR_GROUP, cache);

    m_parent = parent;
    int depth = 0;
    for (; parent instanceof GroupGrammar; ++depth) {
      parent = ((GroupGrammar)parent).m_parent;
    }
    m_depth = depth;
    
    m_compositor = m_schema.getCompositorOfGroup(m_nd);

    m_n_particles = m_schema.getParticleCountOfGroup(m_nd);

    m_particles = new int[m_n_particles];

    for (int i = 0; i < m_n_particles; i++)
        m_particles[i] = m_schema.getParticleOfGroup(m_nd, i);
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Accessors
  ///////////////////////////////////////////////////////////////////////////
  
  public int getCompositor() {
    return m_compositor;
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Implementation of abstract methods declared in CommonState
  ///////////////////////////////////////////////////////////////////////////

  @Override
  public final void chars(GrammarState stateVariables) {
    // nothing to do.
  }

  @Override
  public final void undeclaredChars(GrammarState stateVariables) {
    // nothing to do.
  }

  @Override
  public final void miscContent(GrammarState stateVariables) {
    // nothing to do.
  }

  @Override
  final void done(GrammarState kid, GrammarState stateVariables) {
    if (stateVariables.phase == GROUP_STATE_DONE)
      finish(stateVariables);
  }

  @Override
  final void end(String uri, String name, GrammarState stateVariables) {
    DocumentGrammarState schemaVM = stateVariables.documentGrammarState;
    finish(stateVariables);
    // peek state since finish() may have caused to the parent
    // (group in particular) to finish.
    GrammarState peekedState = schemaVM.currentState;
    peekedState.targetGrammar.end(uri, name, peekedState);
  }

}