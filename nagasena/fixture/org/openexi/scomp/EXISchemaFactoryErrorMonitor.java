package org.openexi.scomp;

import java.util.ArrayList;

/**
 */
public class EXISchemaFactoryErrorMonitor implements EXISchemaFactoryErrorHandler {

  private final ArrayList<EXISchemaFactoryException> m_warnings; 
  private final ArrayList<EXISchemaFactoryException> m_errors;
  private final ArrayList<EXISchemaFactoryException> m_fatalErrors; 

  private final boolean m_dothrow;

  public EXISchemaFactoryErrorMonitor() {
    this(false);
  }

  EXISchemaFactoryErrorMonitor(boolean dothrow) {
    m_dothrow = dothrow;
    m_warnings = new ArrayList<EXISchemaFactoryException>();
    m_errors = new ArrayList<EXISchemaFactoryException>();
    m_fatalErrors = new ArrayList<EXISchemaFactoryException>();
  }

  public void clear() {
    m_warnings.clear();
    m_errors.clear();
    m_fatalErrors.clear();
  }
  
  public void warning(EXISchemaFactoryException exc)
    throws EXISchemaFactoryException {
    m_warnings.add(exc);
  }

  public void error(EXISchemaFactoryException exc)
      throws EXISchemaFactoryException {
    m_errors.add(exc);
    if (m_dothrow)
      throw exc;
  }

  public void fatalError(EXISchemaFactoryException exc)
      throws EXISchemaFactoryException {
    m_fatalErrors.add(exc);
    if (m_dothrow)
      throw exc;
  }

  public int getTotalCount() {
    return m_warnings.size() + m_errors.size() + m_fatalErrors.size();
  }
  
  public int getTotalErrorCount() {
    return m_errors.size() + m_fatalErrors.size();
  }

  public int getWarningCount() {
    return m_warnings.size();
  }
  
  public int getFatalErrorCount() {
    return m_fatalErrors.size();
  }

  public boolean containsError(int code) {
    int i, len;
    for (i = 0, len = m_errors.size(); i < len; i++) {
      EXISchemaFactoryException sce = (EXISchemaFactoryException)m_errors.get(i);
      if (sce.getCode() == code)
        return true;
    }
    return false;
  }

  public EXISchemaFactoryException[] getWarnings(int code) {
    return getItems(code, m_warnings);
  }
  
  public EXISchemaFactoryException[] getErrors(int code) {
    return getItems(code, m_errors);
  }

  public EXISchemaFactoryException[] getFatalErrors(int code) {
    return getItems(code, m_fatalErrors);
  }

  private static EXISchemaFactoryException[] getItems(int code, ArrayList<EXISchemaFactoryException> array) {
    ArrayList<EXISchemaFactoryException> items;
    items = new ArrayList<EXISchemaFactoryException>();
    int i, len;
    for (i = 0, len = array.size(); i < len; i++) {
      EXISchemaFactoryException sce = (EXISchemaFactoryException)array.get(i);
      if (sce.getCode() == code)
        items.add(sce);
    }
    EXISchemaFactoryException[] itemList = new EXISchemaFactoryException[items.size()];
    for (i = 0, len = items.size(); i < len; i++) {
      itemList[i] = (EXISchemaFactoryException)items.get(i);
    }
    return itemList;
  }

  public int getErrorCount(int code) {
    int i, len, n;
    for (i = 0, n = 0, len = m_errors.size(); i < len; i++) {
      EXISchemaFactoryException sce = (EXISchemaFactoryException)m_errors.get(i);
      if (sce.getCode() == code)
        ++n;
    }
    return n;
  }

  public EXISchemaFactoryException[] getErrors() {
    int i, len;
    EXISchemaFactoryException[] errorList = new EXISchemaFactoryException[m_errors.size()];
    for (i = 0, len = m_errors.size(); i < len; i++) {
      errorList[i] = (EXISchemaFactoryException)m_errors.get(i);
    }
    return errorList;
  }

  public EXISchemaFactoryException[] getFatalErrors() {
    int i, len;
    EXISchemaFactoryException[] fatalErrorList = new EXISchemaFactoryException[m_fatalErrors.size()];
    for (i = 0, len = m_fatalErrors.size(); i < len; i++) {
      fatalErrorList[i] = (EXISchemaFactoryException)m_fatalErrors.get(i);
    }
    return fatalErrorList;
  }

}
