package org.openexi.fujitsu.proc.events;

import org.openexi.fujitsu.proc.common.CharacterSequence;
import org.openexi.fujitsu.proc.common.EventCode;
import org.openexi.fujitsu.proc.common.EventType;

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
