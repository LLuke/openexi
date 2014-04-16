package org.openexi.proc.events;

import org.openexi.proc.common.BinaryDataSource;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventDescription;
import org.openexi.schema.Characters;

public final class EXIEventEntityReference implements EventDescription {

  private final String m_name;
  private final EventType m_eventType;

  public EXIEventEntityReference(String name, EventType eventType) {
    assert eventType.itemType == EventType.ITEM_ER;
    m_name = name;
    m_eventType = eventType;
  }

  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EXIEvent interface
  ///////////////////////////////////////////////////////////////////////////

  public byte getEventKind() {
    return EventDescription.EVENT_ER;
  }
  
  public String getURI() {
    return null;
  }
  
  public String getName() {
    return m_name;
  }

  public int getNameId() {
    return -1;
  }

  public int getURIId() {
    return -1;
  }

  public String getPrefix() {
    return null;
  }

  public Characters getCharacters() {
    return null;
  }

  public BinaryDataSource getBinaryDataSource() {
    return null;
  }

  public final EventType getEventType() {
    return m_eventType;
  }

}
