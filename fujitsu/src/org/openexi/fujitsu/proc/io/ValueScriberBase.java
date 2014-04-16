package org.openexi.fujitsu.proc.io;

import java.io.IOException;
import java.io.OutputStream;

import org.openexi.fujitsu.proc.common.QName;
import org.openexi.fujitsu.schema.EXISchema;

abstract class ValueScriberBase extends ValueScriber {
  
  private final QName m_name;

  protected ValueScriberBase(Scriber scriber, QName name) {
    super(scriber);
    m_name = name;
  }
  
  @Override
  public final QName getName() {
    return m_name;
  }
  
  @Override
  public int getBuiltinRCS(int simpleType) {
    return EXISchema.NIL_NODE;
  }

  protected static final int[] NBIT_INTEGER_RANGES = { 0, 1, 3, 7, 15, 31, 63, 127, 255, 511, 1023, 2047, 4095 };

  protected int startPosition;
  protected int limitPosition;

  @Override
  public final void scribe(String value, Scribble scribble, String localName, String uri, int tp) throws IOException {
    scribe(value, scribble, localName, uri, tp, (OutputStream)null);
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
