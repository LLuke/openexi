using System.Diagnostics;

namespace Nagasena.Proc.IO {

  using QName = Nagasena.Proc.Common.QName;
  using StringTable = Nagasena.Proc.Common.StringTable;
  using Apparatus = Nagasena.Proc.Grammars.Apparatus;
  using ExiUriConst = Nagasena.Proc.Util.ExiUriConst;
  using Characters = Nagasena.Schema.Characters;

  internal sealed class StringValueScanner : ValueScannerBase {

    // Owner scanner
    private readonly Scanner m_scanner;
    private StringTable.GlobalValuePartition m_globalValuePartition;

    private int m_valueMaxExclusiveLength;

    internal StringValueScanner(Scanner scanner) : base(new QName("exi:string", ExiUriConst.W3C_2009_EXI_URI)) {
      m_scanner = scanner;
      m_valueMaxExclusiveLength = int.MaxValue;
    }

    public override short CodecID {
      get {
        return Apparatus.CODEC_STRING;
      }
    }

    internal StringTable StringTable {
      set {
        m_globalValuePartition = value.globalValuePartition;
      }
    }

    public int ValueMaxLength {
      set {
        Debug.Assert(value >= 0);
        m_valueMaxExclusiveLength = value != int.MaxValue ? value + 1 : int.MaxValue;
      }
    }

    public override Characters scan(int localNameId, int uriId, int tp) {
      int ucsCount = m_scanner.readUnsignedInteger(m_istream);
      if ((ucsCount & 0xFFFFFFFE) != 0) { // i.e. length > 1
        if ((ucsCount -= 2) != 0) {
          Characters value = m_scanner.readLiteralString(ucsCount, tp, m_istream);
          if (ucsCount < m_valueMaxExclusiveLength) {
            m_globalValuePartition.addValue(value, localNameId, uriId);
          }
          return value;
        }
        else {
          return Characters.CHARACTERS_EMPTY;
        }
      }
      else {
        int id;
        if (ucsCount == 0) {
          StringTable.LocalValuePartition localPartition;
          localPartition = m_globalValuePartition.getLocalPartition(localNameId, uriId);
          id = m_scanner.readNBitUnsigned(localPartition.width, m_istream);
          return localPartition.valueEntries[id].value;
        }
        else { // length == 1
          id = m_scanner.readNBitUnsigned(m_globalValuePartition.width, m_istream);
          return m_globalValuePartition.valueEntries[id].value;
        }
      }
    }

  }

}