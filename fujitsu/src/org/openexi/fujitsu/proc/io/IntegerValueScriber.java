package org.openexi.fujitsu.proc.io;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;

import org.openexi.fujitsu.proc.common.QName;
import org.openexi.fujitsu.proc.util.URIConst;
import org.openexi.fujitsu.schema.EXISchema;

public final class IntegerValueScriber extends ValueScriberBase {

  public IntegerValueScriber(Scriber scriber) {
    super(scriber, new QName("exi:integer", URIConst.W3C_2009_EXI_URI));
  }

  @Override
  public short getCodecID() {
    return Scriber.CODEC_INTEGER;
  }

  @Override
  public int getBuiltinRCS(int simpleType) {
    return BuiltinRCS.RCS_ID_INTEGER;
  }

  ////////////////////////////////////////////////////////////
  
  @Override
  public boolean process(String value, int tp, EXISchema schema, Scribble scribble) {
    if (!trimWhitespaces(value))
      return false;
    int pos = startPosition;
    boolean useIntValue = true;
    int intValue = 0;
    long longValue = 0;
    boolean isNegative = false;
    char c = value.charAt(pos);
    if (c == '-' || c == '+') {
      ++pos;
      if (c != '+')
        isNegative = true;
    }
    boolean foundNonZero = false;
    int st = -1;
    posLoop:
    for (int n_digits = 0; pos < limitPosition; pos++) {
      switch (c = value.charAt(pos)) {
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
        c = value.charAt(pos);
        if (c < '0' || '9' < c)
          return false;
      }
    }
    assert (st >= 0 && foundNonZero) || (!foundNonZero && longValue == 0 && intValue == 0 && useIntValue);
    if (!foundNonZero && isNegative) {
      isNegative = false;
    }
    
    if (schema.isIntegralSimpleType(tp)) {
      switch (scribble.intValue2 = schema.getWidthOfIntegralSimpleType(tp)) {
        /**
         * scribble.booleanValue is relevant only to default integer representation.
         * Values "true" and "false" represent signs of minus and plus. 
         */
        case EXISchema.INTEGER_CODEC_DEFAULT:
          break;
        case EXISchema.INTEGER_CODEC_NONNEGATIVE:
          if (isNegative)
            return false;
          if (scribble.booleanValue2 = useIntValue) {
            scribble.intValue1 = intValue;
          }
          else if ((scribble.longValue = longValue) == Long.MIN_VALUE) {
            scribble.stringValue1 = value.substring(st, limitPosition); 
          }
          return true;
        default:
          if (!useIntValue && longValue == Long.MIN_VALUE)
            return false;
          if (isNegative) {
            if (useIntValue)
              intValue = 0 - intValue;
            else
              longValue = 0 - longValue;
          }
          final int minInclusiveFacet = schema.getMinInclusiveFacetOfSimpleType(tp);
          switch (schema.getTypeOfVariant(minInclusiveFacet)) {
            case EXISchema.VARIANT_INT:
              final int minInclusiveIntValue = schema.getIntValueOfVariant(minInclusiveFacet);
              if (useIntValue) {
                if (intValue < minInclusiveIntValue || (intValue -= minInclusiveIntValue) > NBIT_INTEGER_RANGES[scribble.intValue2])
                  return false;
              }
              else {
                if (longValue < minInclusiveIntValue || (longValue -= minInclusiveIntValue) > NBIT_INTEGER_RANGES[scribble.intValue2])
                  return false;
                intValue = (int)longValue;
              }
              break;
            case EXISchema.VARIANT_LONG:
              final long minInclusiveLongValue = schema.getLongValueOfVariant(minInclusiveFacet);
              if (useIntValue) {
                if (intValue < minInclusiveLongValue || (intValue -= minInclusiveLongValue) > NBIT_INTEGER_RANGES[scribble.intValue2])
                  return false;
              }
              else {
                if (longValue < minInclusiveLongValue || (longValue -= minInclusiveLongValue) > NBIT_INTEGER_RANGES[scribble.intValue2])
                  return false;
                intValue = (int)longValue;
              }
              break;
            default:
              return false;
          }
          scribble.intValue1 = intValue;
          return true;
      }
    }
    scribble.intValue2 = EXISchema.INTEGER_CODEC_DEFAULT;
    scribble.booleanValue1 = isNegative;
    if (scribble.booleanValue2 = useIntValue) {
      scribble.intValue1 = intValue;
    }
    else if ((scribble.longValue = longValue) == Long.MIN_VALUE) {
      scribble.stringValue1 = value.substring(st, limitPosition); 
    }
    return true;
  }

  /**
   * Store an unsigned int value into scribble.
   * (For use by EXIOptionsEncoder)
   */
  public boolean processUnsignedInt(int uintValue, Scribble scribble) {
    assert uintValue >= 0;
    scribble.intValue2 = EXISchema.INTEGER_CODEC_NONNEGATIVE;
    scribble.booleanValue2 = true;
    scribble.intValue1 = uintValue;
    return true;
  }
  
  @Override
  public void scribe(String value, Scribble scribble, String localName, String uri, int tp, OutputStream channelStream) throws IOException {
    scribeIntegerValue(scribble.booleanValue2, scribble.longValue, scribble.stringValue1, scribble.intValue1, scribble.intValue2,
        scribble.booleanValue1, channelStream);
  }

  ////////////////////////////////////////////////////////////
  
  private static class IntegerValue {
    boolean useIntValue;
    long longValue;
    String digits;
    int intValue;
    int width;
    boolean isNegative;
    IntegerValue(boolean useIntValue, long longValue, String digits, int intValue, int width, boolean isNegative) {
      this.useIntValue = useIntValue;
      this.longValue = longValue;
      this.digits = digits;
      this.intValue = intValue;
      this.width = width;
      this.isNegative = isNegative;
    }
  }

  @Override
  public Object toValue(String value, Scribble scribble) {
    return new IntegerValue(scribble.booleanValue2, scribble.longValue, scribble.stringValue1, 
        scribble.intValue1, scribble.intValue2, scribble.booleanValue1);
  }
 
  @Override
  public void doScribe(Object value, String localName, String uri, int tp, OutputStream channelStream) throws IOException  {
    final IntegerValue integerValue = (IntegerValue)value;
    scribeIntegerValue(integerValue.useIntValue, integerValue.longValue, integerValue.digits, integerValue.intValue, 
        integerValue.width, integerValue.isNegative, channelStream);
  }

  ////////////////////////////////////////////////////////////

  private void scribeIntegerValue(boolean useIntValue, long longValue, String digits, int intValue, int width, 
      boolean isNegative, OutputStream ostream) throws IOException {
    switch (width) {
      case EXISchema.INTEGER_CODEC_DEFAULT:
        m_scriber.writeBoolean(isNegative, ostream);
        if (useIntValue) {
          assert isNegative && intValue != 0 || !isNegative;
          m_scriber.writeUnsignedInteger32(isNegative ? intValue - 1 : intValue, ostream);
        }
        else if (longValue != Long.MIN_VALUE)
          m_scriber.writeUnsignedInteger64(isNegative ? longValue - 1 : longValue, ostream);
        else {
          BigInteger bint = new BigInteger(digits);
          if (isNegative)
            bint = bint.subtract(BigInteger.ONE);
          m_scriber.writeUnsignedInteger(bint, ostream);
        }
        break;
      case EXISchema.INTEGER_CODEC_NONNEGATIVE:
        if (useIntValue)
          m_scriber.writeUnsignedInteger32(intValue, ostream);
        else if (longValue != Long.MIN_VALUE)
          m_scriber.writeUnsignedInteger64(longValue, ostream);
        else
          m_scriber.writeUnsignedInteger(new BigInteger(digits), ostream);
        break;
      default:
        m_scriber.writeNBitUnsigned(intValue, width, ostream);
        break;
    }
  }
  
}
