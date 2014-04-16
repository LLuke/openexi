package org.openexi.fujitsu.proc.common;

public interface EventTypeList {
  
  public int getLength();
  
  public EventType item(int i);
  
  public EventType getSD();
  public EventType getED();
  
  public EventType getEE();
  
  public EventType getSchemaAttribute(String uri, String name);
  public EventType getSchemaAttributeInvalid(String uri, String name);
  public EventType getAttribute(String uri, String name);
  public EventType getSchemaAttributeWildcardAny();
  public EventType getAttributeWildcardAnyUntyped();
  public EventType getSchemaAttributeWildcardNS(String uri); 
  
  public EventType getSchemaCharacters();
  /**
   * Returns a characters event type that corresponds to either a mixed content or
   * schema-deviation characters if any. The one corresponding to a mixed content is
   * returned when both are available.
   */
  public EventType getCharacters();
  
  public EventType getNamespaceDeclaration();
  
  public boolean isMutable();
  
}
