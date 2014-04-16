package org.openexi.proc.events;

import org.openexi.proc.common.CharacterSequence;
import org.openexi.proc.common.EventCode;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EXIEvent;

public abstract class EXIEventSchemaCharacters implements EXIEvent {

  private final EventType m_eventType;
  
  public EXIEventSchemaCharacters(EventType eventType) {
    assert eventType.itemType == EventCode.ITEM_SCHEMA_CH;
    m_eventType = eventType;
  }

  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EXIEvent interface
  ///////////////////////////////////////////////////////////////////////////

  public byte getEventVariety() {
    return EXIEvent.EVENT_CH;
  }
  
  public String getURI() {
    return null;
  }
  
  public String getName() {
    return "#text";
  }

  public String getPrefix() {
    return null;
  }

  public abstract CharacterSequence getCharacters();
  
  public EventType getEventType() {
    return m_eventType;
  }

}
