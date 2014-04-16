package org.openexi.schema;

import java.net.URL;

public final class HeaderOptionsSchema {
  
  private static final String COMPILED_SCHEMA_LOCATION = "HeaderOptions.xsc";
  
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
  
  public static EXISchema getEXISchema() {
    return m_schema;
  }
  
  private HeaderOptionsSchema() {
  }

}
