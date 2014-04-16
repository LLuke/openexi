package org.openexi.fujitsu.proc.events;

import org.openexi.fujitsu.proc.common.CharacterSequence;
import org.openexi.fujitsu.proc.common.EventCode;
import org.openexi.fujitsu.proc.common.EventType;
import org.openexi.fujitsu.proc.common.EXIEvent;

public final class EXIEventProcessingInstruction implements EXIEvent {

  private final String m_name;
  private final CharacterSequence m_text;
  private final EventType m_eventType;

  public EXIEventProcessingInstruction(String name, CharacterSequence text, EventType eventType) {
    assert eventType.itemType == EventCode.ITEM_PI;
    m_name = name;
    m_text = text;
    m_eventType = eventType;
  }

  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EXIEvent interface
  ///////////////////////////////////////////////////////////////////////////

  public byte getEventVariety() {
    return EXIEvent.EVENT_PI;
  }
  
  public String getURI() {
    return null;
  }
  
  public String getName() {
    return m_name;
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
