using System.IO;

using Apparatus = Nagasena.Proc.Grammars.Apparatus;
using EXISchema = Nagasena.Schema.EXISchema;
using HexBin = Nagasena.Schema.HexBin;

namespace Nagasena.Proc.IO {

  internal sealed class HexBinaryValueScriber : BinaryValueScriber {

    public static readonly HexBinaryValueScriber instance;
    static HexBinaryValueScriber() {
      instance = new HexBinaryValueScriber();
    }

    private HexBinaryValueScriber() : base("exi:hexBinary") {
    }

    public override short CodecID {
      get {
        return Apparatus.CODEC_HEXBINARY;
      }
    }

    public override int getBuiltinRCS(int simpleType, Scriber scriber) {
      return BuiltinRCS.RCS_ID_HEXBINARY;
    }

    ////////////////////////////////////////////////////////////

    public override bool process(string value, int tp, EXISchema schema, Scribble scribble, Scriber scriber) {
      int len = value.Length;
      int n_maxBytes = (int)((uint)len >> 1);
      byte[] binaryValue = scribble.expandOctetArray(n_maxBytes);
      int n_bytes;
      if ((n_bytes = HexBin.decode(value, binaryValue)) == -1) {
        return false;
      }
      scribble.intValue1 = n_bytes;
      scribble.binaryValue = binaryValue;
      return true;
    }

  }

}