using System.IO;
using System.Numerics;

using AlignmentType = Nagasena.Proc.Common.AlignmentType;

namespace Nagasena.Proc.IO {

  internal sealed class BitPackedScriber : SimpleScriber {

    private BitOutputStream m_dataStream;

    internal BitPackedScriber(bool isForEXIOptions) : base(isForEXIOptions) {
      m_dataStream = null;
    }

    public override AlignmentType AlignmentType {
      get {
        return AlignmentType.bitPacked;
      }
    }

    /// <summary>
    /// Set an output stream to which encoded streams are written out. </summary>
    /// <param name="dataStream"> output stream </param>
    public override Stream OutputStream {
      set {
        m_dataStream = new BitOutputStream(value);
      }
    }

    public BitOutputStream BitOutputStream {
      set {
        m_dataStream = value;
      }
      get {
        return m_dataStream;
      }
    }

    public void writeBoolean(bool val) {
      m_dataStream.putBit(val);
    }

    protected internal override void writeUnsignedInteger32(int @uint, Stream ostream) {
      bool continued = true;
      do {
        int nextByte = @uint & 0x007F;
        if ((@uint = (int)((uint)@uint >> 7)) != 0) {
          nextByte |= 0x0080; // set continuation flag on
        }
        else {
          continued = false;
        }
        writeNBitUnsigned(nextByte, 8, (Stream)null);
      }
      while (continued);
    }

    protected internal override void writeUnsignedInteger64(long @ulong, Stream ostream) {
      bool continued = true;
      do {
        int nextByte = (int)@ulong & 0x007F;
        if ((@ulong = (long)((ulong)@ulong >> 7)) != 0) {
          nextByte |= 0x0080; // set continuation flag on
        }
        else {
          continued = false;
        }
        writeNBitUnsigned(nextByte, 8, (Stream)null);
      }
      while (continued);
    }

    protected internal override void writeUnsignedInteger(BigInteger @uint, Stream ostream) {
      bool continued = true;
      do {
        int nextByte = (int)(@uint & BIGINTEGER_0x007F);
        @uint = @uint >> 7;
        if (!@uint.Equals(BigInteger.Zero)) {
          nextByte |= 0x0080; // set continuation flag on
        }
        else {
          continued = false;
        }
        writeNBitUnsigned(nextByte, 8, (Stream)null);
      }
      while (continued);
    }

    protected internal override void writeBoolean(bool val, Stream ostream) {
      m_dataStream.putBit(val);
    }

    protected internal override void writeNBitUnsigned(int val, int width, Stream ostream) {
      m_dataStream.putBits(width, val);
    }

    public override void finish() {
      m_dataStream.flush();
    }

  }

}