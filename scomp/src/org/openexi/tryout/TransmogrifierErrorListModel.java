package org.openexi.tryout;

import javax.swing.DefaultListModel;

class TransmogrifierErrorListModel extends DefaultListModel<EXISchemaFactoryThread.AnnotException> {

  private static final long serialVersionUID = -2950143839052351909L;
  
  String m_systemId;

  public TransmogrifierErrorListModel() {
    m_systemId = null;
  }

  void setSystemId(String systemId) {
    m_systemId = systemId;
  }

  String getSystemId() {
    return m_systemId;
  }

}
