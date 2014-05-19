namespace Nagasena.Schema {

  /// <exclude/>
  public sealed class HeaderOptionsSchema {

    private const string COMPILED_SCHEMA_LOCATION = "HeaderOptions.xsc";

    private static readonly EXISchema m_schema = null;

    static HeaderOptionsSchema() {
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