package org.openexi.fujitsu.proc.events;

import org.openexi.fujitsu.proc.common.CharacterSequence;
import org.openexi.fujitsu.proc.common.EventType;

public final class EXIEventSchemaCharactersByValue extends EXIEventSchemaCharacters {

  private final CharacterSequence m_text;

  public EXIEventSchemaCharactersByValue(CharacterSequence text, EventType eventType) {
    super(eventType);
    m_text = text;
  }

  @Override
  public CharacterSequence getCharacters() {
    return m_text;
  }

}
