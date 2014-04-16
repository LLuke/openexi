package org.openexi.proc.io;

import org.openexi.proc.common.CharacterSequence;

final class Characters implements CharacterSequence {

  private final char[] m_characters;
  private final int m_startIndex;
  private final int m_length;
  private final int m_mojiCount;
  private final int m_hashCode;

  public static final CharacterSequence TRUE;
  public static final CharacterSequence FALSE;
  static {
    TRUE = new Characters("true".toCharArray(), 0, "true".length());
    FALSE = new Characters("false".toCharArray(), 0, "false".length());
  }
  
  Characters(char[] characters, int startIndex, int length) {
    m_characters = characters;
    m_startIndex = startIndex;
    m_length = length;
    int hashCode = 0;
    final int limit = m_startIndex + m_length;
    int mojiCount = 0;
    for (int i = m_startIndex; i < limit; ++mojiCount) {
      char currentChar = m_characters[i++];
      hashCode = hashCode * 31 + currentChar;
      if ((currentChar & 0xFC00) == 0xD800) { // high surrogate
        if (i < limit) {
          currentChar = m_characters[i];
          if ((currentChar & 0xFC00) == 0xDC00) { // low surrogate
            ++i;
          }
        }
      }
    }
    m_mojiCount = mojiCount;
    m_hashCode = hashCode;
  }
  
  public char[] getCharacters() {
    return m_characters;
  }
  
  public int getStartIndex() {
    return m_startIndex;
  }
  
  public int length() {
    return m_length;
  }
  
  public int getUCSCount() {
    return m_mojiCount;
  }

  public int indexOf(char c) {
    final int limit = m_startIndex + m_length;
    for (int i = m_startIndex; i < limit; i++) {
      if (m_characters[i] == c) 
        return i;
    }
    return -1;
  }

  public String substring(int beginIndex, int endIndex) {
    return new String(m_characters, beginIndex, endIndex - beginIndex);
  }

  @Override
  public int hashCode() {
    return m_hashCode;
  }

  @Override
  public boolean equals(Object object) {
    if (object instanceof Characters) {
      final Characters characterSequence = (Characters)object;
      if (m_length != characterSequence.m_length) 
        return false;
      else {
        final int startIndex = characterSequence.m_startIndex;
        final char[] characters = characterSequence.m_characters;
        for (int i = 0; i < m_length; i++) {
          if (m_characters[m_startIndex + i] != characters[startIndex + i])
            return false;
        }
        return true;
      }
    }
    else
      return false;
  }
  
  public String makeString() {
    return new String(m_characters, m_startIndex, m_length);
  }

}
