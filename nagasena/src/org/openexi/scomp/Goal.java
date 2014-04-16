package org.openexi.scomp;

import java.util.Set;

final class Goal extends Substance {

  private final ProtoGrammar m_ownerProtoGrammar;
  
  public Goal(ProtoGrammar protoGrammar) {
    this(protoGrammar, protoGrammar.getGoalBag());
  }
  
  public Goal(ProtoGrammar protoGrammar, Set<Goal> goalBag) {
    m_ownerProtoGrammar = protoGrammar;
    (goalBag != null ? goalBag : m_ownerProtoGrammar.getGoalBag()).add(this);
  }

  @Override
  public final boolean isProduction() {
    return false;
  }

  @Override
  public final RHSType getRHSType() {
    return RHSType.GOAL;
  }

  @Override
  final short getPriority() {
    return Event.END_ELEMENT;
  }

  public ProtoGrammar getOwnerGrammar() {
    return m_ownerProtoGrammar;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof Goal;
  }

}
