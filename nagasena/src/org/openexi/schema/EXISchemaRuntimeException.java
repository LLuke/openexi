package org.openexi.schema;

import org.openexi.util.MessageResolver;

/**
 * Thrown when any programming errors are detected while accessing
 * SchemaCorpus. This exception is meant to be dealt with during
 * development time as opposed to run-time.
 * @see EXISchema
 */
public class EXISchemaRuntimeException extends RuntimeException {

  private static final long serialVersionUID = 3545264490046630729L;
  
  /** The index is out of bounds. */
  public static final int INDEX_OUT_OF_BOUNDS = 1;

  private static final MessageResolver m_msgs =
      new MessageResolver(EXISchemaRuntimeException.class);

  private final int m_code;
  private final String m_message;

  /**
   * Constructor.
   */
  EXISchemaRuntimeException(int code, String[] texts) {
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
  @Override
  public String getMessage() {
    return m_message;
  }

}