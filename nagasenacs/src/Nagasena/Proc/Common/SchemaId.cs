namespace Nagasena.Proc.Common {

  /// <summary>
  /// SchemaId represents the <see href="http://www.w3.org/TR/exi/#key-schemaIdOption">schemaId</see> 
  /// property of an EXI stream.
  /// </summary>
  public sealed class SchemaId {

    private string m_value;

    public SchemaId(string val) {
      m_value = val;
    }

    public string Value {
      get {
        return m_value;
      }
    }

  }

}