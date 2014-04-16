using System.IO;

namespace Nagasena.Proc.IO {

  public abstract class BitInputStream {

    internal Stream @in;
    internal int nBits;
    internal int bitBuf;

    internal BitInputStream() {
      nBits = 0;
      bitBuf = 0;
    }

    public abstract bool Bit { get; }

    public abstract int getBits(int n);

    public abstract int OneBit { get; }

    public abstract int TwoBits { get; }

    public abstract int ThreeBits { get; }

    public abstract int FourBits { get; }

    public abstract int FiveBits { get; }

    public abstract int EightBits { get; }

  }

}