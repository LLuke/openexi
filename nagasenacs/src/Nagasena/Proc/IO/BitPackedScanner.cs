using System.IO;

using AlignmentType = Nagasena.Proc.Common.AlignmentType;

namespace Nagasena.Proc.IO {

  internal sealed class BitPackedScanner : SimpleScanner {

    /// <summary>
    /// Either an instance of EXIOptionsInputStream or BufferedBitInputStream.
    /// </summary>
    private BitInputStream m_bitInputStream;
    private readonly BodyBitInputStream m_bodyBitInputStream;

    /*
     * Constructor is intentionally non-public.
     * Use ScannerFactory to instantiate a BitPackedScanner.
     */
    internal BitPackedScanner(bool isForEXIOptions) : base(isForEXIOptions) {
      m_bodyBitInputStream = isForEXIOptions ? null : new BodyBitInputStream();
    }

    public override Stream InputStream {
      set {
        m_bodyBitInputStream.InputStream = value;
        m_bitInputStream = m_bodyBitInputStream;
        base.InputStream = (Stream)null;
      }
    }

    public Stream EXIOptionsInputStream {
      set {
        m_bitInputStream = new HeaderOptionsInputStream(value);
        base.InputStream = (Stream)null;
      }
    }

    public void takeover(HeaderOptionsInputStream istream) {
      m_bodyBitInputStream.inheritResidue(istream);
      m_bitInputStream = m_bodyBitInputStream;
      base.InputStream = (Stream)null;
    }

    public BitInputStream BitInputStream {
      get {
        return m_bitInputStream;
      }
    }

    public override AlignmentType AlignmentType {
      get {
        return AlignmentType.bitPacked;
      }
    }

    protected internal override bool readBoolean(Stream istream) {
      // use m_dataStream irrespective of istream
      return m_bitInputStream.Bit;
    }

    protected internal override int readNBitUnsigned(int width, Stream istream) {
      // use m_dataStream irrespective of istream
      switch (width) {
          case 0:
            return 0;
        case 1:
          return m_bitInputStream.OneBit;
        case 2:
          return m_bitInputStream.TwoBits;
        case 3:
          return m_bitInputStream.ThreeBits;
        case 4:
          return m_bitInputStream.FourBits;
        case 5:
          return m_bitInputStream.FiveBits;
        default:
          return m_bitInputStream.getBits(width);
      }
    }

    protected internal override int readEightBitsUnsigned(Stream istream) {
      return m_bitInputStream.EightBits;
    }

  }

}