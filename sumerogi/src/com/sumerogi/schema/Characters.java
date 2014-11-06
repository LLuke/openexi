package com.sumerogi.schema;

public final class Characters {

  public boolean isVolatile;

  public char[] characters;
  public int startIndex;
  public final int length;
  public final int ucsCount;
  private final int m_hashCode;

  public static final Characters CHARACTERS_EMPTY;
  public static final Characters CHARACTERS_NULL;
  static {
    CHARACTERS_EMPTY = new Characters("".toCharArray(), 0, 0, false);
    CHARACTERS_NULL = new Characters("null".toCharArray(), 0, 4, false);
  }
  
  public Characters(char[] characters, int startIndex, int length, boolean isVolatile) {
  	this.isVolatile = isVolatile;
    this.characters = characters;
    this.startIndex = startIndex;
    this.length = length;
    int hashCode = 0;
    final int limit = this.startIndex + this.length;
    int mojiCount = 0;
    for (int i = this.startIndex; i < limit; ++mojiCount) {
      char currentChar = this.characters[i++];
      hashCode = hashCode * 31 + currentChar;
      if ((currentChar & 0xFC00) == 0xD800) { // high surrogate
        if (i < limit) {
          currentChar = this.characters[i];
          if ((currentChar & 0xFC00) == 0xDC00) { // low surrogate
            ++i;
          }
        }
      }
    }
    this.ucsCount = mojiCount;
    m_hashCode = hashCode;
  }
  
  public void turnPermanent() {
    if (isVolatile) {
      final char[] _characters = new char[length];
      System.arraycopy(characters, startIndex, _characters, 0, length);
      characters = _characters;
      startIndex = 0;
      isVolatile = false;
    }
    return;
  }
  
  public int indexOf(char c) {
    final int limit = startIndex + length;
    for (int i = startIndex; i < limit; i++) {
      if (characters[i] == c) 
        return i;
    }
    return -1;
  }

  public String substring(int beginIndex, int endIndex) {
    return new String(characters, beginIndex, endIndex - beginIndex);
  }

  @Override
  public int hashCode() {
    return m_hashCode;
  }

  @Override
  public boolean equals(Object object) {
    if (object instanceof Characters) {
      final Characters characterSequence = (Characters)object;
      if (length != characterSequence.length) 
        return false;
      else {
        final int startIndex = characterSequence.startIndex;
        final char[] characters = characterSequence.characters;
        for (int i = 0; i < length; i++) {
          if (this.characters[this.startIndex + i] != characters[startIndex + i])
            return false;
        }
        return true;
      }
    }
    else
      return false;
  }
  
  public String makeString() {
    return new String(characters, startIndex, length);
  }

}
