using System;
using System.Diagnostics;
using System.IO;
using System.Text;

using QName = Nagasena.Proc.Common.QName;
using ExiUriConst = Nagasena.Proc.Util.ExiUriConst;
using EXISchema = Nagasena.Schema.EXISchema;

namespace Nagasena.Proc.IO {

  internal sealed class FloatValueScriber : ValueScriberBase {

    public static readonly FloatValueScriber instance;
    static FloatValueScriber() {
      instance = new FloatValueScriber();
    }

    private FloatValueScriber() : base(new QName("exi:double", ExiUriConst.W3C_2009_EXI_URI)) {
    }

    public override short CodecID {
      get {
        return Scriber.CODEC_DOUBLE;
      }
    }

    public override int getBuiltinRCS(int simpleType, Scriber scriber) {
      return BuiltinRCS.RCS_ID_DOUBLE;
    }

    ////////////////////////////////////////////////////////////

    public override bool process(string value, int tp, EXISchema schema, Scribble scribble, Scriber scriber) {
      return doProcess(value, scribble, scriber.stringBuilder1);
    }

    public override void scribe(string value, Scribble scribble, int localName, int uri, int tp, Stream channelStream, Scriber scriber) {
      scribeFloatValue(scribble.longValue, scribble.intValue1, channelStream, scriber);
    }

    public override object toValue(string value, Scribble scribble, Scriber scriber) {
      return new FloatValue(scribble.longValue, scribble.intValue1);
    }

    public override void doScribe(object value, int localName, int uri, int tp, Stream channelStream, Scriber scriber) {
      FloatValue floatValue = (FloatValue)value;
      scribeFloatValue(floatValue.mantissa, floatValue.exponent, channelStream, scriber);
    }

    ////////////////////////////////////////////////////////////

    /// <summary>
    /// Extract mantissa value from a scribble.
    /// </summary>
    public static long getMantissa(Scribble scribble) {
      return scribble.longValue;
    }

    /// <summary>
    /// Extract exponent value from a scribble.
    /// </summary>
    public static int getExponent(Scribble scribble) {
      return scribble.intValue1;
    }

    /// <summary>
    /// Convert (mantissa, exponent) value pair into canonical form.
    /// </summary>
    public static void canonicalizeValue(Scribble scribble) {
      long mantissa = scribble.longValue;
      int exponent = scribble.intValue1;
      if (exponent == -16384 && mantissa != 1L && mantissa != -1L) {
        mantissa = 0L; // NaN
      }
      else {
        if (mantissa != 0L) {
          while (mantissa % 10L == 0) {
            mantissa /= 10L;
            exponent += 1;
          }
        }
        else {
          exponent = 0;
        }
      }
      scribble.longValue = mantissa;
      scribble.intValue1 = exponent;
    }

    ////////////////////////////////////////////////////////////

    public bool doProcess(string value, Scribble scribble, StringBuilder integralDigits) {
      if (!trimWhitespaces(value)) {
        return false;
      }

      int n_digits = 0; // for detecting zero-length decimal
      int totalDigits = 0, fractionDigits = 0;
      int trailingZeros = 0;

      int len = limitPosition - startPosition;
      switch (value[limitPosition - 1]) {
        case 'F':
          if (len == 3 && value[startPosition] == 'I' && value[startPosition + 1] == 'N') {
            // mantissa: 1 
            scribble.longValue = 1;
            // exponent: -16384
            scribble.intValue1 = -16384;
            return true;
          }
          else if (len == 4 && value[startPosition] == '-' && value[startPosition + 1] == 'I' && value[startPosition + 2] == 'N') {
            // mantissa: -1 
            scribble.longValue = -1;
            // exponent: -16384
            scribble.intValue1 = -16384;
            return true;
          }
          return false;
        case 'N':
          if (len == 3 && value[startPosition] == 'N' && value[startPosition + 1] == 'a') {
            // mantissa: 0
            scribble.longValue = 0;
            // exponent: -16384
            scribble.intValue1 = -16384;
            return true;
          }
          return false;
        default:
          break;
      }

      integralDigits.Length = 0;
      bool positive = true;
      int pos, mode;
      for (pos = startPosition, mode = DECIMAL_MODE_MAYBE_SIGN; pos < limitPosition; pos++) {
        char c = value[pos];
        switch (mode) {
          case DECIMAL_MODE_MAYBE_SIGN:
            if (c == '-' || c == '+') {
              mode = DECIMAL_MODE_MAYBE_INTEGRAL;
              if (c != '+') {
                positive = false;
              }
            }
            else if (c == '.') {
              mode = DECIMAL_MODE_IS_FRACTION;
            }
            else if (c >= '0' && c <= '9') {
              mode = DECIMAL_MODE_IS_INTEGRAL;
              ++n_digits;
              if (c != '0') {
                integralDigits.Append(c);
                ++totalDigits;
              }
            }
            else {
              return false;
            }
            break;
          case DECIMAL_MODE_MAYBE_INTEGRAL:
          case DECIMAL_MODE_IS_INTEGRAL:
            if (c == '.') {
              mode = DECIMAL_MODE_IS_FRACTION;
            }
            else if (c >= '0' && c <= '9') {
              mode = DECIMAL_MODE_IS_INTEGRAL;
              ++n_digits;
              if (totalDigits > 0 || c != '0') {
                integralDigits.Append(c);
                ++totalDigits;
              }
            }
            else if (mode == DECIMAL_MODE_IS_INTEGRAL && (c == 'e' || c == 'E')) {
              goto parseFloatBreak;
            }
            else {
              return false;
            }
            break;
          case DECIMAL_MODE_IS_FRACTION:
            if (c == '0') {
              ++trailingZeros;
              mode = DECIMAL_MODE_MAYBE_TRAILING_ZEROS;
            }
            else {
              if (c >= '1' && c <= '9') {
                integralDigits.Append(c);
                ++n_digits;
                ++fractionDigits;
              }
              else if (c == 'e' || c == 'E') {
                goto parseFloatBreak;
              }
              else {
                return false;
              }
            }
            break;
          case DECIMAL_MODE_MAYBE_TRAILING_ZEROS:
            Debug.Assert(trailingZeros > 0);
            if (c == '0') {
              ++trailingZeros;
            }
            else {
              if (c >= '1' && c <= '9') {
                n_digits += trailingZeros;
                fractionDigits += trailingZeros;
                for (int i = 0; i < trailingZeros; i++) {
                  integralDigits.Append('0');
                }
                integralDigits.Append(c);
                ++n_digits;
                ++fractionDigits;
                trailingZeros = 0;
                mode = DECIMAL_MODE_IS_FRACTION;
              }
              else if (c == 'e' || c == 'E') {
                n_digits += trailingZeros;
                fractionDigits += trailingZeros;
                for (int i = 0; i < trailingZeros; i++) {
                  integralDigits.Append('0');
                }
                trailingZeros = 0;
                goto parseFloatBreak;
              }
              else {
                return false;
              }
            }
            break;
        }
      }
      parseFloatBreak:

      n_digits += trailingZeros;
      if (mode == DECIMAL_MODE_MAYBE_TRAILING_ZEROS) {
        mode = DECIMAL_MODE_IS_FRACTION;
      }
      if (mode == DECIMAL_MODE_IS_FRACTION && fractionDigits == 0) {
        // Assume there is at least one trailing fraction digits
        // when decimal point has been encountered.
        if (trailingZeros == 0) {
          trailingZeros = 1;
        }
      }
      totalDigits += fractionDigits;

      if (mode != DECIMAL_MODE_IS_INTEGRAL && mode != DECIMAL_MODE_IS_FRACTION || n_digits == 0) {
        return false;
      }

      if (integralDigits.Length == 0) {
        integralDigits.Append('0');
      }

      string digitsString;
      int n_integralDigits;

      digitsString = integralDigits.ToString(/**/);
      n_integralDigits = integralDigits.Length;
      long mantissa;
      if (n_integralDigits > 19) {
        return false;
      }
      else if (n_integralDigits == 19) {
        int cres;
        if (positive) {
          cres = "9223372036854775807".CompareTo(digitsString);
          if (cres == 0) {
            mantissa = long.MaxValue;
          }
          else if (cres < 0) {
            return false;
          }
          else {
            mantissa = Convert.ToInt64(digitsString);
          }
        }
        else {
          cres = "9223372036854775808".CompareTo(digitsString);
          if (cres == 0) {
            mantissa = long.MinValue;
          }
          else if (cres < 0) {
            return false;
          }
          else {
            mantissa = - Convert.ToInt64(digitsString);
          }
        }
      }
      else {
        mantissa = positive ? Convert.ToInt64(digitsString) : -Convert.ToInt64(digitsString);
      }

      integralDigits.Length = 0;

      positive = true;
      if (pos < limitPosition) {
        char c = value[pos++];
        if (c == 'e' || c == 'E') {
          if (pos < limitPosition) {
            c = value[pos];
            if (c == '-' || c == '+') {
              ++pos;
              if (c != '+') {
                positive = false;
              }
            }
            bool foundNonZero = false;
            for (; pos < limitPosition; pos++) {
              switch (c = value[pos]) {
                case '0':
                  if (!foundNonZero) {
                    continue;
                  }
                  else {
                    integralDigits.Append(c);
                  }
                  break;
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                  if (!foundNonZero) {
                    foundNonZero = true;
                  }
                  integralDigits.Append(c);
                  break;
                default:
                  return false;
              }
            }
          }
        }
      }

      if (integralDigits.Length == 0) {
        integralDigits.Append('0');
      }

      digitsString = integralDigits.ToString(/**/);
      int exponent;
      try {
        exponent = Convert.ToInt32(digitsString);
      }
      catch (FormatException) {
        return false;
      }
      catch (OverflowException) {
        return false;
      }
      if (positive) {
        exponent -= fractionDigits;
      }
      else {
        exponent = -(exponent + fractionDigits);
      }
      // range of valid exponent: [-(2^14 - 1) ... 2^14 - 1]
      if (exponent < -16383 || 16383 < exponent) {
        return false;
      }

      scribble.longValue = mantissa;
      scribble.intValue1 = exponent;

      return true;
    }

    private void scribeFloatValue(long mantissa, int exponent, Stream ostream, Scriber scriber) {
      bool isNegative;
      if (isNegative = (((ulong)mantissa) & 0x8000000000000000L) != 0) {
        mantissa = ~mantissa; // same as -(mantissa + 1)
      }
      scriber.writeBoolean(isNegative, ostream);
      scriber.writeUnsignedInteger64(mantissa, ostream);
      if (isNegative = (exponent & 0x80000000) != 0) {
        exponent = ~exponent; // same as -(exponent + 1)
      }
      scriber.writeBoolean(isNegative, ostream);
      scriber.writeUnsignedInteger32(exponent, ostream);
    }

    ////////////////////////////////////////////////////////////

    private sealed class FloatValue {
      internal readonly long mantissa;
      internal readonly int exponent;
      internal FloatValue(long mantissa, int exponent) {
        this.mantissa = mantissa;
        this.exponent = exponent;
      }
    }

  }

}