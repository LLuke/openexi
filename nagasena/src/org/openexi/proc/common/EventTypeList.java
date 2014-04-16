package org.openexi.proc.common;

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
      if (eventType.itemType == EventType.ITEM_ED) {
        return eventType;
      }
    }
    return null;
  }
  
  public abstract EventType getEE();
  
  public abstract EventType getSchemaAttribute(String uri, String name);
  public abstract EventType getSchemaAttributeInvalid(String uri, String name);
  public abstract EventType getLearnedAttribute(String uri, String name);
  public abstract EventType getSchemaAttributeWildcardAny();
  public abstract EventType getAttributeWildcardAnyUntyped();
  public abstract EventType getSchemaAttributeWildcardNS(String uri); 
  
  public abstract EventType getSchemaCharacters();
  /**
   * Returns a characters event type that corresponds to either a mixed content or
   * schema-deviation characters if any. The one corresponding to a mixed content is
   * returned when both are available.
   */
  public abstract EventType getCharacters();
  
  public abstract EventType getNamespaceDeclaration();
  
  public static final EventTypeList EMPTY;
  static {
    EMPTY = new EventTypeList(false) {
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
      public final EventType getSchemaAttribute(String uri, String name) {
        return null;
      }
      @Override
      public final EventType getSchemaAttributeInvalid(String uri, String name) {
        return null;
      }
      @Override
      public final EventType getLearnedAttribute(String uri, String name) {
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
    };
  }
  
}
