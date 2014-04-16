package org.openexi.fujitsu.proc.events;

import org.openexi.fujitsu.proc.common.CharacterSequence;
import org.openexi.fujitsu.proc.common.EventCode;
import org.openexi.fujitsu.proc.common.EventType;
import org.openexi.fujitsu.proc.common.EXIEvent;

public final class EXIEventComment implements EXIEvent {

  private final CharacterSequence m_text;
  private final EventType m_eventType;

  public EXIEventComment(CharacterSequence text, EventType eventType) {
    assert eventType.itemType == EventCode.ITEM_CM;
    m_text = text;
    m_eventType = eventType;
  }

  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EXIEvent interface
  ///////////////////////////////////////////////////////////////////////////

  public byte getEventVariety() {
    return EXIEvent.EVENT_CM;
  }
  
  public String getURI() {
    return null;
  }
  
  public String getName() {
    return null;
  }

  public String getPrefix() {
    return null;
  }

  public CharacterSequence getCharacters() {
    return m_text;
  }

  public final EventType getEventType() {
    return m_eventType;
  }

}
