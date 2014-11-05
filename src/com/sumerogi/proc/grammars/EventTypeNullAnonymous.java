package com.sumerogi.proc.grammars;

import com.sumerogi.proc.common.EventDescription;
import com.sumerogi.proc.common.EventType;
import com.sumerogi.proc.common.EventTypeList;
import com.sumerogi.schema.Characters;

final class EventTypeNullAnonymous extends EventType {
  
  private static final Characters CHARACTERS;
  static {
    CHARACTERS = new Characters("null".toCharArray(), 0, 4, false);
  }
  
  public EventTypeNullAnonymous(byte depth, EventTypeList eventTypeList) {
    super(depth, eventTypeList, EventType.ITEM_NULL_ANONYMOUS, EventDescription.EVENT_NULL);
  }
  
  public Characters getCharacters() {
    return CHARACTERS;
  }

}
