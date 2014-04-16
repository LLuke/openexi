using QName = Nagasena.Proc.Common.QName;
using Apparatus = Nagasena.Proc.Grammars.Apparatus;
using ExiUriConst = Nagasena.Proc.Util.ExiUriConst;
using Base64 = Nagasena.Schema.Base64;
using Characters = Nagasena.Schema.Characters;

namespace Nagasena.Proc.IO {

  internal sealed class Base64BinaryValueScanner : BinaryValueScanner {

    internal Base64BinaryValueScanner(Scanner scanner) : base(new QName("exi:base64Binary", ExiUriConst.W3C_2009_EXI_URI), scanner) {
    }

    public override short CodecID {
      get {
        return Apparatus.CODEC_BASE64BINARY;
      }
    }

    public override int getBuiltinRCS(int simpleType) {
      return BuiltinRCS.RCS_ID_BASE64BINARY;
    }

    public override Characters scan(int localNameId, int uriId, int tp) {
      int len = m_scanner.readUnsignedInteger(m_istream);
      if (m_octets.Length < len) {
        expandBuffer(len);
      }
      for (int i = 0; i < len; i++) {
        m_octets[i] = (byte)m_scanner.readEightBitsUnsigned(m_istream);
      }
      int maxChars = Base64.calculateTextMaxLength(len);
      m_scanner.m_characterBuffer.ensureCharacters(maxChars);
      char[] characters = m_scanner.m_characterBuffer.characters;
      int startIndex = m_scanner.m_characterBuffer.allocCharacters(maxChars);
      int n_chars = Base64.encode(m_octets, 0, len, characters, startIndex);
      return new Characters(characters, startIndex, n_chars, m_scanner.m_characterBuffer.isVolatile);
    }

  }

}