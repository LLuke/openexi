using System.IO;

using QName = Nagasena.Proc.Common.QName;
using Apparatus = Nagasena.Proc.Grammars.Apparatus;
using Characters = Nagasena.Schema.Characters;

namespace Nagasena.Proc.IO {

  internal sealed class ValueScannerLexical : ValueScanner {

    private readonly ValueScanner m_baseValueScanner;
    private readonly StringValueScanner m_stringValueScanner;

    public ValueScannerLexical(ValueScanner baseValueScanner, StringValueScanner stringValueScanner) {
      m_baseValueScanner = baseValueScanner;
      m_stringValueScanner = stringValueScanner;
    }

    public override QName Name {
      get {
        return m_baseValueScanner.Name;
      }
    }

    public override short CodecID {
      get {
        return Apparatus.CODEC_LEXICAL;
      }
    }

    public override int getBuiltinRCS(int simpleType) {
      return m_baseValueScanner.getBuiltinRCS(simpleType);
    }

    public override Stream InputStream {
      set {
        m_baseValueScanner.InputStream = value;
      }
    }

    public override Characters scan(int name, int uri, int tp) {
      return m_stringValueScanner.scan(name, uri, m_baseValueScanner.getBuiltinRCS(tp));
    }

  }

}