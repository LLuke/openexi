package org.openexi.proc.events;

import org.openexi.proc.common.BinaryDataSource;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventDescription;
import org.openexi.proc.common.XmlUriConst;
import org.openexi.schema.Characters;
import org.openexi.schema.EXISchemaConst;

public final class EXIEventSchemaType implements EventDescription {

  private final EventType m_eventType;
  private final int m_tp;
  private final String m_typeUri;
  private final String m_typeLocalName;
  private final String m_typePrefix;
  private final String m_prefix;
  private final Characters m_typeQualifiedName;

  public EXIEventSchemaType(int tp, String typeUri, String typeLocalName, String typePrefix, Characters typeQualifiedName, 
    String prefix, EventType eventType) {
    assert eventType.itemType == EventType.ITEM_SCHEMA_TYPE || 
      eventType.itemType == EventType.ITEM_AT_WC_ANY_UNTYPED ||
      eventType.itemType == EventType.ITEM_AT;
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

  public byte getEventKind() {
    return EventDescription.EVENT_TP;
  }
  
  public String getURI() {
    return XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI;
  }
  
  public String getName() {
    return "type";
  }

  public int getNameId() {
    return EXISchemaConst.XSI_LOCALNAME_TYPE_ID;
  }

  public int getURIId() {
    return XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI_ID;
  }

  public String getPrefix() {
    return m_prefix;
  }

  /**
   * Returns the qualified name of the type when lexical preservation is on, 
   * otherwise returns null. 
   */
  public Characters getCharacters() {
    return m_typeQualifiedName;
  }

  public BinaryDataSource getBinaryDataSource() {
    return null;
  }

  public EventType getEventType() {
    return m_eventType;
  }

}
