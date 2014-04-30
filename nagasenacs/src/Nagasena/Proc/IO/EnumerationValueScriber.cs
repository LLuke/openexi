using System;
using System.Diagnostics;
using System.Globalization;
using System.IO;
using System.Numerics;
using System.Text;
using System.Xml;

using QName = Nagasena.Proc.Common.QName;
using Base64 = Nagasena.Schema.Base64;
using EXISchema = Nagasena.Schema.EXISchema;
using EXISchemaConst = Nagasena.Schema.EXISchemaConst;
using XSDateTime = Nagasena.Schema.XSDateTime;

namespace Nagasena.Proc.IO {

  internal sealed class EnumerationValueScriber : ValueScriberBase {

    private static readonly FloatValueScriber m_floatValueScriber;
    private static readonly DateTimeValueScriber m_dateTimeValueScriber;
    private static readonly DateValueScriber m_dateValueScriber;
    private static readonly TimeValueScriber m_timeValueScriber;
    private static readonly GYearMonthValueScriber m_gYearMonthValueScriber;
    private static readonly GMonthDayValueScriber m_gMonthDayValueScriber;
    private static readonly GYearValueScriber m_gYearValueScriber;
    private static readonly GMonthValueScriber m_gMonthValueScriber;
    private static readonly GDayValueScriber m_gDayValueScriber;
    private static readonly DecimalValueScriber m_decimalValueScriber;
    static EnumerationValueScriber() {
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

    public EnumerationValueScriber() : 
      base((QName)null) {
    }

    public override short CodecID {
      get {
        return Scriber.CODEC_ENUMERATION;
      }
    }

    public override int getBuiltinRCS(int simpleType, Scriber scriber) {
      Debug.Assert(EXISchema.ATOMIC_SIMPLE_TYPE == scriber.schema.getVarietyOfSimpleType(simpleType));
      int baseType = scriber.schema.getBaseTypeOfSimpleType(simpleType);
      return scriber.getValueScriber(baseType).getBuiltinRCS(baseType, scriber);
    }

    public override bool process(string value, int tp, EXISchema schema, Scribble scribble, Scriber scriber) {
      Debug.Assert(schema.getVarietyOfSimpleType(tp) == EXISchema.ATOMIC_SIMPLE_TYPE);
      int n_enums, index;
      n_enums = schema.getEnumerationFacetCountOfAtomicSimpleType(tp);
      Debug.Assert(n_enums > 0);
      sbyte ancestryId = schema.ancestryIds[schema.getSerialOfType(tp)];
      int whiteSpace = ancestryId == EXISchemaConst.STRING_TYPE ? schema.getWhitespaceFacetValueOfStringSimpleType(tp) : EXISchema.WHITESPACE_COLLAPSE;
      string norm = whiteSpace == EXISchema.WHITESPACE_PRESERVE ? value : normalize(value, whiteSpace);
      object data;
      if ((data = parseTextValue(norm, tp, ancestryId, schema, scribble, scriber)) != null && (index = getEnumerationIndex(norm, data, tp, n_enums, schema)) >= 0) {
        scribble.intValue1 = index;
        return true;
      }
      return false;
    }

    public override void scribe(string value, Scribble scribble, int localName, int uri, int tp, Stream channelStream, Scriber scriber) {
      scribeEnumeration(scribble.intValue1, tp, channelStream, scriber);
    }

    private void scribeEnumeration(int value, int tp, Stream ostream, Scriber scriber) {
      int width;
      int n = scriber.schema.getEnumerationFacetCountOfAtomicSimpleType(tp) - 1;
      for (width = 0; n != 0; n >>= 1, ++width) {
        ;
      }
      scriber.writeNBitUnsigned(value, width, ostream);
    }

    ////////////////////////////////////////////////////////////

    private class EnumValue {
      private readonly EnumerationValueScriber outerInstance;

      internal int index;
      internal EnumValue(EnumerationValueScriber outerInstance, int index) {
        this.outerInstance = outerInstance;
        this.index = index;
      }
    }

    public override object toValue(string value, Scribble scribble, Scriber scriber) {
      return new EnumValue(this, scribble.intValue1);
    }

    public override void doScribe(object value, int localName, int uri, int tp, Stream channelStream, Scriber scriber) {
      int width;
      int n = scriber.schema.getEnumerationFacetCountOfAtomicSimpleType(tp) - 1;
      for (width = 0; n != 0; n >>= 1, ++width) {
        ;
      }
      ByteAlignedCommons.writeNBitUnsigned(((EnumValue)value).index, width, channelStream);
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Functions to enumeration
    ///////////////////////////////////////////////////////////////////////////

    private int getEnumerationIndex(string norm, object data, int stype, int n_enums, EXISchema schema) {
      int variety = schema.getVarietyOfSimpleType(stype);
      Debug.Assert(variety != EXISchema.UNION_SIMPLE_TYPE && variety != EXISchema.LIST_SIMPLE_TYPE);
      for (int i = 0; i < n_enums; i++) {
        int facet = schema.getEnumerationFacetOfAtomicSimpleType(stype, i);
        Debug.Assert(facet != EXISchema.NIL_VALUE);
        if (equalToVariant(data, stype, schema.ancestryIds[schema.getSerialOfType(stype)], facet, schema)) {
          return i;
        }
      }
      return -1;
    }

    private bool equalToVariant(object data, int stype, int ancestryId, int variant, EXISchema schema) {
      switch (ancestryId) {
        case EXISchemaConst.ANYURI_TYPE:
        case EXISchemaConst.STRING_TYPE:
          string str = schema.getStringValueOfVariant(variant);
          if (((string)data).Equals(str)) {
            return true;
          }
          break;
        case EXISchemaConst.DECIMAL_TYPE:
          Scribble decimalScribble = (Scribble)data;
          if (DecimalValueScriber.getSign(decimalScribble) == schema.getSignOfDecimalVariant(variant) && 
              DecimalValueScriber.getIntegralDigits(decimalScribble).Equals(schema.getIntegralDigitsOfDecimalVariant(variant)) && 
              DecimalValueScriber.getReverseFractionalDigits(decimalScribble).Equals(schema.getReverseFractionalDigitsOfDecimalVariant(variant))) {
              return true;
          }
          break;
        case EXISchemaConst.INTEGER_TYPE:
          BigInteger integer;
          switch (schema.getTypeOfVariant(variant)) {
            case EXISchema.VARIANT_INT:
              int intVal = schema.getIntValueOfVariant(variant);
              integer = new BigInteger(intVal);
              break;
            case EXISchema.VARIANT_LONG:
              long longVal = schema.getLongValueOfVariant(variant);
              integer = new BigInteger(longVal);
              break;
            case EXISchema.VARIANT_INTEGER:
              integer = schema.getIntegerValueOfVariant(variant);
              break;
            default:
              Debug.Assert(false);
              return false;
          }
          if (((BigInteger)data).Equals(integer)) {
            return true;
          }
          break;
        case EXISchemaConst.FLOAT_TYPE:
        case EXISchemaConst.DOUBLE_TYPE:
          Scribble floatScribble = (Scribble)data;
          if (FloatValueScriber.getMantissa(floatScribble) == schema.getMantissaOfFloatVariant(variant) && 
            FloatValueScriber.getExponent(floatScribble) == schema.getExponentOfFloatVariant(variant)) {
            return true;
          }
          break;
        case EXISchemaConst.G_YEAR_TYPE:
        case EXISchemaConst.G_YEARMONTH_TYPE:
        case EXISchemaConst.G_MONTHDAY_TYPE:
        case EXISchemaConst.G_DAY_TYPE:
        case EXISchemaConst.G_MONTH_TYPE:
        case EXISchemaConst.TIME_TYPE:
        case EXISchemaConst.DATE_TYPE:
        case EXISchemaConst.DATETIME_TYPE:
          XSDateTime dateTime = schema.getComputedDateTimeValueOfVariant(variant);
          return ((XSDateTime)data).Equals(dateTime);
        case EXISchemaConst.DURATION_TYPE:
          TimeSpan duration = schema.getDurationValueOfVariant(variant);
          if (((Duration)data).TimeSpan.Equals(duration)) {
            return true;
          }
          break;
        case EXISchemaConst.HEXBINARY_TYPE:
        case EXISchemaConst.BASE64BINARY_TYPE:
          byte[] binary = schema.getBinaryValueOfVariant(variant);
          byte[] ibin = (byte[]) data; // instance binary data
          if (ibin.Length == binary.Length) {
            int j;
            for (j = 0; j < ibin.Length && ibin[j] == binary[j]; j++) {
              ;
            }
            if (j == ibin.Length) {
              return true;
            }
          }
          break;
        case EXISchemaConst.BOOLEAN_TYPE: // Enum facet does not apply to boolean
          Debug.Assert(false);
          break;
        default:
          Debug.Assert(false);
          break;
      }
      return false;
    }

    private static string normalize(string text, int whiteSpace) {
      Debug.Assert(whiteSpace != EXISchema.WHITESPACE_PRESERVE);
      /* Now we know that whiteSpace is either WHITESPACE_COLLAPSE or
         WHITESPACE_REPLACE. */

      int i;

      int len;
      // for performance optimization
      for (i = 0, len = text.Length; i < len && !char.IsWhiteSpace(text[i]); i++) {
        ;
      }
      if (i == len) { // no whitespace found.
        return text;
      }

      StringBuilder buf = new StringBuilder(len);

      if (whiteSpace == EXISchema.WHITESPACE_COLLAPSE) {
        bool initState = true;
        bool whiteSpaceDeposited = false;
        for (i = 0; i < len; i++) {
          char ch;
          switch (ch = text[i]) {
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
                Debug.Assert(!whiteSpaceDeposited);
                initState = false;
              }
              else if (whiteSpaceDeposited) {
                buf.Append(' ');
                whiteSpaceDeposited = false;
              }
              buf.Append(ch);
              break;
          }
        }
      }
      else {
        Debug.Assert(whiteSpace == EXISchema.WHITESPACE_REPLACE);
        for (i = 0; i < len; i++) {
          char ch;
          switch (ch = text[i]) {
            case '\t':
            case '\n':
            case '\r':
            case ' ':
              buf.Append(' '); // replace
              break;
            default:
              buf.Append(ch);
              break;
          }
        }
      }
      return buf.ToString();
    }

    private object parseTextValue(string norm, int stype, sbyte ancestryId, EXISchema schema, Scribble scribble, Scriber scriber) {
      Debug.Assert(schema.getVarietyOfSimpleType(stype) == EXISchema.ATOMIC_SIMPLE_TYPE);
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
          if (norm.Length != 0 && norm[0] == '+') {
            norm = norm.Substring(1);
          }
          BigInteger integer;
          try {
            integer = BigInteger.Parse(norm, NumberFormatInfo.InvariantInfo);
          }
          catch (FormatException) {
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
          Debug.Assert(false);
          return null;
      }
    }

    private Duration parseDuration(string norm) {
      Duration duration = null;
      if (norm.Length != 0) {
        try {
          duration = new Duration(XmlConvert.ToTimeSpan(norm));
        }
        catch (System.FormatException) {
        }
      }
      return duration;
    }

    private bool? parseBoolean(string norm, int stype) {
      char c;
      bool val;
      switch (norm.Length) {
        case 1:
          if ((c = norm[0]) == '1') {
            val = true;
          }
          else if (c == '0') {
            val = false;
          }
          else {
            return null;
          }
          break;
        case 4:
          if ((c = norm[0]) == 't' && "true".Equals(norm)) {
            val = true;
          }
          else {
            return null;
          }
          break;
        case 5:
          if ((c = norm[0]) == 'f' && "false".Equals(norm)) {
            val = false;
          }
          else {
            return null;
          }
          break;
        default:
          return null;
      }
      return Convert.ToBoolean(val);
    }

    private class HexBin {
      public static byte[] decode(string norm) {
        byte[] octets = null;
        if (norm != null) {
          MemoryStream baos = new MemoryStream();
          try {
            int pos, len, dec;
            for (pos = 0, len = norm.Length, dec = 0; pos < len; dec = 0) {
              int nc;
              for (nc = 0; nc < 2 && pos < len; pos++) {
                char c = norm[pos];
                if (char.IsWhiteSpace(c)) {
                  // Permit whitespaces for now.
                  continue;
                }
                else if ('\u0060' < c) { // 'a' <= c
                  if ('\u0066' < c) { // 'f' < c
                    return null;
                  }
                  else { // between 'a' and 'f'
                    dec |= (10 + (c - 'a')) << (4 * (1 - nc++));
                  }
                }
                else if (c < '\u003a') { // c <= '9'
                  if (c < '\u0030') { // c < '0'
                    return null;
                  }
                  else { // between '0' and '9'
                    dec |= (c - '0') << (4 * (1 - nc++));
                  }
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
              baos.WriteByte((byte)dec);
            }
          }
          finally {
            try {
              baos.Close();
            }
            catch (IOException ioe) {
              Console.WriteLine(ioe.ToString());
              Console.Write(ioe.StackTrace);
            }
          }
          octets = baos.ToArray();
        }
        return octets;
      }
    }

    private class Duration {

      private readonly TimeSpan m_timeSpan;

      public TimeSpan TimeSpan {
        get {
          return m_timeSpan;
        }
      }

      internal Duration(TimeSpan timeSpan) {
        m_timeSpan = timeSpan;
      }
    }

  }

}