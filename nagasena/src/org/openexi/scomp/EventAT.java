package org.openexi.scomp;

import org.apache.xerces.xs.XSObject;
import org.apache.xerces.xs.XSAttributeDeclaration;

abstract class EventAT extends EventQName {

  private final XSAttributeDeclaration m_attributeDeclaration;
  
  protected EventAT(XSAttributeDeclaration attributeDeclaration) {
    m_attributeDeclaration = attributeDeclaration;
  }

  @Override
  public final byte getEventType() {
    return ATTRIBUTE;
  }

  @Override
  public final String getUri() {
    return roundify(m_attributeDeclaration.getNamespace());
  }

  @Override
  public final String getLocalName() {
    return m_attributeDeclaration.getName();
  }
 
  @Override
  public XSObject getDeclaration() {
    return m_attributeDeclaration;
  }
  
}
