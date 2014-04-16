package org.openexi.proc.io;

import org.openexi.util.MessageResolver;

/**
 * ScriberRuntimeException represents a runtime exception found while executing Scribers.
 */
public final class ScriberRuntimeException extends RuntimeException {
  
  private static final long serialVersionUID = 378281583516854150L;

  /**
   * Manifested size and the actual size of the binary data do not match.
   */
  public static final int BINARY_DATA_SIZE_MISMATCH = 1;
  /**
   * Manifested binary data size is too large for compress or preCompress alignment types.
   */
  public static final int BINARY_DATA_SIZE_TOO_LARGE = 2;
  /**
   * Prefix cannot be null.
   */
  public static final int PREFIX_IS_NULL = 3;

  private static final MessageResolver m_msgs =
      new MessageResolver(ScriberRuntimeException.class);

  private static final String[] NO_TEXTS = new String[] { }; 

  private final int m_code;
  private final String m_message;
  
  public ScriberRuntimeException(int code) {
    this(code, NO_TEXTS);
  }

  /**
   * Constructs a new ScriberException.  
   * @param code int value  that represents the type of the exception
   * @param texts one or more strings that describe the exception
   */
  public ScriberRuntimeException(int code, String[] texts) {
    m_code    = code;
    m_message = m_msgs.getMessage(code, texts);
  }

  /**
   * Returns the error code of the exception.
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
  
}
