package org.openexi.scomp;

abstract class EventATWildcard extends Event {
  
  @Override
  public byte getEventType() {
    return ATTRIBUTE_WILDCARD;
  }

  @Override
  public final boolean equals(Object obj) {
    return obj instanceof EventATWildcard;
  }
  
}
