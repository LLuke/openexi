package org.openexi.fujitsu.proc.events;

import org.openexi.fujitsu.proc.common.CharacterSequence;
import org.openexi.fujitsu.proc.common.EventCode;
import org.openexi.fujitsu.proc.common.EventType;
import org.openexi.fujitsu.proc.common.EXIEvent;

public class EXIEventWildcardStartElement implements EXIEvent {

  private final String m_uri;
  private final String m_name;
  private final String m_prefix;
  
  private final EventType m_eventType;
  
  public EXIEventWildcardStartElement(String uri, String name, String prefix, EventType eventType) {
    assert eventType.itemType == EventCode.ITEM_SCHEMA_WC_ANY || eventType.itemType == EventCode.ITEM_SCHEMA_WC_NS; 
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
