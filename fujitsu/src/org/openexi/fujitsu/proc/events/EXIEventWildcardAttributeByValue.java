package org.openexi.fujitsu.proc.events;

import org.openexi.fujitsu.proc.common.CharacterSequence;
import org.openexi.fujitsu.proc.common.EventType;

public final class EXIEventWildcardAttributeByValue extends EXIEventWildcardAttribute {

  private final CharacterSequence m_text;
  
  public EXIEventWildcardAttributeByValue(String uri, String name, String prefix, CharacterSequence text, EventType eventType) {
    super(uri, name, prefix, eventType);
    m_text = text;
  }
  
  @Override
  public final CharacterSequence getCharacters() {
    return m_text;
  }
  
}
