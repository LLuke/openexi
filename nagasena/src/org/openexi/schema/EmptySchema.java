package org.openexi.schema;

import java.net.URL;

/**
 * EmptySchema provides an EXISchema that supports all datatypes inherent
 * in XML Schema such as xsd:int and xsd:dateTime, but with no 
 * user-specific definitions. This is to support the use of dynamic 
 * datatype associations discovered within elements during processing.
 */
public final class EmptySchema {
  
  private static final String COMPILED_SCHEMA_LOCATION = "EmptySchema.xsc";
  
  private static final EXISchema m_schema;
  
  static {
    URL optionslSchemaURI =
      HeaderOptionsSchema.class.getResource(COMPILED_SCHEMA_LOCATION);
    EXISchema schema = null;
    try {
      schema = CommonSchema.loadCompiledSchema(optionslSchemaURI);
    }
    finally {
      m_schema = schema;
    }
  }
  /**
   * Returns an EXISchema that supports all datatypes inherent in XML Schema.
   * Calls to this method always return the same object. 
   * @return
   */
  public static EXISchema getEXISchema() {
    return m_schema;
  }
  
  private EmptySchema() {
  }

}
