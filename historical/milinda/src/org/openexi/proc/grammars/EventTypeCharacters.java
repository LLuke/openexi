package org.openexi.proc.grammars;

import org.openexi.proc.common.EventTypeList;

abstract class EventTypeCharacters extends EventTypeNonSchema {

  EventTypeCharacters(byte depth, Grammar ownerGrammar, EventTypeList eventTypeList) {
    super((String)null, (String)null, depth, ownerGrammar, eventTypeList, ITEM_CH);
  }

  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EventType interface
  ///////////////////////////////////////////////////////////////////////////

  @Override
  final boolean isContent() {
    return true;
  }

}
