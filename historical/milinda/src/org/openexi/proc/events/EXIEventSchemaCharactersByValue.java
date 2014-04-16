package org.openexi.proc.events;

import org.openexi.proc.common.CharacterSequence;
import org.openexi.proc.common.EventType;

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
