using System.Diagnostics;
using System.Numerics;
using System.IO;

namespace Nagasena.Proc.IO {

  /// <exclude/>
  public sealed class ByteAlignedCommons {

    private ByteAlignedCommons() {
    }

    public static bool readBoolean(Stream istream) {
      int val;
      switch (val = istream.ReadByte()) {
        case 1:
          return true;
        case 0:
          return false;
        default:
          Debug.Assert(val == -1);
          throw new EndOfStreamException();
      }
    }

    public static int readNBitUnsigned(int width, Stream istream) {
      Debug.Assert(width != 0);
      int n_bits = (width & 0x0007) != 0 ? (width & ~0x0007) + 8 : width & ~0x0007;
      int val = 0;
      int shift = 0;
      do {
        int nextByte;
        if ((nextByte = istream.ReadByte()) == -1) {
          throw new EndOfStreamException();
        }
        val |= nextByte << shift;
        shift += 8;
      }
      while (shift != n_bits);
      return val;
    }

    public static int readEightBitsUnsigned(Stream istream) {
      int nextByte;
      if ((nextByte = istream.ReadByte()) == -1) {
        throw new EndOfStreamException();
      }
      return nextByte;
    }

    public static void writeNBitUnsigned(int val, int width, Stream ostream) {
      int quotient = (int)((uint)width >> 3); // i.e. width / 8
      int n_bytes = (width & 0x0007) != 0 ? quotient + 1 : quotient;
      for (int i = 0; i < n_bytes; i++) {
        ostream.WriteByte((byte)((((uint)val) >> (i << 3)) & 0xFF));
      }
    }

    public static void writeUnsignedInteger32(int uinit, Stream ostream) {
      bool continued = true;
      do {
        int nextByte = uinit & 0x007F;
        if ((uinit = (int)(((uint)uinit) >> 7)) != 0) {
          nextByte |= 0x0080; // set continuation flag on
        }
        else {
          continued = false;
        }
        ostream.WriteByte((byte)nextByte);
      }
      while (continued);
    }

    public static void writeUnsignedInteger64(long uinit, Stream ostream) {
      bool continued = true;
      do {
        int nextByte = (int)uinit & 0x007F;
        if ((uinit = (long)(((ulong)uinit) >> 7)) != 0) {
          nextByte |= 0x0080; // set continuation flag on
        }
        else {
          continued = false;
        }
        ostream.WriteByte((byte)nextByte);
      }
      while (continued);
    }

    public static void writeUnsignedInteger(BigInteger uinit, Stream ostream) {
      bool continued = true;
      do {
        int nextByte = (int)(uinit & Scriber.BIGINTEGER_0x007F);
        uinit = uinit >> 7;
        if (!uinit.Equals(BigInteger.Zero)) {
          nextByte |= 0x0080; // set continuation flag on
        }
        else {
          continued = false;
        }
        ostream.WriteByte((byte)nextByte);
      }
      while (continued);
    }

  }

}