package org.openexi.scomp;

abstract class EventSEWildcard extends Event {
  
  @Override
  public byte getEventType() {
    return ELEMENT_WILDCARD;
  }

  @Override
  public final boolean equals(Object obj) {
    return obj instanceof EventSEWildcard;
  }

}
