package com.sumerogi.proc.grammars;

import com.sumerogi.proc.common.EventType;
import com.sumerogi.proc.common.EventTypeList;

final class ArrayEventTypeList extends EventTypeList {
  
  private EventType[] m_eventTypes;

  private EventType m_stringValueAnonymous;
  private EventType m_numberValueAnonymous;
  private EventType m_booleanValueAnonymous;
  private EventType m_nullValueAnonymous;

  private EventType m_startArrayAnonymous;
  private EventType m_startObjectAnonymous;

  ArrayEventTypeList() {
    super(false);
    m_stringValueAnonymous = null;
    m_numberValueAnonymous = null;
    m_booleanValueAnonymous = null;
    m_nullValueAnonymous = null;
    m_startArrayAnonymous = null;
    m_startObjectAnonymous = null;
  }

  final void setItems(EventType[] eventTypes) {
    assert m_eventTypes == null;
    m_eventTypes = eventTypes;
    int i, len;
    for (i = 0, len = m_eventTypes.length; i < len; i++) {
      final EventType eventType = eventTypes[i]; 
      eventType.setIndex(i);
      final short itemType = eventType.itemType;
      switch (itemType) {
        case EventType.ITEM_STRING_VALUE_ANONYMOUS:
          assert m_stringValueAnonymous == null;
          m_stringValueAnonymous = eventType;
          break;
        case EventType.ITEM_NUMBER_VALUE_ANONYMOUS:
          assert m_numberValueAnonymous == null;
          m_numberValueAnonymous = eventType;
          break;
        case EventType.ITEM_BOOLEAN_VALUE_ANONYMOUS:
          assert m_booleanValueAnonymous == null;
          m_booleanValueAnonymous = eventType;
          break;
        case EventType.ITEM_NULL_ANONYMOUS:
          assert m_nullValueAnonymous == null;
          m_nullValueAnonymous = eventType;
          break;
        case EventType.ITEM_START_ARRAY_ANONYMOUS:
          assert m_startArrayAnonymous == null;
          m_startArrayAnonymous = eventType;
          break;
        case EventType.ITEM_START_OBJECT_ANONYMOUS:
          assert m_startObjectAnonymous == null;
          m_startObjectAnonymous = eventType;
          break;
        default:
          break;
      }
    }
  }

  @Override
  public int getLength() {
    return m_eventTypes.length;
  }

  @Override
  public EventType item(int i) {
    return m_eventTypes[i];
  }

  @Override
  public final EventType getSD() {
    final EventType eventType = m_eventTypes[0];
    return eventType.itemType == EventType.ITEM_START_DOCUMENT ? eventType : null;
  }

  @Override
  public EventType getStartObjectAnonymous() {
    return m_startObjectAnonymous;
  }

  @Override
  public EventType getStartObjectWildcard() {
    return null;
  }

  @Override
  public EventType getStartArrayAnonymous() {
    return m_startArrayAnonymous;
  }

  @Override
  public EventType getStartArrayWildcard() {
    return null;
  }

  @Override
  public final EventType getEndObject() {
    // REVISIT: replace with optimization
    int i, len;
    for (i = 0, len = m_eventTypes.length; i < len; i++) {
      final EventType eventType = m_eventTypes[i];
      if (eventType.itemType == EventType.ITEM_END_OBJECT)
        return eventType;
    }
    return null;
  }
  
  @Override
  public final EventType getEndArray() {
    // REVISIT: replace with optimization
    int i, len;
    for (i = 0, len = m_eventTypes.length; i < len; i++) {
      final EventType eventType = m_eventTypes[i];
      if (eventType.itemType == EventType.ITEM_END_ARRAY)
        return eventType;
    }
    return null;
  }
  
  @Override
  public final EventType getStringValueAnonymous() {
    return m_stringValueAnonymous;
  }

  @Override
  public final EventType getStringValueWildcard() {
    return null;
  }
  
  @Override
  public final EventType getNumberValueAnonymous() {
    return m_numberValueAnonymous;
  }

  @Override
  public final EventType getNumberValueWildcard() {
    return null;
  }
  
  @Override
  public final EventType getNullValueAnonymous() {
    return m_nullValueAnonymous;
  }

  @Override
  public final EventType getBooleanValueAnonymous() {
    return m_booleanValueAnonymous;
  }

  @Override
  public final EventType getBooleanValueWildcard() {
    return null;
  }

}
