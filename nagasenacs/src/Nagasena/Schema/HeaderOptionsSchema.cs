namespace Nagasena.Schema {

  public sealed class HeaderOptionsSchema {

    private const string COMPILED_SCHEMA_LOCATION = "HeaderOptions.xsc";

    private static readonly EXISchema m_schema = null;

    static HeaderOptionsSchema() {
      //URL optionslSchemaURI = typeof(HeaderOptionsSchema).getResource(COMPILED_SCHEMA_LOCATION);
      EXISchema schema = null;
      try {
        schema = CommonSchema.loadCompiledSchema(COMPILED_SCHEMA_LOCATION);
      }
      finally {
        m_schema = schema;
      }
    }

    public static EXISchema EXISchema {
      get {
        return m_schema;
      }
    }

    private HeaderOptionsSchema() {
    }

  }

}