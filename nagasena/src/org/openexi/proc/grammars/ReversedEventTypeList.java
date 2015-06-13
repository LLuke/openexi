package org.openexi.proc.grammars;

import java.util.ArrayList;
import java.util.List;

import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventTypeList;

final class ReversedEventTypeList extends EventTypeList {

  private final List<EventType> m_eventTypes; // items in reverse order.
  private int m_n_eventTypes;
  
  boolean hasDepthOneEE;
  private EventType m_eventTypeEndElement;

  boolean hasDepthOneCH;
  private EventType m_eventTypeCharacters;

  private EventType m_namespaceDeclaration;
  private EventType m_attributeWildcardAnyUntyped;

  private int m_initial_n_eventTypes;
  private int m_initial_n_attributes;
  private EventType m_initialEventTypeEndElement;
  private EventType m_initialEventTypeCharacters;

  private static final EventType[] ATTRIBUTES_NONE;
  static {
    ATTRIBUTES_NONE = new EventType[0];
  }

  private EventType[] m_attributes;
  private int m_n_attributes;

  ReversedEventTypeList() {
    super(true);
    m_eventTypes = new ArrayList<EventType>(16);
    m_n_eventTypes = 0;
    hasDepthOneEE = false;
    m_eventTypeEndElement = null;
    hasDepthOneCH = false;
    m_eventTypeCharacters = null;
    m_attributes = ATTRIBUTES_NONE;
    m_n_attributes = 0;
    m_namespaceDeclaration = null;
    m_attributeWildcardAnyUntyped = null;
  }
  
  void checkPoint() {
    m_initial_n_eventTypes = m_n_eventTypes;
    m_initial_n_attributes = m_n_attributes;
    m_initialEventTypeEndElement = m_eventTypeEndElement;
    m_initialEventTypeCharacters = m_eventTypeCharacters;
    
  }
  
  void reset() {
    hasDepthOneEE = hasDepthOneCH = false;
    m_n_eventTypes = m_initial_n_eventTypes;
    m_n_attributes = m_initial_n_attributes;
    m_eventTypeEndElement = m_initialEventTypeEndElement;
    m_eventTypeCharacters = m_initialEventTypeCharacters;
  }
  
  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EventTypeList APIs
  ///////////////////////////////////////////////////////////////////////////

  @Override
  public final int getLength() {
    return m_n_eventTypes;
  }

  @Override
  public EventType getSD() {
    return null;
  }

  @Override
  public EventType getEE() {
    return m_eventTypeEndElement;
  }

  @Override
  public final EventTypeSchema getSchemaAttribute(String uri, String name) {
    return null;
  }

  @Override
  public final EventTypeSchema getSchemaAttributeInvalid(String uri, String name) {
    return null;
  }
  
  @Override
  public final EventType getLearnedAttribute(String uri, String name) {
    for (int i = 0; i < m_n_attributes; i++) {
      final EventType eventType = m_attributes[i];
      if (name.equals(eventType.name) && uri.equals(eventType.uri))
        return eventType;
    }
    return null;
  }

  @Override
  public final EventType getSchemaAttributeWildcardAny() {
    return null;
  }

  @Override
  public final EventType getAttributeWildcardAnyUntyped() {
    return m_attributeWildcardAnyUntyped;
  }

  @Override
  public final EventType getSchemaAttributeWildcardNS(String uri) {
    return null;
  }

  @Override
  public final EventType getSchemaCharacters() {
    return null; 
  }

  @Override
  public final EventType getCharacters() {
    return m_eventTypeCharacters; 
  }
  
  @Override
  public EventType getNamespaceDeclaration() {
    return m_namespaceDeclaration;
  }

  @Override
  public EventType item(int i) {
    return m_eventTypes.get(m_n_eventTypes - (i + 1));
  }
  
  ///////////////////////////////////////////////////////////////////////////
  // Methods peculiar to ReversedEventTypeList
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Add an eventType as the head (i.e. at the end of ArrayList).
   */
  void add(EventType eventType) {
    switch (eventType.itemType) {
      case EventType.ITEM_EE:
        m_eventTypeEndElement = eventType;
        if (eventType.getDepth() == 1)
          hasDepthOneEE = true;
        break;
      case EventType.ITEM_CH:
        m_eventTypeCharacters = eventType;
        if (eventType.getDepth() == 1)
          hasDepthOneCH = true;
        break;
      case EventType.ITEM_AT:
        if (m_attributes.length == m_n_attributes) {
          int sz = m_n_attributes == 0 ? 4 : 2 * m_n_attributes;
          EventType[] attributes = new EventType[sz];
          if (m_n_attributes != 0)
            System.arraycopy(m_attributes, 0, attributes, 0, m_n_attributes);
          m_attributes = attributes;
        }
        m_attributes[m_n_attributes++] = eventType;
        break;
      case EventType.ITEM_NS:
        m_namespaceDeclaration = eventType;
        break;
      case EventType.ITEM_AT_WC_ANY_UNTYPED:
        m_attributeWildcardAnyUntyped = eventType;
        break;
    }
    if (m_n_eventTypes == m_eventTypes.size())
      m_eventTypes.add(eventType); // Need to let the array grow
    else
      m_eventTypes.set(m_n_eventTypes, eventType); // simply replace.
    eventType.setIndex(m_n_eventTypes++);
  }

}
