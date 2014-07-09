package org.openexi.tryout;

import java.io.IOException;

import org.xml.sax.InputSource;
import org.xml.sax.Locator;

import org.openexi.util.MessageResolver;
import org.openexi.schema.EXISchema;
import org.openexi.scomp.EXISchemaFactory;
import org.openexi.scomp.EXISchemaFactoryErrorHandler;
import org.openexi.scomp.EXISchemaFactoryException;

import org.openexi.tryout.IValidationContext;

class EXISchemaFactoryThread extends Thread {

  private final IValidationContext  m_context;
  private final ISchemaPage m_schemaPage;

  private final IMessageList m_list;
  private final MessageResolver      m_messages;
  
  private final EXISchemaFactoryErrorListModel m_listModel;
  
  private final EXISchemaFactory m_exiSchemaFactory;
  private final EXISchemaFactoryErrorHandlerImpl m_exiSchemaFactoryErrorHandler;
  
  private final InputSource m_inputSource;
  
  public EXISchemaFactoryThread(ISchemaPage schemaPage, IValidationContext context) {
    m_schemaPage = schemaPage;
    m_context   = context;
    
    m_exiSchemaFactory = new EXISchemaFactory();
    m_exiSchemaFactoryErrorHandler = new EXISchemaFactoryErrorHandlerImpl();
    m_exiSchemaFactory.setCompilerErrorHandler(m_exiSchemaFactoryErrorHandler);
    
    String systemId = m_context.getSystemId();
    m_inputSource = new InputSource(systemId);
    
    m_messages = m_schemaPage.getMainFrame().getMessageResolver(); 
    
    m_list = schemaPage.getMessageList();
    m_listModel = new EXISchemaFactoryErrorListModel();
    m_listModel.setSystemId(systemId);
  }

  public void run() {
    boolean failed = false;
    m_schemaPage.setStatusText("");
    m_listModel.clear();
    EXISchema schema = null;
    try {
      schema = m_exiSchemaFactory.compile(m_inputSource);
    }
    catch (IOException ioe) {
      failed = true;
      m_schemaPage.setStatusText(ioe.getMessage());
      ioe.printStackTrace();
      return;
    }
    catch (EXISchemaFactoryException esfe) {
      // supposedly have already been reported to the error handler.
      return;
    }
    finally {
      m_list.setModel(m_listModel);
      // count and report the number of errors found during validation
      int n = m_listModel.getErrorCount();
      if (n > 0) {
        m_schemaPage.setStatusText(
            m_messages.getMessage(SchemaCompilerXMsg.MF_STATUS_NUM_OF_ERRORS,
                              new String[] {String.valueOf(n)}));
      }
      else if (!failed) { // validation succeeded
        m_schemaPage.setStatusText(m_messages.getMessage(SchemaCompilerXMsg.MF_STATUS_IS_VALID_SCHEMA));
      }
      // extremely important to enable buttons back.
      m_context.setDone(schema, m_listModel.getFatalErrorCount() != 0, m_exiSchemaFactoryErrorHandler.getErrorCount());
    }
  }
  
  /////////////////////////////////////////////////////////////////////////
  // Supporting classes
  /////////////////////////////////////////////////////////////////////////

  private class EXISchemaFactoryErrorHandlerImpl implements EXISchemaFactoryErrorHandler {
    
    private int m_n_errors;
    
    EXISchemaFactoryErrorHandlerImpl() {
      m_n_errors = 0;
    }
    
    public int getErrorCount() {
      return m_n_errors;
    }

    public void warning(EXISchemaFactoryException exc)
        throws EXISchemaFactoryException {
      EXISchemaFactoryThread.this.m_listModel.addElement(
          new AnnotException(AnnotException.EXC_CLASS_WARNING, exc));
      Thread.yield();
    }
      
    public void error(EXISchemaFactoryException exc)
        throws EXISchemaFactoryException {
      ++m_n_errors;
      EXISchemaFactoryThread.this.m_listModel.addElement(
          new AnnotException(AnnotException.EXC_CLASS_ERROR, exc));
      Thread.yield();
    }

    public void fatalError(EXISchemaFactoryException exc)
        throws EXISchemaFactoryException {
      ++m_n_errors;
      EXISchemaFactoryThread.this.m_listModel.addElement(
          new AnnotException(AnnotException.EXC_CLASS_FATAL_ERROR, exc));
      Thread.yield();
    }
  }
  
  static class AnnotException {

    static final short EXC_CLASS_WARNING     = 2;
    static final short EXC_CLASS_ERROR       = 3;
    static final short EXC_CLASS_FATAL_ERROR = 4;

    private final int m_severity;
    private final EXISchemaFactoryException m_exception;

    private Locator m_locator; // computed, cached locator for reuse

    AnnotException(int severity, EXISchemaFactoryException xpe) {
      m_locator = xpe.getLocator();
      m_severity = severity;
      m_exception = xpe;
    }

    public int getSeverity() {
      return m_severity;
    }

    public EXISchemaFactoryException getEXISchemaFactoryException() {
      return m_exception;
    }

    public Locator getLocator() {
      return m_locator;
    }

    public String toString() {
      return m_exception.getMessage();
    }

    void setLocator(Locator locator) {
      m_locator = locator;
    }

  }
  
}
