package com.sumerogi.proc.io;

import com.sumerogi.proc.common.EventDescription;
import com.sumerogi.proc.common.EventType;
import com.sumerogi.schema.Characters;

final class EXIEventBooleanValue extends EXIEventValue {

  public EXIEventBooleanValue(Characters text, EventType eventType) {
    super(text, eventType);
  }
  
  public byte getEventKind() {
    return EventDescription.EVENT_BOOLEAN_VALUE;
  }

}
