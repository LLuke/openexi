package org.openexi.proc.events;

import org.openexi.proc.common.BinaryDataSource;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventDescription;
import org.openexi.schema.Characters;

public final class EXIEventNS implements EventDescription {

  private String m_uri; // "" represents disassociation 
  private String m_prefix; // "" represents the default (i.e. no prefix) 
  private boolean m_localElementNs;
  private EventType m_eventType;
  
  public EXIEventNS(String prefix, String uri, boolean localElementNs, EventType eventType) {
    assert eventType.itemType == EventType.ITEM_NS;

    m_prefix = prefix;
    m_uri = uri;
    m_localElementNs = localElementNs;
    m_eventType = eventType;
  }
  
  public byte getEventKind() {
    return EventDescription.EVENT_NS;
  }

  public String getURI() {
    return m_uri;
  }

  public String getName() {
    return null;
  }
  
  public int getNameId() {
    return -1;
  }

  public int getURIId() {
    return -1;
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

  public Characters getCharacters() {
    return null;
  }

  public BinaryDataSource getBinaryDataSource() {
    return null;
  }
  
}
