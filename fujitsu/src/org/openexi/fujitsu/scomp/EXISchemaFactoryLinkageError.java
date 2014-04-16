package org.openexi.fujitsu.scomp;

import java.io.PrintWriter;
import java.io.StringWriter;

final class EXISchemaFactoryLinkageError extends LinkageError {
    
  private static final long serialVersionUID = 663237775333756655L;
  
  private final Exception m_exception;

  EXISchemaFactoryLinkageError(String msg, Exception exc) {
    super(msg);
    m_exception = exc;
  }

  public Exception getException() {
    return m_exception;
  }

  public String getMessage() {
    String msg = super.getMessage() + '\n';
    if (m_exception != null) {
      StringWriter swriter = new StringWriter();
      PrintWriter pwriter  = new PrintWriter(swriter);
      m_exception.printStackTrace(pwriter);
      pwriter.close();
      swriter.flush();
      msg += swriter.toString();
    }
    return msg;
  }

}