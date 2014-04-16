package org.openexi.proc.common;

public abstract class EventType extends EventCode {
  
  public static final int NIL_INDEX = -1;
  
  public EventType(byte itemType) {
    super(itemType);
  }

  public abstract int getDepth();
  public abstract EventCode[] getItemPath();
  public abstract String getURI();
  public abstract String getName();
  /**
   * Returns true if it implements SchemaInformed interface. 
   */
  public abstract boolean isSchemaInformed();
  public abstract int getIndex();
  public abstract EventTypeList getEventTypeList();
  public abstract EXIEvent asEXIEvent();

}
