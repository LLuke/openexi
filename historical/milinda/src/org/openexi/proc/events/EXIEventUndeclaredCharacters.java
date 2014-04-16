package org.openexi.proc.events;

import org.openexi.proc.common.CharacterSequence;
import org.openexi.proc.common.EventCode;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EXIEvent;

public abstract class EXIEventUndeclaredCharacters implements EXIEvent {
  
  private final EventType m_eventType;

  public EXIEventUndeclaredCharacters(EventType eventType) {
    assert eventType.itemType == EventCode.ITEM_CH;
    m_eventType = eventType;
  }

  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EXIEvent interface
  ///////////////////////////////////////////////////////////////////////////

  public final byte getEventVariety() {
    return EXIEvent.EVENT_CH;
  }
  
  public final String getURI() {
    return null;
  }
  
  public final String getName() {
    return "#text";
  }

  public final String getPrefix() {
    return null;
  }

  public abstract CharacterSequence getCharacters();

  public final EventType getEventType() {
    return m_eventType;
  }

}
