package org.openexi.proc.io;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;

import org.openexi.proc.common.QName;
import org.openexi.proc.util.ExiUriConst;
import org.openexi.schema.EXISchema;

final class DecimalValueScriber extends ValueScriberBase {
  
  private static final int DECIMAL_MODE_MAYBE_SIGN     = 0;
  private static final int DECIMAL_MODE_MAYBE_INTEGRAL = 1;
  private static final int DECIMAL_MODE_IS_INTEGRAL    = 2;
  private static final int DECIMAL_MODE_IS_FRACTION    = 3;
  private static final int DECIMAL_MODE_MAYBE_TRAILING_ZEROS = 4;
  
  private StringBuffer integralDigits;
  private StringBuffer reverseFractionalDigits;

  public DecimalValueScriber(Scriber scriber) {
    super(scriber, new QName("exi:decimal", ExiUriConst.W3C_2009_EXI_URI));
    integralDigits = new StringBuffer();
    reverseFractionalDigits = new StringBuffer();
  }

  @Override
  public short getCodecID() {
    return Scriber.CODEC_DECIMAL;
  }
  
  @Override
  public int getBuiltinRCS(int simpleType) {
    return BuiltinRCS.RCS_ID_DECIMAL;
  }

  ////////////////////////////////////////////////////////////
  
  @Override
  public boolean process(String value, int tp, EXISchema schema, Scribble scribble) {
    
    if (!trimWhitespaces(value))
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
    
    return true;
  }
  
  @Override
  public void scribe(String value, Scribble scribble, String localName, String uri, int tp, OutputStream channelStream) throws IOException {
    scribeDecimalValue(scribble.stringValue1, scribble.stringValue2, scribble.booleanValue1, channelStream);
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
  public Object toValue(String value, Scribble scribble) {
    return new DecimalValue(scribble.stringValue1, scribble.stringValue2, scribble.booleanValue1);
  }
  
  @Override
  public void doScribe(Object value, String localName, String uri, int tp, OutputStream channelStream) throws IOException  {
    final DecimalValue decimalValue = (DecimalValue)value;
    scribeDecimalValue(decimalValue.integralDigits, decimalValue.fractionalDigits, decimalValue.isNegative, channelStream);
  }

  ////////////////////////////////////////////////////////////
  
  private void scribeDecimalValue(String integralDigits, String fractionalDigits, boolean isNegative, 
      OutputStream ostream) throws IOException {

    m_scriber.writeBoolean(isNegative, ostream);

    final int n_integralDigits = integralDigits.length();
    if (n_integralDigits < 10 || n_integralDigits == 10 && "2147483647".compareTo(integralDigits) >= 0) {
      final int integralDigitsIntValue = Integer.parseInt(integralDigits);
      m_scriber.writeUnsignedInteger32(integralDigitsIntValue, ostream);
    }
    else {
      final BigInteger integralDigitsIntegerValue = new BigInteger(integralDigits);
      m_scriber.writeUnsignedInteger(integralDigitsIntegerValue, ostream);
    }
      
    final int n_fractionalDigits = fractionalDigits.length();
    if (n_fractionalDigits < 10 || n_fractionalDigits == 10 && "2147483647".compareTo(fractionalDigits) >= 0) {
      final int fractionalDigitsIntValue = Integer.parseInt(fractionalDigits);
      m_scriber.writeUnsignedInteger32(fractionalDigitsIntValue, ostream);
    }
    else {
      final BigInteger fractionalDigitsIntegerValue = new BigInteger(fractionalDigits);
      m_scriber.writeUnsignedInteger(fractionalDigitsIntegerValue, ostream);
    }
  }
  
}
