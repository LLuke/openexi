package org.openexi.proc.grammars;

import java.util.ArrayList;

import org.openexi.proc.common.EventCode;
import org.openexi.proc.common.EventType;

final class ReversedEventTypeList extends AbstractEventTypeList {

  private final ArrayList<EventType> m_eventTypes; // items in reverse order.
  private int m_n_eventTypes;
  
  boolean hasDepthOneEE;
  private EventTypeEndElement m_eventTypeEndElement;

  boolean hasDepthOneCH;
  private EventTypeCharacters m_eventTypeCharacters;

  private EventTypeNamespaceDeclaration m_namespaceDeclaration;
  private EventTypeAttributeWildcardAnyUntyped m_attributeWildcardAnyUntyped;

  private static final EventTypeAttribute[] ATTRIBUTES_NONE;
  static {
    ATTRIBUTES_NONE = new EventTypeAttribute[0];
  }

  private EventTypeAttribute[] m_attributes;
  private int m_n_attributes;

  ReversedEventTypeList() {
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
  public final EventTypeSchemaAttribute getSchemaAttribute(String uri, String name) {
    return null;
  }

  @Override
  public final EventTypeSchemaAttributeInvalid getSchemaAttributeInvalid(String uri, String name) {
    return null;
  }
  
  @Override
  public final EventTypeAttribute getAttribute(String uri, String name) {
    for (int i = 0; i < m_n_attributes; i++) {
      final EventTypeAttribute eventType = m_attributes[i];
      if (name.equals(eventType.getName()) && uri.equals(eventType.getURI()))
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
  public boolean isMutable() {
    return true;
  }

  @Override
  public EventType item(int i) {
    return m_eventTypes.get(m_eventTypes.size() - (i + 1));
  }
  
  ///////////////////////////////////////////////////////////////////////////
  // Methods peculiar to ReversedEventTypeList
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Add an eventType as the head (i.e. at the end of ArrayList).
   */
  void add(AbstractEventType eventType) {
    switch (eventType.itemType) {
      case EventCode.ITEM_EE:
        m_eventTypeEndElement = (EventTypeEndElement)eventType;
        if (eventType.getDepth() == 1)
          hasDepthOneEE = true;
        break;
      case EventCode.ITEM_CH:
        m_eventTypeCharacters = (EventTypeCharacters)eventType;
        if (eventType.getDepth() == 1)
          hasDepthOneCH = true;
        break;
      case EventCode.ITEM_AT:
        if (m_attributes.length == m_n_attributes) {
          int sz = m_n_attributes == 0 ? 4 : 2 * m_n_attributes;
          EventTypeAttribute[] attributes = new EventTypeAttribute[sz];
          if (m_n_attributes != 0)
            System.arraycopy(m_attributes, 0, attributes, 0, m_n_attributes);
          m_attributes = attributes;
        }
        m_attributes[m_n_attributes++] = (EventTypeAttribute)eventType;
        break;
      case EventCode.ITEM_NS:
        m_namespaceDeclaration = (EventTypeNamespaceDeclaration)eventType;
        break;
      case EventCode.ITEM_AT_WC_ANY_UNTYPED:
        m_attributeWildcardAnyUntyped = (EventTypeAttributeWildcardAnyUntyped)eventType;
        break;
    }
    m_eventTypes.add(eventType);
    eventType.setIndex(m_n_eventTypes++);
  }

}
