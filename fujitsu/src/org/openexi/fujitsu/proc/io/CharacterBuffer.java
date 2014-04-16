package org.openexi.fujitsu.proc.io;

import org.openexi.fujitsu.proc.common.CharacterSequence;

public final class CharacterBuffer {
  
  static final int BUFSIZE_DEFAULT = 4096;
  
  char[] characters;
  private final int m_bufSize; 
  private int m_nextIndex;

  CharacterBuffer() {
    this(BUFSIZE_DEFAULT);
  }

  CharacterBuffer(int bufSize) {
    characters = new char[bufSize];
    m_bufSize = bufSize;
    m_nextIndex = 0;
  }
  
  int availability() {
    return m_bufSize - m_nextIndex;
  }
  
  int allocCharacters(int length) {
    final int nextIndex;
    if ((nextIndex = m_nextIndex + length) > m_bufSize) {
      return -1;
    }
    final int index = m_nextIndex;
    m_nextIndex = nextIndex;
    return index;
  }
  
  void redeemCharacters(int length) {
    m_nextIndex -= length;
  }

  CharacterSequence addChars(char[] chars) {
    final int length = chars.length;
    final int index = m_nextIndex;
    int pos = index;
    int i;
    for (i = 0; i < length && pos < m_bufSize; i++, pos++) {
      characters[pos] = chars[i];
    }
    if (i != length) {
      return null;
    }
    m_nextIndex = pos;
    return new Characters(characters, index, length);
  }

  public CharacterSequence addString(String stringValue, int length) {
    final int index = m_nextIndex;
    int pos = index;
    int i;
    for (i = 0; i < length && pos < m_bufSize; i++, pos++) {
      characters[pos] = stringValue.charAt(i);
    }
    if (i != length) {
      return null;
    }
    m_nextIndex = pos;
    return new Characters(characters, index, length);
  }

}
