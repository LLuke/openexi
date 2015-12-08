package org.openexi.sax;

import org.xml.sax.Locator;
import org.xml.sax.helpers.LocatorImpl;

import org.openexi.util.MessageResolver;

/**
 * Exception handler for the Transmogrifier.
 */
public final class TransmogrifierException extends Exception {

  private static final long serialVersionUID = -4536662596727577640L;
  /**
   * Unexpected Element.
   */
  public static final int UNEXPECTED_ELEM    = 1;
  /**
   * Unexpected Attribute.
   */
  public static final int UNEXPECTED_ATTR    = 2;
  /**
   * Unexpected Character Sequence.
   */
  public static final int UNEXPECTED_CHARS   = 3;
  /**
   * Unexpected Binary value.
   */
  public static final int UNEXPECTED_BINARY_VALUE   = 4;
  /**
   * Unhandled SAX parser feature.
   */
  public static final int UNHANDLED_SAXPARSER_FEATURE = 5;
  /**
   * SAX error reported by XML parser.
   */
  public static final int SAX_ERROR = 6;
  /**
   * Unexpected End of Element event.
   */
  public static final int UNEXPECTED_END_ELEM = 7;
  /**
   * Unexpected End of Document event.
   */
  public static final int UNEXPECTED_ED = 8;
  /**
   * Unexpected Start of Document event.
   */
  public static final int UNEXPECTED_SD = 9;
  /**
   * Prefix is not bound.
   */
  public static final int PREFIX_NOT_BOUND = 10;
  /**
   * Prefix is bound to another namespace.
   */
  public static final int PREFIX_BOUND_TO_ANOTHER_NAMESPACE = 11;
  /**
   * Errors reported by Scriber.
   */
  public static final int SCRIBER_ERROR = 12;
  /**
   * Whitespaces could not be preserved.
   * This occurs when xml:space was specified to be preserved in strict mode.
   */
  public static final int CANNOT_PRESERVE_WHITESPACES = 13;
  /**
   * Errors reported by EXIOptionsEncoder.
   */
  public static final int EXI_OPTIONS_ENCODER_EXCEPTION = 14;

  private static final MessageResolver m_msgs =
    new MessageResolver(TransmogrifierException.class);

  private final int m_code;
  private final String m_message;
  
  private Exception m_exception = null;
  private Locator m_locator = null;
  
  /**
   * Constructs a new TransmogrifierException.  
   * @param code int value  that represents the type of the exception
   * @param texts one or more strings that describe the exception
   */
  TransmogrifierException(int code, String[] texts) {
    m_code    = code;
    m_message = m_msgs.getMessage(code, texts);
    m_locator = null;
  }
  
  /**
   * Constructs a new TransmogrifierException.  
   * @param code int value  that represents the type of the exception
   * @param texts one or more strings that describe the exception
   * @param locator  Locator for where the error occurred
   */
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
  @Override
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
  
  /**
   * Returns the locator that is associated with this compilation error.
   * @return a Locator if available, otherwise null
   */
  public Locator getLocator() {
    return m_locator;
  }

}
