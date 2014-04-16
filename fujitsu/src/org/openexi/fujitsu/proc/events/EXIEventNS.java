package org.openexi.fujitsu.proc.events;

import org.openexi.fujitsu.proc.common.CharacterSequence;
import org.openexi.fujitsu.proc.common.EventCode;
import org.openexi.fujitsu.proc.common.EventType;
import org.openexi.fujitsu.proc.common.EXIEvent;

public class EXIEventNS implements EXIEvent {

  private String m_uri; // "" represents disassociation 
  private String m_prefix; // "" represents the default (i.e. no prefix) 
  private boolean m_localElementNs;
  private EventType m_eventType;
  
  public EXIEventNS(String prefix, String uri, boolean localElementNs, EventType eventType) {
    assert eventType.itemType == EventCode.ITEM_NS;

    m_prefix = prefix;
    m_uri = uri;
    m_localElementNs = localElementNs;
    m_eventType = eventType;
  }
  
  public byte getEventVariety() {
    return EXIEvent.EVENT_NS;
  }

  public String getURI() {
    return m_uri;
  }

  public String getName() {
    return null;
  }
  
  public boolean getLocalElementNs() {
    return m_localElementNs;
  }

  public EventType getEventType() {
    return m_eventType;
  }

  public String getPrefix() {
    return m_prefix;
  }

  public CharacterSequence getCharacters() {
    return null;
  }

}
