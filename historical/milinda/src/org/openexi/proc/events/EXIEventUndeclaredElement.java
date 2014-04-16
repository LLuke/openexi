package org.openexi.proc.events;

import org.openexi.proc.common.CharacterSequence;
import org.openexi.proc.common.EventCode;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EXIEvent;

public class EXIEventUndeclaredElement implements EXIEvent {

  private final String m_uri;
  private final String m_name;
  private final String m_prefix;
  
  private final EventType m_eventType;
  
  public EXIEventUndeclaredElement(String uri, String name, String prefix, EventType eventType) {
    assert eventType.itemType == EventCode.ITEM_SE_WC;
    m_uri = uri != null ? uri : ""; 
    m_name = name;
    m_prefix = prefix;
    m_eventType = eventType;
  }

  public byte getEventVariety() {
    return EXIEvent.EVENT_SE;
  }

  public String getName() {
    return m_name;
  }

  public String getURI() {
    return m_uri;
  }

  public String getPrefix() {
    return m_prefix;
  }

  public CharacterSequence getCharacters() {
    return null;
  }
  
  public EventType getEventType() {
    return m_eventType;
  }
  
}
