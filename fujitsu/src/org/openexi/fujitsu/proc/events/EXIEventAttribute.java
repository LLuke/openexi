package org.openexi.fujitsu.proc.events;

import org.openexi.fujitsu.proc.common.CharacterSequence;
import org.openexi.fujitsu.proc.common.EventCode;
import org.openexi.fujitsu.proc.common.EventType;
import org.openexi.fujitsu.proc.common.EXIEvent;

public abstract class EXIEventAttribute implements EXIEvent {

  private final String m_prefix;
  
  private final EventType m_eventType;
  
  public EXIEventAttribute(EventType eventType) {
    this(null, eventType);
    assert eventType.itemType == EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE;
  }
  
  public EXIEventAttribute(String prefix, EventType eventType) {
    m_prefix = prefix;
    m_eventType = eventType;
  }
  
  public final byte getEventVariety() {
    return EXIEvent.EVENT_AT;
  }

  public String getName() {
    return m_eventType.getName();
  }

  public String getURI() {
    return m_eventType.getURI();
  }
  
  public final String getPrefix() {
    return m_prefix;
  }
  
  public abstract CharacterSequence getCharacters();
  
  public final EventType getEventType() {
    return m_eventType;
  }

}
