package com.sumerogi.proc.events;

import com.sumerogi.proc.common.EventDescription;
import com.sumerogi.proc.common.EventType;
import com.sumerogi.schema.Characters;

public class EXIEventWildcardStartContainer implements EventDescription {

  private final EventType m_eventType;

  private final String m_name;
  private final int m_nameId;
  
  private final byte m_eventKind;

  /**
   * Constructor.  
   * @param name
   * @param nameId
   * @param eventType
   * @param eventKind EventDescription.EVENT_START_OBJECT or EventDescription.EVENT_START_ARRAY
   */
  public EXIEventWildcardStartContainer(String name, int nameId, EventType eventType, byte eventKind) {
    m_eventType = eventType;
    m_name = name;
    m_nameId = nameId;
    m_eventKind = eventKind;
  }
  
  public byte getEventKind() {
    return m_eventKind; 
  }

  public int getNameId() {
    return m_nameId;
  }

  public Characters getCharacters() {
    return null;
  }
  
  public EventType getEventType() {
    return m_eventType;
  }
  
  public String getName() {
    return m_name;
  }
  
}
