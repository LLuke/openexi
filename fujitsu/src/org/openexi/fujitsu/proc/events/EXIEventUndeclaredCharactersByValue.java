package org.openexi.fujitsu.proc.events;

import org.openexi.fujitsu.proc.common.CharacterSequence;
import org.openexi.fujitsu.proc.common.EventType;

public final class EXIEventUndeclaredCharactersByValue extends EXIEventUndeclaredCharacters {
  
  private CharacterSequence m_text; 

  public EXIEventUndeclaredCharactersByValue(CharacterSequence text, EventType eventType) {
    super(eventType);
    m_text = text;
  }

  @Override
  public CharacterSequence getCharacters() {
    return m_text;
  }
  
}
