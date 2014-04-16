package org.openexi.proc.events;

import org.openexi.proc.common.CharacterSequence;
import org.openexi.proc.common.EventCode;
import org.openexi.proc.common.EventType;

public final class EXIEventSchemaMixedCharactersByValue extends EXIEventSchemaMixedCharacters {

  private final CharacterSequence m_text;

  public EXIEventSchemaMixedCharactersByValue(CharacterSequence text, EventType eventType) {
    super(eventType);
    assert eventType.itemType == EventCode.ITEM_SCHEMA_CH_MIXED;
    m_text = text;
  }

  @Override
  public CharacterSequence getCharacters() {
    return m_text;
  }

}
