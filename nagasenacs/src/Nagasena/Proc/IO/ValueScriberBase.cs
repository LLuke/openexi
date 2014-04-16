using System.IO;

using QName = Nagasena.Proc.Common.QName;
using EXISchema = Nagasena.Schema.EXISchema;

namespace Nagasena.Proc.IO {

  internal abstract class ValueScriberBase : ValueScriber {

    private readonly QName m_name;

    protected internal ValueScriberBase(QName name) {
      m_name = name;
    }

    public override QName Name {
      get {
        return m_name;
      }
    }

    public override int getBuiltinRCS(int simpleType, Scriber scriber) {
      return EXISchema.NIL_NODE;
    }

    protected internal static readonly int[] NBIT_INTEGER_RANGES = new int[] { 0, 1, 3, 7, 15, 31, 63, 127, 255, 511, 1023, 2047, 4095 };

    protected internal int startPosition;
    protected internal int limitPosition;

    public override sealed void scribe(string value, Scribble scribble, int localName, int uri, int tp, Scriber scriber) {
      scribe(value, scribble, localName, uri, tp, (Stream)null, scriber);
    }

    /// <summary>
    /// Trims leading and trailing whitespace characters. </summary>
    /// <param name="value"> </param>
    /// <returns> false when there was found no non-whitespace characters,
    /// otherwise returns true. </returns>
    protected internal bool trimWhitespaces(string value) {

      int pos, len;
      int limit = value.Length;

      for (pos = 0; pos < limit; pos++) {
        switch (value[pos]) {
          case '\t':
          case '\n':
          case '\r':
          case ' ':
            break;
          default:
            goto skipWhiteSpacesBreak;
        }
      }
      skipWhiteSpacesBreak:
      if ((len = limit - pos) > 1) {
        int lastPos = limit - 1;
        for (; lastPos >= pos; lastPos--) {
          switch (value[lastPos]) {
            case '\t':
            case '\n':
            case '\r':
            case ' ':
              break;
            default:
              goto skipTrailingWhiteSpacesBreak;
          }
        }
        skipTrailingWhiteSpacesBreak:
        if (lastPos < pos) {
          return false;
        }
        limit = lastPos + 1;
        len = limit - pos;
      }
      else if (len == 0) {
        return false;
      }

      startPosition = pos;
      limitPosition = limit;
      return true;
    }

  }

}