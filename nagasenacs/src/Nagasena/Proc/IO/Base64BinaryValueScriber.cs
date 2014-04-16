using Apparatus = Nagasena.Proc.Grammars.Apparatus;
using Base64 = Nagasena.Schema.Base64;
using EXISchema = Nagasena.Schema.EXISchema;

namespace Nagasena.Proc.IO {

  internal sealed class Base64BinaryValueScriber : BinaryValueScriber {

    public static readonly Base64BinaryValueScriber instance;
    static Base64BinaryValueScriber() {
      instance = new Base64BinaryValueScriber();
    }

    private Base64BinaryValueScriber() : base("exi:base64Binary") {
    }

    public override short CodecID {
      get {
        return Apparatus.CODEC_BASE64BINARY;
      }
    }

    public override int getBuiltinRCS(int simpleType, Scriber scriber) {
      return BuiltinRCS.RCS_ID_BASE64BINARY;
    }

    ////////////////////////////////////////////////////////////

    public override bool process(string value, int tp, EXISchema schema, Scribble scribble, Scriber scriber) {
      int len = value.Length;
      int n_maxBytes = ((int)((uint)len >> 2)) * 3;
      if ((len & 0x03) != 0) {
        n_maxBytes += 3;
      }
      byte[] binaryValue = scribble.expandOctetArray(n_maxBytes);
      int n_bytes;
      if ((n_bytes = Base64.decode(value, binaryValue)) == -1) {
        return false;
      }
      scribble.intValue1 = n_bytes;
      scribble.binaryValue = binaryValue;
      return true;
    }

  }

}