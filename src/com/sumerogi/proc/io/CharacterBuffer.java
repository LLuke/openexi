package com.sumerogi.proc.io;

import com.sumerogi.schema.Characters;

public final class CharacterBuffer {

  public static final int BUFSIZE_DEFAULT = 4096;
  //public static final int BUFSIZE_INCREMENT = 4096;

  /** @y.exclude */
  public final boolean isVolatile;
  
  /** @y.exclude */
  public char[] characters;
  /** @y.exclude */
  public int bufSize; 
  /** @y.exclude */
  public int nextIndex;
  
  public CharacterBuffer(int bufSize) {
    this(bufSize, true);
  }

  /** @y.exclude */
  public CharacterBuffer(boolean isVolatile) {
    this(BUFSIZE_DEFAULT, isVolatile);
  }

  /** @y.exclude */
  public CharacterBuffer(int bufSize, boolean isVolatile) {
  	this.isVolatile = isVolatile;
    characters = new char[bufSize];
    this.bufSize = bufSize;
    nextIndex = 0;
  }
  
  /** @y.exclude */
  public int availability() {
    return bufSize - nextIndex;
  }
  
  /** @y.exclude */
  public void ensureCharacters(final int length) {
    final int shortage;
    if ((shortage = length - availability()) > 0) {
      final int halfBufSize = bufSize >> 1;
      final int expansion;
      if (shortage < halfBufSize) {
        expansion = halfBufSize;
      }
      else {
        if (shortage < bufSize)
          expansion = bufSize;
        else
          expansion = shortage + halfBufSize;
      }
      final int newBufSize = bufSize + expansion;
      final char[] _characters = new char[newBufSize];
      System.arraycopy(characters, 0, _characters, 0, nextIndex);
      characters = _characters;
      bufSize = newBufSize;
    }
  }
  
  /** @y.exclude */
  public int allocCharacters(int length) {
    final int _nextIndex;
    if ((_nextIndex = nextIndex + length) > bufSize) {
      return -1;
    }
    final int index = nextIndex;
    nextIndex = _nextIndex;
    return index;
  }
  
  /** @y.exclude */
  public void redeemCharacters(int length) {
    nextIndex -= length;
  }

  /** @y.exclude */
  public Characters addChars(char[] chars, int length) {
    final int index = nextIndex;
    int pos = index;
    int i;
    for (i = 0; i < length && pos < bufSize; i++, pos++) {
      characters[pos] = chars[i];
    }
    if (i != length) {
      return null;
    }
    nextIndex = pos;
    return new Characters(characters, index, length, isVolatile);
  }

  /** @y.exclude */
  public Characters addCharsReverse(char[] chars, int length) {
    final int index = nextIndex;
    int pos = index;
    final int lastSourceIndex = length - 1;
    int i;
    for (i = 0; i < length && pos < bufSize; i++, pos++)
      characters[pos] = chars[lastSourceIndex - i]; 
    if (i != length)
      return null;
    nextIndex = pos;
    return new Characters(characters, index, length, isVolatile);
  }
  
  /** @y.exclude */
  public Characters addDecimalChars(char[] integralDigitsChars, int n_integralDigits, char[] fractionalDigitsChars, int n_fractionDigits, int totalLength) {
    final int index = nextIndex;
    int pos = index;
    final int lastSourceIndex = n_integralDigits - 1;
    for (int i = 0; i < n_integralDigits; i++, pos++)
      characters[pos] = integralDigitsChars[lastSourceIndex - i]; 
    characters[pos++] = '.';
    for (int i = 0; i < n_fractionDigits; i++, pos++)
      characters[pos] = fractionalDigitsChars[i];
    nextIndex = pos;
    return new Characters(characters, index, totalLength, isVolatile);
  }
  
  /** @y.exclude */
  public Characters addString(String stringValue, int length) {
    final int index = nextIndex;
    int pos = index;
    int i;
    for (i = 0; i < length && pos < bufSize; i++, pos++) {
      characters[pos] = stringValue.charAt(i);
    }
    if (i != length) {
      return null;
    }
    nextIndex = pos;
    return new Characters(characters, index, length, isVolatile);
  }

}
