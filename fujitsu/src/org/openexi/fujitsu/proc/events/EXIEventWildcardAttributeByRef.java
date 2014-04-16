package org.openexi.fujitsu.proc.events;

import org.openexi.fujitsu.proc.common.CharacterSequence;
import org.openexi.fujitsu.proc.common.EventType;

public final class EXIEventWildcardAttributeByRef extends EXIEventWildcardAttribute {

  private EXITextProvider m_textProvider; 

  public EXIEventWildcardAttributeByRef(String uri, String name, String prefix, EXITextProvider textProvider, EventType eventType) {
    super(uri, name, prefix, eventType);
    m_textProvider =  textProvider;
  }
  
  @Override
  public final CharacterSequence getCharacters() {
    return m_textProvider.getCharacters();
  }

}
