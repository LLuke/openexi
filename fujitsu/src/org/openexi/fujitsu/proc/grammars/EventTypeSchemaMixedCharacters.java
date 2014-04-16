package org.openexi.fujitsu.proc.grammars;

import org.openexi.fujitsu.proc.common.EventTypeList;
import org.openexi.fujitsu.schema.EXISchema;

final class EventTypeSchemaMixedCharacters extends EventTypeSchema {

  EventTypeSchemaMixedCharacters(int serial, Grammar ownerGrammar, EventTypeList eventTypeList) {
    super(EXISchema.NIL_NODE, null, "#text", NIL_INDEX, serial, EVENT_CODE_DEPTH_ONE, ownerGrammar, eventTypeList, ITEM_SCHEMA_CH_MIXED);
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
    return new EventTypeSchemaMixedCharacters(serial, m_ownerGrammar, eventTypeList);
  }
  
  @Override
  boolean isAugmented() {
    return false;
  }

}
