package org.openexi.fujitsu.proc.events;

import org.openexi.fujitsu.proc.common.CharacterSequence;
import org.openexi.fujitsu.proc.common.EventType;

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
