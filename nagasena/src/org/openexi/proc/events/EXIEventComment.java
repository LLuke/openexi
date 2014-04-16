package org.openexi.proc.events;

import org.openexi.proc.common.BinaryDataSource;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventDescription;
import org.openexi.schema.Characters;

public final class EXIEventComment implements EventDescription {

  private final Characters m_text;
  private final EventType m_eventType;

  public EXIEventComment(Characters text, EventType eventType) {
    assert eventType.itemType == EventType.ITEM_CM;
    m_text = text;
    m_eventType = eventType;
  }

  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EXIEvent interface
  ///////////////////////////////////////////////////////////////////////////

  public byte getEventKind() {
    return EventDescription.EVENT_CM;
  }
  
  public String getURI() {
    return null;
  }
  
  public String getName() {
    return null;
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
