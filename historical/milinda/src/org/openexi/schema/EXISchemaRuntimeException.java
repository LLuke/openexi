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
  
  /** Requested type is not compatible with the type of the variant. */
  public static final int INCOMPATIBLE_VARIANT_TYPE   = 1;
  /** The element is not an XBRL element. */
  public static final int NOT_AN_XBRL_ELEMENT         = 2;
  /**
   * The complex type does not own a particle.
   * public static final int NO_PARTICLE_IN_COMPLEX_TYPE = 3;
   */
  /** The index is out of bounds. */
  public static final int INDEX_OUT_OF_BOUNDS = 4;
  /** The node is not an attribute. */
  public static final int NOT_ATTRIBUTE       = 5;
  /** The node is not an element. */
  public static final int NOT_ELEMENT         = 6;
  /** The element is not a simple content element. */
  public static final int ELEMENT_CONTENT_NOT_SIMPLE = 7;
  /** The node is not an attribute nor an attribute use. */
  public static final int NOT_ATTRIBUTE_NOR_ATTRIBUTE_USE = 8;
  /** The node is not a simple type node. */
  public static final int NOT_SIMPLE_TYPE = 9;
  /** The node is not a wildcard node. */
  public static final int NOT_WILDCARD = 11;
  /** The node is not an element node nor an attribute node. */
  public static final int NOT_INODE = 12;
  /** Name cannot be changed to null. */
  public static final int NAME_VALUE_NULL = 13;
  /** The simple type is not atomic. */
  public static final int SIMPLE_TYPE_NOT_ATOMIC = 14;
  /** The node is not a type. */
  public static final int NOT_TYPE = 15;
  /** The node is not a complex type node. */
  public static final int NOT_COMPLEX_TYPE = 16;
  /** The node is not a namespace node. */
  public static final int NOT_NAMESPACE = 17;
  /** The node is not a group node. */
  public static final int NOT_GROUP = 18;
  /** The node is not a particle node. */
  public static final int NOT_PARTICLE = 19;
  /** The node is not a union simple type node. */
  public static final int NOT_UNION_SIMPLE_TYPE = 20;

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
  public String getMessage() {
    return m_message;
  }

}