package org.openexi.scomp;

import org.xml.sax.Locator;
import org.xml.sax.helpers.LocatorImpl;

import org.openexi.util.MessageResolver;


/**
 * Any errors encountered during schema compilation are communiated to
 * an applicaion as EXISchemaFactoryException objects.
 * @see EXISchemaFactoryErrorHandler
 */
public class EXISchemaFactoryException extends Exception {

  private static final long serialVersionUID = 3816521974819647026L;
  
  /** There was a SAX parse error while parsing XML Schema documents. */
  public static final int SCHEMAPARSE_ERROR                 = 1002;
  /** There was an error in the schema found by XMLSchemaParser. */
  public static final int XMLSCHEMA_ERROR                = 1004; // fatal, thrown

  private static final MessageResolver m_msgs =
      new MessageResolver(EXISchemaFactoryException.class);

  private final int m_code;
  private final String m_message;

  private Exception m_exception = null;
  private Locator m_locator = null;

  /**
   * Constructor.
   */
  EXISchemaFactoryException(int code, String[] texts, Locator refLocator) {
    m_code    = code;
    m_message = m_msgs.getMessage(code, texts);
    m_locator = refLocator != null ? new LocatorImpl(refLocator) : null;
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