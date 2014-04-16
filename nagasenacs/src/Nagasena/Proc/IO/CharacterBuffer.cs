using System;

using Characters = Nagasena.Schema.Characters;

namespace Nagasena.Proc.IO {

  public sealed class CharacterBuffer {

    public const int BUFSIZE_DEFAULT = 4096;
    //public static final int BUFSIZE_INCREMENT = 4096;

    /// <summary>
    /// @y.exclude </summary>
    public readonly bool isVolatile;

    /// <summary>
    /// @y.exclude </summary>
    public char[] characters;
    /// <summary>
    /// @y.exclude </summary>
    public int bufSize;
    /// <summary>
    /// @y.exclude </summary>
    public int nextIndex;

    public CharacterBuffer(int bufSize) : this(bufSize, true) {
    }

    /// <summary>
    /// @y.exclude </summary>
    public CharacterBuffer(bool isVolatile) : this(BUFSIZE_DEFAULT, isVolatile) {
    }

    /// <summary>
    /// @y.exclude </summary>
    public CharacterBuffer(int bufSize, bool isVolatile) {
      this.isVolatile = isVolatile;
      characters = new char[bufSize];
      this.bufSize = bufSize;
      nextIndex = 0;
    }

    /// <summary>
    /// @y.exclude </summary>
    public int availability() {
      return bufSize - nextIndex;
    }

    /// <summary>
    /// @y.exclude </summary>
    public void ensureCharacters(int length) {
      int shortage;
      if ((shortage = length - availability()) > 0) {
        int halfBufSize = bufSize >> 1;
        int expansion;
        if (shortage < halfBufSize) {
          expansion = halfBufSize;
        }
        else {
          if (shortage < bufSize) {
            expansion = bufSize;
          }
          else {
            expansion = shortage + halfBufSize;
          }
        }
        int newBufSize = bufSize + expansion;
        char[] _characters = new char[newBufSize];
        Array.Copy(characters, 0, _characters, 0, nextIndex);
        characters = _characters;
        bufSize = newBufSize;
      }
    }

    /// <summary>
    /// @y.exclude </summary>
    public int allocCharacters(int length) {
      int _nextIndex;
      if ((_nextIndex = nextIndex + length) > bufSize) {
        return -1;
      }
      int index = nextIndex;
      nextIndex = _nextIndex;
      return index;
    }

    /// <summary>
    /// @y.exclude </summary>
    public void redeemCharacters(int length) {
      nextIndex -= length;
    }

    /// <summary>
    /// @y.exclude </summary>
    public Characters addChars(char[] chars, int length) {
      int index = nextIndex;
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

    /// <summary>
    /// @y.exclude </summary>
    public Characters addCharsReverse(char[] chars, int length) {
      int index = nextIndex;
      int pos = index;
      int lastSourceIndex = length - 1;
      int i;
      for (i = 0; i < length && pos < bufSize; i++, pos++) {
        characters[pos] = chars[lastSourceIndex - i];
      }
      if (i != length) {
        return null;
      }
      nextIndex = pos;
      return new Characters(characters, index, length, isVolatile);
    }

    /// <summary>
    /// @y.exclude </summary>
    public Characters addDecimalChars(char[] integralDigitsChars, int n_integralDigits, char[] fractionalDigitsChars, int n_fractionDigits, int totalLength) {
      int index = nextIndex;
      int pos = index;
      int lastSourceIndex = n_integralDigits - 1;
      for (int i = 0; i < n_integralDigits; i++, pos++) {
        characters[pos] = integralDigitsChars[lastSourceIndex - i];
      }
      characters[pos++] = '.';
      for (int i = 0; i < n_fractionDigits; i++, pos++) {
        characters[pos] = fractionalDigitsChars[i];
      }
      nextIndex = pos;
      return new Characters(characters, index, totalLength, isVolatile);
    }

    /// <summary>
    /// @y.exclude </summary>
    public Characters addString(string stringValue, int length) {
      int index = nextIndex;
      int pos = index;
      int i;
      for (i = 0; i < length && pos < bufSize; i++, pos++) {
        characters[pos] = stringValue[i];
      }
      if (i != length) {
        return null;
      }
      nextIndex = pos;
      return new Characters(characters, index, length, isVolatile);
    }

  }

}