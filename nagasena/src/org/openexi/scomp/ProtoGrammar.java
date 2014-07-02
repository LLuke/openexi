package org.openexi.scomp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.Set;

class ProtoGrammar extends RightHandSide implements Comparable<ProtoGrammar> {
  
  private final TreeSet<Substance> sortedSubstances;
  private final List<ProtoGrammar> linkedGrammars;
  
  private Set<Goal> goalBag;
  
  private final TreeSet<ProtoGrammar> synthesis;
  
  public static final int NO_INDEX = -1;
  /**
   * Index of this proto grammar in the type grammar it belongs to
   */
  private int m_index;

  /**
   * Serial number of non-synthetic grammar.
   */
  private final int m_serialNumber;

  /** 
   * EXISchemaFactory that holds proto grammar serial number. 
   */
  private final EXISchemaFactory m_schemaFactory;

  public ProtoGrammar(int serialNumber, EXISchemaFactory schemaFactory) {
    this(serialNumber, (TreeSet<ProtoGrammar>)null, schemaFactory);
  }
  
  private ProtoGrammar(int serialNumber, TreeSet<ProtoGrammar> synthesis, EXISchemaFactory schemaFactory) {
    sortedSubstances = new TreeSet<Substance>();
    linkedGrammars = new ArrayList<ProtoGrammar>();
    
    goalBag = new HashSet<Goal>();
    m_index = NO_INDEX;
    
    this.synthesis = synthesis;
    
    m_serialNumber = serialNumber;
    m_schemaFactory = schemaFactory;
  }

  private ProtoGrammar createSyntheticProtoGrammar() {
    return new ProtoGrammar(m_schemaFactory.protoGrammarSerial++, new TreeSet<ProtoGrammar>(), m_schemaFactory);
  }
  
  public int compareTo(ProtoGrammar protoGrammar) {
    return m_serialNumber - protoGrammar.m_serialNumber;
  }
  
  @Override
  public final RHSType getRHSType() {
    return RHSType.GRAMMAR;
  }

  public int getIndex() {
    return m_index;
  }

  public Set<Goal> getGoalBag() {
    return goalBag;
  }

  public Substance[] getSubstances() {
    assert linkedGrammars.size() == 0;
    return sortedSubstances.toArray(new Substance[0]);
  }

  public boolean hasGoal() {
    Iterator<Substance> iterSubstances = sortedSubstances.iterator();
    while (iterSubstances.hasNext()) {
      if (iterSubstances.next().getRHSType() == RHSType.GOAL)
        return true;
    }
    return false;
  }

  public boolean isImmutableEnd() {
    assert linkedGrammars.size() == 0;
    // ones with index of NO_INDEX are immutable. 
    // Preceding ones are subject to attribute production insertions. 
    return m_index == NO_INDEX && sortedSubstances.size() == 1 && hasGoal();
  }
  
  private boolean isProxy() {
    return sortedSubstances.size() == 0 && linkedGrammars.size() == 1;
  }
  
  final void normalize(Set<ProtoGrammar> visitedGrammars, ArrayList<ProtoGrammar> syntheticGrammarRegistry) {
    if (visitedGrammars.contains(this))
      return;
    visitedGrammars.add(this);

    Iterator<Substance> iterSubstances;
    Set<ProtoGrammar> processedGrammars = new HashSet<ProtoGrammar>();
    boolean repeat;
    do {
      repeat = false;
      final Object[] grammars = linkedGrammars.toArray();
      for (int i = 0; i < grammars.length; i++) {
        final ProtoGrammar rhsGrammar = (ProtoGrammar)grammars[i];
        linkedGrammars.remove(rhsGrammar);
        if (this == rhsGrammar || processedGrammars.contains(rhsGrammar))
          continue;
        processedGrammars.add(rhsGrammar);
        iterSubstances = rhsGrammar.sortedSubstances.iterator();
        while (iterSubstances.hasNext())
          addSubstance(iterSubstances.next(), syntheticGrammarRegistry);
        if (linkedGrammars.addAll(rhsGrammar.linkedGrammars))
          repeat = true;
      }
    } while (repeat);
    
    iterSubstances = sortedSubstances.iterator();
    while (iterSubstances.hasNext()) {
      Substance substance = (Substance)iterSubstances.next();
      if (substance.getRHSType() == RHSType.PROD) {
        final Production production = (Production)substance;
        ProtoGrammar subsequentGrammar = production.getSubsequentGrammar();
        while (subsequentGrammar.isProxy()) {
          subsequentGrammar = subsequentGrammar.linkedGrammars.get(0);
        }
        if (subsequentGrammar != production.getSubsequentGrammar()) {
          production.setSubsequentGrammar(subsequentGrammar);
        }
        subsequentGrammar.normalize(visitedGrammars, syntheticGrammarRegistry);
      }
    }
  }
  
  void setIndex(int index) {
    m_index = index;
  }

  void appendProtoGrammar(ProtoGrammar protoGrammar) {
    linkedGrammars.add(protoGrammar);
  }
  
  void importGoals(ProtoGrammar protoGrammar) {
    final Iterator<Goal> iterGoals = protoGrammar.goalBag.iterator();
    while (iterGoals.hasNext()) {
      goalBag.add(iterGoals.next());
    }
  }

  void entail(ProtoGrammar protoGrammar) {
    Iterator<Goal> iterGoals = goalBag.iterator();
    while (iterGoals.hasNext()) {
      final Goal goal = iterGoals.next();
      ProtoGrammar ownerGrammar = goal.getOwnerGrammar();
      ownerGrammar.sortedSubstances.remove(goal);
      if (ownerGrammar != protoGrammar) {
        ownerGrammar.linkedGrammars.add(protoGrammar);
      }
    }
    goalBag.clear();
    if (this != protoGrammar)
      importGoals(protoGrammar);
  }
  
  void addSubstance(Substance substance, ArrayList<ProtoGrammar> syntheticGrammarRegistry) {
    Iterator<Substance> iterSubstances = sortedSubstances.iterator();
    while (iterSubstances.hasNext()) {
      Substance nextSubstance;
      if ((nextSubstance = iterSubstances.next()).equals(substance))
        return;
      if (nextSubstance.getPriority() == substance.getPriority()) {
        final Production nextProduction = (Production)nextSubstance;
        Production production = (Production)substance;
        if (nextProduction.getEvent().equals(production.getEvent())) {
          assert nextProduction.getParticleNumber() == production.getParticleNumber();
          final Event event = nextProduction.getEvent();
          assert event.getEventType() != Event.CHARACTERS_TYPED;
          if (event.getEventType() == Event.CHARACTERS_MIXED) {
            return;
          }
          ProtoGrammar protoGrammar1 = nextProduction.getSubsequentGrammar();
          ProtoGrammar protoGrammar2 = production.getSubsequentGrammar();
          assert protoGrammar1 != protoGrammar2;
          ProtoGrammar protoGrammar;
          if (protoGrammar1.synthesis != null || protoGrammar2.synthesis != null) {
            if (protoGrammar1.synthesis != null && protoGrammar2.synthesis == null) {
              if (protoGrammar1.synthesis.contains(protoGrammar2))
                return;
            }
            else if (protoGrammar1.synthesis == null && protoGrammar2.synthesis != null) {
              if (protoGrammar2.synthesis.contains(protoGrammar1)) {
                sortedSubstances.remove(nextProduction);
                sortedSubstances.add(production);
                return;
              }
            }
            else {
              final boolean swapped;
              if (swapped = (protoGrammar1.synthesis.size() < protoGrammar2.synthesis.size())) {
                protoGrammar = protoGrammar1;
                protoGrammar1 = protoGrammar2;
                protoGrammar2 = protoGrammar;
              }
              Object[] subsumedGrammars2 = protoGrammar2.synthesis.toArray();
              int i;
              for (i = 0; i < subsumedGrammars2.length; i++) {
                if (!protoGrammar1.synthesis.contains(subsumedGrammars2[i]))
                  break;
              }
              if (i == subsumedGrammars2.length) {
                if (swapped) {
                  sortedSubstances.remove(nextProduction);
                  sortedSubstances.add(production);
                }
                return;
              }
            }
          }
          protoGrammar = createSyntheticProtoGrammar();
          protoGrammar.appendProtoGrammar(protoGrammar1);
          if (protoGrammar1.synthesis != null)
            protoGrammar.synthesis.addAll(protoGrammar1.synthesis);
          else
            protoGrammar.synthesis.add(protoGrammar1);
          protoGrammar.appendProtoGrammar(protoGrammar2);
          if (protoGrammar2.synthesis != null)
            protoGrammar.synthesis.addAll(protoGrammar2.synthesis);
          else
            protoGrammar.synthesis.add(protoGrammar2);
          
          int i;
          for (i = 0; i < syntheticGrammarRegistry.size(); i++) {
            final ProtoGrammar syntheticGrammar = syntheticGrammarRegistry.get(i);
            Iterator<ProtoGrammar> iterGrammar1 = protoGrammar.synthesis.iterator();
            Iterator<ProtoGrammar> iterGrammar2 = syntheticGrammar.synthesis.iterator();
            final int n_synthesis;
            if ((n_synthesis = protoGrammar.synthesis.size()) == syntheticGrammar.synthesis.size()) {
              int j;
              for (j = 0; iterGrammar1.hasNext(); j++) {
                if (iterGrammar1.next().m_serialNumber != iterGrammar2.next().m_serialNumber)
                  break;
              }
              if (j == n_synthesis) {
                // use the existing synthetic grammar
                protoGrammar = syntheticGrammar;
                break;
              }
            }
          }
          if (i == syntheticGrammarRegistry.size()) {
            syntheticGrammarRegistry.add(protoGrammar);
          }
          sortedSubstances.remove(nextProduction);
          sortedSubstances.add(new Production(nextProduction.getEvent(), protoGrammar, nextProduction.getParticleNumber()));
          return;
        }
      }      
    }
    sortedSubstances.add(substance);
  }

}
