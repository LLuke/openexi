package org.openexi.proc.io;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;

import org.openexi.proc.common.QName;
import org.openexi.proc.util.ExiUriConst;
import org.openexi.schema.EXISchema;

public final class DecimalValueScriber extends ValueScriberBase {
  
  public static final DecimalValueScriber instance;
  static {
    instance = new DecimalValueScriber();
  }
  
  private DecimalValueScriber() {
    super(new QName("exi:decimal", ExiUriConst.W3C_2009_EXI_URI));
  }

  @Override
  public short getCodecID() {
    return Scriber.CODEC_DECIMAL;
  }
  
  @Override
  public int getBuiltinRCS(int simpleType, Scriber scriber) {
    return BuiltinRCS.RCS_ID_DECIMAL;
  }

  ////////////////////////////////////////////////////////////
  
  @Override
  public boolean process(String value, int tp, EXISchema schema, Scribble scribble, Scriber scriber) {
    return doProcess(value, scribble, scriber.stringBuilder1, scriber.stringBuilder2);
  }
  
  @Override
  public void scribe(String value, Scribble scribble, int localName, int uri, int tp, OutputStream channelStream, Scriber scriber) throws IOException {
    scribeDecimalValue(scribble.stringValue1, scribble.stringValue2, scribble.booleanValue1, channelStream, scriber);
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
    return new DecimalValue(scribble.stringValue1, scribble.stringValue2, scribble.booleanValue1);
  }
  
  @Override
  public void doScribe(Object value, int localName, int uri, int tp, OutputStream channelStream, Scriber scriber) throws IOException  {
    final DecimalValue decimalValue = (DecimalValue)value;
    scribeDecimalValue(decimalValue.integralDigits, decimalValue.fractionalDigits, decimalValue.isNegative, channelStream, scriber);
  }

  ////////////////////////////////////////////////////////////
  
  /**
   * Extract sign value from a scribble.
   */
  public static boolean getSign(Scribble scribble) {
    return scribble.booleanValue1;
  }
  
  /**
   * Extract integral digits from a scribble.
   */
  public static String getIntegralDigits(Scribble scribble) {
    return scribble.stringValue1;
  }
  
  /**
   * Extract reverse-fractional digits from a scribble.
   */
  public static String getReverseFractionalDigits(Scribble scribble) {
    return scribble.stringValue2;
  }
  
  /**
   * Convert -0.0 to + 0.0.
   */
  public static final void canonicalizeValue(Scribble scribble) {
    boolean sign = scribble.booleanValue1;
    String integralDigits = scribble.stringValue1;
    String reverseFractionalDigits = scribble.stringValue2;
    if (sign && "0".equals(integralDigits) && "0".equals(reverseFractionalDigits)) {
      scribble.booleanValue1 = false;
    }
  }
  
  public boolean doProcess(String value, Scribble scribble, StringBuilder integralDigits, StringBuilder reverseFractionalDigits) {

    if (!trimWhitespaces(value))
      return false;
    
    final boolean observeC14N = scribble.booleanValue2;

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
    
    final int n_integralDigits = integralDigits.length();
    final int n_reverseFractionalDigits = reverseFractionalDigits.length();
    if (n_integralDigits == 0)
      integralDigits.append('0');
    if (n_reverseFractionalDigits == 0)
      reverseFractionalDigits.append('0');
    else
      reverseFractionalDigits.reverse();

    if (observeC14N && !positive) {
      // According to C14N encoding rule, the sign value MUST be zero (0) if both the integral portion 
      // and the fractional portion of the Decimal value are 0 (zero).
      positive = n_integralDigits == 0 && n_reverseFractionalDigits == 0;
    }
    
    scribble.booleanValue1 = !positive;
    scribble.stringValue1 = integralDigits.toString();
    scribble.stringValue2 = reverseFractionalDigits.toString();
    
    return true;
  }
  
  private void scribeDecimalValue(String integralDigits, String fractionalDigits, boolean isNegative, 
      OutputStream ostream, Scriber scriber) throws IOException {

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
  
}
