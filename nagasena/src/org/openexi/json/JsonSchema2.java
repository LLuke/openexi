package org.openexi.json;

import java.net.URL;

import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.schema.CommonSchema;
import org.openexi.schema.EXISchema;

public final class JsonSchema2 {

  private static final String COMPILED_SCHEMA_LOCATION = "schema-for-json-V2.xsc";
  
  public static final String URI;

  private static final EXISchema m_schema;
  private static final GrammarCache m_grammarCache;
  
  static {
    URL schemaURI = Transmogrifier.class.getResource(COMPILED_SCHEMA_LOCATION);
    m_schema = CommonSchema.loadCompiledSchema(schemaURI);
    m_grammarCache = new GrammarCache(m_schema, GrammarOptions.STRICT_OPTIONS);
    
    final String[] uris = m_schema.uris;
    URI =  uris[uris.length - 1];
  }
  
  public static EXISchema getEXISchema() {
    return m_schema;
  }
  
  public static GrammarCache getGrammarCache() {
    return m_grammarCache;
  }
  
  private JsonSchema2() {
  }
  
}
