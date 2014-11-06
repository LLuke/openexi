package com.sumerogi.proc.grammars;

import com.sumerogi.proc.common.EventDescription;
import com.sumerogi.proc.common.EventType;
import com.sumerogi.proc.common.EventTypeList;
import com.sumerogi.schema.Characters;

class EventTypeNull extends EventType {

  EventTypeNull(int localNameId, String name, EventTypeList eventTypeList) {
    super(name, localNameId, EVENT_CODE_DEPTH_ONE, eventTypeList, EventType.ITEM_NULL_NAMED, EventDescription.EVENT_NULL);
  }

  @Override
  public Characters getCharacters() {
    return Characters.CHARACTERS_NULL;
  }

}
