using System.IO;
using ValueApparatus = Nagasena.Proc.Grammars.ValueApparatus;
using Characters = Nagasena.Schema.Characters;

namespace Nagasena.Proc.IO {

  /// <exclude/>
  public abstract class ValueScanner : ValueApparatus {

    protected internal Stream m_istream;

    protected internal ValueScanner() {
      m_istream = null;
    }

    public abstract int getBuiltinRCS(int simpleType);

    public abstract Stream InputStream { set; }

    public abstract Characters scan(int name, int uri, int tp);

  }

}