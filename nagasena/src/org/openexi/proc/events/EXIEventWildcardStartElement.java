package org.openexi.proc.events;

import org.openexi.proc.common.BinaryDataSource;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventDescription;
import org.openexi.schema.Characters;

public final class EXIEventWildcardStartElement implements EventDescription {

  private final String m_uri;
  private final String m_name;
  private final int m_uriId;
  private final int m_nameId;
  private final String m_prefix;
  
  private final EventType m_eventType;
  
  public EXIEventWildcardStartElement(String uri, String name, int uriId, int nameId, String prefix, EventType eventType) {
    assert eventType.itemType == EventType.ITEM_SCHEMA_WC_ANY || eventType.itemType == EventType.ITEM_SCHEMA_WC_NS || eventType.itemType == EventType.ITEM_SE_WC; 
    m_uri = uri; 
    m_name = name;
    m_uriId = uriId;
    m_nameId = nameId;
    m_prefix = prefix;
    m_eventType = eventType;
  }
  
  public byte getEventKind() {
    return EventDescription.EVENT_SE;
  }

  public String getName() {
    return m_name;
  }

  public String getURI() {
    return m_uri;
  }

  public int getNameId() {
    return m_nameId;
  }

  public int getURIId() {
    return m_uriId;
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
  
  public EventType getEventType() {
    return m_eventType;
  }
  
}
