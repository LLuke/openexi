package org.openexi.proc.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import org.openexi.proc.common.QName;
import org.openexi.schema.Base64;
import org.openexi.schema.EXISchema;
import org.openexi.schema.EXISchemaConst;
import org.openexi.schema.XSDateTime;

final class EnumerationValueScriber extends ValueScriberBase {
  
  private final DatatypeFactory m_datatypeFactory;
  
  private final static FloatValueScriber m_floatValueScriber;
  private final static DateTimeValueScriber m_dateTimeValueScriber;
  private final static DateValueScriber m_dateValueScriber;
  private final static TimeValueScriber m_timeValueScriber;
  private final static GYearMonthValueScriber m_gYearMonthValueScriber;
  private final static GMonthDayValueScriber m_gMonthDayValueScriber;
  private final static GYearValueScriber m_gYearValueScriber;
  private final static GMonthValueScriber m_gMonthValueScriber;
  private final static GDayValueScriber m_gDayValueScriber;
  private final static DecimalValueScriber m_decimalValueScriber;
  static {
    m_floatValueScriber = FloatValueScriber.instance;
    m_dateTimeValueScriber = DateTimeValueScriber.instance;
    m_dateValueScriber = DateValueScriber.instance;
    m_timeValueScriber = TimeValueScriber.instance;
    m_gYearMonthValueScriber = GYearMonthValueScriber.instance;
    m_gMonthDayValueScriber = GMonthDayValueScriber.instance;
    m_gYearValueScriber = GYearValueScriber.instance;
    m_gMonthValueScriber = GMonthValueScriber.instance;
    m_gDayValueScriber = GDayValueScriber.instance;
    m_decimalValueScriber = DecimalValueScriber.instance;
  }

  public EnumerationValueScriber(DatatypeFactory datatypeFactory) {
    super((QName)null);
    m_datatypeFactory = datatypeFactory;
  }

  @Override
  public short getCodecID() {
    return Scriber.CODEC_ENUMERATION;
  }

  @Override
  public int getBuiltinRCS(int simpleType, Scriber scriber) {
    assert EXISchema.ATOMIC_SIMPLE_TYPE == scriber.schema.getVarietyOfSimpleType(simpleType); 
    final int baseType = scriber.schema.getBaseTypeOfSimpleType(simpleType);
    return scriber.getValueScriber(baseType).getBuiltinRCS(baseType, scriber);
  }

  @Override
  public final boolean process(String value, int tp, EXISchema schema, Scribble scribble, Scriber scriber) {
    assert schema.getVarietyOfSimpleType(tp) == EXISchema.ATOMIC_SIMPLE_TYPE;
    final int n_enums, index;
    n_enums = schema.getEnumerationFacetCountOfAtomicSimpleType(tp);
    assert n_enums > 0;
    final byte ancestryId = schema.ancestryIds[schema.getSerialOfType(tp)];
    final int whiteSpace = ancestryId == EXISchemaConst.STRING_TYPE ?
        schema.getWhitespaceFacetValueOfStringSimpleType(tp) : EXISchema.WHITESPACE_COLLAPSE;
    final String norm = whiteSpace == EXISchema.WHITESPACE_PRESERVE ? value : normalize(value, whiteSpace);  
    final Object data;
    if ((data = parseTextValue(norm, tp, ancestryId, schema, scribble, scriber)) != null && 
        (index = getEnumerationIndex(norm, data, tp, n_enums, schema)) >= 0) {
      scribble.intValue1 = index;
      return true;
    }
    return false;
  }

  @Override
  public void scribe(String value, Scribble scribble, int localName, int uri, int tp, OutputStream channelStream, Scriber scriber) throws IOException {
    scribeEnumeration(scribble.intValue1, tp, channelStream, scriber);
  }
  
  private void scribeEnumeration(int value, int tp, OutputStream ostream, Scriber scriber) throws IOException {
    int width;
    int n = scriber.schema.getEnumerationFacetCountOfAtomicSimpleType(tp) - 1;
    for (width = 0; n != 0; n >>= 1, ++width);
    scriber.writeNBitUnsigned(value, width, ostream);
  }
  
  ////////////////////////////////////////////////////////////
  
  private class EnumValue {
    int index;
    EnumValue(int index) {
      this.index = index;
    }
  }

  @Override
  public Object toValue(String value, Scribble scribble, Scriber scriber) {
    return new EnumValue(scribble.intValue1);
  }

  @Override
  public void doScribe(Object value, int localName, int uri, int tp, OutputStream channelStream, Scriber scriber) throws IOException  {
    int width;
    int n = scriber.schema.getEnumerationFacetCountOfAtomicSimpleType(tp) - 1;
    for (width = 0; n != 0; n >>= 1, ++width);
    ByteAlignedCommons.writeNBitUnsigned(((EnumValue)value).index, width, channelStream);
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Functions to enumeration
  ///////////////////////////////////////////////////////////////////////////
  
  private final int getEnumerationIndex(String norm, Object data, int stype, int n_enums, EXISchema schema) {
    final int variety = schema.getVarietyOfSimpleType(stype);
    assert variety != EXISchema.UNION_SIMPLE_TYPE && variety != EXISchema.LIST_SIMPLE_TYPE;
    for (int i = 0; i < n_enums; i++) {
      int facet = schema.getEnumerationFacetOfAtomicSimpleType(stype, i);
      assert facet != EXISchema.NIL_VALUE;
      if (equalToVariant(data, stype, schema.ancestryIds[schema.getSerialOfType(stype)], facet, schema)) {
        return i;
      }
    }
    return -1;
  }
  
  private final boolean equalToVariant(Object data, int stype, int ancestryId, int variant, EXISchema schema) {
    switch (ancestryId) {
      case EXISchemaConst.ANYURI_TYPE:
      case EXISchemaConst.STRING_TYPE:
        String str = schema.getStringValueOfVariant(variant);
        if (((String)data).equals(str))
          return true;
        break;
      case EXISchemaConst.DECIMAL_TYPE:
        final Scribble decimalScribble = (Scribble)data;
        if (DecimalValueScriber.getSign(decimalScribble) == schema.getSignOfDecimalVariant(variant) &&
            DecimalValueScriber.getIntegralDigits(decimalScribble).equals(schema.getIntegralDigitsOfDecimalVariant(variant)) &&
            DecimalValueScriber.getReverseFractionalDigits(decimalScribble).equals(schema.getReverseFractionalDigitsOfDecimalVariant(variant)))
            return true;
        break;
      case EXISchemaConst.INTEGER_TYPE:
        BigInteger integer;
        switch (schema.getTypeOfVariant(variant)) {
          case EXISchema.VARIANT_INT:
            int intVal = schema.getIntValueOfVariant(variant);
            integer = BigInteger.valueOf((long)intVal);
            break;
          case EXISchema.VARIANT_LONG:
            long longVal = schema.getLongValueOfVariant(variant);
            integer = BigInteger.valueOf(longVal);
            break;
          case EXISchema.VARIANT_INTEGER:
            integer = schema.getIntegerValueOfVariant(variant);
            break;
          default:
            assert false;
            return false;
        }
        if (((BigInteger)data).equals(integer))
          return true;
        break;
      case EXISchemaConst.FLOAT_TYPE:
      case EXISchemaConst.DOUBLE_TYPE:
        final Scribble floatScribble = (Scribble)data;
        if (FloatValueScriber.getMantissa(floatScribble) == schema.getMantissaOfFloatVariant(variant) && 
            FloatValueScriber.getExponent(floatScribble) == schema.getExponentOfFloatVariant(variant))
          return true;
        break;
      case EXISchemaConst.G_YEAR_TYPE:
      case EXISchemaConst.G_YEARMONTH_TYPE:
      case EXISchemaConst.G_MONTHDAY_TYPE:
      case EXISchemaConst.G_DAY_TYPE:
      case EXISchemaConst.G_MONTH_TYPE:
      case EXISchemaConst.TIME_TYPE:
      case EXISchemaConst.DATE_TYPE:
      case EXISchemaConst.DATETIME_TYPE:
        final XSDateTime dateTime = schema.getComputedDateTimeValueOfVariant(variant);
        return ((XSDateTime)data).equals(dateTime);
      case EXISchemaConst.DURATION_TYPE:
        Duration duration = schema.getDurationValueOfVariant(variant);
        if (((Duration)data).equals(duration))
          return true;
        break;
      case EXISchemaConst.HEXBINARY_TYPE:
      case EXISchemaConst.BASE64BINARY_TYPE:
        byte[] binary = schema.getBinaryValueOfVariant(variant);
        byte[] ibin = (byte[]) data; // instance binary data
        if (ibin.length == binary.length) {
          int j;
          for (j = 0; j < ibin.length && ibin[j] == binary[j]; j++);
          if (j == ibin.length)
            return true;
        }
        break;
      case EXISchemaConst.BOOLEAN_TYPE: // Enum facet does not apply to boolean
        assert false;
        break;
      default:
        assert false;
        break;
    }
    return false;
  }
  
  private Object parseTextValue(String norm, int stype, byte ancestryId, EXISchema schema, Scribble scribble, Scriber scriber) {
    assert schema.getVarietyOfSimpleType(stype) == EXISchema.ATOMIC_SIMPLE_TYPE;
    switch (ancestryId) {
      case EXISchemaConst.STRING_TYPE:
      case EXISchemaConst.ANYURI_TYPE:
        return norm;
      case EXISchemaConst.DECIMAL_TYPE:
        if (m_decimalValueScriber.doProcess(norm, scribble, scriber.stringBuilder1, scriber.stringBuilder2)) {
          DecimalValueScriber.canonicalizeValue(scribble);
          return scribble;
        }
        return null;
      case EXISchemaConst.INTEGER_TYPE:
        if (norm.length() != 0 && norm.charAt(0) == '+')
          norm = norm.substring(1);
        final BigInteger integer;
        try {
          integer = new BigInteger(norm);
        }
        catch (NumberFormatException nfe) {
          return null;
        }
        return integer;
      case EXISchemaConst.FLOAT_TYPE:
      case EXISchemaConst.DOUBLE_TYPE:
        if (m_floatValueScriber.doProcess(norm, scribble, scriber.stringBuilder1)) {
          FloatValueScriber.canonicalizeValue(scribble);
          return scribble;
        }
        return null;
      case EXISchemaConst.BOOLEAN_TYPE:
        return parseBoolean(norm, stype);
      case EXISchemaConst.DATETIME_TYPE:
        if (m_dateTimeValueScriber.process(norm, EXISchema.NIL_NODE, (EXISchema)null, scribble, (Scriber)null)) {
          DateTimeValueScriberBase.canonicalizeValue(scribble);
          return scribble.dateTime;
        }
        return null;
      case EXISchemaConst.DATE_TYPE:
        if (m_dateValueScriber.process(norm, EXISchema.NIL_NODE, (EXISchema)null, scribble, (Scriber)null)) {
          DateTimeValueScriberBase.canonicalizeValue(scribble);
          return scribble.dateTime;
        }
        return null;
      case EXISchemaConst.TIME_TYPE:
        if (m_timeValueScriber.process(norm, EXISchema.NIL_NODE, (EXISchema)null, scribble, (Scriber)null)) {
          DateTimeValueScriberBase.canonicalizeValue(scribble);
          return scribble.dateTime;
        }
        return null;
      case EXISchemaConst.G_YEARMONTH_TYPE:
        if (m_gYearMonthValueScriber.process(norm, EXISchema.NIL_NODE, (EXISchema)null, scribble, (Scriber)null)) {
          DateTimeValueScriberBase.canonicalizeValue(scribble);
          return scribble.dateTime;
        }
        return null;
      case EXISchemaConst.G_MONTHDAY_TYPE:
        if (m_gMonthDayValueScriber.process(norm, EXISchema.NIL_NODE, (EXISchema)null, scribble, (Scriber)null)) {
          DateTimeValueScriberBase.canonicalizeValue(scribble);
          return scribble.dateTime;
        }
        return null;
      case EXISchemaConst.G_YEAR_TYPE:
        if (m_gYearValueScriber.process(norm, EXISchema.NIL_NODE, (EXISchema)null, scribble, (Scriber)null)) {
          DateTimeValueScriberBase.canonicalizeValue(scribble);
          return scribble.dateTime;
        }
        return null;
      case EXISchemaConst.G_MONTH_TYPE:
        if (m_gMonthValueScriber.process(norm, EXISchema.NIL_NODE, (EXISchema)null, scribble, (Scriber)null)) {
          DateTimeValueScriberBase.canonicalizeValue(scribble);
          return scribble.dateTime;
        }
        return null;
      case EXISchemaConst.G_DAY_TYPE:
        if (m_gDayValueScriber.process(norm, EXISchema.NIL_NODE, (EXISchema)null, scribble, (Scriber)null)) {
          DateTimeValueScriberBase.canonicalizeValue(scribble);
          return scribble.dateTime;
        }
        return null;
      case EXISchemaConst.DURATION_TYPE:
        return parseDuration(norm);
      case EXISchemaConst.BASE64BINARY_TYPE:
        return Base64.decode(norm);
      case EXISchemaConst.HEXBINARY_TYPE:
        return HexBin.decode(norm);
      default:
        assert false;
        return null;
    }
  }

  private Duration parseDuration(String norm) {
    Duration duration = null;
    if (norm.length() != 0) {
      try {
        duration = m_datatypeFactory.newDuration(norm);
      }
      catch (IllegalArgumentException iae) {
      }
    }
    return duration;
  }
  
  private Boolean parseBoolean(String norm, int stype) {
    char c;
    boolean val;
    switch (norm.length()) {
      case 1:
        if ((c = norm.charAt(0)) == '1')
          val = true;
        else if (c == '0')
          val = false;
        else
          return null;
        break;
      case 4:
        if ((c = norm.charAt(0)) == 't' && "true".equals(norm))
          val = true;
        else
          return null;
        break;
      case 5:
        if ((c = norm.charAt(0)) == 'f' && "false".equals(norm))
          val = false;
        else
          return null;
        break;
      default:
        return null;
    }
    return Boolean.valueOf(val);
  }

  private static class HexBin {
    public static byte[] decode(String norm) {
      byte[] octets = null;
      if (norm != null) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
          int pos, len, dec;
          for (pos = 0, len = norm.length(), dec = 0; pos < len; dec = 0) {
            int nc;
            for (nc = 0; nc < 2 && pos < len; pos++) {
              char c = norm.charAt(pos);
              if (Character.isWhitespace(c)) {
                // Permit whitespaces for now.
                continue;
              }
              else if ('\u0060' < c) { // 'a' <= c
                if ('\u0066' < c) { // 'f' < c
                  return null;
                }
                else // between 'a' and 'f'
                  dec |= (10 + (c - 'a')) << (4 * (1 - nc++));
              }
              else if (c < '\u003a') { // c <= '9'
                if (c < '\u0030') { // c < '0'
                  return null;
                }
                else // between '0' and '9'
                  dec |= (c - '0') << (4 * (1 - nc++));
              }
              else if ('\u0040' < c && c < '\u0047') { // between 'A' and 'F'
                dec |= (10 + (c - 'A')) << (4 * (1 - nc++));
              }
              else { // off the range.
                return null;
              }
            }
            if (nc < 2) {
              return null;
            }
            baos.write(dec);
          }
        }
        finally {
          try {
            baos.close();
          }
          catch (IOException ioe) {
            ioe.printStackTrace();
          }
        }
        octets = baos.toByteArray();
      }
      return octets;
    }
  }
  
}
