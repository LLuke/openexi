package org.openexi.proc.events;

import org.openexi.proc.common.CharacterSequence;
import org.openexi.proc.common.EventType;

public final class EXIEventSchemaCharactersByRef extends EXIEventSchemaCharacters {

  private EXITextProvider m_textProvider; 

  public EXIEventSchemaCharactersByRef(EXITextProvider textProvider, EventType eventType) {
    super(eventType);
    m_textProvider = textProvider;
  }

  @Override
  public CharacterSequence getCharacters() {
    return m_textProvider.getCharacters();
  }

}
