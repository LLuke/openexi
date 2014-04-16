package org.openexi.fujitsu.proc.grammars;

import org.openexi.fujitsu.proc.common.EventTypeList;

final class EventTypeSchemaCharacters extends EventTypeSchema {

  EventTypeSchemaCharacters(int simpleType, String uri, String name, int index, int serial, 
    Grammar ownerGrammar, EventTypeList eventTypeList) {
    super(simpleType, uri, name, NIL_INDEX, serial, EVENT_CODE_DEPTH_ONE, ownerGrammar, eventTypeList, ITEM_SCHEMA_CH);
  }
  
  @Override
  boolean isContent() {
    return true;
  }

  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EventTypeSchema interface
  ///////////////////////////////////////////////////////////////////////////

  @Override
  EventTypeSchema duplicate(EventTypeList eventTypeList) {
    return new EventTypeSchemaCharacters(
       m_substance, m_uri, m_name, index, serial, m_ownerGrammar, eventTypeList);
  }
  
  @Override
  boolean isAugmented() {
    return false;
  }

}
