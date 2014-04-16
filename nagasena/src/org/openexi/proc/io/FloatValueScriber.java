package org.openexi.proc.io;

import java.io.IOException;
import java.io.OutputStream;

import org.openexi.proc.common.QName;
import org.openexi.proc.util.ExiUriConst;
import org.openexi.schema.EXISchema;

public final class FloatValueScriber extends ValueScriberBase {
  
  public static final FloatValueScriber instance;
  static {
    instance = new FloatValueScriber();
  }

  private static final int DECIMAL_MODE_MAYBE_SIGN     = 0;
  private static final int DECIMAL_MODE_MAYBE_INTEGRAL = 1;
  private static final int DECIMAL_MODE_IS_INTEGRAL    = 2;
  private static final int DECIMAL_MODE_IS_FRACTION    = 3;
  private static final int DECIMAL_MODE_MAYBE_TRAILING_ZEROS = 4;
  
  private FloatValueScriber() {
    super(new QName("exi:double", ExiUriConst.W3C_2009_EXI_URI));
  }

  @Override
  public short getCodecID() {
    return Scriber.CODEC_DOUBLE;
  }
  
  @Override
  public int getBuiltinRCS(int simpleType, Scriber scriber) {
    return BuiltinRCS.RCS_ID_DOUBLE;
  }

  ////////////////////////////////////////////////////////////
  
  @Override
  public boolean process(String value, int tp, EXISchema schema, Scribble scribble, Scriber scriber) {
    return doProcess(value, scribble, scriber.stringBuilder1);
  }
  
  @Override
  public void scribe(String value, Scribble scribble, int localName, int uri, int tp, OutputStream channelStream, Scriber scriber) throws IOException {
    scribeFloatValue(scribble.longValue, scribble.intValue1, channelStream, scriber);
  }

  @Override
  public Object toValue(String value, Scribble scribble, Scriber scriber) {
    return new FloatValue(scribble.longValue, scribble.intValue1);
  }
  
  @Override
  public void doScribe(Object value, int localName, int uri, int tp, OutputStream channelStream, Scriber scriber) throws IOException  {
    FloatValue floatValue = (FloatValue)value;
    scribeFloatValue(floatValue.mantissa, floatValue.exponent, channelStream, scriber);
  }

  ////////////////////////////////////////////////////////////

  /**
   * Extract mantissa value from a scribble.
   */
  public static long getMantissa(Scribble scribble) {
    return scribble.longValue;
  }
  
  /**
   * Extract exponent value from a scribble.
   */
  public static int getExponent(Scribble scribble) {
    return scribble.intValue1;
  }
  
  /**
   * Convert (mantissa, exponent) value pair into canonical form.
   */
  public static final void canonicalizeValue(Scribble scribble) {
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

  public boolean doProcess(String value, Scribble scribble, StringBuilder integralDigits) {
    if (!trimWhitespaces(value))
      return false;

    int n_digits = 0; // for detecting zero-length decimal
    int totalDigits = 0, fractionDigits = 0;
    int trailingZeros = 0;

    final int len = limitPosition - startPosition;
    switch (value.charAt(limitPosition - 1)) {
      case 'F':
        if (len == 3 && value.charAt(startPosition) == 'I' && value.charAt(startPosition + 1) == 'N') {
          // mantissa: 1 
          scribble.longValue = 1;
          // exponent: -16384
          scribble.intValue1 = -16384;
          return true;
        }
        else if (len == 4 && value.charAt(startPosition) == '-' && value.charAt(startPosition + 1) == 'I' && value.charAt(startPosition + 2) == 'N') {
          // mantissa: -1 
          scribble.longValue = -1;
          // exponent: -16384
          scribble.intValue1 = -16384;
          return true;
        }
        return false;
      case 'N':
        if (len == 3 && value.charAt(startPosition) == 'N' && value.charAt(startPosition + 1) == 'a') {
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
    
    integralDigits.setLength(0);
    boolean positive = true;
    int pos, mode;
    parseFloat:
    for (pos = startPosition, mode = DECIMAL_MODE_MAYBE_SIGN; pos < limitPosition; pos++) {
      final char c = value.charAt(pos);
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
      char c = value.charAt(pos++); 
      if (c == 'e' || c == 'E') {
        if (pos < limitPosition) {
          c = value.charAt(pos);
          if (c == '-' || c == '+') {
            ++pos;
            if (c != '+')
              positive = false;
          }
          boolean foundNonZero = false;
          for (; pos < limitPosition; pos++) {
            switch (c = value.charAt(pos)) {
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
    
    return true;
  }
  
  private void scribeFloatValue(long mantissa, int exponent, OutputStream ostream, Scriber scriber) 
    throws IOException {
    boolean isNegative;
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

  ////////////////////////////////////////////////////////////

  private static final class FloatValue {
    final long mantissa;
    final int exponent;
    FloatValue(long mantissa, int exponent) {
      this.mantissa = mantissa;
      this.exponent = exponent;
    }
  }

}
