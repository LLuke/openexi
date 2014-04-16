package org.openexi.proc.events;

import org.openexi.proc.common.CharacterSequence;
import org.openexi.proc.common.EventCode;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EXIEvent;
import org.openexi.proc.common.XmlUriConst;

public final class EXIEventSchemaType implements EXIEvent {

  private final EventType m_eventType;
  private final int m_tp;
  private final String m_typeUri;
  private final String m_typeLocalName;
  private final String m_typePrefix;
  private final String m_prefix;
  private final CharacterSequence m_typeQualifiedName;

  public EXIEventSchemaType(int tp, String typeUri, String typeLocalName, String typePrefix, CharacterSequence typeQualifiedName, 
    String prefix, EventType eventType) {
    assert eventType.itemType == EventCode.ITEM_SCHEMA_TYPE || 
      eventType.itemType == EventCode.ITEM_AT_WC_ANY_UNTYPED ||
      eventType.itemType == EventCode.ITEM_AT;
    m_eventType = eventType;
    m_tp = tp;
    m_typeUri = typeUri;
    m_typeLocalName = typeLocalName;
    m_typePrefix = typePrefix;
    m_typeQualifiedName = typeQualifiedName;
    m_prefix = prefix;
  }

  public int getTp() {
    return m_tp;
  }
  
  public String getTypeURI() {
    return m_typeUri;
  }
  
  public String getTypeName() {
    return m_typeLocalName;
  }
  
  public String getTypePrefix() {
    return m_typePrefix;
  }

  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EXIEvent interface
  ///////////////////////////////////////////////////////////////////////////

  public byte getEventVariety() {
    return EXIEvent.EVENT_TP;
  }
  
  public String getURI() {
    return XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI;
  }
  
  public String getName() {
    return "type";
  }

  public String getPrefix() {
    return m_prefix;
  }

  /**
   * Returns the qualified name of the type when lexical preservation is on, 
   * otherwise returns null. 
   */
  public CharacterSequence getCharacters() {
    return m_typeQualifiedName;
  }

  public EventType getEventType() {
    return m_eventType;
  }

}
