package com.sumerogi.proc.io;

import java.io.IOException;
import java.io.OutputStream;

abstract class ValueScriberBase extends ValueScriber {
  
  protected ValueScriberBase() {
  }
  
  protected int startPosition;
  protected int limitPosition;

  @Override
  public final void scribe(String value, Scribble scribble, int localName, Scriber scriber) throws IOException {
    scribe(value, scribble, localName, (OutputStream)null, scriber);
  }

  /**
   * Trims leading and trailing whitespace characters.
   * @param value
   * @return false when there was found no non-whitespace characters,
   * otherwise returns true.
   */
  protected final boolean trimWhitespaces(char[] value, int offset, int length) {
    
    int pos, len;
    int limit = offset + length;

    skipWhiteSpaces:
    for (pos = offset; pos < limit; pos++) {
      switch (value[pos]) {
        case '\t':
        case '\n':
        case '\r':
        case ' ':
          break;
        default:
          break skipWhiteSpaces;
      }
    }
    if ((len = limit - pos) > 1) {
      int lastPos = limit - 1;
      skipTrailingWhiteSpaces:
      for (; lastPos >= pos; lastPos--) {
        switch (value[lastPos]) {
          case '\t':
          case '\n':
          case '\r':
          case ' ':
            break;
          default:
            break skipTrailingWhiteSpaces;
        }
      }
      if (lastPos < pos)
        return false;
      limit = lastPos + 1;
      len = limit - pos;
    }
    else if (len == 0)
      return false;

    startPosition = pos;
    limitPosition = limit;
    return true;
  }
  
}
