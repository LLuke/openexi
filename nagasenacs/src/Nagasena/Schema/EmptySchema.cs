namespace Nagasena.Schema {

  /// <summary>
  /// EmptySchema provides an EXISchema that supports all datatypes inherent
  /// in XML Schema such as xsd:int and xsd:dateTime, but with no 
  /// user-specific definitions. This is to support the use of dynamic 
  /// datatype associations discovered within elements during processing.
  /// </summary>
  public sealed class EmptySchema  {

    private const string COMPILED_SCHEMA_LOCATION = "EmptySchema.xsc";

    private static readonly EXISchema m_schema;

    static EmptySchema() {
      //URL optionslSchemaURI = typeof(HeaderOptionsSchema).getResource(COMPILED_SCHEMA_LOCATION);
      EXISchema schema = null;
      try {
        schema = CommonSchema.loadCompiledSchema(COMPILED_SCHEMA_LOCATION);
      }
      finally {
        m_schema = schema;
      }
    }
    /// <summary>
    /// Returns an EXISchema that supports all datatypes inherent in XML Schema.
    /// Calls to this method always return the same object. 
    /// @return
    /// </summary>
    public static EXISchema EXISchema {
      get {
        return m_schema;
      }
    }

    private EmptySchema() {
    }

  }

}