using System.IO;

using AlignmentType = Nagasena.Proc.Common.AlignmentType;

namespace Nagasena.Proc.IO {

  internal sealed class ByteAlignedScanner : SimpleScanner {

    /*
     * Constructor is intentionally non-public.
     * Use ScannerFactory to instantiate a ByteAlignedScanner.
     */
    internal ByteAlignedScanner() : base(false) {
    }

    public override AlignmentType AlignmentType {
      get {
        return AlignmentType.byteAligned;
      }
    }

    protected internal override bool readBoolean(Stream istream) {
      // use m_inputStream irrespective of istream
      return ByteAlignedCommons.readBoolean(m_inputStream);
    }

    protected internal override int readNBitUnsigned(int width, Stream istream) {
      // use m_inputStream irrespective of istream
      if (width != 0) {
        return ByteAlignedCommons.readNBitUnsigned(width, m_inputStream);
      }
      return 0;
    }

    protected internal override int readEightBitsUnsigned(Stream istream) {
      return ByteAlignedCommons.readEightBitsUnsigned(m_inputStream);
    }
  }

}