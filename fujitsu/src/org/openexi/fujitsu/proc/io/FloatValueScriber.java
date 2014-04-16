package org.openexi.fujitsu.proc.io;

import java.io.IOException;
import java.io.OutputStream;

import org.openexi.fujitsu.proc.common.QName;
import org.openexi.fujitsu.proc.util.URIConst;
import org.openexi.fujitsu.schema.EXISchema;

final class FloatValueScriber extends ValueScriberBase {

  private static final int DECIMAL_MODE_MAYBE_SIGN     = 0;
  private static final int DECIMAL_MODE_MAYBE_INTEGRAL = 1;
  private static final int DECIMAL_MODE_IS_INTEGRAL    = 2;
  private static final int DECIMAL_MODE_IS_FRACTION    = 3;
  private static final int DECIMAL_MODE_MAYBE_TRAILING_ZEROS = 4;
  
  private StringBuffer integralDigits;

  public FloatValueScriber(Scriber scriber) {
    super(scriber, new QName("exi:double", URIConst.W3C_2009_EXI_URI));
    integralDigits = new StringBuffer();
  }

  @Override
  public short getCodecID() {
    return Scriber.CODEC_DOUBLE;
  }
  
  @Override
  public int getBuiltinRCS(int simpleType) {
    return BuiltinRCS.RCS_ID_DOUBLE;
  }

  ////////////////////////////////////////////////////////////
  
  @Override
  public boolean process(String value, int tp, EXISchema schema, Scribble scribble) {
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
          scribble.intValue1 = 16384;
          scribble.booleanValue1 = true;
          return true;
        }
        else if (len == 4 && value.charAt(startPosition) == '-' && value.charAt(startPosition + 1) == 'I' && value.charAt(startPosition + 2) == 'N') {
          // mantissa: -1 
          scribble.longValue = -1;
          // exponent: -16384
          scribble.intValue1 = 16384;
          scribble.booleanValue1 = true;
          return true;
        }
        return false;
      case 'N':
        if (len == 3 && value.charAt(startPosition) == 'N' && value.charAt(startPosition + 1) == 'a') {
          // mantissa: 0
          scribble.longValue = 0;
          // exponent: -16384
          scribble.intValue1 = 16384;
          scribble.booleanValue1 = true;
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
    scribble.longValue = mantissa;

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
    if (positive) {
      if ((exponent -= fractionDigits) < 0) {
        positive = false;
        exponent = -exponent;
      }
    }
    else {
      exponent += fractionDigits;
    }
    if (exponent > 16383)
      return false;
    
    scribble.intValue1 = exponent;
    scribble.booleanValue1 = !positive;
    
    return true;
  }
  
  @Override
  public void scribe(String value, Scribble scribble, String localName, String uri, int tp, OutputStream channelStream) throws IOException {
    scribeFloatValue(scribble.longValue, scribble.intValue1, scribble.booleanValue1, channelStream);
  }

  private static class FloatValue {
    long mantissa;
    int exponent;
    boolean isNegativeExponent;
    FloatValue(long mantissa, int exponent, boolean isNegativeExponent) {
      this.mantissa = mantissa;
      this.exponent = exponent;
      this.isNegativeExponent = isNegativeExponent;
    }
  }

  @Override
  public Object toValue(String value, Scribble scribble) {
    return new FloatValue(scribble.longValue, scribble.intValue1, scribble.booleanValue1);
  }
  
  @Override
  public void doScribe(Object value, String localName, String uri, int tp, OutputStream channelStream) throws IOException  {
    FloatValue floatValue = (FloatValue)value;
    scribeFloatValue(floatValue.mantissa, floatValue.exponent, floatValue.isNegativeExponent, channelStream);
  }

  ////////////////////////////////////////////////////////////
  
  private void scribeFloatValue(long mantissa, int unsignedExponent, boolean isNegativeExponent, 
      OutputStream ostream) throws IOException {
    final boolean isNegative;
    if (isNegative = mantissa < 0) {
      mantissa = -(mantissa + 1);
    }
    m_scriber.writeBoolean(isNegative, ostream);
    m_scriber.writeUnsignedInteger64(mantissa, ostream);
    if (isNegativeExponent) {
      if (unsignedExponent != 0)
        --unsignedExponent;
      else
        isNegativeExponent = false;
    }
    m_scriber.writeBoolean(isNegativeExponent, ostream);
    m_scriber.writeUnsignedInteger32(unsignedExponent, ostream);
  }

}
