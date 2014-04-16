package org.openexi.proc.grammars;

import org.openexi.proc.common.EventTypeList;

public final class EventTypeSchemaAttribute extends EventTypeSchema {
  
  private final boolean m_useSpecificType;

  EventTypeSchemaAttribute(int attr, String uri, String name, int index, int serial, 
    Grammar ownerGrammar, EventTypeList eventTypeList) {
    this(attr, true, uri, name, index, serial, ownerGrammar, eventTypeList);
  }

  EventTypeSchemaAttribute(int attr, boolean useSpecificType, String uri, String name, int index, int serial, 
    Grammar ownerGrammar, EventTypeList eventTypeList) {
    super(attr, uri, name, index, serial, EVENT_CODE_DEPTH_ONE, ownerGrammar, eventTypeList, ITEM_SCHEMA_AT);
    m_useSpecificType = useSpecificType; 
  }

  ///////////////////////////////////////////////////////////////////////////
  // Accessors
  ///////////////////////////////////////////////////////////////////////////
  
  public boolean useSpecificType() {
    return m_useSpecificType;
  }

  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EventType interface
  ///////////////////////////////////////////////////////////////////////////

  @Override
  boolean isContent() {
    return false;
  }

  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EventTypeSchema interface
  ///////////////////////////////////////////////////////////////////////////

  @Override
  EventTypeSchema duplicate(EventTypeList eventTypeList) {
    return new EventTypeSchemaAttribute(
       m_substance, m_uri, m_name, index, serial, m_ownerGrammar, eventTypeList);
  }
  
  @Override
  boolean isAugmented() {
    return false;
  }

}
