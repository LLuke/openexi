using System.Text;

using QName = Nagasena.Proc.Common.QName;
using Apparatus = Nagasena.Proc.Grammars.Apparatus;
using ExiUriConst = Nagasena.Proc.Util.ExiUriConst;
using Characters = Nagasena.Schema.Characters;
using HexBin = Nagasena.Schema.HexBin;

namespace Nagasena.Proc.IO {

  internal sealed class HexBinaryValueScanner : BinaryValueScanner {

    private readonly StringBuilder m_stringBuffer;

    internal HexBinaryValueScanner(Scanner scanner) : base(new QName("exi:hexBinary", ExiUriConst.W3C_2009_EXI_URI), scanner) {
      m_stringBuffer = new StringBuilder();
    }

    public override short CodecID {
      get {
        return Apparatus.CODEC_HEXBINARY;
      }
    }

    public override int getBuiltinRCS(int simpleType) {
      return BuiltinRCS.RCS_ID_HEXBINARY;
    }

    public override Characters scan(int localNameId, int uriId, int tp) {
      int len = m_scanner.readUnsignedInteger(m_istream);
      if (m_octets.Length < len) {
        expandBuffer(len);
      }
      for (int i = 0; i < len; i++) {
        m_octets[i] = (byte)m_scanner.readEightBitsUnsigned(m_istream);
      }
      m_stringBuffer.Length = 0;
      HexBin.encode(m_octets, len, m_stringBuffer);
      string stringValue = m_stringBuffer.ToString();
      int length = stringValue.Length;
      m_scanner.m_characterBuffer.ensureCharacters(length);
      return m_scanner.m_characterBuffer.addString(stringValue, length);
    }

  }

}