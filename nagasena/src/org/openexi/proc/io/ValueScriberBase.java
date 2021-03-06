package org.openexi.proc.io;

import java.io.IOException;
import java.io.OutputStream;

import org.openexi.proc.common.QName;
import org.openexi.schema.EXISchema;

abstract class ValueScriberBase extends ValueScriber {

  protected static final int DECIMAL_MODE_MAYBE_SIGN     = 0;
  protected static final int DECIMAL_MODE_MAYBE_INTEGRAL = 1;
  protected static final int DECIMAL_MODE_IS_INTEGRAL    = 2;
  protected static final int DECIMAL_MODE_IS_FRACTION    = 3;
  protected static final int DECIMAL_MODE_MAYBE_TRAILING_ZEROS = 4;

  private final QName m_name;

  protected ValueScriberBase(QName name) {
    m_name = name;
  }
  
  @Override
  public final QName getName() {
    return m_name;
  }
  
  @Override
  public int getBuiltinRCS(int simpleType, Scriber scriber) {
    return EXISchema.NIL_NODE;
  }

  protected static final int[] NBIT_INTEGER_RANGES = { 0, 1, 3, 7, 15, 31, 63, 127, 255, 511, 1023, 2047, 4095 };

  protected int startPosition;
  protected int limitPosition;

  @Override
  public final void scribe(String value, Scribble scribble, int localName, int uri, int tp, Scriber scriber) throws IOException {
    scribe(value, scribble, localName, uri, tp, (OutputStream)null, scriber);
  }

  /**
   * Trims leading and trailing whitespace characters.
   * @param value
   * @return false when there was found no non-whitespace characters,
   * otherwise returns true.
   */
  protected final boolean trimWhitespaces(String value) {
    
    int pos, len;
    int limit = value.length();

    skipWhiteSpaces:
    for (pos = 0; pos < limit; pos++) {
      switch (value.charAt(pos)) {
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
        switch (value.charAt(lastPos)) {
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
