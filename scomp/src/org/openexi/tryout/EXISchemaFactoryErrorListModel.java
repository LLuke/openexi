package org.openexi.tryout;

import javax.swing.DefaultListModel;

class EXISchemaFactoryErrorListModel extends DefaultListModel<EXISchemaFactoryThread.AnnotException> {

  private static final long serialVersionUID = 2682752244364240400L;
  String m_systemId;

  public EXISchemaFactoryErrorListModel() {
    m_systemId = null;
  }

//  public void addElement(Object o) {
//int a = 10;
//int b = a;
//    super.addElement(o);
//  }
  
  void setSystemId(String systemId) {
    m_systemId = systemId;
  }

  String getSystemId() {
    return m_systemId;
  }

  /**
   * Return the number of errors (excluding informatives).
   */
  int getErrorCount() {
    int n, i, size;
    for (n = 0, i = 0, size = getSize(); i < size; i++) {
      Object obj = getElementAt(i);
      if (obj != null && obj instanceof EXISchemaFactoryThread.AnnotException) {
        ++n;
      }
    }
    return n;
  }

  /**
   * Return the number of fatal errors.
   */
  int getFatalErrorCount() {
    int n, i, size;
    for (n = 0, i = 0, size = getSize(); i < size; i++) {
      Object obj = getElementAt(i);
      if (obj != null && obj instanceof EXISchemaFactoryThread.AnnotException) {
        if (((EXISchemaFactoryThread.AnnotException)obj).getSeverity() == EXISchemaFactoryThread.AnnotException.EXC_CLASS_FATAL_ERROR)
          ++n;
      }
    }
    return n;
  }

}
