package org.openexi.proc.events;

import org.openexi.proc.common.BinaryDataSource;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventDescription;
import org.openexi.schema.Characters;

public final class EXIEventDTD implements EventDescription {
  
  private final String m_name;
  private final String m_publicId;
  private final String m_systemId;
  private final Characters m_text;
  private final EventType m_eventType;

  public EXIEventDTD(String name, String publicId, String systemId, Characters text, EventType eventType) {
    assert eventType.itemType == EventType.ITEM_DTD;
    m_name = name;
    m_publicId = publicId;
    m_systemId = systemId;
    m_text = text;
    m_eventType = eventType;
  }

  ///////////////////////////////////////////////////////////////////////////
  // Accessors
  ///////////////////////////////////////////////////////////////////////////
  
  public String getPublicId() {
    return m_publicId;
  }
  
  public String getSystemId() {
    return m_systemId;
  }

  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EXIEvent interface
  ///////////////////////////////////////////////////////////////////////////

  public byte getEventKind() {
    return EventDescription.EVENT_DTD;
  }
  
  public String getURI() {
    return null;
  }
  
  public String getName() {
    return m_name;
  }

  public int getURIId() {
    return -1;
  }

  public int getNameId() {
    return -1;
  }

  public String getPrefix() {
    return null;
  }

  public Characters getCharacters() {
    return m_text;
  }

  public BinaryDataSource getBinaryDataSource() {
    return null;
  }

  public final EventType getEventType() {
    return m_eventType;
  }

}
