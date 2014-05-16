using System;

namespace Nagasena.Schema {

  public sealed class Characters {

    public bool isVolatile;

    public char[] characters;
    public int startIndex;
    public readonly int length;
    public readonly int ucsCount;
    private readonly int m_hashCode;

    public static readonly Characters CHARACTERS_EMPTY;
    static Characters() {
      CHARACTERS_EMPTY = new Characters("".ToCharArray(), 0, 0, false);
    }

    public Characters(char[] characters, int startIndex, int length, bool isVolatile) {
      this.isVolatile = isVolatile;
      this.characters = characters;
      this.startIndex = startIndex;
      this.length = length;
      int hashCode = 0;
      int limit = this.startIndex + this.length;
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
        char[] _characters = new char[length];
        Array.Copy(characters, startIndex, _characters, 0, length);
        characters = _characters;
        startIndex = 0;
        isVolatile = false;
      }
      return;
    }

    public int IndexOf(char c) {
      int limit = startIndex + length;
      for (int i = startIndex; i < limit; i++) {
        if (characters[i] == c) {
          return i;
        }
      }
      return -1;
    }

    public string Substring(int beginIndex, int endIndex) {
      return new string(characters, beginIndex, endIndex - beginIndex);
    }

    public override int GetHashCode() {
      return m_hashCode;
    }

    public override bool Equals(object @object) {
      if (@object is Characters) {
        Characters characterSequence = (Characters)@object;
        if (length != characterSequence.length) {
          return false;
        }
        else {
          int startIndex = characterSequence.startIndex;
          char[] characters = characterSequence.characters;
          for (int i = 0; i < length; i++) {
            if (this.characters[this.startIndex + i] != characters[startIndex + i]) {
              return false;
            }
          }
          return true;
        }
      }
      else {
        return false;
      }
    }

    public string makeString() {
      return new string(characters, startIndex, length);
    }

  }

}