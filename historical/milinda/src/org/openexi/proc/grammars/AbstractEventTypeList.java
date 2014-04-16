package org.openexi.proc.grammars;

import org.openexi.proc.common.EventCode;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventTypeList;

abstract class AbstractEventTypeList implements EventTypeList {
  
  public abstract int getLength();

  public abstract EventType item(int i);

  public abstract EventType getSD();
  
  public final EventType getED() {
    final int len = getLength();
    for (int i = 0; i < len; i++) {
      final EventType eventType = item(i);
      if (eventType.itemType == EventCode.ITEM_ED) {
        return eventType;
      }
    }
    return null;
  }
  
  public abstract EventType getEE();

  public abstract EventType getSchemaAttribute(String uri, String name);
  public abstract EventType getSchemaAttributeInvalid(String uri, String name);
  public abstract EventType getAttribute(String uri, String name);
  public abstract EventType getSchemaAttributeWildcardAny();
  public abstract EventType getAttributeWildcardAnyUntyped();
  public abstract EventType getSchemaAttributeWildcardNS(String uri); 

  public abstract EventType getSchemaCharacters();
  public abstract EventType getCharacters();

  public abstract EventType getNamespaceDeclaration();

  public abstract boolean isMutable();

  static final AbstractEventTypeList EMPTY;
  static {
    EMPTY = new AbstractEventTypeList() {
      @Override
      public final int getLength() {
        return 0;
      }
      @Override
      public final EventType item(int i) {
        assert false;
        return null;
      }
      @Override
      public final EventType getSD() {
        return null;
      }
      @Override
      public final EventType getEE() {
        return null;
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
        return null;
      }
      @Override
      public final EventType getSchemaAttributeWildcardAny() {
        return null;
      }
      @Override
      public final EventType getAttributeWildcardAnyUntyped() {
        return null;
      }
      @Override
      public final EventType getSchemaAttributeWildcardNS(String uri) {
        return null;
      }
      @Override
      public final EventType getSchemaCharacters() {
        return (EventType)null; 
      }
      @Override
      public final EventType getCharacters() {
        return (EventType)null; 
      }
      @Override
      public final EventType getNamespaceDeclaration() {
        return (EventType)null; 
      }
      @Override
      public final boolean isMutable() {
        return false;
      }
    };
  }
  
}
