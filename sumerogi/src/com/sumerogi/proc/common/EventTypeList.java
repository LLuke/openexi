package com.sumerogi.proc.common;

public abstract class EventTypeList {
  
  public final boolean isReverse; 
  
  protected EventTypeList(boolean isReverse) {
    this.isReverse = isReverse;
  }
  
  public abstract int getLength();
  
  public abstract EventType item(int i);
  
  public abstract EventType getSD();
  
  public final EventType getED() {
    final int len = getLength();
    for (int i = 0; i < len; i++) {
      final EventType eventType = item(i);
      if (eventType.itemType == EventType.ITEM_END_DOCUMENT) {
        return eventType;
      }
    }
    return null;
  }
  
  public abstract EventType getStartObjectAnonymous();
  public abstract EventType getStartObjectWildcard();
  public abstract EventType getEndObject();

  public abstract EventType getStartArrayAnonymous();
  public abstract EventType getStartArrayWildcard();
  public abstract EventType getEndArray();

  public final EventType getEventType(byte itemType, String name) {
    final int length = getLength();
    for (int i = 0; i < length; i++) {
      final EventType eventType = item(i);
      if (eventType.itemType == itemType) {
        if (name.equals(eventType.name)) {
          return eventType;
        }
      }
    }
    return null;
  }
  
  public final EventType getStartObjectNamed(String name) {
    final int length = getLength();
    for (int i = 0; i < length; i++) {
      final EventType eventType = item(i);
      if (eventType.itemType == EventType.ITEM_START_OBJECT_NAMED) {
        if (name.equals(eventType.name)) {
          return eventType;
        }
      }
    }
    return null;
  }

  public final EventType getStartArrayNamed(String name) {
    final int length = getLength();
    for (int i = 0; i < length; i++) {
      final EventType eventType = item(i);
      if (eventType.itemType == EventType.ITEM_START_ARRAY_NAMED) {
        if (name.equals(eventType.name)) {
          return eventType;
        }
      }
    }
    return null;
  }

  public abstract EventType getStringValueAnonymous();
  public abstract EventType getStringValueWildcard();
  
  public abstract EventType getNumberValueAnonymous();
  public abstract EventType getNumberValueWildcard();

  public abstract EventType getNullValueAnonymous();
  public abstract EventType getNullValueWildcard();

  public abstract EventType getBooleanValueAnonymous();
  public abstract EventType getBooleanValueWildcard();

}
