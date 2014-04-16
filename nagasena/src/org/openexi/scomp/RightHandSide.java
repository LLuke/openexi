package org.openexi.scomp;

abstract class RightHandSide {
  
  enum RHSType {
    PROD,
    GOAL,
    GRAMMAR
  }
  
  public abstract RHSType getRHSType();
  
}
