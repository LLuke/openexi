package org.openexi.scomp;

import org.xml.sax.Locator;
import org.xml.sax.helpers.LocatorImpl;

import org.openexi.util.MessageResolver;


/**
 * Any errors encountered during schema compilation are communicated to
 * an application as EXISchemaFactoryException objects.
 * @see EXISchemaFactoryErrorHandler
 * @author Dennis Dawson
 */
public class EXISchemaFactoryException extends Exception {

  private static final long serialVersionUID = 3816521974819647026L;
  
  /** The underlying XMLSchema parser found an error in the schema. */
  public static final int XMLSCHEMA_ERROR                 = 1002;

  private static final MessageResolver m_msgs =
      new MessageResolver(EXISchemaFactoryException.class);

  private final int m_code;
  private final String m_message;

  private Exception m_exception = null;
  private Locator m_locator = null;

/**
 * Constructor. 
 * @param code int value of the exception code
 * @param texts String value of the error message(s)
 * @param refLocator location where the error occurred, if available
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
  /**
   * Returns an Exception object.
   * @return an Exception instance for the current exception.
   */
  public Exception getException() {
    return m_exception;
  }

  /**
   * Sets a value for the current exception.
   */
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