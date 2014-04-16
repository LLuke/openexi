package org.openexi.schema;

import java.net.URL;

public class GrammarSchema {

  private static final String COMPILED_SCHEMA_LOCATION = "Grammar.xsc";
  
  private static final EXISchema m_schema;
  
  static {
    URL schemaURI = GrammarSchema.class.getResource(COMPILED_SCHEMA_LOCATION);
    EXISchema schema = null;
    try {
      schema = CommonSchema.loadCompiledSchema(schemaURI);
    }
    finally {
      m_schema = schema;
    }
  }
  
  public static EXISchema getEXISchema() {
    return m_schema;
  }
  
  private GrammarSchema() {
  }


}
