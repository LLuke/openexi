package org.openexi.proc.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.openexi.proc.common.QName;
import org.openexi.proc.util.Base64;
import org.openexi.schema.EXISchema;
import org.openexi.schema.EXISchemaConst;
import org.openexi.schema.SchemaValidatorException;
import org.openexi.schema.XSDateTime;
import org.openexi.schema.XSDecimal;

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
    int variety = m_schema.getVarietyOfSimpleType(simpleType);
    switch (variety) {
      case EXISchema.ATOMIC_SIMPLE_TYPE:
        final int baseType = m_schema.getBaseTypeOfAtomicSimpleType(simpleType);
        return m_scriber.getValueScriber(baseType).getBuiltinRCS(baseType);
      case EXISchema.LIST_SIMPLE_TYPE:
        final int itemType = m_schema.getItemTypeOfListSimpleType(simpleType);
        return m_scriber.getValueScriber(itemType).getBuiltinRCS(itemType);
      default:
        return EXISchema.NIL_NODE;
    }
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
    Object data;
    if ((data = parseTextValue(norm, tp)) != null && 
        (index = getEnumerationIndex(norm, data, tp, n_enums)) >= 0) {
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
  
  private final int getEnumerationIndex(String norm, Object data, int stype, int n_enums) {
    final int variety = m_schema.getVarietyOfSimpleType(stype);
    assert variety != EXISchema.UNION_SIMPLE_TYPE;
    enumerationsLoop:
    for (int i = 0; i < n_enums; i++) {
      int facet;
      if ((facet = m_schema.getEnumerationFacetOfSimpleType(stype, i)) != EXISchema.NIL_VALUE) {
        if (variety == EXISchema.LIST_SIMPLE_TYPE) {
          final int[] variants = m_schema.getListValueOfVariant(facet);
          Object[] objects = (Object[])data;
          final int n_variants = variants.length;
          if (objects.length == n_variants) {
            final int itemType = m_schema.getItemTypeOfListSimpleType(stype);
            final int itemAncestryId = m_schema.getAncestryIdOfSimpleType(itemType);
            for (int j = 0; j < n_variants; j++) {
              if (!equalToVariant(objects[j], itemType, itemAncestryId, variants[j]))
                continue enumerationsLoop;
            }
            return i;
          }
        }
        else if (equalToVariant(data, stype, m_schema.getAncestryIdOfSimpleType(stype), facet)) {
          return i;
        }
      }
    }
    return -1;
  }
  
  private final boolean equalToVariant(Object data, int stype, int ancestryId, int variant) {
    assert m_schema.getVarietyOfSimpleType(stype) == EXISchema.ATOMIC_SIMPLE_TYPE;
    switch (ancestryId) {
      case EXISchemaConst.ANY_SIMPLE_TYPE:
      case EXISchemaConst.ANYURI_TYPE:
      case EXISchemaConst.STRING_TYPE:
        String str = m_schema.getStringValueOfVariant(variant);
        if (((String)data).equals(str))
          return true;
        break;
      case EXISchemaConst.NOTATION_TYPE:
      case EXISchemaConst.QNAME_TYPE:
        assert false;
        break;
      case EXISchemaConst.DECIMAL_TYPE:
      case EXISchemaConst.INTEGER_TYPE:
        BigDecimal decimal = m_schema.getDecimalValueOfVariant(variant);
        if ((((XSDecimal)data).getValue()).compareTo(decimal) == 0)
          return true;
        break;
      case EXISchemaConst.FLOAT_TYPE:
        Float flt = new Float(m_schema.getFloatValueOfVariant(variant));
        if (((Float)data).compareTo(flt) == 0)
          return true;
        break;
      case EXISchemaConst.DOUBLE_TYPE:
        Double dbl = new Double(m_schema.getDoubleValueOfVariant(variant));
        if (((Double)data).compareTo(dbl) == 0)
          return true;
        break;
      case EXISchemaConst.G_YEAR_TYPE:
      case EXISchemaConst.G_YEARMONTH_TYPE:
      case EXISchemaConst.G_MONTHDAY_TYPE:
      case EXISchemaConst.G_MONTH_TYPE:
      case EXISchemaConst.G_DAY_TYPE:
      case EXISchemaConst.TIME_TYPE:
      case EXISchemaConst.DATE_TYPE:
      case EXISchemaConst.DATETIME_TYPE:
        XSDateTime dateTime = m_schema.getDateTimeValueOfVariant(variant);
        if (((XSDateTime)data).equals(dateTime))
          return true;
        break;
      case EXISchemaConst.DURATION_TYPE:
        Duration duration = m_schema.getDurationValueOfVariant(variant);
        if (((Duration)data).equals(duration))
          return true;
        break;
      case EXISchemaConst.HEXBINARY_TYPE:
      case EXISchemaConst.BASE64BINARY_TYPE:
        byte[] binary = m_schema.getBinaryValueOfVariant(variant);
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

  private Object parseTextValue(String norm, int stype) {
    final int variety = m_schema.getVarietyOfSimpleType(stype);
    if (variety == EXISchema.LIST_SIMPLE_TYPE) {
      final String[] stringItems = norm.split(" ");
      final int n_stringItems = stringItems.length;
      final Object[] objects = new Object[n_stringItems];
      final int itemType = m_schema.getItemTypeOfListSimpleType(stype);
      for (int i = 0; i < n_stringItems; i++) {
        objects[i] = parseTextValue(stringItems[i], itemType);
      }
      return objects;
    }
    else {
      assert variety == EXISchema.ATOMIC_SIMPLE_TYPE;
      final int ancestryId;
      switch (ancestryId = m_schema.getAncestryIdOfSimpleType(stype)) {
        // xs:anySimpleType and xdt:anyAtomicType are semi-primitive.
        case EXISchemaConst.ANY_SIMPLE_TYPE:
        case EXISchemaConst.STRING_TYPE:
        case EXISchemaConst.NOTATION_TYPE:
        case EXISchemaConst.QNAME_TYPE:
        case EXISchemaConst.ANYURI_TYPE:
          return norm;
        case EXISchemaConst.DECIMAL_TYPE:
        case EXISchemaConst.INTEGER_TYPE:
          final XSDecimal dec;
          try {
            dec = XSDecimal.parse(norm);
          }
          catch (SchemaValidatorException sve) {
            return null;
          }
          // lexical validation for integer types.
          int serial;
          while ((serial = m_schema.getSerialOfType(stype)) != EXISchemaConst.DECIMAL_TYPE) {
            if (serial == EXISchemaConst.INTEGER_TYPE) {
              if (dec.getTrailingZeros() != 0) {
                return null;
              }
              break;
            }
            stype = m_schema.getBaseTypeOfAtomicSimpleType(stype);
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
          return parseDateTime(norm, m_schema, stype, ancestryId);
        case EXISchemaConst.DURATION_TYPE:
          return parseDuration(norm);
        case EXISchemaConst.BASE64BINARY_TYPE:
        case EXISchemaConst.HEXBINARY_TYPE:
          return parseBinary(norm, stype, ancestryId);
        default:
          return null;
      }
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
