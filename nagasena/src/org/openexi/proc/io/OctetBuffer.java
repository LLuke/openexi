package org.openexi.proc.io;

public final class OctetBuffer {

  byte[] octets;
  private int m_bufSize;
  
  public int nextIndex;

  OctetBuffer() {
    m_bufSize = 0;
    nextIndex = 0;
    octets = null;
  }
  
  final void init(int initialBufferSize) {
    if (octets == null) {
      m_bufSize = initialBufferSize;
      octets = new byte[initialBufferSize];
    }
  }

  final void ensureOctets(final int length) {
    final int shortage;
    if ((shortage = length - (m_bufSize - nextIndex)) > 0) {
      final int halfBufSize = m_bufSize >> 1;
      final int expansion;
      if (shortage < halfBufSize) {
        expansion = halfBufSize;
      }
      else {
        if (shortage < m_bufSize)
          expansion = m_bufSize;
        else
          expansion = shortage + halfBufSize;
      }
      final int newBufSize = m_bufSize + expansion;
      final byte[] _octets = new byte[newBufSize];
      System.arraycopy(octets, 0, _octets, 0, nextIndex);
      octets = _octets;
      m_bufSize = newBufSize;
    }
  }
  
  final int allocOctets(int length) {
    final int _nextIndex;
    if ((_nextIndex = nextIndex + length) > m_bufSize) {
      return -1;
    }
    final int index = nextIndex;
    nextIndex = _nextIndex;
    return index;
  }

}
