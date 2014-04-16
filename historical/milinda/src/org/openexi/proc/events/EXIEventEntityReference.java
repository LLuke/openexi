package org.openexi.proc.events;

import org.openexi.proc.common.CharacterSequence;
import org.openexi.proc.common.EventCode;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EXIEvent;

public final class EXIEventEntityReference implements EXIEvent {

  private final String m_name;
  private final EventType m_eventType;

  public EXIEventEntityReference(String name, EventType eventType) {
    assert eventType.itemType == EventCode.ITEM_ER;
    m_name = name;
    m_eventType = eventType;
  }

  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EXIEvent interface
  ///////////////////////////////////////////////////////////////////////////

  public byte getEventVariety() {
    return EXIEvent.EVENT_ER;
  }
  
  public String getURI() {
    return null;
  }
  
  public String getName() {
    return m_name;
  }

  public String getPrefix() {
    return null;
  }

  public CharacterSequence getCharacters() {
    return null;
  }

  public final EventType getEventType() {
    return m_eventType;
  }

}
