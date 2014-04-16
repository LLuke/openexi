package org.openexi.sax;

import org.openexi.util.MessageResolver;

public class TransmogrifierRuntimeException extends RuntimeException {

  private static final long serialVersionUID = -3795226789155748241L;

  /**
   * Unhandled SAX parser property.
   */
  public static final int UNHANDLED_SAXPARSER_PROPERTY = 1;

  /**
   * Failure to obtain an instance of XML parser.
   */
  public static final int XMLREADER_ACCESS_ERROR = 2;
  
  /**
   * SAXParserFactory for use with Transmogrifier must be aware of namespaces.
   */
  public static final int SAXPARSER_FACTORY_NOT_NAMESPACE_AWARE = 3;


  private static final MessageResolver m_msgs =
      new MessageResolver(TransmogrifierRuntimeException.class);

  private final int m_code;
  private final String m_message;

  private Exception m_exception = null;

  /**
   * Constructs a new TransmogrifierRuntimeException.  
   * @param code int value  that represents the type of the exception
   * @param texts one or more strings that describe the exception
   */
  TransmogrifierRuntimeException(int code, String[] texts) {
    m_code    = code;
    m_message = m_msgs.getMessage(code, texts);
  }

  /**
   * Returns a code that represents the type of the exception.
   * @return error code
   */
  public int getCode() {
    return m_code;
  }
  
  /**
   * Returns a message that describes the exception.
   * @return error message
   */
  public String getMessage() {
    return m_message;
  }
  
  /**
   * Returns an Exception object.
   * @return the error as an Exception instance
   */
  public Exception getException() {
    return m_exception;
  }

  void setException(Exception exc) {
    m_exception = exc;
  }
  
}
