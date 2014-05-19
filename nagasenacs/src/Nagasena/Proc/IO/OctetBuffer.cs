using System;

namespace Nagasena.Proc.IO {

  /// <exclude/>
  public sealed class OctetBuffer {

    internal byte[] octets;
    private int m_bufSize;

    public int nextIndex;

    internal OctetBuffer() {
      m_bufSize = 0;
      nextIndex = 0;
      octets = null;
    }

    internal void init(int initialBufferSize) {
      if (octets == null) {
        m_bufSize = initialBufferSize;
        octets = new byte[initialBufferSize];
      }
    }

    internal void ensureOctets(int length) {
      int shortage;
      if ((shortage = length - (m_bufSize - nextIndex)) > 0) {
        int halfBufSize = m_bufSize >> 1;
        int expansion;
        if (shortage < halfBufSize) {
          expansion = halfBufSize;
        }
        else {
          if (shortage < m_bufSize) {
            expansion = m_bufSize;
          }
          else {
            expansion = shortage + halfBufSize;
          }
        }
        int newBufSize = m_bufSize + expansion;
        byte[] _octets = new byte[newBufSize];
        Array.Copy(octets, 0, _octets, 0, nextIndex);
        octets = _octets;
        m_bufSize = newBufSize;
      }
    }

    internal int allocOctets(int length) {
      int _nextIndex;
      if ((_nextIndex = nextIndex + length) > m_bufSize) {
        return -1;
      }
      int index = nextIndex;
      nextIndex = _nextIndex;
      return index;
    }

  }

}