package org.openexi.schema;

import org.openexi.util.MessageResolver;

/**
 * Thrown when datatype violations are detected during simple-type validation.
 * @see SimpleTypeValidator#validate(String, int)
 */
public class SchemaValidatorException extends Exception {

  private static final long serialVersionUID = -7464459822967621232L;
  
  /** length constraint was violated. */
  public static final int LENGTH_INVALID            = 1;
  /** minLength constraint was violated. */
  public static final int MIN_LENGTH_INVALID        = 2;
  /** maxLength constraint was violated. */
  public static final int MAX_LENGTH_INVALID        = 3;
  /** decimal syntax constraint was violated. */
  public static final int INVALID_DECIMAL           = 4;
  /** totalDigits constraint was violated. */
  public static final int TOTAL_DIGITS_INVALID      = 6;
  /** fractionDigits constraint was violated. */
  public static final int FRACTION_DIGITS_INVALID   = 7;
  /** minInclusive constraint was violated. */
  public static final int MIN_INCLUSIVE_INVALID     = 8;
  /** maxInclusive constraint was violated. */
  public static final int MAX_INCLUSIVE_INVALID     = 9;
  /** minExclusive constraint was violated. */
  public static final int MIN_EXCLUSIVE_INVALID     = 10;
  /** maxExclusive constraint was violated. */
  public static final int MAX_EXCLUSIVE_INVALID     = 11;
  /** Order of the values could not be determined due to equivocalness. */
  public static final int ORDER_EQUIVOCAL           = 12;
  /** float syntax constraint was violated. */
  public static final int INVALID_FLOAT             = 13;
  /** double syntax constraint was violated. */
  public static final int INVALID_DOUBLE            = 14;
  /** boolean syntax constraint was violated. */
  public static final int INVALID_BOOLEAN           = 15;
  /** Name syntax constraint was violated. */
  public static final int INVALID_NAME              = 17;
  /** NCName syntax constraint was violated. */
  public static final int INVALID_NCNAME            = 18;
  /** NMTOKEN syntax constraint was violated. */
  public static final int INVALID_NMTOKEN           = 19;
  /** QName syntax constraint was violated. */
  public static final int INVALID_QNAME             = 20;
  /** dateTime syntax constraint was violated. */
  public static final int INVALID_DATETIME          = 21;
  /** date syntax constraint was violated. */
  public static final int INVALID_DATE              = 22;
  /** time syntax constraint was violated. */
  public static final int INVALID_TIME              = 23;
  /** gYearMonth syntax constraint was violated. */
  public static final int INVALID_GYEARMONTH        = 24;
  /** gMonthDay syntax constraint was violated. */
  public static final int INVALID_GMONTHDAY         = 25;
  /** gYear syntax constraint was violated. */
  public static final int INVALID_GYEAR             = 26;
  /** gMonth syntax constraint was violated. */
  public static final int INVALID_GMONTH            = 27;
  /** gDay syntax constraint was violated. */
  public static final int INVALID_GDAY              = 28;
  /** duration syntax constraint was violated. */
  public static final int INVALID_DURATION          = 29;
  /** base64Binary syntax constraint was violated. */
  public static final int INVALID_BASE64_BINARY     = 30;
  /** hexBinary syntax constraint was violated. */
  public static final int INVALID_HEX_BINARY        = 31;
  /** The value was not valid as any of the union member types. */
  public static final int INVALID_UNION             = 32;
  /** pattern constraint was violated. */
  public static final int INVALID_PATTERN           = 33;
  /** enumeration constraint was violated. */
  public static final int INVALID_ENUMERATION       = 34;
  /** The pattern was not able to be processed. */
  public static final int PATTERN_NOT_PROCESSED     = 35;
  /** The pattern is not a valid regular expression. */
  public static final int INVALID_PATTERN_ITSELF    = 36;
  /** Language syntax constraint was violated. */
  public static final int INVALID_LANGUAGE = 37;
  /** Primary tag of the language is not valid. */
  public static final int INVALID_LANGUAGE_PRIMARY_TAG = 38;
  /** Subsequent tag of the language is not valid. */
  public static final int INVALID_LANGUAGE_SUBSEQUENT_TAG = 39;
  /** The complex type does not have a simple content. */
  public static final int COMPLEX_TYPE_NOT_OF_SIMPLE_CONTENT = 40;
  /** NOTATION cannot be used direcly. */
  public static final int DIRECT_USE_OF_NOTATION = 41;
  /** Invalid UTF-16 surrogate pair was found. */
  public static final int INVALID_SURROGATE_PAIR = 42;
  /** integer syntax constraint was violated. */
  public static final int INVALID_INTEGER = 43;

  /** The attribute's value is not valid per its fixed value constraint. */
  public static final int ATTRIBUTE_INVALID_PER_FIXED        = 116;
  /**
   * The element has to be empty if it has xsi:nil attribute with value "true".
   * NIL_ELEM_NOT_EMPTY has been unified into CHARS_IN_EMPTY_MODEL 
   * public static final int NIL_ELEM_NOT_EMPTY                 = 119;
   */
  /** The element's value is not valid per its fixed value constraint. */
  public static final int ELEMENT_INVALID_PER_FIXED          = 121;

  // Errors of code > 1000 are fatal.
  private static final int MIN_FATAL_CODE                    = 1001;
  /** Unbalanced end-of-element was encountered. */
  public static final int END_ELEMENT_NOT_BALANCED           = 1101;

  private static final MessageResolver m_msgs =
      new MessageResolver(SchemaValidatorException.class);

  private final int m_code;
  private final String m_message;

  private Exception m_exception = null;

  private EXISchema m_schema;
  private int   m_node;

  /**
   * Constructor.
   */
  SchemaValidatorException(int code, String[] texts,
                           EXISchema schema, int node) {
    m_code    = code;
    m_message = m_msgs.getMessage(code, texts);

    m_schema = schema;
    m_node   = node;
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
   * Determines if it is a fatal error.
   * @return true if it is indicated as fatal.
   */
  public boolean isFatal() {
    return m_code >= MIN_FATAL_CODE;
  }

  public Exception getException() {
    return m_exception;
  }

  void setException(Exception exc) {
    m_exception = exc;
  }

  /**
   * Returns the schema used for validation.
   * @return EXISchema
   */
  void setEXISchema(EXISchema schemaCorpus) {
    m_schema = schemaCorpus;
  }

  /**
   * Returns the schema corpus that contains the schema node.
   * @return schema corpus
   */
  public EXISchema getEXISchema() {
    return m_schema;
  }

  /**
   * Returns the schema node that is the direct cause of the error.
   * @return schema node
   */
  public int getSchemaNode() {
    return m_node;
  }

  void setSchemaNode(int node) {
    m_node = node;
  }

}