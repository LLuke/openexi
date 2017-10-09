package org.openexi.proc.io;

import java.io.IOException;
import java.io.OutputStream;

import org.openexi.proc.grammars.ValueApparatus;
import org.openexi.schema.EXISchema;

public abstract class ValueScriber extends ValueApparatus {

  public abstract int getBuiltinRCS(int simpleType, Scriber scriber);
  
  ////////////////////////////////////////////////////////////
  
  public abstract boolean process(String value, int tp, EXISchema schema, Scribble scribble, Scriber scriber);
  
  public abstract void scribe(String value, Scribble scribble, int localName, int uri, int tp, Scriber scriber) throws IOException;
  
  /**
   * ScriberValueHolder calls this method to write out an Object to a channelStream.
   */
  public abstract void scribe(String value, Scribble scribble, int localName, int uri, int tp, OutputStream channelStream, Scriber scriber) throws IOException;
  
  ////////////////////////////////////////////////////////////
  
  public abstract Object toValue(String value, Scribble scribble, Scriber scriber);

  public abstract void doScribe(Object value, int localName, int uri, int tp, OutputStream channelStream, Scriber scriber) throws IOException;

  ////////////////////////////////////////////////////////////
  
  public static final String normalize(final String text, final int whiteSpace) {
    assert whiteSpace != EXISchema.WHITESPACE_PRESERVE;
    /* Now we know that whiteSpace is either WHITESPACE_COLLAPSE or
       WHITESPACE_REPLACE. */
    
    int i;
    
    final int len;
    // for performance optimization
    for (i = 0, len = text.length(); i < len && !Character.isWhitespace(text.charAt(i)); i++);
    if (i == len) // no whitespace found.
      return text;

    final StringBuilder buf = new StringBuilder(len);

    if (whiteSpace == EXISchema.WHITESPACE_COLLAPSE) {
      boolean initState = true;
      boolean whiteSpaceDeposited = false;
      for (i = 0; i < len; i++) {
        final char ch;
        switch (ch = text.charAt(i)) {
          case '\t':
          case '\n':
          case '\r':
          case ' ':
            if (!initState) {
              whiteSpaceDeposited = true;
            }
            break;
          default:
            if (initState) {
              assert !whiteSpaceDeposited;
              initState = false;
            }
            else if (whiteSpaceDeposited) {
              buf.append(' ');
              whiteSpaceDeposited = false;
            }
            buf.append(ch);
            break;
        }
      }
    }
    else {
      assert whiteSpace == EXISchema.WHITESPACE_REPLACE;
      for (i = 0; i < len; i++) {
        final char ch;
        switch (ch = text.charAt(i)) {
          case '\t':
          case '\n':
          case '\r':
          case ' ':
            buf.append(' '); // replace
            break;
          default:
            buf.append(ch);
            break;
        }
      }
    }
    return buf.toString();
  }

}
