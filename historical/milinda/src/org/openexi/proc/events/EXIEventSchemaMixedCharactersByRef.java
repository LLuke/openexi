package org.openexi.proc.events;

import org.openexi.proc.common.CharacterSequence;
import org.openexi.proc.common.EventCode;
import org.openexi.proc.common.EventType;

public final class EXIEventSchemaMixedCharactersByRef extends EXIEventSchemaMixedCharacters {

  private EXITextProvider m_textProvider; 

  public EXIEventSchemaMixedCharactersByRef(EXITextProvider textProvider, EventType eventType) {
    super(eventType);
    assert eventType.itemType == EventCode.ITEM_SCHEMA_CH_MIXED;
    m_textProvider = textProvider;
  }

  @Override
  public CharacterSequence getCharacters() {
    return m_textProvider.getCharacters();
  }

}
