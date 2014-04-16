package org.openexi.proc.events;

import org.openexi.proc.common.CharacterSequence;
import org.openexi.proc.common.EventCode;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EXIEvent;
import org.openexi.proc.common.XmlUriConst;

public final class EXIEventSchemaNil implements EXIEvent {

  private final EventType m_eventType;
  private final boolean m_nilled;
  private final String m_prefix;
  private final CharacterSequence m_lexicalValue;

  public EXIEventSchemaNil(boolean nilled, CharacterSequence lexicalValue, String prefix, EventType eventType) {
    assert eventType.itemType == EventCode.ITEM_SCHEMA_NIL;
    m_nilled = nilled;
    m_lexicalValue = lexicalValue;
    m_eventType = eventType;
    m_prefix = prefix;
  }

  public boolean isNilled() {
    return m_nilled;
  }

  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EXIEvent interface
  ///////////////////////////////////////////////////////////////////////////

  public byte getEventVariety() {
    return EXIEvent.EVENT_NL;
  }
  
  public String getURI() {
    return XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI;
  }
  
  public String getName() {
    return "nil";
  }

  public String getPrefix() {
    return m_prefix;
  }

  /**
   * Returns the lexical value when lexical preservation is on, 
   * otherwise returns null. 
   */
  public CharacterSequence getCharacters() {
    return m_lexicalValue;
  }
  
  public EventType getEventType() {
    return m_eventType;
  }

}
