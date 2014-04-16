package org.openexi.proc.events;

import org.openexi.proc.common.EventType;

public abstract class EXIEventWildcardAttribute extends EXIEventAttribute {

  private final String m_uri;
  private final String m_name;
  
  public EXIEventWildcardAttribute(String uri, String name, String prefix, EventType eventType) {
    super(prefix, eventType);
    m_uri = uri != null ? uri : ""; 
    m_name = name;
  }
  
  @Override
  public String getName() {
    return m_name;
  }

  @Override
  public String getURI() {
    return m_uri;
  }
  
}
