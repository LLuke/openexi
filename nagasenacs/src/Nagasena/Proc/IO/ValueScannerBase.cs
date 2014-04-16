using System.IO;

namespace Nagasena.Proc.IO {

  using QName = Nagasena.Proc.Common.QName;
  using EXISchema = Nagasena.Schema.EXISchema;

  internal abstract class ValueScannerBase : ValueScanner {

    private readonly QName m_name;

    internal ValueScannerBase(QName name) {
      m_name = name;
    }

    public override int getBuiltinRCS(int simpleType) {
      return EXISchema.NIL_NODE;
    }

    public override sealed Stream InputStream {
      set {
        m_istream = value;
      }
    }

    public override QName Name {
      get {
        return m_name;
      }
    }

  }

}