using System.IO;

namespace Nagasena.Proc.IO {

  /// <exclude/>
  public class BitOutputStream {

    private int putCount = 8;
    private int bitBuf = 0;

    private readonly Stream m_out;

    internal BitOutputStream(Stream @out) {
      m_out = @out;
    }

    /// <summary>
    /// Returns the right-most n-bits of x
    /// </summary>
    private static int rightBits(int n, int x) {
      return x & ((1 << n) - 1);
    }

    /// <summary>
    /// Put a single bit.
    /// </summary>
    public virtual void putBit(bool bit) {
      putCount--;
      if (bit) {
        bitBuf |= (1 << putCount);
      }
      if (putCount == 0) {
        m_out.WriteByte((byte)bitBuf);
        bitBuf = 0;
        putCount = 8;
      }
    }

    /// <summary>
    /// Put the right-most n-bits of x.
    /// </summary>
    public virtual void putBits(int n, int x) {
      while (n >= putCount) {
        n -= putCount;
        bitBuf |= rightBits(putCount, (int)((uint)x >> n));
        m_out.WriteByte((byte)bitBuf);
        bitBuf = 0;
        putCount = 8;
      }
      putCount -= n;
      bitBuf |= rightBits(n, x) << putCount;
    }

    public virtual void flush() {
      if (putCount != 8) {
        putBits(putCount, 0);
      }
      m_out.Flush();
    }

  }

}