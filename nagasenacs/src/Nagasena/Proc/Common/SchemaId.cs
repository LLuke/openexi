namespace Nagasena.Proc.Common {

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