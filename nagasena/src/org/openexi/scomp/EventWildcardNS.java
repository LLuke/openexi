package org.openexi.scomp;

abstract class EventWildcardNS extends Event {

  private final String m_uri;

  protected EventWildcardNS(String uri) {
    m_uri = uri;
  }
  
  public final String getUri() {
    return m_uri;
  }

  @Override
  public final boolean equals(Object obj) {
    if (obj instanceof EventWildcardNS) {
      final EventWildcardNS eventWildcardNS = (EventWildcardNS)obj;
      return getEventType() == eventWildcardNS.getEventType() && m_uri.equals(eventWildcardNS.m_uri);
    }
    return false;
  }
  
}
