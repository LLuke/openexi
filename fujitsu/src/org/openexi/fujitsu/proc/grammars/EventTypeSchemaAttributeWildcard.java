package org.openexi.fujitsu.proc.grammars;

import org.openexi.fujitsu.proc.common.EventTypeList;

abstract class EventTypeSchemaAttributeWildcard extends EventTypeSchema {

  EventTypeSchemaAttributeWildcard(int wc, String uri, int serial, byte depth, 
    Grammar ownerGrammar, EventTypeList eventTypeList, byte itemType) {
    super(wc, uri, (String)null, EventTypeSchema.NIL_INDEX, serial, depth, ownerGrammar, eventTypeList, itemType);
  }
  
  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EventType 
  ///////////////////////////////////////////////////////////////////////////
  
  @Override
  final boolean isContent() {
    return false;
  }

}
