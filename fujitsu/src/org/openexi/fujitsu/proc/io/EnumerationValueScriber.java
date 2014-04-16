package org.openexi.fujitsu.proc.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.openexi.fujitsu.proc.common.QName;
import org.openexi.fujitsu.proc.util.Base64;
import org.openexi.fujitsu.schema.EXISchema;
import org.openexi.fujitsu.schema.EXISchemaConst;
import org.openexi.fujitsu.schema.SchemaValidatorException;
import org.openexi.fujitsu.schema.XSDateTime;
import org.openexi.fujitsu.schema.XSDecimal;

final class EnumerationValueScriber extends ValueScriberBase {
  
  private EXISchema m_schema;
  private final DatatypeFactory m_datatypeFactory;

  public EnumerationValueScriber(Scriber scriber, DatatypeFactory datatypeFactory) {
    super(scriber, (QName)null);
    m_datatypeFactory = datatypeFactory;
  }

  @Override
  public short getCodecID() {
    return Scriber.CODEC_ENUMERATION;
  }

  public void setEXISchema(EXISchema schema) {
    m_schema = schema;
  }

  @Override
  public int getBuiltinRCS(int simpleType) {
    final int baseType = m_schema.getBaseTypeOfType(simpleType);
    return m_scriber.getValueScriber(baseType).getBuiltinRCS(baseType);
  }

  @Override
  public final boolean process(String value, int tp, EXISchema schema, Scribble scribble) {
    final int n_enums, index;
    n_enums = m_schema.getEnumerationFacetCountOfSimpleType(tp);
    assert n_enums > 0;
    String norm = value;
    int whiteSpace = m_schema.getWhitespaceFacetValueOfSimpleType(tp);
    if (whiteSpace != EXISchema.WHITESPACE_ABSENT) {
      norm = normalize(value, whiteSpace);
    }
    final int primType = m_schema.getPrimitiveTypeOfAtomicSimpleType(tp);
    int primTypeId = m_schema.getSerialOfType(primType);
    Object data;
    if ((data = parseTextValue(norm, tp, primTypeId)) != null && 
        (index = getEnumerationIndex(norm, data, tp, primTypeId, n_enums)) >= 0) {
      scribble.intValue1 = index;
      return true;
    }
    return false;
  }

  @Override
  public void scribe(String value, Scribble scribble, String localName, String uri, int tp, OutputStream channelStream) throws IOException {
    scribeEnumeration(scribble.intValue1, tp, channelStream);
  }
  
  private void scribeEnumeration(int value, int tp, OutputStream ostream) throws IOException {
    int width;
    int n = m_schema.getEnumerationFacetCountOfSimpleType(tp) - 1;
    for (width = 0; n != 0; n >>= 1, ++width);
    m_scriber.writeNBitUnsigned(value, width, ostream);
  }
  
  ////////////////////////////////////////////////////////////
  
  private class EnumValue {
    int index;
    EnumValue(int index) {
      this.index = index;
    }
  }

  @Override
  public Object toValue(String value, Scribble scribble) {
    return new EnumValue(scribble.intValue1);
  }

  @Override
  public void doScribe(Object value, String localName, String uri, int tp, OutputStream channelStream) throws IOException  {
    int width;
    int n = m_schema.getEnumerationFacetCountOfSimpleType(tp) - 1;
    for (width = 0; n != 0; n >>= 1, ++width);
    ByteAlignedCommons.writeNBitUnsigned(((EnumValue)value).index, width, channelStream);
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Functions to enumeration
  ///////////////////////////////////////////////////////////////////////////
  
  private final int getEnumerationIndex(String norm, Object data, int stype, int primType, int n_enums) {
    int i, j, facet;
    for (i = 0; i < n_enums; i++) {
      if ((facet = m_schema.getEnumerationFacetOfSimpleType(stype, i)) != EXISchema.NIL_VALUE) {
        switch (primType) {
          case EXISchemaConst.ANY_SIMPLE_TYPE:
          case EXISchemaConst.ANYURI_TYPE:
          case EXISchemaConst.STRING_TYPE:
            String str = m_schema.getStringValueOfVariant(facet);
            if (norm.equals(str))
              return i;
            break;
          case EXISchemaConst.NOTATION_TYPE:
          case EXISchemaConst.QNAME_TYPE:
            assert false;
            break;
          case EXISchemaConst.DECIMAL_TYPE:
            BigDecimal decimal = m_schema.getDecimalValueOfVariant(facet);
            if ((((XSDecimal)data).getValue()).compareTo(decimal) == 0)
              return i;
            break;
          case EXISchemaConst.FLOAT_TYPE:
            Float flt = new Float(m_schema.getFloatValueOfVariant(facet));
            if (((Float)data).compareTo(flt) == 0)
              return i;
            break;
          case EXISchemaConst.DOUBLE_TYPE:
            Double dbl = new Double(m_schema.getDoubleValueOfVariant(facet));
            if (((Double)data).compareTo(dbl) == 0)
              return i;
            break;
          case EXISchemaConst.G_YEAR_TYPE:
          case EXISchemaConst.G_YEARMONTH_TYPE:
          case EXISchemaConst.G_MONTHDAY_TYPE:
          case EXISchemaConst.G_MONTH_TYPE:
          case EXISchemaConst.G_DAY_TYPE:
          case EXISchemaConst.TIME_TYPE:
          case EXISchemaConst.DATE_TYPE:
          case EXISchemaConst.DATETIME_TYPE:
            XSDateTime dateTime = m_schema.getDateTimeValueOfVariant(facet);
            if (((XSDateTime)data).equals(dateTime))
              return i;
            break;
          case EXISchemaConst.DURATION_TYPE:
            Duration duration = m_schema.getDurationValueOfVariant(facet);
            if (((Duration)data).equals(duration))
              return i;
            break;
          case EXISchemaConst.HEXBINARY_TYPE:
          case EXISchemaConst.BASE64BINARY_TYPE:
            byte[] binary = m_schema.getBinaryValueOfVariant(facet);
            byte[] ibin = (byte[]) data; // instance binary data
            if (ibin.length == binary.length) {
              for (j = 0; j < ibin.length && ibin[j] == binary[j]; j++);
              if (j == ibin.length)
                return i;
            }
            break;
          case EXISchemaConst.BOOLEAN_TYPE: // Enum facet does not apply to boolean
            assert false;
            break;
          default:
            assert false;
            break;
        }
      }
    }
    return -1;
  }
  
  private static final String normalize(final String text, final int whiteSpace) {
    
    if (whiteSpace != EXISchema.WHITESPACE_COLLAPSE &&
        whiteSpace != EXISchema.WHITESPACE_REPLACE) {
      return text;
    }
    
    /* Now we know that whiteSpace is either WHITESPACE_COLLAPSE or
       WHITESPACE_REPLACE. */
    
    int i;
    
    final int len;
    // for performance optimization
    for (i = 0, len = text.length(); i < len && !Character.isWhitespace(text.charAt(i)); i++);
    if (i == len) // no whitespace found.
      return text;

    final StringBuffer buf = new StringBuffer(len);

    if (whiteSpace == EXISchema.WHITESPACE_COLLAPSE) {
      boolean initState = true;
      boolean whiteSpaceDeposited = false;
      for (i = 0; i < len; i++) {
        final char ch;
        switch (ch = text.charAt(i)) {
          case '\t':
          case '\n':
          case '\r':
          case ' ':
            if (!initState) {
              whiteSpaceDeposited = true;
            }
            break;
          default:
            if (initState) {
              assert !whiteSpaceDeposited;
              initState = false;
            }
            else if (whiteSpaceDeposited) {
              buf.append(' ');
              whiteSpaceDeposited = false;
            }
            buf.append(ch);
            break;
        }
      }
    }
    else {
      assert whiteSpace == EXISchema.WHITESPACE_REPLACE;
      for (i = 0; i < len; i++) {
        final char ch;
        switch (ch = text.charAt(i)) {
          case '\t':
          case '\n':
          case '\r':
          case ' ':
            buf.append(' '); // replace
            break;
          default:
            buf.append(ch);
            break;
        }
      }
    }
    return buf.toString();
  }

  private Object parseTextValue(String norm, int stype, int primTypeId) {
    switch (primTypeId) {
      // xs:anySimpleType and xdt:anyAtomicType are semi-primitive.
      case EXISchemaConst.ANY_SIMPLE_TYPE:
      case EXISchemaConst.STRING_TYPE:
      case EXISchemaConst.NOTATION_TYPE:
      case EXISchemaConst.QNAME_TYPE:
      case EXISchemaConst.ANYURI_TYPE:
        return norm;
      case EXISchemaConst.DECIMAL_TYPE:
        final XSDecimal dec;
        try {
          dec = XSDecimal.parse(norm);
        }
        catch (SchemaValidatorException sve) {
          return null;
        }
        // lexical validation for integer types.
        final int builtinType = m_schema.getBuiltinTypeOfAtomicSimpleType(stype);
        if (m_schema.getSerialOfType(builtinType) != EXISchemaConst.DECIMAL_TYPE &&
            dec.getTrailingZeros() != 0) {
          return null;
        }
        return dec;
      case EXISchemaConst.FLOAT_TYPE:
        return parseFloatOrDouble(norm, true);
      case EXISchemaConst.DOUBLE_TYPE:
        return parseFloatOrDouble(norm, false);
      case EXISchemaConst.BOOLEAN_TYPE:
        return parseBoolean(norm, stype);
      case EXISchemaConst.DATETIME_TYPE:
      case EXISchemaConst.DATE_TYPE:
      case EXISchemaConst.TIME_TYPE:
      case EXISchemaConst.G_YEARMONTH_TYPE:
      case EXISchemaConst.G_MONTHDAY_TYPE:
      case EXISchemaConst.G_YEAR_TYPE:
      case EXISchemaConst.G_MONTH_TYPE:
      case EXISchemaConst.G_DAY_TYPE:
        return parseDateTime(norm, m_schema, stype, primTypeId);
      case EXISchemaConst.DURATION_TYPE:
        return parseDuration(norm);
      case EXISchemaConst.BASE64BINARY_TYPE:
      case EXISchemaConst.HEXBINARY_TYPE:
        return parseBinary(norm, stype, primTypeId);
      default:
        return null;
    }
  }

  private byte[] parseBinary(String norm, int stype, int primTypeId) {
    boolean isBase64;
    switch (primTypeId) {
      case EXISchemaConst.BASE64BINARY_TYPE:
        isBase64 = true;
        break;
      case EXISchemaConst.HEXBINARY_TYPE:
        isBase64 = false;
        break;
      default:
        throw new UnsupportedOperationException(); // for developers
    }
    return isBase64 ? Base64.decode(norm) : HexBin.decode(norm);
  }

  private XSDateTime parseDateTime(String norm, EXISchema corpus, int stype, int primTypeId) {
    final XMLGregorianCalendar dateTime;
    try {
      dateTime = m_datatypeFactory.newXMLGregorianCalendar(norm);
    }
    catch (IllegalArgumentException iae) {
      return null;
    }
    final boolean hasYear, hasMonth, hasDay, hasHour, hasMinute, hasSecond, hasTimezone;
    final javax.xml.namespace.QName eqname;
    switch (primTypeId) {
      case EXISchemaConst.DATETIME_TYPE:
        eqname = DatatypeConstants.DATETIME;
        hasYear = hasMonth = hasDay = hasHour = hasMinute = hasSecond = hasTimezone = true;
        break;
      case EXISchemaConst.TIME_TYPE:
        eqname = DatatypeConstants.TIME;
        hasHour = hasMinute = hasSecond = hasTimezone = true;
        hasYear = hasMonth = hasDay = false; 
        break;
      case EXISchemaConst.DATE_TYPE:
        eqname = DatatypeConstants.DATE;
        hasYear = hasMonth = hasDay = hasTimezone = true;
        hasHour = hasMinute = hasSecond = false;
        break;
      case EXISchemaConst.G_YEARMONTH_TYPE:
        eqname = DatatypeConstants.GYEARMONTH;
        hasYear = hasMonth = hasTimezone = true;
        hasDay = hasHour = hasMinute = hasSecond = false; 
        break;
      case EXISchemaConst.G_YEAR_TYPE:
        eqname = DatatypeConstants.GYEAR;
        hasYear = hasTimezone = true; 
        hasMonth = hasDay = hasHour = hasMinute = hasSecond = false;
        break;
      case EXISchemaConst.G_MONTHDAY_TYPE:
        eqname = DatatypeConstants.GMONTHDAY;
        hasMonth = hasDay = hasTimezone = true;
        hasYear = hasHour = hasMinute = hasSecond = false;
        break;
      case EXISchemaConst.G_DAY_TYPE:
        eqname = DatatypeConstants.GDAY;
        hasDay = hasTimezone = true;
        hasYear = hasMonth = hasHour = hasMinute = hasSecond = false;
        break;
      case EXISchemaConst.G_MONTH_TYPE:
        eqname = DatatypeConstants.GMONTH;
        hasMonth = hasTimezone = true;
        hasYear = hasDay = hasHour = hasMinute = hasSecond = false;
        break;
      default:
        assert false;
        return null;
    }
    return eqname == dateTime.getXMLSchemaType() ? 
      new XSDateTime(hasYear ? dateTime.getYear() : DatatypeConstants.FIELD_UNDEFINED,
          hasMonth ? dateTime.getMonth() : DatatypeConstants.FIELD_UNDEFINED,
          hasDay ? dateTime.getDay() : DatatypeConstants.FIELD_UNDEFINED,
          hasHour ? dateTime.getHour() : DatatypeConstants.FIELD_UNDEFINED,
          hasMinute ? dateTime.getMinute() : DatatypeConstants.FIELD_UNDEFINED,
          hasSecond ? dateTime.getSecond() : DatatypeConstants.FIELD_UNDEFINED,
          hasSecond ? dateTime.getMillisecond() : DatatypeConstants.FIELD_UNDEFINED,
          hasTimezone ? dateTime.getTimezone() : DatatypeConstants.FIELD_UNDEFINED,
          primTypeId) : null;
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

  private Object parseFloatOrDouble(String norm, boolean isFloat) {
    final int len = norm.length();
    if (isFloat) {
      if (len > 0) {
        float val = Float.NaN;
        switch (norm.charAt(len - 1)) {
          case 'F':
            if (len == 3 && "INF".equals(norm))
              val = Float.POSITIVE_INFINITY;
            else if (len == 4 && "-INF".equals(norm))
              val = Float.NEGATIVE_INFINITY;
            break;
          case 'N':
            if (len == 3 && "NaN".equals(norm))
              val = Float.NaN;
            break;
          default:
            try {
              val = Float.parseFloat(norm);
            }
            catch (NumberFormatException nfe) {
              return null;
            }
            break;
        }
        return new Float(val);
      }
    }
    else {
      if (len > 0) {
        double val = Double.NaN;
        switch (norm.charAt(len - 1)) {
          case 'F':
            if (len == 3 && "INF".equals(norm))
              val = Double.POSITIVE_INFINITY;
            else if (len == 4 && "-INF".equals(norm))
              val = Double.NEGATIVE_INFINITY;
            break;
          case 'N':
            if (len == 3 && "NaN".equals(norm))
              val = Double.NaN;
            break;
          default:
            try {
              val = Double.parseDouble(norm);
            }
            catch (NumberFormatException nfe) {
              break;
            }
        }
        return new Double(val);
      }
    }
    return null;
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
