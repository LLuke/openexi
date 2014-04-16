package org.openexi.fujitsu.proc.events;

import org.openexi.fujitsu.proc.common.CharacterSequence;
import org.openexi.fujitsu.proc.common.EventCode;
import org.openexi.fujitsu.proc.common.EventType;

public final class EXIEventAttributeByRef extends EXIEventAttribute {

  private EXITextProvider m_textProvider; 

  public EXIEventAttributeByRef(EXITextProvider textProvider, EventType eventType) {
    this(textProvider, (String)null, eventType);
    assert eventType.itemType == EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE;
  }
  
  public EXIEventAttributeByRef(EXITextProvider textProvider, String prefix, EventType eventType) {
    super(prefix, eventType);
    m_textProvider = textProvider;
  }

  public CharacterSequence getCharacters() {
    return m_textProvider.getCharacters();
  }

}
