namespace Nagasena.Schema {

  public class GrammarSchema {

    private const string COMPILED_SCHEMA_LOCATION = "Grammar.xsc";

    private static readonly EXISchema m_schema = null;

    static GrammarSchema() {
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

    private GrammarSchema() {
    }

  }

}