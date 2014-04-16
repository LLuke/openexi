package org.openexi.schema;

import java.net.URL;

public final class EmptySchema extends CommonSchema {
  
  private static final String COMPILED_SCHEMA_LOCATION = "EmptySchema.xsc";
  
  private static final EXISchema m_schema;
  
  static {
    URL optionslSchemaURI =
      HeaderOptionsSchema.class.getResource(COMPILED_SCHEMA_LOCATION);
    EXISchema schema = null;
    try {
      schema = loadCompiledSchema(optionslSchemaURI);
    }
    finally {
      m_schema = schema;
    }
  }
  
  public static EXISchema getEXISchema() {
    return m_schema;
  }
  
  private EmptySchema() {
  }

}
