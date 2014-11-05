package com.sumerogi.proc.grammars;

import java.util.ArrayList;
import java.util.List;

import com.sumerogi.proc.common.EventType;
import com.sumerogi.proc.common.EventTypeList;

final class ReversedEventTypeList extends EventTypeList {

  private final List<EventType> m_eventTypes; // items in reverse order.
  private int m_n_eventTypes;

  private EventType m_initialEventStartObjectAnonymous;
  private EventType m_eventStartObjectAnonymous;
  private EventType m_eventStartObjectWildcard;
  private EventType m_eventTypeEndObject;

  private EventType m_initialEventStartArrayAnonymous;
  private EventType m_eventStartArrayAnonymous;
  private EventType m_eventStartArrayWildcard;
  private EventType m_eventTypeEndArray;

  private EventType m_initialEventStringValueAnonymous;
  private EventType m_eventStringValueAnonymous;
  private EventType m_eventStringValueWildcard;
  
  private EventType m_eventNumberValueWildcard;
  private EventType m_eventBooleanValueWildcard;

//  boolean hasDepthOneCH;
//  private EventType m_eventTypeCharacters;

//  private EventType m_namespaceDeclaration;
//  private EventType m_attributeWildcardAnyUntyped;

  private int m_initial_n_eventTypes;
//  private int m_initial_n_attributes;
//  private EventType m_initialEventTypeEndElement;
//  private EventType m_initialEventTypeCharacters;

//  private static final EventType[] ATTRIBUTES_NONE;
//  static {
//    ATTRIBUTES_NONE = new EventType[0];
//  }

//  private EventType[] m_attributes;
//  private int m_n_attributes;

  ReversedEventTypeList() {
    super(true);
    m_eventTypes = new ArrayList<EventType>(16);
    m_n_eventTypes = 0;
//    hasDepthOneEE = false;
    m_eventStartObjectAnonymous = null;
    m_eventStartObjectWildcard = null;
    m_eventTypeEndObject = null;
    m_eventStartArrayAnonymous = null;
    m_eventStartArrayWildcard = null;
    m_eventTypeEndArray = null;
    m_eventStringValueAnonymous = null;
    m_eventNumberValueWildcard = null;
    m_eventBooleanValueWildcard = null;
    
//    hasDepthOneCH = false;
//    m_eventTypeCharacters = null;
//    m_attributes = ATTRIBUTES_NONE;
//    m_n_attributes = 0;
//    m_namespaceDeclaration = null;
//    m_attributeWildcardAnyUntyped = null;
  }
  
  void checkPoint() {
    m_initial_n_eventTypes = m_n_eventTypes;
    m_initialEventStartObjectAnonymous = m_eventStartObjectAnonymous;
    m_initialEventStartArrayAnonymous = m_eventStartArrayAnonymous;
    m_initialEventStringValueAnonymous = m_eventStringValueAnonymous;

//    m_initial_n_attributes = m_n_attributes;
//    m_initialEventTypeEndElement = m_eventTypeEndElement;
//    m_initialEventTypeCharacters = m_eventTypeCharacters;
  }
  
  void reset() {
//    hasDepthOneEE = false;
//    hasDepthOneCH = false;
    m_n_eventTypes = m_initial_n_eventTypes;
    m_eventStartObjectAnonymous = m_initialEventStartObjectAnonymous;
    m_eventStartArrayAnonymous = m_initialEventStartArrayAnonymous;
    m_eventStringValueAnonymous = m_initialEventStringValueAnonymous;
//    m_n_attributes = m_initial_n_attributes;
//    m_eventTypeEndElement = m_initialEventTypeEndElement;
//    m_eventTypeCharacters = m_initialEventTypeCharacters;
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
  public EventType getStartObjectAnonymous() {
    return m_eventStartObjectAnonymous;
  }
  
  @Override
  public EventType getStartObjectWildcard() {
    return m_eventStartObjectWildcard;
  }

  @Override
  public EventType getEndObject() {
    return m_eventTypeEndObject;
  }

  @Override
  public EventType getStartArrayAnonymous() {
    return m_eventStartArrayAnonymous;
  }

  @Override
  public EventType getStartArrayWildcard() {
    return m_eventStartArrayWildcard;
  }

  @Override
  public EventType getEndArray() {
    return m_eventTypeEndArray;
  }

  @Override
  public final EventType getStringValueAnonymous() {
    return m_eventStringValueAnonymous;
  }

  @Override
  public final EventType getStringValueWildcard() {
    return m_eventStringValueWildcard;
  }

  @Override
  public final EventType getNumberValueAnonymous() {
    assert false;
    return null;
  }

  @Override
  public final EventType getNumberValueWildcard() {
    return m_eventNumberValueWildcard;
  }
  
  @Override
  public final EventType getNullValueAnonymous() {
    assert false;
    return null;
  }

  @Override
  public final EventType getBooleanValueAnonymous() {
    assert false;
    return null;
  }

  @Override
  public final EventType getBooleanValueWildcard() {
    return m_eventBooleanValueWildcard;
  }
  

//  @Override
//  public final EventTypeSchema getSchemaAttribute(String uri, String name) {
//    return null;
//  }
//
//  @Override
//  public final EventTypeSchema getSchemaAttributeInvalid(String uri, String name) {
//    return null;
//  }
  
//  @Override
//  public final EventType getLearnedAttribute(String uri, String name) {
//    for (int i = 0; i < m_n_attributes; i++) {
//      final EventType eventType = m_attributes[i];
//      if (name.equals(eventType.name) && uri.equals(eventType.uri))
//        return eventType;
//    }
//    return null;
//  }

//  @Override
//  public final EventType getAttributeWildcardAnyUntyped() {
//    return m_attributeWildcardAnyUntyped;
//  }

//  @Override
//  public final EventType getCharacters() {
//    return m_eventTypeCharacters; 
//  }
  
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
      case EventType.ITEM_END_OBJECT:
        m_eventTypeEndObject = eventType;
        assert eventType.getDepth() == 1;
        break;
      case EventType.ITEM_END_ARRAY:
        m_eventTypeEndArray = eventType;
        assert eventType.getDepth() == 1;
        break;
      case EventType.ITEM_START_OBJECT_ANONYMOUS:
        m_eventStartObjectAnonymous = eventType;
        break;
      case EventType.ITEM_START_ARRAY_ANONYMOUS:
        m_eventStartArrayAnonymous = eventType;    
        break;
      case EventType.ITEM_STRING_VALUE_WILDCARD:
        m_eventStringValueWildcard = eventType;
        break;
      case EventType.ITEM_START_OBJECT_WILDCARD:
        m_eventStartObjectWildcard = eventType;
        break;
      case EventType.ITEM_START_ARRAY_WILDCARD:
        m_eventStartArrayWildcard = eventType;
        break;
      case EventType.ITEM_STRING_VALUE_ANONYMOUS:
        m_eventStringValueAnonymous = eventType;
        break;
      case EventType.ITEM_NUMBER_VALUE_WILDCARD:
        m_eventNumberValueWildcard = eventType;
        break;
      case EventType.ITEM_BOOLEAN_VALUE_WILDCARD:
        m_eventBooleanValueWildcard = eventType;
        break;
//      case EventType.ITEM_CH:
//        m_eventTypeCharacters = eventType;
//        if (eventType.getDepth() == 1)
//          hasDepthOneCH = true;
//        break;
    }
    m_eventTypes.add(eventType);
    eventType.setIndex(m_n_eventTypes++);
  }

}
