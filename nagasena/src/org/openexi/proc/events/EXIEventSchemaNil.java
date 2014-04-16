package org.openexi.proc.events;

import org.openexi.proc.common.BinaryDataSource;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventDescription;
import org.openexi.proc.common.XmlUriConst;
import org.openexi.schema.Characters;
import org.openexi.schema.EXISchemaConst;

public final class EXIEventSchemaNil implements EventDescription {

  private final EventType m_eventType;
  private final boolean m_nilled;
  private final String m_prefix;
  private final Characters m_lexicalValue;

  public EXIEventSchemaNil(boolean nilled, Characters lexicalValue, String prefix, EventType eventType) {
    assert eventType.itemType == EventType.ITEM_SCHEMA_NIL;
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

  public byte getEventKind() {
    return EventDescription.EVENT_NL;
  }
  
  public String getURI() {
    return XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI;
  }
  
  public String getName() {
    return "nil";
  }

  public int getNameId() {
    return EXISchemaConst.XSI_LOCALNAME_NIL_ID;
  }

  public int getURIId() {
    return XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI_ID;
  }

  public String getPrefix() {
    return m_prefix;
  }

  /**
   * Returns the lexical value when lexical preservation is on, 
   * otherwise returns null. 
   */
  public Characters getCharacters() {
    return m_lexicalValue;
  }
  
  public BinaryDataSource getBinaryDataSource() {
    return null;
  }

  public EventType getEventType() {
    return m_eventType;
  }

}
