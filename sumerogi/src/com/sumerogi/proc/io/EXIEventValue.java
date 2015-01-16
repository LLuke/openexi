package com.sumerogi.proc.io;

import com.sumerogi.proc.common.EventDescription;
import com.sumerogi.proc.common.EventType;
import com.sumerogi.schema.Characters;

final class EXIEventValue implements EventDescription {
  
  private final EventType m_eventType;
  private final Characters m_text;
  
  private final byte m_eventKind;
  
  public EXIEventValue(Characters text, EventType eventType, byte eventKind) {
    m_eventType = eventType;
    m_text = text;
    m_eventKind = eventKind;
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
  
  public byte getEventKind() {
    return m_eventKind;
  }

}
