using System;
using System.Diagnostics;
using System.Globalization;
using System.IO;
using System.Numerics;
using System.Text;

using QName = Nagasena.Proc.Common.QName;
using ExiUriConst = Nagasena.Proc.Util.ExiUriConst;
using EXISchema = Nagasena.Schema.EXISchema;

namespace Nagasena.Proc.IO {

  internal sealed class DecimalValueScriber : ValueScriberBase {

    public static readonly DecimalValueScriber instance;
    static DecimalValueScriber() {
      instance = new DecimalValueScriber();
    }

    private const int DECIMAL_MODE_MAYBE_SIGN = 0;
    private const int DECIMAL_MODE_MAYBE_INTEGRAL = 1;
    private const int DECIMAL_MODE_IS_INTEGRAL = 2;
    private const int DECIMAL_MODE_IS_FRACTION = 3;
    private const int DECIMAL_MODE_MAYBE_TRAILING_ZEROS = 4;

    private DecimalValueScriber() : base(new QName("exi:decimal", ExiUriConst.W3C_2009_EXI_URI)) {
    }

    public override short CodecID {
      get {
        return Scriber.CODEC_DECIMAL;
      }
    }

    public override int getBuiltinRCS(int simpleType, Scriber scriber) {
      return BuiltinRCS.RCS_ID_DECIMAL;
    }

    ////////////////////////////////////////////////////////////

    public override bool process(string value, int tp, EXISchema schema, Scribble scribble, Scriber scriber) {
      return doProcess(value, scribble, scriber.stringBuilder1, scriber.stringBuilder2);
    }

    public override void scribe(string value, Scribble scribble, int localName, int uri, int tp, Stream channelStream, Scriber scriber) {
      scribeDecimalValue(scribble.stringValue1, scribble.stringValue2, scribble.booleanValue1, channelStream, scriber);
    }

    private class DecimalValue {
      internal string integralDigits;
      internal string fractionalDigits;
      internal bool isNegative;
      internal DecimalValue(string integralDigits, string fractionalDigits, bool isNegative) {
        this.integralDigits = integralDigits;
        this.fractionalDigits = fractionalDigits;
        this.isNegative = isNegative;
      }
    }

    public override object toValue(string value, Scribble scribble, Scriber scriber) {
      return new DecimalValue(scribble.stringValue1, scribble.stringValue2, scribble.booleanValue1);
    }

    public override void doScribe(object value, int localName, int uri, int tp, Stream channelStream, Scriber scriber) {
      DecimalValue decimalValue = (DecimalValue)value;
      scribeDecimalValue(decimalValue.integralDigits, decimalValue.fractionalDigits, decimalValue.isNegative, channelStream, scriber);
    }

    ////////////////////////////////////////////////////////////

    /// <summary>
    /// Extract sign value from a scribble.
    /// </summary>
    public static bool getSign(Scribble scribble) {
      return scribble.booleanValue1;
    }

    /// <summary>
    /// Extract integral digits from a scribble.
    /// </summary>
    public static string getIntegralDigits(Scribble scribble) {
      return scribble.stringValue1;
    }

    /// <summary>
    /// Extract reverse-fractional digits from a scribble.
    /// </summary>
    public static string getReverseFractionalDigits(Scribble scribble) {
      return scribble.stringValue2;
    }

    /// <summary>
    /// Convert -0.0 to + 0.0.
    /// </summary>
    public static void canonicalizeValue(Scribble scribble) {
      bool sign = scribble.booleanValue1;
      string integralDigits = scribble.stringValue1;
      string reverseFractionalDigits = scribble.stringValue2;
      if (sign && "0".Equals(integralDigits) && "0".Equals(reverseFractionalDigits)) {
        scribble.booleanValue1 = false;
      }
    }

    public bool doProcess(string value, Scribble scribble, StringBuilder integralDigits, StringBuilder reverseFractionalDigits) {

      if (!trimWhitespaces(value)) {
        return false;
      }

      int digits = 0; // for detecting zero-length decimal
      int totalDigits = 0, fractionDigits = 0;
      int trailingZeros = 0;

      integralDigits.Length = 0;
      reverseFractionalDigits.Length = 0;

      bool positive = true;
      bool syntaxInvalid = false;
      int pos, mode;
      for (pos = startPosition, mode = DECIMAL_MODE_MAYBE_SIGN; pos < limitPosition && !syntaxInvalid; pos++) {
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
              ++digits;
              if (c != '0') {
                integralDigits.Append(c);
                ++totalDigits;
              }
            }
            else {
              syntaxInvalid = true;
            }
            break;
          case DECIMAL_MODE_MAYBE_INTEGRAL:
          case DECIMAL_MODE_IS_INTEGRAL:
            if (c == '.') {
              mode = DECIMAL_MODE_IS_FRACTION;
            }
            else if (c >= '0' && c <= '9') {
              mode = DECIMAL_MODE_IS_INTEGRAL;
              ++digits;
              if (totalDigits > 0 || c != '0') {
                integralDigits.Append(c);
                ++totalDigits;
              }
            }
            else {
              syntaxInvalid = true;
            }
            break;
          case DECIMAL_MODE_IS_FRACTION:
            if (c == '0') {
              ++trailingZeros;
              mode = DECIMAL_MODE_MAYBE_TRAILING_ZEROS;
            }
            else {
              if (c >= '1' && c <= '9') {
                reverseFractionalDigits.Append(c);
                ++digits;
                ++fractionDigits;
              }
              else {
                syntaxInvalid = true;
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
                digits += trailingZeros;
                fractionDigits += trailingZeros;
                for (int i = 0; i < trailingZeros; i++) {
                  reverseFractionalDigits.Append('0');
                }
                reverseFractionalDigits.Append(c);
                ++digits;
                ++fractionDigits;
                trailingZeros = 0;
                mode = DECIMAL_MODE_IS_FRACTION;
              }
              else {
                syntaxInvalid = true;
              }
            }
            break;
        }
      }
      if (!syntaxInvalid) {
        digits += trailingZeros;
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
      }

      if (syntaxInvalid || mode != DECIMAL_MODE_IS_INTEGRAL && mode != DECIMAL_MODE_IS_FRACTION || digits == 0) {
        return false;
      }

      if (integralDigits.Length == 0) {
        integralDigits.Append('0');
      }
      if (reverseFractionalDigits.Length == 0) {
        reverseFractionalDigits.Append('0');
      }
      else {
        char[] charArray = reverseFractionalDigits.ToString().ToCharArray();
        Array.Reverse(charArray);
        reverseFractionalDigits.Length = 0;
        reverseFractionalDigits.Append(charArray);
      }

      scribble.booleanValue1 = !positive;
      scribble.stringValue1 = integralDigits.ToString();
      scribble.stringValue2 = reverseFractionalDigits.ToString();

      return true;
    }

    private void scribeDecimalValue(string integralDigits, string fractionalDigits, bool isNegative, Stream ostream, Scriber scriber) {

      scriber.writeBoolean(isNegative, ostream);

      int n_integralDigits = integralDigits.Length;
      if (n_integralDigits < 10 || n_integralDigits == 10 && "2147483647".CompareTo(integralDigits) >= 0) {
        int integralDigitsIntValue = Convert.ToInt32(integralDigits);
        scriber.writeUnsignedInteger32(integralDigitsIntValue, ostream);
      }
      else {
        BigInteger integralDigitsIntegerValue = BigInteger.Parse(integralDigits, NumberFormatInfo.InvariantInfo);
        scriber.writeUnsignedInteger(integralDigitsIntegerValue, ostream);
      }

      int n_fractionalDigits = fractionalDigits.Length;
      if (n_fractionalDigits < 10 || n_fractionalDigits == 10 && "2147483647".CompareTo(fractionalDigits) >= 0) {
        int fractionalDigitsIntValue = Convert.ToInt32(fractionalDigits);
        scriber.writeUnsignedInteger32(fractionalDigitsIntValue, ostream);
      }
      else {
        System.Numerics.BigInteger fractionalDigitsIntegerValue = BigInteger.Parse(fractionalDigits, NumberFormatInfo.InvariantInfo);
        scriber.writeUnsignedInteger(fractionalDigitsIntegerValue, ostream);
      }
    }

  }

}