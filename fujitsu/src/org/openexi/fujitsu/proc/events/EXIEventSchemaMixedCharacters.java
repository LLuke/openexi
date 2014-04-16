package org.openexi.fujitsu.proc.events;

import org.openexi.fujitsu.proc.common.CharacterSequence;
import org.openexi.fujitsu.proc.common.EventCode;
import org.openexi.fujitsu.proc.common.EventType;
import org.openexi.fujitsu.proc.common.EXIEvent;

public abstract class EXIEventSchemaMixedCharacters implements EXIEvent {

  private final EventType m_eventType;
  
  public EXIEventSchemaMixedCharacters(EventType eventType) {
    assert eventType.itemType == EventCode.ITEM_SCHEMA_CH_MIXED;
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

  public final EventType getEventType() {
    return m_eventType;
  }

}
