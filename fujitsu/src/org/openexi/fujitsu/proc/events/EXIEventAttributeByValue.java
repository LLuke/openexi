package org.openexi.fujitsu.proc.events;

import org.openexi.fujitsu.proc.common.CharacterSequence;
import org.openexi.fujitsu.proc.common.EventCode;
import org.openexi.fujitsu.proc.common.EventType;

public final class EXIEventAttributeByValue extends EXIEventAttribute {

  private final CharacterSequence m_text;
  
  public EXIEventAttributeByValue(CharacterSequence text, EventType eventType) {
    this((String)null, text, eventType);
    assert eventType.itemType == EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE;
  }
  
  public EXIEventAttributeByValue(String prefix, CharacterSequence text, EventType eventType) {
    super(prefix, eventType);
    m_text = text;
  }

  public CharacterSequence getCharacters() {
    return m_text;
  }

}
