package com.sumerogi.proc.io;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;

public final class NumberValueScriber extends ValueScriberBase {
  
//  private static final int INTEGER_VALUE = 0;
//  private static final int FLOAT_VALUE = 1;
//  private static final int DECIMAL_VALUE = 2;

  private static final int DECIMAL_MODE_MAYBE_SIGN     = 0;
  private static final int DECIMAL_MODE_MAYBE_INTEGRAL = 1;
  private static final int DECIMAL_MODE_IS_INTEGRAL    = 2;
  private static final int DECIMAL_MODE_IS_FRACTION    = 3;
  private static final int DECIMAL_MODE_MAYBE_TRAILING_ZEROS = 4;

  public static final NumberValueScriber instance;
  static {
    instance = new NumberValueScriber();
  }
  
  private NumberValueScriber() {
  }

  @Override
  public boolean process(char[] value, int offset, int length, Scribble scribble, Scriber scriber) {
    switch (scribble.intValue2) {
      case INTEGER_VALUE:
        return processInteger(value, offset, length, scribble, scriber);
      case FLOAT_VALUE:
        return doProcessFloat(value, offset, length, scribble, scriber.stringBuilder1);
      case DECIMAL_VALUE:
        return doProcessDecimal(value, offset, length, scribble, scriber.stringBuilder1, scriber.stringBuilder2);
      default:
        assert false;
        return false;
    }
  }

  public boolean processInteger(char[] value, int offset, int length, Scribble scribble, Scriber scriber) {
    if (!trimWhitespaces(value, offset, length))
      return false;
    int pos = startPosition;
    boolean useIntValue = true;
    int intValue = 0;
    long longValue = 0;
    boolean isNegative = false;
    char c = value[pos];
    if (c == '-' || c == '+') {
      ++pos;
      if (c != '+')
        isNegative = true;
    }
    boolean foundNonZero = false;
    int st = -1;
    posLoop:
    for (int n_digits = 0; pos < limitPosition; pos++) {
      switch (c = value[pos]) {
        case '0':
          if (!foundNonZero)
            continue;
          else {
            if (++n_digits < 10)
              intValue *= 10;
            else {
              if (n_digits == 10) {
                longValue = 10 * (long)intValue;
                useIntValue = false;
              }
              else {
                if (n_digits < 19)
                  longValue *= 10;
                else {
                  longValue = Long.MIN_VALUE;
                  ++pos;
                  break posLoop;
                }
              }
            }
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
            st = pos;
          }
          final int deposit = ((int)c) - 48;
          if (++n_digits < 10)
            intValue = 10 * intValue + deposit;
          else {
            if (n_digits == 10) {
              longValue = 10 * (long)intValue + deposit;
              useIntValue = false;
            }
            else {
              if (n_digits < 19)
                longValue = 10 * longValue + deposit;
              else {
                longValue = Long.MIN_VALUE;
                ++pos;
                break posLoop;
              }
            }
          }
          break;
        default:
          return false;
      }
    }
    if (pos != limitPosition) {
      assert longValue == Long.MIN_VALUE;
      for (; pos != limitPosition; pos++) {
        c = value[pos];
        if (c < '0' || '9' < c)
          return false;
      }
    }
    assert (st >= 0 && foundNonZero) || (!foundNonZero && longValue == 0 && intValue == 0 && useIntValue);
    if (!foundNonZero && isNegative) {
      isNegative = false;
    }
    
    scribble.booleanValue1 = isNegative;
    if (scribble.booleanValue2 = useIntValue) {
      scribble.intValue1 = intValue;
    }
    else if ((scribble.longValue = longValue) == Long.MIN_VALUE) {
      scribble.stringValue1 = new String(value, st, limitPosition - st); 
    }
    
    scribble.intValue2 = INTEGER_VALUE;
    return true;
  }

  public boolean doProcessFloat(char[] value, int offset, int length, Scribble scribble, StringBuilder integralDigits) {
    if (!trimWhitespaces(value, offset, length))
      return false;

    int n_digits = 0; // for detecting zero-length decimal
    int totalDigits = 0, fractionDigits = 0;
    int trailingZeros = 0;

    final int len = limitPosition - startPosition;
    switch (value[limitPosition - 1]) {
      case 'F':
        if (len == 3 && value[startPosition] == 'I' && value[startPosition + 1] == 'N') {
          // mantissa: 1 
          scribble.longValue = 1;
          // exponent: -16384
          scribble.intValue1 = -16384;
          
          scribble.intValue2 = FLOAT_VALUE;
          return true;
        }
        else if (len == 4 && value[startPosition] == '-' && value[startPosition + 1] == 'I' && value[startPosition + 2] == 'N') {
          // mantissa: -1 
          scribble.longValue = -1;
          // exponent: -16384
          scribble.intValue1 = -16384;
          
          scribble.intValue2 = FLOAT_VALUE;
          return true;
        }
        return false;
      case 'N':
        if (len == 3 && value[startPosition] == 'N' && value[startPosition + 1] == 'a') {
          // mantissa: 0
          scribble.longValue = 0;
          // exponent: -16384
          scribble.intValue1 = -16384;
          
          scribble.intValue2 = FLOAT_VALUE;
          return true;
        }
        return false;
      default:
        break;
    }
    
    integralDigits.setLength(0);
    boolean positive = true;
    int pos, mode;
    parseFloat:
    for (pos = startPosition, mode = DECIMAL_MODE_MAYBE_SIGN; pos < limitPosition; pos++) {
      final char c = value[pos];
      switch (mode) {
        case DECIMAL_MODE_MAYBE_SIGN:
          if (c == '-' || c == '+') {
            mode = DECIMAL_MODE_MAYBE_INTEGRAL;
            if (c != '+')
              positive = false;
          }
          else if (c == '.') {
            mode = DECIMAL_MODE_IS_FRACTION;
          }
          else if (c >= '0' && c <= '9') {
            mode = DECIMAL_MODE_IS_INTEGRAL;
            ++n_digits;
            if (c != '0') {
              integralDigits.append(c);
              ++totalDigits;
            }
          }
          else
            return false;
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
              integralDigits.append(c);
              ++totalDigits;
            }
          }
          else if (mode == DECIMAL_MODE_IS_INTEGRAL && (c == 'e' || c == 'E'))
            break parseFloat;
          else
            return false;
          break;
        case DECIMAL_MODE_IS_FRACTION:
          if (c == '0') {
            ++trailingZeros;
            mode = DECIMAL_MODE_MAYBE_TRAILING_ZEROS;
          }
          else {
            if (c >= '1' && c <= '9') {
              integralDigits.append(c);
              ++n_digits;
              ++fractionDigits;
            }
            else if (c == 'e' || c == 'E')
              break parseFloat;
            else {
              return false;
            }
          }
          break;
        case DECIMAL_MODE_MAYBE_TRAILING_ZEROS:
          assert trailingZeros > 0;
          if (c == '0')
            ++trailingZeros;
          else {
            if (c >= '1' && c <= '9') {
              n_digits += trailingZeros;
              fractionDigits += trailingZeros;
              for (int i = 0; i < trailingZeros; i++)
                integralDigits.append('0');
              integralDigits.append(c);
              ++n_digits;
              ++fractionDigits;
              trailingZeros = 0; 
              mode = DECIMAL_MODE_IS_FRACTION;
            }
            else if (c == 'e' || c == 'E') {
              n_digits += trailingZeros;
              fractionDigits += trailingZeros;
              for (int i = 0; i < trailingZeros; i++)
                integralDigits.append('0');
              trailingZeros = 0; 
              break parseFloat;
            }
            else
              return false;
          }
          break;
      }
    }

    n_digits += trailingZeros;
    if (mode == DECIMAL_MODE_MAYBE_TRAILING_ZEROS) {
      mode = DECIMAL_MODE_IS_FRACTION;
    }
    if (mode == DECIMAL_MODE_IS_FRACTION && fractionDigits == 0) {
      // Assume there is at least one trailing fraction digits
      // when decimal point has been encountered.
      if (trailingZeros == 0)
        trailingZeros = 1;
    }
    totalDigits += fractionDigits;

    if (mode != DECIMAL_MODE_IS_INTEGRAL && mode != DECIMAL_MODE_IS_FRACTION || n_digits == 0)
      return false;
    
    if (integralDigits.length() == 0)
      integralDigits.append('0');

    String digitsString;
    int n_integralDigits;
    
    digitsString = integralDigits.toString();
    n_integralDigits = integralDigits.length();
    final long mantissa;
    if (n_integralDigits > 19)
      return false;
    else if (n_integralDigits == 19) {
      int cres;
      if (positive) {
        cres = "9223372036854775807".compareTo(digitsString);
        if (cres == 0)
          mantissa = Long.MAX_VALUE;
        else if (cres < 0)
          return false;
        else
          mantissa = Long.parseLong(digitsString);
      }
      else {
        cres = "9223372036854775808".compareTo(digitsString);
        if (cres == 0)
          mantissa = Long.MIN_VALUE;
        else if (cres < 0)
          return false;
        else
          mantissa = - Long.parseLong(digitsString);
      }
    }
    else {
      mantissa = positive ? Long.parseLong(digitsString) : -Long.parseLong(digitsString);
    }

    integralDigits.setLength(0);
    
    positive = true;
    if (pos < limitPosition) {
      char c = value[pos++]; 
      if (c == 'e' || c == 'E') {
        if (pos < limitPosition) {
          c = value[pos];
          if (c == '-' || c == '+') {
            ++pos;
            if (c != '+')
              positive = false;
          }
          boolean foundNonZero = false;
          for (; pos < limitPosition; pos++) {
            switch (c = value[pos]) {
              case '0':
                if (!foundNonZero)
                  continue;
                else
                  integralDigits.append(c);
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
                if (!foundNonZero)
                  foundNonZero = true;
                integralDigits.append(c);
                break;
              default:
                return false;
            }
          }
        }
      }
    }
    
    if (integralDigits.length() == 0)
      integralDigits.append('0');

    digitsString = integralDigits.toString();
    int exponent;
    try { 
      exponent = Integer.parseInt(digitsString);
    }
    catch (NumberFormatException nfe) {
      return false;
    }
    if (positive)
      exponent -= fractionDigits;
    else
      exponent = -(exponent + fractionDigits);
    // range of valid exponent: [-(2^14 - 1) ... 2^14 - 1]
    if (exponent < -16383 || 16383 < exponent)
      return false;
    
    scribble.longValue = mantissa;
    scribble.intValue1 = exponent;
    
    scribble.intValue2 = FLOAT_VALUE;
    return true;
  }
  
  public boolean doProcessDecimal(char[] value, int offset, int length, Scribble scribble, StringBuilder integralDigits, StringBuilder reverseFractionalDigits) {

    if (!trimWhitespaces(value, offset, length))
      return false;
    
    int digits = 0; // for detecting zero-length decimal
    int totalDigits = 0, fractionDigits = 0;
    int trailingZeros = 0;

    integralDigits.setLength(0);
    reverseFractionalDigits.setLength(0);
    
    boolean positive = true;
    boolean syntaxInvalid = false;
    int pos, mode;
    for (pos = startPosition, mode = DECIMAL_MODE_MAYBE_SIGN;
         pos < limitPosition && !syntaxInvalid; pos++) {
      final char c = value[pos];
      switch (mode) {
        case DECIMAL_MODE_MAYBE_SIGN:
          if (c == '-' || c == '+') {
            mode = DECIMAL_MODE_MAYBE_INTEGRAL;
            if (c != '+')
              positive = false;
          }
          else if (c == '.') {
            mode = DECIMAL_MODE_IS_FRACTION;
          }
          else if (c >= '0' && c <= '9') {
            mode = DECIMAL_MODE_IS_INTEGRAL;
            ++digits;
            if (c != '0') {
              integralDigits.append(c);
              ++totalDigits;
            }
          }
          else
            syntaxInvalid = true;
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
              integralDigits.append(c);
              ++totalDigits;
            }
          }
          else
            syntaxInvalid = true;
          break;
        case DECIMAL_MODE_IS_FRACTION:
          if (c == '0') {
            ++trailingZeros;
            mode = DECIMAL_MODE_MAYBE_TRAILING_ZEROS;
          }
          else {
            if (c >= '1' && c <= '9') {
              reverseFractionalDigits.append(c);
              ++digits;
              ++fractionDigits;
            }
            else {
              syntaxInvalid = true;
            }
          }
          break;
        case DECIMAL_MODE_MAYBE_TRAILING_ZEROS:
          assert trailingZeros > 0;
          if (c == '0')
            ++trailingZeros;
          else {
            if (c >= '1' && c <= '9') {
              digits += trailingZeros;
              fractionDigits += trailingZeros;
              for (int i = 0; i < trailingZeros; i++)
                reverseFractionalDigits.append('0');
              reverseFractionalDigits.append(c);
              ++digits;
              ++fractionDigits;
              trailingZeros = 0; 
              mode = DECIMAL_MODE_IS_FRACTION;
            }
            else
              syntaxInvalid = true;
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
        if (trailingZeros == 0)
          trailingZeros = 1;
      }
      totalDigits += fractionDigits;
    }

    if (syntaxInvalid || mode != DECIMAL_MODE_IS_INTEGRAL && mode != DECIMAL_MODE_IS_FRACTION || digits == 0)
      return false;
    
    if (integralDigits.length() == 0)
      integralDigits.append('0');
    if (reverseFractionalDigits.length() == 0)
      reverseFractionalDigits.append('0');
    else
      reverseFractionalDigits.reverse();
    
    scribble.booleanValue1 = !positive;
    scribble.stringValue1 = integralDigits.toString();
    scribble.stringValue2 = reverseFractionalDigits.toString();
    
    scribble.intValue2 = DECIMAL_VALUE;
    return true;
  }
  
  @Override
  public void scribe(String value, Scribble scribble, int localName, OutputStream channelStream, Scriber scriber) throws IOException {
    switch (scribble.intValue2) {
      case INTEGER_VALUE:
        scribeIntegerValue(scribble.booleanValue2, scribble.longValue, scribble.stringValue1, 
            scribble.intValue1, scribble.booleanValue1, channelStream, scriber);
        return;
      case FLOAT_VALUE:
        scribeFloatValue(scribble.longValue, scribble.intValue1, channelStream, scriber);
        return;
      case DECIMAL_VALUE:
        scribeDecimalValue(scribble.stringValue1, scribble.stringValue2, scribble.booleanValue1, channelStream, scriber);
        return;
      default:
        assert false;
        break;
    }
  }
  
  private void scribeIntegerValue(boolean useIntValue, long longValue, String digits, int intValue, 
      boolean isNegative, OutputStream ostream, Scriber scriber) throws IOException {
      scriber.writeNBitUnsigned(INTEGER_VALUE, NUMBER_TYPE_WIDTH, ostream);
      scriber.writeBoolean(isNegative, ostream);
      if (useIntValue) {
        assert isNegative && intValue != 0 || !isNegative;
        scriber.writeUnsignedInteger32(isNegative ? intValue - 1 : intValue, ostream);
      }
      else if (longValue != Long.MIN_VALUE)
        scriber.writeUnsignedInteger64(isNegative ? longValue - 1 : longValue, ostream);
      else {
        BigInteger bint = new BigInteger(digits);
        if (isNegative)
          bint = bint.subtract(BigInteger.ONE);
        scriber.writeUnsignedInteger(bint, ostream);
      }
  }

  private void scribeFloatValue(long mantissa, int exponent, OutputStream ostream, Scriber scriber) 
    throws IOException {
    boolean isNegative;
    scriber.writeNBitUnsigned(FLOAT_VALUE, NUMBER_TYPE_WIDTH, ostream);
    if (isNegative = (mantissa & 0x8000000000000000L) != 0) {
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

  private void scribeDecimalValue(String integralDigits, String fractionalDigits, boolean isNegative, 
      OutputStream ostream, Scriber scriber) throws IOException {
    scriber.writeNBitUnsigned(DECIMAL_VALUE, NUMBER_TYPE_WIDTH, ostream);
    scriber.writeBoolean(isNegative, ostream);

    final int n_integralDigits = integralDigits.length();
    if (n_integralDigits < 10 || n_integralDigits == 10 && "2147483647".compareTo(integralDigits) >= 0) {
      final int integralDigitsIntValue = Integer.parseInt(integralDigits);
      scriber.writeUnsignedInteger32(integralDigitsIntValue, ostream);
    }
    else {
      final BigInteger integralDigitsIntegerValue = new BigInteger(integralDigits);
      scriber.writeUnsignedInteger(integralDigitsIntegerValue, ostream);
    }
      
    final int n_fractionalDigits = fractionalDigits.length();
    if (n_fractionalDigits < 10 || n_fractionalDigits == 10 && "2147483647".compareTo(fractionalDigits) >= 0) {
      final int fractionalDigitsIntValue = Integer.parseInt(fractionalDigits);
      scriber.writeUnsignedInteger32(fractionalDigitsIntValue, ostream);
    }
    else {
      final BigInteger fractionalDigitsIntegerValue = new BigInteger(fractionalDigits);
      scriber.writeUnsignedInteger(fractionalDigitsIntegerValue, ostream);
    }
  }
  
  ////////////////////////////////////////////////////////////
  
  /**
   * Extract sign value from a scribble.
   */
  public static boolean getSignOfDecimal(Scribble scribble) {
    return scribble.booleanValue1;
  }
  
  /**
   * Extract integral digits from a scribble.
   */
  public static String getIntegralDigitsOfDecimal(Scribble scribble) {
    return scribble.stringValue1;
  }
  
  /**
   * Extract reverse-fractional digits from a scribble.
   */
  public static String getReverseFractionalDigitsOfDecimal(Scribble scribble) {
    return scribble.stringValue2;
  }
  
  /**
   * Convert -0.0 to + 0.0.
   */
  public static final void canonicalizeDecimalValue(Scribble scribble) {
    boolean sign = scribble.booleanValue1;
    String integralDigits = scribble.stringValue1;
    String reverseFractionalDigits = scribble.stringValue2;
    if (sign && "0".equals(integralDigits) && "0".equals(reverseFractionalDigits)) {
      scribble.booleanValue1 = false;
    }
  }
  
  ////////////////////////////////////////////////////////////

  /**
   * Extract mantissa value from a scribble.
   */
  public static long getMantissaOfFloat(Scribble scribble) {
    return scribble.longValue;
  }
  
  /**
   * Extract exponent value from a scribble.
   */
  public static int getExponentOfFloat(Scribble scribble) {
    return scribble.intValue1;
  }
  
  /**
   * Convert (mantissa, exponent) value pair into canonical form.
   */
  public static final void canonicalizeFloatValue(Scribble scribble) {
    long mantissa = scribble.longValue;
    int exponent = scribble.intValue1;
    if (exponent == -16384 && mantissa != 1L && mantissa != -1L)
      mantissa = 0L; // NaN
    else {
      if (mantissa != 0L) {
        while (mantissa % 10L == 0) {
          mantissa /= 10L;
          exponent += 1;
        }
      }
      else 
        exponent = 0;
    }
    scribble.longValue = mantissa;
    scribble.intValue1 = exponent;
  }

  ////////////////////////////////////////////////////////////
  
  private static class IntegerValue {
    boolean useIntValue;
    long longValue;
    String digits;
    int intValue;
    boolean isNegative;
    IntegerValue(boolean useIntValue, long longValue, String digits, int intValue, /*int width,*/ boolean isNegative) {
      this.useIntValue = useIntValue;
      this.longValue = longValue;
      this.digits = digits;
      this.intValue = intValue;
      this.isNegative = isNegative;
    }
  }

  private static final class FloatValue {
    final long mantissa;
    final int exponent;
    FloatValue(long mantissa, int exponent) {
      this.mantissa = mantissa;
      this.exponent = exponent;
    }
  }
  
  private static class DecimalValue {
    String integralDigits;
    String fractionalDigits;
    boolean isNegative;
    DecimalValue(String integralDigits, String fractionalDigits, boolean isNegative) {
      this.integralDigits = integralDigits;
      this.fractionalDigits = fractionalDigits;
      this.isNegative = isNegative;
    }
  }

  @Override
  public Object toValue(String value, Scribble scribble, Scriber scriber) {
    switch (scribble.intValue2) {
      case INTEGER_VALUE:
        return new IntegerValue(scribble.booleanValue2, scribble.longValue, scribble.stringValue1, 
            scribble.intValue1, scribble.booleanValue1);
      case FLOAT_VALUE:
        return new FloatValue(scribble.longValue, scribble.intValue1);
      case DECIMAL_VALUE:
        return new DecimalValue(scribble.stringValue1, scribble.stringValue2, scribble.booleanValue1);
      default:
        assert false;
        return null;
    }
  }

  @Override
  public void doScribe(Object value, int localName, OutputStream channelStream, Scriber scriber) throws IOException  {
    if (value instanceof IntegerValue) {
      final IntegerValue integerValue = (IntegerValue)value;
      scribeIntegerValue(integerValue.useIntValue, integerValue.longValue, integerValue.digits, 
          integerValue.intValue, integerValue.isNegative, channelStream, scriber);
    }
    else if (value instanceof FloatValue) {
      FloatValue floatValue = (FloatValue)value;
      scribeFloatValue(floatValue.mantissa, floatValue.exponent, channelStream, scriber);
    }
    else if (value instanceof DecimalValue) {
      final DecimalValue decimalValue = (DecimalValue)value;
      scribeDecimalValue(decimalValue.integralDigits, decimalValue.fractionalDigits, decimalValue.isNegative, channelStream, scriber);
    }
    else {
      assert false;
    }
  }
  
}
