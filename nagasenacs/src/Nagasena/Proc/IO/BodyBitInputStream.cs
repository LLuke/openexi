using System.Diagnostics;
using System.IO;

namespace Nagasena.Proc.IO {

  /// <summary>
  /// An implementation of BitInputStream representing the EXI body of
  /// a bit-packed stream.
  /// </summary>
  /// <exclude/>
  public sealed class BodyBitInputStream : BitInputStream {

    private const int BYTEBUFFER_LENGTH = 8192;
    private readonly byte[] m_bytes;
    private int m_bufLen;
    private int m_curPos;

    internal BodyBitInputStream() {
      m_bytes = new byte[BYTEBUFFER_LENGTH];
      m_curPos = m_bufLen = 0;
    }

    internal Stream InputStream {
      set {
        this.@in = value;
        nBits = 0;
        bitBuf = 0;
        m_curPos = m_bufLen = 0;
      }
    }

    internal void inheritResidue(HeaderOptionsInputStream inputStreamDirect) {
      @in = inputStreamDirect.@in;
      nBits = inputStreamDirect.nBits;
      bitBuf = inputStreamDirect.bitBuf;
      m_curPos = m_bufLen = 0;
    }

    private int fill() {
      Debug.Assert(m_curPos == m_bufLen);
      m_curPos = 0;
      if ((m_bufLen = @in.Read(m_bytes, 0, BYTEBUFFER_LENGTH)) == -1) {
        m_bufLen = 0;
        return -1; //* EOF
      }
      return m_bufLen;
    }

    /// <summary>
    /// Retrieves a single bit.
    /// </summary>
    public override bool Bit {
      get {
        if (--nBits != -1) {
          return (((int)((uint)bitBuf >> nBits)) & 1) == 1;
        }
        if (m_curPos == m_bufLen && fill() == -1) {
          throw new EndOfStreamException();
        }
        nBits = 7;
        bitBuf = m_bytes[m_curPos++] & 0xFF;
        return (((int)((uint)bitBuf >> 7)) & 1) == 1;
      }
    }

    /// <summary>
    /// Retrieves n-bits where 0 < n <= 32. 
    /// </summary>
    public override int getBits(int n) {
      int x = 0;
      while (n > nBits) {
        /// <summary>
        /// NOTE: used to be the following next two lines.
        /// n -= getCount;
        /// x |= rightBits(getCount, bitBuf) << n;
        /// </summary>
        x |= (bitBuf & ((1 << nBits) - 1)) << (n -= nBits);
        if (m_curPos == m_bufLen && fill() == -1) {
          throw new EndOfStreamException();
        }
        bitBuf = m_bytes[m_curPos++] & 0xFF;
        nBits = 8;
      }
      /// NOTE: used to be the following next two lines.
      /// getCount -= n;
      /// return x | rightBits(n, bitBuf >>> getCount);  
      return x | ((int)((uint)bitBuf >> (nBits -= n))) & ((1 << n) - 1);
    }

    public override int OneBit {
      get {
        switch (nBits) {
          case 0:
            if (m_curPos == m_bufLen && fill() == -1) {
              throw new EndOfStreamException();
            }
            bitBuf = m_bytes[m_curPos++];
            nBits = 7;
            return ((int)((uint)bitBuf >> 7)) & 0x01;
          case 1:
            nBits = 0;
            return bitBuf & 0x01;
          case 2:
            nBits = 1;
            return ((int)((uint)bitBuf >> 1)) & 0x01;
          case 3:
            nBits = 2;
            return ((int)((uint)bitBuf >> 2)) & 0x01;
          case 4:
            nBits = 3;
            return ((int)((uint)bitBuf >> 3)) & 0x01;
          case 5:
            nBits = 4;
            return ((int)((uint)bitBuf >> 4)) & 0x01;
          case 6:
            nBits = 5;
            return ((int)((uint)bitBuf >> 5)) & 0x01;
          case 7:
            nBits = 6;
            return ((int)((uint)bitBuf >> 6)) & 0x01;
          case 8:
            nBits = 7;
            return ((int)((uint)bitBuf >> 7)) & 0x01;
          default:
            Debug.Assert(false);
            return -1;
        }
      }
    }

    public override int TwoBits {
      get {
        switch (nBits) {
          case 0:
            if (m_curPos == m_bufLen && fill() == -1) {
              throw new EndOfStreamException();
            }
            bitBuf = m_bytes[m_curPos++];
            nBits = 6;
            return ((int)((uint)bitBuf >> 6)) & 0x03;
          case 1:
            int x = (bitBuf & 0x01) << 1;
            if (m_curPos == m_bufLen && fill() == -1) {
              throw new EndOfStreamException();
            }
            bitBuf = m_bytes[m_curPos++];
            nBits = 7;
            return x | (((int)((uint)bitBuf >> 7)) & 0x01);
          case 2:
            nBits = 0;
            return bitBuf & 0x03;
          case 3:
            nBits = 1;
            return ((int)((uint)bitBuf >> 1)) & 0x03;
          case 4:
            nBits = 2;
            return ((int)((uint)bitBuf >> 2)) & 0x03;
          case 5:
            nBits = 3;
            return ((int)((uint)bitBuf >> 3)) & 0x03;
          case 6:
            nBits = 4;
            return ((int)((uint)bitBuf >> 4)) & 0x03;
          case 7:
            nBits = 5;
            return ((int)((uint)bitBuf >> 5)) & 0x03;
          case 8:
            nBits = 6;
            return ((int)((uint)bitBuf >> 6)) & 0x03;
          default:
            Debug.Assert(false);
            return -1;
        }
      }
    }

    public override int ThreeBits {
      get {
        int x;
        switch (nBits) {
          case 0:
            if (m_curPos == m_bufLen && fill() == -1) {
              throw new EndOfStreamException();
            }
            bitBuf = m_bytes[m_curPos++];
            nBits = 5;
            return ((int)((uint)bitBuf >> 5)) & 0x07;
          case 1:
            x = (bitBuf & 0x01) << 2;
            if (m_curPos == m_bufLen && fill() == -1) {
              throw new EndOfStreamException();
            }
            bitBuf = m_bytes[m_curPos++];
            nBits = 6;
            return x | (((int)((uint)bitBuf >> 6)) & 0x03);
          case 2:
            x = (bitBuf & 0x03) << 1;
            if (m_curPos == m_bufLen && fill() == -1) {
              throw new EndOfStreamException();
            }
            bitBuf = m_bytes[m_curPos++];
            nBits = 7;
            return x | (((int)((uint)bitBuf >> 7)) & 0x01);
          case 3:
            nBits = 0;
            return bitBuf & 0x07;
          case 4:
            nBits = 1;
            return ((int)((uint)bitBuf >> 1)) & 0x07;
          case 5:
            nBits = 2;
            return ((int)((uint)bitBuf >> 2)) & 0x07;
          case 6:
            nBits = 3;
            return ((int)((uint)bitBuf >> 3)) & 0x07;
          case 7:
            nBits = 4;
            return ((int)((uint)bitBuf >> 4)) & 0x07;
          case 8:
            nBits = 5;
            return ((int)((uint)bitBuf >> 5)) & 0x07;
          default:
            Debug.Assert(false);
            return -1;
        }
      }
    }

    public override int FourBits {
      get {
        int x;
        switch (nBits) {
          case 0:
            if (m_curPos == m_bufLen && fill() == -1) {
              throw new EndOfStreamException();
            }
            bitBuf = m_bytes[m_curPos++];
            nBits = 4;
            return ((int)((uint)bitBuf >> 4)) & 0x0F;
          case 1:
            x = (bitBuf & 0x01) << 3;
            if (m_curPos == m_bufLen && fill() == -1) {
              throw new EndOfStreamException();
            }
            bitBuf = m_bytes[m_curPos++];
            nBits = 5;
            return x | (((int)((uint)bitBuf >> 5)) & 0x07);
          case 2:
            x = (bitBuf & 0x03) << 2;
            if (m_curPos == m_bufLen && fill() == -1) {
              throw new EndOfStreamException();
            }
            bitBuf = m_bytes[m_curPos++];
            nBits = 6;
            return x | (((int)((uint)bitBuf >> 6)) & 0x03);
          case 3:
            x = (bitBuf & 0x07) << 1;
            if (m_curPos == m_bufLen && fill() == -1) {
              throw new EndOfStreamException();
            }
            bitBuf = m_bytes[m_curPos++];
            nBits = 7;
            return x | (((int)((uint)bitBuf >> 7)) & 0x01);
          case 4:
            nBits = 0;
            return bitBuf & 0x0F;
          case 5:
            nBits = 1;
            return ((int)((uint)bitBuf >> 1)) & 0x0F;
          case 6:
            nBits = 2;
            return ((int)((uint)bitBuf >> 2)) & 0x0F;
          case 7:
            nBits = 3;
            return ((int)((uint)bitBuf >> 3)) & 0x0F;
          case 8:
            nBits = 4;
            return ((int)((uint)bitBuf >> 4)) & 0x0F;
          default:
            Debug.Assert(false);
            return -1;
        }
      }
    }

    public override int FiveBits {
      get {
        int x;
        switch (nBits) {
          case 0:
            if (m_curPos == m_bufLen && fill() == -1) {
              throw new EndOfStreamException();
            }
            bitBuf = m_bytes[m_curPos++];
            nBits = 3;
            return ((int)((uint)bitBuf >> 3)) & 0x1F;
          case 1:
            x = (bitBuf & 0x01) << 4;
            if (m_curPos == m_bufLen && fill() == -1) {
              throw new EndOfStreamException();
            }
            bitBuf = m_bytes[m_curPos++];
            nBits = 4;
            return x | (((int)((uint)bitBuf >> 4)) & 0x0F);
          case 2:
            x = (bitBuf & 0x03) << 3;
            if (m_curPos == m_bufLen && fill() == -1) {
              throw new EndOfStreamException();
            }
            bitBuf = m_bytes[m_curPos++];
            nBits = 5;
            return x | (((int)((uint)bitBuf >> 5)) & 0x07);
          case 3:
            x = (bitBuf & 0x07) << 2;
            if (m_curPos == m_bufLen && fill() == -1) {
              throw new EndOfStreamException();
            }
            bitBuf = m_bytes[m_curPos++];
            nBits = 6;
            return x | (((int)((uint)bitBuf >> 6)) & 0x03);
          case 4:
            x = (bitBuf & 0x0F) << 1;
            if (m_curPos == m_bufLen && fill() == -1) {
              throw new EndOfStreamException();
            }
            bitBuf = m_bytes[m_curPos++];
            nBits = 7;
            return x | (((int)((uint)bitBuf >> 7)) & 0x01);
          case 5:
            nBits = 0;
            return bitBuf & 0x1F;
          case 6:
            nBits = 1;
            return ((int)((uint)bitBuf >> 1)) & 0x1F;
          case 7:
            nBits = 2;
            return ((int)((uint)bitBuf >> 2)) & 0x1F;
          case 8:
            nBits = 3;
            return ((int)((uint)bitBuf >> 3)) & 0x1F;
          default:
            Debug.Assert(false);
            return -1;
        }
      }
    }

    public override int EightBits {
      get {
        int x;
        switch (nBits) {
          case 0:
            if (m_curPos == m_bufLen && fill() == -1) {
              throw new EndOfStreamException();
            }
            return m_bytes[m_curPos++] & 0xFF;
          case 8:
            nBits = 0;
            return bitBuf;
          case 1:
            x = (bitBuf & 1) << 7;
            if (m_curPos == m_bufLen && fill() == -1) {
              throw new EndOfStreamException();
            }
            bitBuf = m_bytes[m_curPos++] & 0xFF;
            return x | ((int)((uint)bitBuf >> 1));
          case 2:
            x = (bitBuf & 3) << 6;
            if (m_curPos == m_bufLen && fill() == -1) {
              throw new EndOfStreamException();
            }
            bitBuf = m_bytes[m_curPos++] & 0xFF;
            return x | ((int)((uint)bitBuf >> 2));
          case 3:
            x = (bitBuf & 7) << 5;
            if (m_curPos == m_bufLen && fill() == -1) {
              throw new EndOfStreamException();
            }
            bitBuf = m_bytes[m_curPos++] & 0xFF;
            return x | ((int)((uint)bitBuf >> 3));
          case 4:
            x = (bitBuf & 15) << 4;
            if (m_curPos == m_bufLen && fill() == -1) {
              throw new EndOfStreamException();
            }
            bitBuf = m_bytes[m_curPos++] & 0xFF;
            return x | ((int)((uint)bitBuf >> 4));
          case 5:
            x = (bitBuf & 31) << 3;
            if (m_curPos == m_bufLen && fill() == -1) {
              throw new EndOfStreamException();
            }
            bitBuf = m_bytes[m_curPos++] & 0xFF;
            return x | ((int)((uint)bitBuf >> 5));
          case 6:
            x = (bitBuf & 63) << 2;
            if (m_curPos == m_bufLen && fill() == -1) {
              throw new EndOfStreamException();
            }
            bitBuf = m_bytes[m_curPos++] & 0xFF;
            return x | ((int)((uint)bitBuf >> 6));
          case 7:
            x = (bitBuf & 127) << 1;
            if (m_curPos == m_bufLen && fill() == -1) {
              throw new EndOfStreamException();
            }
            bitBuf = m_bytes[m_curPos++] & 0xFF;
            return x | ((int)((uint)bitBuf >> 7));
          default:
            Debug.Assert(false);
            return -1;
        }
      }
    }

    /// DON't remove this comment!
    /// Returns the right-most n-bits of x
    /// private static int rightBits(int n, int x) {
    ///   return x & ((1 << n) - 1);
    /// }
  }

}