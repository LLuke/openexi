package org.openexi.fujitsu.proc.events;

import org.openexi.fujitsu.proc.common.CharacterSequence;
import org.openexi.fujitsu.proc.common.EventType;
import org.openexi.fujitsu.proc.common.EXIEvent;

public class EXIEventElement implements EXIEvent {

  private final String m_prefix;
  
  private final EventType m_eventType;
  
  public EXIEventElement(String prefix, EventType eventType) {
    m_prefix = prefix;
    m_eventType = eventType;
  }
  
  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EXIEvent interface
  ///////////////////////////////////////////////////////////////////////////


  public final byte getEventVariety() {
    return EXIEvent.EVENT_SE;
  }

  public String getName() {
    return m_eventType.getName();
  }

  public String getURI() {
    return m_eventType.getURI();
  }
  
  public String getPrefix() {
    return m_prefix;
  }
  
  public final CharacterSequence getCharacters() {
    return null;
  }
  
  public final EventType getEventType() {
    return m_eventType;
  }

}
