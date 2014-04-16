package org.openexi.scomp;

import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSObject;

abstract class EventSE extends EventQName {

  private final XSElementDeclaration m_elementDeclaration;

  protected EventSE(XSElementDeclaration elementDeclaration) {
    m_elementDeclaration = elementDeclaration;
  }

  @Override
  public byte getEventType() {
    return ELEMENT;
  }
  
  @Override
  public final String getUri() {
    return roundify(m_elementDeclaration.getNamespace());
  }

  @Override
  public final String getLocalName() {
    return m_elementDeclaration.getName();
  }
  
  @Override
  public final XSObject getDeclaration() {
    return m_elementDeclaration;
  }

}
