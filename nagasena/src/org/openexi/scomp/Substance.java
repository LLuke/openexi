package org.openexi.scomp;

abstract class Substance extends RightHandSide implements Comparable<Substance> {

  /**
   * Determines if the substance is a production.
   * @return true if it is a production, otherwise (i.e. goal) returns false
   */
  public abstract boolean isProduction();

  public int compareTo(Substance substance) {
    final int priority1 = getPriority();
    final int priority2 = substance.getPriority();
    if (priority1 != priority2) {
      return priority1 - priority2;
    }
    switch (priority1) {
      case Event.ATTRIBUTE:
        return Production.compareAT((Production)this, (Production)substance);
      case Event.ATTRIBUTE_WILDCARD_NS:
        return Production.compareATWildcardNS((Production)this, (Production)substance);
      case Event.ELEMENT:
      case Event.ELEMENT_WILDCARD_NS:
      case Event.ELEMENT_WILDCARD:
        return Production.compareSE((Production)this, (Production)substance);
      case Event.END_ELEMENT:
        return 0;
      case Event.CHARACTERS_MIXED:
      case Event.CHARACTERS_TYPED:
        assert this == substance;
        return 0;
      case Event.ATTRIBUTE_WILDCARD:
      default:
        assert false;
        return 0;
    }
  }

  abstract short getPriority();
  
}
