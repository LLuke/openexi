package org.openexi.scomp;

import java.util.HashMap;
import java.util.Map;

import org.apache.xerces.xs.XSAttributeDeclaration;
import org.apache.xerces.xs.XSElementDeclaration;

class EventTypeCache {
  
  private final Map<XSAttributeDeclaration,EventAT> m_attributeCache;
  private final Map<String,EventATWildcardNS> m_attributeWildcardNSCache;
  private final Map<XSElementDeclaration,EventSE> m_elementCache;
  private final Map<String,EventSEWildcardNS> m_elementWildcardNSCache;

  static final EventATWildcard eventATWildcard;
  static final EventSEWildcard eventSEWildcard;
  static final EventCharactersMixed eventCharactersUntyped;
  static final EventCharactersTyped eventCharactersTyped;
  static {
    eventATWildcard = new EventATWildcard() {};
    eventSEWildcard = new EventSEWildcard() {};
    eventCharactersUntyped = new EventCharactersMixed() {};
    eventCharactersTyped = new EventCharactersTyped() {};
  }
  
  EventTypeCache() {
    m_attributeCache = new HashMap<XSAttributeDeclaration,EventAT>();
    m_attributeWildcardNSCache = new HashMap<String,EventATWildcardNS>();
    m_elementCache = new HashMap<XSElementDeclaration,EventSE>();
    m_elementWildcardNSCache = new HashMap<String,EventSEWildcardNS>();
  }

  public final void clear() {
    m_attributeCache.clear();
    m_attributeWildcardNSCache.clear();
    m_elementCache.clear();
    m_elementWildcardNSCache.clear();
  }
  
  public final EventAT getEventAT(XSAttributeDeclaration attributeDeclaration) {
    EventAT eventType;
    if ((eventType = m_attributeCache.get(attributeDeclaration)) != null) {
      return eventType;
    }
    eventType = new EventAT(attributeDeclaration) {};
    m_attributeCache.put(attributeDeclaration, eventType);
    return eventType;
  }
  
  public final EventATWildcardNS getEventATWildcardNS(String uri) {
    assert uri != null;
    EventATWildcardNS eventType;
    if ((eventType = m_attributeWildcardNSCache.get(uri)) != null) {
      return eventType;
    }
    eventType = new EventATWildcardNS(uri) {};
    m_attributeWildcardNSCache.put(uri, eventType);
    return eventType;
  }
  
  public final EventSE getEventSE(XSElementDeclaration elementDeclaration) {
    EventSE eventType;
    if ((eventType = m_elementCache.get(elementDeclaration)) != null) {
      return eventType;
    }
    eventType = new EventSE(elementDeclaration) {};
    m_elementCache.put(elementDeclaration, eventType);
    return eventType;
  }

  public final EventSEWildcardNS getEventSEWildcardNS(String uri) {
    assert uri != null;
    EventSEWildcardNS eventType;
    if ((eventType = m_elementWildcardNSCache.get(uri)) != null) {
      return eventType;
    }
    eventType = new EventSEWildcardNS(uri) {};
    m_elementWildcardNSCache.put(uri, eventType);
    return eventType;
  }

}
