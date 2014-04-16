package org.openexi.scomp;

abstract class EventQName extends Event {

  public abstract String getUri();
  
  public abstract String getLocalName();

  @Override
  public final boolean equals(Object obj) {
    if (obj instanceof EventQName) {
      final EventQName eventQName = (EventQName)obj;
      return getUri().equals(eventQName.getUri()) && getLocalName().equals(eventQName.getLocalName());
    }
    return false;
  }

}
