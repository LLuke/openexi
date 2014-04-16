package org.openexi.proc.grammars;

import org.openexi.util.MessageResolver;

import org.openexi.schema.EXISchema;

public class GrammarRuntimeException extends RuntimeException {

  private static final long serialVersionUID = -2376223102900591097L;

  public static final int AMBIGUOUS_CONTEXT_OF_ELEMENT_PARTICLE            = 1;
  public static final int AMBIGUOUS_CONTEXT_OF_WILDCARD_PARTICLE           = 2;

  private static final MessageResolver m_msgs =
    new MessageResolver(GrammarRuntimeException.class);

  private final int m_code;
  private final String m_message;
  
  private int m_nd;
  private Object m_obj;
  
  /**
   * Constructor.
   */
  GrammarRuntimeException(int code, String[] texts) {
    m_code    = code;
    m_message = m_msgs.getMessage(code, texts);
    m_nd = EXISchema.NIL_NODE;
    m_obj = null;
  }

  /**
   * Returns a code that represents the type of the exception.
   * @return error code
   */
  public int getCode() {
    return m_code;
  }
  
  @Override
  public String getMessage() {
    return m_message;
  }

  void setNode(int nd) {
    m_nd = nd;
  }
  
  public int getNode() {
    return m_nd;
  }

  // REVISIT: make it non-public
  public void setObject(Object obj) {
    m_obj = obj;
  }
  
  public Object getObject() {
    return m_obj;
  }
  
}
