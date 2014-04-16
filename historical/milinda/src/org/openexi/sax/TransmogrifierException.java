package org.openexi.sax;

import org.xml.sax.Locator;
import org.xml.sax.helpers.LocatorImpl;

import org.openexi.util.MessageResolver;

public final class TransmogrifierException extends Exception {

  private static final long serialVersionUID = -4536662596727577640L;

  public static final int UNEXPECTED_ELEM    = 1;
  public static final int UNEXPECTED_ATTR    = 2;
  public static final int UNEXPECTED_CHARS   = 3;
  public static final int UNHANDLED_SAXPARSER_FEATURE = 5;
  public static final int UNHANDLED_SAXPARSER_PROPERTY = 6;
  public static final int SAX_ERROR = 7;
  public static final int UNEXPECTED_END_ELEM = 8;
  public static final int UNEXPECTED_ED = 9;
  public static final int UNEXPECTED_SD = 10;
  public static final int XMLREADER_ACCESS_ERROR = 11;
  
  private static final MessageResolver m_msgs =
    new MessageResolver(TransmogrifierException.class);

  private final int m_code;
  private final String m_message;
  
  private Exception m_exception = null;
  private Locator m_locator = null;
  
  TransmogrifierException(int code, String[] texts, LocatorOnSAXParseException locator) {
    m_code    = code;
    m_message = m_msgs.getMessage(code, texts);
    m_locator = locator;
  }

  TransmogrifierException(int code, String[] texts, LocatorImpl locator) {
    m_code    = code;
    m_message = m_msgs.getMessage(code, texts);
    m_locator = locator; 
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
  
  public Exception getException() {
    return m_exception;
  }
  
  void setException(Exception exc) {
    m_exception = exc;
  }
  
  /**
   * Returns the locator that is associated with this compilation error.
   * @return a Locator if available, otherwise null
   */
  public Locator getLocator() {
    return m_locator;
  }

}
