package com.sumerogi.proc.io;

import com.sumerogi.proc.common.EventDescription;
import com.sumerogi.proc.common.EventType;
import com.sumerogi.schema.Characters;

abstract class EXIEventValue implements EventDescription {
  
  private final EventType m_eventType;
  private final Characters m_text;
  
  public EXIEventValue(Characters text, EventType eventType) {
    m_eventType = eventType;
    m_text = text;
  }
  
  public int getNameId() {
    return m_eventType.getNameId();
  }

  public Characters getCharacters() {
    return m_text;
  }
  
  public EventType getEventType() {
    return m_eventType;
  }
  
  public String getName() {
    return m_eventType.getName();
  }

}
