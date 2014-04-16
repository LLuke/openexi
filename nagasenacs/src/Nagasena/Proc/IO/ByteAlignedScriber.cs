using System.IO;

using AlignmentType = Nagasena.Proc.Common.AlignmentType;

namespace Nagasena.Proc.IO {

  internal sealed class ByteAlignedScriber : SimpleScriber {

    public ByteAlignedScriber() : 
      base(false) {
    }

    public override AlignmentType AlignmentType {
      get {
        return AlignmentType.byteAligned;
      }
    }

    /// <summary>
    /// Set an output stream to which encoded streams are written out. </summary>
    /// <param name="dataStream"> output stream </param>
    public override Stream OutputStream {
      set {
        m_outputStream = value;
      }
    }

    protected internal override void writeUnsignedInteger32(int @ulong, Stream ostream) {
      ByteAlignedCommons.writeUnsignedInteger32(@ulong, m_outputStream);
    }

    protected internal override void writeUnsignedInteger64(long @uint, Stream ostream) {
      ByteAlignedCommons.writeUnsignedInteger64(@uint, m_outputStream);
    }

    protected internal override void writeUnsignedInteger(System.Numerics.BigInteger @uint, Stream ostream) {
      ByteAlignedCommons.writeUnsignedInteger(@uint, m_outputStream);
    }

    protected internal override void writeBoolean(bool val, Stream ostream) {
      m_outputStream.WriteByte((byte)(val ? 1 : 0));
    }

    protected internal override void writeNBitUnsigned(int val, int width, Stream ostream) {
      ByteAlignedCommons.writeNBitUnsigned(val, width, m_outputStream);
    }

    public override void finish() {
      m_outputStream.Flush();
    }

  }

}